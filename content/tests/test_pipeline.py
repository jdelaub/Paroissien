"""Chaîne de contenu : texte canonique, validation, paquet déterministe, registre (ADR 003 et 004)."""

import hashlib
import json
import shutil
import sqlite3
import tempfile
import unittest
from pathlib import Path

from paroissien_content import canon, inlines, package, registry

GOLDEN = Path(__file__).resolve().parents[1] / "golden"


class PlainTextTest(unittest.TestCase):
    def test_symbols_breaks_notes_and_numbers(self):
        value = [{"num": "3"}, "Ad ", {"m": ["i"], "c": ["Deum"]}, " ", {"sym": "R"}, {"br": 1}, "fin", {"note": "n1", "l": "1"}]
        self.assertEqual("Ad Deum ℟\nfin", inlines.plain_text(value))

    def test_forbidden_characters_are_reported(self):
        errors = []
        inlines.validate(["Dó­mi­nus"], "x", errors)
        self.assertTrue(any("U+00AD" in e for e in errors))
        errors = []
        inlines.validate(["é"], "x", errors)  # NFD
        self.assertTrue(any("NFC" in e for e in errors))


class GoldenPackageTest(unittest.TestCase):
    def build(self, out: Path) -> dict:
        return package.build(package.load_definition(GOLDEN / "packages" / "golden-test.json"), GOLDEN / "data", out)

    def test_two_builds_are_identical(self):
        with tempfile.TemporaryDirectory() as a, tempfile.TemporaryDirectory() as b:
            self.assertEqual(self.build(Path(a))["sha256"], self.build(Path(b))["sha256"])

    def test_package_is_readable_and_searchable(self):
        with tempfile.TemporaryDirectory() as tmp:
            self.build(Path(tmp))
            db = sqlite3.connect(Path(tmp) / "golden-test.sqlite")
            self.assertEqual(0x5052534E, db.execute("PRAGMA application_id").fetchone()[0])
            self.assertEqual(100, db.execute("PRAGMA user_version").fetchone()[0])
            info = dict(db.execute("SELECT key, value FROM pack_info"))
            self.assertEqual("domaine-public", info["rights"])
            # « coeur » trouve « Cœur » et « aeternus » trouve « Æternus » grâce au pliage commun.
            for query in ('"coeur"', '"aeternus"', '"iuuentutem"', '"saecula"'):
                hits = db.execute("SELECT count(*) FROM search_fts WHERE search_fts MATCH ?", (query,)).fetchone()[0]
                self.assertGreater(hits, 0, query)
            seq = [r[0] for r in db.execute("SELECT id FROM block ORDER BY seq")]
            self.assertEqual("chap-1.titre", seq[0])
            self.assertEqual("n1", seq[-1])
            first, last = db.execute("SELECT seq_first, seq_last FROM division WHERE id = 'partie-1'").fetchone()
            self.assertEqual((1, 14), (first, last))
            self.assertEqual("chap-2.item", db.execute("SELECT new_id FROM id_alias WHERE old_id = 'chap-2.ancien'").fetchone()[0])

    def test_expected_texts_match_the_package(self):
        expected = json.loads((GOLDEN / "dist" / "golden-test.expected.json").read_text(encoding="utf-8"))
        for block in expected["blocks"]:
            self.assertEqual(block["plain"], inlines.plain_text(block["inlines"]))
            self.assertEqual(block["rev"], hashlib.sha256(block["plain"].encode()).hexdigest()[:16])


class RegistryTest(unittest.TestCase):
    def test_a_published_id_cannot_vanish_without_alias(self):
        with tempfile.TemporaryDirectory() as tmp:
            data = Path(tmp) / "data"
            shutil.copytree(GOLDEN / "data", data)
            reg = Path(tmp) / "registry"
            pub = canon.load(data / "golden.test")
            registry.update(pub, reg)
            self.assertEqual([], registry.check(pub, reg))
            # Retirer un bloc sans alias fait échouer le contrôle.
            path = data / "golden.test" / "blocks" / "chap-1.jsonl"
            lines = [l for l in path.read_text(encoding="utf-8").splitlines() if '"chap-1.p2"' not in l]
            path.write_text("\n".join(lines) + "\n", encoding="utf-8")
            self.assertTrue(registry.check(canon.load(data / "golden.test"), reg))
            # Avec un alias (pierre tombale), il passe.
            (data / "golden.test" / "aliases.json").write_text('{"chap-2.ancien":"chap-2.item","chap-1.p2":null}', encoding="utf-8")
            self.assertEqual([], registry.check(canon.load(data / "golden.test"), reg))


if __name__ == "__main__":
    unittest.main()
