#!/usr/bin/env python3
"""Normalise l'Imitation de Jésus-Christ vers le format canonique v1 (content/schema/canonical-v1.md).

    python3 -I content/sources/imitation/normalize.py [fr] [la]

fr : Lamennais (1824), Gutenberg n° 57824            → content/data/imitation.fr/
la : The Latin Library, kempis1.shtml à kempis4.shtml → content/data/imitation.la/,
     après les corrections consignées dans corrections-la.tsv

Entrées : content/.cache/raw/imitation/ (voir fetch.py). Bibliothèque standard seulement ; les fichiers bruts sont
lus comme des données, jamais exécutés. Toute anomalie de structure arrête le script.
"""

from __future__ import annotations

import csv
import hashlib
import html
import json
import re
import sys
import unicodedata
from pathlib import Path

HERE = Path(__file__).resolve().parent
CONTENT = HERE.parents[1]
RAW = CONTENT / ".cache" / "raw" / "imitation"
DATA = CONTENT / "data"
CORRECTIONS_LA = HERE / "corrections-la.tsv"

NNBSP = "\u202f"
CHAPTERS = (25, 12, 59, 18)
TOC_FR = ("Livre premier", "Livre deuxième", "Livre troisième", "Livre quatrième")
TOC_LA = ("Liber primus", "Liber secundus", "Liber tertius", "Liber quartus")
EXHORTATION = "exhortation"  # clé du préambule du livre IV, sans numéro de chapitre


class SourceError(Exception):
    pass


def roman(n: int) -> str:
    out = ""
    for value, sym in ((50, "L"), (40, "XL"), (10, "X"), (9, "IX"), (5, "V"), (4, "IV"), (1, "I")):
        while n >= value:
            out += sym
            n -= value
    return out


def from_roman(s: str) -> int:
    values = {"I": 1, "V": 5, "X": 10, "L": 50, "C": 100}
    total = 0
    for i, c in enumerate(s):
        v = values[c]
        total += -v if i + 1 < len(s) and values[s[i + 1]] > v else v
    return total


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def fetch_log() -> dict:
    return json.loads((RAW / "fetch-log.json").read_text(encoding="utf-8"))


# ---------------------------------------------------------------------------------------------------------------
# Inlines communs

def compact(nodes: list) -> list:
    """Fusionne les chaînes voisines et retire les vides ; les blancs au bord d'une marque passent hors de la marque ;
    rogne les blancs aux extrémités du bloc."""
    out = _merge(nodes)
    _trim(out, 0, str.lstrip)
    _trim(out, -1, str.rstrip)
    return _merge(out)


def _merge(nodes: list) -> list:
    out: list = []

    def push(node) -> None:
        if isinstance(node, str):
            if not node:
                return
            if out and isinstance(out[-1], str):
                out[-1] += node
                return
        out.append(node)

    for node in nodes:
        if not (isinstance(node, dict) and "c" in node):
            push(node)
            continue
        inner = _merge(node["c"])
        lead = trail = ""
        if inner and isinstance(inner[0], str):
            lead = inner[0][:len(inner[0]) - len(inner[0].lstrip())]
            inner[0] = inner[0].lstrip()
        if inner and isinstance(inner[-1], str):
            trail = inner[-1][len(inner[-1].rstrip()):]
            inner[-1] = inner[-1].rstrip()
        inner = [n for n in inner if n != ""]
        push(lead)
        if inner:
            push({**node, "c": inner})
        push(trail)
    return out


def _trim(nodes: list, index: int, fn) -> None:
    if not nodes:
        return
    node = nodes[index]
    if isinstance(node, str):
        nodes[index] = fn(node)
    elif isinstance(node, dict) and "c" in node and node["c"]:
        _trim(node["c"], index, fn)


def block(id_: str, type_: str, lang: str, inlines: list, **attrs) -> dict:
    out = {"id": id_, "type": type_}
    out.update({k: v for k, v in attrs.items() if v is not None})
    out["t"] = {lang: inlines}
    return out


