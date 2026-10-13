"""Clés de propre de Divinum Officium dont le calendrier de 1962 de l'app a besoin.

Bibliothèque standard seule. Trois sources, réunies par `all_keys()` :
- le temporal : port en Python de `Calendar1962.base()` (android/core/liturgy/…/Calendar1962.kt), parcouru sur
  1900-2299 pour obtenir toutes les clés possibles, avec leur classe et leurs couleurs ;
- le sanctoral : entrées de content/data/calendar/1962/general.json et fr.json (`proper`, sinon `Sancti/<date>`),
  comme `sanctoralCandidates()` ;
- les messes de la Sainte Vierge au samedi (`Commune/C10…`), le Baptême du Seigneur (`Sancti/01-13`) et le
  Christ-Roi (`Sancti/10-DU`), posés par `resolve()` et `afterPentecost()`.
`oracle_keys()` relit l'oracle Missalemeum 2020-2045 et ramène ses clés à celles de l'app (même normalisation que
GoldenTest.kt), pour vérifier que rien ne manque.
"""

from __future__ import annotations

import json
import re
from dataclasses import dataclass, field
from datetime import date, timedelta
from pathlib import Path

CONTENT = Path(__file__).resolve().parents[2]
CALENDAR = CONTENT / "data" / "calendar" / "1962"
ORACLE = CONTENT / "golden" / "calendar" / "1962-missalemeum.jsonl"

WEEKDAYS_FR = ["Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche"]


def easter(y: int) -> date:
    a, b, c = y % 19, y // 100, y % 100
    d, e = b // 4, b % 4
    f = (b + 8) // 25
    g = (b - f + 1) // 3
    h = (19 * a + b - d - g + 15) % 30
    i, k = c // 4, c % 4
    l = (32 + 2 * e + 2 * i - h - k) % 7
    m = (a + 11 * h + 22 * l) // 451
    month = (h + l - 7 * m + 114) // 31
    day = (h + l - 7 * m + 114) % 31 + 1
    return date(y, month, day)


def is_sunday(d: date) -> bool:
    return d.weekday() == 6


def sunday_on_or_before(d: date) -> date:
    return d - timedelta(days=(d.weekday() + 1) % 7)


def next_weekday(d: date, wd: int) -> date:
    """Prochain jour `wd` (0 = lundi … 6 = dimanche) strictement après `d`."""
    delta = (wd - d.weekday()) % 7
    return d + timedelta(days=delta or 7)


def n(d: date) -> int:
    """Numéro de DO : dimanche 0, lundi 1 … samedi 6 (`DayOfWeek.n()`)."""
    return (d.weekday() + 1) % 7


def ordinal(k: int) -> str:
    return "1er" if k == 1 else f"{k}e"


def ordinal_fem(k: int) -> str:
    return "1re" if k == 1 else f"{k}e"


@dataclass
class Temporal:
    key: str
    fr: str
    la: str | None
    precedence: int
    colors: tuple[str, ...]
    season: str
    dates: list[date] = field(default_factory=list)


class Year:
    def __init__(self, y: int):
        self.y = y
        self.easter = easter(y)
        self.septuagesima = self.easter - timedelta(days=63)
        self.ash_wednesday = self.easter - timedelta(days=46)
        self.passion_sunday = self.easter - timedelta(days=14)
        self.palm_sunday = self.easter - timedelta(days=7)
        self.ascension = self.easter + timedelta(days=39)
        self.pentecost = self.easter + timedelta(days=49)
        christmas = date(y, 12, 25)
        prev_sunday = christmas - timedelta(days=(christmas.weekday() + 1) % 7 or 7)
        self.advent1 = prev_sunday - timedelta(weeks=3)
        self.last_pentecost_sunday = self.advent1 - timedelta(weeks=1)
        self.holy_family = next_weekday(date(y, 1, 6), 6)
        self.holy_name = next((date(y, 1, k) for k in range(2, 6) if is_sunday(date(y, 1, k))), date(y, 1, 2))
        self.christ_the_king = sunday_on_or_before(date(y, 10, 31))
        sep1 = date(y, 9, 1)
        first = sep1 if is_sunday(sep1) else next_weekday(sep1, 6)
        self.ember_september = first + timedelta(weeks=2, days=3)
        self.gaudete = self.advent1 + timedelta(weeks=2)


