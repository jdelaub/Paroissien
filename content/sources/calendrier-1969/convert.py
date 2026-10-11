"""Convertit les sorties brutes de romcal au format v1 du calendrier (content/schema/calendar-v1.md).

    python3 -I content/sources/calendrier-1969/convert.py <dossier brut> [<racine content/>]

Entrées, dans <dossier brut> : romcal-defs.json, romcal-datefns.json, romcal-france-days.jsonl
(generate.mjs) et litcal.json (litcal.py). Sorties, sous <racine content/> (par défaut le
dossier `content/` de ce dépôt) :

    data/calendar/1969/general.json         sanctoral à date fixe du calendrier romain général
    data/calendar/1969/fr.json              propre de France (et patrons de l'Europe), précepte corrigé
    golden/calendar/1969-fr-romcal.jsonl    oracle : romcal, France, une ligne par jour
    golden/calendar/1969-fr-romcal.meta.json
    sources/calendrier-1969/titres-litcal.tsv  titres français de romcal et de LitCal, pour relecture

Le script s'arrête dès qu'une donnée ne correspond pas à ce qu'il attend (forme de date
inconnue, date nominale démentie par romcal, correction de titre qui ne s'applique plus…).
"""

import collections
import datetime
import hashlib
import json
import re
import sys
import unicodedata
from pathlib import Path

HERE = Path(__file__).resolve().parent
CONTENT = HERE.parent.parent

# Fêtes que romcal place par une fonction (utils/dates.ts) mais dont la date est fixe :
# les transferts (Annonciation en Semaine sainte, Immaculée Conception un dimanche) relèvent
# des règles générales du moteur (NU 5 et 60). Vérifiées contre romcal-datefns.json.
NOMINAL = {
    "presentationOfTheLord": "02-02",
    "annunciation": "03-25",
    "nativityOfJohnTheBaptist": "06-24",
    "peterAndPaulApostles": "06-29",
    "transfiguration": "08-06",
    "assumption": "08-15",
    "exaltationOfTheHolyCross": "09-14",
    "allSaints": "11-01",
    "immaculateConceptionOfMary": "12-08",
}
# Fêtes mobiles du calendrier général de romcal : hors fichier, placées par le code.
MOBILE = {"maryMotherOfTheChurch", "immaculateHeartOfMary"}

RANKS = {"SOLEMNITY", "FEAST", "MEMORIAL", "OPTIONAL_MEMORIAL"}
COLORS = {"PURPLE": "VIOLET"}  # romcal → schéma ; les autres noms sont identiques

# Précepte en France (rapport 05, §2.1.1) : Noël, Ascension (mobile, dans le code), Assomption,
# Toussaint. Les drapeaux `isHolyDayOfObligation` de romcal sont ignorés : faux pour la France.
OBLIGATION_FR = {"nativity_of_the_lord", "assumption_of_the_blessed_virgin_mary", "all_saints"}
# Patrons de la France ; les patrons de l'Europe (degré 8c chez romcal) sont PRINCIPAL.
PATRON_FR = {
    "assumption_of_the_blessed_virgin_mary": "PRINCIPAL",
    "joan_of_arc_virgin": "SECONDARY",
    "therese_of_the_child_jesus_and_the_holy_face_of_lisieux_virgin": "SECONDARY",
}
PATRON_PRECEDENCE = {
    "PROPER_FEAST__PRINCIPAL_PATRON_OF_A_REGION_8C": "PRINCIPAL",
    "PROPER_SOLEMNITY__PRINCIPAL_PATRON_4A": "PRINCIPAL",
    "PROPER_MEMORIAL__SECOND_PATRON_11A": "SECONDARY",
}
# Définitions du propre de France non reprises : le schéma n'a pas de solennité facultative.
SKIP_FR = {"dedication_of_consecrated_churches"}

