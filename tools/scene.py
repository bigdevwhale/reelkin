"""
Cuts the drawn booth, its things, the hall's stage and the film cans (art/booth, art/hall, art/eggs; drawn by Codex
through tools/art_gen.py) into the app's pictures, FINE times finer than the art pixels the app draws in.

    python tools/scene.py

Writes app/src/main/resources/booth/*.png and booth.txt (where each thing stands and the spots the app draws into:
the reels, the port, the poster, the hall's screen), the cans as resources/pets/<species>_egg.png, and a look at
the booth to app/build/art/booth.png.
"""
import os
import sys

import numpy as np
from PIL import Image
from scipy import ndimage

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from cut import load, figures, crisp, outline, SPECIES, FINE  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ART = os.path.join(ROOT, "art")
OUT = os.path.join(ROOT, "app", "src", "main", "resources", "booth")
PETS = os.path.join(ROOT, "app", "src", "main", "resources", "pets")
LOOK = os.path.join(ROOT, "app", "build", "art", "booth.png")
W, H = 174, 150          # the booth, in art pixels (BoothScene.W, H)
HALL_W, HALL_H = 174, 58  # the stage at the top of the hall
EGG = 64                 # an egg sprite, like a pet's (PetArt.SIZE)

# Where each thing stands in the booth: box x, y, w, h in art pixels; it's fitted in, feet on the box's floor.
PLACES = {
    "projector": (2, 30, 64, 111),
    "popcorn": (130, 84, 26, 57),
    "chair": (93, 102, 42, 39),
    "rug": (84, 132, 62, 12),
    "plant": (157, 96, 17, 46),
    "clock": (125, 37, 18, 18),
    "cans": (66, 113, 30, 28),
}
WALLS = ["room", "room_green", "room_navy", "room_deco", "room_brick"]
POSTER = (92, 16, 24, 34)   # the picture inside the frame


def magenta(a):
    r, g, b = a[..., 0].astype(int), a[..., 1].astype(int), a[..., 2].astype(int)
    return (r > 170) & (b > 170) & (g < 110)


def fine(part, w, h, line=True):
    s = crisp(part, w * FINE, h * FINE)
    return outline(s) if line else s


def fit(part, box):
    x, y, w, h = box
    ph, pw = part.shape[:2]
    k = min(w / pw, h / ph)
    sw, sh = max(1, round(pw * k)), max(1, round(ph * k))
    return sw, sh, x + (w - sw) // 2, y + h - sh


