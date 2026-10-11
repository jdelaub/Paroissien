#!/usr/bin/env python3
"""Crampon 1923 : USFM du dépôt GitLab CrossWire `crampon` → content/data/bible.crampon/.

    python3 -I content/sources/bible/crampon.py

Lit l'archive mise en cache par fetch.py (sans l'extraire), livre par livre. Les défauts de la source
sont corrigés par des retouches explicites (PATCHES), consignées dans le README.
"""

from __future__ import annotations

import re
import sys
import tarfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import books  # noqa: E402
import common  # noqa: E402
import typo  # noqa: E402
from textrun import Runs  # noqa: E402

SOURCE = "crampon"
PUB_ID = "bible.crampon"

# Retouches de la source, appliquées au texte USFM préparé (typo.prepare : espaces insécables → espaces,
# apostrophes droites → ’). (livre, ancien, nouveau, motif) ; l'ancien texte doit apparaître exactement une fois.
PATCHES: list[tuple[str, str, str, str]] = [
    # Versets dédoublés ou coupés par l'extraction de Wikisource (signalés par \vp).
    ("GEN", "\\v 25 \\vp 23\\vp* Elle conçut et enfanta un fils, et elle dit : « Dieu a ôté mon opprobre. » \n"
            "\\v 26 \\vp 24\\vp* Et elle le nomma Joseph, en disant : « Que Yahweh m’ajoute encore un autre fils ! »\n",
     "", "Gn 30, 23-24 répétés en 25-26 : doublon supprimé"),
    ("GEN", "\\v 33 \\vp 30\\vp*", "\\v 31", "Gn 30, 31 imprimé « 30 »"),
    ("GEN", "\\v 43 \\vp 40\\vp*", "\\v 41", "Gn 30, 41 imprimé « 40 »"),
    ("1SA", "\\v 2 \\vp 1\\vp* \n", "", "1 S 2, 1 coupé en deux versets"),
    ("EZR", "\\v 33 \\vp \\vp* trois cent vingt", "trois cent vingt", "Esd 2, 32 coupé en deux versets"),
    ("EZR", "\\v 37 \\vp \\vp* trente.", "trente.", "Esd 2, 35 coupé en deux versets"),
    ("EZR", "\\v 12 \\vp \\vp* à l’autre", "à l’autre", "Esd 9, 11 coupé en deux versets"),
    ("NEH", "\\v 17  Je leur dis \n", "", "Ne 2, 17 : début tronqué répété avant le verset complet"),
    ("1MA", "\\v 24 \\vp 22\\vp*", "\\v 24", "1 M 16, 24 : numéro imprimé erroné (22)"),
    ("PSA", "\\v 7 \\vp \\vp* — ", "— ", "Ps 60, 6 : « — Séla » détaché en verset"),
    ("BAR", "\\v 19 \\vp \\vp*", "\\v 19", "Ba 2, 19 : \\vp vide"),
    ("EZK", "\\v 17 \\vp 16\\vp* Il mesura le côté du septentrion : cinq cents roseaux avec le roseau à mesurer tout autour. "
            "avec le roseau à mesurer : cinq cents roseaux avec le roseau à mesurer tout autour. \n",
     "", "Ez 42 : verset fantôme (fragments des versets 16 et 17)"),
    # Poèmes en prose écrits en \qa avec les versets suivants collés, puis répétés sans espaces.
    ("NUM", "\\qa Monte, puits ! Acclamez-le ! 18. Le puits, que des princes ont creusé, que les grands du peuple ont ouvert, "
            "avec le sceptre, avec leurs bâtons !\n\\b\n\n\\p \n\\v 18 Le puits, que des princes ont creusé,que les grands du peuple ont ouvert,\n",
     "\\q1 Monte, puits ! Acclamez-le !\n\\q1 \n\\v 18 Le puits, que des princes ont creusé,\n\\q1 que les grands du peuple ont ouvert,\n"
     "\\q1 avec le sceptre, avec leurs bâtons !\n", "Nb 21, 17-18 : cantique du puits reconstruit"),
    ("NUM", "\\q1 \n\\qa Que la ville de Séhon soit rebâtie et fortifiée ! 28. Car il est sorti un feu de Hésebon, une flamme de la ville "
            "de Séhon ; elle a dévoré Ar-Moab. Les maîtres des hauteurs de l’arnon. 29. Malheur à toi, Moab ! Tu es perdu, peuple de "
            "Chamos ! Il a livré ses fils fugitifs et ses filles captives à Séhon, roi des Amorrhéens. 30. Et nous avons lancé sur eux "
            "nos traits ; Hésebon est détruite jusqu’à Dibon ; nous avons dévasté jusqu’à Nophé, avec le feu jusqu’à Médaba[59].\n\\b\n\n\\p \n",
     "\\q1 Que la ville de Séhon soit rebâtie et fortifiée !\n\\q1 \n", "Nb 21, 27-30 : poème écrit en \\qa, versets 28-30 collés"),
    ("NUM", "\\v 28 Car il est sorti un feu de Hésebon,une flamme de la ville de Séhon ;elle a dévoré Ar-Moab.Les maîtres des hauteurs de l’arnon.\n"
            "\\v 29 Malheur à toi, Moab !Tu es perdu, peuple de Chamos !Il a livré ses fils fugitifset ses filles captivesà Séhon, roi des Amorrhéens.\n"
            "\\v 30 Et nous avons lancé sur eux nos traits ;Hésebon est détruite jusqu’à Dibon ;nous avons dévasté jusqu’à Nophé,\n",
     "\\v 28 Car il est sorti un feu de Hésebon,\n\\q1 une flamme de la ville de Séhon ;\n\\q1 elle a dévoré Ar-Moab.\n"
     "\\q1 Les maîtres des hauteurs de l’arnon.\n\\q1 \n\\v 29 Malheur à toi, Moab !\n\\q1 Tu es perdu, peuple de Chamos !\n"
     "\\q1 Il a livré ses fils fugitifs\n\\q1 et ses filles captives\n\\q1 à Séhon, roi des Amorrhéens.\n\\q1 \n"
     "\\v 30 Et nous avons lancé sur eux nos traits ;\n\\q1 Hésebon est détruite jusqu’à Dibon ;\n"
     "\\q1 nous avons dévasté jusqu’à Nophé,\n\\q1 avec le feu jusqu’à Médaba.\n",
     "Nb 21, 28-30 : vers recollés sans espaces, fin du verset 30 reprise du \\qa"),
    ("1KI", "\\qa « Quelle part avons-nous avec David ? Nous n’avons point d’héritage avec le fils d’Isaï ! À tes tentes, Israël ! "
            "Quant à toi, pourvois à ta maison, David ! »\n\\b\n\n\\p Nous n’avons point d’héritage avec le fils d’Isaï !À tes tentes, Israël !\n",
     "\\q1 « Quelle part avons-nous avec David ?\n\\q1 Nous n’avons point d’héritage avec le fils d’Isaï !\n"
     "\\q1 À tes tentes, Israël !\n\\q1 Quant à toi, pourvois à ta maison, David ! »\n",
     "1 R 12, 16 : réponse du peuple écrite en \\qa puis répétée"),
    # Note de Ps 120, 1 restée dans le texte du verset.
    ("PSA", "\\q1 Cantique des montées. CXX, 1. \\add Cantique des montées\\add*. — Ce titre",
     "\\q1 Cantique des montées.\\f + \\ft CXX, 1. \\fq Cantique des montées\\ft . — Ce titre", "Ps 120, 1 : note fondue dans le verset"),
    ("PSA", "à la Ville Sainte.</ref>", "à la Ville Sainte.\\f*", "Ps 120, 1 : fin de la note"),
    ("EXO", "\\p 23\n", "\\p \n", "Ex 23, 14 : « 23 » parasite"),
    ("2KI", "\\p — \\add Phacée d’Israël ; Les Assyriens s’emparent d’une partie du pays.\\add* — \n", "",
     "2 R 15, 27 : intertitre répété en texte"),
    ("1CH", "\\add \\add* issues\\add de Thola", "issues \\add de Thola", "1 Ch 7, 2 : italique mal placé, espace perdue"),
    ("EZK", "\\add et pa\\add*r leurs", "\\add et par\\add* leurs", "Ez 43, 7 : italique décalé d'une lettre"),
    # Restes de wikitexte.
    ("SIR", "\\is1 PROLOGUE[2] \n", "", "Si : titre « Prologue » sans le texte du prologue"),
    ("SIR", "\\p </poem> \n", "\\p \n", "Si 46 : balise </poem>"),
    ("SIR", "(comp. [[\\+xt Col. i, 15|COL 1:15\\+xt*)", "(comp. \\+xt Col. i, 15|COL 1:15\\+xt*)", "lien wiki"),
    ("SIR", "voir [[Bible_Crampon_1923/2_Rois#27-41|II Rois, xvii, 41,", "voir II Rois, xvii, 41,", "lien wiki"),
    ("SIR", "(comp. [[Bible_Crampon_1923/Ecclésiastique#34-12|xxxiv, 12).", "(comp. xxxiv, 12).", "lien wiki"),
    ("2KI", "([[Bible_Crampon_1923/1 Chroniques#26-16|I Par. xxvi, 16, 18).", "(I Par. xxvi, 16, 18).", "lien wiki"),
    ("1CH", "Ps. c{sc|cv}}|PSA 105:1", "Ps. cv|PSA 105:1", "modèle wiki"),
    ("1MA", "NEH 6:15\\+xt*]])", "NEH 6:15\\+xt*)", "lien wiki"),
    ("ISA", "chap, xxi, xxi}i", "chap, xxi, xxii", "accolade parasite"),
    ("JER", "(\\+xt II Rois, xxvxxiv|2KI 24:1\\+xt*, 18 — xxv, 30]])", "(\\+xt II Rois, xxiv|2KI 24:18\\+xt*, 18 — xxv, 30)", "lien wiki"),
    ("LUK", "[[Bible_Crampon_1923/Jean#16-21|Jean, xiv, 21}}.", "Jean, xiv, 21.", "lien wiki"),
    ("ACT", "[[ Antiquités judaïques/Livre VII|Josèphe, Ant. vii, 15, 3]] ;", "Josèphe, Ant. vii, 15, 3 ;", "lien wiki"),
    ("ACT", "xvi, 7, 1]].\\f*", "xvi, 7, 1.\\f*", "lien wiki"),
    ("ROM", "\\+qt* {produisant des fruits) ’de mort, sous laquelle\\+qt , etc.\\+qt*",
     "\\+qt* (produisant des fruits) \\+qt de mort, sous laquelle\\+qt*, etc.", "italique wiki"),
    ("REV", "emprunté à vii}}, 14)", "emprunté à vii, 14)", "accolades parasites"),
]

