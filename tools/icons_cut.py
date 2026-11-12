"""The icons' and cans' boxes on a 3x3 sheet (from Songling's tools/icons_cut.py)."""
import numpy as np
from scipy import ndimage


def boxes(solid, n):
    """The n icons' boxes, row by row, left to right. Loose bits (notes, sparks) join the icon they float by."""
    h, w = solid.shape
    near = ndimage.binary_dilation(solid, iterations=max(6, w // 90))
    lab, k = ndimage.label(near)
    found = []
    for i, sl in enumerate(ndimage.find_objects(lab), start=1):
        m = (lab[sl] == i) & solid[sl]
        if m.sum() < solid.sum() * 0.004:
            continue
        ys, xs = np.nonzero(m)
        found.append((sl[1].start + xs.min(), sl[0].start + ys.min(), sl[1].start + xs.max() + 1, sl[0].start + ys.max() + 1, m.sum()))
    if len(found) != n:
        # Fall back to the grid: what is in each third.
        found = []
        for j in range(3):
            for i in range(3):
                if len(found) == n:
                    break
                y0, y1, x0, x1 = h * j // 3, h * (j + 1) // 3, w * i // 3, w * (i + 1) // 3
                ys, xs = np.nonzero(solid[y0:y1, x0:x1])
                found.append((x0 + xs.min(), y0 + ys.min(), x0 + xs.max() + 1, y0 + ys.max() + 1, len(xs)))
        print("  (cut by the grid)")
    rows = sorted(found, key=lambda b: (b[1] + b[3]) / 2)
    out, line = [], []
    for b in rows:
        if line and (b[1] + b[3]) / 2 - (line[0][1] + line[0][3]) / 2 > h / 6:
            out += sorted(line, key=lambda c: c[0])
            line = []
        line.append(b)
    out += sorted(line, key=lambda c: c[0])
    return [b[:4] for b in out]


# ---------------------------------------------------------------- the menu's glyphs
import os  # noqa: E402
from PIL import Image  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res", "drawable-nodpi")
MASK = 72
# art/icons/sheets/ui.png, in the order of art/icons/prompts/ui.txt.
UI = ["dock_booth", "dock_bill", "dock_saga", "dock_goals", "act_seance", "act_log", "act_pick", "act_can", "act_buffet"]
# art/icons/sheets/ui2.png: the saga's blocks, four to a row.
UI2 = ["sec_chart", "sec_star", "sec_palette", "sec_clock", "sec_hourglass", "sec_popcorn", "sec_medal", "sec_calendar", "sec_seats", "sec_globe", "sec_trophy", "sec_target"]


def masks(path, names):
    """Dark glyphs on white become white with the darkness as alpha, softly shrunk, so the app tints them."""
    g = np.array(Image.open(path).convert("L")).astype(float)
    ink = np.clip((235 - g) / 200, 0, 1)
    solid = ink > 0.5
    for name, (x0, y0, x1, y1) in zip(names, boxes(solid, len(names))):
        side = max(x1 - x0, y1 - y0)
        pad = side // 14
        cx, cy = (x0 + x1) // 2, (y0 + y1) // 2
        half = side // 2 + pad
        crop = np.zeros((2 * half, 2 * half))
        sy0, sx0 = max(0, cy - half), max(0, cx - half)
        part = ink[sy0:cy + half, sx0:cx + half]
        crop[sy0 - (cy - half):sy0 - (cy - half) + part.shape[0], sx0 - (cx - half):sx0 - (cx - half) + part.shape[1]] = part
        a = np.array(Image.fromarray((crop * 255).astype(np.uint8)).resize((MASK, MASK), Image.LANCZOS))
        img = np.zeros((MASK, MASK, 4), np.uint8)
        img[..., :3] = 255
        img[..., 3] = a
        Image.fromarray(img, "RGBA").save(os.path.join(RES, name + ".png"))


if __name__ == "__main__":
    os.makedirs(RES, exist_ok=True)
    masks(os.path.join(ROOT, "art", "icons", "sheets", "ui.png"), UI)
    masks(os.path.join(ROOT, "art", "icons", "sheets", "ui2.png"), UI2)
    print("icons:", ", ".join(UI + UI2))


# ---------------------------------------------------------------- the achievements' pictures
def feats():
    """art/icons/sheets/featsN.png, nine to a sheet in tools/feat_prompts.FEATS's order, into resources/feats/<name>.png, 48x48 in colour."""
    from cut import load, crisp
    from feat_prompts import sheets
    out = os.path.join(ROOT, "app", "src", "main", "resources", "feats")
    os.makedirs(out, exist_ok=True)
    size, pad = 48, 1
    for sheet, items in sheets().items():
        p = os.path.join(ROOT, "art", "icons", "sheets", sheet + ".png")
        if not os.path.exists(p):
            print("missing", sheet)
            continue
        im, solid = load(p)
        for (name, _), (x0, y0, x1, y1) in zip(items, boxes(solid, len(items))):
            part = im[y0:y1, x0:x1].copy()
            part[..., 3] = np.where(solid[y0:y1, x0:x1], part[..., 3], 0)
            room = size - 2 * pad
            k = room / max(x1 - x0, y1 - y0)
            w, h = max(1, round((x1 - x0) * k)), max(1, round((y1 - y0) * k))
            arr = crisp(part, w * 3, h * 3, colors=48)
            icon = np.zeros((size * 3, size * 3, 4), np.uint8)
            ox, oy = (size * 3 - w * 3) // 2, (size * 3 - h * 3) // 2
            icon[oy:oy + h * 3, ox:ox + w * 3] = arr
            Image.fromarray(icon, "RGBA").save(os.path.join(out, name + ".png"))
    print("feats cut")


if __name__ == "__main__" and "feats" in __import__("sys").argv:
    feats()