def room(table, name="room"):
    p = os.path.join(ART, "booth", "sheets", name + ".png")
    if not os.path.exists(p):
        print("no", name, "yet")
        return None
    im = np.array(Image.open(p).convert("RGBA"))
    im[..., 3] = 255
    a = crisp(im, W * FINE, H * FINE, colors=96)
    Image.fromarray(a, "RGBA").save(os.path.join(OUT, name + ".png"))
    # The port: a small, nearly square, pitch-black patch in the right third.
    L = a[..., :3].astype(int).sum(axis=2)
    dark = L < 75
    dark[:, :int(W * FINE * 0.66)] = False
    lab, n = ndimage.label(dark)
    best = None
    for i, sl in enumerate(ndimage.find_objects(lab), start=1):
        h, w = sl[0].stop - sl[0].start, sl[1].stop - sl[1].start
        fill = (lab[sl] == i).mean()
        if not (5 * FINE <= w <= 16 * FINE and 5 * FINE <= h <= 18 * FINE) or fill < .75 or not (.6 < w / h < 1.6):
            continue
        if best is None or w * h > best[0]:
            best = (w * h, sl)
    if best:
        sl = best[1]
        table.append(("port" if name == "room" else "port_" + name[5:], sl[1].start // FINE, sl[0].start // FINE, (sl[1].stop - sl[1].start) // FINE, (sl[0].stop - sl[0].start) // FINE))
    else:
        print("no port found on", name)
    return a


def things(table, shown, sheet="things", names=("projector", "popcorn", "chair", "frame")):
    p = os.path.join(ART, "booth", "sheets", sheet + ".png")
    if not os.path.exists(p):
        return
    im, solid = load(p)
    for name, (s, e) in zip(names, figures(solid, len(names))):
        rows = np.nonzero(solid[:, s:e].any(axis=1))[0]
        part = im[rows[0]:rows[-1] + 1, s:e].copy()
        if name == "frame":
            hole = magenta(part)
            ys, xs = np.nonzero(hole)
            hx0, hx1, hy0, hy1 = xs.min(), xs.max() + 1, ys.min(), ys.max() + 1
            part[hole] = 0
            tx, ty, tw, th = POSTER
            k = tw / (hx1 - hx0)
            sw, sh = round(part.shape[1] * k), round(part.shape[0] * k)
            sx, sy = round(tx - hx0 * k), round(ty - hy0 * k)
            spr = fine(part, sw, sh)
            shown[name] = spr
            table.append((name, sx, sy, sw, sh))
            # The window the poster goes in, as the frame came out: a pixel in under the rim on each side.
            table.append(("poster", round(sx + hx0 * k), round(sy + hy0 * k), round((hx1 - hx0) * k), round((hy1 - hy0) * k)))
            continue
        if name == "rug":
            # Flat on the floor: stretched to its place.
            x, y, w, h = PLACES[name]
            sw, sh, sx, sy = w, h, x, y
        else:
            sw, sh, sx, sy = fit(part, PLACES[name])
            if name == "clock":
                sy = PLACES[name][1] + (PLACES[name][3] - sh) // 2
        spr = fine(part, sw, sh)
        shown[name] = spr
        table.append((name, sx, sy, sw, sh))
        if name == "projector":
            # The reels sit on the tips of the two arms: the topmost pixels of the left and right halves.
            m = spr[..., 3] > 0
            for half, r in ((slice(0, spr.shape[1] // 2), 14), (slice(spr.shape[1] // 2, spr.shape[1]), 11)):
                sub = m[:, half]
                ys = np.nonzero(sub.any(axis=1))[0]
                y = ys[0]
                xs = np.nonzero(sub[y])[0]
                x = xs.mean() + (half.start or 0)
                table.append(("reel", round(sx + x / FINE), round(sy + y / FINE), r))
            # The lens: the rightmost solid point in the body's height.
            cols = np.nonzero(m.any(axis=0))[0]
            xr = cols[-1]
            yr = np.nonzero(m[:, xr])[0].mean()
            table.append(("lens", round(sx + xr / FINE), round(sy + yr / FINE)))


def stage(table):
    p = os.path.join(ART, "hall", "sheets", "stage.png")
    im = np.array(Image.open(p).convert("RGBA"))
    im[..., 3] = 255
    hole = magenta(im)
    a = crisp(im, HALL_W * FINE, HALL_H * FINE, colors=96)
    small = np.array(Image.fromarray(hole).resize((HALL_W * FINE, HALL_H * FINE), Image.NEAREST))
    lab, n = ndimage.label(small)
    i = 1 + int(np.argmax(ndimage.sum(small, lab, range(1, n + 1))))
    ys, xs = np.nonzero(lab == i)
    x0, y0, x1, y1 = -(-xs.min() // FINE), -(-ys.min() // FINE), (xs.max() + 1) // FINE, (ys.max() + 1) // FINE
    a[small] = (10, 8, 16, 255)
    Image.fromarray(a, "RGBA").save(os.path.join(OUT, "stage.png"))
    table.append(("screen", x0, y0, x1 - x0, y1 - y0))


def eggs():
    from icons_cut import boxes
    from art_prompts import egg_sheets
    for n, names in enumerate(egg_sheets()):
        p = os.path.join(ART, "eggs", "sheets", f"eggs{n + 1}.png")
        if not os.path.exists(p):
            continue
        im = np.array(Image.open(p).convert("RGBA"))
        rgb = im[..., :3].astype(int)
        solid = ndimage.binary_fill_holes(rgb.min(axis=2) < 232)
        lab, k = ndimage.label(solid)
        sizes = ndimage.sum(solid, lab, range(1, k + 1))
        for i, s in enumerate(sizes, start=1):
            if s < 400:
                solid[lab == i] = False
        im[..., 3] = np.where(solid, 255, 0)
        for sp, (a, b, c, d) in zip(names, boxes(solid, len(names))):
            part = im[b:d, a:c].copy()
            # A can in a pet's frame: about two thirds as wide, standing on the same floor.
            mw, mh = 42 * FINE, 40 * FINE
            k = min(mw / (c - a), mh / (d - b))
            sw, sh = round((c - a) * k), round((d - b) * k)
            size = EGG * FINE
            sprite = np.zeros((size, size, 4), np.uint8)
            ox, oy = (size - sw) // 2, 63 * FINE + 1 - sh
            sprite[oy:oy + sh, ox:ox + sw] = crisp(part, sw, sh)
            outline(sprite)
            Image.fromarray(sprite, "RGBA").save(os.path.join(PETS, f"{sp}_egg.png"))


def main():
    os.makedirs(OUT, exist_ok=True)
    table, shown = [], {}
    bg = room(table)
    for w in WALLS[1:]:
        room(table, w)
    things(table, shown)
    things(table, shown, "things2", ("rug", "plant", "clock"))
    things(table, shown, "cans", ("cans",))
    stage(table)
    eggs()
    for name, spr in shown.items():
        Image.fromarray(spr, "RGBA").save(os.path.join(OUT, f"{name}.png"))
    with open(os.path.join(OUT, "booth.txt"), "w", encoding="utf-8") as f:
        f.write("# Made by tools/scene.py: name x y [w h] in art pixels (the booth's, or the hall's for the screen).\n")
        for t in table:
            f.write(" ".join(str(int(v)) if not isinstance(v, str) else v for v in t) + "\n")
    if bg is not None:
        look = Image.fromarray(bg, "RGBA")
        for t in table:
            if t[0] in shown:
                look.alpha_composite(Image.fromarray(shown[t[0]], "RGBA"), (t[1] * FINE, t[2] * FINE))
        os.makedirs(os.path.dirname(LOOK), exist_ok=True)
        look.resize((W * 4, H * 4), Image.NEAREST).save(LOOK)
    print("\n".join(" ".join(map(str, t)) for t in table))


if __name__ == "__main__":
    main()
