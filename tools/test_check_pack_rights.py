"""Tests de check-pack-rights.py : python3 -I tools/test_check_pack_rights.py"""

import contextlib
import importlib.util
import io
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

SCRIPT = Path(__file__).resolve().parent / "check-pack-rights.py"
_spec = importlib.util.spec_from_file_location("check_pack_rights", SCRIPT)
cpr = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(cpr)


class CheckPackRightsTest(unittest.TestCase):
    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory()
        self.root = Path(self._tmp.name)
        self.packs = self.root / "packs"
        self.packs.mkdir()

    def tearDown(self):
        self._tmp.cleanup()

    def add_pack(self, pack_id, rights=None, suffix=".sqlite", directory=None, manifest=True):
        directory = directory or self.packs
        directory.mkdir(parents=True, exist_ok=True)
        (directory / f"{pack_id}{suffix}").write_bytes(b"SQLite format 3\x00")
        if manifest:
            data = {"id": pack_id} if rights is None else {"id": pack_id, "rights": rights}
            (directory / f"{pack_id}.manifest.json").write_text(json.dumps(data), encoding="utf-8")

    def run_main(self, *dirs):
        out, err = io.StringIO(), io.StringIO()
        with contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
            code = cpr.main([str(d) for d in dirs])
        return code, out.getvalue(), err.getvalue()

    # --- cas conformes

    def test_domaine_public_ok(self):
        self.add_pack("imitation-la", "domaine-public")
        code, out, _ = self.run_main(self.packs)
        self.assertEqual(code, 0)
        self.assertIn("1 paquet(s) conforme(s)", out)

    def test_licence_libre_ok_with_paq(self):
        self.add_pack("images-cc0", "licence-libre:CC0-1.0", suffix=".paq")
        self.assertEqual(self.run_main(self.packs)[0], 0)

    def test_empty_dir_and_non_pack_files_ok(self):
        (self.packs / "lisezmoi.txt").write_text("x", encoding="utf-8")
        (self.packs / "orphelin.manifest.json").write_text("{}", encoding="utf-8")
        code, out, _ = self.run_main(self.packs)
        self.assertEqual(code, 0)
        self.assertIn("0 paquet(s)", out)

    def test_missing_dir_warns_and_succeeds(self):
        missing = self.root / "domainePublic" / "assets" / "packs"
        code, _, err = self.run_main(missing)
        self.assertEqual(code, 0)
        self.assertIn("avertissement", err)
        self.assertIn(str(missing), err)

    # --- cas refusés

    def test_missing_manifest_fails(self):
        self.add_pack("prieres", manifest=False)
        code, _, err = self.run_main(self.packs)
        self.assertEqual(code, 1)
        self.assertIn("prieres.sqlite", err)
        self.assertIn("manifeste absent", err)

    def test_autorisation_ecrite_fails(self):
        self.add_pack("clairval-fr", "autorisation-écrite:clairval-2026")
        code, _, err = self.run_main(self.packs)
        self.assertEqual(code, 1)
        self.assertIn("autorisation-écrite:clairval-2026", err)

    def test_usage_prive_fails(self):
        self.add_pack("propre-1962-fr", "usage-privé-seulement", suffix=".paq")
        code, _, err = self.run_main(self.packs)
        self.assertEqual(code, 1)
        self.assertIn("usage-privé-seulement", err)

    def test_rights_absent_fails(self):
        self.add_pack("sans-droits", None)
        self.assertEqual(self.run_main(self.packs)[0], 1)

    def test_empty_licence_fails(self):
        self.add_pack("licence-vide", "licence-libre:")
        self.assertEqual(self.run_main(self.packs)[0], 1)

    def test_invalid_json_fails(self):
        self.add_pack("casse", manifest=False)
        (self.packs / "casse.manifest.json").write_text("{rights:", encoding="utf-8")
        code, _, err = self.run_main(self.packs)
        self.assertEqual(code, 1)
        self.assertIn("illisible", err)

    def test_nested_pack_is_checked(self):
        self.add_pack("cache", "usage-privé-seulement", directory=self.packs / "sous" / "dossier")
        self.assertEqual(self.run_main(self.packs)[0], 1)

    def test_several_dirs_one_bad_fails(self):
        self.add_pack("bible-crampon", "domaine-public")
        other = self.root / "personnel"
        self.add_pack("aelf", "usage-privé-seulement", directory=other)
        code, _, err = self.run_main(self.packs, other, self.root / "absent")
        self.assertEqual(code, 1)
        self.assertIn("aelf.sqlite", err)
        self.assertNotIn("bible-crampon", err)
        self.assertIn("2 paquet(s)", err)

    def test_file_instead_of_dir_fails(self):
        f = self.root / "fichier"
        f.write_text("x", encoding="utf-8")
        self.assertEqual(self.run_main(f)[0], 1)

    # --- ligne de commande

    def test_cli_exit_codes(self):
        self.add_pack("ok", "domaine-public")
        ok = subprocess.run([sys.executable, "-I", str(SCRIPT), str(self.packs)],
                            capture_output=True, text=True)
        self.assertEqual(ok.returncode, 0, ok.stderr)
        self.add_pack("prive", "usage-privé-seulement")
        bad = subprocess.run([sys.executable, "-I", str(SCRIPT), str(self.packs)],
                             capture_output=True, text=True)
        self.assertEqual(bad.returncode, 1)
        self.assertIn("prive.sqlite", bad.stderr)
        usage = subprocess.run([sys.executable, "-I", str(SCRIPT)], capture_output=True, text=True)
        self.assertEqual(usage.returncode, 2)


if __name__ == "__main__":
    unittest.main()