# Corrections des titres français de romcal, après retrait des dates de mort « († …) ».
# Seulement des erreurs objectives (mot anglais, faute, majuscule) ; le reste est signalé
# dans titres-litcal.tsv sans être modifié.
TITLE_FR_FIXES = {
    "michael_gabriel_and_raphael_archangels": [(" and ", " et ")],
    "passion_of_saint_john_the_baptist": [("martyre de ", "Martyre de ")],
    "our_lady_of_guadalupe": [("Guadalupé", "Guadalupe")],
    "margaret_of_scotland": [("d’Ecosse", "d’Écosse")],
    "joseph_spouse_of_mary": [("Époux", "époux")],
    "teresa_benedicta_of_the_cross_stein_virgin": [("martyr en ", "martyre en ")],
}
# « la vierge Marie » → « la Vierge Marie » (Notre-Dame), partout.
VIERGE = re.compile(r"\bvierge Marie\b")

KEY_ORDER = ["id", "date", "rank", "colors", "title", "lord", "validFrom", "validUntil", "proper",
             "commemorations", "saints", "remove", "obligation", "patron"]

fixes_used = collections.Counter()


def nfc(s: str) -> str:
    return unicodedata.normalize("NFC", s)


def title_fr(def_id: str, raw: str) -> str:
    t = re.sub(r"\s*\(†[^()]*\)", "", raw).rstrip(" ,")
    for old, new in TITLE_FR_FIXES.get(def_id, []):
        if old in t:
            t = t.replace(old, new)
            fixes_used[(def_id, old)] += 1
    t, n = VIERGE.subn("Vierge Marie", t)
    fixes_used[("*", "vierge Marie")] += n
    if "†" in t or not t[:1].isupper():
        sys.exit(f"titre inattendu pour {def_id} : {t!r}")
    return nfc(t)


def title(d: dict, fallback_la: str | None = None) -> dict:
    """Titre fr (obligatoire) et la (si la locale latine de romcal le donne, sinon `fallback_la`)."""
    name = d["name"]
    if name["fr"] == name["en"]:
        sys.exit(f"pas de titre français dans romcal pour {d['id']}")
    t = {"fr": title_fr(d["id"], name["fr"])}
    la = name["la"] if name["la"] and name["la"] != name["en"] else fallback_la
    if la:
        t["la"] = nfc(la)
    return t


def date_of(d: dict, datefns: dict) -> str | None:
    dd = d["dateDef"]
    if set(dd) == {"month", "date"}:
        return f"{dd['month']:02d}-{dd['date']:02d}"
    fn = dd.get("dateFn")
    if fn in MOBILE:
        return None
    if fn in NOMINAL and set(dd) == {"dateFn"}:
        seen = collections.Counter(v[5:] for v in datefns[fn].values() if v)
        if seen.most_common(1)[0][0] != NOMINAL[fn]:
            sys.exit(f"{fn} : romcal donne surtout {seen.most_common(1)}, pas {NOMINAL[fn]}")
        return NOMINAL[fn]
    sys.exit(f"date inconnue pour {d['id']} : {dd}")


def colors(cs: list[str]) -> list[str]:
    out = [COLORS.get(c, c) for c in cs]
    if not out:
        sys.exit("couleur absente")
    return out


def entry(**fields) -> dict:
    """Entrée canonique : clés dans l'ordre du schéma, champs nuls, faux ou vides omis."""
    unknown = set(fields) - set(KEY_ORDER)
    if unknown:
        sys.exit(f"champs hors schéma : {unknown}")
    return {k: fields[k] for k in KEY_ORDER if fields.get(k) not in (None, False, [], {}, "")}


def sanctoral(d: dict, date: str, valid_from: dict, fallback_la=None, **extra) -> dict:
    if d["rank"] not in RANKS:
        sys.exit(f"rang inattendu pour {d['id']} : {d['rank']}")
    return entry(
        id=d["id"],
        date=date,
        rank=d["rank"],
        colors=colors(d["colors"]),
        title=title(d, fallback_la),
        lord=d["precedence"] == "GENERAL_LORD_FEAST_5",
        validFrom=valid_from.get(d["id"], {}).get("year"),
        **extra,
    )


