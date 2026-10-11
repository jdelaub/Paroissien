#!/usr/bin/env python3
"""Vulgate clémentine : OSIS du dépôt GitLab CrossWire `vulgclementine` → content/data/bible.vulgata/.

    python3 -I content/sources/bible/vulgata.py

L'OSIS est lu avec expat dans l'ordre du document (versets en jalons sID/eID à cheval sur <p>, <lg> et <l>),
et chaque livre est écrit dès qu'il est terminé. Le texte n'a pas d'accents : on n'en ajoute aucun.
"""

from __future__ import annotations

import re
import sys
import tarfile
import xml.parsers.expat
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import books  # noqa: E402
import common  # noqa: E402
import typo  # noqa: E402
from textrun import Runs  # noqa: E402

SOURCE = "vulgclementine"
PUB_ID = "bible.vulgata"


class Book:
    def __init__(self, code: str) -> None:
        self.code = code
        self.key = code.lower()
        self.chapters: list[dict] = []
        self.prologue: list[tuple[str, str]] = []  # ("title"|"p", texte)


class Reader:
    """Machine à états sur les évènements expat ; même logique de sauts que crampon.py."""

    def __init__(self, report: common.Report, out: common.DataWriter) -> None:
        self.report = report
        self.out = out
        self.order: list[str] = []
        self.book: Book | None = None
        self.chapter: dict | None = None
        self.verse: dict | None = None       # verset qui reçoit le texte
        self.open_verse: dict | None = None  # verset ouvert (sID vu, eID pas encore)
        self.last_verse: dict | None = None
        self.pending = 0
        self.after_heading = True
        self.capture: list[str] | None = None  # texte d'un <title> ou <speaker>
        self.capture_kind = ""
        self.intro = 0
        self.intro_text: list[str] | None = None
        self.depth_header = False

    # --- évènements -----------------------------------------------------------------------------

    def start(self, name: str, attrs: dict) -> None:
        if name == "header":
            self.depth_header = True
        if self.depth_header:
            return
        if name == "div" and attrs.get("type") == "book":
            self.book = Book(books.OSIS_TO_USFM[attrs["osisID"]])
            self.chapter = None
        elif name == "div" and attrs.get("type") == "introduction":
            self.intro += 1
        elif name == "chapter" and "sID" in attrs:
            n = int(attrs["osisID"].split(".")[-1])
            self.chapter = {"n": n, "items": []}
            self.book.chapters.append(self.chapter)
            self.verse = self.open_verse = self.last_verse = None
            self.pending = 0
            self.after_heading = True
        elif name == "verse" and "sID" in attrs:
            self.start_verse(attrs["osisID"].split(".")[-1])
        elif name == "verse" and "eID" in attrs:
            self.open_verse = None
        elif name in ("p", "lg"):
            if self.intro:
                self.intro_text = []
            else:
                self.pending = 2
        elif name == "l":
            self.pending = max(self.pending, 1)
        elif name in ("title", "speaker"):
            self.capture = []
            self.capture_kind = "speaker" if name == "speaker" else attrs.get("type", "title")
        elif name in ("chapter", "osis", "osisText"):
            pass
        else:
            self.report.warn(f"{self.book and self.book.key} : élément inattendu <{name}>")

    def end(self, name: str) -> None:
        if name == "header":
            self.depth_header = False
            return
        if self.depth_header:
            return
        if name in ("title", "speaker") and self.capture is not None:
            text = " ".join("".join(self.capture).split())
            self.capture = None
            if not text:
                return
            if self.intro:
                self.book.prologue.append(("title", text))
            else:
                self.chapter["items"].append({"type": "heading", "marker": self.capture_kind, "raw": text})
                self.verse = None
                self.after_heading = True
        elif name == "p" and self.intro and self.intro_text is not None:
            text = " ".join("".join(self.intro_text).split())
            if text:
                self.book.prologue.append(("p", text))
            self.intro_text = None
        elif name == "div" and self.intro:
            self.intro -= 1
        elif name == "div" and self.book is not None:
            self.finish_book()

    def data(self, text: str) -> None:
        if self.depth_header:
            return
        if self.capture is not None:
            self.capture.append(text)
            return
        if self.intro:
            if self.intro_text is not None:
                self.intro_text.append(text)
            elif text.strip():
                self.report.warn(f"{self.book.key} : texte d'introduction hors paragraphe {text[:40]!r}")
            return
        if not text.strip():
            return
        if self.verse is None:
            if self.open_verse is not None:
                # Suite d'un verset coupé par un intertitre (locuteur du Cantique, lettre acrostiche).
                base = self.open_verse["n"]
                part = "bcdefgh"[sum(1 for i in self.chapter["items"] if i["type"] == "verse" and i["n"].rstrip("bcdefgh") == base) - 1]
                self.verse = {"type": "verse", "n": base + part, "p": True, "segments": [], "cont": True}
                self.chapter["items"].append(self.verse)
                self.report.count("suites de verset après un intertitre (bloc <v>b)")
                self.after_heading = False
                self.pending = 0
            elif self.chapter is not None and self.last_verse is None:
                # « Alleluja. » avant Ps 118, 1 : rendu en intertitre, comme dans crampon.py.
                self.chapter["items"].append({"type": "heading", "marker": "p", "raw": text})
                self.report.count("texte avant le premier verset du chapitre, rendu en intertitre")
                return
            else:
                self.report.warn(f"{self.book.key} {self.chapter and self.chapter['n']} : texte hors verset ignoré {text[:50]!r}")
                return
        segs = self.verse["segments"]
        if self.pending and any(isinstance(s, str) and s.strip() for s in segs):
            segs.append(BREAK)
        segs.append(text)
        self.pending = 0

    def start_verse(self, raw_n: str) -> None:
        if not raw_n.isdigit():
            raise ValueError(f"{self.book.key} : numéro de verset inattendu {raw_n!r}")
        p = self.after_heading or self.pending == 2
        if self.last_verse is not None and not self.after_heading and self.pending == 1:
            self.last_verse["segments"].append(BREAK)
        verse = {"type": "verse", "n": raw_n, "p": p, "segments": []}
        self.chapter["items"].append(verse)
        self.verse = self.open_verse = self.last_verse = verse
        self.pending = 0
        self.after_heading = False

    # --- blocs ----------------------------------------------------------------------------------

    def finish_book(self) -> None:
        book = self.book
        self.book = None
        title = books.BOOKS[book.code][2]
        blocks: list[dict] = [{"id": f"{book.key}.titre", "type": "heading", "level": 1, "t": {"la": [title]}}]
        para = 0
        for kind, text in book.prologue:
            if kind == "title":
                blocks.append({"id": f"{book.key}.prologus", "type": "heading", "level": 3, "x-osis": "x-IS",
                               "t": {"la": runs_inlines(text)}})
            else:
                para += 1
                blocks.append({"id": f"{book.key}.prologus.p{para}", "type": "para", "t": {"la": runs_inlines(text)}})
        first = True
        for chapter in book.chapters:
            ch = chapter["n"]
            label = f"Psalmus {ch}" if book.code == "PSA" else f"Caput {ch}"
            blocks.append({"id": f"{book.key}.{ch}", "type": "heading", "level": 2, "label": label, "t": {"la": [label]}})
            h = 0
            for item in chapter["items"]:
                if item["type"] == "heading":
                    h += 1
                    blocks.append({"id": f"{book.key}.{ch}.h{h}", "type": "heading", "level": 3, "x-osis": item["marker"],
                                   "t": {"la": runs_inlines(item["raw"])}})
                    continue
                runs = Runs()
                for seg in item["segments"]:
                    if seg is BREAK:
                        runs.br()
                    else:
                        runs.text(seg)
                typo.spacing(runs, french=False)
                vid = f"{book.key}.{ch}.{item['n']}"
                block = {"id": vid, "type": "verse", "n": item["n"]}
                if item["p"]:
                    block["p"] = True
                if first:
                    block["dropCap"] = True
                    first = False
                number = [] if item.get("cont") else [{"num": item["n"]}]
                block["t"] = {"la": number + runs.inlines()}
                if not runs.has_content():
                    self.report.empty.append(vid)
                blocks.append(block)
        self.out.write_unit(book.key, blocks)
        self.report.book(book.key, blocks)
        self.order.append(book.code)


BREAK = object()


def runs_inlines(text: str) -> list:
    runs = Runs()
    runs.text(text)
    typo.spacing(runs, french=False)
    return runs.inlines()


def main() -> int:
    report = common.Report(PUB_ID)
    out = common.DataWriter(PUB_ID)
    reader = Reader(report, out)
    with tarfile.open(common.archive(SOURCE)) as tar:
        member = next(m for m in tar.getmembers() if m.isfile() and m.name.endswith(".osis.xml"))
        data = typo.prepare(tar.extractfile(member).read().decode("utf-8"))
    parser = xml.parsers.expat.ParserCreate()
    parser.StartElementHandler = reader.start
    parser.EndElementHandler = reader.end
    parser.CharacterDataHandler = reader.data
    parser.Parse(data.encode("utf-8"), True)
    for code in reader.order:
        if not re.fullmatch(r"[1-3A-Z]{3}", code):
            raise ValueError(code)
    out.write_publication(common.vulgata_meta(reader.order))
    report.print()
    return 0


if __name__ == "__main__":
    sys.exit(main())
