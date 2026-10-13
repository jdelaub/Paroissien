#!/usr/bin/env python3
"""Extraction partielle de Divinum Officium pour le Propre de la messe (bibliothèque standard ; python3 -I).

    python3 -I content/sources/missel/fetch.py [dossier]
    python3 -I content/sources/missel/fetch.py archive     # textes OCR d'archive.org (propres de France, stations)
    python3 -I content/sources/missel/fetch.py page ID N… [--scale 2]   # fac-similés, pour la transcription

Défaut : content/.cache/raw/missel/ (ignoré par git). Résultat : <dossier>/divinum-officium, au commit figé du
calendrier et de l'Ordinaire, clone superficiel (--depth 1, --filter=blob:none) limité par sparse-checkout à :
- web/www/missa/Latin/ et web/www/missa/Francais/ (propres, Ordo, préfaces, conclusions) ;
- web/www/horas/Latin/Commune/ et web/www/horas/Francais/Commune/ : la messe y lit les Communs (`C1`… `C12`),
  comme `setupstring_layers` de DO ;
- web/www/Tabulae/ (Kalendaria, substitutions du temporal) ;
- LICENSE.
Environ 15 Mo. Les fichiers sont des données non fiables : aucun n'est exécuté. Le français (traduction du Barroux)
ne sert qu'au dépôt privé.

`archive` télécharge, par la fonction `get` de content/sources/prieres/fetch.py (User-Agent du projet, une seconde
entre deux requêtes, journal fetch-log.json avec sha256), les métadonnées et le texte OCR déjà produit par archive.org
(`_djvu.txt`) des éditions de ARCHIVE, dans content/.cache/raw/missel/archive/<identifiant>/. Aucun OCR n'est lancé.
"""

from __future__ import annotations

import importlib.util
import json
import subprocess
import sys
import urllib.parse
from pathlib import Path

HERE = Path(__file__).resolve().parent
CACHE = HERE.parents[1] / ".cache" / "raw" / "missel"

USER_AGENT = "Paroissien-pipeline/0.1"
DO_URL = "https://github.com/DivinumOfficium/divinum-officium.git"
DO_COMMIT = "b9c100d79ae481c67b2801fdd3622b00dd86bd73"  # même commit que le calendrier de 1962 et l'Ordinaire
SPARSE = [
    "/web/www/missa/Latin/",
    "/web/www/missa/Francais/",
    "/web/www/horas/Latin/Commune/",
    "/web/www/horas/Francais/Commune/",
    "/web/www/Tabulae/",
    "/LICENSE",
]


# Éditions d'archive.org lues pour le propre de France et les stations (voir README.md).
ARCHIVE = {
    "messes-diocese-de-paris": "Missae propriae archidiœcesis Parisiensis (missel, 1953) : sainte Geneviève, sainte Jeanne d'Arc",
    "missaleromanume00churgoog": "Missale Romanum, Benziger, 1942 (typica de 1920) : stations",
    "bc.radom.pl.Missale_romanum_skaryszew": "Missale Romanum, Ratisbonne, Pustet (édition de Benoît XV), exemplaire de Skaryszew : "
                                             "messe de sainte Jeanne d'Arc (pro aliquibus locis), stations",
}


def _prieres_fetch():
    spec = importlib.util.spec_from_file_location("prieres_fetch", HERE.parent / "prieres" / "fetch.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def fetch_archive() -> int:
    pf = _prieres_fetch()
    pf.CACHE = CACHE / "archive"  # même fonction get(), autre cache et autre journal
    pf.LOG = pf.CACHE / "fetch-log.json"
    for ident in ARCHIVE:
        meta = pf.get(f"https://archive.org/metadata/{ident}", pf.CACHE / ident / "metadata.json")
        files = [f["name"] for f in json.loads(meta.read_text(encoding="utf-8")).get("files", [])
                 if f["name"].endswith(("_djvu.txt", "_page_numbers.json"))]
        for name in files:
            path = pf.get(f"https://archive.org/download/{ident}/{urllib.parse.quote(name)}", pf.CACHE / ident / name)
            print(path)
    return 0


def git(*args: str, cwd: Path | None = None) -> None:
    subprocess.run(["git", "-c", f"http.userAgent={USER_AGENT}", *args], cwd=cwd, check=True)


def main(argv: list[str]) -> int:
    if argv[:1] == ["archive"]:
        return fetch_archive()
    if argv[:1] == ["page"]:
        pf = _prieres_fetch()
        pf.CACHE = CACHE / "archive"
        pf.LOG = pf.CACHE / "fetch-log.json"
        return pf.main(argv)
    dest = Path(argv[0]).resolve() if argv else CACHE
    repo = dest / "divinum-officium"
    if repo.exists():
        print(f"{repo} : déjà présent, ignoré")
        return 0
    repo.mkdir(parents=True)
    git("init", "-q", cwd=repo)
    git("remote", "add", "origin", DO_URL, cwd=repo)
    git("sparse-checkout", "set", "--no-cone", *SPARSE, cwd=repo)
    git("fetch", "-q", "--depth", "1", "--filter=blob:none", "origin", DO_COMMIT, cwd=repo)
    git("checkout", "-q", "FETCH_HEAD", cwd=repo)
    print(f"divinum-officium : {DO_COMMIT} → {repo}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
