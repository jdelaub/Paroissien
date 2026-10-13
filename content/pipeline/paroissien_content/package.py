"""Construction d'un paquet SQLite déterministe (ADR 003) et de son manifeste.

Définition d'un paquet, dans packages/<packageId>.json :
    {"packageId": "imitation-fr", "packageVersion": 2026101101, "rights": "domaine-public",
     "publications": ["imitation.fr"], "title": {"fr": "…"}, "minAppFormat": "1.0"}
Aucune horloge n'entre dans le fichier : deux constructions des mêmes données sont identiques octet pour octet.
"""

from __future__ import annotations

import hashlib
import json
import os
import re
import sqlite3
import tempfile
from pathlib import Path

from . import canon
from . import inlines as inl
from .fold import fold

FORMAT_MAJOR = 1
FORMAT_MINOR = 0
APPLICATION_ID = 0x5052534E
PACKAGE_ID = re.compile(r"^[a-z0-9]+(-[a-z0-9]+)*$")
PUBLIC_RIGHTS = ("domaine-public", "licence-libre:")
SCHEMA = Path(__file__).resolve().parents[2] / "schema" / "pack-v1.sql"


def is_public(rights: str) -> bool:
    return rights == PUBLIC_RIGHTS[0] or (rights.startswith(PUBLIC_RIGHTS[1]) and len(rights) > len(PUBLIC_RIGHTS[1]))


def check_definition(definition: dict) -> list[str]:
    errors = []
    pid = definition.get("packageId", "")
    if not PACKAGE_ID.match(pid):
        errors.append(f"packageId invalide {pid!r}")
    version = definition.get("packageVersion")
    if not isinstance(version, int) or not 2026000000 <= version <= 9999123199:
        errors.append(f"{pid} : packageVersion doit être un entier AAAAMMJJNN")
    rights = definition.get("rights", "")
    if not (is_public(rights) or rights == "usage-privé-seulement" or rights.startswith("autorisation-écrite:")):
        errors.append(f"{pid} : statut de droits inconnu {rights!r}")
    if not definition.get("publications"):
        errors.append(f"{pid} : aucune publication")
    return errors


def build(definition: dict, data_root: Path, out_dir: Path) -> dict:
    """Construit <packageId>.sqlite et <packageId>.manifest.json dans out_dir ; renvoie le manifeste."""
    errors = check_definition(definition)
    pubs = [canon.load(data_root / pid) for pid in definition.get("publications", [])]
    for pub in pubs:
        errors += canon.validate(pub)
    if errors:
        raise ValueError("Paquet invalide :\n  " + "\n  ".join(errors))

    package_id = definition["packageId"]
    out_dir.mkdir(parents=True, exist_ok=True)
    target = out_dir / f"{package_id}.sqlite"
    fd, tmp_name = tempfile.mkstemp(suffix=".sqlite", dir=out_dir)
    os.close(fd)
    tmp = Path(tmp_name)
    tmp.unlink()
    db = sqlite3.connect(tmp, isolation_level=None)
    try:
        db.execute("PRAGMA page_size = 4096")
        db.execute("PRAGMA journal_mode = DELETE")
        db.executescript(SCHEMA.read_text(encoding="utf-8"))
        db.execute("BEGIN")
        content_hashes = []
        for pub in pubs:
            content_hashes.append(_insert_publication(db, pub))
        info = {
            "formatMajor": str(FORMAT_MAJOR),
            "formatMinor": str(FORMAT_MINOR),
            "packageId": package_id,
            "packageVersion": str(definition["packageVersion"]),
            "minAppFormat": definition.get("minAppFormat", f"{FORMAT_MAJOR}.{FORMAT_MINOR}"),
            "rights": definition["rights"],
            "builtAt": str(definition["packageVersion"])[:8],  # date de version, pas l'horloge
            "contentHash": "sha256:" + hashlib.sha256("\n".join(content_hashes).encode()).hexdigest(),
            "sqliteVersion": sqlite3.sqlite_version,
        }
        db.executemany("INSERT INTO pack_info(key, value) VALUES (?, ?)", sorted(info.items()))
        db.execute("INSERT INTO search_fts(search_fts) VALUES ('rebuild')")
        db.execute("COMMIT")
        db.execute("VACUUM")
    finally:
        db.close()
    tmp.replace(target)

    data = target.read_bytes()
    manifest = {
        "packageId": package_id,
        "packageVersion": definition["packageVersion"],
        "formatMajor": FORMAT_MAJOR,
        "formatMinor": FORMAT_MINOR,
        "minAppFormat": info["minAppFormat"],
        "rights": definition["rights"],
        "title": definition.get("title", {}),
        "languages": sorted({lang for pub in pubs for lang in pub.meta["languages"]}),
        "publications": [pub.id for pub in pubs],
        "license": [pub.meta["license"] for pub in pubs],
        "contentHash": info["contentHash"],
        "sqliteVersion": sqlite3.sqlite_version,
        "size": len(data),
        "sha256": hashlib.sha256(data).hexdigest(),
    }
    (out_dir / f"{package_id}.manifest.json").write_text(canon.dumps(manifest) + "\n", encoding="utf-8")
    return manifest


