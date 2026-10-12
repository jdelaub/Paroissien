#!/usr/bin/env python3
"""Transcriptions relues (texte/*.txt) → content/data/ordinaire.1962/ (format canonique v1).

Bibliothèque standard seulement ; à lancer avec python3 -I depuis n'importe où :

    python3 -I content/sources/ordinaire/normalize.py [-v]

Format des transcriptions : celui des prières (content/sources/prieres/normalize.py), avec trois ajouts.

    == clé [niveau]                 une section : bloc heading « clé », de niveau 1 (défaut) ou 2
    src: do | la | Fichier.txt | lignes        latin de Divinum Officium (fichier de missa/Latin/Ordo)
    src: ident | langues | pages | images      paroissien d'archive.org (comme pour les prières)
    wit: ident | langues | pages | images      témoin, lu par check.py seulement (pas d'attribution)

Les blocs et leurs lignes la:, fr:, review: sont ceux des prières ; « ✠ » note un signe de croix, « / » un saut de
ligne. Les rubriques n'ont que le français.
"""

from __future__ import annotations

import json
import re
import sys
import unicodedata
from pathlib import Path

HERE = Path(__file__).resolve().parent
CONTENT = HERE.parents[1]
OUT = CONTENT / "data" / "ordinaire.1962"
NNBSP = " "

UNITS = [
    ("avant-messe", "Prières au bas de l’autel"),
    ("messe-des-catechumenes", "Messe des catéchumènes"),
    ("offertoire", "Offertoire"),
    ("canon", "Préface et Canon"),
    ("communion", "Communion"),
    ("fin-de-messe", "Fin de la messe"),
]

EDITIONS = {
    "cihm_990278": "Nouveau paroissien romain, Saint-Hyacinthe (Québec), É. Solis, 1913",
    "cihm_80314": "Paroissien de poche, Québec, I.-P. Déry, 1914 (imprimatur Malines, 10 mai 1914)",
    "paroissienromai00churgoog": "Paroissien romain, Tours, Alfred Mame et fils, 1869",
    "paroissienromain00tour": "Paroissien romain, Tours, A. Mame et fils, 1890 (approuvé par Mgr Meignan)",
}
DO_REPO = "https://github.com/DivinumOfficium/divinum-officium"
DO_COMMIT = "b9c100d79ae481c67b2801fdd3622b00dd86bd73"
DO_DIR = "web/www/missa/Latin/Ordo"

SYM = {"℣": "V", "℟": "R", "✠": "cross", "†": "dagger"}


def typo(text: str, lang: str) -> str:
    """Typographie française pour le français ; le latin garde celle de Divinum Officium (pas d'espace avant : ;)."""
    text = unicodedata.normalize("NFC", text)
    text = text.replace("'", "’")
    text = re.sub(r"\s+", " ", text).strip()
    text = re.sub(r"\s+([,.)])", r"\1", text)
    text = re.sub(r"\(\s+", "(", text)
    if lang != "fr":
        return text
    text = re.sub(r"\s*([;:!?])", NNBSP + r"\1", text)
    text = re.sub(r"«\s*", "«" + NNBSP, text)
    text = re.sub(r"\s*»", NNBSP + "»", text)
    return text


def inlines(text: str, lang: str) -> list:
    """Texte de transcription → liste d'inlines (symboles, saut de ligne, italique)."""
    parts = re.split(r"(℣\.?|℟\.?|✠|†|(?<= )/(?= )|_[^_]+_)", typo(text, lang))
    out: list = []
    for i, part in enumerate(parts):
        if i % 2 == 0:
            if part:
                out.append(part)
        elif part.startswith("_"):
            out.append({"m": ["i"], "c": [part[1:-1]]})
        elif part == "/":
            if out and isinstance(out[-1], str):
                out[-1] = out[-1].rstrip()
            out.append({"br": 1})
            if i + 1 < len(parts):
                parts[i + 1] = parts[i + 1].lstrip()
        else:
            out.append({"sym": SYM[part[0]]})
    return [n for n in out if n != ""]


def parse_source(value: str, where: str) -> dict:
    ident, langs, pages, images = [p.strip() for p in value.split("|")]
    if ident == "do":
        return {"do": pages, "lang": langs.split(), "lines": images}
    if ident not in EDITIONS:
        raise SystemExit(f"{where} : édition inconnue {ident!r}")
    return {"archive": ident, "lang": langs.split(), "pages": pages, "images": images}


