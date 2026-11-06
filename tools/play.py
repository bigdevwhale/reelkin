"""Builds the Google Play graphics in docs/google-play/graphics from the README screenshots.

python tools/play.py

Takes docs/screenshots/<lang>/<n>-<name>.jpg (1080x2341 renders of the demo cinema) and writes the
512 px icon, the 1024x500 feature graphics and eight 1080x1920 captioned screenshots per language.
Play wants screenshots no longer than twice their width, so each screen runs off the bottom edge under
its caption. The icon is docs/logo.png (python tools/icon.py), already a full square.
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "docs/google-play/graphics"
SHOTS = ROOT / "docs/screenshots"
FONTS = ROOT / "app/src/main/res/font"

BG = (0x17, 0x10, 0x19)
VELVET_DEEP = (0x3F, 0x1C, 0x29)
LINE = (0x3E, 0x2F, 0x42)
INK = (0xF3, 0xE9, 0xDA)
MUTED = (0xB7, 0xA6, 0xAC)
GOLD = (0xE8, 0xB8, 0x50)
VELVET = (0xD8, 0x43, 0x5E)
SCREEN = (0xFF, 0xF2, 0xC8)

# The order they appear in the store, with the caption over each: a line in Prata and one in gold.
PAGES = [
    ("1-booth", ("A pet lives in the booth,", "raised by your films"), ("В будке живёт питомец.", "Его растят твои фильмы")),
    ("2-seance", ("Lights down,", "the reels are turning"), ("Гасим свет,", "бобины крутятся")),
    ("5-reward", ("Every film is a stub", "and a spectator in the hall"), ("Каждый фильм — корешок", "и зритель в зале")),
    ("4-hall", ("A hundred velvet seats", "fill up by genre"), ("Сто бархатных кресел", "заполняются по жанрам")),
    ("3-bill", ("Films and series,", "what's waiting and what's on"), ("Фильмы и сериалы:", "что ждёт и что идёт")),
    ("6-pick", ("No idea what to watch?", "The pet picks for tonight"), ("Не знаешь, что смотреть?", "Питомец выберет на вечер")),
    ("8-stub", ("Popcorn, three words,", "the day you watched it"), ("Попкорн, три слова", "и день, когда смотрел")),
    ("7-pet", ("Every minute you watch", "helps it grow"), ("Каждая минута", "растит питомца")),
]


def prata(size):
    return ImageFont.truetype(str(FONTS / "prata.ttf"), size)


def manrope(size):
    return ImageFont.truetype(str(FONTS / "manrope_bold.ttf"), size)


def fit(make, text, size, width):
    while make(size).getlength(text) > width:
        size -= 2
    return make(size)


def rounded(im, r):
    mask = Image.new("L", im.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, im.width - 1, im.height - 1), r, fill=255)
    out = im.convert("RGBA")
    out.putalpha(mask)
    return out


def lay(g, card, x, y, r=36, shadow=(0, 0, 0, 170), outline=LINE):
    """A screen on the board: rounded corners, a soft shadow and a thin edge."""
    sh = Image.new("RGBA", (card.width + 120, card.height + 120), (0, 0, 0, 0))
    ImageDraw.Draw(sh).rounded_rectangle((60, 76, 60 + card.width, 76 + card.height), r, fill=shadow)
    g.alpha_composite(sh.filter(ImageFilter.GaussianBlur(24)), (x - 60, y - 60))
    g.alpha_composite(rounded(card, r), (x, y))
    ImageDraw.Draw(g).rounded_rectangle((x - 1, y - 1, x + card.width, y + card.height), r, outline=outline, width=3)


def board(W, H, cx, cy, rad):
    """The booth's dark plum, with velvet light where the beam falls."""
    g = Image.new("RGB", (W, H), BG)
    glow = Image.new("L", (W, H), 0)
    ImageDraw.Draw(glow).ellipse((cx - rad, cy - rad, cx + rad, cy + rad), fill=255)
    glow = glow.filter(ImageFilter.GaussianBlur(rad // 2))
    g.paste(Image.new("RGB", (W, H), VELVET_DEEP), (0, 0), glow)
    return g.convert("RGBA")


def bulbs(d, W, y, step=26, r=4):
    """A row of marquee bulbs, every third one dim."""
    x, i = step / 2, 0
    while x < W:
        d.ellipse((x - r, y - r, x + r, y + r), fill=SCREEN if i % 3 else (0x6A, 0x55, 0x30))
        x += step
        i += 1


def screenshot(src, caption):
    W, H = 1080, 1920
    g = board(W, H, W // 2, 160, 620)
    d = ImageDraw.Draw(g)
    bulbs(d, W, 24)
    d.text((W / 2, 150), caption[0], font=fit(prata, caption[0], 70, W - 120), fill=INK, anchor="mm")
    d.text((W / 2, 250), caption[1], font=fit(manrope, caption[1], 60, W - 120), fill=GOLD, anchor="mm")
    im = Image.open(src).convert("RGB")
    w = 860
    im = im.resize((w, round(im.height * w / im.width)), Image.LANCZOS)
    top = 340
    # Only the top corners show: the screen runs on past the bottom edge.
    lay(g, im.crop((0, 0, w, H - top + 60)), (W - w) // 2, top)
    return g.convert("RGB")


def feature(lang):
    W, H = 1024, 500
    g = board(W, H, 760, 250, 420)
    d = ImageDraw.Draw(g)
    bulbs(d, W, 14, step=22, r=3)
    bulbs(d, W, H - 14, step=22, r=3)
    logo = Image.open(ROOT / "docs/logo.png").convert("RGBA").resize((150, 150), Image.LANCZOS)
    g.alpha_composite(rounded(logo, 34), (64, 70))
    name = "Reelkin" if lang == "en" else "Бобина"
    lines = ("A film diary", "with a pet in the booth") if lang == "en" else ("Дневник фильмов", "с питомцем в будке")
    d.text((62, 238), name, font=prata(84), fill=GOLD)
    d.rectangle((66, 352, 330, 354), fill=VELVET)
    f = fit(manrope, max(lines, key=len), 38, 440)
    for i, l in enumerate(lines):
        d.text((66, 370 + i * 46), l, font=f, fill=INK if i == 0 else MUTED)
    # The booth with the pet, cut from the first screen.
    im = Image.open(SHOTS / lang / "1-booth.jpg").convert("RGB").crop((40, 170, 1040, 1040))
    im = im.resize((round(im.width * 400 / im.height), 400), Image.LANCZOS)
    lay(g, im, W - im.width - 50, 50, r=24, outline=GOLD)
    g.convert("RGB").save(OUT / f"feature-graphic-{lang}.png", optimize=True)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    Image.open(ROOT / "docs/logo.png").convert("RGB").resize((512, 512), Image.LANCZOS).save(OUT / "icon-512.png", optimize=True)
    for lang in ("en", "ru"):
        feature(lang)
        folder = OUT / f"screenshots-{lang}"
        folder.mkdir(exist_ok=True)
        for i, (name, en, ru) in enumerate(PAGES, 1):
            screenshot(SHOTS / lang / f"{name}.jpg", en if lang == "en" else ru).save(folder / f"{i}.png", optimize=True)
    print("done:", OUT)


if __name__ == "__main__":
    main()