def sort_entries(entries: list[dict]) -> list[dict]:
    ids = [e["id"] for e in entries]
    if len(ids) != len(set(ids)):
        sys.exit("identifiant en double")
    return sorted(entries, key=lambda e: (e["date"], e["id"]))


def write_json(path: Path, obj) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main(raw: Path, content: Path) -> None:
    defs = json.loads((raw / "romcal-defs.json").read_text("utf-8"))
    datefns = json.loads((raw / "romcal-datefns.json").read_text("utf-8"))
    litcal = json.loads((raw / "litcal.json").read_text("utf-8"))
    src = defs["source"]
    if litcal["romcal"]["commit"] != src["commit"]:
        sys.exit("litcal.json a été produit avec un autre commit de romcal : relancer litcal.py")
    valid_from = litcal["validFrom"]
    romcal_ref = f"romcal@{src['commit']}"
    litcal_ref = f"LiturgicalCalendarAPI@{litcal['litcal']['commit']}"

    # ── Calendrier romain général ─────────────────────────────────────────
    general = {d["id"]: d for d in defs["general"]}
    gen_entries, mobiles = [], []
    for d in defs["general"]:
        if d["fromCalendarId"] != "general_roman":
            continue
        date = date_of(d, datefns)
        if date is None:
            mobiles.append(d["id"])
            continue
        gen_entries.append(sanctoral(d, date, valid_from))
    gen_entries = sort_entries(gen_entries)
    by_id = {e["id"]: e for e in gen_entries}
    write_json(content / "data/calendar/1969/general.json", {
        "format": 1, "rite": "1969", "calendar": "general",
        "sources": [romcal_ref, litcal_ref], "entries": gen_entries,
    })

    # ── Propre de France (avec la région Europe, parent de France chez romcal) ──
    fr_entries, skipped, unchanged = [], [], []
    for d in defs["france"]:
        layer = d["fromCalendarId"]
        if layer in ("ProperOfTime", "general_roman"):
            continue
        if d["id"] in SKIP_FR:
            skipped.append(d["id"])
            continue
        base = general.get(d["id"])
        date = date_of(d, datefns)
        patron = PATRON_FR.get(d["id"]) or PATRON_PRECEDENCE.get(d["precedence"])
        obligation = d["id"] in OBLIGATION_FR
        if base is not None:
            base_entry = by_id[d["id"]]
            e = sanctoral(d, date, valid_from, base_entry["title"].get("la"),
                          obligation=obligation, patron=patron)
            same = {k: v for k, v in e.items() if k not in ("obligation", "patron")} == base_entry
            if same and not obligation and not patron:
                unchanged.append(d["id"])  # romcal n'y change que le commun ou l'ordre des lectures
                continue
        else:
            e = sanctoral(d, date, valid_from, obligation=obligation, patron=patron)
        fr_entries.append(e)
    # Toussaint : seulement le précepte, au-dessus de l'entrée générale.
    for gid in sorted(OBLIGATION_FR & set(by_id)):
        if gid not in {e["id"] for e in fr_entries}:
            fr_entries.append(entry(**{**by_id[gid], "obligation": True}))
    # Noël appartient au temporal de romcal : l'entrée ne porte que le précepte (voir SOURCES.md).
    nativity = next(d for d in defs["france"] if d["id"] == "nativity_of_the_lord")
    fr_entries.append(entry(
        id=nativity["id"], date="12-25", rank=nativity["rank"], colors=colors(nativity["colors"]),
        title=title(nativity), obligation=True,
    ))
    fr_entries = sort_entries(fr_entries)
    if {e["id"] for e in fr_entries if e.get("obligation")} != OBLIGATION_FR:
        sys.exit("précepte : liste inattendue")
    write_json(content / "data/calendar/1969/fr.json", {
        "format": 1, "rite": "1969", "calendar": "fr",
        "sources": [romcal_ref], "entries": fr_entries,
    })

    missing_fixes = [(i, old) for i, fx in TITLE_FR_FIXES.items() for old, _ in fx if not fixes_used[(i, old)]]
    if missing_fixes:
        sys.exit(f"corrections de titre sans effet (romcal a changé ?) : {missing_fixes}")

    # ── Oracle : romcal France, une ligne par jour ────────────────────────
    days, secondary, years = [], [], set()
    cycles_map = {"YEAR_A": "A", "YEAR_B": "B", "YEAR_C": "C", "YEAR_1": "I", "YEAR_2": "II"}
    with (raw / "romcal-france-days.jsonl").open(encoding="utf-8") as f:
        for line in f:
            day = json.loads(line)
            years.add(int(day["date"][:4]))
            items = day["items"]
            main_item = items[0]
            if main_item["isOptional"]:
                sys.exit(f"{day['date']} : la première célébration est facultative")
            others = [x["id"] for x in items[1:] if not x["isOptional"]]
            if others:
                secondary.append({"date": day["date"], "id": main_item["id"], "also": others})
            c = main_item["cycles"]
            days.append(json.dumps({
                "date": day["date"],
                "id": main_item["id"],
                "rank": main_item["rank"],
                "colors": [COLORS.get(x, x) for x in main_item["colors"]],
                "optional": [x["id"] for x in items if x["isOptional"]],
                "sundayCycle": cycles_map[c["sundayCycle"]],
                "weekdayCycle": cycles_map[c["weekdayCycle"]],
                "psalterWeek": int(c["psalterWeek"].removeprefix("WEEK_")),
            }, ensure_ascii=False, separators=(",", ":")))
    anomalies = oracle_anomalies(days, gen_entries, fr_entries)
    golden = content / "golden/calendar/1969-fr-romcal.jsonl"
    golden.parent.mkdir(parents=True, exist_ok=True)
    golden.write_text("\n".join(days) + "\n", encoding="utf-8")
    first, last = min(years), max(years)
    holy_thursday = [s for s in secondary if s["id"] == "holy_thursday" and s["also"] == ["thursday_of_the_lords_supper"]]
    others = [s for s in secondary if s not in holy_thursday]
    cfg = defs["config"]["france"]
    write_json(golden.with_suffix(".meta.json"), {
        "file": golden.name,
        "engine": "romcal",
        "repository": "https://github.com/romcal/romcal",
        "license": "MIT",
        "branch": src["branch"],
        "version": src["version"],
        "commit": src["commit"],
        "commitDate": src["commitDate"],
        "node": src["node"],
        "calendar": "france (calendriers parents : europe, general_roman), locale fr",
        "config": {k: cfg[k] for k in ("scope", "epiphanyOnSunday", "ascensionOnSunday", "corpusChristiOnSunday",
                                       "easterCalculationType", "elevatedMemorialIds")},
        "years": [first, last],
        "days": len(days),
        "sha256": hashlib.sha256(golden.read_bytes()).hexdigest(),
        "generated": datetime.date.today().isoformat(),
        "commands": [
            "git clone --branch dev https://github.com/romcal/romcal.git && cd romcal && git checkout " + src["commit"],
            "npm ci && npm run build",
            f"node content/sources/calendrier-1969/generate.mjs <romcal> content/.cache/raw/calendrier-1969 {first} {last}",
            "python3 -I content/sources/calendrier-1969/litcal.py <LiturgicalCalendarAPI> content/.cache/raw/calendrier-1969",
            "python3 -I content/sources/calendrier-1969/convert.py content/.cache/raw/calendrier-1969",
        ],
        "fields": {
            "id": "identifiant romcal de la première célébration du jour (la célébration principale)",
            "rank": "rang romcal de cette célébration (SOLEMNITY, SUNDAY, FEAST, MEMORIAL, OPTIONAL_MEMORIAL, WEEKDAY)",
            "colors": "couleurs romcal, PURPLE écrit VIOLET ; vide le Samedi saint (romcal n'en donne pas)",
            "optional": "célébrations marquées isOptional par romcal : mémoires facultatives, mémoires obligatoires "
                        "réduites en Carême, et la solennité facultative "
                        "dedication_of_consecrated_churches (25 octobre)",
            "sundayCycle, weekdayCycle, psalterWeek": "cycles romcal de la célébration principale",
        },
        "notes": [
            "Précepte : non repris (drapeaux de romcal faux pour la France).",
            "Newman (john_henry_newman_priest) figure aussi en 2025 : romcal ignore la date d'inscription (2026).",
            "Jeudi saint : romcal donne holy_thursday puis thursday_of_the_lords_supper, toutes deux non "
            f"facultatives ; seule la première est gardée ({len(holy_thursday)} jours).",
            "Autres jours à deux célébrations non facultatives (Cœur immaculé de Marie et une mémoire "
            "obligatoire, allowSimilarRankItems) ; seule la première est gardée : "
            + ", ".join(f"{s['date']} {'+'.join(s['also'])}" for s in others) + ".",
            *anomalies,
        ],
    })

    # ── Titres à relire : romcal (après corrections) en regard de LitCal ────
    rows = ["id\tdate\tromcal\tlitcal"]
    for t in litcal["titles"]:
        ours = by_id[t["id"]]["title"]["fr"]
        if _norm(ours) != _norm(t["litcalFr"]):
            rows.append(f"{t['id']}\t{t['date']}\t{ours}\t{nfc(t['litcalFr'])}")
    (HERE / "titres-litcal.tsv").write_text("\n".join(rows) + "\n", encoding="utf-8")

    print(f"general.json : {len(gen_entries)} entrées ; mobiles laissées au code : {', '.join(mobiles)}")
    print(f"fr.json : {len(fr_entries)} entrées ; non reprises : {', '.join(skipped)} ; "
          f"sans changement visible : {', '.join(unchanged)}")
    print(f"oracle : {len(days)} jours ({first}-{last}), {len(secondary)} jours à deux célébrations non facultatives")
    print(f"corrections de titres : {sum(fixes_used.values())} ; titres différents de LitCal : {len(rows) - 1}")
    print("anomalies de l'oracle :", *anomalies, sep="\n  ")


