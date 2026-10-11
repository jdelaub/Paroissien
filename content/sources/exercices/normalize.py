#!/usr/bin/env python3
"""Normalise la page livres-mystiques des Exercices spirituels en JSON canonique (content/data/exercices.fr/).

    python3 -I content/sources/exercices/normalize.py [--crampon ARCHIVE.tar.gz]

Entrée : content/.cache/raw/exercices/exercices.html (fetch.py). Sortie : publication.json et blocks/<unité>.jsonl.
Le script vérifie les 370 numéros canoniques (une fois chacun, dans l'ordre) et les plages des unités. Avec l'archive
du dépôt Crampon (par défaut content/.cache/raw/bible/crampon-*.tar.gz, lue sans extraction), il contrôle aussi que
chaque renvoi biblique existe dans la versification du Crampon. Bibliothèque standard seulement ; la page
téléchargée est une donnée, jamais exécutée.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
import tarfile
import unicodedata
from html.parser import HTMLParser
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RAW = ROOT / ".cache" / "raw" / "exercices"
SOURCE = RAW / "exercices.html"
OUT = ROOT / "data" / "exercices.fr"
URL = "http://livres-mystiques.com/partieTEXTES/Exercices_Ignace/exercices.html"
RETRIEVED = "2026-10-11"
NNBSP = "\u202f"
NBSP = "\u00a0"

# ---------------------------------------------------------------------------------------------------------------
# Plan : unités de lecture, chacune ouverte par un titre de la source ; plages canoniques attendues.

UNITS = [
    # (id, clé du titre qui ouvre l'unité, kind, toc, plage, semaine)
    ("annotations", "annotations", "annotations", "Annotations", (1, 20), None),
    ("fondement", "exercices", "section", "Présupposé, principe et fondement", (21, 23), 1),
    ("examen", "examen-particulier", "examen", "Examens et confession générale", (24, 44), 1),
    ("semaine-1", "s1.exercice-1", "week", "Première semaine", (45, 90), 1),
    ("semaine-2", "semaine-2", "week", "Seconde semaine", (91, 189), 2),
    ("semaine-3", "semaine-3", "week", "Troisième semaine", (190, 217), 3),
    ("semaine-4", "semaine-4", "week", "Quatrième semaine", (218, 237), 4),
    ("manieres-de-prier", "oraisons", "methods_of_prayer", "Trois manières de prier", (238, 260), None),
    ("mysteres", "mysteres", "mysteries", "Mystères de la vie de Jésus-Christ notre Seigneur", (261, 312), None),
    ("regles", "discernement", "rules", "Règles : discernement des esprits, aumônes, scrupules, Église", (313, 370), None),
]

# ---------------------------------------------------------------------------------------------------------------
# Corrections consignées dans le README (texte de la source → texte publié). Chacune doit servir exactement une fois.

CITE_FIXES = {
    # [298] chapitre impossible ; l'édition de 1913 porte « S. Joan. xix. 30-42 ».
    "Jean 29, 30-42": "Jean 19, 30-42",
    # [279] la tempête apaisée est Lc 8, 22-25 ; l'erreur figure déjà dans l'édition de 1913 (« S. Luc. vii. 22-25 »).
    "Luc 7, 22-25": "Luc 8, 22-25",
    # [280] l'édition de 1913 porte « S. Matth. xiv. 22-33 » ; les points 2 et 3 couvrent les v. 24-33.
    "Matthieu 14, 22-23": "Matthieu 14, 22-33",
    # [281] deux-points pour point-virgule ; 1913 : « S. Matth. x. 1-42 ; xi. 1 ».
    "Matthieu 10, 1-42: 11, 1": "Matthieu 10, 1-42; 11, 1",
    # [302] 1913 : « S. Luc. xxiv. 12, 34 » (deux versets, et non une plage qui engloberait Emmaüs, cité au [303]).
    "Luc 24, 12-34": "Luc 24, 12; 24, 34",
}
TEXT_FIXES = {
    "prier.L'oraison": "prier. L'oraison",  # [258] espace manquante après le point
}

BOOKS = {"Matthieu": "MAT", "Marc": "MRK", "Luc": "LUK", "Jean": "JHN", "Actes": "ACT", "I Corinthiens": "1CO"}
# Dernier verset des chapitres cités en entier (« Matthieu 5-7 », « Jean 13, 31-38 ; 14-17 ; … »), contrôlé contre le Crampon.
CHAPTER_ENDS = {("MAT", 7): 29, ("JHN", 17): 26}
CRAMPON_FILES = {"MAT": "47_Matt", "MRK": "48_Mark", "LUK": "49_Luke", "JHN": "50_John", "ACT": "51_Acts", "1CO": "53_1Cor"}

LIGATURES = {"boeuf": "bœuf", "coeur": "cœur", "oeil": "œil", "oeuvre": "œuvre", "soeur": "sœur", "voeu": "vœu"}

ORD = {
    "premier": 1, "première": 1, "second": 2, "seconde": 2, "deuxième": 2, "troisième": 3, "quatrième": 4,
    "cinquième": 5, "sixième": 6, "septième": 7, "huitième": 8, "neuvième": 9, "dixième": 10, "onzième": 11,
    "douzième": 12, "treizième": 13,
}
POINT = r"(Premier|Second|Deuxième|Troisième|Quatrième|Cinquième|Sixième) point"

# ---------------------------------------------------------------------------------------------------------------
# Clés des titres : ex.h.<mot-clé>. Tables explicites, pour que chaque clé reste stable.

H2 = {"annotations": "annotations", "exercices spirituels": "exercices", "oraisons": "oraisons",
      "mystères de la vie de jésus-christ notre seigneur": "mysteres", "discernement des esprits": "discernement"}
H3 = {"supposition préalable": "presupposition", "première semaine": "semaine-1", "seconde semaine": "semaine-2",
      "troisième semaine": "semaine-3", "quatrième semaine": "semaine-4", "de trois manières de prier": "manieres-de-prier",
      "autres règles": "discernement-2", "de la distribution des aumônes": "aumones", "sur les scrupules": "scrupules",
      "de la soumission à l’église": "eglise"}
H4 = {"principe et fondement": "fondement", "examen particulier et quotidien": "examen-particulier",
      "quatre additions": "examen-particulier.additions", "examen général de conscience": "examen-general",
      "manière de faire l’examen général": "examen-general.methode", "confession générale et communion": "confession-generale",
      "additions à observer": "s1.additions", "considération": "s2.roi", "première partie": "s2.roi.partie-1",
      "seconde partie": "s2.roi.partie-2", "prélude pour la considération de divers états de vie": "s2.etats-de-vie",
      "des trois degrés d’humilité": "s2.humilite", "de l’élection": "s2.election", "règles de tempérance": "s3.temperance",
      "première manière de prier": "priere-1", "seconde manière de prier": "priere-2", "troisième manière de prier": "priere-3"}
H5 = {"de la pensée": "examen-general.pensee", "de la parole": "examen-general.parole", "de l’action": "examen-general.action",
      "contemplation": "s4.amour", "sur les commandements de dieu": "priere-1.commandements",
      "sur les péchés capitaux": "priere-1.peches-capitaux", "sur les trois puissances de l’âme": "priere-1.puissances",
      "sur les cinq sens": "priere-1.sens"}
H5_PREFIX = {"méditation de deux étendards": "s2.etendards", "prélude, ou principe fondamental": "s2.election.prelude",
             "de la nature des objets": "s2.election.matieres", "de trois temps": "s2.election.temps",
             "premier mode": "s2.election.mode-1", "second mode": "s2.election.mode-2",
             "de l’amendement personnel": "s2.election.reforme"}
MYSTERIES = {
    262: "annonciation", 263: "visitation", 264: "nativite", 265: "bergers", 266: "circoncision", 267: "mages",
    268: "presentation", 269: "fuite-egypte", 270: "retour-egypte", 271: "vie-cachee", 272: "temple-douze-ans",
    273: "bapteme", 274: "tentation", 275: "vocation-apotres", 276: "cana", 277: "vendeurs-temple",
    278: "sermon-montagne", 279: "tempete", 280: "marche-sur-les-eaux", 281: "mission-apotres", 282: "madeleine",
    283: "multiplication-pains", 284: "transfiguration", 285: "lazare", 286: "bethanie", 287: "rameaux",
    288: "predication-temple", 289: "cene", 290: "cene-jardin", 291: "jardin-anne", 292: "anne-caiphe",
    293: "caiphe-pilate", 294: "pilate-herode", 295: "herode-pilate", 296: "pilate-croix", 297: "croix",
    298: "croix-sepulcre", 299: "resurrection", **{300 + i: f"apparition-{i + 2}" for i in range(12)}, 312: "ascension",
}
# Sous-titres h6 des jours 7 à 12 de la 2e semaine : ils suivent le [161] et en sont le texte canonique.
CONTINUATION_SUBTITLES = {f"s2.jour-{d}" for d in range(7, 13)}


def heading_key(level: int, text: str, ctx: dict) -> str:
    t = text.lower().rstrip(". ")
    if level == 2:
        return H2[t]
    if level == 3:
        return H3[t]
    if level == 4:
        if t in H4:
            return H4[t]
        if m := re.fullmatch(r"(\w+) (exercice|jour)", t):
            return f"{ctx['week']}.{m[2]}-{ORD[m[1]]}"
        if t == "remarques":
            return f"{ctx['week']}.remarques"
    if level == 5:
        if m := re.fullmatch(r"(\w+) contemplation", t):
            return f"{ctx['h4']}.contemplation-{ORD[m[1]]}"
        if t == "remarques":
            return f"{ctx['h4']}.remarques"
        if t in H5:
            return H5[t]
        for prefix, key in H5_PREFIX.items():
            if t.startswith(prefix):
                return key
    raise KeyError(f"titre h{level} sans clé : {text!r}")


# ---------------------------------------------------------------------------------------------------------------
# Lecture HTML : un petit arbre, puis une suite de segments (titres, alinéas, texte libre entre les blocs, <pre>).

VOID = {"br", "img", "hr", "input", "meta", "link"}
BLOCK = {"h2", "h3", "h4", "h5", "h6", "p", "pre"}


class Node:
    def __init__(self, tag: str, attrs: dict):
        self.tag, self.attrs, self.children = tag, attrs, []

    def text(self) -> str:
        return "".join(c if isinstance(c, str) else c.text() for c in self.children)


class TreeBuilder(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.root = Node("#root", {})
        self.stack = [self.root]

    def handle_starttag(self, tag, attrs):
        node = Node(tag, dict(attrs))
        self.stack[-1].children.append(node)
        if tag not in VOID:
            self.stack.append(node)

    def handle_startendtag(self, tag, attrs):
        self.stack[-1].children.append(Node(tag, dict(attrs)))

    def handle_endtag(self, tag):
        for i in range(len(self.stack) - 1, 0, -1):
            if self.stack[i].tag == tag:
                del self.stack[i:]
                return

    def handle_data(self, data):
        self.stack[-1].children.append(data)


def find_div(node: Node, div_id: str) -> Node | None:
    for c in node.children:
        if isinstance(c, Node):
            if c.tag == "div" and c.attrs.get("id") == div_id:
                return c
            if (found := find_div(c, div_id)) is not None:
                return found
    return None


def split_br(children: list) -> list[list]:
    parts, cur = [], []
    for c in children:
        if isinstance(c, Node) and c.tag == "br":
            parts.append(cur)
            cur = []
        else:
            cur.append(c)
    parts.append(cur)
    return parts


def segments(div: Node):
    loose: list = []

    def flush():
        for part in split_br(loose):
            yield {"src": "loose", "nodes": part}
        loose.clear()

    for c in div.children:
        if isinstance(c, Node) and c.tag in BLOCK:
            yield from flush()
            if c.tag == "p":
                for i, part in enumerate(split_br(c.children)):
                    yield {"src": "p", "pid": c.attrs.get("id", "") if i == 0 else "", "nodes": part}
            elif c.tag == "pre":
                yield {"src": "pre", "nodes": c.children}
            else:
                yield {"src": "h", "level": int(c.tag[1]), "nodes": c.children}
        else:
            loose.append(c)
    yield from flush()


def is_empty(nodes: list) -> bool:
    """Vide, ou seulement des appels de note."""
    for c in nodes:
        if isinstance(c, str):
            if c.strip():
                return False
        elif c.tag != "a" and (c.tag == "cite" or not is_empty(c.children)):
            return False
    return True


# ---------------------------------------------------------------------------------------------------------------
# Inlines : on aplatit en caractères porteurs d'une clé de style (marques, renvoi), on applique la typographie,
# puis on regroupe. Les appels de note (modifications de 2005) sont retirés et consignés.

class Text:
    def __init__(self):
        self.chars: list[str] = []
        self.keys: list[tuple] = []

    def add(self, s: str, key: tuple):
        self.chars.extend(s)
        self.keys.extend([key] * len(s))

    def sub(self, pattern: str, repl) -> int:
        s = "".join(self.chars)
        chars, keys, pos, n = [], [], 0, 0
        for m in re.finditer(pattern, s):
            r = repl(m) if callable(repl) else m.expand(repl)
            chars.extend(self.chars[pos:m.start()])
            keys.extend(self.keys[pos:m.start()])
            if len(r) == m.end() - m.start():
                keys.extend(self.keys[m.start():m.end()])
            else:
                keys.extend([self.keys[m.end() - 1 if m.end() > m.start() else m.start()]] * len(r))
            chars.extend(r)
            pos = m.end()
            n += r != m.group(0)
        chars.extend(self.chars[pos:])
        keys.extend(self.keys[pos:])
        self.chars, self.keys = chars, keys
        return n


class Converter:
    def __init__(self):
        self.log = {"cites": [], "text": [], "markup": [], "typo": dict.fromkeys(
            ("apostrophes", "guillemets", "espaces fines", "espaces retirées", "points de suspension",
             "tirets", "ligatures œ", "capitales accentuées"), 0)}
        self.notes: list[tuple[str, str]] = []  # (numéro de l'appel, bloc)
        self.links: list[dict] = []

    def flatten(self, nodes: list, text: Text, block: str, marks: tuple = ()):
        for c in nodes:
            if isinstance(c, str):
                text.add(c, (marks, None))
            elif c.tag == "a" and c.attrs.get("href", "").startswith("#n"):
                self.notes.append((c.attrs["href"][2:], block))
            elif c.tag in ("i", "b"):
                self.flatten(c.children, text, block, marks + (c.tag,))
            elif c.tag == "q":  # paroles de l'Évangile : « … » ([261])
                text.add("«", (marks, None))
                self.flatten(c.children, text, block, marks)
                text.add("»", (marks, None))
            elif c.tag == "cite":
                self.cite(c.text(), text, marks, block)
            else:
                raise ValueError(f"balise inattendue <{c.tag}> dans {block}")

    def cite(self, raw: str, text: Text, marks: tuple, block: str):
        raw = " ".join(raw.split())
        fixed = CITE_FIXES.get(raw, raw)
        if fixed != raw:
            self.log["cites"].append({"block": block, "source": raw, "publié": fixed})
        book = None
        for i, item in enumerate(s.strip() for s in fixed.split(";")):
            if i:
                text.add("; ", (marks, None))
            m = re.fullmatch(r"(?:(?P<book>(?:I )?[A-Z][a-zé]+) )?(?P<ref>[\d, -]+)", item)
            book = m["book"] or book
            target = osis(BOOKS[book], m["ref"])
            self.links.append({"block": block, "text": item, "target": target})
            text.add(item, (marks, "bible:" + target))

    def inlines(self, nodes: list, block: str) -> list:
        text = Text()
        self.flatten(nodes, text, block)
        self.typography(text, block)
        return group(text)

    def typography(self, text: Text, block: str) -> None:
        count = self.log["typo"]
        for bad, good in TEXT_FIXES.items():
            if text.sub(re.escape(bad), good):
                self.log["text"].append({"block": block, "source": bad, "publié": good})
        text.sub(r"[\s\u00a0]+", " ")
        while text.chars and text.chars[0] == " ":
            del text.chars[0], text.keys[0]
        while text.chars and text.chars[-1] == " ":
            del text.chars[-1], text.keys[-1]
        count["apostrophes"] += text.sub("'", "’")
        count["guillemets"] += text.sub(r"« ?", "«" + NNBSP)
        text.sub(r" ?»", NNBSP + "»")
        count["espaces retirées"] += text.sub(r" +(?=[.,])", "")
        count["espaces fines"] += text.sub(r"(?<=[^\s\u202f;:!?])\s?(?=[;:!?])", NNBSP)
        count["points de suspension"] += text.sub(r"\.\.\.", "…")
        count["tirets"] += text.sub(r"(?<= )-(?= )", "–")  # séparateur des références des mystères
        for word, lig in LIGATURES.items():
            count["ligatures œ"] += text.sub(rf"\b{word}", lig)
            count["ligatures œ"] += text.sub(rf"\b{word.capitalize()}", lig.capitalize())
        count["capitales accentuées"] += text.sub(r"\bA(?= )", "À")
        count["capitales accentuées"] += text.sub(r"\bEve\b", "Ève")
        # Une espace au bord d'une marque sort de la marque.
        for i, ch in enumerate(text.chars):
            marks, link = text.keys[i]
            if ch in (" ", NNBSP) and marks:
                if i == 0 or i == len(text.chars) - 1 or text.keys[i - 1] != text.keys[i] or text.keys[i + 1] != text.keys[i]:
                    text.keys[i] = ((), link)


def osis(book: str, ref: str) -> str:
    ref = ref.replace(" ", "")
    if m := re.fullmatch(r"(\d+),(\d+)-(\d+)", ref):
        return f"{book}.{m[1]}.{m[2]}-{m[3]}"
    if m := re.fullmatch(r"(\d+),(\d+)", ref):
        return f"{book}.{m[1]}.{m[2]}"
    if m := re.fullmatch(r"(\d+)-(\d+)", ref):  # chapitres entiers
        return f"{book}.{m[1]}.1-{m[2]}.{CHAPTER_ENDS[(book, int(m[2]))]}"
    raise ValueError(f"référence non reconnue {book} {ref!r}")


def group(text: Text) -> list:
    out: list = []
    i = 0
    while i < len(text.chars):
        j = i
        while j < len(text.chars) and text.keys[j] == text.keys[i]:
            j += 1
        marks, link = text.keys[i]
        s = unicodedata.normalize("NFC", "".join(text.chars[i:j]))
        node = {"m": list(dict.fromkeys(marks)), "c": [s]} if marks else s
        if link:
            node = {"link": link, "c": [node]}
        if isinstance(node, str) and out and isinstance(out[-1], str):
            out[-1] += node
        else:
            out.append(node)
        i = j
    return out


def plain(inlines: list) -> str:
    return "".join(n if isinstance(n, str) else plain(n.get("c", [])) for n in inlines)


def leading_number(nodes: list, pid: str = "") -> tuple[int | None, list]:
    """Numéro canonique en tête : <b>N</b>, ou « N » nu quand <p id="pN"> l'annonce (cas du [102])."""
    rest = list(nodes)
    while rest and isinstance(rest[0], str) and not rest[0].strip():
        rest.pop(0)
    if rest and isinstance(rest[0], Node) and rest[0].tag == "b" and rest[0].text().strip().isdigit():
        return int(rest[0].text().strip()), rest[1:]
    if pid.startswith("p") and rest and isinstance(rest[0], str):
        if m := re.match(rf"\s*{pid[1:]}\s+", rest[0]):
            return int(pid[1:]), [rest[0][m.end():]] + rest[1:]
    return None, nodes


