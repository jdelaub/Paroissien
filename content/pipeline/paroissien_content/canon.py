"""Données canoniques d'une publication (rapport 11 §3.2, ADR 003 et 004).

Disposition dans le dépôt :
    data/<publication>/publication.json   métadonnées et arbre des divisions
    data/<publication>/blocks/<unité>.jsonl   un bloc par ligne, dans l'ordre de lecture
    data/<publication>/aliases.json       (facultatif) identifiants retirés : {"ancien": "nouveau" | null}
    data/<publication>/assets/<nom>       (facultatif) images des blocs `image` (WebP ou PNG), rangées dans le paquet

Une division porte "unit": true quand elle est une unité de lecture ; chaque unité a son fichier de blocs.
"""

from __future__ import annotations

import hashlib
import json
import re
from dataclasses import dataclass, field
from pathlib import Path

from . import inlines as inl

BLOCK_TYPES = {"heading", "para", "verse", "rubric", "versicle", "quote", "stanza", "note", "image", "separator", "item", "slot"}
PUB_ID = re.compile(r"^[a-z0-9]+(\.[a-z0-9-]+)+$")
NODE_ID = re.compile(r"^[a-z0-9][a-z0-9._-]{0,63}$")
ASSET_NAME = re.compile(r"^[a-z0-9][a-z0-9._-]{0,63}\.(webp|png)$")
MEDIA_TYPES = {"webp": "image/webp", "png": "image/png"}
REQUIRED_META = ("id", "revision", "kind", "title", "languages", "primaryLanguage", "source", "license")


@dataclass
class Division:
    id: str
    kind: str
    toc: str | None
    unit: bool
    attrs: dict
    children: list["Division"] = field(default_factory=list)


@dataclass
class Publication:
    path: Path
    meta: dict
    divisions: list[Division]
    blocks: dict[str, list[dict]]  # unité → blocs
    aliases: dict[str, str | None]
    assets: dict[str, Path] = field(default_factory=dict)  # nom → fichier

    @property
    def id(self) -> str:
        return self.meta["id"]

    def units(self) -> list[Division]:
        return [d for d in walk(self.divisions) if d.unit]

    def all_blocks(self) -> list[dict]:
        return [b for u in self.units() for b in self.blocks.get(u.id, [])]


def walk(divisions: list[Division]):
    for d in divisions:
        yield d
        yield from walk(d.children)


def _division(raw: dict) -> Division:
    return Division(
        id=raw["id"],
        kind=raw.get("kind", "section"),
        toc=raw.get("toc"),
        unit=bool(raw.get("unit", False)),
        attrs={k: v for k, v in raw.items() if k not in {"id", "kind", "toc", "unit", "children"}},
        children=[_division(c) for c in raw.get("children", [])],
    )


def load(path: Path) -> Publication:
    meta = json.loads((path / "publication.json").read_text(encoding="utf-8"))
    divisions = [_division(d) for d in meta.pop("divisions", [])]
    blocks: dict[str, list[dict]] = {}
    for d in walk(divisions):
        if d.unit:
            f = path / "blocks" / f"{d.id}.jsonl"
            lines = f.read_text(encoding="utf-8").splitlines() if f.exists() else []
            blocks[d.id] = [json.loads(line) for line in lines if line.strip()]
    aliases_file = path / "aliases.json"
    aliases = json.loads(aliases_file.read_text(encoding="utf-8")) if aliases_file.exists() else {}
    assets_dir = path / "assets"
    assets = {f.name: f for f in sorted(assets_dir.iterdir()) if f.is_file()} if assets_dir.is_dir() else {}
    return Publication(path, meta, divisions, blocks, aliases, assets)


def rev(text: str) -> str:
    """ADR 004 : 16 premiers caractères hexadécimaux du SHA-256 du texte canonique en UTF-8."""
    return hashlib.sha256(text.encode("utf-8")).hexdigest()[:16]


def dumps(obj) -> str:
    """JSON canonique : NFC déjà vérifié, clés triées, sans espaces, UTF-8 littéral."""
    return json.dumps(obj, ensure_ascii=False, sort_keys=True, separators=(",", ":"))


