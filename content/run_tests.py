#!/usr/bin/env python3
"""Lance les tests sans uv ni dépendance : python3 -I run_tests.py [-v]."""

import sys
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE / "pipeline"))

if __name__ == "__main__":
    verbosity = 2 if "-v" in sys.argv[1:] else 1
    suite = unittest.defaultTestLoader.discover(str(HERE / "tests"), top_level_dir=str(HERE / "tests"))
    result = unittest.TextTestRunner(verbosity=verbosity).run(suite)
    sys.exit(0 if result.wasSuccessful() else 1)
