#!/usr/bin/env python3
"""Propre de la messe de 1962 (Divinum Officium) → publication `missel.1962`, latin seul et latin-français.

Bibliothèque standard seulement ; à lancer avec python3 -I :

    python3 -I content/sources/missel/build.py [--divinum DIR] [--private DIR] [-v]

- `--divinum` : clone de Divinum Officium (défaut content/.cache/raw/missel/divinum-officium, cf. fetch.py).
- `--private` : racine du dépôt privé (défaut ../paroissien-content si elle existe). Le même passage écrit
  content/data/missel.1962 (latin ; titres et rubriques de Paroissien en français) et
  <privé>/data/missel.1962 (latin et français de DO aligné bloc à bloc).
- Écrit aussi content/sources/missel/cles.txt (clés de DO couvertes) et sources.sha256 (sha256 des fichiers latins
  lus) ; dans le dépôt privé, sources/missel/sources.sha256 (latin et français) et sources/missel/rapport.json
  (chiffres, sections sans français ou non alignées).

Règles de 1960 (Gloria, Credo, préface, graduel du temps) : port des fonctions de web/cgi-bin/missa/propers.pl
(`gloriflag`, `Credo`, `prefatio`, `getitem`, `oratio`) ; voir README.md.
"""

from __future__ import annotations

import argparse
import difflib
import hashlib
import html
import importlib.util
import json
import re
import sys
import unicodedata
from collections import Counter, defaultdict
from dataclasses import dataclass, field
from datetime import date, timedelta
from pathlib import Path

HERE = Path(__file__).resolve().parent
CONTENT = HERE.parents[1]
sys.path.insert(0, str(HERE))

import do  # noqa: E402
import keys as K  # noqa: E402

PUB_ID = "missel.1962"
OVERLAY = HERE / "propres"  # propres de France transcrits par Paroissien (format de DO)
DO_REPO = "https://github.com/DivinumOfficium/divinum-officium"
DO_COMMIT = "b9c100d79ae481c67b2801fdd3622b00dd86bd73"
NNBSP = " "


