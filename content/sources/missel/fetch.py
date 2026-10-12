#!/usr/bin/env python3
"""Extraction partielle de Divinum Officium pour le Propre de la messe (bibliothèque standard ; python3 -I).

    python3 -I content/sources/missel/fetch.py [dossier]

Défaut : content/.cache/raw/missel/ (ignoré par git). Résultat : <dossier>/divinum-officium, au commit figé du
calendrier et de l'Ordinaire, clone superficiel (--depth 1, --filter=blob:none) limité par sparse-checkout à :
- web/www/missa/Latin/ et web/www/missa/Francais/ (propres, Ordo, préfaces, conclusions) ;
- web/www/horas/Latin/Commune/ et web/www/horas/Francais/Commune/ : la messe y lit les Communs (`C1`… `C12`),
  comme `setupstring_layers` de DO ;
- web/www/Tabulae/ (Kalendaria, substitutions du temporal) ;
- LICENSE.
Environ 15 Mo. Les fichiers sont des données non fiables : aucun n'est exécuté. Le français (traduction du Barroux)
ne sert qu'au dépôt privé.
"""

from __future__ import annotations

import subprocess
import sys
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


def git(*args: str, cwd: Path | None = None) -> None:
    subprocess.run(["git", "-c", f"http.userAgent={USER_AGENT}", *args], cwd=cwd, check=True)


def main(argv: list[str]) -> int:
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
