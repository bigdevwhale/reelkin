"""Draws the launcher icon as pixel art and writes it as vector drawables (and docs/logo.png).

python tools/icon.py

A 24×24 grid: a film reel with a cat peeking over it, on plum velvet. Each run of same-coloured
pixels in a row becomes one rectangle in the vector path.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DRAW = ROOT / "app/src/main/res/drawable"

GRID = [
    "........................",
    "........................",
    "......k.........k.......",
    ".....kfk.......kfk......",
    ".....kffkkkkkkkffk......",
    ".....kfffffffffffk......",
    ".....kfwkffffkwffk......",
    ".....kfwkffffkwffk......",
    "......kffffpffffk.......",
    ".....kkkkkkkkkkkkkk.....",
    "....kGGGGGGGGGGGGGGk....",
    "...kGGLLLLGGGGLLLLGGk...",
    "...kGLLhhLLGGLLhhLLGk...",
    "...kGLhhhhLGGLhhhhLGk...",
    "...kGLLhhLLccLLhhLLGk...",
    "...kGGLLLLGccGLLLLGGk...",
    "...kGGGGGGGGGGGGGGGGk...",
    "...kGGLLLLGGGGLLLLGGk...",
    "...kGLLhhLLGGLLhhLLGk...",
    "...kGLhhhhLGGLhhhhLGk...",
    "...kGGLhhLLGGLLhhLGGk...",
    "....kGGLLLGGGGLLLGGk....",
    ".....kkkkkkkkkkkkkk.....",
    "........................",
]
GRID = [r.ljust(24, ".")[:24] for r in GRID]
COLORS = {"k": "#FF1A0E14", "f": "#FF5C6A8C", "w": "#FFFFF2C0", "p": "#FFE890A8",
          "G": "#FFE8B850", "L": "#FFB8862A", "h": "#FF3A1A26", "c": "#FFFFF0A0"}
BG = "#FF6E1026"


def runs():
    out = {}
    for y, row in enumerate(GRID):
        x = 0
        while x < len(row):
            ch = row[x]
            if ch == ".":
                x += 1
                continue
            start = x
            while x < len(row) and row[x] == ch:
                x += 1
            out.setdefault(ch, []).append((start, y, x - start))
    return out


def vector(scale, offset, mono=False):
    paths = []
    for ch, rs in runs().items():
        if mono and ch in "khLp":
            continue
        d = " ".join(f"M{offset + x * scale:.2f},{offset + y * scale:.2f}h{w * scale:.2f}v{scale:.2f}h{-w * scale:.2f}z" for x, y, w in rs)
        color = "#FFFFFFFF" if mono else COLORS[ch]
        paths.append(f'    <path android:fillColor="{color}" android:pathData="{d}" />')
    return paths


def main():
    DRAW.mkdir(parents=True, exist_ok=True)
    head = '<?xml version="1.0" encoding="utf-8"?>\n<vector xmlns:android="http://schemas.android.com/apk/res/android"\n    android:width="108dp"\n    android:height="108dp"\n    android:viewportWidth="108"\n    android:viewportHeight="108">\n'
    scale = 66 / 24
    (DRAW / "ic_launcher_foreground.xml").write_text(head + "\n".join(vector(scale, 21)) + "\n</vector>\n", encoding="utf-8", newline="\n")
    (DRAW / "ic_launcher_background.xml").write_text(head + f'    <path android:fillColor="{BG}" android:pathData="M0,0h108v108h-108z" />\n</vector>\n', encoding="utf-8", newline="\n")
    try:
        from PIL import Image

        def rgba(c):
            return tuple(int(c[i:i + 2], 16) for i in (3, 5, 7)) + (255,)
        img = Image.new("RGBA", (24, 24), rgba(BG))
        for y, row in enumerate(GRID):
            for x, ch in enumerate(row):
                if ch != ".":
                    img.putpixel((x, y), rgba(COLORS[ch]))
        (ROOT / "docs").mkdir(exist_ok=True)
        img.resize((512, 512), Image.NEAREST).save(ROOT / "docs/logo.png")
    except ImportError:
        pass


if __name__ == "__main__":
    main()
