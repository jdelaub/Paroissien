#!/usr/bin/env python3
"""Oracle 1962 : exécute le calendrier de Missalemeum (MIT) et écrit un JSONL normalisé.

    <venv>/bin/python -I content/sources/calendrier-1962/oracle.py \\
        --missalemeum <clone>/Missalemeum --divinum <clone>/divinum-officium \\
        --from 2020 --to 2045 --out content/golden/calendar/1962-missalemeum.jsonl

Le venv ne contient que les deux dépendances du module `api.kalendar` de Missalemeum
(python-dateutil, PyYAML). `-I` : ni PYTHONPATH ni dossier courant ; le dossier `backend/`
de Missalemeum est ajouté explicitement à sys.path, c'est le seul code tiers exécuté.

Une ligne par jour :
    {"date":"2026-10-11","proper":"Tempora/Pent19-0","rank":"II","colors":["GREEN"],"commemorations":[]}

`proper` et `commemorations` sont des clés de propre de Divinum Officium (`Tempora/…`, `Sancti/…`,
`Commune/…`), obtenues par `normalize()` ; la correspondance est décrite dans le README.
"""

from __future__ import annotations

import argparse
import datetime
import hashlib
import json
import re
import subprocess
import sys
from pathlib import Path

COLORS = {"w": "WHITE", "r": "RED", "g": "GREEN", "v": "VIOLET", "p": "ROSE", "b": "BLACK"}
RANKS = {1: "I", 2: "II", 3: "III", 4: "IV"}

# Noms de Missalemeum qui désignent un autre fichier de Divinum Officium (même célébration).
RENAMES = {
    "sancti:01-05": "Sancti/01-05cc",    # S. Télesphore (Kalendarium 1955 : 01-05cc)
    "sancti:05-31c": "Sancti/05-31o",    # S. Pétronille
    "sancti:07-18cc": "Sancti/07-18o",   # Ste Symphorose et ses fils
    "sancti:08-08": "Sancti/08-09",      # S. Jean-Marie Vianney : fichier 08-09 dans DO (Kalendarium 1960 : 08-09~08-08)
    "sancti:08-08c": "Sancti/08-08",     # Ss. Cyriaque, Large et Smaragde
    "sancti:10-08c": "Sancti/10-07cc",   # Ss. Serge, Bacchus, Marcel et Apulée
    "sancti:10-DU": "Sancti/10-DU",      # Christ-Roi (fête mobile, clé de DO inchangée)
    "sancti:11-02m1": "Sancti/11-02",    # Défunts : les trois messes m1, m2, m3 → jour 11-02
    "sancti:11-02m2": "Sancti/11-02",
    "sancti:11-02m3": "Sancti/11-02",
    "sancti:12-25m1": "Sancti/12-25",    # Noël : les trois messes → jour 12-25
    "sancti:12-25m2": "Sancti/12-25",
    "sancti:12-25m3": "Sancti/12-25",
    "sancti:12-28": "Sancti/12-28r",     # Kalendarium 1960 : 12-28r, 12-29r, 12-31r
    "sancti:12-29": "Sancti/12-29r",
    "sancti:12-31": "Sancti/12-31r",
    "commune:C10t": "Commune/C10",       # Ste Vierge le samedi, de la Trinité à l'Avent
}


def normalize(observance_id: str, date: datetime.date, subs: dict[str, str] | None = None) -> str:
    """`sancti:10-11:2:w` → `Sancti/10-11` ; `tempora:Pent19-0:2:g` → `Tempora/Pent19-0`.

    `subs` : substitutions du temporal de DO pour 1960 (`divinum.tempora_substitutions`), p. ex.
    `Tempora/Pasc5-1` → `Tempora/Pasc5-1r` ; Missalemeum les applique déjà presque toutes."""
    flexibility, name = observance_id.split(":")[:2]
    key = f"{flexibility}:{name}"
    if key in RENAMES:
        return RENAMES[key]
    if key == "tempora:Nat1-1":           # férie dans l'octave de Noël → Tempora/Nat29, Nat30, Nat31
        return f"Tempora/Nat{date.day:02d}"
    if flexibility == "tempora" and re.fullmatch(r"Epi[3-6]-[0-6]", name) and date.month >= 10:
        return f"Tempora/PentEpi{name[3:]}"  # dimanches après l'Épiphanie repris (n. 18) : DO Date.pm, getweek()
    if flexibility == "" and name == "feria":
        raise ValueError("férie sans temporal : à traiter par l'appelant")
    out = {"sancti": "Sancti", "tempora": "Tempora", "commune": "Commune"}[flexibility] + "/" + name
    return (subs or {}).get(out, out)