# ---------------------------------------------------------------------------------------------------------------

class Builder:
    def __init__(self, root: Node):
        self.root = root
        self.conv = Converter()
        self.blocks: dict[str, list[dict]] = {u[0]: [] for u in UNITS}
        self.unit_starts = {u[1]: u[0] for u in UNITS}
        self.unit: str | None = None
        self.ctx = {"week": None, "h4": None, "last": None}
        self.current, self.sub, self.pending = 0, 0, None
        self.numbers: list[int] = []
        self.seen: set[str] = set()

    def emit(self, block: dict):
        if block["id"] in self.seen:
            raise SystemExit(f"identifiant en double {block['id']}")
        self.seen.add(block["id"])
        self.blocks[self.unit].append(block)

    def block_type(self, inl: list, bid: str) -> str:
        """Les points (« Premier point. ») sont des item ; un libellé non italique est aligné sur les autres."""
        first = inl[0] if inl else ""
        if isinstance(first, dict) and first.get("m") == ["i"] and re.match(POINT, plain([first])):
            return "item"
        if isinstance(first, str) and (m := re.match(POINT + r"\.", first)):
            inl[0:1] = [{"m": ["i"], "c": [m[0]]}, first[m.end():]]
            self.conv.log["markup"].append({"block": bid, "correction": f"« {m[0]} » mis en italique comme les autres points"})
            return "item"
        return "para"

    def canonical(self, n: int, nodes: list, style: str | None = None):
        if n != self.current + 1:
            raise SystemExit(f"numéro {n} après {self.current}")
        self.current, self.sub = n, 1
        self.numbers.append(n)
        bid = f"ex.{n}"
        inl = self.conv.inlines(nodes, bid)
        block = {"id": bid, "type": self.block_type(inl, bid), "n": str(n)}
        if style:
            block["style"] = style
        block["t"] = {"fr": [{"num": str(n)}] + inl}
        self.emit(block)

    def continuation(self, nodes: list | None, style: str | None = None, inlines: list | None = None, extra: dict | None = None):
        self.sub += 1
        bid = f"ex.{self.current}.{self.sub}"
        inl = inlines if inlines is not None else self.conv.inlines(nodes, bid)
        block = {"id": bid, "type": self.block_type(inl, bid) if inlines is None else "para"}
        if style:
            block["style"] = style
        block.update(extra or {})
        block["t"] = {"fr": inl}
        self.emit(block)

    def heading(self, seg: dict):
        level, nodes = seg["level"], seg["nodes"]
        n, rest = leading_number(nodes)
        if n is not None and level >= 5:  # paragraphe canonique présenté en sous-titre ([21], [45], [313]…)
            self.canonical(n, rest, style="intro")
            return
        if n is None and level >= 5 and self.pending is not None:  # références d'un mystère, sous-titre du [328]
            n, self.pending = self.pending, None
            self.canonical(n, nodes, style="intro")
            return
        if n is None and level == 6:
            if self.ctx["last"] in CONTINUATION_SUBTITLES:
                self.continuation(nodes, style="intro")
                return
            key = self.ctx["last"] + ".st"
        else:
            marks = len(self.conv.notes), len(self.conv.links)
            title = " ".join(plain(self.conv.inlines(rest, "titre")).split())
            del self.conv.notes[marks[0]:], self.conv.links[marks[1]:]  # lecture du titre seulement : rien à consigner
            key = "m." + MYSTERIES[n] if level == 4 and n is not None else heading_key(level, title, self.ctx)
            if n is not None:  # titre numéroté ([262]–[312], [328]) : le numéro passe au bloc suivant
                self.pending = n
            if level == 3:
                self.ctx["week"] = {"semaine-1": "s1", "semaine-2": "s2", "semaine-3": "s3", "semaine-4": "s4"}.get(key, self.ctx["week"])
            if level == 4:
                self.ctx["h4"] = key
            self.ctx["last"] = key
        if key in self.unit_starts:
            self.unit = self.unit_starts[key]
        bid = f"ex.h.{key}"
        self.emit({"id": bid, "type": "heading", "level": level - 1, "t": {"fr": self.conv.inlines(rest, bid)}})  # h2 → 1 … h6 → 5

    def chart(self, seg: dict):
        """Le graphique de l'examen particulier ([31]) : lignes de la source, séparées par {"br":1}."""
        lines = [ln.rstrip() for ln in "".join(c for c in seg["nodes"] if isinstance(c, str)).split("\n") if ln.strip()]
        indent = min(len(ln) - len(ln.lstrip()) for ln in lines)
        inl: list = []
        for ln in lines:
            ln = ln[indent:]
            if inl:
                inl.append({"br": 1})
            inl.append(NBSP * (len(ln) - len(ln.lstrip())) + ln.lstrip())
        self.continuation(None, inlines=inl, extra={"x-chart": "examen-particulier"})

    def run(self):
        for div_id in ("c1", "c2", "c3", "c4", "c5"):
            for seg in segments(find_div(self.root, div_id)):
                if seg["src"] == "pre":
                    self.chart(seg)
                elif is_empty(seg["nodes"]):
                    self.conv.flatten(seg["nodes"], Text(), f"ex.{self.current}")  # appel de note isolé
                elif seg["src"] == "h":
                    self.heading(seg)
                else:
                    n, rest = leading_number(seg["nodes"], seg.get("pid", ""))
                    if n is not None:
                        self.canonical(n, rest)
                    elif self.pending is not None:
                        n, self.pending = self.pending, None
                        self.canonical(n, seg["nodes"])
                    else:
                        self.continuation(seg["nodes"])