def content_hash(pub: Publication) -> str:
    h = hashlib.sha256()
    h.update(dumps({**pub.meta, "divisions": [_raw(d) for d in pub.divisions]}).encode("utf-8"))
    for block in pub.all_blocks():
        h.update(b"\n")
        h.update(dumps(block).encode("utf-8"))
    for name, f in sorted(pub.assets.items()):
        h.update(f"\n{name}:{hashlib.sha256(f.read_bytes()).hexdigest()}".encode("utf-8"))
    return "sha256:" + h.hexdigest()


def _raw(d: Division) -> dict:
    out = {"id": d.id, "kind": d.kind, "unit": d.unit, **d.attrs}
    if d.toc is not None:
        out["toc"] = d.toc
    if d.children:
        out["children"] = [_raw(c) for c in d.children]
    return out


def validate(pub: Publication) -> list[str]:
    errors: list[str] = []
    where = pub.path.name
    for key in REQUIRED_META:
        if key not in pub.meta:
            errors.append(f"{where} : métadonnée manquante « {key} »")
    if not PUB_ID.match(str(pub.meta.get("id", ""))):
        errors.append(f"{where} : identifiant de publication invalide {pub.meta.get('id')!r}")
    if pub.path.name != pub.meta.get("id"):
        errors.append(f"{where} : le dossier doit porter l'identifiant de la publication")
    languages = pub.meta.get("languages", [])
    if pub.meta.get("primaryLanguage") not in languages:
        errors.append(f"{where} : primaryLanguage absente de languages")
    license_ = pub.meta.get("license", {})
    if not isinstance(license_, dict) or not license_.get("spdx"):
        errors.append(f"{where} : license.spdx obligatoire")

    seen: set[str] = set()
    for d in walk(pub.divisions):
        if not NODE_ID.match(d.id):
            errors.append(f"{where} : identifiant de division invalide {d.id!r}")
        if d.id in seen:
            errors.append(f"{where} : identifiant en double {d.id!r}")
        seen.add(d.id)
        if d.unit and d.children and any(c.unit for c in walk(d.children)):
            errors.append(f"{where} : unité {d.id} imbriquée dans une autre unité")
    if not pub.units():
        errors.append(f"{where} : aucune unité de lecture")

    note_ids = {b["id"] for b in pub.all_blocks() if b.get("type") == "note"}
    for unit in pub.units():
        if not pub.blocks.get(unit.id):
            errors.append(f"{where} : unité {unit.id} sans blocs")
        for i, block in enumerate(pub.blocks.get(unit.id, [])):
            at = f"{where}/{unit.id}#{i}"
            bid = block.get("id", "")
            if not NODE_ID.match(str(bid)):
                errors.append(f"{at} : identifiant de bloc invalide {bid!r}")
            if bid in seen:
                errors.append(f"{at} : identifiant en double {bid!r}")
            seen.add(bid)
            if block.get("type") not in BLOCK_TYPES:
                errors.append(f"{at} : type de bloc inconnu {block.get('type')!r}")
            texts = block.get("t", {})
            if block.get("type") not in ("separator", "image") and not texts:
                errors.append(f"{at} : bloc sans texte")
            if block.get("type") == "image":
                # Une image du paquet (assets/) ; sa légende facultative est dans t, sa description dans alt.
                href = block.get("href", "")
                if href not in pub.assets:
                    errors.append(f"{at} : image {href!r} absente de assets/")
                if not isinstance(block.get("alt"), dict) or not block["alt"]:
                    errors.append(f"{at} : image sans description alt")
            if block.get("type") == "slot" and not block.get("slot"):
                errors.append(f"{at} : emplacement sans attribut slot")
            for lang, value in texts.items():
                if lang not in languages:
                    errors.append(f"{at} : langue {lang} non déclarée")
                inl.validate(value, f"{at}.t.{lang}", errors, note_ids)
    for name in pub.assets:
        if not ASSET_NAME.match(name):
            errors.append(f"{where} : nom de ressource invalide {name!r} (minuscules, .webp ou .png)")
    for old, new in pub.aliases.items():
        if old in seen:
            errors.append(f"{where} : l'alias {old} est encore un identifiant vivant")
        if new is not None and new not in seen:
            errors.append(f"{where} : l'alias {old} pointe vers {new}, inconnu")
    return errors


def node_ids(pub: Publication) -> set[str]:
    return {d.id for d in walk(pub.divisions)} | {b["id"] for b in pub.all_blocks()}
