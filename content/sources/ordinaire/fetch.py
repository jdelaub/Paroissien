#!/usr/bin/env python3
"""Télécharge les sources de l'Ordinaire de 1962 dans le cache (bibliothèque standard ; python3 -I).

    python3 -I content/sources/ordinaire/fetch.py                 Ordo de Divinum Officium et OCR des paroissiens
    python3 -I content/sources/ordinaire/fetch.py page ID N… [--scale N]   fac-similés, pour la relecture

- Latin : fichiers de web/www/missa/Latin/Ordo/ de Divinum Officium, au commit figé du calendrier de 1962,
  dans content/.cache/raw/ordinaire/divinum-officium/ (raw.githubusercontent.com, un fichier par requête).
- Français : OCR et fac-similés d'archive.org par la chaîne des prières (content/sources/prieres/fetch.py), dans
  son cache content/.cache/raw/prieres/<identifiant>/.

User-Agent du projet, une seconde entre deux requêtes, rien n'est retéléchargé ; fetch-log.json garde l'URL, la
date, la taille et le sha256. Aucun fichier téléchargé n'est exécuté : ce ne sont que du texte, du JSON et des images.
"""

from __future__ import annotations

import importlib.util
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
CONTENT = HERE.parents[1]
CACHE = CONTENT / ".cache" / "raw" / "ordinaire"

DO_REPO = "DivinumOfficium/divinum-officium"
DO_COMMIT = "b9c100d79ae481c67b2801fdd3622b00dd86bd73"  # même commit que content/sources/calendrier-1962/fetch.py
DO_FILES = [
    "LICENSE",
    "web/www/missa/Latin/Ordo/Ordo.txt",         # Ordo Missæ (rubriques conditionnelles, dont celles de 1960)
    "web/www/missa/Latin/Ordo/Prayers.txt",      # répons et formules appelés par l'Ordo ($Pater noster, &Gloria…)
    "web/www/missa/Latin/Ordo/Prefationes.txt",  # préfaces
    "web/www/missa/Latin/Ordo/Communio.txt",     # communion des fidèles (avec le Confiteor d'avant 1960)
]

# Paroissiens dont on tire le français (rôle détaillé dans README.md).
EDITIONS = ["cihm_990278", "cihm_80314", "paroissienromai00churgoog", "paroissienromain00tour"]


def _prieres_fetch():
    spec = importlib.util.spec_from_file_location("prieres_fetch", HERE.parent / "prieres" / "fetch.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def do_path(name: str) -> Path:
    return CACHE / "divinum-officium" / name


def fetch_divinum(pf) -> None:
    pf.CACHE = CACHE  # même fonction get(), autre cache et autre journal
    pf.LOG = CACHE / "fetch-log.json"
    for name in DO_FILES:
        path = pf.get(f"https://raw.githubusercontent.com/{DO_REPO}/{DO_COMMIT}/{name}", do_path(name))
        print(path.relative_to(CONTENT))


def main(argv: list[str]) -> int:
    pf = _prieres_fetch()
    if argv and argv[0] == "page":
        return pf.main(argv)
    for ident in EDITIONS:
        pf.fetch_edition(ident)
    fetch_divinum(pf)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
