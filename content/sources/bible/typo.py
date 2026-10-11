"""Typographie du texte canonique (content/schema/canonical-v1.md, « Règles du texte »)."""

from __future__ import annotations

import re
import unicodedata

from textrun import BR, Runs

NNBSP = " "

# Œ et æ que la source a écrits « oe » ou « ae » dans un mot français qui les prend.
LIGATURES = [
    (re.compile(r"\boeil\b"), "œil"),
    (re.compile(r"\bCoelé"), "Cœlé"),
    (re.compile(r"\bPhoebé\b"), "Phœbé"),
]


def prepare(raw: str) -> str:
    """Avant toute analyse : NFC, sans U+FEFF, apostrophe typographique, espaces insécables ramenées
    à des espaces simples (elles sont reposées par spacing())."""
    raw = unicodedata.normalize("NFC", raw).replace("﻿", "")
    raw = raw.replace("'", "’")
    return re.sub("[  ]", " ", raw)


_BEFORE_HIGH = re.compile(r"(?<=[^\s;:!?«(\[" + BR + r"])( *)([;:!?])")
_OPEN_GUILLEMET = re.compile(r"« *")
_CLOSE_GUILLEMET = re.compile(r"(?<=[^\s" + BR + r"]) *»")
_SPACE_BEFORE_LOW = re.compile(r" +(?=[,.])")


def _high(m: re.Match) -> str:
    s = m.string
    # « 51:12 », « 10:30 » : deux-points entre chiffres, sans espace.
    if m.group(2) == ":" and not m.group(1) and m.start() > 0 and s[m.start() - 1].isdigit() and s[m.end():m.end() + 1].isdigit():
        return m.group(0)
    return NNBSP + m.group(2)


def spacing(runs: Runs, french: bool = True) -> None:
    """Espace fine insécable avant ; : ! ? et à l'intérieur des guillemets, aucune espace avant , et .
    Le latin de la Clémentine imprime aussi une espace avant ces signes : on la rend insécable de la même façon."""
    runs.tidy()
    runs.sub(_SPACE_BEFORE_LOW, "")
    runs.sub(_BEFORE_HIGH, _high)
    if french:
        runs.sub(_OPEN_GUILLEMET, "«" + NNBSP)
        runs.sub(_CLOSE_GUILLEMET, NNBSP + "»")
        for pattern, repl in LIGATURES:
            runs.sub(pattern, repl)
    runs.tidy()
