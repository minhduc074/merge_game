"""Post-process decor_raw/*.png -> drawable-nodpi/decor_<id>.png.

White background -> transparent via edge flood-fill (reuses
slice_sheet.exterior_to_alpha), trim, center on 512x512 canvas.
Large raw images are downscaled first so the BFS fill stays fast.
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from slice_sheet import exterior_to_alpha, trim, center_square  # noqa: E402

RAW = Path(__file__).resolve().parent.parent / "art_out" / "decor_raw"
DST = (Path(__file__).resolve().parent.parent / "app" / "src" / "main" /
       "res" / "drawable-nodpi")
OUT_PREVIEW = Path(__file__).resolve().parent.parent / "art_out" / "decor_slices"
SIZE = 512


def process_file(src: Path):
    img = Image.open(src).convert("RGBA")
    if max(img.size) > 1400:
        sc = 1400 / max(img.size)
        img = img.resize((int(img.width * sc), int(img.height * sc)),
                         Image.LANCZOS)
    img = exterior_to_alpha(img)
    img = trim(img, pad=10)
    img = center_square(img, SIZE)
    out = DST / src.name
    img.save(out)
    pv = OUT_PREVIEW / src.name
    img.save(pv)
    return img.size


def main():
    OUT_PREVIEW.mkdir(parents=True, exist_ok=True)
    files = sorted(RAW.glob("decor_*.png"))
    if not files:
        print("no decor_*.png in", RAW)
        return
    for f in files:
        try:
            size = process_file(f)
            print(f"{f.name}: -> {size}")
        except Exception as e:
            print(f"{f.name}: FAIL {e}")


if __name__ == "__main__":
    main()
