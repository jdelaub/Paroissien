"""Registre des identifiants publiés (ADR 004) : un identifiant publié ne disparaît jamais sans alias.

registry/<publication>.ids liste, une par ligne et triés, tous les identifiants (divisions et blocs) déjà publiés.
On n'en retire jamais : un identifiant retiré doit figurer dans data/<publication>/aliases.json.
"""

from __future__ import annotations

from pathlib import Path

from . import canon


def path_for(registry_dir: Path, pub_id: str) -> Path:
    return registry_dir / f"{pub_id}.ids"


def published(registry_dir: Path, pub_id: str) -> set[str]:
    f = path_for(registry_dir, pub_id)
    return set(f.read_text(encoding="utf-8").split()) if f.exists() else set()


def check(pub: canon.Publication, registry_dir: Path) -> list[str]:
    live = canon.node_ids(pub)
    lost = sorted(published(registry_dir, pub.id) - live - set(pub.aliases))
    return [f"{pub.id} : identifiant publié disparu sans alias : {i}" for i in lost]


def update(pub: canon.Publication, registry_dir: Path) -> int:
    registry_dir.mkdir(parents=True, exist_ok=True)
    ids = published(registry_dir, pub.id) | canon.node_ids(pub)
    path_for(registry_dir, pub.id).write_text("\n".join(sorted(ids)) + "\n", encoding="utf-8")
    return len(ids)
