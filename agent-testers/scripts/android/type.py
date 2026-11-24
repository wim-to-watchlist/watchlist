"""Replaces the focused field's content. adb cannot type accents: they are
stripped. Usage: python3 scripts/android/type.py "text"
Watchlist: `--secret email` or `--secret password` types the SIMKL test account's
e-mail or password from the SIMKL_TEST_EMAIL / SIMKL_TEST_PASSWORD secrets, so the
agent never sees them and they never appear in its output."""
import os, re, subprocess, sys, unicodedata

SECRETS = {"email": "SIMKL_TEST_EMAIL", "password": "SIMKL_TEST_PASSWORD"}
if sys.argv[1] == "--secret":
    name = SECRETS.get(sys.argv[2] if len(sys.argv) > 2 else "")
    if not name or not os.environ.get(name):
        sys.exit(f"--secret needs one of {sorted(SECRETS)}, and its secret must be set")
    raw = os.environ[name]
else:
    raw = sys.argv[1]
t = unicodedata.normalize("NFKD", raw).encode("ascii", "ignore").decode()
subprocess.run(["adb", "shell", "input", "keyevent", "KEYCODE_MOVE_END"])
subprocess.run(["adb", "shell", "input", "keyevent", *(["67"] * 40)])  # 40 backspaces
t = re.sub(r"([\\'\"`$&|;<>()*~#])", r"\\\1", t).replace(" ", "%s")
subprocess.run(["adb", "shell", "input", "text", t])
print("typed: check it on the next screenshot")