def _t(key, fr, la, precedence, colors, season) -> Temporal:
    return Temporal(key, fr, la, precedence, tuple(colors), season)


def base(t: Year, d: date) -> Temporal:
    """Port de `Calendar1962.base()` (clé, titre, préséance, couleurs, temps)."""
    wd = n(d)
    if d < t.holy_family and d.day <= 13 and d.month == 1:
        return christmas_tide(t, d)
    if d < t.septuagesima:
        return after_epiphany(t, d, (sunday_on_or_before(d) - t.holy_family).days // 7 + 1)
    if d < t.ash_wednesday:
        week = (d - t.septuagesima).days // 7 + 1
        names = ["Septuagésime", "Sexagésime", "Quinquagésime"]
        la = ["Septuagesima", "Sexagesima", "Quinquagesima"]
        if wd == 0:
            return _t(f"Tempora/Quadp{week}-0", f"Dimanche de la {names[week - 1]}", f"Dominica in {la[week - 1]}", 31, ["VIOLET"], "SEPTUAGESIMA")
        return _t(f"Tempora/Quadp{week}-{wd}", f"{WEEKDAYS_FR[d.weekday()]} après la {names[week - 1]}", None, 51, ["VIOLET"], "SEPTUAGESIMA")
    if d < t.passion_sunday:
        return lent(t, d)
    if d < t.easter:
        return passiontide(t, d)
    if d <= t.pentecost + timedelta(days=6):
        return paschaltide(t, d)
    if d < t.advent1:
        return after_pentecost(t, d)
    if d.month == 12 and d.day >= 25:
        return christmas_octave(d)
    return advent(t, d)


def christmas_tide(t: Year, d: date) -> Temporal:
    k = d.day
    if d == t.holy_name:
        return _t("Tempora/Nat2-0", "Très saint Nom de Jésus", "Ss.mi Nominis Iesu", 30, ["WHITE"], "CHRISTMAS")
    if k == 1:
        return _t("Sancti/01-01", "Octave de la Nativité", "In Octava Nativitatis Domini", 14, ["WHITE"], "CHRISTMAS")
    if k < 6:
        return _t(f"Tempora/Nat0{k}", f"{k} janvier, férie du temps de Noël", None, 51, ["WHITE"], "CHRISTMAS")
    if k == 6:
        return _t("Sancti/01-06", "Épiphanie de Notre-Seigneur", "In Epiphania Domini", 12, ["WHITE"], "CHRISTMAS")
    return _t(f"Sancti/01-{k:02d}", f"{k} janvier, férie du temps de l'Épiphanie", None, 51, ["WHITE"], "CHRISTMAS")


def after_epiphany(t: Year, d: date, week: int) -> Temporal:
    wd = n(d)
    season = "CHRISTMAS" if d.month == 1 and d.day <= 13 else "TIME_AFTER_EPIPHANY"
    if week == 1:
        if wd == 0:
            return _t("Tempora/Epi1-0", "Sainte Famille, Jésus, Marie, Joseph", "S. Familiae Iesu, Mariae, Ioseph", 30, ["WHITE"], season)
        color = "WHITE" if season == "CHRISTMAS" else "GREEN"
        return _t(f"Tempora/Epi1-{wd}", f"{WEEKDAYS_FR[d.weekday()]} après la Sainte Famille", None, 51, [color], season)
    return epiphany_week(d, week, season)


def epiphany_week(d: date, week: int, season: str) -> Temporal:
    wd = n(d)
    if wd == 0:
        return _t(f"Tempora/Epi{week}-0", f"{ordinal(week)} dimanche après l'Épiphanie", f"Dominica {roman(week)} post Epiphaniam", 31, ["GREEN"], season)
    return _t(f"Tempora/Epi{week}-{wd}", f"{WEEKDAYS_FR[d.weekday()]} de la {ordinal_fem(week)} semaine après l'Épiphanie", None, 51, ["GREEN"], season)


def lent(t: Year, d: date) -> Temporal:
    wd = n(d)
    if d < next_weekday(t.ash_wednesday, 6):
        if d == t.ash_wednesday:
            return _t("Tempora/Quadp3-3", "Mercredi des Cendres", "Feria IV Cinerum", 16, ["VIOLET"], "LENT")
        return _t(f"Tempora/Quadp3-{wd}", f"{WEEKDAYS_FR[d.weekday()]} après les Cendres", None, 40, ["VIOLET"], "LENT")
    week = (d - next_weekday(t.ash_wednesday, 6)).days // 7 + 1
    if wd == 0:
        colors = ["ROSE", "VIOLET"] if week == 4 else ["VIOLET"]
        return _t(f"Tempora/Quad{week}-0", f"{ordinal(week)} dimanche de Carême", f"Dominica {roman(week)} in Quadragesima", 15, colors, "LENT")
    if week == 1 and wd in (3, 5, 6):
        return _t(f"Tempora/Quad1-{wd}", f"{WEEKDAYS_FR[d.weekday()]} des Quatre-Temps de Carême", None, 34, ["VIOLET"], "LENT")
    return _t(f"Tempora/Quad{week}-{wd}", f"{WEEKDAYS_FR[d.weekday()]} de la {ordinal_fem(week)} semaine de Carême", None, 40, ["VIOLET"], "LENT")


def passiontide(t: Year, d: date) -> Temporal:
    wd = n(d)
    days = (d - t.passion_sunday).days
    if days < 7:
        if wd == 0:
            return _t("Tempora/Quad5-0", "1er dimanche de la Passion", "Dominica I Passionis", 15, ["VIOLET"], "PASSIONTIDE")
        return _t(f"Tempora/Quad5-{wd}", f"{WEEKDAYS_FR[d.weekday()]} de la semaine de la Passion", None, 40, ["VIOLET"], "PASSIONTIDE")
    if wd == 0:
        return _t("Tempora/Quad6-0", "2e dimanche de la Passion ou des Rameaux", "Dominica II Passionis seu in Palmis", 15, ["VIOLET", "RED"], "PASSIONTIDE")
    if wd == 4:
        return _t("Tempora/Quad6-4", "Jeudi saint", "Feria V in Cena Domini", 11, ["WHITE"], "PASSIONTIDE")
    if wd == 5:
        return _t("Tempora/Quad6-5", "Vendredi saint", "Feria VI in Passione et Morte Domini", 11, ["BLACK", "VIOLET"], "PASSIONTIDE")
    if wd == 6:
        return _t("Tempora/Quad6-6", "Samedi saint", "Sabbato Sancto", 11, ["VIOLET", "WHITE"], "PASSIONTIDE")
    return _t(f"Tempora/Quad6-{wd}", f"{WEEKDAYS_FR[d.weekday()]} saint", None, 16, ["VIOLET"], "PASSIONTIDE")


def paschaltide(t: Year, d: date) -> Temporal:
    wd = n(d)
    days = (d - t.easter).days
    week = days // 7
    if days == 0:
        return _t("Tempora/Pasc0-0", "Dimanche de Pâques", "Dominica Resurrectionis", 10, ["WHITE"], "EASTER")
    if days < 7:
        return _t(f"Tempora/Pasc0-{wd}", f"{WEEKDAYS_FR[d.weekday()]} de Pâques", None, 17, ["WHITE"], "EASTER")
    if days == 7:
        return _t("Tempora/Pasc1-0", "Dimanche in albis, octave de Pâques", "Dominica in Albis", 15, ["WHITE"], "EASTER")
    if d == t.ascension:
        return _t("Tempora/Pasc5-4", "Ascension de Notre-Seigneur", "In Ascensione Domini", 12, ["WHITE"], "EASTER")
    if d == t.ascension - timedelta(days=1):
        return _t("Tempora/Pasc5-3", "Vigile de l'Ascension", "In Vigilia Ascensionis", 30, ["WHITE"], "EASTER")
    if d == t.ascension - timedelta(days=3):
        return _t("Tempora/Pasc5-1", "Lundi des Rogations", "Feria II in Rogationibus", 51, ["VIOLET"], "EASTER")
    if d == t.pentecost - timedelta(days=1):
        return _t("Tempora/Pasc6-6", "Vigile de la Pentecôte", "Sabbato in Vigilia Pentecostes", 18, ["RED"], "EASTER")
    if d == t.pentecost:
        return _t("Tempora/Pasc7-0", "Dimanche de la Pentecôte", "Dominica Pentecostes", 10, ["RED"], "EASTER")
    if d > t.pentecost:
        ember = wd in (3, 5, 6)
        return _t(f"Tempora/Pasc7-{wd}", f"{WEEKDAYS_FR[d.weekday()]} de la Pentecôte" + (", Quatre-Temps" if ember else ""), None, 17, ["RED"], "EASTER")
    if wd == 0:
        fr = "Dimanche après l'Ascension" if week == 6 else f"{ordinal(week)} dimanche après Pâques"
        la = "Dominica post Ascensionem" if week == 6 else f"Dominica {roman(week)} post Pascha"
        return _t(f"Tempora/Pasc{week}-0", fr, la, 31, ["WHITE"], "EASTER")
    return _t(f"Tempora/Pasc{week}-{wd}", f"{WEEKDAYS_FR[d.weekday()]} de la {ordinal_fem(week)} semaine après Pâques", None, 51, ["WHITE"], "EASTER")


def after_pentecost(t: Year, d: date) -> Temporal:
    wd = n(d)
    k = (sunday_on_or_before(d) - t.pentecost).days // 7
    last = (t.last_pentecost_sunday - t.pentecost).days // 7
    if d == t.christ_the_king:
        return _t("Sancti/10-DU", "Notre-Seigneur Jésus-Christ Roi", "D. N. Iesu Christi Regis", 12, ["WHITE"], "TIME_AFTER_PENTECOST")
    if 24 <= k < last:
        resumed = epiphany_week(d, k - last + 7, "TIME_AFTER_PENTECOST")
        resumed.key = resumed.key.replace("Tempora/Epi", "Tempora/PentEpi")
        return resumed
    week = 24 if k == last else k
    kk = f"{week:02d}"
    if k == 1 and wd == 0:
        return _t("Tempora/Pent01-0", "Très Sainte Trinité", "Sanctissimae Trinitatis", 12, ["WHITE"], "TIME_AFTER_PENTECOST")
    if k == 1 and wd == 4:
        return _t("Tempora/Pent01-4", "Fête-Dieu, le Très Saint-Sacrement", "Ss.mi Corporis Christi", 12, ["WHITE"], "TIME_AFTER_PENTECOST")
    if k == 2 and wd == 5:
        return _t("Tempora/Pent02-5", "Sacré-Cœur de Jésus", "Ss.mi Cordis D. N. Iesu Christi", 12, ["WHITE"], "TIME_AFTER_PENTECOST")
    if t.ember_september <= d <= t.ember_september + timedelta(days=3) and wd != 4:
        return _t(f"Tempora/093-{wd}", f"{WEEKDAYS_FR[d.weekday()]} des Quatre-Temps de septembre", None, 34, ["VIOLET"], "TIME_AFTER_PENTECOST")
    if wd == 0:
        return _t(f"Tempora/Pent{kk}-0", f"{ordinal(week)} dimanche après la Pentecôte", f"Dominica {roman(week)} post Pentecosten", 31, ["GREEN"], "TIME_AFTER_PENTECOST")
    return _t(f"Tempora/Pent{kk}-{wd}", f"{WEEKDAYS_FR[d.weekday()]} de la {ordinal_fem(week)} semaine après la Pentecôte", None, 51, ["GREEN"], "TIME_AFTER_PENTECOST")


def advent(t: Year, d: date) -> Temporal:
    wd = n(d)
    week = (d - t.advent1).days // 7 + 1
    if d.month == 12 and d.day == 24:
        return _t("Sancti/12-24", "Vigile de la Nativité", "In Vigilia Nativitatis Domini", 14, ["VIOLET"], "ADVENT")
    if wd == 0:
        colors = ["ROSE", "VIOLET"] if d == t.gaudete else ["VIOLET"]
        return _t(f"Tempora/Adv{week}-0", f"{ordinal(week)} dimanche de l'Avent", f"Dominica {roman(week)} Adventus", 15, colors, "ADVENT")
    if week == 3 and wd in (3, 5, 6):
        return _t(f"Tempora/Adv3-{wd}", f"{WEEKDAYS_FR[d.weekday()]} des Quatre-Temps de l'Avent", None, 34, ["VIOLET"], "ADVENT")
    if d.month == 12 and d.day >= 17:
        return _t(f"Tempora/Adv{week}-{wd}", f"{WEEKDAYS_FR[d.weekday()]} de la {ordinal_fem(week)} semaine de l'Avent", None, 34, ["VIOLET"], "ADVENT")
    return _t(f"Tempora/Adv{week}-{wd}", f"{WEEKDAYS_FR[d.weekday()]} de la {ordinal_fem(week)} semaine de l'Avent", None, 43, ["VIOLET"], "ADVENT")


def christmas_octave(d: date) -> Temporal:
    if d.day == 25:
        return _t("Sancti/12-25", "Nativité de Notre-Seigneur", "In Nativitate Domini", 10, ["WHITE"], "CHRISTMAS")
    if is_sunday(d):
        return _t("Tempora/Nat1-0", "Dimanche dans l'octave de Noël", "Dominica infra octavam Nativitatis", 31, ["WHITE"], "CHRISTMAS")
    return _t(f"Tempora/Nat{d.day}", f"{ordinal(d.day - 24)} jour dans l'octave de Noël", None, 33, ["WHITE"], "CHRISTMAS")


def roman(k: int) -> str:
    out, rest = "", k
    for v, s in [(10, "X"), (9, "IX"), (5, "V"), (4, "IV"), (1, "I")]:
        while rest >= v:
            out += s
            rest -= v
    return out


def saturday_of_our_lady(t: Year, d: date, b: Temporal) -> str | None:
    """`resolve()` : un samedi de férie de IVᵉ classe prend la messe de la Sainte Vierge."""
    if n(d) != 6 or b.precedence != 51:
        return None
    if b.season == "ADVENT":
        return "Commune/C10a"
    if d.month == 12 or d < date(t.y, 2, 2):
        return "Commune/C10b"
    if d < t.palm_sunday + timedelta(days=3):
        return "Commune/C10c"
    if b.season == "EASTER":
        return "Commune/C10Pasc"
    return "Commune/C10"


def temporal(first: int = 1900, last: int = 2299) -> dict[str, Temporal]:
    """Toutes les clés du temporal (et des samedis de la Sainte Vierge), avec leurs dates sur [first, last]."""
    out: dict[str, Temporal] = {}
    for y in range(first, last + 1):
        t = Year(y)
        d = date(y, 1, 1)
        while d.year == y:
            b = base(t, d)
            cur = out.setdefault(b.key, Temporal(b.key, b.fr, b.la, b.precedence, b.colors, b.season))
            cur.dates.append(d)
            if (b.precedence, b.colors) != (cur.precedence, cur.colors):
                cur.__dict__.setdefault("variants", set()).add((b.precedence, b.colors, b.season))
            sat = saturday_of_our_lady(t, d, b)
            if sat:
                c = out.setdefault(sat, Temporal(sat, "Sainte Marie au samedi", "S. Maria in Sabbato", 50, ("WHITE",), b.season))
                c.dates.append(d)
            d += timedelta(days=1)
    return out


@dataclass
class Saint:
    key: str
    date: str
    id: str
    rank: str
    colors: list[str]
    fr: str
    la: str | None
    calendar: str  # general | fr
    commemorations: list[dict]
    lord: bool = False


def sanctoral() -> dict[str, list[Saint]]:
    """Clé → entrées du sanctoral (général puis France) qui la portent."""
    out: dict[str, list[Saint]] = {}
    for cal in ("general", "fr"):
        data = json.loads((CALENDAR / f"{cal}.json").read_text(encoding="utf-8"))
        for e in data["entries"]:
            if e.get("remove"):
                continue
            key = e.get("proper") or f"Sancti/{e['date']}"
            title = e.get("title", {})
            out.setdefault(key, []).append(Saint(
                key, e["date"], e["id"], e.get("rank", ""), e.get("colors", []), title.get("fr", e["id"]), title.get("la"),
                cal, e.get("commemorations", []), bool(e.get("lord")),
            ))
    return out


EXTRA = {
    # Kotlin : baptismOfTheLord() le 13 janvier, et le Christ-Roi dans afterPentecost().
    "Sancti/01-13": ("Commémoraison du Baptême de Notre-Seigneur", "In Commemoratione Baptismatis D. N. Iesu Christi", "II", ["WHITE"]),
}


def all_keys() -> set[str]:
    return set(temporal()) | set(sanctoral()) | set(EXTRA)


def commemoration_keys() -> set[str]:
    """Clés des commémoraisons : `commemorations[].proper`, sinon `Sancti/<date>` de l'entrée (`sanctoralCandidates()`),
    et les Sept Douleurs du vendredi de la Passion (`passiontide()`)."""
    out = {"Tempora/Quad5-5Feriac"}
    for entries in sanctoral().values():
        for e in entries:
            for c in e.commemorations:
                out.add(c.get("proper") or f"Sancti/{e.date}")
    return out


def normalize(key: str | None) -> str | None:
    """`GoldenTest.normalize` : on ne compare que la clé du jour, sans les variantes de fichier."""
    if key is None:
        return None
    key = key.replace("Tempora/PentEpi", "Tempora/Epi")
    return re.sub(r"^(Tempora|Sancti|Commune)/([A-Za-z]*\d+-\d+|\d{2}-\d{2}|\d{2}-DU|C10)[A-Za-z]*.*$", r"\1/\2", key)


def oracle_keys() -> dict[str, int]:
    out: dict[str, int] = {}
    for line in ORACLE.read_text(encoding="utf-8").splitlines():
        if line.strip():
            k = json.loads(line)["proper"]
            out[k] = out.get(k, 0) + 1
    return out


def missing_from_oracle(keys: set[str]) -> list[str]:
    """Clés de l'oracle dont la forme normalisée n'est couverte par aucune de nos clés."""
    ours = {normalize(k) for k in keys}
    return sorted(k for k in oracle_keys() if normalize(k) not in ours)


if __name__ == "__main__":
    keys = all_keys()
    tem = temporal()
    print(f"{len(keys)} clés : {len(tem)} du temporal, {len(sanctoral())} du sanctoral, {len(EXTRA)} autres")
    print(f"{len(commemoration_keys())} clés de commémoraison")
    print("absentes de nos clés (oracle) :", missing_from_oracle(keys | commemoration_keys()))