def check_text(nodes: list, where: str) -> None:
    for node in nodes:
        if isinstance(node, str):
            if unicodedata.normalize("NFC", node) != node:
                raise SourceError(f"{where} : texte non NFC")
            if any(unicodedata.category(c) == "Cc" or c in "\u00ad\u200b\u2060\ufeff" for c in node):
                raise SourceError(f"{where} : caractère interdit dans {node[:40]!r}")
        elif isinstance(node, dict) and "c" in node:
            check_text(node["c"], where)


def numbering(segments: list[list[int]]) -> str | None:
    """Numéros de paragraphe d'un chapitre, par segment entre intertitres. Chaque segment se suit et part de 1 ou
    du numéro suivant le segment précédent. Renvoie None si tout se suit, sinon une description."""
    desc = " | ".join(",".join(map(str, seg)) for seg in segments if seg)
    last, restarted = 0, False
    for seg in filter(None, segments):
        if seg != list(range(seg[0], seg[0] + len(seg))) or seg[0] not in (1, last + 1):
            return f"irrégulière : {desc}"
        restarted |= seg[0] == 1 and last > 0
        last = seg[-1]
    return f"reprise à 1 après l'intertitre : {desc}" if restarted else None


def write_publication(meta: dict, units: dict[str, list[dict]]) -> None:
    target = DATA / meta["id"]
    (target / "blocks").mkdir(parents=True, exist_ok=True)
    for stale in (target / "blocks").glob("*.jsonl"):
        if stale.stem not in units:
            stale.unlink()
    (target / "publication.json").write_text(json.dumps(meta, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    for unit, blocks in units.items():
        lines = [json.dumps(b, ensure_ascii=False, separators=(",", ":")) for b in blocks]
        (target / "blocks" / f"{unit}.jsonl").write_text("\n".join(lines) + "\n", encoding="utf-8")


# ---------------------------------------------------------------------------------------------------------------
# Français : Lamennais, Gutenberg n° 57824

FR_FILE = "pg57824.txt"
FR_BOOK_HEADS = ("LIVRE PREMIER.", "LIVRE DEUXIÈME.", "LIVRE TROISIÈME.", "LIVRE QUATRIÈME.")
FR_BOOK_ENDS = ("FIN DU PREMIER LIVRE.", "FIN DU DEUXIÈME LIVRE.", "FIN DU TROISIÈME LIVRE.", "FIN DU LIVRE QUATRIÈME.")

# Intitulés imprimés en capitales → casse de lecture (aucun autre mot n'est retouché).
FR_CAPS = {
    "AVIS UTILES POUR ENTRER DANS LA VIE INTÉRIEURE.": "Avis utiles pour entrer dans la vie intérieure.",
    "INSTRUCTION POUR AVANCER DANS LA VIE INTÉRIEURE.": "Instruction pour avancer dans la vie intérieure.",
    "DE LA VIE INTÉRIEURE.": "De la vie intérieure.",
    "DU SACREMENT DE L'EUCHARISTIE.": "Du sacrement de l'Eucharistie.",
    "EXHORTATION À LA SAINTE COMMUNION.": "Exhortation à la sainte communion.",
    "RÉFLEXION.": "Réflexion.",
    "VOIX DE JÉSUS-CHRIST.": "Voix de Jésus-Christ.",
    "VOIX DU DISCIPLE.": "Voix du disciple.",
    "VOIX DU BIEN-AIMÉ.": "Voix du Bien-Aimé.",
    "PRIÈRE POUR DEMANDER LA GRÂCE DE LA DÉVOTION.": "Prière pour demander la grâce de la dévotion.",
    "PRIÈRE POUR DEMANDER À DIEU LA GRÂCE D'ACCOMPLIR SA VOLONTÉ.":
        "Prière pour demander à Dieu la grâce d'accomplir sa volonté.",
    "PRIÈRE POUR OBTENIR D'ÊTRE DÉLIVRÉ DES MAUVAISES PENSÉES.": "Prière pour obtenir d'être délivré des mauvaises pensées.",
    "PRIÈRE POUR DEMANDER À DIEU LA LUMIÈRE.": "Prière pour demander à Dieu la lumière.",
    "PRIÈRE POUR OBTENIR LA PURETÉ DU COEUR ET LA SAGESSE CÉLESTE.":
        "Prière pour obtenir la pureté du cœur et la sagesse céleste.",
}

# Mots où la transcription a perdu la ligature œ (liste fermée : un autre « oe » arrête le script).
FR_OE = {w: w.replace("oe", "œ").replace("OE", "Œ").replace("Oe", "Œ") for w in (
    "coeur", "coeurs", "Coeur", "COEUR", "oeuvre", "oeuvres", "OEuvre", "oeil", "moeurs", "voeu", "voeux",
    "choeur", "choeurs", "soeur", "soeurs", "boeuf", "boeufs",
)}
# Numéros de chapitre fautifs dans la source (le libellé est recalculé, rien d'autre ne change).
FR_CHAPTER_ERRATA = {(3, 38): "XXXIII"}
FR_WORD = re.compile(r"[^\W\d_]+")
NOTE_DEF = re.compile(r"^\s+\[(\d+)\]\s+(.*)$")
NOTE_CALL = re.compile(r"\[(\d+)\]")
FR_VERSE = re.compile(r"^(\d+)\.\s+(.*)$", re.S)


def typo_fr(text: str) -> str:
    """Typographie française de la publication (guide §Règles du texte), sans toucher à l'orthographe."""
    text = re.sub(r"\s+", " ", text).strip()
    text = text.replace("'", "’").replace("...", "…")

    def oe(m: re.Match) -> str:
        word = m.group(0)
        if word in FR_OE:
            return FR_OE[word]
        if "oe" in word.lower():
            raise SourceError(f"mot en « oe » non prévu : {word!r}")
        return word

    text = FR_WORD.sub(oe, text)
    text = re.sub(r"\s+([.,])", r"\1", text)
    text = re.sub(r"[ \u00a0\u202f]*([;:!?]+)", lambda m: NNBSP + m.group(1), text)
    text = re.sub(r"«[ \u00a0\u202f]*", "«" + NNBSP, text)
    text = re.sub(r"[ \u00a0\u202f]*»", NNBSP + "»", text)
    return text


def fr_inlines(text: str, on_note) -> list:
    """« _italique_ » → marque i ; « [n] » → appel de note (on_note(n) renvoie l'inline)."""
    text = typo_fr(text)
    out: list = []
    italic: list | None = None
    for part in re.split(r"(_|\[\d+\])", text):
        target = out if italic is None else italic
        if part == "_":
            if italic is None:
                italic = []
            else:
                out.append({"m": ["i"], "c": italic})
                italic = None
        elif NOTE_CALL.fullmatch(part):
            target.append(on_note(int(part[1:-1])))
        elif part:
            target.append(part)
    if italic is not None:
        raise SourceError(f"italique non fermé : {text[:60]!r}")
    return compact(out)


def fr_chunks(lines: list[str]) -> list[list[str]]:
    out, cur = [], []
    for line in lines:
        if line.strip():
            cur.append(line.rstrip())
        elif cur:
            out.append(cur)
            cur = []
    if cur:
        out.append(cur)
    return out


def fr_parse_book(lines: list[str], book: int) -> dict:
    """Une liste de lignes (de « LIVRE … » exclu à « FIN DU … » exclu) → structure du livre."""
    chunks = fr_chunks(lines)
    subtitle = FR_CAPS[" ".join(chunks[0])]
    chapters: list[dict] = []
    notes: dict[int, str] = {}
    cur: dict | None = None
    mode = "body"
    i = 1
    while i < len(chunks):
        chunk = chunks[i]
        text = " ".join(line.strip() for line in chunk)
        if NOTE_DEF.match(chunk[0]):
            defs: list[list[str]] = []
            for line in chunk:
                if NOTE_DEF.match(line):
                    defs.append([line])
                elif line.startswith("    "):
                    defs[-1].append(line)
                else:
                    raise SourceError(f"livre {book} : ligne inattendue dans une note : {line!r}")
            for d in defs:
                m = re.match(r"\[(\d+)\]\s+(.*)", " ".join(x.strip() for x in d))
                n = int(m.group(1))
                if n in notes:
                    raise SourceError(f"note [{n}] définie deux fois")
                notes[n] = m.group(2)
            i += 1
            continue
        if chunk[0].startswith("CHAPITRE "):
            num = chunk[0].removeprefix("CHAPITRE ").rstrip(".")
            n = 1 if num == "PREMIER" else from_roman(num)
            expected = sum(1 for c in chapters if c["key"] != EXHORTATION) + 1
            if n != expected and FR_CHAPTER_ERRATA.get((book, expected)) != num:
                raise SourceError(f"livre {book} : chapitre {n} au lieu de {expected}")
            n = expected
            cur = {"key": f"c{n}", "label": f"Chapitre {roman(n)}", "title": " ".join(x.strip() for x in chunks[i + 1]),
                   "items": [], "reflection": []}
            chapters.append(cur)
            mode = "body"
            i += 2
            continue
        if text == "EXHORTATION":
            cur = {"key": EXHORTATION, "label": None, "title": FR_CAPS[f"EXHORTATION {' '.join(chunks[i + 1])}"],
                   "items": [], "reflection": []}
            chapters.append(cur)
            mode = "body"
            i += 2
            continue
        if cur is None:
            raise SourceError(f"livre {book} : texte avant le premier chapitre : {text[:60]!r}")
        if text == "RÉFLEXION.":
            if cur["reflection"] or mode == "reflection":
                raise SourceError(f"livre {book} {cur['key']} : deux réflexions")
            mode = "reflection"
            i += 1
            continue
        if text == "PRIÈRE":
            cur["items"].append(("heading", FR_CAPS[f"PRIÈRE {' '.join(chunks[i + 1])}"]))
            i += 2
            continue
        if text.startswith("VOIX "):
            cur["items"].append(("heading", FR_CAPS[text]))
            i += 1
            continue
        if mode == "reflection":
            if not chunk[0].startswith("  "):
                raise SourceError(f"livre {book} {cur['key']} : texte non indenté dans la réflexion : {text[:60]!r}")
            cur["reflection"].append(text)
        else:
            if chunk[0].startswith(" "):
                raise SourceError(f"livre {book} {cur['key']} : texte indenté hors réflexion : {text[:60]!r}")
            cur["items"].append(("para", text))
        i += 1
    return {"subtitle": subtitle, "chapters": chapters, "notes": notes}


def fr_book_blocks(parsed: dict, book: int, stats: dict) -> list[dict]:
    unit = f"l{book}"
    defs = parsed["notes"]
    called: dict[int, str] = {}

    def note_ref(prefix: str):
        def ref(n: int) -> dict:
            if n not in defs:
                raise SourceError(f"{prefix} : appel [{n}] sans note")
            if n in called:
                raise SourceError(f"{prefix} : note [{n}] appelée deux fois")
            called[n] = f"{prefix}.n{n}"
            return {"note": called[n], "l": str(n)}
        return ref

    title = [{"m": ["sc"], "c": [TOC_FR[book - 1]]}, {"br": 1}, typo_fr(parsed["subtitle"])]
    blocks = [block(f"{unit}.titre", "para", "fr", title, style="centered")]
    for ch in parsed["chapters"]:
        key = f"{unit}.{ch['key']}"
        blocks.append(block(key, "heading", "fr", fr_inlines(ch["title"], note_ref(key)), level=1, label=ch["label"]))
        p = h = 0
        verses: list[list[int]] = [[]]
        for kind, text in ch["items"]:
            if kind == "heading":
                h += 1
                blocks.append(block(f"{key}.h{h}", "heading", "fr", fr_inlines(text, note_ref(key)), level=2))
                verses.append([])
                continue
            p += 1
            m = FR_VERSE.match(text)
            inlines = fr_inlines(m.group(2) if m else text, note_ref(key))
            if m:
                verses[-1].append(int(m.group(1)))
                inlines = [{"num": m.group(1)}] + inlines
            blocks.append(block(f"{key}.p{p}", "para", "fr", inlines, dropCap=True if p == 1 else None,
                                verse=m.group(1) if m else None))
        problem = numbering(verses)
        if problem and not problem.startswith("reprise"):
            raise SourceError(f"{key} : numérotation {problem}")
        if problem:
            stats["numbering"].append(f"{key} : {problem}")
        stats["verses"] += sum(map(len, verses))
        stats["paras"] += p
        if ch["reflection"]:
            blocks.append(block(f"{key}.r", "heading", "fr", [typo_fr(FR_CAPS["RÉFLEXION."])], level=2))
            for k, text in enumerate(ch["reflection"], start=1):
                blocks.append(block(f"{key}.r{k}", "para", "fr", fr_inlines(text, note_ref(key)), style="small"))
            stats["reflections"] += 1
        else:
            stats.setdefault("without_reflection", []).append(key)
    unused = sorted(set(defs) - set(called))
    if unused:
        raise SourceError(f"livre {book} : notes jamais appelées {unused}")
    for n in sorted(called):
        blocks.append(block(called[n], "note", "fr", fr_inlines(defs[n], note_ref("note")), placement="foot", label=str(n)))
    stats["notes"] += len(called)
    for b in blocks:
        check_text(b["t"]["fr"], b["id"])
    return blocks


def build_fr() -> dict:
    path = RAW / FR_FILE
    lines = path.read_text(encoding="utf-8").replace("\r\n", "\n").split("\n")
    start = next(i for i, line in enumerate(lines) if line.startswith("*** START OF THE PROJECT GUTENBERG EBOOK"))
    units: dict[str, list[dict]] = {}
    stats = {"verses": 0, "paras": 0, "reflections": 0, "notes": 0, "chapters": [], "numbering": []}
    pos = start
    for b, (head, end) in enumerate(zip(FR_BOOK_HEADS, FR_BOOK_ENDS), start=1):
        s = lines.index(head, pos)
        e = lines.index(end, s)
        parsed = fr_parse_book(lines[s + 1:e], b)
        count = sum(1 for c in parsed["chapters"] if c["key"] != EXHORTATION)
        if count != CHAPTERS[b - 1]:
            raise SourceError(f"livre {b} : {count} chapitres au lieu de {CHAPTERS[b - 1]}")
        stats["chapters"].append(count)
        units[f"l{b}"] = fr_book_blocks(parsed, b, stats)
        pos = e
    log = fetch_log()[FR_FILE]
    meta = {
        "id": "imitation.fr",
        "revision": 1,
        "kind": "book",
        "title": {"fr": "Imitation de Jésus-Christ"},
        "subtitle": {"fr": "Traduction de F. de Lamennais"},
        "languages": ["fr"],
        "primaryLanguage": "fr",
        "aligned": False,
        "contributors": [
            {"name": "Thomas a Kempis", "role": "author"},
            {"name": "Félicité de Lamennais", "role": "translator"},
        ],
        "issued": "1824",
        "source": {
            "name": "Projet Gutenberg n° 57824",
            "url": "https://www.gutenberg.org/ebooks/57824",
            "file": log["url"],
            "edition": "L’Imitation de Jésus-Christ, traduction nouvelle avec des réflexions à la fin de chaque chapitre, "
                       "par M. l’abbé F. de Lamennais, 43e édition, Paris, Ambroise Bray, 1859 ; numérisation Google Books, "
                       "transcription de Laurent Vogel et de l’équipe Distributed Proofreaders (pgdp.net), 2018",
            "retrieved": log["retrieved"],
            "rawSha256": sha256(path),
        },
        "license": {
            "spdx": "LicenseRef-PublicDomain",
            "attribution": "Traduction de F. de Lamennais (1824), domaine public. Texte établi d’après le Projet Gutenberg "
                           "n° 57824 (transcription Distributed Proofreaders), sans l’en-tête ni la licence du Projet "
                           "Gutenberg ; préface, messe et vêpres de l’édition de 1859 non reprises.",
        },
        "divisions": [{"id": f"l{b}", "kind": "book", "toc": TOC_FR[b - 1], "unit": True} for b in range(1, 5)],
    }
    write_publication(meta, units)
    return {"blocks": sum(len(v) for v in units.values()), **stats}


# ---------------------------------------------------------------------------------------------------------------
# Latin : The Latin Library

LA_FILES = tuple(f"kempis{n}.shtml" for n in range(1, 5))
LA_P = re.compile(r"<p(?:\s+class=(\w+))?\s*>(.*?)</p>", re.I | re.S)
LA_CAP = re.compile(r"^<b>\s*C(?:ap|hap)\.\s*([0-9IVXL]+)\.\s*(.*?)\s*</b>$", re.I | re.S)
LA_VERSE = re.compile(r"^(\d+)\.\s*(?=\D)(.*)$", re.S)
LA_CENTER = re.compile(r"<\s*(center|div)\b", re.I)


def la_clean(raw: str) -> str:
    """HTML d'un paragraphe → texte avec seulement <i>…</i> ; entités décodées, blancs réduits."""
    text = re.sub(r"\s*<br\s*/?>\s*", "\n", re.sub(r"[ \t\r\n]+", " ", raw), flags=re.I)
    text = re.sub(r"</?(?:center|div)[^>]*>", "", text, flags=re.I)
    text = re.sub(r"<(/?)i>", lambda m: f"<{m.group(1)}i>", text, flags=re.I)
    if re.search(r"<(?!/?i>)[^>]*>", text):
        raise SourceError(f"balise inattendue : {text[:80]!r}")
    text = html.unescape(text)
    text = re.sub(r" *\n *", "\n", text).strip()
    return unicodedata.normalize("NFC", text)


def la_inlines(text: str) -> list:
    out: list = []
    italic: list | None = None
    text = re.sub(r"\s+", " ", text)
    for part in re.split(r"(</?i>)", text):
        if part == "<i>":
            if italic is not None:
                raise SourceError(f"italique imbriqué : {text[:60]!r}")
            italic = []
        elif part == "</i>":
            if italic is None:
                raise SourceError(f"italique fermé sans ouverture : {text[:60]!r}")
            out.append({"m": ["i"], "c": italic})
            italic = None
        elif part:
            (out if italic is None else italic).append(part)
    if italic is not None:
        raise SourceError(f"italique non fermé : {text[:60]!r}")
    out = compact(out)
    # Espace avant la ponctuation (artefact de saisie) : retirée, sans toucher aux mots.
    return compact(_la_punct(out))


def _la_punct(nodes: list) -> list:
    out = []
    for node in nodes:
        if isinstance(node, str):
            out.append(re.sub(r" +([,.;:?!])", r"\1", node))
        elif "c" in node:
            out.append({**node, "c": _la_punct(node["c"])})
        else:
            out.append(node)
    return out


def la_parse_book(book: int) -> dict:
    """kempisN.shtml → {"title": [lignes], "chapters": [{"key", "n", "title", "items": [(kind, num, texte)]}]}."""
    source = (RAW / LA_FILES[book - 1]).read_text(encoding="latin-1")
    title: list[str] = []
    pagehead = ""
    chapters: list[dict] = []
    cur: dict | None = None
    for cls, inner in LA_P.findall(source):
        cls = (cls or "").lower()
        if "<table" in inner.lower():
            break
        if cls == "pagehead":
            pagehead = la_clean(inner)
            continue
        if cls in ("border", "shortborder") or not inner.strip():
            continue
        stripped = inner.strip()
        m = LA_CAP.match(stripped)
        if m:
            n = int(m.group(1)) if m.group(1).isdigit() else from_roman(m.group(1).upper())
            expected = sum(1 for c in chapters if c["key"] != EXHORTATION) + 1
            if n != expected:
                raise SourceError(f"liber {book} : caput {n} au lieu de {expected}")
            cur = {"key": f"c{n}", "n": n, "title": la_clean(m.group(2)), "items": []}
            chapters.append(cur)
            continue
        text = la_clean(stripped)
        if LA_CENTER.search(stripped):
            if cur is None:
                raise SourceError(f"liber {book} : intertitre avant tout chapitre : {text!r}")
            cur["items"].append(("heading", None, re.sub(r"</?i>", "", text)))
            continue
        if cur is None:
            if "\n" in text:  # « Liber Primus <br> Admonitiones… »
                title = text.split("\n")
            elif book == 4:  # préambule du livre IV : « De devota exhortatione… »
                cur = {"key": EXHORTATION, "n": None, "title": text, "items": []}
                chapters.append(cur)
            else:
                raise SourceError(f"liber {book} : paragraphe avant le premier chapitre : {text[:60]!r}")
            continue
        m = LA_VERSE.match(text)
        cur["items"].append(("para", m.group(1), m.group(2)) if m else ("para", None, text))
    if not title:
        # Le livre IV n'a pas de titre propre : on reprend celui de l'en-tête de page (« … LIBER QUARTUS »).
        title = [pagehead.rsplit("CHRISTI ", 1)[1].title()]
    return {"title": title, "chapters": chapters}


def load_corrections() -> dict[str, list[dict]]:
    out: dict[str, list[dict]] = {}
    with CORRECTIONS_LA.open(encoding="utf-8", newline="") as f:
        for row in csv.DictReader((line for line in f if not line.startswith("#")), delimiter="\t"):
            out.setdefault(row["id"], []).append(row)
    return out


def apply_corrections(text: str, key: str, corrections: dict, used: set) -> str:
    for row in corrections.get(key, []):
        before, after = row["avant"], row["après"]
        left = r"(?<!\w)" if before[0].isalnum() else ""
        right = r"(?!\w)" if before[-1].isalnum() else ""
        pattern = re.compile(left + re.escape(before) + right)
        count = len(pattern.findall(text))
        if count != int(row.get("n") or 1):
            raise SourceError(f"{key} : « {before} » trouvé {count} fois (attendu {row.get('n') or 1})")
        text = pattern.sub(lambda _m: after, text)
        used.add((key, before))
    return text


def la_book_blocks(parsed: dict, book: int, corrections: dict, used: set, stats: dict) -> list[dict]:
    unit = f"l{book}"
    lines = apply_corrections("\n".join(parsed["title"]), f"{unit}.titre", corrections, used).split("\n")
    title: list = [{"m": ["sc"], "c": [lines[0]]}]
    for extra in lines[1:]:
        title += [{"br": 1}, extra]
    blocks = [block(f"{unit}.titre", "para", "la", title, style="centered")]
    for ch in parsed["chapters"]:
        key = f"{unit}.{ch['key']}"
        label = f"Caput {roman(ch['n'])}" if ch["n"] else None
        heading = la_inlines(apply_corrections(ch["title"], key, corrections, used))
        blocks.append(block(key, "heading", "la", heading, level=1, label=label))
        h = p = sub = 0
        verses: list[list[int]] = [[]]
        first = True
        for kind, num, text in ch["items"]:
            if kind == "heading":
                h += 1
                hid = f"{key}.h{h}"
                blocks.append(block(hid, "heading", "la", la_inlines(apply_corrections(text, hid, corrections, used)), level=2))
                verses.append([])
                continue
            if num:
                verses[-1].append(int(num))
                bid, sub = f"{key}.v{num}", 1
            elif any(verses):
                sub += 1  # alinéa sans numéro à l'intérieur d'un paragraphe numéroté
                last = next(seg[-1] for seg in reversed(verses) if seg)
                bid = f"{key}.v{last}.{sub}"
            else:
                p += 1
                bid = f"{key}.p{p}"
            inlines = la_inlines(apply_corrections(text, bid, corrections, used))
            if num:
                inlines = [{"num": num}] + inlines
            blocks.append(block(bid, "para", "la", inlines, dropCap=True if first else None, verse=num))
            first = False
        problem = numbering(verses)
        if problem:
            stats["numbering"].append(f"{key} : {problem}")
        stats["verses"] += sum(map(len, verses))
        stats["paras"] += sum(1 for b in blocks if b["id"].startswith(key + ".") and b["type"] == "para")
    for b in blocks:
        check_text(b["t"]["la"], b["id"])
    return blocks


def build_la() -> dict:
    corrections = load_corrections()
    used: set = set()
    units: dict[str, list[dict]] = {}
    stats = {"verses": 0, "paras": 0, "chapters": [], "numbering": []}
    for b in range(1, 5):
        parsed = la_parse_book(b)
        count = sum(1 for c in parsed["chapters"] if c["key"] != EXHORTATION)
        if count != CHAPTERS[b - 1]:
            raise SourceError(f"liber {b} : {count} capita au lieu de {CHAPTERS[b - 1]}")
        stats["chapters"].append(count)
        units[f"l{b}"] = la_book_blocks(parsed, b, corrections, used, stats)
    unused = [(k, r["avant"]) for k, rows in corrections.items() for r in rows if (k, r["avant"]) not in used]
    if unused:
        raise SourceError(f"corrections non appliquées : {unused}")
    log = fetch_log()
    digest = hashlib.sha256(b"".join((RAW / f).read_bytes() for f in LA_FILES)).hexdigest()
    meta = {
        "id": "imitation.la",
        "revision": 1,
        "kind": "book",
        "title": {"la": "De imitatione Christi"},
        "subtitle": {"la": "Libri quatuor"},
        "languages": ["la"],
        "primaryLanguage": "la",
        "aligned": False,
        "contributors": [{"name": "Thomas a Kempis", "role": "author"}],
        "issued": "c. 1427",
        "source": {
            "name": "The Latin Library",
            "url": "https://www.thelatinlibrary.com/kempis.html",
            "files": [{"url": log[f]["url"], "sha256": log[f]["sha256"]} for f in LA_FILES],
            "edition": "Édition imprimée non indiquée par la source (impression ancienne à s long, d’après les erreurs "
                       "de lecture) ; OCR corrigé d’après la.wikisource et des éditions de 1840, 1867 et 1888 "
                       "(archive.org), voir content/sources/imitation/README.md",
            "retrieved": log[LA_FILES[0]]["retrieved"],
            "rawSha256": digest,
        },
        "license": {
            "spdx": "LicenseRef-PublicDomain",
            "attribution": "Texte latin du domaine public, d’après The Latin Library (thelatinlibrary.com), "
                           f"avec {sum(len(r) for r in corrections.values())} corrections d’OCR consignées.",
        },
        "divisions": [{"id": f"l{b}", "kind": "book", "toc": TOC_LA[b - 1], "unit": True} for b in range(1, 5)],
    }
    write_publication(meta, units)
    return {"blocks": sum(len(v) for v in units.values()), "corrections": sum(len(r) for r in corrections.values()), **stats}


def main(argv: list[str]) -> int:
    targets = argv or ["fr", "la"]
    try:
        for target in targets:
            result = {"fr": build_fr, "la": build_la}[target]()
            print(f"imitation.{target} : {json.dumps(result, ensure_ascii=False)}")
    except SourceError as e:
        print(f"erreur : {e}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