def oracle_anomalies(days: list[str], general: list[dict], national: list[dict]) -> list[str]:
    """Défauts de romcal repérés dans l'oracle : solennité absente d'une année entière, ou fête et
    solennité évincées par le Cœur immaculé de Marie (mémoire avec allowSimilarRankItems)."""
    merged = {e["id"]: e for e in general}
    merged.update({e["id"]: e for e in national if not e.get("remove")})
    seen = collections.defaultdict(set)
    principal = {}
    for line in days:
        d = json.loads(line)
        principal[d["date"]] = d["id"]
        for i in [d["id"], *d["optional"]]:
            seen[i].add(int(d["date"][:4]))
    years = {int(k[:4]) for k in principal}
    out = []
    for e in sorted(merged.values(), key=lambda e: (e["date"], e["id"])):
        if e["rank"] != "SOLEMNITY" or e["id"] == "nativity_of_the_lord":
            continue
        for y in sorted(years - seen[e["id"]] - set(range(0, e.get("validFrom") or 0))):
            out.append(f"Anomalie romcal : {e['id']} absente en {y} (date nominale {y}-{e['date']}).")
    for date, i in sorted(principal.items()):
        if i != "immaculate_heart_of_mary":
            continue
        for e in merged.values():
            if e["date"] == date[5:] and e["rank"] in ("SOLEMNITY", "FEAST"):
                out.append(f"Anomalie romcal : le {date}, le Cœur immaculé de Marie (mémoire) évince {e['id']} ({e['rank']}).")
    return out


def _norm(s: str) -> str:
    s = unicodedata.normalize("NFKD", s.replace("’", "'")).lower()
    return re.sub(r"[^a-z]+", " ", "".join(c for c in s if not unicodedata.combining(c))).strip()


if __name__ == "__main__":
    if len(sys.argv) not in (2, 3):
        sys.exit(__doc__)
    main(Path(sys.argv[1]), Path(sys.argv[2]) if len(sys.argv) == 3 else CONTENT)
