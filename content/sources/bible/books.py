"""Livres bibliques : code USFM, testament, titre du Crampon 1923 et titre latin.

Titres français : table des matières de l'édition de 1923 (fr.wikisource, Page:…djvu/8 et /1758).
Titres latins : formes traditionnelles de la Clémentine, ajoutées par nous (la source OSIS n'a pas de titres).
L'ordre des livres n'est pas fixé ici : chaque édition garde l'ordre de sa source.
"""

from __future__ import annotations

# code USFM : (testament, titre français, titre latin)
BOOKS: dict[str, tuple[str, str, str]] = {
    "GEN": ("at", "Genèse", "Liber Genesis"),
    "EXO": ("at", "Exode", "Liber Exodus"),
    "LEV": ("at", "Lévitique", "Liber Leviticus"),
    "NUM": ("at", "Nombres", "Liber Numeri"),
    "DEU": ("at", "Deutéronome", "Liber Deuteronomii"),
    "JOS": ("at", "Josué", "Liber Josue"),
    "JDG": ("at", "Juges", "Liber Judicum"),
    "RUT": ("at", "Ruth", "Liber Ruth"),
    "1SA": ("at", "1 Samuel", "Liber I Regum"),
    "2SA": ("at", "2 Samuel", "Liber II Regum"),
    "1KI": ("at", "1 Rois", "Liber III Regum"),
    "2KI": ("at", "2 Rois", "Liber IV Regum"),
    "1CH": ("at", "1 Chroniques", "Liber I Paralipomenon"),
    "2CH": ("at", "2 Chroniques", "Liber II Paralipomenon"),
    "EZR": ("at", "Esdras", "Liber I Esdræ"),
    "NEH": ("at", "Néhémie", "Liber Nehemiæ, qui et Esdræ secundus dicitur"),
    "TOB": ("at", "Tobie", "Liber Tobiæ"),
    "JDT": ("at", "Judith", "Liber Judith"),
    "EST": ("at", "Esther", "Liber Esther"),
    "1MA": ("at", "1 Machabées", "Liber I Machabæorum"),
    "2MA": ("at", "2 Machabées", "Liber II Machabæorum"),
    "JOB": ("at", "Job", "Liber Job"),
    "PSA": ("at", "Psaumes", "Liber Psalmorum"),
    "PRO": ("at", "Proverbes", "Liber Proverbiorum"),
    "ECC": ("at", "Ecclésiaste", "Liber Ecclesiastes"),
    "SNG": ("at", "Cantique", "Canticum Canticorum"),
    "WIS": ("at", "Sagesse", "Liber Sapientiæ"),
    "SIR": ("at", "Ecclésiastique", "Liber Ecclesiasticus"),
    "ISA": ("at", "Isaïe", "Prophetia Isaiæ"),
    "JER": ("at", "Jérémie", "Prophetia Jeremiæ"),
    "LAM": ("at", "Lamentations", "Lamentationes Jeremiæ"),
    "BAR": ("at", "Baruch", "Prophetia Baruch"),
    "EZK": ("at", "Ézéchiel", "Prophetia Ezechielis"),
    "DAN": ("at", "Daniel", "Prophetia Danielis"),
    "HOS": ("at", "Osée", "Prophetia Osee"),
    "JOL": ("at", "Joël", "Prophetia Joel"),
    "AMO": ("at", "Amos", "Prophetia Amos"),
    "OBA": ("at", "Abdias", "Prophetia Abdiæ"),
    "JON": ("at", "Jonas", "Prophetia Jonæ"),
    "MIC": ("at", "Michée", "Prophetia Michææ"),
    "NAM": ("at", "Nahum", "Prophetia Nahum"),
    "HAB": ("at", "Habacuc", "Prophetia Habacuc"),
    "ZEP": ("at", "Sophonie", "Prophetia Sophoniæ"),
    "HAG": ("at", "Aggée", "Prophetia Aggæi"),
    "ZEC": ("at", "Zacharie", "Prophetia Zachariæ"),
    "MAL": ("at", "Malachie", "Prophetia Malachiæ"),
    "MAT": ("nt", "Évangile selon Saint Matthieu", "Evangelium secundum Matthæum"),
    "MRK": ("nt", "Évangile selon Saint Marc", "Evangelium secundum Marcum"),
    "LUK": ("nt", "Évangile selon Saint Luc", "Evangelium secundum Lucam"),
    "JHN": ("nt", "Évangile selon Saint Jean", "Evangelium secundum Joannem"),
    "ACT": ("nt", "Actes des Apôtres", "Actus Apostolorum"),
    "ROM": ("nt", "Épître aux Romains", "Epistola B. Pauli Apostoli ad Romanos"),
    "1CO": ("nt", "I^re épître aux Corinthiens", "Epistola B. Pauli Apostoli ad Corinthios prima"),
    "2CO": ("nt", "II^e épître aux Corinthiens", "Epistola B. Pauli Apostoli ad Corinthios secunda"),
    "GAL": ("nt", "Épître aux Galates", "Epistola B. Pauli Apostoli ad Galatas"),
    "EPH": ("nt", "Épître aux Éphésiens", "Epistola B. Pauli Apostoli ad Ephesios"),
    "PHP": ("nt", "Épître aux Philippiens", "Epistola B. Pauli Apostoli ad Philippenses"),
    "COL": ("nt", "Épître aux Colossiens", "Epistola B. Pauli Apostoli ad Colossenses"),
    "1TH": ("nt", "I^re épître aux Thessaloniciens", "Epistola B. Pauli Apostoli ad Thessalonicenses prima"),
    "2TH": ("nt", "II^e épître aux Thessaloniciens", "Epistola B. Pauli Apostoli ad Thessalonicenses secunda"),
    "1TI": ("nt", "I^re épître à Timothée", "Epistola B. Pauli Apostoli ad Timotheum prima"),
    "2TI": ("nt", "II^e épître à Timothée", "Epistola B. Pauli Apostoli ad Timotheum secunda"),
    "TIT": ("nt", "Épître à Tite", "Epistola B. Pauli Apostoli ad Titum"),
    "PHM": ("nt", "Épître à Philémon", "Epistola B. Pauli Apostoli ad Philemonem"),
    "HEB": ("nt", "Épître aux Hébreux", "Epistola B. Pauli Apostoli ad Hebræos"),
    "JAS": ("nt", "Épître de Saint Jacques", "Epistola Catholica B. Jacobi Apostoli"),
    "1PE": ("nt", "I^re épître de Saint Pierre", "Epistola B. Petri Apostoli prima"),
    "2PE": ("nt", "II^e épître de Saint Pierre", "Epistola B. Petri Apostoli secunda"),
    "1JN": ("nt", "I^re épître de Saint Jean", "Epistola B. Joannis Apostoli prima"),
    "2JN": ("nt", "II^e épître de Saint Jean", "Epistola B. Joannis Apostoli secunda"),
    "3JN": ("nt", "III^e épître de Saint Jean", "Epistola B. Joannis Apostoli tertia"),
    "JUD": ("nt", "Épître de Saint Jude", "Epistola Catholica B. Judæ Apostoli"),
    "REV": ("nt", "Apocalypse de Saint Jean", "Apocalypsis B. Joannis Apostoli"),
}

