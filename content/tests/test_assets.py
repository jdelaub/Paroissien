"""Données du calendrier embarquées dans l'APK public : format et ordre canonique."""

import json
import re
import unittest
from pathlib import Path

DATA = Path(__file__).resolve().parent.parent / "data"
SLUG = re.compile(r"^[a-z0-9_]+$")


class CalendarDataTest(unittest.TestCase):
    """`content/schema/calendar-v1.md` : dates valides, slugs, rangs du rite, ordre canonique."""

    RANKS = {
        "1969": {"SOLEMNITY", "FEAST", "MEMORIAL", "OPTIONAL_MEMORIAL"},
        "1962": {"I", "II", "III", "IV", "COMMEMORATION", "VIGIL_I", "VIGIL_II", "VIGIL_III"},
    }
    COLORS = {"WHITE", "RED", "GREEN", "VIOLET", "ROSE", "BLACK", "GOLD"}

    def test_files(self):
        files = sorted((DATA / "calendar").glob("*/*.json"))
        self.assertTrue(files)
        for path in files:
            with self.subTest(file=str(path.relative_to(DATA))):
                data = json.loads(path.read_text(encoding="utf-8"))
                self.assertEqual(data["format"], 1)
                rite = data["rite"]
                keys = [(e["date"], e["id"]) for e in data["entries"]]
                self.assertEqual(sorted(keys), keys, "entrées triées par date puis id")
                for e in data["entries"]:
                    self.assertRegex(e["id"], SLUG)
                    month, day = map(int, e["date"].split("-"))
                    self.assertTrue(1 <= month <= 12 and 1 <= day <= 31, e["date"])
                    if e.get("remove"):
                        continue
                    if "rank" in e:
                        self.assertIn(e["rank"], self.RANKS[rite], e["id"])
                    self.assertTrue(set(e.get("colors", [])) <= self.COLORS, e["id"])


if __name__ == "__main__":
    unittest.main()
