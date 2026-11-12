"""
Draws the pictures from art/*/prompts with ChatGPT image generation through Codex, a few at a time.
Pictures already there are skipped; pass names to draw them again.

    python tools/art_gen.py [-j 4] [name ...]      # name: cat, eggs1, room, things, stage…
"""
import argparse
import concurrent.futures as cf
import glob
import os
import re
import shutil
import subprocess

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ART = os.path.join(ROOT, "art")
PET_REF = os.path.join(ART, "ref", "pet_style.png")
BOOTH_REF = os.path.join(ART, "ref", "booth_old.png")


def jobs():
    out = []
    for p in sorted(glob.glob(os.path.join(ART, "*", "prompts", "*.txt"))):
        group = os.path.basename(os.path.dirname(os.path.dirname(p)))
        name = os.path.splitext(os.path.basename(p))[0]
        target = os.path.join(ART, group, "sheets", name + ".png")
        refs = [PET_REF] + ([BOOTH_REF] if group in ("pets", "booth") else [])
        # The icon is the detective cat itself: its own sheet is the reference.
        if group == "icon":
            refs = [os.path.join(ART, "pets", "sheets", "cat.png")]
        out.append((name, p, target, refs))
    return out


def run(name, prompt, target, refs):
    codex = shutil.which("codex.cmd") or shutil.which("codex")
    with open(prompt, encoding="utf-8") as f:
        text = f.read()
    cmd = [codex, "exec", "--skip-git-repo-check", "-s", "workspace-write", "-C", ROOT]
    for r in refs:
        cmd += ["-i", r]
    cmd += ["-"]
    log = os.path.join(ART, "logs", name + ".log")
    os.makedirs(os.path.dirname(log), exist_ok=True)
    with open(log, "w", encoding="utf-8") as lf:
        r = subprocess.run(cmd, input=text, text=True, encoding="utf-8", stdout=lf, stderr=subprocess.STDOUT, timeout=1800)
    if not os.path.exists(target):
        # Codex's sandbox may not let it write into the repo: take the picture from where it was drawn.
        with open(log, encoding="utf-8", errors="replace") as lf:
            found = re.findall(r"[A-Za-z]:[^\s()\[\]]*?generated_images[^\s()\[\]]+?\.png", lf.read())
        if not found:
            # Or the newest picture of this Codex session.
            with open(log, encoding="utf-8", errors="replace") as lf:
                sid = re.search(r"session id: ([0-9a-f-]+)", lf.read())
            if sid:
                found = sorted(glob.glob(os.path.join(os.path.expanduser("~"), ".codex", "generated_images", sid.group(1), "*.png")), key=os.path.getmtime)
        if found:
            os.makedirs(os.path.dirname(target), exist_ok=True)
            shutil.copy(found[-1], target)
    return name, r.returncode, os.path.exists(target)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("-j", type=int, default=4)
    ap.add_argument("names", nargs="*")
    a = ap.parse_args()
    todo = [j for j in jobs() if j[0] in a.names] if a.names else [j for j in jobs() if not os.path.exists(j[2])]
    print("drawing:", " ".join(j[0] for j in todo), flush=True)
    with cf.ThreadPoolExecutor(a.j) as ex:
        for f in cf.as_completed([ex.submit(run, *j) for j in todo]):
            name, code, ok = f.result()
            print(f"{name}: {'ok' if ok else 'MISSING'} (exit {code})", flush=True)


if __name__ == "__main__":
    main()
