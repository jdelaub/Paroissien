"""Images pieuses et données du calendrier embarquées dans l'APK public : droits, intégrité, format."""

import hashlib
import json
import re
import unittest
from pathlib import Path

DATA = Path(__file__).resolve().parent.parent / "data"
SLUG = re.compile(r"^[a-z0-9_]+$")


class PiousImagesTest(unittest.TestCase):
    """Rapport 09, §3.2 : seulement des œuvres CC0 ou du domaine public déclarées par l'institution."""

    def setUp(self):
        self.dir = DATA / "images"
        self.catalog = json.loads((self.dir / "catalog.json").read_text(encoding="utf-8"))

    def test_rights_and_integrity(self):
        cards = self.catalog["cards"]
        self.assertEqual(sorted(c["id"] for c in cards), [c["id"] for c in cards], "catalogue trié par id")
        for card in cards:
            with self.subTest(card=card["id"]):
                self.assertRegex(card["id"], SLUG)
                self.assertIn(card["licence"], {"CC0", "domaine-public"})
                self.assertTrue(card["licenceEvidence"])
                self.assertTrue(card["credit"])
                for kind, path in (("full", self.dir / f"{card['id']}.webp"), ("thumb", self.dir / "thumbs" / f"{card['id']}.webp")):
                    self.assertEqual(hashlib.sha256(path.read_bytes()).hexdigest(), card["sha256"][kind], path.name)

    def test_budget(self):
        total = sum(p.stat().st_size for p in self.dir.rglob("*.webp"))
        self.assertLessEqual(total, 7 * 1024 * 1024, "budget du lot MVP : 7 Mo au plus")


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
