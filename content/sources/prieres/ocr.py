"""Accès au texte OCR d'archive.org, page par page (bibliothèque standard).

Le texte vient de <id>_hocr_searchtext.txt.gz, découpé par <id>_hocr_pageindex.json.gz : l'entrée i de l'index
donne [début, fin) de la page i dans le texte. L'image de cette page s'obtient par
https://archive.org/download/<id>/page/n<i - décalage>.jpg ; le décalage propre à chaque édition est dans
LEAF_OFFSET (vérifié sur le fac-similé).

    python3 -I content/sources/prieres/ocr.py find ID REGEX     pages où REGEX apparaît (texte recollé)
    python3 -I content/sources/prieres/ocr.py page ID I [J]     texte OCR brut des pages I à J
"""

from __future__ import annotations

import gzip
import json
import re
import sys
import unicodedata
from functools import lru_cache
from pathlib import Path

CACHE = Path(__file__).resolve().parents[2] / ".cache" / "raw" / "prieres"

# Index hOCR i → image BookReader n(i - décalage) ; relevé sur le fac-similé.
LEAF_OFFSET = {
    "paroissienromain00tour": 1,
    "cihm_80314": 3,
    "cihm_990278": 3,
}


@lru_cache(maxsize=None)
def pages(ident: str) -> list[str]:
    base = CACHE / ident / ident
    text = gzip.open(f"{base}_hocr_searchtext.txt.gz", "rt", encoding="utf-8").read()
    index = json.load(gzip.open(f"{base}_hocr_pageindex.json.gz", "rt", encoding="utf-8"))
    return [unicodedata.normalize("NFC", text[a:b]) for a, b, *_ in index]


def glue(text: str) -> str:
    """Recolle le texte d'une page : coupures de mots en fin de ligne, espaces multiples."""
    text = re.sub(r"(\w)- *\n *(\w)", r"\1\2", text)
    text = re.sub(r"[ \t]+", " ", text)
    return re.sub(r" *\n *", " ", text).strip()


def find(ident: str, pattern: str) -> list[tuple[int, str]]:
    rx = re.compile(pattern, re.IGNORECASE)
    out = []
    for i, p in enumerate(pages(ident)):
        g = glue(p)
        for m in rx.finditer(g):
            out.append((i, g[max(0, m.start() - 60): m.end() + 80]))
    return out


def image_index(ident: str, i: int) -> int:
    return i - LEAF_OFFSET.get(ident, 0)


def main(argv: list[str]) -> int:
    if argv[0] == "find":
        for i, ctx in find(argv[1], argv[2]):
            print(f"[{i} → n{image_index(argv[1], i)}] …{ctx}…")
    elif argv[0] == "page":
        a = int(argv[2])
        b = int(argv[3]) if len(argv) > 3 else a
        for i in range(a, b + 1):
            print(f"===== {argv[1]} hOCR {i} (image n{image_index(argv[1], i)})")
            print(re.sub(r"[ \t]+", " ", pages(argv[1])[i]))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
