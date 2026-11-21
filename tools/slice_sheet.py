"""Slice the AI-generated contact sheet into individual transparent PNGs.

Item bands are segmented by column whitespace; UI/icon region uses fixed rects
(two staggered rows that defeat row/column segmentation).
Exterior white is flood-filled to alpha; interior whites are preserved.
"""
from collections import deque
from pathlib import Path

import numpy as np
from PIL import Image

SHEET = Path(__file__).resolve().parent.parent / "art_out" / "Gemini_Generated_Image_tg93natg93natg93.png"
OUT = Path(__file__).resolve().parent.parent / "art_out" / "slices"

WHITE_TOL = 18
MIN_SEG_W = 40
GAP_COL = 8

# (y0, y1, [labels in column order])
BANDS = [
    (100, 290, ["fruit_1", "fruit_2", "fruit_3", "fruit_4", "fruit_5", "fruit_6",
                "fruit_7", "fruit_8", "fruit_9", "fruit_10", "fruit_11"]),
    (345, 535, ["bakery_1", "bakery_2", "bakery_3", "bakery_4", "bakery_5",
                "bakery_6", "bakery_7", "bakery_8", "bakery_9", "bakery_10"]),
    (640, 838, ["drink_1", "drink_2", "drink_3", "drink_4", "drink_5",
                "drink_6", "drink_7", "drink_8"]),
    (878, 1082, ["viet_1", "viet_2", "viet_3", "viet_4", "viet_5", "viet_6", "viet_7"]),
    (1195, 1395, ["gen_fruit_1", "gen_fruit_2", "gen_fruit_3", "gen_bakery_1",
                  "gen_bakery_2", "gen_bakery_3", "spare_cakemachine"]),
    (1432, 1612, ["gen_drink_1", "gen_drink_2", "gen_drink_3",
                  "gen_viet_1", "gen_viet_2", "gen_viet_3"]),
    (1725, 1940, ["cust_cat", "cust_dog", "cust_rabbit", "cust_panda", "cust_fox",
                  "cust_frog", "cust_pig", "cust_koala", "mascot"]),
]

# (x0, y0, x1, y1, name) — staggered UI + icon rows (detected bounds)
RECTS = [
    (10, 2050, 315, 2275, "ui_panel"),
    (335, 2030, 600, 2275, "ui_dialog"),
    (635, 2095, 810, 2275, "ui_cell"),
    (825, 2095, 985, 2275, "ui_ordercard"),
    (995, 2050, 1160, 2175, "ui_btn_green"),
    (1170, 2050, 1340, 2175, "ui_btn_gold"),
    (1345, 2050, 1505, 2175, "ui_btn_red"),
    (1520, 2040, 1660, 2190, "ui_btn_round"),
    (1005, 2185, 1145, 2330, "icon_coin"),
    (1170, 2185, 1340, 2330, "icon_energy"),
    (1360, 2185, 1500, 2330, "icon_star"),
    (1535, 2185, 1660, 2330, "icon_xp"),
    (10, 2355, 160, 2498, "icon_bag"),
    (182, 2355, 315, 2498, "icon_gift"),
    (330, 2355, 480, 2498, "icon_trophy"),
    (495, 2355, 640, 2498, "icon_question"),
    (660, 2355, 800, 2498, "icon_snow"),
    (820, 2355, 978, 2498, "icon_web"),
    (1000, 2355, 1150, 2498, "fx_frozen"),
    (1180, 2355, 1328, 2498, "fx_webtile"),
    (1360, 2355, 1498, 2498, "fx_sparkle"),
    (1530, 2355, 1675, 2498, "fx_coinfly"),
]


def mask_nonwhite(arr):
    diff = 255 - arr[:, :, :3].astype(np.int16)
    return (diff > WHITE_TOL).any(axis=2)


def col_segments(mask, gap, min_w):
    proj = mask.any(axis=0)
    out, start, empty = [], None, 0
    for i, v in enumerate(proj):
        if v:
            if start is None:
                start = i
            empty = 0
        else:
            empty += 1
            if start is not None and empty >= gap:
                if i - gap - start >= min_w:
                    out.append((start, i - gap))
                start, empty = None, 0
    if start is not None and len(proj) - start >= min_w:
        out.append((start, len(proj)))
    return out