PARA_MARKERS = {"id", "c", "cp", "p", "q1", "b", "s1", "s2", "ms1", "qa", "is1", "is3", "v"}
HEADING_MARKERS = {"ms1", "s1", "s2", "qa", "is1", "is3"}
INLINE = re.compile(r"\\(\+?)([a-z]+[0-9]*)(\*?)")
STRAY_CALL = re.compile(r"\s*\(\[\d+\]\)|\[\d+\]")  # appels de note de Wikisource restés dans le texte


class Book:
    """État de conversion d'un livre."""

    def __init__(self, code: str, report: common.Report) -> None:
        self.code = code
        self.key = code.lower()
        self.report = report
        self.chapters: list[dict] = []  # {"n", "items": [heading|verse]}
        self.pre: list[tuple[str, str]] = []  # intertitres avant \c 1

    # --- lecture des lignes -------------------------------------------------------------------

    def parse(self, text: str) -> None:
        chapter = None
        verse = None
        pending = 0  # 0 rien, 1 saut de ligne poétique, 2 paragraphe ou strophe
        empty_q = 0
        after_heading = True

        def add_text(raw: str) -> None:
            nonlocal pending, empty_q, verse, after_heading
            if not raw.strip():
                return
            if verse is None and is_summary(raw):
                chapter["items"].append({"type": "heading", "marker": "p", "raw": raw})
                self.report.count("sommaires écrits en \\p, rendus en intertitres")
                return
            if verse is None:
                # Texte après un intertitre placé au milieu d'un verset (« 19b ») : bloc de suite du verset.
                previous = next((i for i in reversed(chapter["items"]) if i["type"] == "verse"), None) if chapter else None
                if previous is None:
                    self.report.count("texte avant le premier verset du chapitre, rendu en intertitre")
                    chapter["items"].append({"type": "heading", "marker": "p", "raw": raw})
                    return
                base = previous["n"].rstrip("bcdefgh")
                part = "bcdefgh"[sum(1 for i in chapter["items"] if i["type"] == "verse" and i["n"].rstrip("bcdefgh") == base) - 1]
                verse = {"type": "verse", "n": base + part, "p": True, "segments": [], "cont": True}
                chapter["items"].append(verse)
                self.report.count("suites de verset après un intertitre (bloc <v>b)")
                after_heading = False
                pending = 0
            if pending and verse["segments"] and any(s.strip() for s in verse["segments"] if isinstance(s, str)):
                verse["segments"].append(BREAK)
            verse["segments"].append(raw)
            pending = 0
            empty_q = 0

        def start_verse(raw_n: str) -> None:
            nonlocal verse, pending, empty_q, after_heading
            if chapter is None:
                raise ValueError(f"{self.key} : verset avant le premier chapitre")
            m = re.fullmatch(r"(\d+)", raw_n)
            if not m:
                raise ValueError(f"{self.key} {chapter['n']} : numéro de verset inattendu {raw_n!r}")
            p = after_heading or pending == 2
            if verse is not None and not after_heading and pending == 1:
                verse["segments"].append(BREAK)  # le verset suivant commence un nouveau vers
            verse = {"type": "verse", "n": m.group(1), "p": p, "segments": []}
            chapter["items"].append(verse)
            pending = 0
            empty_q = 0
            after_heading = False

        for line in text.split("\n"):
            line = line.rstrip()
            if not line.strip():
                continue
            m = re.match(r"\\([a-z]+[0-9]*)(?:\s|$)(.*)", line)
            marker, rest = (m.group(1), m.group(2)) if m and m.group(1) in PARA_MARKERS else (None, line)
            if marker == "id":
                continue
            if marker == "c":
                chapter = {"n": int(rest.split()[0]), "items": []}
                self.chapters.append(chapter)
                verse = None
                pending = 0
                after_heading = True
                continue
            if marker == "cp":
                if rest.strip() != str(chapter["n"]):
                    self.report.warn(f"{self.key} {chapter['n']} : \\cp {rest!r} différent de \\c")
                continue
            if marker in HEADING_MARKERS:
                if not rest.strip():
                    continue  # \qa vide
                if chapter is None:
                    self.pre.append((marker, rest))
                else:
                    chapter["items"].append({"type": "heading", "marker": marker, "raw": rest})
                    verse = None
                    after_heading = True
                continue
            if marker == "p" and is_summary(rest):
                chapter["items"].append({"type": "heading", "marker": "p", "raw": rest})
                self.report.count("sommaires écrits en \\p, rendus en intertitres")
                verse = None
                after_heading = True
                continue
            if marker == "p" or marker == "b":
                pending = 2
                empty_q = 0
                add_text_with_verses(rest, add_text, start_verse)
                continue
            if marker == "q1":
                if rest.strip():
                    pending = max(pending, 1)
                else:
                    empty_q += 1
                    pending = max(pending, 2 if empty_q >= 2 else 1)
                add_text_with_verses(rest, add_text, start_verse)
                continue
            if marker == "v":
                add_text_with_verses("\\v " + rest, add_text, start_verse)
                continue
            add_text_with_verses(" " + rest, add_text, start_verse)  # ligne de suite (\qs…)

    # --- blocs ----------------------------------------------------------------------------------

    def blocks(self, title: str) -> list[dict]:
        out: list[dict] = [{"id": f"{self.key}.titre", "type": "heading", "level": 1,
                            "t": {"fr": books.title_inlines(title)}}]
        first_verse = True
        for chapter in self.chapters:
            ch = chapter["n"]
            label = f"Psaume {ch}" if self.code == "PSA" else f"Chapitre {ch}"
            out.append({"id": f"{self.key}.{ch}", "type": "heading", "level": 2, "label": label, "t": {"fr": [label]}})
            notes: list[dict] = []
            h = 0
            items = chapter["items"]
            if ch == 1 and self.pre:
                items = [{"type": "heading", "marker": mk, "raw": raw} for mk, raw in self.pre] + items
            for item in items:
                if item["type"] == "heading":
                    h += 1
                    hid = f"{self.key}.{ch}.h{h}"
                    runs = self.convert([item["raw"]], hid, notes)
                    if not runs.has_content():
                        h -= 1
                        continue
                    out.append({"id": hid, "type": "heading", "level": 3, "x-usfm": item["marker"],
                                "t": {"fr": runs.inlines()}})
                    continue
                vid = f"{self.key}.{ch}.{item['n']}"
                runs = self.convert(item["segments"], vid, notes)
                block = {"id": vid, "type": "verse", "n": item["n"]}
                if item["p"]:
                    block["p"] = True
                if first_verse:
                    block["dropCap"] = True
                    first_verse = False
                number = [] if item.get("cont") else [{"num": item["n"]}]
                block["t"] = {"fr": number + runs.inlines()}
                if not runs.has_content():
                    self.report.empty.append(vid)
                out.append(block)
            out.extend(notes)
        return out

    def convert(self, segments: list, owner: str, notes: list[dict]) -> Runs:
        runs = Runs()
        k = 0
        for seg in segments:
            if seg is BREAK:
                runs.br()
                continue
            k = self.inline(STRAY_CALL.sub(lambda m: self.report.count("appel de note orphelin [n] retiré") or "", seg),
                            runs, owner, notes, k)
        typo.spacing(runs)
        return runs

    def inline(self, raw: str, runs: Runs, owner: str, notes: list[dict], k: int) -> int:
        """Marqueurs de caractère du texte : \\add, \\qs (italique), \\f…\\f* (note), \\vp (ignoré)."""
        italic = 0
        pos = 0
        while True:
            m = INLINE.search(raw, pos)
            chunk = raw[pos:m.start() if m else len(raw)]
            if chunk:
                runs.text(chunk, italic=italic > 0)
            if not m:
                return k
            plus, name, close = m.groups()
            pos = m.end()
            if not close and raw[pos:pos + 1] == " ":
                pos += 1
            if name == "f" and not close:
                end = raw.find("\\f*", pos)
                if end < 0:
                    raise ValueError(f"{owner} : note non fermée")
                k += 1
                note_id = f"{owner}.n{k}"
                label = str(len(notes) + 1)
                notes.append(self.note(raw[pos:end], note_id, label))
                runs.note(note_id, label)
                pos = end + 3
            elif name in ("add", "qs"):
                italic += -1 if close else 1
            elif name == "vp":
                if not close:
                    end = raw.find("\\vp*", pos)
                    pos = end + 4
            else:
                self.report.warn(f"{owner} : marqueur inattendu \\{plus}{name}{close}")

    def note(self, raw: str, note_id: str, label: str) -> dict:
        raw = re.sub(r"^[+\-]\s?", "", raw)
        runs = Runs()
        mode_italic = False
        span_italic = 0
        pos = 0
        while True:
            m = INLINE.search(raw, pos)
            chunk = raw[pos:m.start() if m else len(raw)]
            if chunk:
                runs.text(chunk, italic=mode_italic or span_italic > 0)
            if not m:
                break
            plus, name, close = m.groups()
            pos = m.end()
            if not close and raw[pos:pos + 1] == " ":
                pos += 1
            if name == "fq":
                mode_italic = not close
            elif name == "ft":
                mode_italic = False
            elif name == "qt":
                span_italic += -1 if close else 1
            elif name == "xt" and not close:
                end = raw.find("\\" + plus + "xt*", pos)
                label_text, _, target = raw[pos:end].rpartition("|")
                if not label_text:
                    label_text, target = target, ""
                link = self.link(label_text, target, note_id)
                runs.text(label_text, italic=mode_italic or span_italic > 0, link=link)
                pos = end + len(plus) + 4
            else:
                self.report.warn(f"{note_id} : marqueur inattendu \\{plus}{name}{close}")
        typo.spacing(runs)
        self.report.notes += 1
        return {"id": note_id, "type": "note", "placement": "foot", "label": label, "t": {"fr": runs.inlines()}}

    def link(self, label: str, target: str, where: str) -> str | None:
        """Renvoi « Ps. li, 12|PSA 51:12 » → bible:PSA.51.12, si le chapitre affiché concorde avec la cible."""
        m = re.fullmatch(r"([1-3A-Z]{3}) (\d+)(?::(\d+))?", target.strip())
        if not m or m.group(1) not in books.BOOKS:
            self.report.count("renvoi sans cible exploitable (texte gardé)")
            return None
        shown = common.shown_chapter(label)
        if shown is not None and shown != int(m.group(2)):
            self.report.count("renvoi incohérent avec le texte affiché (lien retiré)")
            self.report.bad_links.append(f"{where} : « {label} » → {target}")
            return None
        ref = f"bible:{m.group(1)}.{m.group(2)}" + (f".{m.group(3)}" if m.group(3) else "")
        self.report.links.append((where, ref))
        return ref


