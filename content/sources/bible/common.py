"""Outils partagés par crampon.py et vulgata.py : archives, écriture des données, contrôles, métadonnées."""

from __future__ import annotations

import collections
import json
import re
from pathlib import Path

import books

CONTENT = Path(__file__).resolve().parents[2]
RAW = CONTENT / ".cache" / "raw" / "bible"
DATA = CONTENT / "data"
GIANT = 900  # caractères : au-delà, un verset est suspect (chapitres fusionnés du module SWORD)


def fetch_log() -> dict:
    return json.loads((RAW / "fetch-log.json").read_text(encoding="utf-8"))


def archive(name: str) -> Path:
    entry = fetch_log()[name]
    path = RAW / entry["file"]
    if not path.exists():
        raise SystemExit(f"{path} absent : lancez d'abord python3 -I content/sources/bible/fetch.py")
    return path


def plain(inlines: list) -> str:
    out = []
    for node in inlines:
        if isinstance(node, str):
            out.append(node)
        elif "c" in node:
            out.append(plain(node["c"]))
        elif "br" in node:
            out.append("\n")
    return "".join(out)


ROMAN = {"i": 1, "v": 5, "x": 10, "l": 50, "c": 100, "d": 500, "m": 1000}
_SHOWN = re.compile(r"(?<![A-Za-zÀ-ÿ])([ivxlcdm]+|[IVXLCDM]+)\.?(?=\s*(?:[,(\-–—;]|$))")


def roman(s: str) -> int:
    s = s.lower()
    total = 0
    for i, ch in enumerate(s):
        v = ROMAN[ch]
        total += -v if i + 1 < len(s) and ROMAN[s[i + 1]] > v else v
    return total


def shown_chapter(label: str) -> int | None:
    """Chapitre affiché dans un renvoi des notes (« Ps. li, 12 » → 51), ou None s'il n'est pas lisible."""
    m = _SHOWN.search(label)
    return roman(m.group(1)) if m else None


class Report:
    def __init__(self, pub_id: str) -> None:
        self.pub_id = pub_id
        self.warnings: list[str] = []
        self.counters: collections.Counter = collections.Counter()
        self.patches: list[str] = []
        self.empty: list[str] = []
        self.giants: list[str] = []
        self.notes = 0
        self.links: list[tuple[str, str]] = []
        self.bad_links: list[str] = []
        self.books: dict[str, dict[int, int]] = {}
        self.blocks = 0
        self.continuations = 0
        self.suspects: collections.Counter = collections.Counter()
        self.examples: dict[str, list[str]] = collections.defaultdict(list)

    def warn(self, message: str) -> None:
        self.warnings.append(message)

    def count(self, what: str, n: int = 1) -> None:
        self.counters[what] += n

    def suspect(self, kind: str, where: str, text: str) -> None:
        self.suspects[kind] += 1
        if len(self.examples[kind]) < 8:
            self.examples[kind].append(f"{where} : {text}")

    def book(self, key: str, blocks: list[dict]) -> None:
        chapters: dict[int, int] = collections.Counter()
        for b in blocks:
            self.blocks += 1
            if b["type"] != "verse":
                continue
            if not b["n"].isdigit():
                self.continuations += 1
                continue
            ch = int(b["id"].split(".")[1])
            chapters[ch] += 1
            lang = next(iter(b["t"]))
            text = plain(b["t"][lang])
            if len(text) > GIANT:
                self.giants.append(f"{b['id']} ({len(text)} car.)")
            for m in re.finditer(r"(?<![\d,])\s(\d{1,3})\.\s+[A-ZÀ-Ý]", text):
                self.suspect("numéro de verset dans le texte", b["id"], text[max(0, m.start() - 30):m.end() + 20])
            for m in re.finditer(r"[a-zà-ÿ][,;:!?][a-zà-ÿA-ZÀ-Ý]|[a-zà-ÿ]{2}\.[A-ZÀ-Ý]", text):
                self.suspect("ponctuation collée", b["id"], text[max(0, m.start() - 30):m.end() + 20])
        self.books[key] = dict(chapters)

    def print(self) -> None:
        verses = sum(sum(c.values()) for c in self.books.values())
        chapters = sum(len(c) for c in self.books.values())
        print(f"{self.pub_id} : {len(self.books)} livres, {chapters} chapitres, {verses} versets (+ {self.continuations} suites « b »), {self.notes} notes, {self.blocks} blocs")
        print(f"  renvois gardés : {len(self.links)}")
        for k, v in sorted(self.counters.items()):
            print(f"  {k} : {v}")
        print(f"  retouches : {len(self.patches)}")
        print(f"  versets vides : {len(self.empty)} {self.empty[:20]}")
        print(f"  versets de plus de {GIANT} caractères : {len(self.giants)} {self.giants[:20]}")
        for kind, n in self.suspects.items():
            print(f"  à vérifier — {kind} : {n}")
            for e in self.examples[kind]:
                print(f"      {e!r}")
        for b in self.bad_links[:10]:
            print(f"  renvoi retiré : {b}")
        for w in self.warnings[:200]:
            print(f"  ! {w}")
        if len(self.warnings) > 200:
            print(f"  ! … {len(self.warnings) - 200} autres avertissements")