def modifications(root: Node, notes: list[tuple[str, str]]) -> list[dict]:
    """La liste « Modifications du texte original » (2005), avec le bloc où figurait chaque appel."""
    div = find_div(root, "a1")
    where = {}
    for n, block in notes:
        where.setdefault(n, []).append(block)
    out, cur = [], None
    for c in div.children:
        if isinstance(c, Node) and c.tag == "a" and c.attrs.get("name", "").startswith("n"):
            label = c.text().strip()
            cur = {"n": label.rstrip("*"), "general": label.endswith("*"), "parts": []}
            out.append(cur)
        elif cur is not None and isinstance(c, Node) and c.tag == "q":
            cur["parts"].append("«" + NNBSP + c.text().strip().replace("'", "’") + NNBSP + "»")
        elif cur is not None and isinstance(c, str) and c.strip():
            cur["parts"].append(c.strip())
    return [{"n": m["n"], "general": m["general"], "text": " ".join(m["parts"]), "at": where.get(m["n"], [])} for m in out]


def crampon_check(archive: Path, links: list[dict]) -> list[str]:
    """Chaque renvoi existe-t-il dans la versification du Crampon ? Lecture de l'archive en mémoire, sans extraction."""
    verses: dict[str, dict[int, int]] = {}
    with tarfile.open(archive, "r:gz") as tar:
        for member in tar.getmembers():
            for book, name in CRAMPON_FILES.items():
                if member.isfile() and member.name.endswith(f"/usfm/{name}.usfm"):
                    data = tar.extractfile(member).read().decode("utf-8")
                    chapters: dict[int, int] = {}
                    chapter = 0
                    for m in re.finditer(r"\\(c|v) (\d+)(?:-(\d+))?", data):
                        if m[1] == "c":
                            chapter = int(m[2])
                        else:
                            chapters[chapter] = max(chapters.get(chapter, 0), int(m[3] or m[2]))
                    verses[book] = chapters
    problems = []
    for link in links:
        book, rest = link["target"].split(".", 1)
        if book not in verses:
            problems.append(f"{link['block']} : livre {book} absent de l'archive")
            continue
        m = re.fullmatch(r"(\d+)\.(\d+)(?:-(?:(\d+)\.)?(\d+))?", rest)
        c1, v1, c2, v2 = int(m[1]), int(m[2]), int(m[3] or m[1]), int(m[4] or m[2])
        for c, v in ((c1, v1), (c2, v2)):
            if v > verses[book].get(c, 0):
                problems.append(f"{link['block']} : {link['text']} → {link['target']} hors du Crampon ({book} {c} a {verses[book].get(c, 0)} versets)")
    return problems