BREAK = object()
_NOTE_SPAN = re.compile(r"\\f .*?\\f\*")
_ADD_SPAN = re.compile(r"\\add .*?\\add\*")


def is_summary(rest: str) -> bool:
    """Sommaire analytique écrit en \\p : « 2. Chap. xiv. … », « Ps. xlvi (Vulg. xlv) : … », ou ligne
    entièrement en italique suivie de renvois entre parenthèses (« \\add Tempérance…\\add* (xxiii, 1-3). »)."""
    text = _NOTE_SPAN.sub("", rest).strip()
    if re.match(r"\d+\. Chap[.,] ", text) or re.match(r"Ps\. [ivxlc]+\b", text):
        return True
    if "\\add " not in text:
        return False
    residue = re.sub(r"\([^)]*\)", "", _ADD_SPAN.sub("", text))
    return re.fullmatch(r"[\s.,;:—–-]*", residue) is not None


def add_text_with_verses(raw: str, add_text, start_verse) -> None:
    """Découpe une ligne aux marqueurs \\v et répartit le texte entre les versets."""
    parts = re.split(r"\\v (\S+) ?", raw)
    add_text(parts[0])
    for i in range(1, len(parts), 2):
        start_verse(parts[i])
        add_text(parts[i + 1])


def apply_patches(code: str, text: str, report: common.Report) -> str:
    for book, old, new, why in PATCHES:
        if book != code:
            continue
        found = text.count(old)
        if found != 1:
            raise ValueError(f"retouche {book} « {why} » : trouvée {found} fois")
        text = text.replace(old, new)
        report.patches.append(f"{book} : {why}")
    # Espace perdue après un italique fermé sur une ponctuation (« \\add ô Dieu,\\add*que »).
    text, n = re.subn(r"([,;.!?])\\add\*(?=[A-Za-zÀ-ÿ])", r"\1\\add* ", text)
    if n:
        report.count("espaces rétablies après un italique", n)
    # En-têtes de colonnes « Psaumes  Livres » de la mise en page, restés entre les livres du Psautier.
    text, n = re.subn(r"\\p \\add Psaumes\\add\* +\\add Livres\\add\*\n", "", text)
    if n:
        report.count("lignes « Psaumes Livres » (mise en page) retirées", n)
    # Numéros de verset imprimés (\vp) : ils font foi une fois les doublons retirés.
    text, n = re.subn(r"\\v \d+ \\vp (\d+)\\vp\*", r"\\v \1", text)
    if n:
        report.count("versets renumérotés d'après \\vp", n)
    return text


def main() -> int:
    report = common.Report(PUB_ID)
    archive = common.archive(SOURCE)
    out = common.DataWriter(PUB_ID)
    order: list[str] = []
    with tarfile.open(archive) as tar:
        members = sorted((m for m in tar.getmembers() if m.isfile() and m.name.endswith(".usfm")), key=lambda m: m.name)
        for member in members:
            raw = typo.prepare(tar.extractfile(member).read().decode("utf-8"))
            code = re.match(r"\\id (\S+)", raw).group(1)
            raw = apply_patches(code, raw, report)
            book = Book(code, report)
            book.parse(raw)
            blocks = book.blocks(books.BOOKS[code][1])
            out.write_unit(book.key, blocks)
            report.book(book.key, blocks)
            order.append(code)
    out.check_links(report)
    meta = common.crampon_meta(order)
    out.write_publication(meta)
    report.print()
    return 0


if __name__ == "__main__":
    sys.exit(main())
