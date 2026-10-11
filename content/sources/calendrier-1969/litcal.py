"""Confronte le sanctoral général de romcal à celui de LitCal (Apache-2.0).

    python3 -I content/sources/calendrier-1969/litcal.py <dépôt LitCal> <dossier brut>

Lit `<dossier brut>/romcal-defs.json` (sortie de generate.mjs) et, dans LitCal,
`jsondata/sourcedata/rite/roman/` : propres des saints des éditions typiques 1970,
2002 et 2008, et `decrees/decrees.json`. Écrit `<dossier brut>/litcal.json` :

- `validFrom` : année d'inscription au calendrier général, par identifiant romcal,
  d'après LitCal (`since_year` d'un décret, ou année de l'édition typique) ;
- `differences` : rang ou date différents entre romcal et LitCal (état actuel) ;
- `titles` : titre français de LitCal en regard de celui de romcal.

Associer une fête LitCal à un identifiant romcal se fait par la date, puis par la
ressemblance des titres anglais quand plusieurs fêtes partagent un jour. Le script
s'arrête si une fête reste sans correspondant.
"""

import difflib
import json
import sys
from pathlib import Path

# Date nominale des fêtes que romcal place par une fonction (voir convert.py).
NOMINAL = {
    "presentationOfTheLord": "02-02",
    "annunciation": "03-25",
    "nativityOfJohnTheBaptist": "06-24",
    "peterAndPaulApostles": "06-29",
    "transfiguration": "08-06",
    "assumption": "08-15",
    "exaltationOfTheHolyCross": "09-14",
    "allSaints": "11-01",
    "immaculateConceptionOfMary": "12-08",
}
MISSALS = {"1970": 1970, "2002": 2002, "2008": 2008}
# Déplacements codés en dur dans LitCal (src/Handlers/CalendarHandler.php), état actuel.
LITCAL_MOVES = {"StJaneFrancesDeChantal": ("08-12", 2002)}  # handleSaintJaneFrancesDeChantal : 12-12 → 08-12
# Degrés de LitCal : 2 mémoire facultative, 3 mémoire, 4 fête, 5 fête du Seigneur, 6 solennité, 7 solennité majeure.
ROMCAL_GRADE = {"OPTIONAL_MEMORIAL": 2, "MEMORIAL": 3, "FEAST": 4, "SOLEMNITY": 6}


def romcal_grade(d):
    if d["precedence"] == "GENERAL_LORD_FEAST_5":
        return 5
    return ROMCAL_GRADE[d["rank"]]


def mmdd(month, day):
    return f"{month:02d}-{day:02d}"


