"""catalog.json et sa signature ECDSA P-256 détachée (ADR 003), signée avec openssl."""

from __future__ import annotations

import subprocess
from pathlib import Path

from . import canon


def write(manifests: list[dict], out_dir: Path, sign_key: Path | None) -> Path:
    packs = [
        {
            "packageId": m["packageId"],
            "packageVersion": m["packageVersion"],
            "file": f"{m['packageId']}.sqlite",
            "manifest": f"{m['packageId']}.manifest.json",
            "sha256": m["sha256"],
            "size": m["size"],
            "rights": m["rights"],
            "minAppFormat": m["minAppFormat"],
            "title": m["title"],
            "publications": m["publications"],
        }
        for m in sorted(manifests, key=lambda m: m["packageId"])
    ]
    catalog = {
        "catalogFormat": 1,
        "generated": max((m["packageVersion"] for m in manifests), default=0),
        "packs": packs,
    }
    target = out_dir / "catalog.json"
    target.write_text(canon.dumps(catalog) + "\n", encoding="utf-8")
    if sign_key is not None:
        subprocess.run(
            ["openssl", "dgst", "-sha256", "-sign", str(sign_key), "-out", str(out_dir / "catalog.json.sig"), str(target)],
            check=True,
        )
    return target


def verify(out_dir: Path, public_key: Path) -> bool:
    result = subprocess.run(
        ["openssl", "dgst", "-sha256", "-verify", str(public_key), "-signature", str(out_dir / "catalog.json.sig"), str(out_dir / "catalog.json")],
        capture_output=True,
    )
    return result.returncode == 0
