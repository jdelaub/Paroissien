#!/usr/bin/env python3
"""Télécharge les paroissiens du domaine public (archive.org) dans content/.cache/raw/prieres/.

Bibliothèque standard seulement ; à lancer avec python3 -I. Chaque fichier n'est téléchargé qu'une fois
(cache) ; le journal fetch-log.json garde l'URL, la date et le sha256 de chaque fichier.

    python3 -I content/sources/prieres/fetch.py                 texte OCR et index de pages de chaque édition
    python3 -I content/sources/prieres/fetch.py page ID LEAF… [--scale N]   fac-similé de pages (JPEG) pour la relecture

Aucun fichier téléchargé n'est exécuté : ce ne sont que du texte, du JSON et des images.
"""

from __future__ import annotations

import datetime
import hashlib
import json
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]  # content/
CACHE = ROOT / ".cache" / "raw" / "prieres"
LOG = CACHE / "fetch-log.json"
USER_AGENT = "Paroissien-pipeline/0.1"

# Éditions (rapport 14, §6). La première sert de référence, les autres de collation.
EDITIONS = {
    "paroissienromain00tour": "Paroissien romain, Tours, A. Mame, 1890",
    "paroissienromain00cath": "Paroissien romain, Zech, 1901",
    "paroissienromain00cath_0": "Paroissien romain, Tours, Mame, 1872",
    "paroissienromai00churgoog": "Paroissien romain, Mame, 1869",
    "leparoissienroma00unse": "Le paroissien romain, Barbou, vers 1895",
    "bub_gb_PcH1G0A8jZUC": "Paroissien romain, Lecoffre, 1860",
    "cihm_95158": "Paroissien romain, Canadiana, 1893",
    "cihm_80314": "Paroissien de poche, Canadiana, 1914",
    "cihm_990278": "Nouveau paroissien romain, Canadiana, 1913",
}
SUFFIXES = ("_djvu.txt", "_page_numbers.json", "_hocr_searchtext.txt.gz", "_hocr_pageindex.json.gz")


def get(url: str, target: Path) -> Path:
    if target.exists() and target.stat().st_size > 0:
        return target
    target.parent.mkdir(parents=True, exist_ok=True)
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    for attempt in range(3):
        try:
            with urllib.request.urlopen(req, timeout=120) as resp:
                data = resp.read()
            break
        except urllib.error.HTTPError as e:
            if e.code < 500 or attempt == 2:
                raise
            time.sleep(5 * (attempt + 1))
    tmp = target.with_suffix(target.suffix + ".part")
    tmp.write_bytes(data)
    tmp.replace(target)
    log = json.loads(LOG.read_text(encoding="utf-8")) if LOG.exists() else {}
    log[str(target.relative_to(CACHE))] = {
        "url": url,
        "retrieved": datetime.date.today().isoformat(),
        "sha256": hashlib.sha256(data).hexdigest(),
        "size": len(data),
    }
    LOG.write_text(json.dumps(log, ensure_ascii=False, indent=1, sort_keys=True) + "\n", encoding="utf-8")
    time.sleep(1)  # politesse envers archive.org
    return target


def fetch_edition(ident: str) -> None:
    meta_path = get(f"https://archive.org/metadata/{ident}", CACHE / ident / "metadata.json")
    meta = json.loads(meta_path.read_text(encoding="utf-8"))
    names = {f["name"] for f in meta.get("files", [])}
    for suffix in SUFFIXES:
        name = ident + suffix
        if name in names:
            try:
                get(f"https://archive.org/download/{ident}/{name}", CACHE / ident / name)
            except urllib.error.URLError as e:
                print(f"{ident} : échec de {name} ({e})")
        else:
            print(f"{ident} : pas de {suffix}")
    m = meta.get("metadata", {})
    print(f"{ident} : {m.get('title', '?')[:70]} — {m.get('date', '?')} — {m.get('publisher', '?')}")


def fetch_pages(ident: str, leaves: list[str], scale: int) -> None:
    """Images BookReader n<leaf> ; scale 1 = pleine résolution, 2 = moitié, etc."""
    for leaf in leaves:
        suffix = f"_s{scale}" if scale > 1 else ""
        path = get(f"https://archive.org/download/{ident}/page/n{int(leaf)}{suffix}.jpg",
                   CACHE / ident / "pages" / f"n{int(leaf):04d}{suffix}.jpg")
        print(path)


def main(argv: list[str]) -> int:
    if argv and argv[0] == "page":
        scale = 1
        if "--scale" in argv:
            k = argv.index("--scale")
            scale = int(argv[k + 1])
            argv = argv[:k] + argv[k + 2:]
        fetch_pages(argv[1], argv[2:], scale)
        return 0
    for ident in argv or EDITIONS:
        fetch_edition(ident)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
