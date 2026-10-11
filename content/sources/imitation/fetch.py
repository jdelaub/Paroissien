#!/usr/bin/env python3
"""Télécharge les sources de l'Imitation dans content/.cache/raw/imitation/ (une seule fois par fichier).

    python3 -I content/sources/imitation/fetch.py [--compare]

Sans option : Gutenberg n° 57824 (Lamennais) et les quatre pages de The Latin Library.
--compare : ajoute les témoins latins, utilisés seulement pour recouper l'OCR : la.wikisource (wikitexte brut)
et trois éditions imprimées lues par OCR sur archive.org (1840, 1867, 1888).
Chaque téléchargement est journalisé dans fetch-log.json (URL, date, taille, sha256). Bibliothèque standard seulement ;
les fichiers téléchargés sont des données, jamais exécutées.
"""

from __future__ import annotations

import datetime
import hashlib
import json
import sys
import time
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RAW = ROOT / ".cache" / "raw" / "imitation"
LOG = RAW / "fetch-log.json"
USER_AGENT = "Paroissien-pipeline/0.1"

SOURCES = {
    "pg57824.txt": "https://www.gutenberg.org/cache/epub/57824/pg57824.txt",
    **{f"kempis{n}.shtml": f"https://www.thelatinlibrary.com/kempis/kempis{n}.shtml" for n in range(1, 5)},
}

# Wikisource latine : une page par livre (wikitexte brut, action=raw).
WIKISOURCE_TITLES = {n: f"De imitatione Christi/{r}" for n, r in ((1, "I"), (2, "II"), (3, "III"), (4, "IV"))}


# Éditions imprimées sur archive.org (texte OCR djvu), témoins indépendants de The Latin Library.
ARCHIVE_ITEMS = (
    "thomaekempisde00thom",  # Leipzig, Tauchnitz, 1840
    "deimitationechri00unse",  # New York et Londres, White and Allen, 1888
    "deimitationechr00thomgoog",  # Londres, Williams et Norgate, 1867
)


def wikisource_url(title: str) -> str:
    return "https://la.wikisource.org/w/index.php?" + urllib.parse.urlencode({"title": title, "action": "raw"})


def fetch(name: str, url: str, log: dict) -> None:
    target = RAW / name
    if target.exists():
        print(f"{name} : déjà en cache")
        return
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=60) as response:
        data = response.read()
    target.write_bytes(data)
    log[name] = {
        "url": url,
        "retrieved": datetime.date.today().isoformat(),
        "size": len(data),
        "sha256": hashlib.sha256(data).hexdigest(),
    }
    LOG.write_text(json.dumps(log, ensure_ascii=False, indent=1, sort_keys=True) + "\n", encoding="utf-8")
    print(f"{name} : {len(data)} octets")
    time.sleep(2)  # politesse envers les serveurs


def main(argv: list[str]) -> int:
    RAW.mkdir(parents=True, exist_ok=True)
    log = json.loads(LOG.read_text(encoding="utf-8")) if LOG.exists() else {}
    sources = dict(SOURCES)
    if "--compare" in argv:
        sources.update({f"wikisource-l{n}.wiki": wikisource_url(t) for n, t in WIKISOURCE_TITLES.items()})
        sources.update({f"archive-{i}.txt": f"https://archive.org/download/{i}/{i}_djvu.txt" for i in ARCHIVE_ITEMS})
    for name, url in sources.items():
        try:
            fetch(name, url, log)
        except OSError as e:
            print(f"{name} : échec ({e})", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
