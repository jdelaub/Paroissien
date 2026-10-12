#!/usr/bin/env python3
"""Collation des transcriptions (texte/*.txt) avec leurs sources (bibliothèque standard ; python3 -I).

- Latin : mots de chaque bloc cherchés dans les lignes de Divinum Officium données par « src: do » (cache de fetch.py).
- Français : mots cherchés dans l'OCR archive.org des pages données par « src » et « wit » (chaîne des prières).

Un mot non confirmé est une faute d'OCR corrigée sur le fac-similé, une correction ou un ajustement consigné dans le
README, ou un texte composé (bloc « review ») : rien d'autre ne doit apparaître. Les rubriques ne sont pas collationnées.

    python3 -I content/sources/ordinaire/check.py            résumé par section
    python3 -I content/sources/ordinaire/check.py -v         détail des mots non confirmés
    python3 -I content/sources/ordinaire/check.py --tsv F    écrit le détail dans F (relecture)
"""

from __future__ import annotations

import difflib
import importlib.util
import re
import sys
import unicodedata
from pathlib import Path

HERE = Path(__file__).resolve().parent
CONTENT = HERE.parents[1]
DO_CACHE = CONTENT / ".cache" / "raw" / "ordinaire" / "divinum-officium" / "web" / "www" / "missa" / "Latin" / "Ordo"


def _load(name: str, path: Path):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


normalize = _load("ordinaire_normalize", HERE / "normalize.py")
ocr = _load("prieres_ocr", HERE.parent / "prieres" / "ocr.py")  # même cache que les prières

FOLD = str.maketrans({"æ": "ae", "œ": "oe", "Æ": "ae", "Œ": "oe", "’": "'", "ſ": "s"})


def words(text: str) -> list[str]:
    text = text.translate(FOLD).lower()
    text = re.sub(r"(\w)- (\w)", r"\1\2", text)
    text = unicodedata.normalize("NFD", text)
    text = "".join(c for c in text if unicodedata.category(c) != "Mn")
    return re.findall(r"[a-z0-9]+", text)


def source_words(src: dict) -> list[str]:
    if "do" in src:
        lines = (DO_CACHE / src["do"]).read_text(encoding="utf-8").splitlines()
        first, _, last = src["lines"].partition("-")
        return words(" ".join(lines[int(first) - 1:int(last or first)]))
    first, _, last = src["images"].partition("-")
    lo, hi = int(first), int(last or first)
    off = ocr.LEAF_OFFSET.get(src["archive"], 0)
    pages = ocr.pages(src["archive"])
    return words(" ".join(ocr.glue(pages[i + off]) for i in range(lo, hi + 1)))


def collate() -> list[tuple]:
    rows = []
    for unit, _ in normalize.UNITS:
        for s in normalize.parse(HERE / "texte" / f"{unit}.txt"):
            for lang in ("la", "fr"):
                srcs = [x for x in s["src"] + s["wit"] if lang in x["lang"]]
                if not srcs:
                    continue
                pool = [w for x in srcs for w in source_words(x)]
                for n, b in enumerate(s["blocks"], 1):
                    text = b["t"].get(lang)
                    if not text or b["type"] == "rubric":
                        continue
                    mine = words(re.sub(r"[℣℟✠/_]", " ", text))
                    sm = difflib.SequenceMatcher(None, mine, pool, autojunk=False)
                    ok = set()
                    for a, _b, size in sm.get_matching_blocks():
                        ok.update(range(a, a + size))
                    missing = [mine[i] for i in range(len(mine)) if i not in ok]
                    rows.append((s["key"], f"{s['key']}.{n}", lang, len(mine), missing))
    return rows


def main(argv: list[str]) -> int:
    rows = collate()
    total = {"la": [0, 0], "fr": [0, 0]}
    by_section: dict[str, list[int]] = {}
    for key, _bid, lang, n, missing in rows:
        acc = by_section.setdefault(key, [0, 0])
        acc[0] += n
        acc[1] += len(missing)
        total[lang][0] += n
        total[lang][1] += len(missing)
    for key, (n, m) in by_section.items():
        print(f"{key:28} {n:5} mots, {m:4} non confirmés ({100 * (n - m) / max(n, 1):.1f} % confirmés)")
    for lang, (n, m) in total.items():
        print(f"TOTAL {lang} {n} mots, {m} non confirmés ({100 * (n - m) / max(n, 1):.1f} % confirmés)")
    detail = [f"{bid}\t{lang}\t{' '.join(missing)}" for _k, bid, lang, _n, missing in rows if missing]
    if "-v" in argv:
        print("\n".join(detail))
    if "--tsv" in argv:
        out = Path(argv[argv.index("--tsv") + 1])
        out.write_text("bloc\tlangue\tmots non confirmés par la source (forme pliée)\n" + "\n".join(detail) + "\n",
                       encoding="utf-8")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
