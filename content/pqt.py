#!/usr/bin/env python3
"""Chaîne de contenu Paroissien. Bibliothèque standard seulement ; à lancer avec python3 -I.

    python3 -I content/pqt.py validate [publication…]
    python3 -I content/pqt.py build [--public-only] [--sign-key PEM] --out DIR [paquet…]
    python3 -I content/pqt.py registry check|update [publication…]
    python3 -I content/pqt.py golden check|update

Options communes : --data DIR (défaut content/data), --packages DIR (défaut content/packages),
--registry DIR (défaut content/registry). Le dépôt privé passe ses propres dossiers.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent
sys.path.insert(0, str(ROOT / "pipeline"))

from paroissien_content import canon, catalog, inlines, package, registry  # noqa: E402

GOLDEN = ROOT / "golden"


def publications(data: Path, names: list[str]) -> list[canon.Publication]:
    dirs = [data / n for n in names] if names else sorted(p for p in data.iterdir() if (p / "publication.json").exists())
    return [canon.load(d) for d in dirs]


def cmd_validate(args) -> int:
    failures = 0
    for pub in publications(args.data, args.names):
        errors = canon.validate(pub) + registry.check(pub, args.registry)
        status = "OK" if not errors else f"{len(errors)} erreur(s)"
        print(f"{pub.id} : {len(pub.all_blocks())} blocs, {len(pub.units())} unités — {status}")
        for e in errors[:50]:
            print("  " + e)
        failures += bool(errors)
    return 1 if failures else 0


def cmd_build(args) -> int:
    defs = sorted(args.packages.glob("*.json")) if not args.names else [args.packages / f"{n}.json" for n in args.names]
    manifests = []
    for path in defs:
        definition = package.load_definition(path)
        if args.public_only and not package.is_public(definition.get("rights", "")):
            print(f"{definition.get('packageId')} : ignoré (droits {definition.get('rights')!r}, construction publique)")
            continue
        manifest = package.build(definition, args.data, args.out)
        manifests.append(manifest)
        print(f"{manifest['packageId']} {manifest['packageVersion']} : {manifest['size'] // 1024} Ko, sha256 {manifest['sha256'][:12]}…")
    catalog.write(manifests, args.out, args.sign_key)
    return 0


def cmd_registry(args) -> int:
    failures = 0
    for pub in publications(args.data, args.names):
        if args.action == "update":
            print(f"{pub.id} : {registry.update(pub, args.registry)} identifiants enregistrés")
        else:
            errors = registry.check(pub, args.registry)
            for e in errors:
                print(e)
            failures += bool(errors)
    return 1 if failures else 0


def cmd_golden(args) -> int:
    """Le paquet golden se reconstruit à l'identique (ADR 003) ; ses textes canoniques servent aux tests Kotlin."""
    dist = GOLDEN / "dist"
    with tempfile.TemporaryDirectory() as tmp:
        out = Path(tmp)
        manifest = package.build(package.load_definition(GOLDEN / "packages" / "golden-test.json"), GOLDEN / "data", out)
        pub = canon.load(GOLDEN / "data" / "golden.test")
        expected = {
            "publication": pub.id,
            "blocks": [
                {"id": b["id"], "lang": lang, "inlines": b["t"][lang], "plain": inlines.plain_text(b["t"][lang]),
                 "rev": canon.rev(inlines.plain_text(b["t"][lang]))}
                for b in pub.all_blocks() for lang in sorted(b.get("t", {}))
            ],
        }
        expected_text = canon.dumps(expected) + "\n"
        if args.action == "update":
            dist.mkdir(parents=True, exist_ok=True)
            shutil.copy(out / "golden-test.sqlite", dist / "golden-test.sqlite")
            shutil.copy(out / "golden-test.manifest.json", dist / "golden-test.manifest.json")
            (dist / "golden-test.expected.json").write_text(expected_text, encoding="utf-8")
            print(f"golden mis à jour : sha256 {manifest['sha256']}")
            return 0
        committed = hashlib.sha256((dist / "golden-test.sqlite").read_bytes()).hexdigest()
        same_expected = (dist / "golden-test.expected.json").read_text(encoding="utf-8") == expected_text
        if committed != manifest["sha256"] or not same_expected:
            print(f"golden différent : reconstruit {manifest['sha256']}, commité {committed} ; lancez « golden update » et relisez le diff")
            return 1
        print(f"golden identique octet pour octet ({committed[:12]}…)")
        return 0


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(prog="pqt")
    parser.add_argument("--data", type=Path, default=ROOT / "data")
    parser.add_argument("--packages", type=Path, default=ROOT / "packages")
    parser.add_argument("--registry", type=Path, default=ROOT / "registry")
    sub = parser.add_subparsers(dest="command", required=True)
    p = sub.add_parser("validate")
    p.add_argument("names", nargs="*")
    p = sub.add_parser("build")
    p.add_argument("--out", type=Path, required=True)
    p.add_argument("--public-only", action="store_true")
    p.add_argument("--sign-key", type=Path)
    p.add_argument("names", nargs="*")
    p = sub.add_parser("registry")
    p.add_argument("action", choices=["check", "update"])
    p.add_argument("names", nargs="*")
    p = sub.add_parser("golden")
    p.add_argument("action", choices=["check", "update"])
    args = parser.parse_args(argv)
    return {"validate": cmd_validate, "build": cmd_build, "registry": cmd_registry, "golden": cmd_golden}[args.command](args)


if __name__ == "__main__":
    sys.exit(main())
