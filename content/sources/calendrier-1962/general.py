#!/usr/bin/env python3
"""Construit content/data/calendar/1962/general.json (sanctoral de 1960-1962) depuis Divinum Officium.

    python3 -I content/sources/calendrier-1962/general.py --divinum <clone>/divinum-officium \\
        [--missalemeum <clone>/Missalemeum] [--out content/data/calendar/1962/general.json]
    python3 -I content/sources/calendrier-1962/general.py --divinum <clone> --skeleton   # gabarit de sanctoral.tsv

Données lues (Divinum Officium, MIT) :
- le Kalendarium de 1960 composé sur ses bases (`divinum.kalendarium`) : fichiers de chaque jour,
  le premier célébré, les suivants commémorés ;
- pour chaque fichier, `[Officium]` et `[Rank]` de `horas/Latin/Sancti/` (conditions « rubrica 196 »
  évaluées), qui déterminent le titre latin et la classe, comme le fait `precedence()` de DO ;
- l'existence du propre de messe `missa/Latin/Sancti/<clé>.txt` (champ `proper`).

Données saisies ici : `sanctoral.tsv` (identifiant et titre français de chaque clé), les tables
TEMPORAL_KEYS, VIGILS, RANK_OVERRIDES, COLOR_OVERRIDES, INTRINSIC, LORD et LA_FIXES ci-dessous,
chacune justifiée.

Avec `--missalemeum`, compare classes, couleurs et commémoraisons à la liste SANCTI de Missalemeum
et affiche les écarts (rapport, rien n'est modifié).
"""

from __future__ import annotations

import argparse
import csv
import json
import re
import sys
import unicodedata
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))

import divinum  # noqa: E402

ROOT = HERE.parents[2]
DEFAULT_OUT = ROOT / "content" / "data" / "calendar" / "1962" / "general.json"
TSV = HERE / "sanctoral.tsv"

# Jours du Kalendarium qui relèvent du temporal (Proprium de Tempore du Missel de 1962) :
# cycle de Noël du 24 décembre au 13 janvier, féries après l'Épiphanie, Saint Nom de Jésus.
# Ils sont décrits dans TEMPORAL.md ; les saints commémorés ces jours-là restent ici.
TEMPORAL_KEYS = {
    "Sancti/01-00",  # Saint Nom de Jésus (fête mobile, date fictive 01-00 dans DO)
    "Sancti/01-01",  # Octave de Noël
    "Sancti/01-06",  # Épiphanie
    *(f"Sancti/01-{d:02d}" for d in range(7, 13)),  # féries « Die septima Januarii »… (IV)
    "Sancti/01-13",  # Commémoraison du Baptême
    "Sancti/12-24",  # Vigile de Noël
    "Sancti/12-25",  # Noël (trois messes 12-25m1, m2, m3)
}

# Vigiles (Rubricae generales n. 28-33). Couleur violette hors du temps pascal (n. 128 d).
VIGILS = {
    "Sancti/06-23": "VIGIL_II",   # Vigile de saint Jean-Baptiste
    "Sancti/06-28r": "VIGIL_II",  # Vigile des saints Pierre et Paul
    "Sancti/08-09t": "VIGIL_III",  # Vigile de saint Laurent
    "Sancti/08-14": "VIGIL_II",   # Vigile de l'Assomption
}

# Classe imposée quand le rang numérique de DO ne la donne pas directement.
RANK_OVERRIDES = {
    # Dans l'octave de Noël, saint Thomas Becket et saint Sylvestre ne sont plus que commémorés :
    # le jour (II) est « Die V/VII infra octavam » (n. 68 d-e ; Kalendarium 1960 ; Missalemeum rang 4).
    "Sancti/12-29r": "COMMEMORATION",
    "Sancti/12-31r": "COMMEMORATION",
}

# Couleur par commun de DO (« vide C2a-1 », « ex C11 »…), Rubricae generales n. 117-132.
COMMUNE_COLORS = [
    (r"^C1", "RED"),    # apôtres, évangélistes
    (r"^C2", "RED"),    # un martyr
    (r"^C3", "RED"),    # plusieurs martyrs
    (r"^C4", "WHITE"),  # confesseur pontife, docteur, pape
    (r"^C5", "WHITE"),  # confesseur non pontife, abbé
    (r"^C6a", "WHITE"),  # vierge non martyre
    (r"^C6", "RED"),    # vierge martyre (C6, C6-1, C6b)
    (r"^C7a", "WHITE"),  # sainte femme non martyre
    (r"^C7", "RED"),    # sainte femme martyre (C7, C7b)
    (r"^C8", "WHITE"),  # dédicace
    (r"^C9", "BLACK"),  # défunts
]  # C10, C11 (Sainte Vierge) : blanc, testé avant ^C1 dans color()

