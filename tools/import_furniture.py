"""Import selected furniture-pack slices into drawable-nodpi as decor_<id>.png.

Each entry: decor_id -> (slice filename). Slices were produced by
slice_furniture.py into art_out/furniture_pack/slices/.
"""
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).parent.parent
SRC = ROOT / "art_out" / "furniture_pack" / "slices"
DST = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"

# decor_id -> slice file (numbered per contact_<sheet>.png)
PICKS = {
    # seating
    "sofa_mint": "furniture_1_00", "sofa_cat": "furniture_1_01",
    "sofa_wood": "furniture_1_02", "armchair_pink": "furniture_1_04",
    "armchair_mauve": "furniture_1_05", "armchair_blue": "furniture_1_08",
    "armchair_shell": "furniture_1_11", "chair_plaid": "furniture_1_16",
    "chair_green": "furniture_1_23", "ottoman_mint": "furniture_1_15",
    "stool_cloud": "furniture_1_17",
    # beds
    "bed_pink": "furniture_1_09", "bed_iron": "furniture_1_10",
    "bed_blue": "furniture_1_12", "loft_bed": "furniture_1_18",
    "bed_mint": "furniture_1_21", "bed_green": "furniture_1_24",
    # tables & cabinets
    "table_peach": "furniture_1_22", "table_cream": "furniture_1_34",
    "table_mint": "furniture_1_37", "bench_stripe": "furniture_1_32",
    "side_table": "furniture_1_33", "vanity": "furniture_1_36",
    "desk": "furniture_1_38", "cabinet_orange": "furniture_1_30",
    "cabinet_blue": "furniture_1_40", "dresser_cream": "furniture_1_47",
    "wardrobe_wood": "furniture_1_49", "cabinet_green": "furniture_1_50",
    "wardrobe_mint": "furniture_1_53", "shelf_mauve": "furniture_1_54",
    "wardrobe_ornate": "furniture_1_56", "shelf_wood": "furniture_2_walls_03",
    "bookshelf": "furniture_2_walls_08", "shelf_flower": "furniture_2_walls_05",
    "ladder_shelf": "furniture_2_walls_25",
    # decor
    "plant_big": "furniture_2_walls_24", "plant_basket": "furniture_2_walls_18",
    "plant_hang": "furniture_2_walls_12", "plant_stool": "furniture_2_walls_14",
    "plant_rack": "furniture_2_walls_23", "lamp_arc": "furniture_2_walls_17",
    "lamp_cat": "furniture_2_walls_15", "rug_oval": "furniture_2_walls_26",
    "rug_geo": "furniture_2_walls_28", "rug_cloud": "furniture_2_walls_29",
    "easel": "things_1_79", "guitar": "things_1_71",
    "radio": "things_1_74", "snow_globe": "things_2_people_25",
    "terrarium": "things_2_people_38",
    # fun / pets
    "cat_tower": "furniture_2_walls_21", "teepee": "furniture_2_walls_19",
    "cat_sit": "things_2_people_76", "cat_sleep": "things_2_people_70",
    # wall
    "door_wood": "furniture_2_walls_30", "door_arch": "furniture_2_walls_37",
    "window_mint": "furniture_2_walls_34", "window_curtain": "furniture_2_walls_42",
    "wall_shelf_wood": "furniture_2_walls_31", "wall_shelf_cream": "furniture_2_walls_33",
    "frame_hearts": "furniture_2_walls_50", "sign_dreams": "furniture_2_walls_52",
    "banner_moon": "furniture_2_walls_48", "mirror_oval": "furniture_2_walls_60",
    "mirror_ornate": "furniture_2_walls_57", "wall_cubby": "furniture_2_walls_44",
    "plant_wall": "furniture_2_walls_62", "garland": "things_1_10",
    "poster": "things_1_12", "macrame": "things_1_13",
    "wall_lamp": "things_1_15", "cabinet_wall": "furniture_2_walls_38",
}


def trim(im: Image.Image) -> Image.Image:
    a = np.array(im)
    ys, xs = np.where(a[:, :, 3] > 8)
    if len(ys) == 0:
        return im
    return im.crop((xs.min(), ys.min(), xs.max() + 1, ys.max() + 1))


def main():
    missing = []
    for decor_id, stem in PICKS.items():
        src = SRC / f"{stem}.png"
        if not src.exists():
            missing.append((decor_id, stem))
            continue
        im = trim(Image.open(src))
        im.thumbnail((512, 512), Image.LANCZOS)
        im.save(DST / f"decor_{decor_id}.png")
    if missing:
        print("MISSING:", missing)
    else:
        print(f"imported {len(PICKS)} sprites")


if __name__ == "__main__":
    main()
