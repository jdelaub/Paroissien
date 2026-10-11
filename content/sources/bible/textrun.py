"""Texte enrichi d'un bloc : une suite de « morceaux » (texte, attribut) convertie en inlines canoniques.

Attributs : ("t", italique, lien) pour le texte ; ("note", identifiant, étiquette) pour un appel de note ;
("br",) pour un saut de ligne. Les appels et les sauts sont des caractères sentinelles de la zone privée,
ce qui permet de nettoyer les espaces sur la chaîne entière sans perdre la structure.
"""

from __future__ import annotations

import re

NOTE = ""
BR = ""
PLAIN = ("t", False, None)


class Runs:
    def __init__(self) -> None:
        self.chars: list[str] = []
        self.attrs: list[tuple] = []

    def text(self, s: str, italic: bool = False, link: str | None = None) -> None:
        attr = ("t", italic, link)
        for ch in s:
            self.chars.append(ch)
            self.attrs.append(attr)

    def note(self, note_id: str, label: str) -> None:
        self.chars.append(NOTE)
        self.attrs.append(("note", note_id, label))

    def br(self) -> None:
        self.chars.append(BR)
        self.attrs.append(("br",))

    def has_content(self) -> bool:
        return any(not c.isspace() for c in self.chars)

    def string(self) -> str:
        return "".join(self.chars)

    def sub(self, pattern: re.Pattern, repl) -> int:
        """Remplacement sur la chaîne ; les caractères repris gardent leur attribut, les nouveaux prennent
        celui du premier caractère non blanc remplacé (ou du voisin)."""
        s = self.string()
        out_c: list[str] = []
        out_a: list[tuple] = []
        last = 0
        count = 0
        for m in pattern.finditer(s):
            new = repl(m) if callable(repl) else m.expand(repl)
            old = s[m.start():m.end()]
            if new == old:
                continue
            count += 1
            out_c.extend(self.chars[last:m.start()])
            out_a.extend(self.attrs[last:m.start()])
            olda = self.attrs[m.start():m.end()]
            default = next((olda[i] for i, ch in enumerate(old) if not ch.isspace() and olda[i][0] == "t"), None)
            if default is None:
                near = self.attrs[m.start() - 1] if m.start() > 0 else (self.attrs[m.end()] if m.end() < len(s) else PLAIN)
                default = near if near[0] == "t" else PLAIN
            j = 0
            for ch in new:
                k = old.find(ch, j)
                if k >= 0:
                    out_a.append(olda[k])
                    j = k + 1
                else:
                    out_a.append(default)
                out_c.append(ch)
            last = m.end()
        if count:
            out_c.extend(self.chars[last:])
            out_a.extend(self.attrs[last:])
            self.chars, self.attrs = out_c, out_a
        return count

    def tidy(self) -> None:
        """Espaces : un seul, aucun en tête, en fin ni autour d'un saut de ligne ; pas de saut en tête."""
        self.sub(re.compile(r"[ \t\r\n]{2,}"), " ")
        self.sub(re.compile(r"\s*" + BR + r"\s*"), BR)
        self.sub(re.compile(r"^\s+|\s+$"), "")
        self.sub(re.compile("^" + BR + "+"), "")
        self.sub(re.compile("(" + BR + ")" + BR + "+"), r"\1")

    def inlines(self) -> list:
        """Regroupe les caractères en inlines : chaînes, {"m":["i"]}, {"link"}, {"note"}, {"br":1}."""
        out: list = []
        i = 0
        n = len(self.chars)
        while i < n:
            attr = self.attrs[i]
            if attr[0] == "note":
                out.append({"note": attr[1], "l": attr[2]})
                i += 1
                continue
            if attr[0] == "br":
                out.append({"br": 1})
                i += 1
                continue
            j = i
            while j < n and self.attrs[j] == attr:
                j += 1
            text = "".join(self.chars[i:j])
            node: object = text
            if attr[1]:
                node = {"m": ["i"], "c": [text]}
            if attr[2]:
                node = {"link": attr[2], "c": [node]}
            _append(out, node)
            i = j
        return out


def _append(out: list, node: object) -> None:
    """Fusionne un lien avec le précédent quand ils visent la même cible (italique dans un renvoi)."""
    if isinstance(node, dict) and "link" in node and out and isinstance(out[-1], dict) and out[-1].get("link") == node["link"]:
        out[-1]["c"].extend(node["c"])
        return
    out.append(node)
