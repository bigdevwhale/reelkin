"""
Writes the prompts for the achievements' pictures (art/icons/prompts/featsN.txt): nine to a 3x3 sheet, in the order of
FEATS, which is the order of the app's Feat enum. Draw them with tools/art_gen.py, cut them with tools/icons_cut.py.

    python tools/feat_prompts.py
"""
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "art", "icons", "prompts")

FEATS = [
    # time
    ("hours", "An hourglass whose falling sand is tiny golden film frames."),
    ("films", "A tall stack of round film cans with a golden ribbon around it."),
    ("days", "A tear-off desk calendar page with a small film strip instead of the date."),
    ("streak", "A flame shaped like a film reel."),
    ("big_day", "A sun wearing tiny 3D cinema glasses."),
    ("marathon", "A running sneaker with a film strip trailing behind it like a scarf."),
    ("veteran", "An old brass pocket watch with a film reel on its face."),
    ("weekend", "A comfy sofa with a bowl of popcorn on it."),
    # finds
    ("countries", "A small globe on a stand with a film strip orbiting it."),
    ("decades", "A vintage radio-style wooden cabinet television with a glowing screen."),
    ("old", "A black-and-white silent-film camera on a wooden tripod."),
    ("silent", "A bowler hat and a bamboo cane crossed."),
    ("new", "A shiny new cinema ticket with a sparkle."),
    ("directors", "A director's canvas chair with a megaphone on the seat."),
    ("director_fan", "A director's megaphone with a small heart."),
    ("actor_fan", "A golden star on a pink star-shaped plaque like a walk of fame."),
    ("genres", "A painter's palette whose paint blobs are tiny film reels in many colours."),
    ("east", "A red paper lantern with a small film reel painted on it."),
    # habits
    ("night", "An owl with cinema 3D glasses sitting on a crescent moon."),
    ("morning", "A coffee cup with steam shaped like a film strip."),
    ("rewatch", "A circular repeat arrow wrapped around a film reel."),
    ("scenes", "A small cinema screen with a star glowing on it after the end credits."),
    ("words", "A cream notepad with a pencil and three short lines written."),
    ("rated", "A striped popcorn box with a gold check mark."),
    ("five", "Five popcorn kernels in a row, glowing golden."),
    ("company", "Two cinema seats side by side with a heart above them."),
    ("date", "Two drinks with straws clinking, a small heart between them."),
    ("party", "A big shared bucket of popcorn with several hands reaching in."),
    ("honest", "An exit sign with a little running figure."),
    ("short", "A small stopwatch with a lightning bolt."),
    # genres
    ("g_noir", "A detective's fedora and a magnifying glass."),
    ("g_scifi", "A small flying saucer with a beam of light."),
    ("g_romance", "A red rose with a heart-shaped leaf."),
    ("g_comedy", "A clown's red nose and a smiling theatre mask."),
    ("g_doc", "A vintage microphone in front of an open notebook."),
    ("g_horror", "A cute little ghost peeking out from behind a curtain."),
    ("g_western", "A cowboy hat and a sheriff's star."),
    ("g_animation", "A pencil drawing a little smiling cartoon star."),
    ("g_musical", "A top hat with a cane and a musical note."),
    ("g_drama", "A pair of theatre masks, happy and sad."),
    ("g_adventure", "A compass on an old treasure map."),
    ("g_reality", "A smartphone with a red recording dot on its screen."),
    # series
    ("episodes", "A long ticket roll unspooling."),
    ("seasons", "A box set of four film cans tied with a red ribbon."),
    ("binge", "A cosy blanket with a TV remote on it."),
    ("shows", "A vintage television set with a small antenna."),
    # the booth
    ("pets", "A nest with three closed round film cans in it like eggs."),
    ("grown", "A friendly small dragon proudly wearing a tiny bow tie."),
    ("ancient", "A golden crown with a film reel jewel."),
    ("shiny", "A sparkling rainbow-coloured film can surrounded by twinkles."),
    ("cosy", "A velvet armchair with a string of fairy lights above."),
    ("quests", "A clipboard with three ticked boxes and a film frame."),
    ("goals", "A chequered finish flag crossed with a golden trophy cup."),
    ("awards", "A small golden statuette on a black stand."),
    ("hall", "A row of red velvet cinema seats, all full of tiny spectators' heads."),
    ("buffet", "A popcorn machine cart with a red striped roof."),
]

HEAD = """Use your image generation tool to create ONE image, then save it as PNG with transparency to the path given at the end (create folders if needed). The attached pictures are style references from this game: a pet and the booth it lives in. Match their style: detailed pixel art with fine pixels (like a 64x64 game icon, not chunky), crisp dark outline, soft cel shading with 3-4 tones per colour, rich saturated cosy colours, warm lamp-lit evening mood. Everything is about cinema and watching films.

A sheet of {n} separate game icons in a 3x3 grid, read left to right, row by row, in the order listed. Each icon sits in its own cell, centred, about the same size as the others, filling most of its cell, with wide empty space between icons: they must not touch or overlap. Each icon is a single compact object or a small tight group, drawn complete and alone, seen from the front.
Transparent background. No text, no letters, no numbers, no labels, no frames or circles around the icons, no grid lines, no ground shadow, nothing else in the picture.

"""


def sheets():
    return {f"feats{i // 9 + 1}": FEATS[i:i + 9] for i in range(0, len(FEATS), 9)}


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    for k, items in sheets().items():
        body = "\n".join(f"{i + 1}. {d}" for i, (_, d) in enumerate(items))
        with open(os.path.join(OUT, k + ".txt"), "w", encoding="utf-8") as f:
            f.write(HEAD.format(n=len(items)) + body + f"\n\nSave the image to: art/icons/sheets/{k}.png\n")
    print(len(FEATS), "feats,", len(sheets()), "sheets")