def even_segments(mask, n):
    """Split content range into n cells; snap each boundary to the column
    with fewest non-white pixels within +-18px (finds the seam between
    touching items)."""
    proj = mask.any(axis=0)
    xs = np.nonzero(proj)[0]
    lo, hi = int(xs[0]), int(xs[-1])
    w = (hi - lo + 1) / n
    density = mask.sum(axis=0)
    bounds = [lo]
    for i in range(1, n):
        guess = int(lo + w * i)
        lo2, hi2 = max(lo + 1, guess - 18), min(hi, guess + 18)
        bounds.append(int(np.argmin(density[lo2:hi2])) + lo2)
    bounds.append(hi + 1)
    return [(bounds[i], bounds[i + 1]) for i in range(n)]


def keep_main_content(cell_mask):
    proj = cell_mask.any(axis=1)
    segs, start = [], None
    for i, v in enumerate(proj):
        if v and start is None:
            start = i
        elif not v and start is not None:
            segs.append((start, i))
            start = None
    if start is not None:
        segs.append((start, len(proj)))
    if len(segs) <= 1:
        return cell_mask
    main = max(segs, key=lambda s: s[1] - s[0])
    keep = np.zeros_like(cell_mask)
    keep[main[0]:main[1], :] = cell_mask[main[0]:main[1], :]
    return keep


def exterior_to_alpha(img):
    a = np.array(img.convert("RGBA"))
    h, w = a.shape[:2]
    near_white = (a[:, :, :3].astype(np.int16) > 255 - WHITE_TOL * 3).all(axis=2)
    seen = np.zeros((h, w), dtype=bool)
    q = deque()
    for x in range(w):
        for y in (0, h - 1):
            if near_white[y, x] and not seen[y, x]:
                seen[y, x] = True
                q.append((y, x))
    for y in range(h):
        for x in (0, w - 1):
            if near_white[y, x] and not seen[y, x]:
                seen[y, x] = True
                q.append((y, x))
    while q:
        y, x = q.popleft()
        a[y, x, 3] = 0
        for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            ny, nx = y + dy, x + dx
            if 0 <= ny < h and 0 <= nx < w and near_white[ny, nx] and not seen[ny, nx]:
                seen[ny, nx] = True
                q.append((ny, nx))
    return Image.fromarray(a)


def trim(img, pad=4):
    bbox = img.getbbox()
    if bbox:
        x0, y0, x1, y1 = bbox
        x0 = max(0, x0 - pad); y0 = max(0, y0 - pad)
        x1 = min(img.width, x1 + pad); y1 = min(img.height, y1 + pad)
        return img.crop((x0, y0, x1, y1))
    return img


def center_square(img, size=256):
    scale = min((size - 20) / img.width, (size - 20) / img.height, 1.0)
    w, h = max(1, int(img.width * scale)), max(1, int(img.height * scale))
    img = img.resize((w, h), Image.LANCZOS)
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    canvas.paste(img, ((size - w) // 2, (size - h) // 2), img)
    return canvas


def process(sub):
    sub = exterior_to_alpha(sub)
    return center_square(trim(sub))


def main():
    im = Image.open(SHEET).convert("RGBA")
    arr = np.array(im)
    mask = mask_nonwhite(arr)
    OUT.mkdir(parents=True, exist_ok=True)
    saved = []

    for bi, (y0, y1, labels) in enumerate(BANDS):
        band_mask = mask[y0:y1, :]
        cols = col_segments(band_mask, GAP_COL, MIN_SEG_W)
        if len(cols) != len(labels):
            cols = even_segments(band_mask, len(labels))
            how = "even"
        else:
            how = "gaps"
        print(f"band {bi} y{y0}-{y1}: {len(cols)} cols ({how}), {len(labels)} labels")
        for ci, (x0, x1) in enumerate(cols):
            name = labels[ci] if ci < len(labels) else f"band{bi}_{ci}"
            cell_mask = keep_main_content(band_mask[:, x0:x1])
            sa = np.array(im.crop((x0, y0, x1, y1)))
            sa[~cell_mask] = [0, 0, 0, 0]
            out = process(Image.fromarray(sa))
            out.save(OUT / f"{name}.png")
            saved.append(name)

    for x0, y0, x1, y1, name in RECTS:
        out = process(im.crop((x0, y0, x1, y1)))
        out.save(OUT / f"{name}.png")
        saved.append(name)

    print(f"\n{len(saved)} slices -> {OUT}")


if __name__ == "__main__":
    main()
