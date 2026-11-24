"""One-off: drive the open Gemini Edge tab to generate the block-puzzle
screen backdrop, then download it to art_out/block_raw/.

Reuses gen_decor_browser's machinery; only HWND differs (window was
re-opened since the last batch)."""
import sys, os, time, shutil

sys.path.insert(0, os.path.dirname(__file__))
import gen_decor_browser as g

g.HWND = 0x74005A
OUT_DIR = os.path.join(os.path.dirname(__file__), "..", "art_out", "block_raw")

PROMPT = (
    "Generate an image: dark navy blue night sky background for a block "
    "puzzle game, deep indigo #1B243B, a few faint glowing pastel jelly "
    "squares and tiny stars softly scattered and dimmed, dreamy and very "
    "dark, minimal, mobile game background, full bleed edge to edge, "
    "no objects, no characters, no text, no watermark"
)


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    g.ensure_chat()
    prev = g.bottom_band()
    prev_sig = g.band_sig(prev) if prev else None
    seen = [prev_sig] if prev_sig else []
    g.send(PROMPT)
    box, sig = g.wait_new_image(prev_sig, seen)
    if not box:
        print("[fail] no new image", flush=True)
        return
    f = g.download_via_viewer(box)
    if not f:
        print("[fail] dl timeout", flush=True)
        return
    dst = os.path.join(OUT_DIR, "bg_blockblast.png")
    shutil.copy(f, dst)
    print(f"[ ok ] {dst} <- {os.path.basename(f)}", flush=True)


if __name__ == "__main__":
    main()
