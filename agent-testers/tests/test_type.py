"""scripts/android/type.py: typing text, and the SIMKL test account without showing it."""
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
FAKE_ADB = "#!/usr/bin/env bash\nprintf '%s\\n' \"$*\" >> \"$ADB_LOG\"\n"


class Type(unittest.TestCase):
    def run_type(self, *args, **env):
        with tempfile.TemporaryDirectory() as d:
            adb = Path(d, "adb")
            adb.write_text(FAKE_ADB)
            adb.chmod(0o755)
            log = Path(d, "adb.log")
            full_env = dict(os.environ, PATH=f"{d}:{os.environ['PATH']}", ADB_LOG=str(log), **env)
            for name in ("SIMKL_TEST_EMAIL", "SIMKL_TEST_PASSWORD"):
                if name not in env:
                    full_env.pop(name, None)
            out = subprocess.run([sys.executable, "scripts/android/type.py", *args], cwd=ROOT,
                                 env=full_env, capture_output=True, text=True)
            return out, (log.read_text() if log.exists() else "")

    def test_plain_text_is_escaped_for_adb(self):
        out, log = self.run_type("Breaking Bad & co")
        self.assertEqual(out.returncode, 0, out.stderr)
        self.assertIn("input text Breaking%sBad%s\\&%sco", log)

    def test_secret_is_typed_but_never_printed(self):
        out, log = self.run_type("--secret", "password", SIMKL_TEST_PASSWORD="hunter2 x")
        self.assertEqual(out.returncode, 0, out.stderr)
        self.assertIn("input text hunter2%sx", log)
        self.assertNotIn("hunter2", out.stdout + out.stderr)

    def test_unknown_or_missing_secret_stops_without_typing(self):
        for args in (("--secret", "token"), ("--secret", "email"), ("--secret",)):
            with self.subTest(args):
                out, log = self.run_type(*args)
                self.assertNotEqual(out.returncode, 0)
                self.assertEqual(log, "")


if __name__ == "__main__":
    unittest.main()