def _insert_publication(db: sqlite3.Connection, pub: canon.Publication) -> str:
    meta = pub.meta
    primary = meta["primaryLanguage"]
    content_hash = canon.content_hash(pub)
    blocks = pub.all_blocks()
    char_count = sum(len(inl.plain_text(b.get("t", {}).get(primary, []))) for b in blocks)
    extra = {k: v for k, v in meta.items() if k not in {"id", "revision", "kind", "title", "languages", "primaryLanguage", "aligned"}}
    cur = db.execute(
        "INSERT INTO publication(id, revision, kind, primary_lang, languages, aligned, title, meta, content_hash, char_count)"
        " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        (meta["id"], meta["revision"], meta["kind"], primary, canon.dumps(meta["languages"]), int(bool(meta.get("aligned"))),
         canon.dumps(meta["title"]), canon.dumps(extra), content_hash, char_count),
    )
    pub_row = cur.lastrowid

    division_rows: dict[str, int] = {}

    def insert_division(d: canon.Division, parent: int | None, ord_: int) -> None:
        row = db.execute(
            "INSERT INTO division(pub, id, parent, ord, kind, toc_title, is_unit, seq_first, seq_last, attrs)"
            " VALUES (?, ?, ?, ?, ?, ?, ?, 0, 0, ?)",
            (pub_row, d.id, parent, ord_, d.kind, d.toc, int(d.unit), canon.dumps(d.attrs) if d.attrs else None),
        ).lastrowid
        division_rows[d.id] = row
        for i, child in enumerate(d.children):
            insert_division(child, row, i)

    for i, d in enumerate(pub.divisions):
        insert_division(d, None, i)

    seq = 0
    char_start = 0
    for unit in pub.units():
        for block in pub.blocks.get(unit.id, []):
            seq += 1
            texts = block.get("t", {})
            attrs = {k: v for k, v in block.items() if k not in {"id", "type", "t"}}
            primary_text = inl.plain_text(texts.get(primary, []))
            block_row = db.execute(
                "INSERT INTO block(pub, id, unit, seq, type, attrs, char_start, char_len) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                (pub_row, block["id"], division_rows[unit.id], seq, block["type"], canon.dumps(attrs) if attrs else None,
                 char_start, len(primary_text)),
            ).lastrowid
            char_start += len(primary_text)
            for lang in sorted(texts):
                plain = inl.plain_text(texts[lang])
                db.execute(
                    "INSERT INTO block_text(block, lang, inlines, rev) VALUES (?, ?, ?, ?)",
                    (block_row, lang, canon.dumps(texts[lang]), canon.rev(plain)),
                )
                folded = fold(plain)
                head, body = (folded, "") if block["type"] == "heading" else ("", folded)
                db.execute("INSERT INTO search_doc(block, lang, head, body) VALUES (?, ?, ?, ?)", (block_row, lang, head, body))

    # Plages de blocs de chaque division, unités et ancêtres compris.
    for d in canon.walk(pub.divisions):
        ids = [u.id for u in canon.walk([d]) if u.unit]
        rows = db.execute(
            f"SELECT MIN(seq), MAX(seq) FROM block WHERE pub = ? AND unit IN ({','.join('?' * len(ids))})",
            (pub_row, *[division_rows[i] for i in ids]),
        ).fetchone() if ids else (0, 0)
        db.execute("UPDATE division SET seq_first = ?, seq_last = ? WHERE rowid = ?", (rows[0] or 0, rows[1] or 0, division_rows[d.id]))

    for old, new in sorted(pub.aliases.items()):
        db.execute("INSERT INTO id_alias(pub, old_id, new_id, since_revision) VALUES (?, ?, ?, ?)", (pub_row, old, new, meta["revision"]))
    # Images des blocs `image` : rangées par nom, préfixé de la publication (un paquet peut en porter plusieurs).
    for name, f in sorted(pub.assets.items()):
        data = f.read_bytes()
        db.execute(
            "INSERT INTO asset(href, media_type, sha256, meta, data) VALUES (?, ?, ?, ?, ?)",
            (f"{pub.id}/{name}", canon.MEDIA_TYPES[name.rsplit(".", 1)[1]], hashlib.sha256(data).hexdigest(), "{}", data),
        )
    return content_hash


def load_definition(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))