def parse(path: Path) -> list[dict]:
    """Sections d'un fichier : {key, level, title, src, wit, removed, blocks:[{type, attrs, t, review}]}."""
    sections: list[dict] = []
    block: dict | None = None
    for lineno, raw in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        line = raw.rstrip()
        if not line or line.startswith("#"):
            block = None if not line else block
            continue
        where = f"{path.name}:{lineno}"
        if line.startswith("== "):
            key, *level = line[3:].split()
            sections.append({"key": key, "level": int(level[0]) if level else 1, "title": {}, "src": [], "wit": [],
                             "removed": [], "blocks": [], "where": where})
            block = None
            continue
        if not sections:
            raise SystemExit(f"{where} : texte avant la première section")
        cur = sections[-1]
        m = re.match(r"^(title\.(la|fr)|src|wit|removed|la|fr|review):\s?(.*)$", line)
        if m:
            key, value = m.group(1), m.group(3)
            if key.startswith("title."):
                cur["title"][m.group(2)] = value
            elif key in ("src", "wit"):
                cur[key].append(parse_source(value, where))
            elif key == "removed":
                cur["removed"].append(value)
            elif key == "review":
                if block is None:
                    raise SystemExit(f"{where} : review hors d'un bloc")
                block["review"] = value
            else:
                if block is None:
                    raise SystemExit(f"{where} : texte hors d'un bloc")
                block["t"][key] = (block["t"].get(key, "") + " " + value).strip()
            continue
        words = line.split()
        btype, args = words[0], words[1:]
        if btype not in {"para", "rubric", "versicle", "stanza", "heading"}:
            raise SystemExit(f"{where} : type de bloc inconnu {btype!r}")
        attrs: dict = {}
        if btype == "para" and args:
            attrs["style"] = args[0]
        elif btype == "versicle":
            attrs["role"] = args[0]
        elif btype == "stanza":
            attrs["kind"] = args[0] if args else "psalm"
        elif btype == "heading":
            attrs["level"] = int(args[0]) if args else 3
        block = {"type": btype, "attrs": attrs, "t": {}, "review": None, "where": where}
        cur["blocks"].append(block)
    return sections


def source_attr(src: list[dict]) -> list[dict]:
    out = []
    for s in src:
        if "do" in s:
            first, _, last = s["lines"].partition("-")
            anchor = f"#L{first}" + (f"-L{last}" if last else "")
            out.append({
                "repository": DO_REPO, "commit": DO_COMMIT, "file": f"{DO_DIR}/{s['do']}", "lang": s["lang"],
                "lines": s["lines"], "url": f"{DO_REPO}/blob/{DO_COMMIT}/{DO_DIR}/{s['do']}{anchor}",
            })
        else:
            first = s["images"].split("-")[0]
            out.append({
                "archive": s["archive"], "edition": EDITIONS[s["archive"]], "lang": s["lang"], "pages": s["pages"],
                "images": s["images"], "url": f"https://archive.org/details/{s['archive']}/page/n{first}",
            })
    return out


def build() -> dict:
    report = {"sections": [], "reviews": [], "removed": []}
    blocks_by_unit: dict[str, list[dict]] = {}
    for unit, _toc in UNITS:
        path = HERE / "texte" / f"{unit}.txt"
        if not path.exists():
            print(f"(unité {unit} absente : ignorée)")
            continue
        rows: list[dict] = []
        for s in parse(path):
            rows.append({"id": s["key"], "type": "heading", "level": s["level"], "x-source": source_attr(s["src"]),
                         "t": {lang: inlines(title, lang) for lang, title in sorted(s["title"].items())}})
            for n, b in enumerate(s["blocks"], 1):
                row = {"id": f"{s['key']}.{n}", "type": b["type"], **b["attrs"]}
                if b["review"]:
                    row["x-review"] = "à vérifier : " + b["review"]
                    report["reviews"].append((row["id"], b["review"]))
                row["t"] = {lang: inlines(text, lang) for lang, text in sorted(b["t"].items())}
                rows.append(row)
            report["sections"].append((unit, s["key"], s["title"].get("fr", ""), len(s["blocks"])))
            report["removed"] += [(s["key"], r) for r in s["removed"]]
        blocks_by_unit[unit] = rows
    return {"blocks": blocks_by_unit, "report": report}


def write(result: dict) -> None:
    (OUT / "blocks").mkdir(parents=True, exist_ok=True)
    meta = json.loads((HERE / "publication.base.json").read_text(encoding="utf-8"))
    meta["divisions"] = [{"id": unit, "kind": "section", "toc": toc, "unit": True}
                         for unit, toc in UNITS if unit in result["blocks"]]
    (OUT / "publication.json").write_text(json.dumps(meta, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    for unit, rows in result["blocks"].items():
        text = "".join(json.dumps(r, ensure_ascii=False, separators=(",", ":")) + "\n" for r in rows)
        (OUT / "blocks" / f"{unit}.jsonl").write_text(text, encoding="utf-8")


def main() -> int:
    result = build()
    write(result)
    rep = result["report"]
    total = sum(len(rows) for rows in result["blocks"].values())
    print(f"{len(result['blocks'])} unités, {len(rep['sections'])} sections, {total} blocs ;"
          f" {len(rep['reviews'])} blocs à vérifier ; {len(rep['removed'])} mentions retirées → {OUT.relative_to(CONTENT)}")
    if "-v" in sys.argv:
        for unit, rows in result["blocks"].items():
            print(f"  {unit:24} {len(rows):4} blocs")
        for unit, key, title, n in rep["sections"]:
            print(f"  {unit:24} {key:28} {n:3} {title}")
        for bid, why in rep["reviews"]:
            print(f"  à vérifier {bid} : {why}")
        for key, text in rep["removed"]:
            print(f"  retiré {key} : {text}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
