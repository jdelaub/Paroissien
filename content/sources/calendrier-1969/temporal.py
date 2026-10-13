"""Règles du temporal de romcal pour la France, réécrites en Python et vérifiées contre romcal.

    python3 -I content/sources/calendrier-1969/temporal.py <dossier brut>

Ce script est la référence exécutable de `content/data/calendar/1969/TEMPORAL.md` : chaque
règle du tableau y figure une fois. Il lit `romcal-defs.json` et `romcal-france-days.jsonl`
(generate.mjs) et vérifie, pour chaque jour :

- que la célébration principale, quand elle vient du temporal, a l'identifiant calculé ici ;
- que les cycles (A/B/C, I/II, semaine du psautier) sont ceux calculés ici.

Configuration France : Épiphanie et Saint-Sacrement au dimanche, Ascension le jeudi.
"""

import json
import sys
from datetime import date, timedelta
from pathlib import Path

DOW = ["monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"]  # date.weekday()
D = timedelta


def easter(y: int) -> date:
    """Comput grégorien (Meeus, Jones et Butcher)."""
    a, b, c = y % 19, y // 100, y % 100
    d, e = b // 4, b % 4
    f = (b + 8) // 25
    g = (b - f + 1) // 3
    h = (19 * a + b - d - g + 15) % 30
    i, k = c // 4, c % 4
    l_ = (32 + 2 * e + 2 * i - h - k) % 7
    m = (a + 11 * h + 22 * l_) // 451
    month = (h + l_ - 7 * m + 114) // 31
    return date(y, month, (h + l_ - 7 * m + 114) % 31 + 1)


def sunday_on_or_before(d: date) -> date:
    return d - D((d.weekday() + 1) % 7)


def advent1(y: int) -> date:
    """1er dimanche de l'Avent : le dimanche du 27 novembre au 3 décembre."""
    return sunday_on_or_before(date(y, 12, 3))


def epiphany(y: int) -> date:
    """France : le dimanche du 2 au 8 janvier."""
    return sunday_on_or_before(date(y, 1, 8))


def baptism(y: int) -> date:
    """Dimanche après l'Épiphanie ; lundi si l'Épiphanie tombe le 7 ou le 8."""
    ep = epiphany(y)
    return ep + D(1) if ep.day >= 7 else ep + D(7)


def holy_family(y: int) -> date:
    """Dimanche du 26 au 31 décembre ; le 30 si Noël est un dimanche."""
    xmas = date(y, 12, 25)
    return date(y, 12, 30) if xmas.weekday() == 6 else xmas + D(7 - (xmas.weekday() + 1) % 7)


def ordinal_week_early(d: date, y: int) -> int:
    """Temps ordinaire avant le Carême : la semaine 1 commence au dimanche du Baptême
    (ou au dimanche de l'Épiphanie quand le Baptême est un lundi)."""
    return (d - sunday_on_or_before(baptism(y))).days // 7 + 1


def ordinal_week_late(d: date, y: int) -> int:
    """Temps ordinaire après la Pentecôte : compté à rebours depuis le Christ-Roi (34e dimanche)."""
    christ_king = advent1(y) - D(7)
    return 34 - (christ_king - sunday_on_or_before(d)).days // 7


