#!/usr/bin/env python3
"""Contrôles propres à l'Imitation, en plus de « pqt validate » : structure, restes de balisage, typographie.

    python3 -I content/sources/imitation/check.py

Lit content/data/imitation.fr et imitation.la ; affiche les écarts et sort en erreur s'il y en a.
"""

from __future__ import annotations

import csv
import json
import re
import sys
from collections import Counter
from pathlib import Path

DATA = Path(__file__).resolve().parents[2] / "data"
CHAPTERS = (25, 12, 59, 18)
NNBSP = "\u202f"


def plain(nodes: list) -> str:
    out = []
    for n in nodes:
        if isinstance(n, str):
            out.append(n)
        elif "c" in n:
            out.append(plain(n["c"]))
        elif "br" in n:
            out.append("\n")
    return "".join(out)


def strings(nodes: list):
    for n in nodes:
        if isinstance(n, str):
            yield n
        elif "c" in n:
            yield from strings(n["c"])


def load(pub: str) -> dict[str, list[dict]]:
    meta = json.loads((DATA / pub / "publication.json").read_text(encoding="utf-8"))
    return {d["id"]: [json.loads(line) for line in (DATA / pub / "blocks" / f"{d['id']}.jsonl").read_text(encoding="utf-8").splitlines()]
            for d in meta["divisions"]}


FR_RULES = [
    (re.compile(r"\[\d+\]"), "appel de note resté dans le texte"),
    (re.compile(r"_"), "souligné resté dans le texte"),
    (re.compile(r"'"), "apostrophe droite"),
    (re.compile(r"(?i)(?<![^\W\d_])[^\W\d_]*oe[^\W\d_]*"), "« oe » sans ligature"),
    (re.compile(r"(?<![\u202f;:!?])[;:!?]"), "ponctuation haute sans espace fine insécable avant"),
    (re.compile(r"(?<!\u202f)»"), "» sans espace fine insécable avant"),
    (re.compile(r"«[^\u202f]"), "« sans espace fine insécable après"),
    (re.compile(r"\s[.,]"), "espace avant . ou ,"),
    (re.compile(r"[ \u00a0]\u202f|\u202f[ \u00a0]|  "), "double espace"),
    (re.compile(r"[\u00a0]"), "espace insécable ordinaire"),
    (re.compile(r"\.\.\."), "points de suspension en trois points"),
]
LA_RULES = [
    (re.compile(r"\[\d+\]"), "appel de note resté dans le texte"),
    (re.compile(r"[_<>&]"), "balisage resté dans le texte"),
    (re.compile(r"\s[.,;:?!]"), "espace avant la ponctuation"),
    (re.compile(r"  "), "double espace"),
    (re.compile(r"\d"), "chiffre dans le texte"),
]


def check(pub: str, lang: str, rules) -> list[str]:
    errors = []
    units = load(pub)
    if list(units) != ["l1", "l2", "l3", "l4"]:
        errors.append(f"{pub} : unités {list(units)}")
    for (unit, blocks), expected in zip(units.items(), CHAPTERS):
        chapters = [b for b in blocks if b["type"] == "heading" and b.get("level") == 1 and b["id"].count(".") == 1
                    and re.fullmatch(r"l\d\.c\d+", b["id"])]
        if len(chapters) != expected:
            errors.append(f"{pub}/{unit} : {len(chapters)} chapitres au lieu de {expected}")
        for i, ch in enumerate(chapters, start=1):
            if ch["id"] != f"{unit}.c{i}":
                errors.append(f"{pub}/{unit} : chapitre {ch['id']} à la place {i}")
        firsts = Counter()
        current = None
        for b in blocks:
            if b["type"] == "heading" and b.get("level") == 1:
                current = b["id"]
            elif b["type"] == "para" and b.get("style") is None and current:
                firsts[current] += 1
                if (firsts[current] == 1) != bool(b.get("dropCap")):
                    errors.append(f"{b['id']} : lettrine attendue sur le seul premier paragraphe du chapitre")
            for s in strings(b["t"][lang]):
                for rule, label in rules:
                    for m in rule.finditer(s):
                        errors.append(f"{b['id']} : {label} : …{s[max(0, m.start() - 20):m.end() + 20]}…")
    return errors


def readme_lists_corrections() -> list[str]:
    """Chaque correction du latin doit figurer dans le README (bloc, avant, après)."""
    here = Path(__file__).resolve().parent
    readme = (here / "README.md").read_text(encoding="utf-8")
    rows = list(csv.DictReader((here / "corrections-la.tsv").open(encoding="utf-8"), delimiter="\t"))
    return [f"README : correction absente {r['id']} {r['avant']!r}" for r in rows
            if f"| `{r['id']}` |" not in readme or f"`{r['avant']}` | `{r['après']}` | {r['n']} |" not in readme]


def main() -> int:
    errors = check("imitation.fr", "fr", FR_RULES) + check("imitation.la", "la", LA_RULES) + readme_lists_corrections()
    for e in errors[:200]:
        print(e)
    print(f"{len(errors)} écart(s)")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
