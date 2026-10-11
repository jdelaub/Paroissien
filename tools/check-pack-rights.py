#!/usr/bin/env python3
"""Contrôle des droits des paquets embarqués (CI publique ; revue 15, B1).

Usage : check-pack-rights.py DOSSIER [DOSSIER ...]

Chaque paquet <id>.paq ou <id>.sqlite trouvé sous un dossier (sous-dossiers
compris) doit avoir, à côté de lui, un <id>.manifest.json dont le champ
`rights` vaut « domaine-public » ou commence par « licence-libre: ».
Tout autre statut (autorisation-écrite:…, usage-privé-seulement) fait échouer
le contrôle. Un dossier absent n'est qu'un avertissement (saveur sans paquets).

Codes de sortie : 0 conforme, 1 non conforme, 2 usage incorrect.
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

PACK_SUFFIXES = (".paq", ".sqlite")
PUBLIC_RIGHTS = "domaine-public"
FREE_LICENCE_PREFIX = "licence-libre:"


def rights_problem(rights: object) -> str | None:
    """Renvoie None si `rights` est publiable, sinon la raison du refus."""
    if not isinstance(rights, str):
        return f"champ `rights` absent ou non textuel ({rights!r})"
    if rights == PUBLIC_RIGHTS:
        return None
    if rights.startswith(FREE_LICENCE_PREFIX) and rights[len(FREE_LICENCE_PREFIX):].strip():
        return None
    return (
        f"rights = {rights!r} ; seuls « {PUBLIC_RIGHTS} » et "
        f"« {FREE_LICENCE_PREFIX}<licence> » sont admis dans une saveur publique"
    )


def check_pack(pack: Path) -> str | None:
    """Vérifie le manifeste d'un paquet ; renvoie un message d'erreur ou None."""
    manifest = pack.with_name(pack.stem + ".manifest.json")
    if not manifest.is_file():
        return f"{pack} : manifeste absent (attendu : {manifest.name})"
    try:
        data = json.loads(manifest.read_text(encoding="utf-8"))
    except (OSError, UnicodeDecodeError, json.JSONDecodeError) as e:
        return f"{pack} : manifeste illisible ({manifest.name} : {e})"
    if not isinstance(data, dict):
        return f"{pack} : {manifest.name} n'est pas un objet JSON"
    problem = rights_problem(data.get("rights"))
    return f"{pack} : {problem}" if problem else None


def find_packs(directory: Path) -> list[Path]:
    return sorted(
        p for p in directory.rglob("*")
        if p.is_file() and p.suffix.lower() in PACK_SUFFIXES
    )


def check_dirs(directories: list[Path]) -> tuple[list[str], list[str], int]:
    """Renvoie (erreurs, avertissements, nombre de paquets vérifiés)."""
    errors: list[str] = []
    warnings: list[str] = []
    count = 0
    for directory in directories:
        if not directory.exists():
            warnings.append(f"{directory} : dossier absent, ignoré")
            continue
        if not directory.is_dir():
            errors.append(f"{directory} : n'est pas un dossier")
            continue
        for pack in find_packs(directory):
            count += 1
            problem = check_pack(pack)
            if problem:
                errors.append(problem)
    return errors, warnings, count


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        description="Vérifie que chaque paquet embarqué est domaine-public ou licence-libre."
    )
    parser.add_argument("dirs", nargs="+", type=Path, metavar="DOSSIER")
    args = parser.parse_args(argv)

    errors, warnings, count = check_dirs(args.dirs)
    for w in warnings:
        print(f"avertissement : {w}", file=sys.stderr)
    if errors:
        for e in errors:
            print(f"ERREUR : {e}", file=sys.stderr)
        print(
            f"check-pack-rights : {len(errors)} problème(s) sur {count} paquet(s).",
            file=sys.stderr,
        )
        return 1
    print(f"check-pack-rights : {count} paquet(s) conforme(s).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