def feria_without_tempora(date: datetime.date) -> str:
    """Jours sans temporal chez Missalemeum : 2-5 janvier (Tempora/Nat02…) et féries après
    l'Épiphanie avant la Sainte Famille (Sancti/01-07… dans le Kalendarium de DO)."""
    if date.month == 1 and 2 <= date.day <= 5:
        return f"Tempora/Nat{date.day:02d}"
    if date.month == 1 and 7 <= date.day <= 12:
        return f"Sancti/01-{date.day:02d}"
    raise ValueError(f"{date} : férie sans temporal inattendue")


def git_commit(path: Path) -> str:
    return subprocess.run(["git", "-C", str(path), "rev-parse", "HEAD"], check=True, capture_output=True, text=True).stdout.strip()


def main(argv: list[str]) -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--missalemeum", type=Path, required=True, help="clone de mmolenda/Missalemeum")
    ap.add_argument("--divinum", type=Path, help="clone de Divinum Officium, pour vérifier les clés")
    ap.add_argument("--from", dest="first", type=int, default=2020)
    ap.add_argument("--to", dest="last", type=int, default=2045)
    ap.add_argument("--out", type=Path, required=True)
    args = ap.parse_args(argv)

    subs = {}
    if args.divinum:
        sys.path.insert(0, str(Path(__file__).resolve().parent))
        import divinum  # noqa: E402
        subs = divinum.tempora_substitutions(args.divinum / "web" / "www")

    backend = (args.missalemeum / "backend").resolve()
    sys.path.insert(0, str(backend))
    from api.kalendar.factory import MissalFactory  # noqa: E402  (code de Missalemeum, l'oracle)

    lines = []
    keys: dict[str, int] = {}
    for year in range(args.first, args.last + 1):
        calendar = MissalFactory().create(year, "la")
        for date, day in calendar.items():
            main_obs = day.celebration[0]
            if main_obs.id.startswith(":feria:"):
                proper = normalize(day.tempora[0].id, date, subs) if day.tempora else feria_without_tempora(date)
            else:
                proper = normalize(main_obs.id, date, subs)
            commemorations = []
            for c in day.get_commemorations():
                key = normalize(c.id, date, subs)
                if key != proper and key not in commemorations:
                    commemorations.append(key)
            for k in [proper, *commemorations]:
                keys[k] = keys.get(k, 0) + 1
            lines.append(json.dumps({
                "date": date.isoformat(),
                "proper": proper,
                "rank": RANKS[main_obs.rank],
                "colors": [COLORS[c] for c in main_obs.colors],
                "commemorations": commemorations,
            }, ensure_ascii=False, separators=(",", ":")))

    args.out.parent.mkdir(parents=True, exist_ok=True)
    body = "\n".join(lines) + "\n"
    args.out.write_text(body, encoding="utf-8")

    missing = []
    if args.divinum:
        www = args.divinum / "web" / "www"
        for k in sorted(keys):
            folder, _, name = k.partition("/")
            candidates = [www / "missa" / "Latin" / folder / f"{name}.txt", www / "horas" / "Latin" / folder / f"{name}.txt"]
            if not any(p.exists() for p in candidates):
                missing.append(k)

    meta = {
        "engine": "Missalemeum (mmolenda/Missalemeum, MIT), module backend/api/kalendar, langue « la »",
        "commit": git_commit(args.missalemeum),
        "divinumOfficiumCommit": git_commit(args.divinum) if args.divinum else None,
        "command": "python3 -I content/sources/calendrier-1962/oracle.py --missalemeum <clone> --divinum <clone> "
                   f"--from {args.first} --to {args.last} --out {args.out.as_posix()}",
        "python": sys.version.split()[0],
        "generated": datetime.date.today().isoformat(),
        "years": [args.first, args.last],
        "days": len(lines),
        "sha256": hashlib.sha256(body.encode("utf-8")).hexdigest(),
        "normalization": "content/sources/calendrier-1962/README.md § Oracle",
        "keysWithoutDivinumOfficiumFile": missing,
    }
    meta_path = args.out.with_name(args.out.name.removesuffix(".jsonl") + ".meta.json")
    meta_path.write_text(json.dumps(meta, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"{args.out} : {len(lines)} jours, {len(keys)} clés distinctes")
    if missing:
        print("clés sans fichier dans Divinum Officium :", ", ".join(missing))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