COLOR_OVERRIDES = {
    "Sancti/01-25r": "WHITE",   # Conversion de saint Paul (ex Sancti/01-25)
    "Sancti/03-19": "WHITE",    # saint Joseph
    "Sancti/03-24": "WHITE",    # saint Gabriel (ex Sancti/05-08)
    "Sancti/05-01r": "WHITE",   # saint Joseph artisan
    "Sancti/06-24": "WHITE",    # Nativité de saint Jean-Baptiste
    "Sancti/07-01": "RED",      # Précieux Sang
    "Sancti/08-06": "WHITE",    # Transfiguration
    "Sancti/09-14": "RED",      # Exaltation de la sainte Croix (ex Sancti/05-03)
    "Sancti/09-29": "WHITE",    # Dédicace de saint Michel (ex Sancti/05-08)
    "Sancti/10-24": "WHITE",    # saint Raphaël (ex Sancti/05-08)
    "Sancti/11-01": "WHITE",    # Toussaint (vide C3)
    "Sancti/12-27": "WHITE",    # saint Jean, apôtre et évangéliste (ex C1)
}

# Commémoraisons du Calendarium officiel (Breviarium Romanum 1961, `horas/Help/Rubrics/1960calendarium.pdf`
# dans DO) absentes du Kalendarium de DO, qui les traite dans le propre du jour : saint Pierre et saint Paul
# (n. 110), octave de Noël (n. 68, 108 c), Litanies majeures (n. 80, 108 f). `proper` : clé de DO quand elle
# existe, sinon None (oraisons dans le propre principal, sections « Commemoratio »/« Oratio Petri »…).
INTRINSIC = {
    "Sancti/01-25r": [("peter_apostle", "Saint Pierre, apôtre", "S. Petri Apostoli", None)],
    "Sancti/02-22": [("paul_apostle", "Saint Paul, apôtre", "S. Pauli Apostoli", None)],
    "Sancti/04-25": [("greater_litanies", "Litanies majeures", "Litaniæ Maiores", "Sancti/04-25c")],
    "Sancti/06-30": [("peter_apostle", "Saint Pierre, apôtre", "S. Petri Apostoli", None)],
    "Sancti/12-26": [("commemoration_of_the_octave_of_christmas", "Octave de Noël", "Octavæ Nativitatis Domini", None)],
    "Sancti/12-27": [("commemoration_of_the_octave_of_christmas", "Octave de Noël", "Octavæ Nativitatis Domini", None)],
    "Sancti/12-28r": [("commemoration_of_the_octave_of_christmas", "Octave de Noël", "Octavæ Nativitatis Domini", None)],
}

# Fêtes du Seigneur de Iʳᵉ ou IIᵉ classe (Rubricae generales n. 16 a) : l'emportent sur un dimanche
# de IIᵉ classe, sans commémoraison du dimanche. Missalemeum : FEASTS_OF_JESUS_CLASS_1_AND_2
# (02-02, 08-06) et second_class_feast_no_sunday_commemoration (09-14, 11-09).
LORD = {"Sancti/02-02", "Sancti/07-01", "Sancti/08-06", "Sancti/09-14", "Sancti/11-09"}

ENTRY_KEYS = ["id", "date", "rank", "colors", "title", "lord", "validFrom", "validUntil", "proper", "commemorations", "saints"]


def nfc(s: str) -> str:
    return unicodedata.normalize("NFC", s)


def commune_code(commune: str) -> str:
    m = re.search(r"\b(C\d+[a-z]*(?:-\d+)?[a-z]*)", commune)
    return m.group(1) if m else ""


def color(key: str, rank: str, proper: divinum.Proper) -> str:
    if key in COLOR_OVERRIDES:
        return COLOR_OVERRIDES[key]
    if rank.startswith("VIGIL"):
        return "VIOLET"
    code = commune_code(proper.commune)
    if re.match(r"^C1[01]", code):
        return "WHITE"
    for pattern, col in COMMUNE_COLORS:
        if re.match(pattern, code):
            return col
    raise SystemExit(f"{key} : couleur inconnue (commun « {proper.commune} ») — ajouter à COLOR_OVERRIDES")


