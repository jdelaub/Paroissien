#!/usr/bin/env python3
"""Aide à la relecture du latin : repère les mots suspects de The Latin Library en les confrontant à d'autres témoins.

    python3 -I content/sources/imitation/collate_la.py [--vocab FICHIER…] > rapport.txt

Témoins (fetch.py --compare) : la.wikisource (copie partiellement corrigée de The Latin Library) et trois éditions
imprimées lues par OCR sur archive.org (Leipzig 1840, Londres 1867, New York 1888). --vocab ajoute des textes latins
servant seulement de lexique (par exemple la Vulgate clémentine).

Le rapport ne corrige rien : chaque correction retenue est recopiée à la main dans corrections-la.tsv, puis
normalize.py l'applique. Il porte sur le texte de content/data/imitation.la, donc après les corrections déjà admises.

Signalements :
  hors-lexique  mot absent de tous les témoins et du lexique ;
  contexte      un témoin a, entre les deux mêmes mots voisins, un mot proche différent (s long lu f, lettre sautée…) ;
  wikisource    la.wikisource a corrigé ce mot dans sa copie.
"""

from __future__ import annotations

import difflib
import json
import re
import sys
import unicodedata
from collections import Counter, defaultdict
from pathlib import Path

HERE = Path(__file__).resolve().parent
CONTENT = HERE.parents[1]
RAW = CONTENT / ".cache" / "raw" / "imitation"
DATA = CONTENT / "data" / "imitation.la"
ARCHIVE = ("archive-thomaekempisde00thom.txt", "archive-deimitationechr00thomgoog.txt", "archive-deimitationechri00unse.txt")
WORD = re.compile(r"[^\W\d_]+")


def norm(word: str) -> str:
    """Forme de comparaison : minuscules, sans accents, æ → ae, œ → oe, j → i, v → u."""
    w = unicodedata.normalize("NFD", word.lower())
    w = "".join(c for c in w if unicodedata.category(c) != "Mn")
    return w.replace("æ", "ae").replace("œ", "oe").replace("j", "i").replace("v", "u")


def tokens(text: str) -> list[str]:
    return WORD.findall(text)


def ocr_text(path: Path) -> str:
    text = path.read_text(encoding="utf-8", errors="replace")
    return re.sub(r"(\w)-\s*\n\s*(\w)", r"\1\2", text)  # mots coupés en fin de ligne


def wikisource_chapters() -> dict[str, list[str]]:
    out: dict[str, list[str]] = {}
    for book in range(1, 5):
        path = RAW / f"wikisource-l{book}.wiki"
        if not path.exists():
            continue
        text = re.sub(r"\{\{.*?\}\}", " ", path.read_text(encoding="utf-8"), flags=re.S)
        text = re.sub(r"<[^>]+>", " ", text)
        key = f"l{book}.x"
        for line in text.splitlines():
            m = re.match(r"^==\s*C(?:ap|hap)\.?\s*([0-9IVXL]+)", line)
            if m:
                key = f"l{book}.c{int(m.group(1)) if m.group(1).isdigit() else roman_to_int(m.group(1))}"
                continue
            out.setdefault(key, []).extend(tokens(line))
    return out


def roman_to_int(s: str) -> int:
    values = {"I": 1, "V": 5, "X": 10, "L": 50}
    return sum(-values[c] if i + 1 < len(s) and values[s[i + 1]] > values[c] else values[c] for i, c in enumerate(s))


def plain(nodes: list) -> str:
    out = []
    for n in nodes:
        if isinstance(n, str):
            out.append(n)
        elif "c" in n:
            out.append(plain(n["c"]))
        elif "br" in n:
            out.append(" ")
    return "".join(out)


def latin_blocks() -> list[tuple[str, str]]:
    meta = json.loads((DATA / "publication.json").read_text(encoding="utf-8"))
    out = []
    for d in meta["divisions"]:
        for line in (DATA / "blocks" / f"{d['id']}.jsonl").read_text(encoding="utf-8").splitlines():
            b = json.loads(line)
            out.append((b["id"], plain(b["t"]["la"])))
    return out


def distance(a: str, b: str) -> int:
    """Distance d'édition (Levenshtein), bornée à 3."""
    if abs(len(a) - len(b)) > 2:
        return 3
    prev = list(range(len(b) + 1))
    for i, ca in enumerate(a, start=1):
        cur = [i]
        for j, cb in enumerate(b, start=1):
            cur.append(min(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + (ca != cb)))
        prev = cur
    return min(prev[-1], 3)


def main(argv: list[str]) -> int:
    vocab_files = argv[argv.index("--vocab") + 1:] if "--vocab" in argv else []
    witnesses: dict[str, list[str]] = {}
    for name in ARCHIVE:
        if (RAW / name).exists():
            witnesses[name.removeprefix("archive-").removesuffix(".txt")] = [norm(t) for t in tokens(ocr_text(RAW / name))]
    lexicon = Counter(t for toks in witnesses.values() for t in toks)
    for f in vocab_files:
        lexicon.update(norm(t) for t in tokens(re.sub(r"<[^>]+>", " ", Path(f).read_text(encoding="utf-8", errors="replace"))))
    contexts: dict[tuple[str, str], Counter] = defaultdict(Counter)
    for toks in witnesses.values():
        for a, w, b in zip(toks, toks[1:], toks[2:]):
            contexts[(a, b)][w] += 1

    blocks = latin_blocks()
    ws = wikisource_chapters()
    ll_by_chapter: dict[str, list[tuple[str, str]]] = defaultdict(list)
    for bid, text in blocks:
        chapter = ".".join(bid.split(".")[:2])
        ll_by_chapter[chapter].extend((bid, t) for t in tokens(text))

    report: list[str] = []
    for bid, text in blocks:
        toks = tokens(text)
        normed = [norm(t) for t in toks]
        for i, (raw, w) in enumerate(zip(toks, normed)):
            ctx = " ".join(toks[max(0, i - 4):i + 5])
            if lexicon[w] == 0 and len(w) > 1:
                near = sorted({x for x in lexicon if abs(len(x) - len(w)) <= 2 and x[:1] == w[:1] and distance(w, x) <= 2},
                              key=lambda x: (distance(w, x), -lexicon[x]))[:5]
                report.append(f"{bid}\thors-lexique\t{raw}\t{' '.join(f'{x}({lexicon[x]})' for x in near)}\t{ctx}")
                continue
            if 0 < i < len(toks) - 1:
                seen = contexts.get((normed[i - 1], normed[i + 1]))
                if seen and w not in seen:
                    alt = [x for x, _ in seen.most_common() if distance(w, x) <= 2 and lexicon[x] > lexicon[w]]
                    if alt:
                        report.append(f"{bid}\tcontexte\t{raw}\t{' '.join(f'{x}({lexicon[x]}/{lexicon[w]})' for x in alt)}\t{ctx}")

    for chapter, pairs in ll_by_chapter.items():
        if chapter not in ws:
            continue
        a = [t for _, t in pairs]
        b = ws[chapter]
        sm = difflib.SequenceMatcher(None, a, b, autojunk=False)
        for op, i1, i2, j1, j2 in sm.get_opcodes():
            if op == "equal":
                continue
            before, after = " ".join(a[i1:i2]), " ".join(b[j1:j2])
            if norm(before) == norm(after):
                continue
            where = pairs[i1][0] if i1 < len(pairs) else chapter
            report.append(f"{where}\twikisource\t{before}\t{after}\t{' '.join(a[max(0, i1 - 4):i2 + 4])}")

    print("\n".join(report))
    print(f"# {len(report)} signalements", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
