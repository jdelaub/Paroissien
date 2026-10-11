"""Vecteurs de pliage partagés avec Kotlin (schema/fold-vectors.json)."""

import doctest
import json
import unittest
from pathlib import Path

from paroissien_content import fold as fold_module
from paroissien_content.fold import SPEC, fold

VECTORS_PATH = Path(__file__).resolve().parents[1] / "schema" / "fold-vectors.json"


class FoldVectorsTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        with VECTORS_PATH.open(encoding="utf-8") as f:
            cls.doc = json.load(f)
        cls.vectors = cls.doc["vectors"]

    def test_format(self):
        self.assertEqual(set(self.doc), {"spec", "steps", "vectors"})
        self.assertEqual(self.doc["spec"], SPEC)
        self.assertGreaterEqual(len(self.vectors), 40)
        for v in self.vectors:
            self.assertEqual(set(v), {"in", "out", "note"}, v)
        inputs = [v["in"] for v in self.vectors]
        self.assertEqual(len(inputs), len(set(inputs)), "entrée en double")

    def test_vectors(self):
        for v in self.vectors:
            with self.subTest(src=v["in"], note=v["note"]):
                self.assertEqual(fold(v["in"]), v["out"])

    def test_idempotent(self):
        for v in self.vectors:
            with self.subTest(src=v["in"]):
                once = fold(v["in"])
                self.assertEqual(fold(once), once)

    def test_no_soft_hyphen_in_output(self):
        for v in self.vectors:
            with self.subTest(src=v["in"]):
                self.assertNotIn("\u00ad", fold(v["in"]))
                self.assertNotIn("\u00ad", v["out"])

    def test_vectors_cover_removed_characters(self):
        joined = "".join(v["in"] for v in self.vectors)
        for ch in "\u00ad\u200b\u2060\u00a0\u202f":
            self.assertIn(ch, joined, f"U+{ord(ch):04X} sans vecteur")


def load_tests(loader, tests, ignore):
    tests.addTests(doctest.DocTestSuite(fold_module))
    return tests


if __name__ == "__main__":
    unittest.main()