def rank_of(key: str, proper: divinum.Proper) -> str:
    if key in VIGILS:
        return VIGILS[key]
    if key in RANK_OVERRIDES:
        return RANK_OVERRIDES[key]
    cls = divinum.dclass(proper.rank)
    if cls is None:
        raise SystemExit(f"{key} : rang {proper.rank} sans classe de 1960")
    return "COMMEMORATION" if cls == "IV" else cls


# Coquilles évidentes des titres latins de DO (le titre reste celui de DO pour tout le reste).
LA_FIXES = [
    (r"^SS\. ", "Ss. "),
    (r"^S\. Abdon et Sennen", "Ss. Abdon et Sennen"),
    (r"\bEpiscopis\b", "Episcopi"),
    (r"\bEpíscopi\b", "Episcopi"),
    (r"\bUrbanis\b", "Urbani"),
    (r"\bPresbyteris\b", "Presbyteri"),
    (r"\bArchangelis\b", "Archangeli"),
    (r"\bMártyrum\b", "Martyrum"),
    (r"Symphoriani Martyres$", "Symphoriani Martyrum"),
    (r"Canuti Regis et Martyrum$", "Canuti Regis et Martyris"),
    (r"Naboris and Nazarii", "Naboris et Nazarii"),
    (r"^Ss\. Mart\. Vitalis et Agricolæ$", "Ss. Vitalis et Agricolæ Martyrum"),
    (r"Laurentii Mart\.$", "Laurentii Martyris"),
]


def la_title(proper: divinum.Proper) -> str:
    title = re.sub(r"\s+", " ", proper.title).strip()
    for pattern, repl in LA_FIXES:
        title = re.sub(pattern, repl, title)
    return nfc(title)


def collect(www: Path) -> list[dict]:
    """Une entrée par jour du Kalendarium (hors temporal), avec ses commémoraisons."""
    days = divinum.kalendarium(www)
    out = []
    for day, info in sorted(days.items()):
        if day.endswith("-00") or day == "02-29":
            continue
        keys = [f"Sancti/{f}" for f in info["files"] if f"Sancti/{f}" not in TEMPORAL_KEYS]
        if not keys:
            continue
        items = []
        for key in keys:
            office = divinum.Proper(www, key, "horas")
            if not office.exists:
                office = divinum.Proper(www, key, "missa")
            mass = divinum.Proper(www, key, "missa")
            if not mass.exists:
                raise SystemExit(f"{day} : propre de messe absent pour {key}")
            rank = rank_of(key, office)
            items.append({"key": key, "date": day, "rank": rank, "color": color(key, rank, office),
                          "la": la_title(office), "commune": office.commune, "dorank": office.rank})
        out.append({"main": items[0], "commemorations": items[1:]})
    return out


def read_tsv() -> dict[str, dict]:
    rows = {}
    with TSV.open(encoding="utf-8", newline="") as f:
        for row in csv.DictReader((l for l in f if not l.startswith("#")), delimiter="\t"):
            rows[row["key"]] = {"id": row["id"].strip(), "fr": nfc(row["fr"].strip())}
    return rows


def build(www: Path, sources: list[str]) -> dict:
    meta = read_tsv()
    entries, seen_ids, used = [], set(), set()

    def ident(key: str) -> dict:
        if key not in meta:
            raise SystemExit(f"{key} absent de sanctoral.tsv (lancer --skeleton)")
        used.add(key)
        m = meta[key]
        if not re.fullmatch(r"[a-z0-9_]+", m["id"]):
            raise SystemExit(f"{key} : identifiant invalide {m['id']!r}")
        if m["id"] in seen_ids:
            raise SystemExit(f"{key} : identifiant en double {m['id']!r}")
        seen_ids.add(m["id"])
        return m

    for day in collect(www):
        main = day["main"]
        m = ident(main["key"])
        entry = {
            "id": m["id"],
            "date": main["date"],
            "rank": main["rank"],
            "colors": [main["color"]],
            "title": {"fr": m["fr"], "la": main["la"]},
            "lord": main["key"] in LORD,
            "proper": main["key"],
            "commemorations": [],
        }
        for cid, fr, la, proper in INTRINSIC.get(main["key"], []):
            entry["commemorations"].append(canonical({"id": cid, "title": {"fr": fr, "la": la}, "proper": proper}))
        for c in day["commemorations"]:
            cm = ident(c["key"])
            entry["commemorations"].append({"id": cm["id"], "title": {"fr": cm["fr"], "la": c["la"]}, "proper": c["key"]})
        entries.append(entry)

    unused = sorted(set(meta) - used)
    if unused:
        raise SystemExit(f"lignes inutilisées dans sanctoral.tsv : {unused}")
    entries.sort(key=lambda e: (e["date"], e["id"]))
    return {"format": 1, "rite": "1962", "calendar": "general", "sources": sources, "entries": [canonical(e) for e in entries]}


