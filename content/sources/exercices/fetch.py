#!/usr/bin/env python3
"""Télécharge les sources des Exercices dans content/.cache/raw/exercices/ (une seule fois par fichier).

    python3 -I content/sources/exercices/fetch.py [--witness]

Sans option : la page HTML de livres-mystiques.com (trad. Jennesseaux, numérisation Frère Jérôme 2005).
--witness : ajoute deux témoins, jamais ingérés : le PDF de raggionline.com (impression de la même page, 2013)
et l'OCR archive.org de l'édition Jennesseaux de 1913 (pour trancher les références douteuses).
Chaque téléchargement est journalisé dans fetch-log.json (URL, date, taille, sha256). Bibliothèque standard seulement ;
les fichiers téléchargés sont des données, jamais exécutées.
"""

from __future__ import annotations

import datetime
import hashlib
import json
import sys
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RAW = ROOT / ".cache" / "raw" / "exercices"
LOG = RAW / "fetch-log.json"
USER_AGENT = "Paroissien-pipeline/0.1"

SOURCES = {
    "exercices.html": "http://livres-mystiques.com/partieTEXTES/Exercices_Ignace/exercices.html",
}
WITNESSES = {
    "raggionline-exercices.pdf": "http://www.raggionline.com/saggi/scritti/fr/exercices.pdf",
    # Édition de 1913 (J. de Gigord, 17e éd.), OCR d'archive.org : sert à trancher les références douteuses.
    "jennesseaux-1913_djvu.txt": "https://archive.org/download/exercicesspirit00igna/exercicesspirit00igna_djvu.txt",
}


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
    print(f"{name} : {len(data)} octets, sha256 {log[name]['sha256'][:12]}…")


def main(argv: list[str]) -> int:
    RAW.mkdir(parents=True, exist_ok=True)
    log = json.loads(LOG.read_text(encoding="utf-8")) if LOG.exists() else {}
    sources = dict(SOURCES)
    if "--witness" in argv:
        sources.update(WITNESSES)
    for name, url in sources.items():
        fetch(name, url, log)
    LOG.write_text(json.dumps(log, ensure_ascii=False, indent=1, sort_keys=True) + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
