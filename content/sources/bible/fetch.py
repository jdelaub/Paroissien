#!/usr/bin/env python3
"""Télécharge les sources bibliques (archives GitLab CrossWire à un commit figé) dans content/.cache/raw/bible/.

    python3 -I content/sources/bible/fetch.py [--force]

Bibliothèque standard seulement. Rien n'est extrait ni exécuté : normalize.py lit les archives directement.
Chaque téléchargement est journalisé (URL, commit, taille, sha256, date) dans fetch-log.json.
"""

from __future__ import annotations

import datetime
import hashlib
import json
import sys
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RAW = ROOT / ".cache" / "raw" / "bible"
USER_AGENT = "Paroissien-pipeline/0.1"
API = "https://gitlab.com/api/v4/projects"

# Commits figés : changer un commit, c'est changer de source ; relire le diff des données produites.
SOURCES = {
    "crampon": {"project": 14219928, "path": "crosswire-bible-society/crampon",
                "commit": "8dbc427f853798132620ccff958fdb25459d2c0a"},
    "vulgclementine": {"project": 39534063, "path": "crosswire-bible-society/vulgclementine",
                       "commit": "be6ca9c13addb83ea6faa056e78971fd408e8bb4"},
}

# Tables de versification SWORD (GPL-2) : témoins pour check.py, jamais recopiées dans les données.
REFERENCES = {
    "canon_catholic2.h": "https://www.crosswire.org/svn/sword/trunk/include/canon_catholic2.h",
    "canon_vulg.h": "https://www.crosswire.org/svn/sword/trunk/include/canon_vulg.h",
}


def archive_path(name: str) -> Path:
    return RAW / f"{name}-{SOURCES[name]['commit'][:12]}.tar.gz"


def archive_url(name: str) -> str:
    src = SOURCES[name]
    return f"{API}/{src['project']}/repository/archive.tar.gz?sha={src['commit']}"


def download(url: str, target: Path, gzip: bool = True) -> bytes:
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT, "Accept": "application/octet-stream"})
    with urllib.request.urlopen(request, timeout=120) as response:
        data = response.read()
    if gzip and data[:2] != b"\x1f\x8b":
        raise RuntimeError(f"{url} : réponse inattendue (pas une archive gzip, {len(data)} octets)")
    tmp = target.with_suffix(".part")
    tmp.write_bytes(data)
    tmp.replace(target)
    return data


def main(argv: list[str]) -> int:
    force = "--force" in argv
    RAW.mkdir(parents=True, exist_ok=True)
    log_file = RAW / "fetch-log.json"
    log = json.loads(log_file.read_text(encoding="utf-8")) if log_file.exists() else {}
    for name in SOURCES:
        target = archive_path(name)
        url = archive_url(name)
        if target.exists() and not force:
            data = target.read_bytes()
            print(f"{name} : en cache ({target.name})")
        else:
            data = download(url, target)
            log[name] = {"retrieved": datetime.date.today().isoformat()}
            print(f"{name} : téléchargé ({len(data)} octets)")
        log.setdefault(name, {"retrieved": datetime.date.today().isoformat()})
        log[name].update({
            "url": url, "repository": f"https://gitlab.com/{SOURCES[name]['path']}", "commit": SOURCES[name]["commit"],
            "file": target.name, "size": len(data), "sha256": hashlib.sha256(data).hexdigest(),
        })
    for name, url in REFERENCES.items():
        target = RAW / name
        data = target.read_bytes() if target.exists() and not force else download(url, target, gzip=False)
        log.setdefault(name, {"retrieved": datetime.date.today().isoformat()})
        log[name].update({"url": url, "file": name, "size": len(data), "sha256": hashlib.sha256(data).hexdigest()})
        print(f"{name} : {len(data)} octets")
    log_file.write_text(json.dumps(log, ensure_ascii=False, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
