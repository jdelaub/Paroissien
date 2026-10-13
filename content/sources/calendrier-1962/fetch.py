#!/usr/bin/env python3
"""Clone Divinum Officium et Missalemeum aux commits figés, dans un dossier vide donné en argument.

    python3 -I content/sources/calendrier-1962/fetch.py <dossier>

Résultat : <dossier>/divinum-officium (extraction partielle : Tabulae, missa/Latin, horas/Latin/{Sancti,
Tempora,Commune,Psalterium}, les rubriques de 1960 et le Calendarium de 1961 de horas/Help/Rubrics, cgi-bin)
et <dossier>/Missalemeum (complet, ~32 Mo).
Les dépôts sont des données non fiables : on ne lance jamais leurs scripts, sauf le module
`api.kalendar` de Missalemeum, qui est l'oracle (voir oracle.py).
"""

from __future__ import annotations

import subprocess
import sys
from pathlib import Path

USER_AGENT = "Paroissien-content-pipeline (+https://github.com/jdelaub/Paroissien)"

REPOS = {
    "divinum-officium": (
        "https://github.com/DivinumOfficium/divinum-officium.git",
        "b9c100d79ae481c67b2801fdd3622b00dd86bd73",
        [  # motifs sparse-checkout (mode non-cone) : ~35 Mo au lieu de 240
            "/web/www/Tabulae/",
            "/web/www/missa/Latin/",
            "/web/www/horas/Latin/Sancti/",
            "/web/www/horas/Latin/Tempora/",
            "/web/www/horas/Latin/Commune/",
            "/web/www/horas/Latin/Psalterium/",
            "/web/www/horas/Help/Rubrics/General Rubrics.html",
            "/web/www/horas/Help/Rubrics/1960calendarium.pdf",
            "/web/cgi-bin/",
            "/LICENSE",
        ],
    ),
    "Missalemeum": (
        "https://github.com/mmolenda/Missalemeum.git",
        "88c491d17d7e7edabaa43d2cf02463413336fb75",
        None,
    ),
}


def git(*args: str, cwd: Path | None = None) -> None:
    subprocess.run(["git", "-c", f"http.userAgent={USER_AGENT}", *args], cwd=cwd, check=True)


def main(argv: list[str]) -> int:
    if len(argv) != 1:
        print(__doc__)
        return 2
    dest = Path(argv[0]).resolve()
    dest.mkdir(parents=True, exist_ok=True)
    for name, (url, commit, sparse) in REPOS.items():
        repo = dest / name
        if repo.exists():
            print(f"{name} : déjà présent, ignoré")
            continue
        repo.mkdir()
        git("init", "-q", cwd=repo)
        git("remote", "add", "origin", url, cwd=repo)
        if sparse:
            git("sparse-checkout", "set", "--no-cone", *sparse, cwd=repo)
        git("fetch", "-q", "--depth", "1", "--filter=blob:none", "origin", commit, cwd=repo)
        git("checkout", "-q", "FETCH_HEAD", cwd=repo)
        print(f"{name} : {commit}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
