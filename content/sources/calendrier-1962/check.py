#!/usr/bin/env python3
"""Vérifie les livrables du calendrier de 1962 contre Divinum Officium.

    python3 -I content/sources/calendrier-1962/check.py --divinum <clone>/divinum-officium

1. `content/data/calendar/1962/general.json` : forme canonique (ordre des clés, champs vides omis,
   NFC, tri), valeurs autorisées, identifiants uniques, clés `proper` présentes dans `missa/Latin/`.
2. `content/data/calendar/1962/TEMPORAL.md` : chaque clé citée entre accents graves existe dans DO
   (notation `{04..23}` et `{a,b}` développée).
3. `content/golden/calendar/1962-missalemeum.jsonl` : chaque clé de l'oracle est couverte par
   general.json, TEMPORAL.md ou la liste des clés propres à Missalemeum (README).
Code de sortie 1 au premier type d'erreur rencontré ; les écarts sont listés.
"""

from __future__ import annotations

import argparse
import itertools
import json
import re
import sys
import unicodedata
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
GENERAL = ROOT / "content" / "data" / "calendar" / "1962" / "general.json"
TEMPORAL = ROOT / "content" / "data" / "calendar" / "1962" / "TEMPORAL.md"
ORACLE = ROOT / "content" / "golden" / "calendar" / "1962-missalemeum.jsonl"

# Clés de l'oracle qui n'ont pas de fichier dans DO (README, § Oracle).
MISSALEMEUM_ONLY = {
    "Sancti/01-25c", "Sancti/02-22c",                    # S. Pierre / S. Paul (n. 110), dans le propre chez DO
    "Sancti/12-26c", "Sancti/12-27c", "Sancti/12-28c",   # commémoraison de l'octave de Noël
    "Tempora/Quad5-5Feriac",                             # Sept Douleurs le vendredi de la Passion
}

ENTRY_KEYS = ["id", "date", "rank", "colors", "title", "lord", "validFrom", "validUntil", "proper", "commemorations", "saints"]
RANKS = {"I", "II", "III", "IV", "COMMEMORATION", "VIGIL_I", "VIGIL_II", "VIGIL_III"}
COLORS = {"WHITE", "RED", "GREEN", "VIOLET", "ROSE", "BLACK", "GOLD"}
KEY = re.compile(r"`((?:Tempora|Sancti|Commune)/[^`\s]+)`")


def exists(www: Path, key: str) -> bool:
    folder, _, name = key.partition("/")
    if (www / "missa" / "Latin" / folder / f"{name}.txt").exists():
        return True
    # La messe lit les communs dans horas/ (SetupString.pl, setupstring_layers).
    return folder == "Commune" and (www / "horas" / "Latin" / folder / f"{name}.txt").exists()


def expand(pattern: str) -> list[str]:
    """`Tempora/Pent{04..06}-{1,2}` → Pent04-1, Pent04-2, Pent05-1… ; `{1..4,6}` → 1, 2, 3, 4, 6."""
    parts = re.split(r"(\{[^}]+\})", pattern)
    choices = []
    for part in parts:
        if part.startswith("{") and part.endswith("}"):
            items = []
            for item in part[1:-1].split(","):
                m = re.fullmatch(r"(\d+)\.\.(\d+)", item)
                if m:
                    a, b = m.group(1), m.group(2)
                    items += [str(i).zfill(len(a)) for i in range(int(a), int(b) + 1)]
                else:
                    items.append(item)
            choices.append(items)
        else:
            choices.append([part])
    return ["".join(c) for c in itertools.product(*choices)]


def check_general(www: Path, errors: list[str]) -> set[str]:
    raw = GENERAL.read_text(encoding="utf-8")
    if unicodedata.normalize("NFC", raw) != raw:
        errors.append("general.json n'est pas en NFC")
    data = json.loads(raw)
    if json.dumps(data, ensure_ascii=False, indent=2) + "\n" != raw:
        errors.append("general.json : sérialisation non canonique (indentation 2, UTF-8)")
    if list(data) != ["format", "rite", "calendar", "sources", "entries"] or data["rite"] != "1962":
        errors.append("general.json : en-tête inattendu")
    keys, ids = set(), set()
    previous = ("", "")
    for e in data["entries"]:
        order = [k for k in ENTRY_KEYS if k in e]
        if list(e) != order:
            errors.append(f"{e.get('id')} : ordre des clés {list(e)}")
        for k, v in e.items():
            if v in (None, False, [], {}, ""):
                errors.append(f"{e['id']} : champ vide {k}")
        if not re.fullmatch(r"[a-z0-9_]+", e["id"]) or e["id"] in ids:
            errors.append(f"identifiant invalide ou en double : {e['id']}")
        ids.add(e["id"])
        if not re.fullmatch(r"(0[1-9]|1[0-2])-(0[1-9]|[12]\d|3[01])", e["date"]):
            errors.append(f"{e['id']} : date {e['date']}")
        if (e["date"], e["id"]) < previous:
            errors.append(f"{e['id']} : tri par date puis id")
        previous = (e["date"], e["id"])
        if e["rank"] not in RANKS:
            errors.append(f"{e['id']} : rang {e['rank']}")
        if not e["colors"] or set(e["colors"]) - COLORS:
            errors.append(f"{e['id']} : couleurs {e['colors']}")
        if not e["title"].get("fr"):
            errors.append(f"{e['id']} : titre français manquant")
        for item in [e, *e.get("commemorations", [])]:
            if "proper" in item:
                keys.add(item["proper"])
                if not exists(www, item["proper"]):
                    errors.append(f"{item['id']} : {item['proper']} absent de DO")
            if list(item["title"]) not in (["fr", "la"], ["fr"]):
                errors.append(f"{item['id']} : titre {list(item['title'])}")
    return keys


def check_temporal(www: Path, errors: list[str]) -> set[str]:
    keys = set()
    for pattern in KEY.findall(TEMPORAL.read_text(encoding="utf-8")):
        if "…" in pattern:
            continue  # mention générique (« `Commune/…` »), pas une clé
        for key in expand(pattern):
            keys.add(key)
            if not exists(www, key):
                errors.append(f"TEMPORAL.md : {key} absent de DO")
    return keys


def check_oracle(covered: set[str], errors: list[str]) -> None:
    seen: dict[str, int] = {}
    for line in ORACLE.read_text(encoding="utf-8").splitlines():
        day = json.loads(line)
        for key in [day["proper"], *day["commemorations"]]:
            seen[key] = seen.get(key, 0) + 1
    for key in sorted(set(seen) - covered - MISSALEMEUM_ONLY):
        errors.append(f"oracle : {key} ({seen[key]} jours) ni dans general.json, ni dans TEMPORAL.md")


def main(argv: list[str]) -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--divinum", type=Path, required=True)
    args = ap.parse_args(argv)
    www = args.divinum / "web" / "www"
    errors: list[str] = []
    covered = check_general(www, errors)
    covered |= check_temporal(www, errors)
    check_oracle(covered, errors)
    for e in errors:
        print(e)
    print(f"{len(errors)} erreur(s)")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