def _ordinaire_typo():
    spec = importlib.util.spec_from_file_location("ordinaire_normalize", HERE.parent / "ordinaire" / "normalize.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module.typo


typo = _ordinaire_typo()

# ── Libellés ─────────────────────────────────────────────────────────────────────────────────

MONTHS = ["janvier", "février", "mars", "avril", "mai", "juin", "juillet", "août", "septembre", "octobre", "novembre", "décembre"]
CLASS_FR = {"I": "Iʳᵉ classe", "II": "IIᵉ classe", "III": "IIIᵉ classe", "IV": "IVᵉ classe", "COMMEMORATION": "Commémoraison",
            "VIGIL_I": "Vigile de Iʳᵉ classe", "VIGIL_II": "Vigile de IIᵉ classe", "VIGIL_III": "Vigile de IIIᵉ classe"}
COLOR_FR = {"WHITE": "blancs", "RED": "rouges", "GREEN": "verts", "VIOLET": "violets", "BLACK": "noirs", "ROSE": "roses"}

# (identifiant, section DO, titre français, titre latin)
PIECE_FR = {
    "Introitus": ("introit", "Introït", "Introitus"),
    "Oratio": ("collecte", "Collecte", "Oratio"),
    "Lectio": ("lecon", "Leçon", "Lectio"),
    "Graduale": ("graduel", "Graduel", "Graduale"),
    "Sequentia": ("sequence", "Séquence", "Sequentia"),
    "Evangelium": ("evangile", "Évangile", "Evangelium"),
    "Offertorium": ("offertoire", "Offertoire", "Offertorium"),
    "Secreta": ("secrete", "Secrète", "Secreta"),
    "Communio": ("communion", "Communion", "Communio"),
    "Postcommunio": ("postcommunion", "Postcommunion", "Postcommunio"),
    "Super populum": ("super-populum", "Oraison sur le peuple", "Oratio super populum"),
}

PREFACES = {  # nom de DO → titre français (rédigé par Paroissien)
    "Nat": "Préface de la Nativité", "Epi": "Préface de l’Épiphanie", "Quad": "Préface du Carême",
    "Quad5": "Préface de la Sainte Croix", "Pasch": "Préface de Pâques", "Asc": "Préface de l’Ascension",
    "Cord": "Préface du Sacré-Cœur", "Regis": "Préface du Christ-Roi", "Spiritu": "Préface du Saint-Esprit",
    "Trinitate": "Préface de la Sainte Trinité", "Maria": "Préface de la Sainte Vierge", "Joseph": "Préface de saint Joseph",
    "Apostolis": "Préface des Apôtres", "Communis": "Préface commune", "Defunctorum": "Préface des défunts",
    "Adv": "Préface de l’Avent", "Sacramento": "Préface du Saint-Sacrement", "Sanctis": "Préface de tous les Saints",
    "Dedicatio": "Préface de la Dédicace", "Baptista": "Préface de saint Jean-Baptiste", "Chrismatis": "Préface de la messe chrismale",
}
PREFACE_ORDER = ["Nat", "Epi", "Quad", "Quad5", "Pasch", "Asc", "Cord", "Regis", "Spiritu", "Trinitate", "Maria", "Joseph",
                 "Apostolis", "Communis", "Defunctorum", "Adv", "Sacramento", "Sanctis", "Dedicatio", "Baptista", "Chrismatis"]
PREFACE_LOCAL = {"Adv", "Sacramento", "Sanctis", "Dedicatio", "Baptista"}

# Temps fins (pour la préface et les variantes des Communs) et libellés des variantes.
SEASON_TEMPUS = {"adv": "Adventus", "nat": "Nativitatis", "epiph": "Epiphaniæ", "epi": "post Epiphaniam",
                 "septua": "Septuagesimæ", "quad": "Quadragesimæ", "passio": "Passionis", "holyweek": "Passionis",
                 "pasc0": "Octava Paschæ", "pasch": "post Octavam Paschæ", "asc": "Octava Ascensionis",
                 "pent": "Octava Pentecostes", "perannum": "post Pentecosten"}
GROUP = {"adv": "N", "nat": "N", "epiph": "N", "epi": "N", "perannum": "N", "septua": "Q", "quad": "Q", "passio": "Q",
         "pasch": "P", "asc": "P", "pent": "P"}
SEASON_LABEL = {"adv": "en Avent", "nat": "au temps de Noël", "epiph": "au temps de l’Épiphanie", "epi": "après l’Épiphanie",
                "septua": "au temps de la Septuagésime", "quad": "en Carême", "passio": "au temps de la Passion",
                "pasch": "au temps pascal", "asc": "après l’Ascension", "pent": "dans l’octave de la Pentecôte",
                "perannum": "après la Pentecôte"}
VARIANT = {  # groupe → (suffixe d'identifiant, français, latin)
    "P": ("tp", "Au temps pascal :", "Tempore paschali:"),
    "Q": ("sept", "Après la Septuagésime :", "Post Septuagesimam:"),
    "N": ("hors", "Hors du temps pascal :", "Extra tempus paschale:"),
    "N-before-Q": ("hors", "Avant la Septuagésime :", "Ante Septuagesimam:"),
}

# Stations romaines, relues sur le Missale Romanum de Pustet (archive.org bc.radom.pl.Missale_romanum_skaryszew,
# « Statio ad… » en tête de chaque messe) ; noms français de Paroissien.
STATIONS = {
    "Tempora/Adv1-0": "Sainte-Marie-Majeure", "Tempora/Adv2-0": "Sainte-Croix-de-Jérusalem", "Tempora/Adv3-0": "Saint-Pierre",
    "Tempora/Adv3-3": "Sainte-Marie-Majeure", "Tempora/Adv3-5": "Saints-Douze-Apôtres", "Tempora/Adv3-6": "Saint-Pierre",
    "Tempora/Adv4-0": "Saints-Douze-Apôtres", "Sancti/12-24": "Sainte-Marie-Majeure",
    "Sancti/12-25m1": "Sainte-Marie-Majeure, à la Crèche", "Sancti/12-25m2": "Sainte-Anastasie", "Sancti/12-25m3": "Sainte-Marie-Majeure",
    "Sancti/12-26": "Saint-Étienne-au-Mont-Cælius", "Sancti/12-27": "Sainte-Marie-Majeure", "Sancti/12-28r": "Saint-Paul-hors-les-Murs",
    "Sancti/01-01": "Sainte-Marie-du-Transtévère", "Sancti/01-06": "Saint-Pierre",
    "Tempora/Quadp1-0": "Saint-Laurent-hors-les-Murs", "Tempora/Quadp2-0": "Saint-Paul-hors-les-Murs", "Tempora/Quadp3-0": "Saint-Pierre",
    "Tempora/Quadp3-3": "Sainte-Sabine", "Tempora/Quadp3-4": "Saint-Georges", "Tempora/Quadp3-5": "Saints-Jean-et-Paul",
    "Tempora/Quadp3-6": "Saint-Tryphon",
    "Tempora/Quad1-0": "Saint-Jean-de-Latran", "Tempora/Quad1-1": "Saint-Pierre-aux-Liens", "Tempora/Quad1-2": "Sainte-Anastasie",
    "Tempora/Quad1-3": "Sainte-Marie-Majeure", "Tempora/Quad1-4": "Saint-Laurent-in-Panisperna", "Tempora/Quad1-5": "Saints-Douze-Apôtres",
    "Tempora/Quad1-6": "Saint-Pierre",
    "Tempora/Quad2-0": "Sainte-Marie-in-Domnica", "Tempora/Quad2-1": "Saint-Clément", "Tempora/Quad2-2": "Sainte-Balbine",
    "Tempora/Quad2-3": "Sainte-Cécile", "Tempora/Quad2-4": "Sainte-Marie-du-Transtévère", "Tempora/Quad2-5": "Saint-Vital",
    "Tempora/Quad2-6": "Saints-Marcellin-et-Pierre",
    "Tempora/Quad3-0": "Saint-Laurent-hors-les-Murs", "Tempora/Quad3-1": "Saint-Marc", "Tempora/Quad3-2": "Sainte-Pudentienne",
    "Tempora/Quad3-3": "Saint-Sixte", "Tempora/Quad3-4": "Saints-Côme-et-Damien", "Tempora/Quad3-5": "Saint-Laurent-in-Lucina",
    "Tempora/Quad3-6": "Sainte-Suzanne",
    "Tempora/Quad4-0": "Sainte-Croix-de-Jérusalem", "Tempora/Quad4-1": "Saints-Quatre-Couronnés", "Tempora/Quad4-2": "Saint-Laurent-in-Damaso",
    "Tempora/Quad4-3": "Saint-Paul-hors-les-Murs", "Tempora/Quad4-4": "Saints-Silvestre-et-Martin", "Tempora/Quad4-5": "Saint-Eusèbe",
    "Tempora/Quad4-6": "Saint-Nicolas-in-Carcere",
    "Tempora/Quad5-0": "Saint-Pierre", "Tempora/Quad5-1": "Saint-Chrysogone", "Tempora/Quad5-2": "Saint-Cyriaque",
    "Tempora/Quad5-3": "Saint-Marcel", "Tempora/Quad5-4": "Saint-Apollinaire", "Tempora/Quad5-5": "Saint-Étienne-au-Mont-Cælius",
    "Tempora/Quad5-6": "Saint-Jean-Porte-Latine",
    "Tempora/Quad6-0": "Saint-Jean-de-Latran", "Tempora/Quad6-1": "Sainte-Praxède", "Tempora/Quad6-2": "Sainte-Prisque",
    "Tempora/Quad6-3": "Sainte-Marie-Majeure", "Tempora/Quad6-4": "Saint-Jean-de-Latran", "Tempora/Quad6-5": "Sainte-Croix-de-Jérusalem",
    "Tempora/Quad6-6": "Saint-Jean-de-Latran",
    "Tempora/Pasc0-0": "Sainte-Marie-Majeure", "Tempora/Pasc0-1": "Saint-Pierre", "Tempora/Pasc0-2": "Saint-Paul-hors-les-Murs",
    "Tempora/Pasc0-3": "Saint-Laurent-hors-les-Murs", "Tempora/Pasc0-4": "Saints-Douze-Apôtres", "Tempora/Pasc0-5": "Sainte-Marie-aux-Martyrs",
    "Tempora/Pasc0-6": "Saint-Jean-de-Latran", "Tempora/Pasc1-0": "Saint-Pancrace", "Tempora/Pasc5-4": "Saint-Pierre",
    "Tempora/Pasc6-6": "Saint-Jean-de-Latran", "Tempora/Pasc5-1": "Sainte-Marie-Majeure", "Sancti/04-25c": "Saint-Pierre", "Tempora/Pasc7-0": "Saint-Pierre", "Tempora/Pasc7-1": "Saint-Pierre-aux-Liens",
    "Tempora/Pasc7-2": "Sainte-Anastasie", "Tempora/Pasc7-3": "Sainte-Marie-Majeure", "Tempora/Pasc7-4": "Saint-Laurent-hors-les-Murs",
    "Tempora/Pasc7-5": "Saints-Douze-Apôtres", "Tempora/Pasc7-6": "Saint-Pierre",
    "Tempora/093-3": "Sainte-Marie-Majeure", "Tempora/093-5": "Saints-Douze-Apôtres", "Tempora/093-6": "Saint-Pierre",
}

# Titres des messes multiples et des cérémonies (rédigés par Paroissien).
MULTI_TITLES = {
    "Sancti/12-25m1": ("Messe de la nuit", "Ad primam Missam in nocte"),
    "Sancti/12-25m2": ("Messe de l’aurore", "Ad secundam Missam in aurora"),
    "Sancti/12-25m3": ("Messe du jour", "Ad tertiam Missam in die"),
    "Sancti/11-02m1": ("Première messe", "Ad primam Missam"),
    "Sancti/11-02m2": ("Deuxième messe", "Ad secundam Missam"),
    "Sancti/11-02m3": ("Troisième messe", "Ad tertiam Missam"),
    "Tempora/Quad6-4rm1": ("Messe chrismale", "Missa Chrismatis"),
    "Tempora/Quad6-4rm2": ("Messe vespérale de la Cène du Seigneur", "Missa vespertina in Cena Domini"),
}
PRELUDE_TITLES = {
    "Tempora/Quad6-4rm1": ("Bénédiction des saintes huiles", "Benedictio oleorum"),
    "Tempora/Quad6-0r": ("Bénédiction et procession des rameaux", "Benedictio et processio palmarum"),
    "Tempora/Quad6-5r": ("Action liturgique de l’après-midi", "Solemnis actio liturgica postmeridiana"),
    "Tempora/Quad6-6r": ("Vigile pascale", "Vigilia paschalis"),
    "Sancti/02-02": ("Bénédiction et procession des cierges", "Benedictio et processio candelarum"),
    "Tempora/Quadp3-3": ("Bénédiction et imposition des cendres", "Benedictio et impositio cinerum"),
    "Tempora/Quad6-4m1": ("Bénédiction des saintes huiles", "Benedictio oleorum"),
}

# Messes sans propre dans DO pour une clé du calendrier de France : commun et nom (latin, français).
# Jours de l'octave de Noël toujours empêchés par une fête de IIᵉ classe : commémoraison seule (oraisons de la 3ᵉ messe).
OCTAVE_COMMEMORATIONS = {"Tempora/Nat26", "Tempora/Nat27", "Tempora/Nat28"}

# Communs : (clé, titre français) dans l'ordre du Missel romain.
COMMONS = [
    ("Commune/C1v", "Vigile d’un Apôtre : messe Ego autem"),
    ("Commune/C2", "Commun d’un martyr pontife : messe Statuit"),
    ("Commune/C2-1", "Commun d’un martyr pontife : messe Sacerdotes Dei"),
    ("Commune/C2b", "Commun d’un souverain pontife martyr : messe Si diligis"),
    ("Commune/C2a", "Commun d’un martyr non pontife : messe In virtute"),
    ("Commune/C2a-1", "Commun d’un martyr non pontife : messe Lætabitur"),
    ("Commune/C2p", "Commun d’un martyr au temps pascal : messe Protexisti"),
    ("Commune/C3", "Commun de plusieurs martyrs : messe Intret"),
    ("Commune/C3a", "Commun de plusieurs martyrs : messe Sapientiam"),
    ("Commune/C3a-1", "Commun de plusieurs martyrs : messe Salus autem"),
    ("Commune/C3b", "Commun de plusieurs souverains pontifes martyrs : messe Si diligis"),
    ("Commune/C3p", "Commun de plusieurs martyrs au temps pascal : messe Sancti tui"),
    ("Commune/C4", "Commun d’un confesseur pontife : messe Statuit"),
    ("Commune/C4-1", "Commun d’un confesseur pontife : messe Sacerdotes tui"),
    ("Commune/C4b", "Commun d’un souverain pontife confesseur : messe Si diligis"),
    ("Commune/C4a", "Commun d’un docteur pontife : messe In medio"),
    ("Commune/C5a", "Commun d’un docteur non pontife : messe In medio"),
    ("Commune/C5", "Commun d’un confesseur non pontife : messe Os justi"),
    ("Commune/C5-1", "Commun d’un confesseur non pontife : messe Justus ut palma"),
    ("Commune/C5b", "Commun d’un abbé : messe Os justi"),
    ("Commune/C6", "Commun d’une vierge martyre : messe Loquebar"),
    ("Commune/C6-1", "Commun d’une vierge martyre : messe Me exspectaverunt"),
    ("Commune/C6a", "Commun d’une vierge non martyre : messe Dilexisti"),
    ("Commune/C6a-1", "Commun d’une vierge non martyre : messe Vultum tuum"),
    ("Commune/C7", "Commun d’une sainte femme martyre : messe Me exspectaverunt"),
    ("Commune/C7a", "Commun d’une sainte femme non martyre : messe Cognovi"),
    ("Commune/C8", "Commun de la Dédicace d’une église : messe Terribilis"),
    ("Commune/C11", "Commun des fêtes de la Sainte Vierge : messe Salve, sancta Parens"),
    ("Commune/C10a", "Sainte Marie au samedi, en Avent : messe Rorate"),
    ("Commune/C10b", "Sainte Marie au samedi, de Noël à la Purification : messe Vultum tuum"),
    ("Commune/C10c", "Sainte Marie au samedi, de la Purification à Pâques : messe Salve, sancta Parens"),
    ("Commune/C10Pasc", "Sainte Marie au samedi, au temps pascal : messe Salve, sancta Parens"),
    ("Commune/C10", "Sainte Marie au samedi, après la Pentecôte : messe Salve, sancta Parens"),
]
COMMON_SEASON = {  # contexte fixe des Communs de saison ; les autres reçoivent les variantes Q (et P)
    "Commune/C10a": ["adv"], "Commune/C10b": ["nat", "epi"], "Commune/C10c": ["epi", "septua"], "Commune/C10Pasc": ["pasch"],
    "Commune/C10": ["perannum"], "Commune/C1p": ["pasch"], "Commune/C1ap": ["pasch"], "Commune/C2p": ["pasch"], "Commune/C3p": ["pasch"],
    "Commune/C1v": ["perannum"], "Commune/C9": ["perannum"],
}

# Libellés de rubriques connus de DO → français de Paroissien (paquet public ; le privé prend le français de DO).
RUBRIC_FR = {"Tractus": "Trait", "Sequentia": "Séquence", "Graduale": "Graduel", "Allelúja": "Alléluia"}

SKIP_MACROS = {"Deo gratias", "Gloria tibi", "Laus tibi"}
# Graphies fautives de DO ramenées au nom de la section de Prayers.txt.
MACRO_FIX = {"per dominum": "Per Dominum", "per dominium": "Per Dominum", "per dominum ejusdem": "Per Dominum eiusdem",
             "deo gratias": "Deo gratias", "glória": "Gloria", "per e": "Per eundem"}
CONCLUSIONS = {"Per Dominum", "Per Dominum eiusdem", "Qui cum Patre", "Per eumdem", "Per eundem", "Qui vivis", "Qui tecum",
               "Qui tecum eiusdem", "Per e"}
REF = re.compile(r"^(?:[1-4]\s?)?[A-Z][a-zæœ]+\.?\s?\d+[,:]\s?\d|^(?:Ps|Psalm)\s?\d+|^[A-Z][a-z]+ \d+\s*$")


# ── Calendrier : temps d'une date fixe ─────────────────────────────────────────────────────────

def fine_season(d: date) -> str:
    t = K.Year(d.year)
    if d >= t.advent1 and d < date(d.year, 12, 25):
        return "adv"
    if d >= date(d.year, 12, 25) or (d.month == 1 and d.day <= 5):
        return "nat"
    if d.month == 1 and d.day <= 13:
        return "epiph"
    if d < t.septuagesima:
        return "epi"
    if d < t.ash_wednesday:
        return "septua"
    if d < t.passion_sunday:
        return "quad"
    if d < t.palm_sunday:
        return "passio"
    if d < t.easter:
        return "holyweek"
    if d < t.easter + timedelta(days=7):
        return "pasc0"
    if d < t.ascension:
        return "pasch"
    if d < t.pentecost:
        return "asc"
    if d < t.pentecost + timedelta(days=7):
        return "pent"
    return "perannum"


_SEASON_CACHE: dict[str, Counter] = {}


def seasons_of(mmdd: str) -> Counter:
    """Fréquence des temps fins d'une date fixe sur 1900-2299 (le 29 février est compté les années bissextiles)."""
    if mmdd not in _SEASON_CACHE:
        m, dd = int(mmdd[:2]), int(mmdd[3:5])
        c: Counter = Counter()
        for y in range(1900, 2300):
            try:
                c[fine_season(date(y, m, dd))] += 1
            except ValueError:
                continue
        _SEASON_CACHE[mmdd] = c
    return _SEASON_CACHE[mmdd]


# ── Contexte d'un formulaire ───────────────────────────────────────────────────────────────────

@dataclass
class Ctx:
    season: str          # temps fin (préface, conditions)
    group: str           # N, Q ou P
    dow: int             # 0 dimanche … 6 samedi
    dayname: str         # semaine de DO : Adv1, Quadp3, Pasc5, Pent05…
    month: int = 0
    day: int = 0

    def do(self) -> tuple:
        return tuple(sorted({"tempore": SEASON_TEMPUS[self.season], "missa": 1, "feria": self.dow + 1, "commune": ""}.items()))


def tempora_ctx(key: str, tem: K.Temporal | None) -> Ctx:
    stem = key.split("/", 1)[1]
    dow = int(stem.rsplit("-", 1)[1][0]) if re.search(r"-\d", stem) else 1
    dayname = stem.rsplit("-", 1)[0] if "-" in stem else stem
    d = tem.dates[0] if tem and tem.dates else None
    if stem.startswith("093"):
        season = "perannum"
    elif d is not None:
        season = fine_season(d)
        if season == "holyweek" and dayname.startswith("Quad5"):
            season = "passio"
    else:
        season = "perannum"
    if dayname.startswith("Quad6"):
        season = "holyweek"
    group = {"holyweek": "Q", "pasc0": "P"}.get(season, GROUP.get(season, "N"))
    return Ctx(season, group, dow, dayname, d.month if d else 0, d.day if d else 0)


def week_dayname(d: date) -> str:
    """`getweek` de DO pour la messe (semaine du temporal d'une date)."""
    t = K.Year(d.year)
    if d >= t.advent1:
        if d < date(d.year, 12, 25):
            return f"Adv{(d - t.advent1).days // 7 + 1}"
        return f"Nat{d.day}"
    if d < t.holy_family:
        return f"Nat{d.day:02d}"
    if d < t.septuagesima:
        return f"Epi{(d - t.holy_family).days // 7 + 1}"
    if d < t.ash_wednesday + timedelta(days=4):
        return f"Quadp{(d - t.septuagesima).days // 7 + 1}"
    if d < t.easter:
        return f"Quad{(d - (t.easter - timedelta(days=42))).days // 7 + 1}"
    if d < t.easter + timedelta(days=56):
        return f"Pasc{(d - t.easter).days // 7}"
    return f"Pent{(d - t.pentecost).days // 7:02d}"


def fixed_ctx(mmdd: str, season: str) -> Ctx:
    """Contexte représentatif d'une date fixe dans un temps donné (première année qui l'y place)."""
    m, dd = int(mmdd[:2]), int(mmdd[3:5])
    for y in range(1900, 2300):
        try:
            d = date(y, m, dd)
        except ValueError:
            continue
        if fine_season(d) == season:
            return Ctx(season, GROUP.get(season, "N"), K.n(d) if d.weekday() != 6 else 1, week_dayname(d), m, dd)
    return Ctx(season, GROUP.get(season, "N"), 1, "Pent10", m, dd)


# ── Rendu des lignes de DO ─────────────────────────────────────────────────────────────────────

@dataclass
class Item:
    kind: str                 # para | versicle | rubric | ref | heading | stanza
    text: str = ""
    role: str | None = None   # V ou R
    label: str | None = None  # S., M., D., C., J., P., A., O., Ant., Ps.
    lines: list[str] = field(default_factory=list)

    def shape(self) -> str:
        """Forme pour l'alignement : un verset ℣/℟ et un paragraphe se valent (DO met « V. » tantôt dans une langue
        seulement) ; une référence et une rubrique aussi."""
        return {"ref": "rubric", "versicle": "text", "para": "text", "stanza": "text"}.get(self.kind, self.kind)


class Renderer:
    """Lignes d'une section de DO → éléments de texte (une langue)."""

    def __init__(self, lib: do.Library, lang: str, report: dict):
        self.lib, self.lang, self.report = lib, lang, report
        self.prayers = lib.load("Ordo/Prayers")[0]

    def prayer(self, name: str) -> list[str] | None:
        lines = self.prayers.get(name)
        return [l for l in lines if l.strip()] if lines else None

    def conclusion(self, name: str) -> str | None:
        lines = self.prayer(name)
        if not lines:
            return None
        text = " ".join(re.sub(r"^\s*[rv]\.\s*", "", l) for l in lines if not re.match(r"^\s*R\.", l))
        amen = [re.sub(r"^\s*R\.\s*", "", l) for l in lines if re.match(r"^\s*R\.", l)]
        return text.strip() + (f" ℟. {amen[0].strip()}" if amen else "")

    def render(self, lines: list[str], *, passiontide=False, stanzas=False, full_text=False, paschal=None, red_roles=False) -> list[Item]:
        items: list[Item] = []
        # Lignes continuées par « ~ ».
        joined: list[str] = []
        buf = ""
        split: list[str] = []
        for raw in lines:
            # « …du Seigneur.&Gloria » (fichiers français) : la macro reprend sa ligne.
            m = re.match(r"^(.*\S)\s*([&$][A-Z][A-Za-z ]*)$", raw.rstrip())
            if m and not raw.lstrip().startswith(("&", "$")):
                split += [m.group(1), m.group(2)]
            else:
                split.append(raw)
        for raw in split:
            line = html.unescape(raw.rstrip()) if "&" in raw and ";" in raw else raw.rstrip()
            if buf and re.match(r"^\s*[VR]\.\s", line):
                joined.append(buf.strip())  # « ~ » devant un verset : le verset garde sa ligne
                buf = ""
            if line.endswith("~"):
                buf += line[:-1].strip() + " "
                continue
            joined.append(buf + line.strip() if buf else line)
            buf = ""
        if buf:
            joined.append(buf.strip())
        run: list[str] = []

        tail_conclusion: list[str] = []

        def flush_run():
            if not run:
                return
            if stanzas and len(run) >= 2:
                items.append(Item("stanza", lines=list(run)))
            else:
                items.extend(Item("para", text=t) for t in run)
            run.clear()

        for line in joined:
            s = line.strip()
            if not s or re.match(r"^_?\s*`?\s*_?$", s):
                flush_run()
                continue
            if s.startswith("&"):
                flush_run()
                name = MACRO_FIX.get(s[1:].strip().lower(), s[1:].strip())
                if name in ("Gloria", "Gl"):
                    if not passiontide:
                        items.extend(self.render(self.prayer("Gloria") or []))
                elif name in ("DominusVobiscum", "Dominus_vobiscum"):
                    items.extend(self.render(self.prayer("Dominus vobiscum") or []))
                elif name in ("Ultimaev",):
                    pass
                else:
                    self.report["macros"][f"&{name}"] += 1
                continue
            if s.startswith("$"):
                name = s[1:].strip().rstrip(".")
                name = MACRO_FIX.get(name.lower(), name)
                if name in SKIP_MACROS:
                    continue
                if name in CONCLUSIONS:
                    concl = self.conclusion(name)
                    target = None
                    if run:
                        run[-1] = run[-1] + " " + concl
                        continue
                    for it in reversed(items):
                        if it.kind in ("para", "versicle", "stanza"):
                            target = it
                            break
                        if it.kind in ("heading",):
                            break
                    if target is None:
                        tail_conclusion.append(concl)  # conclusion écrite avant l'oraison (fichier français) : à la fin
                    elif target.kind == "stanza":
                        target.lines.append(concl)
                    else:
                        target.text += " " + concl
                    continue
                flush_run()
                expansion = self.prayer(name)
                if expansion:
                    items.extend(self.render(expansion))
                else:
                    self.report["macros"][f"${name}"] += 1
                continue
            if s.startswith("!!!"):
                flush_run()
                items.append(Item("para", text=s[3:].strip()))
                continue
            if s.startswith("!!"):
                flush_run()
                items.append(Item("heading", text=s[2:].strip()))
                continue
            if s.startswith("!"):
                flush_run()
                body = re.sub(r"^!x!", "", s)[1:].strip() if not s.startswith("!x!") else s[3:].strip()
                if not body:
                    continue
                items.append(Item("ref" if REF.match(body) else "rubric", text=body))
                continue
            if s in ("Tractus", "Tractus.", "Sequentia", "Graduale"):
                flush_run()
                items.append(Item("rubric", text=s.rstrip(".")))
                continue
            m = re.match(r"^(V\.|R\.br\.|R\.)\s*(.*)$", s)
            if m:
                flush_run()
                items.append(Item("versicle", text=m.group(2), role="V" if m.group(1) == "V." else "R"))
                continue
            m = re.match(r"^[vr]\.\s*(.*)$", s)
            if m:
                flush_run()
                items.append(Item("para", text=m.group(1)))
                continue
            m = re.match(r"^(S\.|M\.|D\.|C\.|J\.|P\.|A\.|O\.|Ant\.|Ps\.)\s+(.*)$", s)
            if m:
                flush_run()
                items.append(Item("para", text=m.group(2), label=m.group(1)))
                continue
            if stanzas == "strict" or (stanzas and len(s) <= 70):
                run.append(s)
                continue
            flush_run()
            items.append(Item("para", text=s))
        flush_run()
        if tail_conclusion:
            target = next((it for it in reversed(items) if it.kind in ("para", "versicle", "stanza")), None)
            if target is None:
                items.append(Item("para", text=" ".join(tail_conclusion)))
            elif target.kind == "stanza":
                target.lines.extend(tail_conclusion)
            else:
                target.text += " " + " ".join(tail_conclusion)
        items = normalize_items(items)
        for it in items:
            it.text = clean(it.text, paschal)
            it.lines = [clean(l, paschal) for l in it.lines]
        return [it for it in items if it.text or it.lines or it.kind == "heading"]


ALLELUIA_ONLY = re.compile(r"^(?:(?:Allel[uú][ij]a|All[eé]luia)[\s,.!]*)+$", re.I)
OREMUS = re.compile(r"^(?:Or[eé]mus|Prions)\.?$", re.I)


def normalize_items(items: list[Item]) -> list[Item]:
    """Mêmes coupures dans les deux langues : alléluia isolé rattaché au verset qui précède ; « Orémus. » seul rattaché
    au texte qui suit (DO les écrit tantôt sur une ligne, tantôt sur deux, selon la langue)."""
    merged: list[Item] = []
    for it in items:
        if it.kind == "para" and not it.label and ALLELUIA_ONLY.match(it.text.strip()) and merged and merged[-1].kind in ("para", "versicle"):
            merged[-1].text += " " + it.text.strip()
            continue
        merged.append(it)
    out: list[Item] = []
    pending: str | None = None
    for it in merged:
        if it.kind == "para" and not it.label and OREMUS.match(it.text.strip()):
            pending = it.text.strip() if pending is None else pending + " " + it.text.strip()
            continue
        if pending is not None:
            if it.kind == "para" and not it.label:
                it.text = pending + " " + it.text
            else:
                out.append(Item("para", text=pending))
            pending = None
        out.append(it)
    if pending is not None:
        out.append(Item("para", text=pending))
    return out


LIGATURES = {"ﬁ": "fi", "ﬂ": "fl", "ﬀ": "ff", "ﬃ": "ffi", "ﬄ": "ffl"}


def clean(text: str, paschal: bool | None) -> str:
    """Marques de DO retirées : alléluias entre parenthèses selon le temps, N.b/N.p, waitN, croix ; ligatures typographiques
    (ﬁ, ﬂ) des fichiers français décomposées."""
    if not text:
        return text
    for lig, plain in LIGATURES.items():
        text = text.replace(lig, plain)
    text = re.sub("[\u00ad\u200b\u2060\ufeff]", "", text)  # césures et espaces invisibles (fichiers français)
    if paschal is True:
        text = re.sub(r"\((Al+[eé]l[uú][ij]a[^()]*)\)", r"\1", text, flags=re.I)
    elif paschal is False:
        text = re.sub(r"\s*\(Al+[eé]l[uú][ij]a[^()]*\)", "", text, flags=re.I)
    text = re.sub(r"\bN\.[bp]\b", "N.", text)
    text = re.sub(r"\bwait\d+\b", "", text)
    text = re.sub(r"\s*\+\+?\s*", " ✠ ", text)
    text = re.sub(r"^ ✠ ", "✠ ", text)
    text = re.sub(r"\s+", " ", text).strip()
    return text


# ── Inlines ─────────────────────────────────────────────────────────────────────────────────

SYMS = {"℣": "V", "℟": "R", "✠": "cross"}
RED_TOKEN = re.compile(r"(?<![\wÀ-ÿ])([CJS])\.(?=\s)")


def inl(text: str, lang: str, *, red_tokens: bool = False) -> list:
    text = typo(unicodedata.normalize("NFC", text), lang)
    out: list = []
    for part in re.split(r"(℣\.?|℟\.?|✠)", text):
        if not part:
            continue
        if part[0] in SYMS:
            out.append({"sym": SYMS[part[0]]})
            continue
        if red_tokens:
            pieces = RED_TOKEN.split(part)
            for i, p in enumerate(pieces):
                if i % 2 == 1:
                    out.append({"m": ["red"], "c": [p + "."]})
                elif p:
                    out.append(p)
        else:
            out.append(part)
    return [n for n in out if n != ""]


def item_inlines(it: Item, lang: str, red_tokens: bool = False) -> list:
    if it.kind == "stanza":
        out: list = []
        for i, l in enumerate(it.lines):
            if i:
                out.append({"br": 1})
            out.extend(inl(l, lang, red_tokens=red_tokens))
        return out
    body = inl(it.text, lang, red_tokens=red_tokens)
    if it.kind == "versicle":
        return [{"sym": it.role}, " "] + body
    if it.label:
        return [{"m": ["red"], "c": [it.label]}, " "] + body
    return body


def combined_inlines(items: list[Item], lang: str, red_tokens: bool) -> list:
    out: list = []
    for i, it in enumerate(items):
        if i:
            out.append({"br": 1})
        if it.kind in ("rubric", "ref", "heading"):
            out.append({"m": ["red", "i"], "c": inl(it.text, lang)})
        else:
            out.extend(item_inlines(it, lang, red_tokens))
    return out


# ── Titres des lectures (traduits par Paroissien) ─────────────────────────────────────────────

def _fold(s: str) -> str:
    s = unicodedata.normalize("NFD", s)
    s = "".join(c for c in s if unicodedata.category(c) != "Mn")
    return s.replace("æ", "ae").replace("Æ", "Ae").replace("œ", "oe").replace("Œ", "Oe")


EPISTLES = {"Romanos": "aux Romains", "Corinthios": "aux Corinthiens", "Galatas": "aux Galates", "Ephesios": "aux Éphésiens",
            "Philippenses": "aux Philippiens", "Colossenses": "aux Colossiens", "Thessalonicenses": "aux Thessaloniciens",
            "Hebraeos": "aux Hébreux", "Timotheum": "à Timothée", "Titum": "à Tite"}
APOSTLES = {"Jacobi": "Jacques", "Petri": "Pierre", "Joannis": "Jean", "Joanni": "Jean", "Judae": "Jude"}
PROPHETS = {"Isaiae": "Isaïe", "Jeremiae": "Jérémie", "Ezechielis": "Ézéchiel", "Danielis": "Daniel", "Joelis": "Joël",
            "Jonae": "Jonas", "Malachiae": "Malachie", "Michaeae": "Michée", "Osee": "Osée", "Zachariae": "Zacharie", "Amos": "Amos"}
BOOKS = {"Apocalypsis": "de l’Apocalypse de saint Jean, Apôtre", "Deuteronomii": "du Deutéronome", "Esdrae": "d’Esdras",
         "Esther": "d’Esther", "Exodi": "de l’Exode", "Genesis": "de la Genèse", "Judith": "de Judith", "Levitici": "du Lévitique",
         "Machabaeorum": "des Maccabées", "Numeri": "des Nombres", "Regum": "des Rois", "Sapientiae": "de la Sagesse",
         "Tobiae": "de Tobie", "Ecclesiastici": "de l’Ecclésiastique", "Proverbiorum": "des Proverbes"}
EVANGELISTS = {"Joannem": "Jean", "Ioannem": "Jean", "Lucam": "Luc", "Marcum": "Marc", "Matthaeum": "Matthieu", "Mattheum": "Matthieu"}


def reading_title(text: str) -> str | None:
    """Titre latin d'une lecture (« Lectio Epístolæ beáti Pauli Apóstoli ad Romános ») → français ; None sinon."""
    t = _fold(text).strip().rstrip(".")
    cross = "✠" in text or "+" in text
    c = " ✠" if cross else ""
    m = re.match(r"^Lectio (?:prima )?(?:Epistolae )?(?:beati|sancti) Pauli Apostoli ad (\w+)$", t, re.I)
    if m and m.group(1) in EPISTLES:
        first = "la première Épître" if "prima" in t else "l’Épître"
        return f"Lecture de {first} de saint Paul, Apôtre, {EPISTLES[m.group(1)]}"
    m = re.match(r"^Lectio Epistolae (?:beati|sancti) (\w+) Apostoli$", t, re.I)
    if m and m.group(1) in APOSTLES:
        return f"Lecture de l’Épître de saint {APOSTLES[m.group(1)]}, Apôtre"
    if re.match(r"^Lectio Actuum Apostolorum$", t, re.I):
        return "Lecture des Actes des Apôtres"
    m = re.match(r"^Lectio (\w+) Prophetae$", t, re.I)
    if m and m.group(1) in PROPHETS:
        return f"Lecture du prophète {PROPHETS[m.group(1)]}"
    m = re.match(r"^Lectio libri (\w+)", t, re.I)
    if m and m.group(1) in BOOKS:
        return f"Lecture du livre {BOOKS[m.group(1)]}"
    m = re.match(r"^Sequentia[\s✠+]*sancti Evangel\w* secundum (\w+)$", t, re.I)
    if m and m.group(1) in EVANGELISTS:
        return f"Suite{c} du saint Évangile selon saint {EVANGELISTS[m.group(1)]}"
    m = re.match(r"^Initium[\s✠+]*sancti Evangelii secundum (\w+)$", t, re.I)
    if m and m.group(1) in EVANGELISTS:
        return f"Commencement{c} du saint Évangile selon saint {EVANGELISTS[m.group(1)]}"
    m = re.match(r"^Lectio sancti Evangelii secundum (\w+)$", t, re.I)
    if m and m.group(1) in EVANGELISTS:
        return f"Lecture du saint Évangile selon saint {EVANGELISTS[m.group(1)]}"
    m = re.match(r"^Passio Domini nostri [IJ]esu Christi secundum (\w+)$", t, re.I)
    if m and m.group(1) in EVANGELISTS:
        return f"Passion de notre Seigneur Jésus-Christ selon saint {EVANGELISTS[m.group(1)]}"
    return None


# ── Blocs ───────────────────────────────────────────────────────────────────────────────────

@dataclass
class Block:
    id: str
    type: str
    la: list | None = None
    fr: list | None = None        # français du paquet privé (DO ou Paroissien)
    pub: list | None = None       # français du paquet public : rédigé par Paroissien seulement
    attrs: dict = field(default_factory=dict)
    source: list | None = None

    def row(self, public: bool) -> dict | None:
        t = {}
        if self.la:
            t["la"] = self.la
        fr = self.pub if public else (self.fr or self.pub)
        if fr:
            t["fr"] = fr
        if not t:
            return None
        row = {"id": self.id, "type": self.type, **self.attrs}
        if self.source:
            row["x-source"] = self.source
        row["t"] = t
        return row


def _btype(it: Item, heading_level: int) -> tuple[str, dict]:
    if it.kind == "versicle":
        return "versicle", {"role": it.role}
    if it.kind in ("rubric", "ref"):
        return "rubric", {}
    if it.kind == "heading":
        return "heading", {"level": heading_level}
    if it.kind == "stanza":
        return "stanza", {"kind": "hymn"}
    return "para", {}


def _own_fr(it: Item, title: bool) -> list | None:
    """Français de Paroissien : références (identiques en latin), libellés connus, titres des lectures."""
    if it.kind == "ref":
        return inl(it.text, "la")
    if it.kind == "rubric" and it.text in RUBRIC_FR:
        return inl(RUBRIC_FR[it.text], "fr")
    if title and it.kind == "para":
        tr = reading_title(it.text)
        if tr:
            return inl(tr, "fr")
    return None


def _reorder_refs(la: list[Item], fr: list[Item]) -> list[Item] | None:
    """Même suite de textes, références placées autrement (« Tu es Petrus… » puis « !Matt 16:18 ») : on les remet en place."""
    if len(la) != len(fr):
        return None
    la_text = [i.shape() for i in la if i.kind != "ref"]
    fr_text = [i for i in fr if i.kind != "ref"]
    fr_refs = [i for i in fr if i.kind == "ref"]
    if la_text != [i.shape() for i in fr_text] or len(fr_refs) != sum(1 for i in la if i.kind == "ref"):
        return None
    out, ti, ri = [], 0, 0
    for a in la:
        if a.kind == "ref":
            out.append(fr_refs[ri])
            ri += 1
        else:
            out.append(fr_text[ti])
            ti += 1
    return out


def _same(a: Item, b: Item) -> bool:
    """Le « français » est le latin recopié (rubriques latines laissées telles quelles dans les fichiers français)."""
    def key(i: Item) -> str:
        return re.sub(r"[\W_]+", "", _fold(i.text or " ".join(i.lines))).lower()
    return bool(key(a)) and key(a) == key(b)


def _pair_inlines(a: Item, b: Item, red_tokens: bool) -> list:
    if _same(a, b):
        return item_inlines(a, "la", red_tokens)
    if b.kind != a.kind and a.kind in ("para", "versicle"):
        b = Item(a.kind, b.text or " ".join(b.lines), a.role, b.label)
    elif b.kind != a.kind and a.kind == "stanza":
        b = Item("stanza", lines=b.lines or [b.text])
    return item_inlines(b, "fr", red_tokens)


def _align_long(prefix: str, la: list[Item], fr: list[Item], stats: dict, where: str, red_tokens: bool, heading_level: int):
    """Longues cérémonies (Semaine sainte, Chandeleur) : alignement par repères (rubriques latines identiques, titres,
    forme des lignes). Les plages appariées 1:1 gardent un bloc par ligne ; les autres deviennent un bloc à deux langues."""
    def token(i: Item) -> str:
        if i.kind in ("rubric", "ref", "heading"):
            return i.kind + ":" + re.sub(r"[\W_]+", "", _fold(i.text)).lower()[:40]
        return i.shape()
    sm = difflib.SequenceMatcher(None, [token(i) for i in la], [token(i) for i in fr], autojunk=False)
    blocks: list[Block] = []
    n = 0
    matched = 0
    for tag, i1, i2, j1, j2 in sm.get_opcodes():
        if tag == "equal":
            for a, b in zip(la[i1:i2], fr[j1:j2]):
                n += 1
                t, attrs = _btype(a, heading_level)
                own = _own_fr(a, False)
                fr_inl = own if a.kind == "ref" else _pair_inlines(a, b, red_tokens)
                blocks.append(Block(f"{prefix}.{n}", t, item_inlines(a, "la", red_tokens), fr_inl, own, attrs))
                matched += 1
        elif tag == "insert":
            if blocks:
                prev = blocks[-1]
                prev.fr = (prev.fr or []) + [{"br": 1}] + combined_inlines(fr[j1:j2], "fr", red_tokens)
                if prev.type == "heading":
                    prev.type, prev.attrs = "para", {}
            else:
                n += 1
                blocks.append(Block(f"{prefix}.{n}", "para", None, combined_inlines(fr[j1:j2], "fr", red_tokens)))
        elif tag == "delete":
            for a in la[i1:i2]:
                n += 1
                t, attrs = _btype(a, heading_level)
                own = _own_fr(a, False)
                blocks.append(Block(f"{prefix}.{n}", t, item_inlines(a, "la", red_tokens), None, own, attrs))
        else:
            n += 1
            blocks.append(Block(f"{prefix}.{n}", "para", combined_inlines(la[i1:i2], "la", red_tokens),
                                combined_inlines(fr[j1:j2], "fr", red_tokens)))
    if matched < 0.6 * max(len(la), len(fr)):
        return None
    stats["partial"] += 1
    stats["partial_list"].append(f"{where} (la {len(la)}, fr {len(fr)}, {matched} lignes appariées)")
    return blocks


def items_to_blocks(prefix: str, la: list[Item], fr: list[Item] | None, stats: dict, where: str, *, red_tokens=False,
                    dropcap=False, heading_level=3, reading=False) -> list[Block]:
    """Aligne latin et français : ligne à ligne si les deux ont la même forme, sinon un bloc pour la section."""
    blocks: list[Block] = []
    if fr is not None and not (len(fr) == len(la) and all(a.shape() == b.shape() for a, b in zip(la, fr))):
        fr = _reorder_refs(la, fr) or fr
    first_para = next((i for i, a in enumerate(la) if a.kind == "para"), None)
    if fr is not None and len(fr) == len(la) and all(a.shape() == b.shape() for a, b in zip(la, fr)):
        stats["aligned"] += 1
        for i, (a, b) in enumerate(zip(la, fr), 1):
            t, attrs = _btype(a, heading_level)
            title = reading and i - 1 == first_para
            if dropcap and i - 1 == first_para and t == "para":
                attrs = {**attrs, "dropCap": True}
            own = _own_fr(a, title)
            fr_inl = _pair_inlines(a, b, red_tokens)
            if a.kind == "ref":
                fr_inl = own
            elif title and own and reading_title(b.text):
                fr_inl = own  # DO a laissé le titre latin dans le fichier français
            if title and own:
                attrs = {**attrs, "style": "intro"}
            blocks.append(Block(f"{prefix}.{i}", t, item_inlines(a, "la", red_tokens), fr_inl, own, attrs))
        return blocks
    if fr is None:
        stats["no_fr"] += 1
        stats["no_fr_list"].append(where)
        for i, a in enumerate(la, 1):
            t, attrs = _btype(a, heading_level)
            title = reading and i - 1 == first_para
            if dropcap and i - 1 == first_para and t == "para":
                attrs = {**attrs, "dropCap": True}
            own = _own_fr(a, title)
            if title and own:
                attrs = {**attrs, "style": "intro"}
            blocks.append(Block(f"{prefix}.{i}", t, item_inlines(a, "la", red_tokens), None, own, attrs))
        return blocks
    if len(la) >= 15 and len(fr) >= 15:
        fine = _align_long(prefix, la, fr, stats, where, red_tokens, heading_level)
        if fine is not None:
            return fine
    stats["combined"] += 1
    stats["combined_list"].append(f"{where} (la {len(la)}, fr {len(fr)})")
    if len(la) == 1 and len(fr) == 1:
        same = la[0].kind == fr[0].kind
        t, attrs = _btype(la[0], heading_level) if same else ("para", {})
        return [Block(f"{prefix}.1", t, item_inlines(la[0], "la", red_tokens), item_inlines(fr[0], "fr", red_tokens), None, attrs)]
    return [Block(f"{prefix}.1", "para", combined_inlines(la, "la", red_tokens), combined_inlines(fr, "fr", red_tokens))]


def heading(bid: str, fr: str, la: str | None, level: int, source: list | None = None) -> Block:
    return Block(bid, "heading", inl(la, "la") if la else None, inl(fr, "fr"), inl(fr, "fr"), {"level": level}, source)


def rubric(bid: str, fr: str, la: str | None = None) -> Block:
    return Block(bid, "rubric", inl(la, "la") if la else None, inl(fr, "fr"), inl(fr, "fr"))


def x_source(lib: do.Library, chain: list[tuple[str, str]]) -> list:
    seen, out = set(), []
    for key, section in chain:
        rel = lib.relpath(key)
        if not rel or (rel, section) in seen:
            continue
        seen.add((rel, section))
        # Forme courte (dépôt et commit dans x-divinum-officium de publication.json) : 8 000 titres la portent.
        out.append({"file": rel.removeprefix("web/www/"), "section": section})
    return out


# ── Formulaire ──────────────────────────────────────────────────────────────────────────────

class Mass:
    """Un formulaire : fichier de DO, contexte(s), règles de 1960."""

    def __init__(self, builder: "Builder", winner: str | None, ctx: Ctx, *, commune_override: str | None = None,
                 name_override: tuple[str, str] | None = None, tempora: bool = False):
        self.b, self.winner, self.ctx = builder, winner, ctx
        self.dctx = ctx.do()
        la = builder.la
        self.tempora = tempora
        self.rule = self._rule(la, winner, self.dctx) if winner else []
        com = None
        if commune_override:
            com = ("ex", commune_override)
        elif winner:
            com = do.commune_of(la, winner, self.dctx, ctx.group == "P")
        self.commune_type, self.commune = com if com else (None, None)
        if self.commune:
            self.dctx = tuple(sorted({**dict(self.dctx), "commune": self.commune}.items()))
        self.communerule = self._rule(la, self.commune, self.dctx) if self.commune and self.commune_type == "ex" else []
        rf = la.rank_fields(winner, self.dctx) if winner else []
        try:
            self.rank = float(rf[2]) if len(rf) > 2 else 0.0
        except ValueError:
            self.rank = 0.0
        self.rank_title = (rf[0].strip() if rf else "") or (la.officium(winner, self.dctx) if winner else "")
        self.name_override = name_override
        self.sancti = (winner or "").startswith(("Sancti", "Commune")) or name_override is not None
        self.cal_class: str | None = None  # classe du calendrier de l'app (I… IV, COMMEMORATION, VIGIL_*)
        self.sunday = None
        if tempora and winner:
            week = ctx.dayname
            sunday = f"{week}-0"
            if re.match(r"^(Epi1|Nat)", week):
                sunday = "Epi1-0a"
            elif week.startswith("Pent01"):
                sunday = "Pent01-0a"
            self.sunday = f"Tempora/{sunday}"

    @staticmethod
    def _rule(lib: do.Library, key: str | None, ctx: tuple) -> list[str]:
        if not key:
            return []
        r = lib.resolve(key, "Rule", ctx)
        return [l.strip() for l in (r.lines if r else []) if l.strip()]

    def has_rule(self, pattern: str, commune: bool = False) -> bool:
        lines = self.rule + (self.communerule if commune else [])
        return any(re.search(pattern, l, re.I) for l in lines)

    # Règles de 1960 (propers.pl).

    def gloria(self) -> bool:
        flag = self.ctx.dow != 0
        defunct = self.commune == "Commune/C9" or (self.winner or "").endswith("C9")
        if self.has_rule(r"no Gloria"):
            flag = True
        elif self.has_rule(r"Gloria", commune=True):
            flag = False
        elif defunct:
            flag = True
        elif self.sancti:
            flag = False
        elif re.search("Adv|Quad", self.ctx.dayname):
            flag = True
        elif re.search("Pasc", self.ctx.dayname):
            flag = False
        return not flag

    def credo(self) -> bool:
        flag = True
        title = self.rank_title
        if (self.ctx.dow == 0 or (self.rank >= 5 and self.sancti and not re.search("Vigil", title, re.I))
                or (re.search("Octav", title, re.I) and not re.search("post Octavam|Simplex", title, re.I))):
            flag = False
        if self.has_rule(r"no Credo"):
            flag = True
        elif self.has_rule(r"Credo", commune=True):
            flag = False
        if self.has_rule(r"CredoDA"):
            flag = True
        # Rubriques de 1960, n. 471 : pas de Credo aux fêtes de IIIᵉ classe (sauf saint Barnabé, apôtre), aux
        # commémoraisons ni aux féries de IIIᵉ et IVᵉ classe ; DO l'indique encore pour Notre-Dame de Lourdes, le
        # Saint Nom de Marie, Notre-Dame de la Merci et le samedi de la 2ᵉ semaine après Pâques.
        if self.ctx.dow != 0 and self.cal_class in ("III", "IV", "COMMEMORATION") and self.winner != "Sancti/06-11":
            flag = True
        return not flag

    def preface(self, season: str | None = None, month: int | None = None, day: int | None = None, dow: int | None = None) -> tuple[str, str | None]:
        season = season or self.ctx.season
        month = self.ctx.month if month is None else month
        day = self.ctx.day if day is None else day
        dow = self.ctx.dow if dow is None else dow
        for line in self.rule:
            m = re.match(r"^Prefatio1960=([A-Za-z0-9]+)", line)
            if m:
                return m.group(1), None
        for line in self.rule + self.communerule:
            m = re.match(r"^Prefatio=([A-Za-z0-9]+)(?:=([^;]*);?)?", line)
            if m:
                return m.group(1), (m.group(2) or None)
        if season == "adv":
            return "Adv", None
        if (month == 12 and day > 24) or (month == 1 and day == 1):
            return "Nat", None
        if month == 1 and 5 < day < 14:
            return "Epi", None
        if season == "quad":
            return "Quad", None
        if season in ("passio", "holyweek"):
            return "Quad5", None
        if season in ("pasc0", "pasch"):
            return "Pasch", None
        if season == "asc":
            return "Asc", None
        if season == "pent":
            return "Spiritu", None
        title = self.rank_title + " " + (self.b.la.officium(self.winner, self.dctx) if self.winner else "")
        if re.search(r"Beat[aæ].*?Mari[aæ].*?Virg", title, re.I):
            return "Maria", None
        if self.commune and re.search(r"C1p?$|C[234]b", self.commune):
            return "Apostolis", None
        if self.commune == "Commune/C9":
            return "Defunctorum", None
        if dow == 0:
            return "Trinitate", None
        return "Communis", None

    # Sections (getitem, oratio).

    def getitem(self, kind: str) -> tuple[str, str, do.Resolved] | None:
        la, ctx = self.b.la, self.dctx
        for src in (self.winner, self.commune):
            if not src:
                continue
            name = kind
            if kind == "Graduale" and self.ctx.group == "P" and la.has(src, "GradualeP", ctx):
                name = "GradualeP"
            if kind == "Graduale" and re.search("Quad", self.ctx.dayname) and la.has(src, "Tractus", ctx):
                name = "Tractus"
            r = la.resolve(src, name, ctx)
            if r:
                return src, name, r
        if self.sunday:
            name = kind
            if kind == "Graduale" and self.ctx.dow > 0 and la.has(self.sunday, "GradualeF", ctx):
                name = "GradualeF"
            r = la.resolve(self.sunday, name, ctx)
            if r:
                return self.sunday, name, r
        return None

    def oratio(self, kind: str) -> tuple[str, str, do.Resolved] | None:
        la, ctx = self.b.la, self.dctx
        src = self.winner
        if src and self.has_rule("Oratio Dominica") and not la.has(src, kind, ctx) and self.sunday:
            src = self.sunday
        if src:
            name = kind + "W" if self.ctx.dow > 0 and la.has(src, kind + "W", ctx) else kind
            r = la.resolve(src, name, ctx)
            if r:
                return src, name, r
        if self.commune:
            r = la.resolve(self.commune, kind, ctx)
            if r:
                return self.commune, kind, r
        if self.sunday:
            r = la.resolve(self.sunday, kind, ctx)
            if r:
                return self.sunday, kind, r
        return None

    def name_for(self, lib: do.Library, kind: str) -> str | None:
        if self.name_override:
            return self.name_override[0 if lib.lang == "Latin" else 1]
        if not self.winner:
            return None
        r = lib.resolve(self.winner, "Name", self.dctx)
        if not r:
            return None
        lines = [l.strip() for l in r.lines if l.strip()]
        for l in lines:
            if l.startswith(kind + "="):
                return l.split("=", 1)[1].strip()
        plain = [l for l in lines if "=" not in l]
        return plain[0] if plain else None


def drop_first_conclusion(lines: list[str]) -> list[str]:
    """« Sub unica conclusione » (rubriques de 1960) : la première conclusion tombe quand l'oraison en a plusieurs."""
    idx = [i for i, l in enumerate(lines) if re.match(r"^\s*\$(Per|Qui) ", l, re.I)]
    if len(idx) < 2:
        return lines
    return lines[:idx[0]] + lines[idx[0] + 1:]


def replace_n(lines: list[str], name: str | None) -> list[str]:
    if not name:
        return lines
    out = []
    for l in lines:
        if "N." in l:
            l2 = re.sub(r"N\. .*? N\.", name, l, count=1)
            if l2 == l:
                l2 = re.sub(r"N\.", name, l, count=1)
            l = l2
        out.append(l)
    return out


# ── Construction ────────────────────────────────────────────────────────────────────────────

class Builder:
    def __init__(self, www: Path):
        self.www = www
        self.la = do.Library(www, "Latin", OVERLAY)
        self.fr = do.Library(www, "Francais", OVERLAY)
        self.subs = do.divinum.tempora_substitutions(www)
        self.report = {"macros": Counter()}
        self.r_la = Renderer(self.la, "la", self.report)
        self.r_fr = Renderer(self.fr, "fr", self.report)
        self.stats = {"aligned": 0, "combined": 0, "no_fr": 0, "partial": 0, "no_fr_list": [], "combined_list": [], "partial_list": []}
        self.files_read: set[str] = {self.la.relpath("Ordo/Prayers"), self.fr.relpath("Ordo/Prayers"), "LICENSE",
                                      "web/www/Tabulae/Tempora/Generale.txt"}
        self.missing_pieces: list[str] = []
        self.no_name: list[str] = []

    # Français : même chaîne que le latin, dans l'arbre français.
    def french(self, chain: list[tuple[str, str]], name_kind: str | None, mass: Mass) -> list[str] | None:
        lines = None
        for key, section in chain:
            r = self.fr.resolve(key, section, mass.dctx)
            if r:
                self.files_read.add(self.fr.relpath(key))
                lines = r.lines
                break
        if lines is None:
            # Section latine faite d'inclusions seulement (« @:Oratio Pauli », « _ », « $Oremus ») : on la rejoue en français.
            for key, section in chain:
                loaded = self.la.load(key, mass.dctx)
                raw = loaded[0].get(section) if loaded else None
                if raw and any(l.lstrip().startswith("@") for l in raw) and all(
                        not l.strip() or l.strip() == "_" or re.match(r"^[vr]\.\s*~?$", l.strip()) or l.strip()[0] in "@$&" for l in raw):
                    lines = self.fr._expand(raw, mass.dctx, 0, [])
                    if lines is not None:
                        break
        if lines is not None and name_kind:
            lines = replace_n(lines, mass.name_for(self.fr, name_kind))
        return lines

    def section_items(self, mass: Mass, src: str, section: str, resolved: do.Resolved, *, name_kind=None, stanzas=False,
                      full_text=False):
        self.files_read.add(self.la.relpath(src))
        for k, _ in resolved.chain:
            if self.la.relpath(k):
                self.files_read.add(self.la.relpath(k))
        lines_la = resolved.lines
        if name_kind:
            lines_la = replace_n(lines_la, mass.name_for(self.la, name_kind))
            if any(re.search(r"\bN\.", l) for l in lines_la) and (mass.winner or "").startswith("Sancti"):
                self.no_name.append(f"{mass.winner}:{section}")
        lines_fr = self.french([(src, section)] + resolved.chain[1:], name_kind, mass)
        if name_kind and mass.has_rule(r"Sub unica concl"):
            lines_la, lines_fr = drop_first_conclusion(lines_la), drop_first_conclusion(lines_fr) if lines_fr else lines_fr
        passiontide = (mass.tempora and re.match(r"Quad5|Quad6", mass.ctx.dayname) is not None and mass.ctx.dow < 6
                       and "Septem Dolorum" not in mass.rank_title)
        paschal = mass.ctx.group == "P"
        opts = dict(passiontide=passiontide, stanzas="strict" if section == "Sequentia" else stanzas, full_text=full_text, paschal=paschal)
        la_items = self.r_la.render(lines_la, **opts)
        fr_items = self.r_fr.render(lines_fr, **opts) if lines_fr is not None else None
        if section == "Super populum":
            # Le titre « Oratio super populum » double l'intitulé ; l'oraison n'est pas un verset.
            for items in (la_items, fr_items):
                if items and items[0].kind == "rubric":
                    del items[0]
                for it in items or []:
                    if it.kind == "versicle" and len(it.text) > 80:
                        it.kind, it.role = "para", None
        return la_items, fr_items, x_source(self.la, [(src, section)] + resolved.chain[1:])

    def piece(self, mass: Mass, prefix: str, pid: str, fr_title: str, la_title: str, got, *, name_kind=None, red_tokens=False,
              dropcap=False, level=2, stanzas=False, extra: list[Block] | None = None) -> list[Block]:
        src, section, resolved = got
        la_items, fr_items, source = self.section_items(mass, src, section, resolved, name_kind=name_kind, stanzas=stanzas)
        blocks = [heading(f"{prefix}.{pid}", fr_title, la_title, level, source)]
        blocks += extra or []
        blocks += items_to_blocks(f"{prefix}.{pid}", la_items, fr_items, self.stats, f"{mass.winner or mass.commune}:{section}",
                                  red_tokens=red_tokens, dropcap=dropcap, reading=re.match(r"Lectio|Evangelium", section) is not None)
        return blocks

    # Variantes des Communs et des saints (temps pascal, Septuagésime).

    def variant_blocks(self, prefix: str, pid: str, main: Mass, others: list[tuple[str, Mass]], getter, *, name_kind=None,
                       stanzas=False) -> list[Block]:
        out: list[Block] = []
        main_got = getter(main)
        main_text = self._text_of(main, main_got, name_kind)
        seen = {main_text}
        for group, m in others:
            got = getter(m)
            if not got:
                continue
            text = self._text_of(m, got, name_kind)
            if text in seen:
                continue
            seen.add(text)
            key = group
            if group == "N" and main.ctx.group == "Q":
                key = "N-before-Q"
            suffix, fr, la = VARIANT[key]
            vid = f"{prefix}.{pid}.{suffix}"
            src, section, resolved = got
            la_items, fr_items, source = self.section_items(m, src, section, resolved, name_kind=name_kind, stanzas=stanzas)
            rb = rubric(vid, fr.rstrip(" :"), la.rstrip(":"))
            rb.source = source
            out.append(rb)
            out += items_to_blocks(vid, la_items, fr_items, self.stats, f"{m.winner or m.commune}:{section}",
                                   reading=re.match(r"Lectio|Evangelium", section) is not None)
        return out

    def _text_of(self, mass: Mass, got, name_kind) -> str:
        if not got:
            return ""
        lines = got[2].lines
        if name_kind:
            lines = replace_n(lines, mass.name_for(self.la, name_kind))
        items = self.r_la.render(lines, paschal=mass.ctx.group == "P")
        return json.dumps([(i.kind, i.text, i.lines) for i in items], ensure_ascii=False)

    # Formulaire complet.

    def mass_blocks(self, prefix: str, mass: Mass, title_fr: str, title_la: str | None, rank_text: str, *, others=(),
                    commemorations=(), extra_rubrics=(), preface_text: str | None = None, title_level=1) -> list[Block]:
        la, ctx = self.la, mass.dctx
        blocks: list[Block] = []
        src_title = x_source(la, [(mass.winner, "Officium")]) if mass.winner and la.relpath(mass.winner) else None
        blocks.append(heading(f"{prefix}.titre", title_fr, title_la, title_level, src_title))
        if rank_text:
            blocks.append(rubric(f"{prefix}.rang", rank_text))
        for i, (fr, la_) in enumerate(extra_rubrics, 1):
            blocks.append(rubric(f"{prefix}.note{i if i > 1 else ''}", fr, la_))
        full_text = mass.has_rule(r"^Full text$")
        red = mass.tempora and mass.ctx.dayname.startswith("Quad6")
        # Cérémonies avant la messe (rameaux, cierges, cendres) ou office complet (Vendredi saint, Vigile pascale).
        if mass.winner and la.has(mass.winner, "Prelude", ctx):
            fr_t, la_t = PRELUDE_TITLES.get(mass.winner, ("Avant la messe", "Ante Missam"))
            r = la.resolve(mass.winner, "Prelude", ctx)
            la_items, fr_items, source = self.section_items(mass, mass.winner, "Prelude", r, stanzas=True)
            blocks.append(heading(f"{prefix}.prelude", fr_t, la_t, 2, source))
            blocks += self._split_prelude(f"{prefix}.prelude", la_items, fr_items, f"{mass.winner}:Prelude", red)
        if full_text and not la.has(mass.winner, "Introitus", ctx):
            return blocks
        g = mass.getitem("Introitus")
        if g:
            blocks += self.piece(mass, prefix, "introit", "Introït", "Introitus", g, dropcap=True)
            blocks += self.variant_blocks(prefix, "introit", mass, others, lambda m: m.getitem("Introitus"))
        else:
            self.missing_pieces.append(f"{prefix}:Introitus")
        if mass.tempora and re.match(r"Tempora/Epi1-[1-6]$", mass.winner or ""):
            # Féries après la Sainte Famille : temps de Noël jusqu'au 13 janvier (n. 72), puis temps après l'Épiphanie.
            rb = rubric(f"{prefix}.gloria", "On dit le Gloria in excelsis jusqu’au 13 janvier ; ensuite on ne le dit pas.")
            rb.attrs = {"gloria": True, "gloriaUntil": "01-13"}
        else:
            rb = rubric(f"{prefix}.gloria", "On dit le Gloria in excelsis." if mass.gloria() else "On ne dit pas le Gloria in excelsis.",
                        "Gloria in excelsis." if mass.gloria() else None)
            rb.attrs = {"gloria": mass.gloria()}
        blocks.append(rb)
        # Collecte (et Flectamus genua des Quatre-Temps).
        ember = None
        for line in mass.rule:
            m = re.match(r"^LectioL(\d)", line)
            if m:
                ember = int(m.group(1))
        flect = self.r_la.prayer("Flectamus")
        flect_fr = self.r_fr.prayer("Flectamus")
        g = mass.oratio("Oratio")
        if g:
            extra = []
            if ember and mass.ctx.group != "P":
                extra = items_to_blocks(f"{prefix}.collecte.fg", self.r_la.render(flect), self.r_fr.render(flect_fr) if flect_fr else None,
                                        self.stats, "Ordo/Prayers:Flectamus")
            blocks += self.piece(mass, prefix, "collecte", "Collecte", "Oratio", g, name_kind="Oratio", extra=extra)
            blocks += self.variant_blocks(prefix, "collecte", mass, others, lambda m: m.oratio("Oratio"), name_kind="Oratio")
            blocks += self.commemorations(prefix, "collecte", mass, commemorations, "Oratio")
            blocks += self.alternatives(prefix, "collecte", mass, "Oratio")
        else:
            self.missing_pieces.append(f"{prefix}:Oratio")
        # Leçons des Quatre-Temps.
        if ember:
            for i in range(1, ember + 1):
                r = la.resolve(mass.winner, f"LectioL{i}", ctx)
                if r:
                    blocks += self.piece(mass, prefix, f"lecon-{i}", "Leçon", "Lectio", (mass.winner, f"LectioL{i}", r))
                r = la.resolve(mass.winner, f"GradualeL{i}", ctx)
                if r:
                    blocks += self.piece(mass, prefix, f"graduel-{i}", "Graduel", "Graduale", (mass.winner, f"GradualeL{i}", r))
                r = la.resolve(mass.winner, f"OratioL{i}", ctx)
                if r:
                    extra = []
                    if i < ember and mass.ctx.group != "P":
                        extra = items_to_blocks(f"{prefix}.oraison-{i}.fg", self.r_la.render(flect),
                                                self.r_fr.render(flect_fr) if flect_fr else None, self.stats, "Ordo/Prayers:Flectamus")
                    blocks += self.piece(mass, prefix, f"oraison-{i}", "Oraison", "Oratio", (mass.winner, f"OratioL{i}", r), extra=extra)
        # Épître ou leçon.
        g = mass.getitem("Lectio")
        if g:
            first = next((l for l in g[2].lines if l.strip() and not l.strip().startswith("!")), "")
            epistle = re.search(r"Ep[ií]st[oó]l", first) is not None
            blocks += self.piece(mass, prefix, "epitre" if epistle else "lecon", "Épître" if epistle else "Leçon",
                                 "Epistola" if epistle else "Lectio", g)
            blocks += self.variant_blocks(prefix, "epitre" if epistle else "lecon", mass, others, lambda m: m.getitem("Lectio"))
        else:
            self.missing_pieces.append(f"{prefix}:Lectio")
        # Graduel, alléluia ou trait.
        g = mass.getitem("Graduale")
        if g:
            fr_t, la_t, pid = self.gradual_title(g, mass.ctx.group)
            blocks += self.piece(mass, prefix, pid, fr_t, la_t, g)
            blocks += self.variant_blocks(prefix, pid, mass, others, lambda m: m.getitem("Graduale"))
        # Séquence.
        seq = None
        if mass.winner and la.has(mass.winner, "Sequentia", ctx):
            seq = (mass.winner, "Sequentia", la.resolve(mass.winner, "Sequentia", ctx))
        elif mass.commune and mass.has_rule("Sequentia", commune=True) and la.has(mass.commune, "Sequentia", ctx):
            seq = (mass.commune, "Sequentia", la.resolve(mass.commune, "Sequentia", ctx))
        if seq and seq[2]:
            blocks += self.piece(mass, prefix, "sequence", "Séquence", "Sequentia", seq, stanzas=True)
        # Évangile (et lavement des pieds le Jeudi saint).
        g = mass.getitem("Evangelium")
        if g:
            blocks += self.piece(mass, prefix, "evangile", "Évangile", "Evangelium", g, red_tokens=red)
            blocks += self.variant_blocks(prefix, "evangile", mass, others, lambda m: m.getitem("Evangelium"))
        else:
            self.missing_pieces.append(f"{prefix}:Evangelium")
        if mass.has_rule("Maundi") and la.has(mass.winner, "Maundi", ctx):
            blocks += self.piece(mass, prefix, "mandatum", "Lavement des pieds", "Mandatum", (mass.winner, "Maundi", la.resolve(mass.winner, "Maundi", ctx)), stanzas=True)
        rb = rubric(f"{prefix}.credo", "On dit le Credo." if mass.credo() else "On ne dit pas le Credo.",
                    "Credo." if mass.credo() else None)
        rb.attrs = {"credo": mass.credo()}
        blocks.append(rb)
        for kind, pid, fr_t, la_t in (("Offertorium", "offertoire", "Offertoire", "Offertorium"),):
            g = mass.getitem(kind)
            if g:
                blocks += self.piece(mass, prefix, pid, fr_t, la_t, g)
                blocks += self.variant_blocks(prefix, pid, mass, others, lambda m, k=kind: m.getitem(k))
            else:
                self.missing_pieces.append(f"{prefix}:{kind}")
        g = mass.oratio("Secreta")
        if g:
            blocks += self.piece(mass, prefix, "secrete", "Secrète", "Secreta", g, name_kind="Secreta")
            blocks += self.variant_blocks(prefix, "secrete", mass, others, lambda m: m.oratio("Secreta"), name_kind="Secreta")
            blocks += self.commemorations(prefix, "secrete", mass, commemorations, "Secreta")
            blocks += self.alternatives(prefix, "secrete", mass, "Secreta")
        else:
            self.missing_pieces.append(f"{prefix}:Secreta")
        # Préface (renvoi) et Communicantes propres.
        blocks.append(heading(f"{prefix}.preface", "Préface", "Præfatio", 2))
        preface_attrs = {"preface": mass.preface()[0].lower()}
        if mass.tempora and re.match(r"Tempora/Epi1-[1-6]$", mass.winner or ""):
            preface_text = "Préface de l’Épiphanie jusqu’au 13 janvier ; ensuite préface commune."
            preface_attrs = {"preface": "epi", "prefaceAfter": {"01-13": "communis"}}
        comm_key, hanc_key, comm = self.communicantes(mass)
        if comm_key:
            preface_attrs["communicantes"] = comm_key
        if hanc_key:
            preface_attrs["hancIgitur"] = hanc_key
        rb = rubric(f"{prefix}.preface.1", preface_text or self.preface_text(mass), self.preface_la(mass))
        rb.attrs = preface_attrs
        blocks.append(rb)
        if comm:
            blocks.append(rubric(f"{prefix}.preface.2", comm))
        if mass.has_rule(r"^Communicantes$") and la.has(mass.winner, "Communicantes", ctx):
            blocks += self.piece(mass, prefix, "communicantes", "Communicantes propre", "Communicantes",
                                 (mass.winner, "Communicantes", la.resolve(mass.winner, "Communicantes", ctx)))
        g = mass.getitem("Communio")
        if g:
            blocks += self.piece(mass, prefix, "communion", "Communion", "Communio", g)
            blocks += self.variant_blocks(prefix, "communion", mass, others, lambda m: m.getitem("Communio"))
        else:
            self.missing_pieces.append(f"{prefix}:Communio")
        g = mass.oratio("Postcommunio")
        if g:
            blocks += self.piece(mass, prefix, "postcommunion", "Postcommunion", "Postcommunio", g, name_kind="Postcommunio")
            blocks += self.variant_blocks(prefix, "postcommunion", mass, others, lambda m: m.oratio("Postcommunio"), name_kind="Postcommunio")
            blocks += self.commemorations(prefix, "postcommunion", mass, commemorations, "Postcommunio")
            blocks += self.alternatives(prefix, "postcommunion", mass, "Postcommunio")
        else:
            self.missing_pieces.append(f"{prefix}:Postcommunio")
        if mass.has_rule("Super pop"):
            g = mass.getitem("Super populum")
            if g:
                blocks += self.piece(mass, prefix, "super-populum", "Oraison sur le peuple", "Oratio super populum", g)
        if mass.has_rule("Post Missam") and la.has(mass.winner, "Post Missam", ctx):
            r = la.resolve(mass.winner, "Post Missam", ctx)
            la_items, fr_items, source = self.section_items(mass, mass.winner, "Post Missam", r, stanzas=True)
            blocks.append(heading(f"{prefix}.apres", "Après la messe", "Post Missam", 2, source))
            blocks += self._split_prelude(f"{prefix}.apres", la_items, fr_items, f"{mass.winner}:Post Missam", red)
        return blocks

    def _split_prelude(self, prefix: str, la: list[Item], fr: list[Item] | None, where: str, red: bool) -> list[Block]:
        """Longues sections (cérémonies) : découpées aux titres « !! » pour aligner par morceaux."""
        def chunks(items: list[Item]) -> list[list[Item]]:
            out: list[list[Item]] = [[]]
            for it in items:
                if it.kind == "heading" and out[-1]:
                    out.append([])
                out[-1].append(it)
            return out
        cl = chunks(la)
        cf = chunks(fr) if fr is not None else None
        if cf is not None and len(cf) != len(cl):
            return items_to_blocks(prefix, la, fr, self.stats, where, red_tokens=red)
        blocks: list[Block] = []
        for i, part in enumerate(cl, 1):
            blocks += items_to_blocks(f"{prefix}.{i}", part, cf[i - 1] if cf is not None else None, self.stats, f"{where}#{i}", red_tokens=red)
        return blocks

    @staticmethod
    def gradual_title(got, group: str = "N") -> tuple[str, str, str]:
        _, section, r = got
        text = "\n".join(r.lines)
        if group == "Q" and "Tractus" not in text and "Allel" not in text and len(re.findall(r"^\s*V\.", text, re.M)) >= 2:
            return "Trait", "Tractus", "trait"
        first = next((l.strip() for l in r.lines if l.strip() and not l.strip().startswith("!")), "")
        if section == "Tractus" and not any(l.strip().startswith(("!Tractus", "Tractus")) for l in r.lines[1:]) \
                and not any("Graduale" in l for l in r.lines):
            if not re.search(r"V\.", "\n".join(r.lines[:3])):
                return "Trait", "Tractus", "trait"
        if re.match(r"^(v\.\s*)?Al+[eé]l[uú][ij]a", first, re.I):
            return "Alléluia", "Allelúja", "alleluia"
        return "Graduel", "Graduale", "graduel"

    # Messe quotidienne des défunts : oraisons au choix (DO, C9 : [Oratio1], [Oratio2]…).
    C9_ALTERNATIVES = [("1", "Pour les évêques ou les prêtres défunts", "Pro defunctis Episcopis vel Sacerdotibus"),
                       ("2", "Pour les frères, les proches et les bienfaiteurs défunts", "Pro defunctis fratribus, propinquis et benefactoribus")]

    def alternatives(self, prefix: str, pid: str, mass: Mass, kind: str) -> list[Block]:
        if mass.winner != "Commune/C9":
            return []
        out: list[Block] = []
        for n, fr, la in self.C9_ALTERNATIVES:
            r = self.la.resolve(mass.winner, f"{kind}{n}", mass.dctx)
            if not r:
                continue
            aid = f"{prefix}.{pid}.a{n}"
            la_items, fr_items, source = self.section_items(mass, mass.winner, f"{kind}{n}", r)
            fr_items = [i for i in fr_items if i.kind not in ("rubric", "ref")] if fr_items else fr_items  # « !Pour les évêques… »
            rb = rubric(aid, fr, la)
            rb.source = source
            out.append(rb)
            out += items_to_blocks(aid, la_items, fr_items, self.stats, f"{mass.winner}:{kind}{n}")
        return out

    def commemorations(self, prefix: str, pid: str, mass: Mass, commemorations, kind: str) -> list[Block]:
        out: list[Block] = []
        for j, (title_fr, getter_mass, section) in enumerate(commemorations, 1):
            m = getter_mass
            got = None
            if section:  # commémoraison intrinsèque : [Commemoratio Oratio] du formulaire
                r = self.la.resolve(m.winner, f"Commemoratio {kind}", m.dctx)
                if r:
                    got = (m.winner, f"Commemoratio {kind}", r)
            else:
                got = m.oratio(kind)
            if not got:
                continue
            cid = f"{prefix}.{pid}.c{j}"
            la_items, fr_items, source = self.section_items(m, *got, name_kind=None if section else kind)
            rb = rubric(cid, f"Commémoraison : {title_fr}")
            rb.source = source
            out.append(rb)
            out += items_to_blocks(cid, la_items, fr_items, self.stats, f"{got[0]}:{got[1]}")
        return out

    def preface_text(self, mass: Mass) -> str:
        name, subst = mass.preface()
        text = PREFACES.get(name, f"Préface {name}")
        if subst:
            text += f" (« Et te in {subst} »)"
        return text + "."

    def preface_la(self, mass: Mass) -> str | None:
        name, subst = mass.preface()
        r = self.la.resolve("Ordo/Prefationes", name, mass.dctx)
        if not r:
            return None
        first = next((l for l in r.lines if l.strip().startswith("!")), None)
        return f"Præfatio {first.strip()[1:].strip()}." if first else None

    @staticmethod
    def communicantes(mass: Mass) -> tuple[str | None, str | None, str | None]:
        """Communicantes et Hanc igitur propres (clés des sous-sections de l'unité `prefaces`) et rubrique de renvoi.
        Noël : toutes les messes de l'octave, saints compris (« In Nativitate Domini et per Octavam ») ; Épiphanie et
        Ascension : le jour seulement (1962) ; Pâques et Pentecôte : l'octave, avec le Hanc igitur."""
        w = mass.winner or ""
        if (w.startswith("Sancti/12-25") or re.match(r"Tempora/Nat(1-0|2\d|3\d)$", w) or w == "Sancti/01-01"
                or re.match(r"Sancti/12-(2[6-9]|3[01])", w)):
            return "c-nat1962", None, "Communicantes propre de Noël (voir aux Préfaces)."
        if w == "Sancti/01-06":
            return "c-epi1962", None, "Communicantes propre de l’Épiphanie (voir aux Préfaces)."
        if re.match(r"Tempora/Pasc0-|Tempora/Quad6-6", w):
            return "c-pasc1962", "h-pent", "Communicantes et Hanc igitur propres de Pâques (voir aux Préfaces)."
        if w == "Tempora/Pasc5-4":
            return "c-asc1962", None, "Communicantes propre de l’Ascension (voir aux Préfaces)."
        if re.match(r"Tempora/Pasc6-6|Tempora/Pasc7-", w):
            return "c-pent1962", "h-pent", "Communicantes et Hanc igitur propres de la Pentecôte (voir aux Préfaces)."
        return None, None, None


# ── Unités ──────────────────────────────────────────────────────────────────────────────────

def unit_id(key: str) -> str:
    """`missalUnit()` de TodayScreen.kt : « Tempora/Adv1-0 » → « tempora-adv1-0 »."""
    return key.lower().replace("/", "-")


def colors_text(colors) -> str:
    colors = list(colors)
    if not colors:
        return "ornements blancs"
    if colors == ["ROSE", "VIOLET"]:
        return "ornements roses ou violets"
    if colors == ["VIOLET", "RED"]:
        return "ornements rouges pour la procession, violets pour la messe"
    if colors == ["BLACK", "VIOLET"]:
        return "ornements noirs, violets pour la communion"
    if colors == ["VIOLET", "WHITE"]:
        return "ornements violets, puis blancs pour la messe de la Vigile"
    if colors == ["RED", "VIOLET"]:
        return "ornements rouges"
    return "ornements " + " ou ".join(COLOR_FR[c] for c in colors)


def class_of_precedence(p: int) -> str:
    return "I" if p < 30 else "II" if p < 40 else "III" if p < 50 else "IV" if p < 60 else "COMMEMORATION"


def mmdd_title(mmdd: str) -> str:
    m, d = int(mmdd[:2]), int(mmdd[3:5])
    return f"{'1er' if d == 1 else d} {MONTHS[m - 1]}"


@dataclass
class Unit:
    id: str
    key: str
    toc: str
    blocks: list[Block]
    division: str
    order: tuple
    kind: str = "mass"


class Book:
    def __init__(self, www: Path):
        self.b = Builder(www)
        self.temporal = K.temporal()
        self.sanctoral = K.sanctoral()
        self.units: list[Unit] = []
        self.keys_covered: list[str] = []
        self.absent: list[str] = []
        self.notes: list[str] = []

    # Temporal.

    def temporal_unit(self, key: str) -> Unit | None:
        b = self.b
        tem = self.temporal.get(key)
        file_key = b.subs.get(key, key)
        if key.startswith("Sancti/") or key.startswith("Commune/"):
            file_key = key
        ctx = tempora_ctx(key, tem) if key.startswith("Tempora/") else None
        if ctx is None:
            d = tem.dates[0]
            season = fine_season(d)
            ctx = Ctx(season, GROUP.get(season, "N"), K.n(d), week_dayname(d), d.month, d.day)
        uid = unit_id(key)
        rank = self.temporal_rank(key, tem)
        station = STATIONS.get(key)
        title_fr = tem.fr if tem else key
        if key.startswith("Tempora/PentEpi"):
            title_fr += ", reprise après la Pentecôte"
        title_la = tem.la or b.la.officium(file_key, ctx.do())
        masses = [f"{file_key}m{i}" for i in (1, 2, 3) if b.la.exists(f"{file_key}m{i}")]
        blocks: list[Block] = []
        if masses:
            blocks.append(heading(f"{uid}.titre", title_fr, title_la, 1, x_source(b.la, [(file_key, "Rank")])))
            blocks.append(rubric(f"{uid}.rang", rank + (f". Station à {station}" if station else "") + "."))
            for i, mk in enumerate(masses, 1):
                mfr, mla = MULTI_TITLES.get(mk, (f"Messe {i}", f"Missa {i}"))
                mass = Mass(b, mk, ctx, tempora=key.startswith("Tempora/"))
                st = STATIONS.get(mk)
                blocks += b.mass_blocks(f"{uid}.m{i}", mass, mfr, mla, f"Station à {st}." if st else None, title_level=1,
                                        commemorations=self.intrinsic(mass))
        else:
            mass = Mass(b, file_key, ctx, tempora=key.startswith("Tempora/"))
            if tem is not None and tem.precedence >= 40:
                mass.cal_class = class_of_precedence(tem.precedence)
            intrinsic = self.intrinsic(mass)
            blocks = b.mass_blocks(uid, mass, title_fr, title_la, rank + (f". Station à {station}" if station else "") + ".",
                                   commemorations=intrinsic)
        return Unit(uid, key, title_fr, blocks, "", ())

    def temporal_rank(self, key: str, tem: K.Temporal | None) -> str:
        if key.startswith("Commune/C10"):
            return "IVᵉ classe — ornements blancs"
        if tem is None:
            return ""
        cls = CLASS_FR[class_of_precedence(tem.precedence)]
        variants = getattr(tem, "variants", set())
        text = f"{cls} — {colors_text(tem.colors)}"
        precs = {v[0] for v in variants} | {tem.precedence}
        if len(precs) > 1 and key.startswith("Tempora/Adv"):
            text = f"IIIᵉ classe, IIᵉ classe du 17 au 23 décembre — {colors_text(tem.colors)}"
        cols = {v[1] for v in variants} | {tem.colors}
        if len(cols) > 1 and key.startswith("Tempora/Epi1"):
            text = f"{cls} — ornements blancs jusqu’au 13 janvier, verts ensuite"
        return text

    def intrinsic(self, mass: Mass, titles: dict | None = None) -> list:
        """Commémoraisons écrites dans le formulaire ([Commemoratio Oratio]), filtrées comme `commemoratio()` pour 1960."""
        la = self.b.la
        if not mass.winner or mass.rank > 6.9 or mass.has_rule("no commemoratio") or mass.has_rule("nocomm1960"):
            return []
        r = la.resolve(mass.winner, "Commemoratio Oratio", mass.dctx)
        if not r:
            return []
        head = next((l.strip()[1:] for l in r.lines if l.strip().startswith("!")), "")
        if re.search(r"Octav|Dominica", head, re.I) and not re.search(r"Octav.*?Nativ", head, re.I):
            return []
        if re.search("Vigil", head, re.I) and (mass.has_rule("no Vigil1960") or (mass.winner.startswith("Sancti") and
                                                                                  not re.search(r"08-14|06-23|06-28|08-09", mass.winner))):
            return []
        title = (titles or {}).get("intrinsic") or commemoration_title(head)
        return [(title, mass, "intrinsic")]

    # Sanctoral.

    def sancti_unit(self, key: str) -> Unit | None:
        b = self.b
        entries = self.sanctoral.get(key, [])
        general = [e for e in entries if e.calendar == "general"]
        france = [e for e in entries if e.calendar == "fr"]
        main = (general or france)[0] if entries else None
        mmdd = key.split("/", 1)[1][:5] if not key.endswith("-DU") else "10-28"
        uid = unit_id(key)
        counts = seasons_of(mmdd)
        allowed = {s: c for s, c in counts.items() if s not in ("holyweek", "pasc0") and c >= 12}
        if not allowed:
            allowed = dict(counts)
        groups: Counter = Counter()
        for s, c in allowed.items():
            groups[GROUP.get(s, "N")] += c
        main_group = groups.most_common(1)[0][0]
        main_season = max((s for s in allowed if GROUP.get(s, "N") == main_group), key=lambda s: allowed[s])
        winner = key

        def make(season: str) -> Mass:
            ctx = fixed_ctx(mmdd, season)
            if key.endswith("-DU"):
                ctx.dow = 0
            return Mass(b, winner, ctx)

        mass = make(main_season)
        mass.cal_class = main.rank if main else None
        others = []
        for g in ("N", "Q", "P"):
            if g != main_group and groups.get(g):
                season = max((s for s in allowed if GROUP.get(s, "N") == g), key=lambda s: allowed[s])
                others.append((g, make(season)))
        # Titre, rang, couleurs.
        if main:
            title_fr = main.fr
            title_la = main.la or b.la.officium(key, mass.dctx)
            colors = main.colors
            rank = f"{CLASS_FR.get(main.rank, main.rank)} — {colors_text(colors)}"
        else:
            title_fr, title_la, rank = key, b.la.officium(key, mass.dctx), ""
        notes = []
        if general and france and france[0].id == general[0].id and france[0].rank != general[0].rank:
            rank += f" ; en France : {CLASS_FR.get(france[0].rank, france[0].rank)}"
        for f in france:
            if general and f.id != general[0].id:
                notes.append((f"En France, ce jour est la fête de {lower_first(f.fr)} ({CLASS_FR.get(f.rank, f.rank)}). Divinum Officium "
                               f"n’en donne pas la messe propre : on trouve ici la messe du calendrier général.", None))
        if key in b.la.overlay_keys:
            notes.append(("Propre de France, absent de Divinum Officium : transcrit par Paroissien (voir le README du missel).", None))
        station = STATIONS.get(key)
        if station:
            rank += f". Station à {station}"
        # Préface selon le temps, pour une fête sans préface propre.
        preface_text = self.preface_by_season(mass, mmdd, allowed)
        # Commémoraison écrite dans le formulaire de DO, sauf si le calendrier la donne déjà avec sa propre clé
        # (saint Marc et les Litanies majeures : Sancti/04-25c).
        listed = main.commemorations if main else []
        commemorations = self.intrinsic(mass) if winner and (not listed or any(not c.get("proper") for c in listed)) else []
        for c in (main.commemorations if main else []):
            if c.get("proper"):
                cm = Mass(b, c["proper"], mass.ctx)
                commemorations.append((c.get("title", {}).get("fr", c["id"]), cm, None))
            elif not commemorations:
                pass
        if main and commemorations and commemorations[0][2] == "intrinsic":
            intrinsic_titles = [c.get("title", {}).get("fr") for c in main.commemorations if not c.get("proper")]
            if intrinsic_titles and intrinsic_titles[0]:
                commemorations[0] = (intrinsic_titles[0], commemorations[0][1], "intrinsic")
        masses = [f"{key}m{i}" for i in (1, 2, 3) if winner and b.la.exists(f"{key}m{i}")]
        if masses:
            blocks = [heading(f"{uid}.titre", title_fr, title_la, 1, x_source(b.la, [(key, "Rank")])), rubric(f"{uid}.rang", rank + ".")]
            for i, mk in enumerate(masses, 1):
                mfr, mla = MULTI_TITLES.get(mk, (f"Messe {i}", f"Missa {i}"))
                st = STATIONS.get(mk)
                m_mass = Mass(b, mk, mass.ctx)
                blocks += b.mass_blocks(f"{uid}.m{i}", m_mass, mfr, mla, f"Station à {st}." if st else None,
                                        commemorations=self.intrinsic(m_mass))
        else:
            blocks = b.mass_blocks(uid, mass, title_fr, title_la, rank + ".", others=others, commemorations=commemorations,
                                   extra_rubrics=notes, preface_text=preface_text)
        toc = f"{mmdd_title(mmdd)} — {title_fr}" if not key.endswith("10-DU") else f"Dernier dimanche d’octobre — {title_fr}"
        return Unit(uid, key, toc, blocks, "", ())

    def preface_by_season(self, mass: Mass, mmdd: str, allowed: dict) -> str | None:
        m, d = int(mmdd[:2]), int(mmdd[3:5])
        names: dict[str, list[str]] = defaultdict(list)
        main_name = mass.preface(mass.ctx.season, m, d, 1)
        for season in sorted(allowed, key=lambda s: -allowed[s]):
            nm = mass.preface(season, m, d, 1)
            if nm != main_name:
                names[f"{PREFACES.get(nm[0], nm[0])}"].append(SEASON_LABEL.get(season, season))
        text = PREFACES.get(main_name[0], main_name[0])
        if main_name[1]:
            text += f" (« Et te in {main_name[1]} »)"
        if not names:
            return text + "."
        parts = [f"{', '.join(labels)} : {lower_first(name)}" for name, labels in names.items()]
        return text + " ; " + " ; ".join(parts) + "."

    # Commémoraisons : une unité par clé (collecte, secrète, postcommunion).

    def commemoration_unit(self, key: str, main: K.Saint | None, comm: dict) -> Unit:
        """Oraisons d'une commémoraison du calendrier (`commemorations[].proper`, ou `Sancti/<date>` pour une
        commémoraison écrite dans le formulaire du jour, comme saint Pierre le 25 janvier ou l'octave de Noël)."""
        b = self.b
        uid = unit_id(key)
        title = comm.get("title", {})
        title_fr = commemoration_heading(title.get("fr") or comm["id"])
        title_la = f"Commemoratio {title['la']}" if title.get("la") else None
        if key == "Tempora/Quad5-5Feriac":
            ctx = tempora_ctx("Tempora/Quad5-5", self.temporal.get("Tempora/Quad5-5"))
            rank = "Commémoraison. Ces oraisons suivent celles de la férie (vendredi de la semaine de la Passion)."
        else:
            mmdd = main.date
            counts = seasons_of(mmdd)
            allowed = {s_: c for s_, c in counts.items() if s_ not in ("holyweek", "pasc0") and c >= 12} or dict(counts)
            ctx = fixed_ctx(mmdd, max(allowed, key=lambda s_: allowed[s_]))
            rank = f"Commémoraison. Ces oraisons suivent celles de la messe du jour ({lower_first(main.fr)})."
            if STATIONS.get(key):
                rank += f" Station à {STATIONS[key]} pour la procession."
        intrinsic = not comm.get("proper") or key in OCTAVE_COMMEMORATIONS
        suffix = {"peter_apostle": "Petri", "paul_apostle": "Pauli"}.get(comm["id"])
        blocks = [heading(f"{uid}.titre", title_fr, title_la, 1, x_source(b.la, [(key, "Rank")]) if b.la.exists(key) else None),
                  rubric(f"{uid}.rang", rank)]
        for kind, pid, fr_t, la_t in (("Oratio", "collecte", "Collecte", "Oratio"), ("Secreta", "secrete", "Secrète", "Secreta"),
                                      ("Postcommunio", "postcommunion", "Postcommunion", "Postcommunio")):
            got, mass, name_kind = None, None, None
            candidates: list[tuple[str, str]] = []
            if key == "Tempora/Quad5-5Feriac":
                candidates = [("Tempora/Quad5-5Feria", f"Commemoratio {kind}")]
            elif intrinsic:
                if suffix:
                    candidates.append((key, f"{kind} {suffix}"))
                candidates += [(main.key, f"Commemoratio {kind}"), (f"Sancti/{main.date}", f"Commemoratio {kind}")]
            for src, section in candidates:
                mass = Mass(b, src, ctx)
                r = b.la.resolve(src, section, mass.dctx)
                if r:
                    got = (src, section, r)
                    break
            if not candidates:
                mass = Mass(b, key, ctx)
                got = mass.oratio(kind)
                name_kind = kind
            if not got:
                self.b.missing_pieces.append(f"{uid}:{kind}")
                continue
            la_items, fr_items, source = b.section_items(mass, *got, name_kind=name_kind)
            # L'intitulé de DO (« Pro S. Petro », « Commemoratio… ») double le titre de l'unité.
            for items in (la_items, fr_items):
                while items and items[0].kind == "rubric":
                    del items[0]
            blocks.append(heading(f"{uid}.{pid}", fr_t, la_t, 2, source))
            blocks += items_to_blocks(f"{uid}.{pid}", la_items, fr_items, b.stats, f"{got[0]}:{got[1]}")
        toc = title_fr if key.startswith("Tempora/") else f"{mmdd_title(main.date)} — {title_fr}"
        return Unit(uid, key, toc, blocks, "", ())

    # Communs.

    def commune_unit(self, key: str, title_fr: str) -> Unit:
        b = self.b
        uid = unit_id(key)
        seasons = COMMON_SEASON.get(key)
        if seasons:
            main_season = seasons[0]
            mass = Mass(b, key, fixed_ctx_for_season(main_season))
            others = [(GROUP[s], Mass(b, key, fixed_ctx_for_season(s))) for s in seasons[1:] if GROUP[s] != GROUP[main_season]]
        else:
            mass = Mass(b, key, fixed_ctx_for_season("perannum"))
            others = [("Q", Mass(b, key, fixed_ctx_for_season("quad")))]
        title_la = b.la.officium(key, mass.dctx) or ""
        if key.startswith("Commune/C10"):
            rank = "IVᵉ classe — ornements blancs"
        elif key == "Commune/C1v":
            rank = "Vigile — ornements violets"
        elif key == "Commune/C9":
            rank = "Ornements noirs"
        elif re.match(r"Commune/C[1-3]|Commune/C6$|Commune/C6-1|Commune/C7$", key):
            rank = "Ornements rouges"  # martyrs
        else:
            rank = "Ornements blancs"
        blocks = b.mass_blocks(uid, mass, title_fr, title_la or None, rank + ".", others=others,
                               preface_text=None if key.startswith("Commune/C10") or key in ("Commune/C9",) else self.common_preface(mass))
        return Unit(uid, key, title_fr, blocks, "", ())

    def common_preface(self, mass: Mass) -> str:
        name, subst = mass.preface("perannum", 0, 0, 1)
        text = PREFACES.get(name, name) + (f" (« Et te in {subst} »)" if subst else "")
        if name == "Communis":
            return text + ", ou préface du temps (Carême, Passion, Pâques, Ascension) quand il y en a une."
        return text + "."

    # Préfaces.

    def prefaces_unit(self) -> Unit:
        b = self.b
        uid = "prefaces"
        blocks = [heading(f"{uid}.titre", "Préfaces", "Præfationes", 1, x_source(b.la, [("Ordo/Prefationes", "Communis")]))]
        blocks.append(rubric(f"{uid}.1", "Le dialogue (Sursum corda) et le Sanctus sont à l’Ordinaire de la messe. Les préfaces suivantes, "
                                         "après la préface des défunts, sont propres à certains lieux, dont la France, ou à certaines messes."))
        ctx = fixed_ctx_for_season("perannum").do()
        for name in PREFACE_ORDER:
            r = b.la.resolve("Ordo/Prefationes", name, ctx)
            if not r:
                continue
            pid = f"{uid}.{name.lower()}"
            la_items = b.r_la.render([l.replace("*", "") for l in r.lines])
            fr_r = b.fr.resolve("Ordo/Prefationes", name, ctx)
            fr_items = b.r_fr.render([l.replace("*", "") for l in fr_r.lines]) if fr_r else None
            la_title = next((i.text for i in la_items if i.kind in ("rubric", "ref")), name)
            if name == "Chrismatis":
                la_title = "in Missa Chrismatis"
            la_items = [i for i in la_items if i.kind not in ("rubric", "ref")]
            if fr_items is not None:
                fr_items = [i for i in fr_items if i.kind not in ("rubric", "ref")]
            b.files_read.add(b.la.relpath("Ordo/Prefationes"))
            blocks.append(heading(pid, PREFACES[name], f"Præfatio {la_title}", 2, x_source(b.la, [("Ordo/Prefationes", name)])))
            if name in PREFACE_LOCAL:
                blocks.append(rubric(f"{pid}.lieux", "Préface propre à certains lieux."))
            if name in ("Maria", "Joseph"):
                word = "Festivitáte" if name == "Maria" else "Veneratióne"
                blocks.append(rubric(f"{pid}.var", f"« Et te in {word} » : le propre de la fête donne le mot qui convient "
                                                    f"(par exemple « Assumptióne » le 15 août)."))
            if name == "Pasch":
                blocks.append(rubric(f"{pid}.var", "« in hoc potíssimum » ; à la Vigile pascale : « in hac potíssimum nocte » ; "
                                                    "le jour de Pâques et dans l’octave : « in hac potíssimum die »."))
            blocks += items_to_blocks(pid, la_items, fr_items, b.stats, f"Ordo/Prefationes:{name}")
        blocks.append(heading(f"{uid}.communicantes", "Communicantes et Hanc igitur propres", "Communicantes et Hanc igitur", 2))
        for name, fr_t in (("C-Nat1962", "Noël et son octave"), ("C-Epi1962", "Épiphanie"), ("C-Pasc1962", "Pâques et son octave"),
                           ("C-Asc1962", "Ascension"), ("C-Pent1962", "Pentecôte et son octave"), ("H-Pent", "Hanc igitur de Pâques et de la Pentecôte")):
            r = b.la.resolve("Ordo/Prefationes", name, ctx)
            if not r:
                continue
            pid = f"{uid}.{name.lower()}"
            la_items = b.r_la.render(r.lines)
            fr_r = b.fr.resolve("Ordo/Prefationes", name, ctx)
            fr_items = b.r_fr.render(fr_r.lines) if fr_r else None
            blocks.append(heading(pid, fr_t, None, 3, x_source(b.la, [("Ordo/Prefationes", name)])))
            blocks += items_to_blocks(pid, la_items, fr_items, b.stats, f"Ordo/Prefationes:{name}")
        return Unit(uid, "", "Préfaces", blocks, "", ())


def fixed_ctx_for_season(season: str) -> Ctx:
    rep = {"adv": "12-10", "nat": "12-30", "epiph": "01-08", "epi": "01-25", "septua": "02-10", "quad": "03-10", "passio": "03-28",
           "pasch": "04-28", "asc": "05-15", "pent": "06-05", "perannum": "08-20"}[season]
    return fixed_ctx(rep, season)


def lower_first(s: str) -> str:
    return s[:1].lower() + s[1:] if s else s


COMMEMORATION_TITLES = {"Litanies majeures": "Commémoraison des Litanies majeures",
                        "Octave de Noël": "Commémoraison de l’octave de Noël",
                        "Sept Douleurs de la Bienheureuse Vierge Marie": "Commémoraison des Sept Douleurs de la bienheureuse Vierge Marie"}


def commemoration_heading(fr: str) -> str:
    """« Saint Félix de Nole, prêtre et martyr » → « Commémoraison de saint Félix de Nole, prêtre et martyr »."""
    if fr in COMMEMORATION_TITLES:
        return COMMEMORATION_TITLES[fr]
    if fr[:1].isdigit():
        return "Commémoraison du " + fr
    for article, prep in (("Les ", "des "), ("Le ", "du "), ("La ", "de la ")):
        if fr.startswith(article):
            return "Commémoraison " + prep + fr[len(article):]
    if fr[:1] in "AEIOUÉÈÊHaeiouéèêh":
        return "Commémoraison de l’" + lower_first(fr)
    return "Commémoraison de " + lower_first(fr)


INTRINSIC_FR = {"Septem Dolorum B. M. V.": "Sept Douleurs de la bienheureuse Vierge Marie", "Pro S. Anastasiæ": "Sainte Anastasie, martyre",
                "Pro rogationibus": "Rogations", "Pro Octava Nativitatis.": "Octave de Noël"}


def commemoration_title(head: str) -> str:
    head = re.sub(r"^Commemoratio\s*", "", head).strip()
    return INTRINSIC_FR.get(head, head) or "commémoraison"


# ── Ordre et divisions ──────────────────────────────────────────────────────────────────────

TEMPORAL_DIVISIONS = [
    ("avent", "Avent"), ("temps-de-noel", "Temps de Noël"), ("apres-epiphanie", "Temps après l’Épiphanie"),
    ("septuagesime", "Septuagésime"), ("careme", "Carême"), ("passion", "Temps de la Passion et Semaine sainte"),
    ("temps-pascal", "Temps pascal"), ("apres-pentecote", "Temps après la Pentecôte"),
]


def temporal_division(key: str) -> tuple[str, tuple]:
    stem = key.split("/", 1)[1]
    m = re.match(r"^([A-Za-z]+?)(\d+)-(\d)", stem)
    if stem in ("12-24", "12-25") or key == "Sancti/12-24" or key == "Sancti/12-25":
        return "temps-de-noel", (0, int(stem[3:5]) - 24, 0)
    if key.startswith("Sancti/01-"):
        dd = int(stem[3:5])
        order = {1: 30, 6: 50, 13: 80}.get(dd, 50 + dd)
        return "temps-de-noel", (1, order, 0)
    if stem.startswith("Nat"):
        num = stem[3:]
        if num == "1-0":
            return "temps-de-noel", (0, 2, 0)
        if num == "2-0":
            return "temps-de-noel", (1, 35, 0)
        v = int(num)
        return ("temps-de-noel", (0, 3 + v, 0)) if v > 20 else ("temps-de-noel", (1, 35 + v, 0))
    if stem.startswith("Adv"):
        return "avent", (int(m.group(2)), int(m.group(3)), 0)
    if stem.startswith("Epi1"):
        return "temps-de-noel", (1, 70, int(m.group(3)))
    if stem.startswith("Epi"):
        return "apres-epiphanie", (int(m.group(2)), int(m.group(3)), 0)
    if stem.startswith("Quadp"):
        w, dw = int(m.group(2)), int(m.group(3))
        if w == 3 and dw >= 3:
            return "careme", (0, dw, 0)
        return "septuagesime", (w, dw, 0)
    if stem.startswith("Quad"):
        w, dw = int(m.group(2)), int(m.group(3))
        return ("passion" if w >= 5 else "careme"), (w, dw, 0)
    if stem.startswith("Pasc"):
        return "temps-pascal", (int(m.group(2)), int(m.group(3)), 0)
    if stem.startswith("PentEpi"):
        return "apres-pentecote", (23, int(m.group(2)), int(m.group(3)))
    if stem.startswith("Pent"):
        w = int(m.group(2))
        return "apres-pentecote", (w if w < 24 else 24, 0, int(m.group(3)))
    if stem.startswith("093"):
        return "apres-pentecote", (17, 9, int(stem[-1]))
    return "apres-pentecote", (99, 0, 0)


def build_book(www: Path, verbose: bool = False) -> Book:
    book = Book(www)
    b = book.b
    temporal_keys = set(book.temporal) - {k for k in book.temporal if k.startswith("Commune/")}
    temporal_keys |= {"Sancti/01-13"}
    temporal_keys -= {"Sancti/10-DU"}
    covered: list[str] = []
    for key in sorted(temporal_keys):
        if key in OCTAVE_COMMEMORATIONS:
            continue
        if key == "Sancti/01-13":
            book.temporal[key] = K.Temporal(key, K.EXTRA[key][0], K.EXTRA[key][1], 30, ("WHITE",), "CHRISTMAS", [date(2026, 1, 13)])
        u = book.temporal_unit(key)
        u.division, u.order = temporal_division(key)
        book.units.append(u)
        covered.append(key)
    for key in sorted(book.sanctoral) + ["Sancti/10-DU"]:
        if key in temporal_keys or key == "Sancti/11-02" and False:
            continue
        if key == "Sancti/10-DU":
            book.sanctoral.setdefault(key, [K.Saint(key, "10-31", "christ_the_king", "I", ["WHITE"], "Notre-Seigneur Jésus-Christ Roi",
                                                    "D. N. Iesu Christi Regis", "general", [], True)])
        u = book.sancti_unit(key)
        mmdd = key.split("/", 1)[1][:5]
        if key == "Sancti/11-02":
            u.division, u.order = "defunts", (0,)
        else:
            u.division = f"m{mmdd[:2]}"
            u.order = (int(mmdd[3:5]) if not key.endswith("DU") else 32, 0, key)
        book.units.append(u)
        covered.append(key)
    # Commémoraisons du calendrier sans formulaire propre : oraisons seules.
    comm_keys: dict[str, tuple] = {}
    for key in sorted(book.sanctoral):
        for main in book.sanctoral[key]:
            for c in main.commemorations:
                ck = c.get("proper") or f"Sancti/{main.date}"
                if ck not in covered and ck not in comm_keys:
                    comm_keys[ck] = (main, c)
    for ck in sorted(OCTAVE_COMMEMORATIONS):
        day = book.temporal[ck]
        main = next(e for es in book.sanctoral.values() for e in es if e.date == f"12-{ck[-2:]}" and e.calendar == "general")
        comm_keys[ck] = (main, {"id": "commemoration_of_the_octave_of_christmas", "proper": ck,
                                "title": {"fr": day.fr, "la": b.la.officium(ck) or day.la}})
    comm_keys["Tempora/Quad5-5Feriac"] = (None, {"id": "seven_sorrows_friday", "proper": "Tempora/Quad5-5Feriac",
                                                 "title": {"fr": "Sept Douleurs de la Bienheureuse Vierge Marie", "la": "Septem Dolorum B. M. V."}})
    for ck, (main, c) in sorted(comm_keys.items()):
        u = book.commemoration_unit(ck, main, c)
        if ck in OCTAVE_COMMEMORATIONS:
            u.division, u.order = temporal_division(ck)
        elif ck.startswith("Tempora/"):
            u.division, u.order = "passion", (5, 5, 1)
        else:
            u.division, u.order = f"m{main.date[:2]}", (int(main.date[3:5]), 1, ck)
        u.kind = "commemoration"
        book.units.append(u)
        covered.append(ck)
    for i, (key, title) in enumerate(COMMONS):
        if not b.la.exists(key):
            book.absent.append(key)
            continue
        u = book.commune_unit(key, title)
        u.division, u.order = "communs", (i,)
        book.units.append(u)
        if key.startswith("Commune/C10"):
            covered.append(key)
    u = book.commune_unit("Commune/C9", "Messe quotidienne des défunts")
    u.division, u.order = "defunts", (1,)
    book.units.append(u)
    u = book.prefaces_unit()
    u.division, u.order = "prefaces", (0,)
    book.units.append(u)
    book.keys_covered = sorted(set(covered))
    return book


# ── Écriture ────────────────────────────────────────────────────────────────────────────────

def divisions(book: Book) -> list[dict]:
    by: dict[str, list[Unit]] = defaultdict(list)
    for u in book.units:
        by[u.division].append(u)

    def units_of(name: str) -> list[dict]:
        return [{"id": u.id, "kind": u.kind if u.id != "prefaces" else "section", "toc": typo(u.toc, "fr"), "unit": True}
                for u in sorted(by.get(name, []), key=lambda u: u.order)]

    out = [{"id": "propre-du-temps", "kind": "part", "toc": "Propre du temps",
            "children": [{"id": d, "kind": "section", "toc": t, "children": units_of(d)} for d, t in TEMPORAL_DIVISIONS if by.get(d)]}]
    out.append({"id": "propre-des-saints", "kind": "part", "toc": "Propre des saints",
                "children": [{"id": f"saints-{MONTHS[m - 1].replace('é', 'e').replace('û', 'u')}", "kind": "section",
                              "toc": MONTHS[m - 1].capitalize(), "children": units_of(f"m{m:02d}")} for m in range(1, 13) if by.get(f"m{m:02d}")]})
    out.append({"id": "communs-des-saints", "kind": "part", "toc": "Communs des saints", "children": units_of("communs")})
    out.append({"id": "messes-des-defunts", "kind": "part", "toc": "Messes des défunts", "children": units_of("defunts")})
    out += units_of("prefaces")
    return out


def publication_meta(public: bool, sha: dict[str, str], www: Path) -> dict:
    notice = (www.parent.parent / "LICENSE").read_text(encoding="utf-8").strip()
    meta = {
        "id": PUB_ID,
        "revision": 1,
        "kind": "missal",
        "title": {"fr": "Missel romain", "la": "Missale Romanum"},
        "subtitle": {"fr": "Propre de la messe, 1962" + (" — latin" if public else " — latin et français")},
        "languages": ["la", "fr"],
        "primaryLanguage": "la" if public else "fr",
        "aligned": not public,
        "contributors": [{"name": "Divinum Officium (2026)", "role": "editor"}]
                        + ([] if public else [{"name": "Abbaye Sainte-Madeleine du Barroux (traduction française, via Divinum Officium)", "role": "translator"}]),
        "issued": "1962",
        "source": {
            "name": "Divinum Officium, rubriques de 1960 (« Rubrics 1960 - 1960 »)",
            "url": f"{DO_REPO}/tree/{DO_COMMIT}/web/www/missa",
            "edition": f"Divinum Officium, commit {DO_COMMIT} : missa/Latin/{{Tempora,Sancti,Ordo}}, horas/Latin/Commune"
                       + ("" if public else " ; français : missa/Francais, horas/Francais/Commune")
                       + ". Rubriques, titres français et règles de 1960 : Paroissien (content/sources/missel/README.md).",
            "retrieved": "2026-10-12",
        },
        "license": {
            "spdx": "MIT" if public else "MIT AND LicenseRef-Barroux-UsagePrive",
            "attribution": "Texte latin : Divinum Officium (github.com/DivinumOfficium/divinum-officium), licence MIT. Titres français, "
                           "rubriques de rang, de Gloria, de Credo et de préface, stations : Paroissien."
                           + ("" if public else " Traduction française du Propre : abbaye du Barroux, reprise par Divinum Officium ; "
                                                "usage personnel seulement, ne pas diffuser."),
            "notice": notice,
        },
        "x-divinum-officium": sha,
    }
    return meta


def write_tree(book: Book, root: Path, public: bool, sha: dict[str, str], www: Path) -> dict:
    out = root / PUB_ID
    blocks_dir = out / "blocks"
    blocks_dir.mkdir(parents=True, exist_ok=True)
    for old in blocks_dir.glob("*.jsonl"):
        old.unlink()
    meta = publication_meta(public, sha, www)
    meta["divisions"] = divisions(book)
    (out / "publication.json").write_text(json.dumps(meta, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    count = Counter()
    for u in book.units:
        rows = [r for r in (bl.row(public) for bl in u.blocks) if r is not None]
        ids = [r["id"] for r in rows]
        dup = [i for i, c in Counter(ids).items() if c > 1]
        if dup:
            raise SystemExit(f"{u.id} : identifiants en double {dup[:5]}")
        count["blocks"] += len(rows)
        for r in rows:
            count[r["type"]] += 1
        (blocks_dir / f"{u.id}.jsonl").write_text("".join(json.dumps(r, ensure_ascii=False, separators=(",", ":")) + "\n" for r in rows),
                                                  encoding="utf-8")
    return dict(count)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--divinum", type=Path, default=CONTENT / ".cache" / "raw" / "missel" / "divinum-officium")
    default_private = CONTENT.parent.parent / "paroissien-content"
    parser.add_argument("--private", type=Path, default=default_private if default_private.exists() else None)
    parser.add_argument("-v", action="store_true")
    args = parser.parse_args(argv)
    www = args.divinum / "web" / "www"
    if not www.exists():
        print(f"{www} introuvable : lancer d'abord fetch.py")
        return 2
    book = build_book(www, args.v)
    b = book.b

    def manifest(path: Path, latin_only: bool) -> dict:
        rows = []
        for rel in sorted(f for f in b.files_read if f and (not latin_only or "/Francais/" not in f)):
            source = CONTENT.parent / rel if rel.startswith("content/") else args.divinum / rel
            rows.append(f"{hashlib.sha256(source.read_bytes()).hexdigest()}  {rel}\n")
        text = "".join(rows)
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8")
        return {"repository": DO_REPO, "commit": DO_COMMIT, "manifest": "content/sources/missel/sources.sha256" if latin_only
                else "sources/missel/sources.sha256", "manifestSha256": hashlib.sha256(text.encode()).hexdigest()[:16], "files": len(rows)}

    pub_counts = write_tree(book, CONTENT / "data", True, manifest(HERE / "sources.sha256", True), www)
    priv_counts = None
    if args.private:
        priv_counts = write_tree(book, args.private / "data", False, manifest(args.private / "sources" / "missel" / "sources.sha256", False), www)
    (HERE / "cles.txt").write_text("".join(k + "\n" for k in sorted(book.keys_covered)), encoding="utf-8")
    report = {
        "units": len(book.units),
        "keys": len(book.keys_covered),
        "public": pub_counts,
        "private": priv_counts,
        "sections": {"aligned": b.stats["aligned"], "partial": b.stats["partial"], "combined": b.stats["combined"], "no_fr": b.stats["no_fr"]},
        "partial": b.stats["partial_list"],
        "no_fr": sorted(set(b.stats["no_fr_list"])),
        "combined": sorted(set(b.stats["combined_list"])),
        "missing_pieces": b.missing_pieces,
        "no_name": sorted(set(b.no_name)),
        "macros": dict(b.report["macros"]),
        "missing_refs": sorted(b.la.missing_refs),
        "unevaluated": sorted(do.divinum.UNEVALUATED),
        "files_latin": len([f for f in b.files_read if f and "/Latin/" in f]),
        "files_french": len([f for f in b.files_read if f and "/Francais/" in f]),
        "absent": book.absent,
    }
    if args.private:  # sections sans français ou non alignées : rapport du dépôt privé
        (args.private / "sources" / "missel" / "rapport.json").write_text(json.dumps(report, ensure_ascii=False, indent=1) + "\n",
                                                                         encoding="utf-8")
    print(f"{report['units']} unités, {report['keys']} clés ; public : {pub_counts.get('blocks')} blocs"
          + (f" ; privé : {priv_counts.get('blocks')} blocs" if priv_counts else ""))
    print(f"sections : {report['sections']} ; pièces manquantes : {len(b.missing_pieces)} ; macros inconnues : {dict(b.report['macros'])}")
    if args.v:
        for m in b.missing_pieces:
            print("  manque", m)
    return 0


if __name__ == "__main__":
    sys.exit(main())