def temporal(d: date) -> tuple[str, int, list[str]]:
    """(identifiant du jour au temporal, semaine du temps pour le psautier, autres célébrations du temporal
    que romcal place ce jour-là). La semaine vaut 0 du mercredi des Cendres au samedi suivant."""
    y = d.year
    e = easter(y)
    a1 = advent1(y)
    wd = DOW[d.weekday()]
    pent = e + D(49)

    # Temps de Noël, début janvier.
    if d <= baptism(y):
        ep, bap = epiphany(y), baptism(y)
        if d == date(y, 1, 1):
            i = "mary_mother_of_god"
        elif d < ep:
            i = f"christmas_time_january_{d.day}"
        elif d == ep:
            i = "epiphany_of_the_lord"
        elif d < bap:
            i = f"{wd}_after_epiphany"
        else:
            i = "baptism_of_the_lord"
        return i, christmas_week(d, date(y - 1, 12, 25)), []
    if d < e - D(46):
        w = ordinal_week_early(d, y)
        return ot(w, wd), w, []
    if d < e - D(42):
        return ("ash_wednesday" if d == e - D(46) else f"{wd}_after_ash_wednesday"), 0, []
    if d < e - D(7):
        w = (d - (e - D(42))).days // 7 + 1
        return f"lent_{w}_{wd}", w, []
    if d < e - D(3):
        return ("palm_sunday_of_the_passion_of_the_lord" if d == e - D(7) else f"holy_{wd}"), 6, []
    if d == e - D(3):
        return "holy_thursday", 6, ["thursday_of_the_lords_supper"]
    if d < e:
        return ("friday_of_the_passion_of_the_lord" if d == e - D(2) else "holy_saturday"), 1, []
    if d <= pent:
        w = (d - e).days // 7 + 1
        if d == e:
            i = "easter_sunday"
        elif d < e + D(7):
            i = f"easter_{wd}"
        elif d == e + D(7):
            i = "divine_mercy_sunday"
        elif d == e + D(39):
            i = "ascension_of_the_lord"
        elif d == pent:
            i = "pentecost_sunday"
        else:
            i = f"easter_time_{w}_{wd}"
        return i, w, []
    if d < a1:
        w = ordinal_week_late(d, y)
        mobile = {
            pent + D(1): "mary_mother_of_the_church",
            pent + D(7): "most_holy_trinity",
            pent + D(14): "most_holy_body_and_blood_of_christ",
            pent + D(19): "most_sacred_heart_of_jesus",
            pent + D(20): "immaculate_heart_of_mary",
        }
        return mobile.get(d, ot(w, wd)), w, []
    if d < date(y, 12, 25):
        w = (d - a1).days // 7 + 1
        if wd == "sunday":
            i = f"advent_{w}_sunday"
        elif d.day >= 17 and d.month == 12:
            i = f"advent_december_{d.day}"
        else:
            i = f"advent_{w}_{wd}"
        return i, w, []
    xmas = date(y, 12, 25)
    if d == xmas:
        i = "nativity_of_the_lord"
    elif d == holy_family(y):
        i = "holy_family_of_jesus_mary_and_joseph"
    else:
        i = f"christmas_octave_day_{d.day - 24}"
    return i, christmas_week(d, xmas), []


def ot(w: int, wd: str) -> str:
    if wd == "sunday" and w == 3:
        return "sunday_of_the_word_of_god"
    if wd == "sunday" and w == 34:
        return "our_lord_jesus_christ_king_of_the_universe"
    return f"ordinary_time_{w}_{wd}"


def christmas_week(d: date, xmas: date) -> int:
    """Semaine du temps de Noël au sens de romcal, encodée pour `psalter_week` : valeur négative
    quand Noël n'est pas un dimanche (le psautier continue alors celui de l'Avent, PGLH 133)."""
    n = (d - xmas).days + 1
    shift = (xmas.weekday() + 1) % 7  # 0 = dimanche
    w = -(-(n + shift) // 7)
    return -w if shift else w


def psalter_week(week: int) -> int:
    if week < 0:  # temps de Noël, Noël en semaine
        return (2 - week) % 4 + 1
    if week == 0:  # Cendres → samedi
        return 4
    return (week - 1) % 4 + 1


def cycles(d: date) -> tuple[str, str]:
    lit_year = d.year + 1 if d >= advent1(d.year) else d.year
    return "ABC"[(lit_year - 1) % 3], "I" if lit_year % 2 else "II"


def main(raw: Path) -> None:
    defs = {x["id"]: x for x in json.loads((raw / "romcal-defs.json").read_text("utf-8"))["france"]}
    temporal_ids = {i for i, x in defs.items() if x["properCycle"] == "PROPER_OF_TIME"}
    seen, errors = set(), []
    cyc = {"YEAR_A": "A", "YEAR_B": "B", "YEAR_C": "C", "YEAR_1": "I", "YEAR_2": "II"}
    with (raw / "romcal-france-days.jsonl").open(encoding="utf-8") as f:
        for line in f:
            day = json.loads(line)
            d = date.fromisoformat(day["date"])
            i, week, extra = temporal(d)
            first = day["items"][0]
            ids = [x["id"] for x in day["items"]]
            seen.update(x for x in ids if x in temporal_ids)
            if first["id"] in temporal_ids and first["id"] != i:
                errors.append(f"{d} : romcal {first['id']}, règle {i}")
            for x in extra:
                if x not in ids:
                    errors.append(f"{d} : {x} absent")
            c = first["cycles"]
            got = (cyc[c["sundayCycle"]], cyc[c["weekdayCycle"]], int(c["psalterWeek"][5:]))
            want = (*cycles(d), psalter_week(week))
            if got != want:
                errors.append(f"{d} {first['id']} : cycles romcal {got}, règle {want}")
    never = sorted(temporal_ids - seen)
    print(f"{len(seen)} identifiants du temporal rencontrés ; jamais en tête ou absents : {len(never)}")
    print("  ", ", ".join(never))
    if errors:
        print(f"{len(errors)} écarts :", *errors[:40], sep="\n  ")
        sys.exit(1)
    print("règles conformes à romcal sur toute la plage")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    main(Path(sys.argv[1]))
