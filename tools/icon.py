"""Draws the launcher icon, a film projector with its beam, as vector drawables and docs/logo.png.

python tools/icon.py

Shapes are described once in the 108×108 adaptive-icon space; the same list writes the Android
vector paths and a supersampled PNG preview.
"""
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DRAW = ROOT / "app/src/main/res/drawable"

BG = "#1F1424"
GOLD = "#E8B850"
GOLD_D = "#B8862A"
BODY = "#D8435E"
BODY_D = "#A82A44"
BEAM = "#FFF2C8"


def circle(cx, cy, r):
    return f"M{cx - r:.2f},{cy:.2f}a{r:.2f},{r:.2f} 0 1,0 {2 * r:.2f},0a{r:.2f},{r:.2f} 0 1,0 {-2 * r:.2f},0z"


def rrect(x, y, w, h, r):
    return (f"M{x + r:.2f},{y:.2f}h{w - 2 * r:.2f}a{r},{r} 0 0,1 {r},{r}v{h - 2 * r:.2f}a{r},{r} 0 0,1 {-r},{r}"
            f"h{-(w - 2 * r):.2f}a{r},{r} 0 0,1 {-r},{-r}v{-(h - 2 * r):.2f}a{r},{r} 0 0,1 {r},{-r}z")


def reel(cx, cy, r):
    """A reel with five round windows and a hub hole, as one even-odd path."""
    d = circle(cx, cy, r)
    for k in range(5):
        a = k * 2 * math.pi / 5 - math.pi / 2
        d += circle(cx + math.cos(a) * r * .55, cy + math.sin(a) * r * .55, r * .19)
    return d + circle(cx, cy, r * .13)


# (path, colour, alpha, even-odd)
SHAPES = [
    ("M70,57 L104,38 L104,86 L70,67z", BEAM, .30, False),
    ("M70,59.5 L104,50 L104,74 L70,64.5z", BEAM, .45, False),
    (reel(39, 37, 13), GOLD, 1, True),
    (reel(62, 41, 10), GOLD, 1, True),
    ("M37.5,48 L41,52 L39,53 L35.5,49z M60,49 L62,53 L60,53.5 L58,50z", GOLD_D, 1, False),
    (rrect(26, 50, 40, 24, 6), BODY, 1, False),
    (rrect(26, 68, 40, 6, 3), BODY_D, 1, False),
    (rrect(32, 56, 14, 5, 2.5), GOLD, 1, False),
    (rrect(64, 55.5, 9, 13, 2), GOLD, 1, False),
    (circle(72.5, 62, 3.2), BEAM, 1, False),
    ("M36,74 h6 l-3,10 h-4z M56,74 h-6 l3,10 h4z", GOLD_D, 1, False),
]


def vector(mono=False):
    out = []
    for d, c, a, eo in SHAPES:
        if mono and c in (BODY_D, GOLD_D):
            continue
        attrs = f'android:fillColor="{"#FFFFFFFF" if mono else c}"'
        if a < 1 and not mono:
            attrs += f' android:fillAlpha="{a}"'
        if eo:
            attrs += ' android:fillType="evenOdd"'
        out.append(f'    <path {attrs} android:pathData="{d}" />')
    return out


def main():
    DRAW.mkdir(parents=True, exist_ok=True)
    head = ('<?xml version="1.0" encoding="utf-8"?>\n<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    android:width="108dp"\n    android:height="108dp"\n    android:viewportWidth="108"\n    android:viewportHeight="108">\n')
    (DRAW / "ic_launcher_foreground.xml").write_text(head + "\n".join(vector()) + "\n</vector>\n", encoding="utf-8", newline="\n")
    (DRAW / "ic_launcher_monochrome.xml").write_text(head + "\n".join(vector(True)) + "\n</vector>\n", encoding="utf-8", newline="\n")
    (DRAW / "ic_launcher_background.xml").write_text(head + f'    <path android:fillColor="{BG}" android:pathData="M0,0h108v108h-108z" />\n</vector>\n', encoding="utf-8", newline="\n")
    preview()


def preview():
    """docs/logo.png, drawn by PIL from the same shapes (crude path parser for M/L/h/v/a/z used above)."""
    try:
        from PIL import Image, ImageDraw
    except ImportError:
        return
    S = 16
    size = 108 * S

    def rgba(c, a=1):
        return tuple(int(c[i:i + 2], 16) for i in (1, 3, 5)) + (int(255 * a),)

    img = Image.new("RGBA", (size, size), rgba(BG))
    for d, c, a, eo in SHAPES:
        layer = Image.new("L", (size, size), 0)
        dr = ImageDraw.Draw(layer)
        for sub in d.split("z"):
            pts = trace(sub)
            if len(pts) > 2:
                dr.polygon([(x * S, y * S) for x, y in pts], fill=255 if not eo else None)
        if eo:
            layer = evenodd(d, size, S)
        colour = Image.new("RGBA", (size, size), rgba(c, a))
        img = Image.alpha_composite(img, Image.composite(colour, Image.new("RGBA", (size, size), (0, 0, 0, 0)), layer))
    (ROOT / "docs").mkdir(exist_ok=True)
    img.resize((512, 512), Image.LANCZOS).save(ROOT / "docs/logo.png")


def evenodd(d, size, S):
    from PIL import Image, ImageChops, ImageDraw
    acc = Image.new("L", (size, size), 0)
    for sub in d.split("z"):
        pts = trace(sub)
        if len(pts) < 3:
            continue
        one = Image.new("L", (size, size), 0)
        ImageDraw.Draw(one).polygon([(x * S, y * S) for x, y in pts], fill=255)
        acc = ImageChops.logical_xor(acc.convert("1"), one.convert("1")).convert("L")
    return acc


def trace(sub):
    """Points along one subpath; arcs are sampled (only the arc forms written above)."""
    import re
    toks = re.findall(r"[MLlhvaz]|-?\d+(?:\.\d+)?", sub)
    pts = []
    x = y = 0.0
    i = 0
    cmd = None
    while i < len(toks):
        t = toks[i]
        if t in "MLlhva":
            cmd = t
            i += 1
            continue
        if cmd in ("M", "L"):
            x, y = float(toks[i]), float(toks[i + 1]); i += 2; pts.append((x, y))
        elif cmd == "l":
            x += float(t); y += float(toks[i + 1]); i += 2; pts.append((x, y))
        elif cmd == "h":
            x += float(t); i += 1; pts.append((x, y))
        elif cmd == "v":
            y += float(t); i += 1; pts.append((x, y))
        elif cmd == "a":
            r = float(toks[i]); sweep = int(toks[i + 4])
            dx, dy = float(toks[i + 5]), float(toks[i + 6]); i += 7
            ex, ey = x + dx, y + dy
            mx, my = (x + ex) / 2, (y + ey) / 2
            ch = math.hypot(dx, dy) / 2
            h = math.sqrt(max(0.0, r * r - ch * ch))
            nx, ny = -dy / (2 * ch), dx / (2 * ch)
            for sgn in (1, -1):
                cx, cy = mx + sgn * nx * h, my + sgn * ny * h
                a0 = math.atan2(y - cy, x - cx); a1 = math.atan2(ey - cy, ex - cx)
                da = a1 - a0
                if sweep == 1 and da < 0: da += 2 * math.pi
                if sweep == 0 and da > 0: da -= 2 * math.pi
                if abs(da) <= math.pi + 1e-6:
                    break
            for k in range(1, 25):
                a = a0 + da * k / 24
                pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
            x, y = ex, ey
        else:
            i += 1
    return pts


if __name__ == "__main__":
    main()