def dumps_block(block: dict) -> str:
    return json.dumps(block, ensure_ascii=False, separators=(",", ":"))


class DataWriter:
    """Écrit content/data/<publication>/ ; vide d'abord les anciens fichiers de blocs."""

    def __init__(self, pub_id: str) -> None:
        self.root = DATA / pub_id
        self.blocks_dir = self.root / "blocks"
        self.blocks_dir.mkdir(parents=True, exist_ok=True)
        for old in self.blocks_dir.glob("*.jsonl"):
            old.unlink()
        self.ids: set[str] = set()
        self.units: list[str] = []

    def write_unit(self, unit: str, blocks: list[dict]) -> None:
        self.ids.update(b["id"] for b in blocks)
        self.units.append(unit)
        text = "".join(dumps_block(b) + "\n" for b in blocks)
        (self.blocks_dir / f"{unit}.jsonl").write_text(text, encoding="utf-8")

    def check_links(self, report: Report) -> None:
        """Retire les renvois dont la cible (verset ou chapitre) n'existe pas dans cette édition."""
        bad: dict[str, set[str]] = collections.defaultdict(set)
        for where, ref in report.links:
            target = ref.split(":", 1)[1].lower()
            if target not in self.ids:
                bad[where.split(".")[0]].add(ref)
        report.links = [(w, r) for w, r in report.links if r.split(":", 1)[1].lower() in self.ids]
        for unit, refs in bad.items():
            path = self.blocks_dir / f"{unit}.jsonl"
            lines = path.read_text(encoding="utf-8").splitlines()
            out = []
            for line in lines:
                block = json.loads(line)
                if block["type"] == "note":
                    block["t"] = {lang: _unlink(v, refs, report) for lang, v in block["t"].items()}
                out.append(dumps_block(block))
            path.write_text("\n".join(out) + "\n", encoding="utf-8")

    def write_publication(self, meta: dict) -> None:
        (self.root / "publication.json").write_text(json.dumps(meta, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def _unlink(inlines: list, refs: set[str], report: Report) -> list:
    out: list = []
    for node in inlines:
        if isinstance(node, dict) and node.get("link") in refs:
            report.count("renvoi vers un verset absent de l'édition (lien retiré)")
            for child in node["c"]:
                if isinstance(child, str) and out and isinstance(out[-1], str):
                    out[-1] += child
                else:
                    out.append(child)
            continue
        if isinstance(node, str) and out and isinstance(out[-1], str):
            out[-1] += node
            continue
        out.append(node)
    return out


def divisions(order: list[str], lang: str, testament_names: dict[str, str]) -> list[dict]:
    groups: dict[str, list[dict]] = {"at": [], "nt": []}
    for code in order:
        testament, fr, la = books.BOOKS[code]
        title = books.title_plain(fr if lang == "fr" else la)
        groups[testament].append({"id": code.lower(), "kind": "book", "toc": title, "unit": True})
    return [{"id": t, "kind": "testament", "toc": testament_names[t], "children": groups[t]} for t in ("at", "nt")]


def source_entry(name: str, label: str, edition: str) -> dict:
    entry = fetch_log()[name]
    return {
        "name": label,
        "url": entry["repository"],
        "file": entry["url"],
        "edition": edition,
        "commit": entry["commit"],
        "retrieved": entry["retrieved"],
        "rawSha256": entry["sha256"],
    }


def crampon_meta(order: list[str]) -> dict:
    return {
        "id": "bible.crampon",
        "revision": 1,
        "kind": "bible",
        "title": {"fr": "La Sainte Bible"},
        "subtitle": {"fr": "Traduction du chanoine A. Crampon, édition révisée de 1923"},
        "languages": ["fr"],
        "primaryLanguage": "fr",
        "aligned": False,
        "versification": "crampon",
        "contributors": [
            {"name": "Augustin Crampon", "role": "translator"},
            {"name": "Pères de la Compagnie de Jésus, avec des professeurs de Saint-Sulpice", "role": "reviser"},
            {"name": "Société de Saint-Jean-l’Évangéliste, Desclée et Cie", "role": "publisher"},
        ],
        "issued": "1923",
        "source": source_entry(
            "crampon", "CrossWire Bible Society, dépôt « crampon » (USFM)",
            "La Sainte Bible, traduction d’après les textes originaux par le chanoine A. Crampon, édition révisée par des "
            "Pères de la Compagnie de Jésus avec la collaboration de professeurs de Saint-Sulpice, Paris, Tournai, Rome, "
            "Desclée et Cie, 1923 ; transcription fr.wikisource « Bible Crampon 1923 » (BnF bpt6k318720w), extraite en USFM par CrossWire",
        ),
        "license": {
            "spdx": "LicenseRef-PublicDomain",
            "status": "Œuvre collective publiée en 1923 sans nom de réviseur (initiative et nom de l’éditeur, CPI L113-2 al. 3) : "
                      "domaine public en France depuis le 1er janvier 1994 (L123-3), aux États-Unis par la date de publication. "
                      "Risque faible selon la revue critique (docs/planning/15-revue-critique.md) ; repli : Glaire 1905.",
            "attribution": "Numérisation : CrossWire Bible Society, dépôt gitlab.com/crosswire-bible-society/crampon "
                           "(module FreCrampon, DistributionLicense=Public Domain ; OCR de Jean-Marie Weber), d’après la transcription "
                           "relue de fr.wikisource. Retouches de Paroissien : content/sources/bible/README.md.",
        },
        "divisions": divisions(order, "fr", {"at": "Ancien Testament", "nt": "Nouveau Testament"}),
    }


def vulgata_meta(order: list[str]) -> dict:
    return {
        "id": "bible.vulgata",
        "revision": 1,
        "kind": "bible",
        "title": {"la": "Biblia Sacra Vulgatæ editionis"},
        "subtitle": {"la": "Vulgata Clementina"},
        "languages": ["la"],
        "primaryLanguage": "la",
        "aligned": False,
        "versification": "vulgata",
        "contributors": [
            {"name": "Alberto Colunga et Laurentio Turrado", "role": "editor"},
            {"name": "Michael Tweedale (Clementine Text Project)", "role": "transcriber"},
        ],
        "issued": "1592",
        "source": source_entry(
            "vulgclementine", "CrossWire Bible Society, dépôt « vulgclementine » (OSIS)",
            "Biblia Sacra juxta Vulgatam Clementinam, éd. A. Colunga et L. Turrado, Madrid, BAC, 1946, transcrite par le "
            "Clementine Text Project (M. Tweedale) ; module CrossWire VulgClementine 2.1.0 (2022-09-19)",
        ),
        "license": {
            "spdx": "LicenseRef-PublicDomain",
            "status": "Vulgate clémentine (1592-1598), domaine public ; texte numérique placé dans le domaine public par le Clementine Text Project.",
            "attribution": "Clementine Text Project (M. Tweedale), d’après l’édition Colunga-Turrado (BAC, 1946) ; module CrossWire "
                           "VulgClementine (DistributionLicense=Public Domain), dépôt gitlab.com/crosswire-bible-society/vulgclementine. "
                           "Titres des livres ajoutés par Paroissien.",
        },
        "divisions": divisions(order, "la", {"at": "Vetus Testamentum", "nt": "Novum Testamentum"}),
    }
