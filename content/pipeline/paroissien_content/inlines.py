"""Grammaire des inlines (rapport 11 §3.2.3) et texte canonique (ADR 004).

inline :=
    "texte"                                     texte simple (NFC)
  | {"m": ["i"|"b"|"sc"|"red"|"sup"|"sub"|"sp"], "c": [inline…]}
  | {"lang": "la", "c": [inline…]}             changement de langue
  | {"note": "<blockId>", "l": "3"}            appel de note (ne compte pas)
  | {"link": "<ref>", "c": [inline…]}          renvoi
  | {"sym": "V"|"R"|"cross"|"star"|"dagger"}   symbole liturgique (compte pour son caractère)
  | {"num": "16"}                              numéro en ligne (ne compte pas)
  | {"br": 1}                                  saut de ligne (compte pour \\n)
"""

from __future__ import annotations

import re
import unicodedata

SYMBOLS = {"V": "℣", "R": "℟", "cross": "✠", "star": "*", "dagger": "†"}
MARKS = {"i", "b", "sc", "red", "sup", "sub", "sp"}
FORBIDDEN = {"­", "​", "⁠", "﻿"}
LANG_RE = re.compile(r"^[a-z]{2,3}$")


def plain_text(inlines: list) -> str:
    """Texte canonique : chaînes concaténées, sym → son caractère, br → \\n, note et num → rien."""
    out: list[str] = []
    _plain(inlines, out)
    return "".join(out)


def _plain(inlines: list, out: list[str]) -> None:
    for node in inlines:
        if isinstance(node, str):
            out.append(node)
        elif "c" in node:
            _plain(node["c"], out)
        elif "sym" in node:
            out.append(SYMBOLS[node["sym"]])
        elif "br" in node:
            out.append("\n")
        # note, num : hors du texte canonique


def validate(inlines: object, where: str, errors: list[str], note_ids: set[str] | None = None) -> None:
    """Ajoute à [errors] chaque écart à la grammaire ou aux règles du texte canonique."""
    if not isinstance(inlines, list):
        errors.append(f"{where} : les inlines doivent former une liste")
        return
    for i, node in enumerate(inlines):
        at = f"{where}[{i}]"
        if isinstance(node, str):
            _check_text(node, at, errors)
            continue
        if not isinstance(node, dict):
            errors.append(f"{at} : nœud inconnu {node!r}")
            continue
        keys = set(node)
        if "m" in node:
            if keys != {"m", "c"} or not node["m"] or not set(node["m"]) <= MARKS:
                errors.append(f"{at} : marque invalide {node!r}")
            validate(node.get("c"), at, errors, note_ids)
        elif "lang" in node:
            if keys != {"lang", "c"} or not LANG_RE.match(str(node["lang"])):
                errors.append(f"{at} : changement de langue invalide {node!r}")
            validate(node.get("c"), at, errors, note_ids)
        elif "link" in node:
            if keys != {"link", "c"} or not isinstance(node["link"], str):
                errors.append(f"{at} : renvoi invalide {node!r}")
            validate(node.get("c"), at, errors, note_ids)
        elif "note" in node:
            if not keys <= {"note", "l"}:
                errors.append(f"{at} : appel de note invalide {node!r}")
            elif note_ids is not None and node["note"] not in note_ids:
                errors.append(f"{at} : la note {node['note']} n'existe pas")
        elif "sym" in node:
            if keys != {"sym"} or node["sym"] not in SYMBOLS:
                errors.append(f"{at} : symbole inconnu {node!r}")
        elif "num" in node:
            if keys != {"num"} or not isinstance(node["num"], str):
                errors.append(f"{at} : numéro invalide {node!r}")
        elif "br" in node:
            if node != {"br": 1}:
                errors.append(f"{at} : saut de ligne invalide {node!r}")
        else:
            errors.append(f"{at} : nœud inconnu {node!r}")


def _check_text(text: str, where: str, errors: list[str]) -> None:
    if unicodedata.normalize("NFC", text) != text:
        errors.append(f"{where} : texte non NFC {text[:40]!r}")
    bad = sorted({c for c in text if c in FORBIDDEN or (unicodedata.category(c) == "Cc")})
    if bad:
        errors.append(f"{where} : caractères interdits {[f'U+{ord(c):04X}' for c in bad]} dans {text[:40]!r}")


def note_refs(inlines: list) -> list[str]:
    refs: list[str] = []
    for node in inlines:
        if isinstance(node, dict):
            if "note" in node:
                refs.append(node["note"])
            if "c" in node:
                refs.extend(note_refs(node["c"]))
    return refs
