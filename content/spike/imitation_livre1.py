"""Extraction PROVISOIRE du Livre I de l'Imitation (trad. Lamennais, Gutenberg n° 57824) pour le spike 1b.

Remplacée en phase 2 par la vraie chaîne (identifiants stables, validation, paquet SQLite). Elle ne sert
qu'à donner au paginateur un livre réel de 25 chapitres. Bibliothèque standard seulement.

Usage : python3 -I imitation_livre1.py pg57824.txt sortie.json
"""

import json
import re
import sys

NNBSP = " "


def typography(text: str) -> str:
    text = re.sub(r"\[\d+\]", "", text)  # appels de note (les notes viendront en phase 2)
    text = re.sub(r"\s+", " ", text).strip()
    text = re.sub(r"(?<![A-Za-z])oe(?=u|il)", "œ", text)  # coeur, oeuvre, voeu, oeil : Gutenberg a perdu la ligature
    text = re.sub(r"(?<=[a-zé])oe(?=u)", "œ", text)
    text = re.sub(r"\bOE(?=U|IL)", "Œ", text)
    text = text.replace("'", "’")
    text = re.sub(r"\s*([;:!?»])", lambda m: (NNBSP if m.group(1) != "»" else NNBSP) + m.group(1), text)
    text = re.sub(r"«\s*", "«" + NNBSP, text)
    return text


def paragraphs(lines):
    block, out = [], []
    for line in lines + [""]:
        if line.strip() == "":
            if block:
                out.append(" ".join(block))
                block = []
        elif re.match(r"^\s*\[\d+\]", line):  # ligne de note
            continue
        else:
            block.append(line.strip())
    return [p for p in out if p]


def main(source: str, target: str) -> None:
    lines = open(source, encoding="utf-8").read().splitlines()
    start = lines.index("LIVRE PREMIER.")
    end = next(i for i in range(start + 1, len(lines)) if lines[i].startswith("LIVRE SECOND") or lines[i].startswith("LIVRE DEUXIÈME"))
    body = lines[start + 1:end]
    heads = [i for i, l in enumerate(body) if l.startswith("CHAPITRE ")]
    subtitle = typography(" ".join(l for l in body[:heads[0]] if l.strip()).rstrip("."))
    chapters = []
    for n, h in enumerate(heads, start=1):
        stop = heads[n] if n < len(heads) else len(body)
        chunk = body[h + 1:stop]
        t = 0
        while chunk[t].strip() == "":
            t += 1
        title_lines = []
        while chunk[t].strip():
            title_lines.append(chunk[t])
            t += 1
        rest = chunk[t:]
        split = next((i for i, l in enumerate(rest) if l.strip() == "RÉFLEXION."), len(rest))
        verses = []
        for p in paragraphs(rest[:split]):
            m = re.match(r"^(\d+)\.\s+(.*)$", p)
            verses.append({"verse": int(m.group(1)), "text": typography(m.group(2))} if m else {"text": typography(p)})
        chapters.append({
            "n": n,
            "title": typography(" ".join(title_lines)),
            "paragraphs": verses,
            "reflection": [typography(p) for p in paragraphs(rest[split + 1:])],
        })
    data = {
        "source": "Imitation de Jésus-Christ, trad. F. de Lamennais (1824), Projet Gutenberg n° 57824",
        "book": "Livre premier",
        "subtitle": subtitle,
        "chapters": chapters,
    }
    with open(target, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=1)
    words = sum(len(p["text"].split()) for c in chapters for p in c["paragraphs"] + [{"text": r} for r in c["reflection"]])
    print(f"{len(chapters)} chapitres, {words} mots -> {target}")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