def canonical(entry: dict) -> dict:
    """Ordre des clés du schéma ; champs null, false ou vides omis."""
    out = {}
    for k in [k for k in ENTRY_KEYS if k in entry]:
        v = entry.get(k)
        if v is None or v is False or v == [] or v == {} or v == "":
            continue
        out[k] = v
    return out


def compare_missalemeum(www: Path, mm: Path) -> None:
    """Écarts avec la liste SANCTI de Missalemeum (clés renommées comme dans oracle.py)."""
    sys.path.insert(0, str((mm / "backend").resolve()))
    from api.constants.la import blocks  # noqa: E402
    import oracle  # noqa: E402

    import datetime
    letters = {"w": "WHITE", "r": "RED", "g": "GREEN", "v": "VIOLET", "p": "ROSE", "b": "BLACK"}
    mm_days: dict[str, list[tuple[str, int, str]]] = {}
    for sid in blocks.SANCTI:
        flex, name, rank, col = sid.split(":")
        if name.endswith("DU"):
            continue
        key = oracle.normalize(sid, datetime.date(2001, int(name[:2]), int(name[3:5])))
        mm_days.setdefault(name[:5], []).append((key, int(rank.rstrip("c")), letters[col[0]]))
    ours = {d["main"]["date"]: d for d in collect(www)}
    for day in sorted(set(mm_days) | set(ours)):
        o = ours.get(day)
        mlist = mm_days.get(day, [])
        okeys = [o["main"]["key"]] + [c["key"] for c in o["commemorations"]] if o else []
        mkeys = [k for k, _, _ in mlist]
        if okeys != mkeys:
            print(f"{day} clés : DO {okeys}  MM {mkeys}")
        if o and mlist and mlist[0][0] == o["main"]["key"]:
            _, mr, mc = mlist[0]
            orank = {"I": 1, "II": 2, "III": 3, "COMMEMORATION": 4}.get(o["main"]["rank"], {"VIGIL_II": 2, "VIGIL_III": 3}.get(o["main"]["rank"]))
            if orank != mr:
                print(f"{day} classe : DO {o['main']['rank']} ({o['main']['dorank']})  MM {mr}  {o['main']['la']}")
            if o["main"]["color"] != mc:
                print(f"{day} couleur : DO {o['main']['color']} ({o['main']['commune']})  MM {mc}  {o['main']['la']}")


def skeleton(www: Path) -> None:
    w = csv.writer(sys.stdout, delimiter="\t", lineterminator="\n")
    w.writerow(["key", "id", "fr", "rank", "la"])
    for day in collect(www):
        for i, item in enumerate([day["main"], *day["commemorations"]]):
            w.writerow([item["key"], "", "", ("" if i == 0 else "comm. ") + item["rank"], item["la"]])


def main(argv: list[str]) -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--divinum", type=Path, required=True)
    ap.add_argument("--missalemeum", type=Path)
    ap.add_argument("--out", type=Path, default=DEFAULT_OUT)
    ap.add_argument("--skeleton", action="store_true")
    args = ap.parse_args(argv)
    www = args.divinum / "web" / "www"
    if args.skeleton:
        skeleton(www)
        return 0
    import subprocess

    def commit(repo: Path) -> str:
        return subprocess.run(["git", "-C", str(repo), "rev-parse", "HEAD"], check=True, capture_output=True, text=True).stdout.strip()

    sources = [f"divinum-officium@{commit(args.divinum)}"]
    if args.missalemeum:  # contrôle des classes, couleurs et commémoraisons ; drapeaux `lord` de 09-14 et 11-09
        sources.append(f"missalemeum@{commit(args.missalemeum)}")
    data = build(www, sources)
    text = json.dumps(data, ensure_ascii=False, indent=2) + "\n"
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(nfc(text), encoding="utf-8")
    n_comm = sum(len(e.get("commemorations", [])) for e in data["entries"])
    print(f"{args.out} : {len(data['entries'])} entrées, {n_comm} commémoraisons attachées")
    if args.missalemeum:
        compare_missalemeum(www, args.missalemeum)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
