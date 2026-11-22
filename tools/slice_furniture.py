"""Slice sticker sprites out of the furniture-pack catalog screenshots.

Each sheet is a cream-background collage. Strategy:
1. bg = "light & low-saturation" pixels reachable from the image edge
   (covers the cream bg AND the white rounded cards some sprites sit on —
   sprite interiors are protected by their dark outlines).
2. Connected components of the remainder = sprites (+ badges, header
   text, section icons) — filtered by area.
3. Each component is exported as a transparent PNG (bg flooded to alpha).

Outputs: art_out/furniture_pack/slices/<sheet>_<n>.png + contact sheet.
"""
from collections import deque
from pathlib import Path

import numpy as np
from PIL import Image

SRC = Path(__file__).parent.parent / "art_out" / "furniture_pack"
OUT = SRC / "slices"
MIN_AREA = 900  # px — below this it's a badge/icon/text speck
PAD = 6

SHEETS = ["furniture_1.jpg", "furniture_2_walls.jpg",
          "things_1.jpg", "things_2_people.jpg", "people.png"]


def bg_mask(a: np.ndarray) -> np.ndarray:
    """True where the pixel looks like cream bg / white card."""
    mx = a.max(axis=2)
    mn = a.min(axis=2)
    return (mx - mn < 28) & (mn > 218)


def edge_flood(mask: np.ndarray) -> np.ndarray:
    """Return pixels of `mask` reachable from the image border."""
    h, w = mask.shape
    seen = np.zeros_like(mask)
    q = deque()
    for x in range(w):
        for y in (0, h - 1):
            if mask[y, x] and not seen[y, x]:
                seen[y, x] = True
                q.append((y, x))
    for y in range(h):
        for x in (0, w - 1):
            if mask[y, x] and not seen[y, x]:
                seen[y, x] = True
                q.append((y, x))
    while q:
        y, x = q.popleft()
        for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            ny, nx = y + dy, x + dx
            if 0 <= ny < h and 0 <= nx < w and mask[ny, nx] and not seen[ny, nx]:
                seen[ny, nx] = True
                q.append((ny, nx))
    return seen


def components(fg: np.ndarray):
    h, w = fg.shape
    label = np.full((h, w), -1, dtype=np.int32)
    boxes = []  # (x0, y0, x1, y1, area)
    q = deque()
    for y in range(h):
        for x in range(w):
            if fg[y, x] and label[y, x] < 0:
                i = len(boxes)
                label[y, x] = i
                q.append((y, x))
                x0 = x1 = x
                y0 = y1 = y
                area = 0
                while q:
                    cy, cx = q.popleft()
                    area += 1
                    x0 = min(x0, cx); x1 = max(x1, cx)
                    y0 = min(y0, cy); y1 = max(y1, cy)
                    for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                        ny, nx = cy + dy, cx + dx
                        if 0 <= ny < h and 0 <= nx < w and fg[ny, nx] and label[ny, nx] < 0:
                            label[ny, nx] = i
                            q.append((ny, nx))
                boxes.append((x0, y0, x1, y1, area))
    return label, boxes


def main():
    OUT.mkdir(exist_ok=True)
    for sheet in SHEETS:
        img = Image.open(SRC / sheet).convert("RGB")
        a = np.array(img).astype(np.int16)
        h, w, _ = a.shape
        bg = edge_flood(bg_mask(a))
        fg = ~bg
        label, boxes = components(fg)
        # sort top-to-bottom, left-to-right for deterministic names
        boxes = [b for b in boxes if b[4] >= MIN_AREA]
        boxes.sort(key=lambda b: (b[1] // 80, b[0]))
        print(f"{sheet}: {len(boxes)} sprites")
        stem = sheet.split(".")[0]
        for i, (x0, y0, x1, y1, area) in enumerate(boxes):
            x0 = max(0, x0 - PAD); y0 = max(0, y0 - PAD)
            x1 = min(w, x1 + PAD); y1 = min(h, y1 + PAD)
            crop = img.crop((x0, y0, x1, y1)).convert("RGBA")
            ca = np.array(crop)
            inner_bg = edge_flood(bg_mask(ca[:, :, :3].astype(np.int16)))
            ca[inner_bg, 3] = 0
            Image.fromarray(ca).save(OUT / f"{stem}_{i:02d}.png")


if __name__ == "__main__":
    main()