def main(litcal_repo: Path, raw: Path) -> None:
    base = litcal_repo / "jsondata/sourcedata/rite/roman"
    events = {}  # event_key → dict(date, grade, since, source, en, fr)
    for edition, year in MISSALS.items():
        folder = base / f"missals/propriumdesanctis_{edition}"
        names = {lang: json.loads((folder / f"i18n/{lang}.json").read_text("utf-8")) for lang in ("en", "fr")}
        for e in json.loads((folder / f"propriumdesanctis_{edition}.json").read_text("utf-8")):
            events[e["event_key"]] = {
                "date": mmdd(e["month"], e["day"]),
                "grade": e["grade"],
                "since": year,
                "source": f"Missale Romanum, editio typica {edition} (LitCal propriumdesanctis_{edition})",
                "en": names["en"][e["event_key"]],
                "fr": names["fr"][e["event_key"]],
            }
    decree_names = {lang: json.loads((base / f"decrees/i18n/{lang}.json").read_text("utf-8")) for lang in ("en", "fr")}
    for dec in json.loads((base / "decrees/decrees.json").read_text("utf-8")):
        ev, meta = dec["liturgical_event"], dec.get("metadata", {})
        key = ev["event_key"]
        if meta.get("action") == "createNew" and ev.get("type") == "fixed":
            events[key] = {
                "date": mmdd(ev["month"], ev["day"]),
                "grade": ev["grade"],
                "since": meta["since_year"],
                "source": f"décret du {dec['decree_date']} ({dec['decree_id']}, LitCal decrees.json)",
                "en": decree_names["en"].get(key, key),
                "fr": decree_names["fr"].get(key, key),
            }
        elif meta.get("action") == "setProperty" and meta.get("property") == "grade":
            events[key]["grade"] = ev["grade"]
            events[key]["gradeSince"] = meta["since_year"]
        elif meta.get("action") == "setProperty" and meta.get("property") == "name":
            events[key]["fr"] = decree_names["fr"].get(key, events[key]["fr"])
            events[key]["nameSince"] = meta["since_year"]

    for key, (date, year) in LITCAL_MOVES.items():
        events[key]["date"], events[key]["dateSince"] = date, year

    defs = json.loads((raw / "romcal-defs.json").read_text("utf-8"))
    romcal = {}
    for d in defs["general"]:
        if d["fromCalendarId"] != "general_roman":
            continue
        dd = d["dateDef"]
        date = mmdd(dd["month"], dd["date"]) if "month" in dd else NOMINAL.get(dd.get("dateFn"))
        if date:
            romcal[d["id"]] = dict(d, date=date)

    by_date_l, by_date_r = {}, {}
    for k, e in events.items():
        by_date_l.setdefault(e["date"], []).append(k)
    for i, d in romcal.items():
        by_date_r.setdefault(d["date"], []).append(i)

    match = {}  # event_key → romcal id
    for date, keys in by_date_l.items():
        ids = list(by_date_r.get(date, []))
        pairs = sorted(
            ((difflib.SequenceMatcher(None, events[k]["en"].lower(), romcal[i]["name"]["en"].lower()).ratio(), k, i)
             for k in keys for i in ids),
            reverse=True,
        )
        for _, k, i in pairs:
            if k not in match and i not in match.values():
                match[k] = i
    unmatched_l = sorted(set(events) - set(match))
    unmatched_r = sorted(set(romcal) - set(match.values()))
    if unmatched_l or unmatched_r:
        sys.exit(f"sans correspondant — LitCal : {unmatched_l} ; romcal : {unmatched_r}")

    valid_from, differences, titles = {}, [], []
    for k, i in sorted(match.items(), key=lambda kv: (events[kv[0]]["date"], kv[1])):
        e, d = events[k], romcal[i]
        if e["since"] > 1970:
            valid_from[i] = {"year": e["since"], "litcal": k, "source": e["source"]}
        rg = romcal_grade(d)
        lg = 6 if e["grade"] == 7 else e["grade"]
        if rg != lg or e["date"] != d["date"]:
            differences.append({"id": i, "litcal": k, "romcal": [d["date"], rg], "litcalNow": [e["date"], lg],
                                **({"gradeSince": e["gradeSince"]} if "gradeSince" in e else {})})
        titles.append({"id": i, "litcal": k, "date": d["date"], "romcal": d["name"]["fr"], "litcalFr": e["fr"]})

    out = {
        "litcal": {"commit": _git_head(litcal_repo)},
        "romcal": defs["source"],
        "matches": len(match),
        "validFrom": valid_from,
        "differences": differences,
        "titles": titles,
    }
    (raw / "litcal.json").write_text(json.dumps(out, ensure_ascii=False, indent=1) + "\n", "utf-8")
    print(f"{len(match)} fêtes associées ; validFrom pour {len(valid_from)} ; {len(differences)} différences de rang ou de date")
    for x in differences:
        print("  différence :", json.dumps(x, ensure_ascii=False))


def _git_head(repo: Path) -> str:
    head = (repo / ".git/HEAD").read_text().strip()
    if head.startswith("ref: "):
        ref = repo / ".git" / head[5:]
        if ref.exists():
            return ref.read_text().strip()
        for line in (repo / ".git/packed-refs").read_text().splitlines():
            if line.endswith(head[5:]):
                return line.split()[0]
    return head


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    main(Path(sys.argv[1]), Path(sys.argv[2]))
