"""The launcher icon from Codex's drawing (art/icon/sheets/icon.png, prompt in art/icon/prompts/icon.txt): the detective cat
peeking out of a film can, on the booth's plum.

    python tools/icon.py

Writes the adaptive icon's layers (foreground and monochrome as pictures, the background as a plain colour) and
docs/logo.png. The pet stays in the middle 60% of the layer, where every launcher's mask keeps it whole.
"""
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "art/icon/sheets/icon.png"
NODPI = ROOT / "app/src/main/res/drawable-nodpi"
DRAW = ROOT / "app/src/main/res/drawable"
BG = "#1F1424"
SIDE = 432          # 108 dp at xxxhdpi
SAFE = 0.64         # how much of the layer the drawing may fill


def subject():
    im = Image.open(SRC).convert("RGBA")
    a = np.array(im)[..., 3]
    ys, xs = np.nonzero(a > 16)
    return im.crop((xs.min(), ys.min(), xs.max() + 1, ys.max() + 1))


def layer(img, side, fill):
    k = side * fill / max(img.size)
    small = img.resize((max(1, round(img.width * k)), max(1, round(img.height * k))), Image.LANCZOS)
    out = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    out.alpha_composite(small, ((side - small.width) // 2, (side - small.height) // 2))
    return out


def main():
    NODPI.mkdir(parents=True, exist_ok=True)
    pic = subject()
    fg = layer(pic, SIDE, SAFE)
    fg.save(NODPI / "ic_launcher_foreground.png")
    # The themed icon: the drawing's shape in one colour, as Android tints it.
    a = np.array(fg)[..., 3]
    mono = np.zeros((SIDE, SIDE, 4), np.uint8)
    mono[..., :3] = 255
    mono[..., 3] = a
    Image.fromarray(mono, "RGBA").save(NODPI / "ic_launcher_monochrome.png")
    for old in ("ic_launcher_foreground.xml", "ic_launcher_monochrome.xml"):
        (DRAW / old).unlink(missing_ok=True)
    (DRAW / "ic_launcher_background.xml").write_text(
        '<?xml version="1.0" encoding="utf-8"?>\n<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">\n'
        f'    <path android:fillColor="{BG}" android:pathData="M0,0h108v108h-108z" />\n</vector>\n', encoding="utf-8", newline="\n")
    # The logo: the icon in a rounded square, as the stores show it.
    logo = Image.new("RGBA", (512, 512), BG)
    logo.alpha_composite(layer(pic, 512, 0.86))
    mask = Image.new("L", (512, 512), 0)
    from PIL import ImageDraw
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, 511, 511), 112, fill=255)
    logo.putalpha(mask)
    logo.save(ROOT / "docs/logo.png")
    print("icon written")


if __name__ == "__main__":
    main()