# OSIS (Vulgate) → USFM
OSIS_TO_USFM = {
    "Gen": "GEN", "Exod": "EXO", "Lev": "LEV", "Num": "NUM", "Deut": "DEU", "Josh": "JOS", "Judg": "JDG",
    "Ruth": "RUT", "1Sam": "1SA", "2Sam": "2SA", "1Kgs": "1KI", "2Kgs": "2KI", "1Chr": "1CH", "2Chr": "2CH",
    "Ezra": "EZR", "Neh": "NEH", "Tob": "TOB", "Jdt": "JDT", "Esth": "EST", "Job": "JOB", "Ps": "PSA",
    "Prov": "PRO", "Eccl": "ECC", "Song": "SNG", "Wis": "WIS", "Sir": "SIR", "Isa": "ISA", "Jer": "JER",
    "Lam": "LAM", "Bar": "BAR", "Ezek": "EZK", "Dan": "DAN", "Hos": "HOS", "Joel": "JOL", "Amos": "AMO",
    "Obad": "OBA", "Jonah": "JON", "Mic": "MIC", "Nah": "NAM", "Hab": "HAB", "Zeph": "ZEP", "Hag": "HAG",
    "Zech": "ZEC", "Mal": "MAL", "1Macc": "1MA", "2Macc": "2MA", "Matt": "MAT", "Mark": "MRK", "Luke": "LUK",
    "John": "JHN", "Acts": "ACT", "Rom": "ROM", "1Cor": "1CO", "2Cor": "2CO", "Gal": "GAL", "Eph": "EPH",
    "Phil": "PHP", "Col": "COL", "1Thess": "1TH", "2Thess": "2TH", "1Tim": "1TI", "2Tim": "2TI", "Titus": "TIT",
    "Phlm": "PHM", "Heb": "HEB", "Jas": "JAS", "1Pet": "1PE", "2Pet": "2PE", "1John": "1JN", "2John": "2JN",
    "3John": "3JN", "Jude": "JUD", "Rev": "REV",
}

SUPERSCRIPT = {"re": "ʳᵉ", "e": "ᵉ"}


def title_inlines(title: str) -> list:
    """« I^re épître » → ["I", {"m":["sup"],"c":["re"]}, " épître"]."""
    if "^" not in title:
        return [title]
    head, rest = title.split("^", 1)
    sup, tail = rest.split(" ", 1)
    return [head, {"m": ["sup"], "c": [sup]}, " " + tail]


def title_plain(title: str) -> str:
    """Pour la table des matières (chaîne simple) : « Iʳᵉ épître »."""
    if "^" not in title:
        return title
    head, rest = title.split("^", 1)
    sup, tail = rest.split(" ", 1)
    return head + SUPERSCRIPT[sup] + " " + tail