def write_unit(path: Path, blocks: list[dict]) -> None:
    order = ["id", "type", "level", "n", "dropCap", "style", "x-chart", "t"]
    lines = [json.dumps({k: b[k] for k in order if k in b}, ensure_ascii=False, separators=(",", ":")) for b in blocks]
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(prog="normalize.py")
    default_crampon = sorted((ROOT / ".cache" / "raw" / "bible").glob("crampon-*.tar.gz"))
    parser.add_argument("--crampon", type=Path, default=default_crampon[-1] if default_crampon else None)
    args = parser.parse_args(argv)

    raw = SOURCE.read_bytes()
    tree = TreeBuilder()
    tree.feed(raw.decode("utf-8"))
    mention = " ".join(" ".join(n.text().split()) for n in find_div(tree.root, "t1").children if isinstance(n, Node) and n.tag == "h6")
    if mention != "Édition libre de tout droit":
        raise SystemExit(f"mention de droits inattendue : {mention!r}")

    b = Builder(tree.root)
    b.run()
    log = b.conv.log

    # Contrôles : 370 numéros une fois chacun, dans l'ordre ; plages des unités ; corrections toutes appliquées.
    if b.numbers != list(range(1, 371)):
        raise SystemExit(f"numéros canoniques : {len(b.numbers)} trouvés, suite incorrecte")
    for uid, _, _, _, (lo, hi), _ in UNITS:
        found = [int(x["id"][3:]) for x in b.blocks[uid] if re.fullmatch(r"ex\.\d+", x["id"])]
        if found != list(range(lo, hi + 1)):
            raise SystemExit(f"unité {uid} : plage {found[:1]}…{found[-1:]} au lieu de [{lo}]–[{hi}]")
    if len(log["cites"]) != len(CITE_FIXES) or len(log["text"]) != len(TEXT_FIXES):
        raise SystemExit(f"corrections non appliquées : {log['cites']} {log['text']}")
    for uid, blocks in b.blocks.items():
        para = next(x for x in blocks if x["type"] == "para")
        para["dropCap"] = True
        for x in blocks:
            s = plain(x["t"]["fr"])
            if re.search(r"'| [;:!?.,]|\w[;:!?]|\boe", s):
                raise SystemExit(f"typographie à revoir dans {x['id']} : {s[:120]}")

    if args.crampon and args.crampon.exists():
        problems = crampon_check(args.crampon, b.conv.links)
        for p in problems:
            print("renvoi douteux :", p)
        if problems:
            return 1
        print(f"renvois bibliques : {len(b.conv.links)} contrôlés contre {args.crampon.name}")
    else:
        print("renvois bibliques : archive Crampon absente, contrôle de versification sauté")

    mods = modifications(tree.root, b.conv.notes)
    divisions = [{"id": uid, "kind": kind, "toc": toc, "unit": True, "range": [f"ex.{lo}", f"ex.{hi}"], **({"week": w} if w else {})}
                 for uid, _, kind, toc, (lo, hi), w in UNITS]
    publication = {
        "id": "exercices.fr",
        "revision": 1,
        "kind": "book",
        "title": {"fr": "Exercices spirituels"},
        "subtitle": {"fr": "de saint Ignace de Loyola, traduits par le P. Pierre Jennesseaux sj"},
        "languages": ["fr"],
        "primaryLanguage": "fr",
        "aligned": False,
        "contributors": [
            {"name": "Ignace de Loyola", "role": "author"},
            {"name": "Pierre Jennesseaux", "role": "translator"},
            {"name": "Frère Jérôme", "role": "transcriber"},
        ],
        "issued": "1913",
        "citation": {"prefix": "ES", "key": "ex.<n>", "range": [1, 370]},
        "source": {
            "name": "livres-mystiques.com",
            "url": URL,
            "edition": "Traduction du texte espagnol par le P. Pierre Jennesseaux sj, numérisation de l’édition de 1913 par le Frère Jérôme, Namur, 2005",
            "retrieved": RETRIEVED,
            "rawSha256": hashlib.sha256(raw).hexdigest(),
        },
        "license": {
            "spdx": "LicenseRef-PublicDomain",
            "attribution": "Trad. P. Pierre Jennesseaux sj (éd. 1913), numérisation Frère Jérôme, Namur 2005 — livres-mystiques.com."
                           " Mention de la source" + NNBSP + ": «" + NNBSP + mention + NNBSP + "»",
        },
        "x-modifications2005": mods,
        "divisions": divisions,
    }

    (OUT / "blocks").mkdir(parents=True, exist_ok=True)
    for old in (OUT / "blocks").glob("*.jsonl"):
        if old.stem not in b.blocks:
            old.unlink()
    for uid, blocks in b.blocks.items():
        write_unit(OUT / "blocks" / f"{uid}.jsonl", blocks)
    (OUT / "publication.json").write_text(json.dumps(publication, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")

    total = sum(len(x) for x in b.blocks.values())
    print(f"exercices.fr : {total} blocs, {len(b.blocks)} unités, {len(b.numbers)} numéros, "
          f"{len(b.conv.links)} renvois bibliques, {len(b.conv.notes)} appels de note retirés")
    print("corrections :", json.dumps(log["cites"] + log["text"] + log["markup"], ensure_ascii=False, indent=1))
    print("typographie :", json.dumps(log["typo"], ensure_ascii=False))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
