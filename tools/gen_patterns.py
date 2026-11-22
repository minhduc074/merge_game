"""Generate seamless line-art pattern tiles for the decor room shell.

Each tile is a transparent PNG with soft brown ink strokes only -- the
surface tint color is drawn underneath by the renderer, so one texture
works for every color variant (like the reference picker: pattern tile +
color dots).

Output -> app/src/main/res/drawable-nodpi/{wtex,ftex}_*.png
"""

from pathlib import Path

from PIL import Image, ImageDraw

TILE = 256
INK = (110, 80, 62, 115)       # warm brown line art
INK_SOFT = (110, 80, 62, 70)   # lighter inner strokes
OUT = Path(__file__).parent.parent / "app/src/main/res/drawable-nodpi"


def new():
    img = Image.new("RGBA", (TILE, TILE), (0, 0, 0, 0))
    return img, ImageDraw.Draw(img)


def wrapped(d, fn, pts):
    """Draw motif at every position ± one tile so the texture tiles."""
    for x, y in pts:
        for dx in (-TILE, 0, TILE):
            for dy in (-TILE, 0, TILE):
                fn(d, x + dx, y + dy)


def flower(d, x, y, r=9):
    d.line([x, y + r, x, y + r + 8], fill=INK_SOFT, width=2)
    for a in range(5):
        import math
        px = x + math.cos(a * 1.2566) * r * 0.7
        py = y + math.sin(a * 1.2566) * r * 0.7
        d.ellipse([px - 3, py - 3, px + 3, py + 3], fill=INK)
    d.ellipse([x - 2, y - 2, x + 2, y + 2], fill=(110, 80, 62, 150))


def wtex_floral():
    img, d = new()
    pts = [(20, 30), (150, 20), (90, 90), (200, 80), (40, 150),
           (140, 160), (220, 150), (70, 230), (180, 240)]
    wrapped(d, flower, pts)
    # small sprig leaves
    def leaf(dd, x, y):
        dd.ellipse([x, y, x + 6, y + 3], outline=INK_SOFT, width=2)
    wrapped(d, leaf, [(60, 60), (180, 120), (110, 200), (240, 30)])
    return img


def wtex_gingham():
    img, d = new()
    s = 64
    for gy in range(TILE // s):
        for gx in range(TILE // s):
            if (gx + gy) % 2 == 1:
                d.rectangle([gx * s, gy * s, gx * s + s, gy * s + s],
                            fill=(110, 80, 62, 28))
    for i in range(TILE // s + 1):
        d.line([i * s, 0, i * s, TILE], fill=(110, 80, 62, 45), width=2)
        d.line([0, i * s, TILE, i * s], fill=(110, 80, 62, 45), width=2)
    return img


def dots(step=48, r=5):
    img, d = new()
    pts = [(x + (step // 2 if (y // step) % 2 else 0), y)
           for y in range(0, TILE, step) for x in range(0, TILE, step)]

    def dot(dd, x, y):
        dd.ellipse([x - r, y - r, x + r, y + r], fill=INK)
    wrapped(d, dot, pts)
    return img


def wtex_stripes():
    img, d = new()
    for x in range(0, TILE, 40):
        d.rectangle([x, 0, x + 18, TILE], fill=(110, 80, 62, 26))
        d.line([x + 18, 0, x + 18, TILE], fill=(110, 80, 62, 55), width=2)
    return img


def wtex_panel():
    """Wainscot: vertical panels with an inset frame (drawn on the wall's
    bottom band; the renderer clips it to the wall quad)."""
    img, d = new()
    pw = 64
    for x in range(0, TILE, pw):
        d.rectangle([x + 6, 10, x + pw - 6, TILE - 10],
                    outline=(110, 80, 62, 90), width=3)
        d.rectangle([x + 14, 22, x + pw - 14, TILE - 22],
                    outline=INK_SOFT, width=2)
        d.line([x, 0, x, TILE], fill=(110, 80, 62, 55), width=3)
    return img


def ftex_wood():
    img, d = new()
    rh = 43  # plank row height
    for row, y in enumerate(range(0, TILE, rh)):
        d.line([0, y, TILE, y], fill=INK, width=3)
        # staggered joints
        for jx in range((row * 97) % TILE - 60, TILE + 80, 130):
            x = jx % TILE
            d.line([x, y, x, y + rh], fill=INK, width=3)
        # grain squiggles
        for gx in range(30 + (row * 53) % 80, TILE, 170):
            gy = y + rh // 2 + (row % 3 - 1) * 6
            d.arc([gx, gy - 5, gx + 46, gy + 5], 10, 170,
                  fill=INK_SOFT, width=2)
    return img


def ftex_checker():
    img, d = new()
    s = 64
    for gy in range(TILE // s):
        for gx in range(TILE // s):
            if (gx + gy) % 2 == 1:
                d.rectangle([gx * s, gy * s, gx * s + s, gy * s + s],
                            fill=(110, 80, 62, 42))
    return img


def ftex_parquet():
    """Basket-weave parquet: pairs of small planks h/v in a checker
    arrangement."""
    img, d = new()
    pw, ph = 64, 32
    for row in range(TILE // ph):
        for col in range(TILE // pw):
            x0, y0 = col * pw, row * ph
            horiz = (row + col) % 2 == 0
            if horiz:
                d.rectangle([x0 + 3, y0 + 5, x0 + pw - 3, y0 + ph // 2],
                            outline=INK, width=2)
                d.rectangle([x0 + 3, y0 + ph // 2, x0 + pw - 3, y0 + ph - 5],
                            outline=INK, width=2)
            else:
                d.rectangle([x0 + 5, y0 + 3, x0 + pw // 2, y0 + ph - 3],
                            outline=INK, width=2)
                d.rectangle([x0 + pw // 2, y0 + 3, x0 + pw - 5, y0 + ph - 3],
                            outline=INK, width=2)
    return img


def main():
    tiles = {
        "wtex_floral": wtex_floral(),
        "wtex_gingham": wtex_gingham(),
        "wtex_dots": dots(step=44, r=5),
        "wtex_stripes": wtex_stripes(),
        "wtex_panel": wtex_panel(),
        "ftex_wood": ftex_wood(),
        "ftex_dots": dots(step=52, r=6),
        "ftex_checker": ftex_checker(),
        "ftex_parquet": ftex_parquet(),
    }
    for name, img in tiles.items():
        img.save(OUT / f"{name}.png")
        print(name, "ok")


if __name__ == "__main__":
    main()
