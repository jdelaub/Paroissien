#!/usr/bin/env python3
"""Contrôle des Bibles converties : livres, chapitres, versets vides ou « géants », numérotation, et écarts
avec la versification SWORD correspondante (Catholic2 pour le Crampon, Vulg pour la Vulgate).

    python3 -I content/sources/bible/check.py [bible.crampon] [bible.vulgata]

Les tables SWORD (GPL-2) servent seulement de témoin, lues dans le cache ; rien n'en est recopié.
Un écart n'est pas une erreur : SWORD donne le maximum de chaque chapitre pour toutes les éditions.
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import books  # noqa: E402
import common  # noqa: E402

REFERENCE = {"bible.crampon": "canon_catholic2.h", "bible.vulgata": "canon_vulg.h"}
NT_NAMES = ["Matthew", "Mark", "Luke", "John", "Acts", "Romans", "I Corinthians", "II Corinthians", "Galatians",
            "Ephesians", "Philippians", "Colossians", "I Thessalonians", "II Thessalonians", "I Timothy", "II Timothy",
            "Titus", "Philemon", "Hebrews", "James", "I Peter", "II Peter", "I John", "II John", "III John", "Jude",
            "Revelation of John"]
NT_OSIS = ["Matt", "Mark", "Luke", "John", "Acts", "Rom", "1Cor", "2Cor", "Gal", "Eph", "Phil", "Col", "1Thess", "2Thess",
           "1Tim", "2Tim", "Titus", "Phlm", "Heb", "Jas", "1Pet", "2Pet", "1John", "2John", "3John", "Jude", "Rev"]


def reference(header: str) -> dict[str, list[int]]:
    """{code USFM: [versets max par chapitre]} d'après un fichier canon_*.h (lu comme texte)."""
    text = (common.RAW / header).read_text(encoding="utf-8")
    names = dict(zip(NT_NAMES, NT_OSIS))
    for name, osis in re.findall(r'\{"([^"]+)", "([^"]+)", "[^"]+", \d+\}', text):
        names[name] = osis
    vm = text[text.index("int vm_"):]
    vm = vm[:vm.index("};")]
    out: dict[str, list[int]] = {}
    for name, numbers in re.findall(r"//\s*([^\n]+)\n((?:[\s\d,]+))", vm):
        osis = names.get(name.strip())
        if osis in books.OSIS_TO_USFM:
            out[books.OSIS_TO_USFM[osis]] = [int(n) for n in re.findall(r"\d+", numbers)]
    return out


def check(pub_id: str) -> int:
    root = common.DATA / pub_id
    meta = json.loads((root / "publication.json").read_text(encoding="utf-8"))
    units = [d["id"] for t in meta["divisions"] for d in t["children"]]
    ref = reference(REFERENCE[pub_id])
    lang = meta["primaryLanguage"]
    verses = notes = conts = 0
    empty: list[str] = []
    giants: list[str] = []
    numbering: list[str] = []
    missing: list[str] = []
    diffs: list[str] = []
    longest = ("", 0)
    for unit in units:
        chapters: dict[int, list[int]] = {}
        headings: set[int] = set()
        for line in (root / "blocks" / f"{unit}.jsonl").open(encoding="utf-8"):
            b = json.loads(line)
            parts = b["id"].split(".")
            if b["type"] == "heading" and b.get("level") == 2:
                headings.add(int(parts[1]))
            if b["type"] == "note":
                notes += 1
            if b["type"] != "verse":
                continue
            if not b["n"].isdigit():
                conts += 1
                continue
            verses += 1
            chapters.setdefault(int(parts[1]), []).append(int(b["n"]))
            text = common.plain(b["t"][lang])
            if not text.strip():
                empty.append(b["id"])
            if len(text) > common.GIANT:
                giants.append(f"{b['id']} ({len(text)})")
            if len(text) > longest[1]:
                longest = (b["id"], len(text))
        code = unit.upper()
        expected = ref.get(code, [])
        if sorted(chapters) != list(range(1, len(chapters) + 1)) or headings != set(chapters):
            missing.append(f"{unit} : chapitres {sorted(chapters)[:3]}…, titres {len(headings)}")
        if len(chapters) != len(expected):
            missing.append(f"{unit} : {len(chapters)} chapitres, SWORD {len(expected)}")
        for ch, ns in sorted(chapters.items()):
            if ns != list(range(1, len(ns) + 1)):
                numbering.append(f"{unit}.{ch} : {ns[:5]}… ({len(ns)})")
            if ch <= len(expected) and len(ns) != expected[ch - 1]:
                diffs.append(f"{unit} {ch} : {len(ns)} (SWORD {expected[ch - 1]})")
    print(f"{pub_id} : {len(units)} livres, {verses} versets (+ {conts} suites), {notes} notes")
    print(f"  versets vides : {len(empty)} {empty[:10]}")
    print(f"  versets de plus de {common.GIANT} caractères : {len(giants)} {giants[:10]} ; le plus long : {longest[0]} ({longest[1]})")
    print(f"  numérotation non continue : {len(numbering)} {numbering[:10]}")
    print(f"  chapitres manquants ou en trop : {len(missing)} {missing[:10]}")
    print(f"  chapitres dont le nombre de versets diffère de SWORD : {len(diffs)}")
    for d in diffs:
        print(f"    {d}")
    return 1 if empty or giants or numbering or missing or len(units) != 73 else 0


def main(argv: list[str]) -> int:
    return max(check(p) for p in (argv or list(REFERENCE)))


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
