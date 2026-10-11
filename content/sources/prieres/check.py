#!/usr/bin/env python3
"""Collation des transcriptions (texte/*.txt) avec l'OCR d'archive.org des pages citées.

Pour chaque bloc et chaque langue, aligne les mots de la transcription sur les mots OCR des pages données
par « src » et liste les mots de la transcription que l'OCR ne confirme pas. Ces écarts sont soit des fautes
d'OCR corrigées sur le fac-similé, soit des corrections consignées dans le README : rien d'autre ne doit
apparaître. Bibliothèque standard ; python3 -I.

    python3 -I content/sources/prieres/check.py            résumé par prière
    python3 -I content/sources/prieres/check.py -v         détail des mots non confirmés
    python3 -I content/sources/prieres/check.py --tsv F    écrit le détail dans F (relecture)
"""

from __future__ import annotations

import difflib
import re
import sys
import unicodedata
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))

import normalize  # noqa: E402
import ocr  # noqa: E402

FOLD = str.maketrans({"æ": "ae", "œ": "oe", "Æ": "ae", "Œ": "oe", "’": "'", "ſ": "s"})


def words(text: str) -> list[str]:
    text = text.translate(FOLD).lower()
    text = re.sub(r"(\w)- (\w)", r"\1\2", text)
    text = unicodedata.normalize("NFD", text)
    text = "".join(c for c in text if unicodedata.category(c) != "Mn")
    return re.findall(r"[a-z0-9]+", text)


def ocr_words(src: dict) -> list[str]:
    first, _, last = src["images"].partition("-")
    lo, hi = int(first), int(last or first)
    off = ocr.LEAF_OFFSET.get(src["archive"], 0)
    pages = ocr.pages(src["archive"])
    return words(" ".join(ocr.glue(pages[i + off]) for i in range(lo, hi + 1)))


def collate() -> list[tuple]:
    rows = []
    for unit, _ in normalize.UNITS:
        for p in normalize.parse(HERE / "texte" / f"{unit}.txt"):
            for lang in ("la", "fr"):
                srcs = [s for s in p["src"] if lang in s["lang"]]
                if not srcs:
                    continue
                page_words = [w for s in srcs for w in ocr_words(s)]
                for n, b in enumerate(p["blocks"], 1):
                    text = b["t"].get(lang)
                    if not text:
                        continue
                    mine = words(re.sub(r"[℣℟¶*/_]", " ", text))
                    sm = difflib.SequenceMatcher(None, mine, page_words, autojunk=False)
                    ok = set()
                    for a, _b, size in sm.get_matching_blocks():
                        ok.update(range(a, a + size))
                    missing = [mine[i] for i in range(len(mine)) if i not in ok]
                    rows.append((p["key"], f"{p['key']}.{n}", lang, len(mine), missing))
    return rows


def main(argv: list[str]) -> int:
    rows = collate()
    total = sum(r[3] for r in rows)
    miss = sum(len(r[4]) for r in rows)
    by_prayer: dict[str, list[int]] = {}
    for key, _bid, _lang, n, missing in rows:
        acc = by_prayer.setdefault(key, [0, 0])
        acc[0] += n
        acc[1] += len(missing)
    for key, (n, m) in by_prayer.items():
        print(f"{key:28} {n:5} mots, {m:4} non confirmés par l'OCR ({100 * (n - m) / max(n, 1):.1f} % confirmés)")
    print(f"TOTAL {total} mots, {miss} non confirmés ({100 * (total - miss) / max(total, 1):.1f} % confirmés)")
    detail = [f"{bid}\t{lang}\t{' '.join(missing)}" for _k, bid, lang, _n, missing in rows if missing]
    if "-v" in argv:
        print("\n".join(detail))
    if "--tsv" in argv:
        out = Path(argv[argv.index("--tsv") + 1])
        out.write_text("bloc\tlangue\tmots non confirmés par l'OCR (forme pliée)\n" + "\n".join(detail) + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
