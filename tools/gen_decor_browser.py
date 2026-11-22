"""Batch-drive the open Gemini Edge tab to generate & save decor sprites.

Modes:
  python -u tools/gen_decor_browser.py            -> full batch (send+gen+dl)
  python -u tools/gen_decor_browser.py ids...     -> subset
  python -u tools/gen_decor_browser.py --collect a b c
      salvage: walk chat bottom->top; for each unseen image band open the
      Edge viewer, download full-size, save as decor_<a>.png etc.

Extraction: click image center -> Edge image viewer -> download icon
(1696,124) -> newest ~/Downloads file (~40-60s for full-size) ->
back arrow (1096,123).
"""
import sys, time, os, glob, shutil, hashlib
import pyautogui, pyperclip
import win32gui, win32con
from PIL import ImageGrab

sys.stdout.reconfigure(encoding="utf-8", errors="replace")
pyautogui.FAILSAFE = False

HWND = 0x10696
CHAT = (1100, 95, 1850, 880)
INPUT_POS = (1480, 925)
VIEWER_DL = (1696, 124)
VIEWER_BACK = (1096, 123)
SCROLL_PT = (1480, 500)
DL_DIR = os.path.expanduser("~/Downloads")
OUT_DIR = os.path.join(os.path.dirname(__file__), "..", "art_out", "decor_raw")

STYLE = (", hand-drawn cartoon style, thick dark-brown outlines, soft cel "
         "shading with subtle highlights, warm pastel color palette, kawaii "
         "cozy cafe game asset, centered on plain white background, clean "
         "vector look, mobile game art, no text, no watermark, no ground shadow")

ITEMS = [
    ("chair_wood", "a single wooden cafe dining chair, slight 3/4 oblique view"),
    ("armchair", "a single soft padded armchair in warm beige, slight 3/4 oblique view"),
    ("sofa", "a long cozy three-seat sofa couch in cream, slight 3/4 oblique view"),
    ("table_cafe", "a small round wooden cafe table, slight 3/4 oblique view"),
    ("counter", "a bakery display counter with glass pastry case, slight 3/4 oblique view"),
    ("shelf", "a tall narrow wooden shelf with pastries and jars, slight 3/4 oblique view"),
    ("plant", "a potted leafy green houseplant in terracotta pot, slight 3/4 oblique view"),
    ("lamp", "a standing floor lamp with warm glowing lampshade, slight 3/4 oblique view"),
    ("rug_round", "a round braided pastel rug viewed from directly above, flat top-down view"),
    ("rug_big", "a large rectangular soft pastel rug with simple border pattern viewed from directly above, flat top-down view"),
    ("wall_art", "a framed cute painting of a cupcake, straight front view"),
    ("clock", "a cute round wall clock with wooden frame showing 10:10, straight front view"),
    ("shelf_wall", "a small wooden wall-mounted shelf holding a tiny plant and two books, straight front view"),
    ("window", "a cute window with light blue curtains and a view of blue sky, straight front view"),
    # room backgrounds — full-bleed textures, NOT sprites (custom prompt,
    # no STYLE suffix): flat straight-on views that tile under the grid.
    ("bg_wall", "a cute cafe interior wall seen straight on, flat front view, warm cream wallpaper with a subtle tiny polka dot pattern, thin white baseboard along the very bottom edge, kawaii cozy cartoon style, soft hand-drawn look, mobile game background, full bleed edge to edge, no furniture, no objects, no window, no text, no watermark", ""),
    ("bg_floor", "cute light wooden floor planks texture seen from directly above, flat top-down view, horizontal planks in warm honey beige with visible wood grain, kawaii cozy cartoon style, soft hand-drawn look, mobile game background, full bleed edge to edge, no objects, no rug, no text, no watermark", ""),
]


CHAT_ID = "8904eca08615ed2f"


def activate():
    pyautogui.keyDown("alt"); pyautogui.keyUp("alt")
    win32gui.ShowWindow(HWND, win32con.SW_RESTORE)
    time.sleep(0.3)
    try:
        win32gui.SetForegroundWindow(HWND)
    except Exception:
        pass
    time.sleep(0.4)


def ensure_chat():
    """If the tab drifted to another Gemini chat, navigate back."""
    try:
        from pywinauto import Application
        app = Application(backend="uia").connect(handle=HWND)
        win = app.window(handle=HWND)
        url = ""
        for e in win.descendants(control_type="Edit"):
            if "Address" in (e.window_text() or ""):
                url = e.get_value() or ""
                break
        if CHAT_ID in url:
            return
    except Exception:
        pass
    activate()
    pyautogui.hotkey("ctrl", "l"); time.sleep(0.4)
    pyperclip.copy(f"https://gemini.google.com/app/{CHAT_ID}")
    pyautogui.hotkey("ctrl", "v"); time.sleep(0.3)
    pyautogui.press("enter"); time.sleep(5)
    scroll_back_to_bottom()


def send(text):
    activate()
    pyautogui.click(*INPUT_POS)
    time.sleep(0.5)
    pyperclip.copy(text)
    pyautogui.hotkey("ctrl", "a"); time.sleep(0.15)
    pyautogui.hotkey("ctrl", "v"); time.sleep(0.5)
    pyautogui.press("enter")


def bottom_band():
    img = ImageGrab.grab(bbox=CHAT).convert("L")
    img = img.resize((img.width // 2, img.height // 2))
    W, H = img.size
    px = img.load()
    rows = [sum(1 for x in range(W) if px[x, y] > 120) for y in range(H)]
    bands, cur = [], []
    for y, c in enumerate(rows):
        if c > W * 0.15:
            cur.append(y)
        elif cur:
            bands.append(cur); cur = []
    if cur:
        bands.append(cur)
    # image bands are tall; ignore thin bright slivers (partial scroll edges)
    bands = [b for b in bands if b[-1] - b[0] > 60]
    if not bands:
        return None
    b = max(bands, key=lambda bb: bb[-1])
    y0, y1 = b[0] * 2 + CHAT[1], b[-1] * 2 + CHAT[1]
    cols = [sum(1 for y in range(b[0], b[-1] + 1) if px[x, y] > 120) for x in range(W)]
    xs = [x for x, c in enumerate(cols) if c > (b[-1] - b[0]) * 0.3]
    if not xs:
        return None
    return xs[0] * 2 + CHAT[0], y0, xs[-1] * 2 + CHAT[0], y1


def band_sig(box):
    x0, y0, x1, y1 = box
    pad_x, pad_y = (x1 - x0) // 6, (y1 - y0) // 6
    crop = ImageGrab.grab(bbox=(x0 + pad_x, y0 + pad_y, x1 - pad_x, y1 - pad_y))
    return crop.resize((24, 24)).convert("L").tobytes()


def _diff(a, b):
    return sum(abs(x - y) for x, y in zip(a, b))


def wait_new_image(prev_sig, seen_sigs, timeout=160):
    t0, same = time.time(), 0
    while time.time() - t0 < timeout:
        box = bottom_band()
        if box:
            sig = band_sig(box)
            if sig != prev_sig and all(_diff(sig, s) > 200 for s in seen_sigs):
                same += 1
                if same >= 2:
                    return box, sig
            else:
                same = 0
        time.sleep(4)
    return None, prev_sig


def wait_dl(before, wait=110):
    t0 = time.time()
    while time.time() - t0 < wait:
        time.sleep(2)
        for f in glob.glob(DL_DIR + "/*.png"):
            if f not in before or os.path.getmtime(f) > before.get(f, 0):
                if time.time() - os.path.getmtime(f) > 2:
                    return f
    return None


def download_via_viewer(box, wait=110):
    """Click image -> Edge viewer -> download icon -> wait for file.
    Esc (not the back arrow) closes the viewer — safe even if the
    viewer never opened."""
    before = {f: os.path.getmtime(f) for f in glob.glob(DL_DIR + "/*")}
    activate()
    cx, cy = (box[0] + box[2]) // 2, (box[1] + box[3]) // 2
    pyautogui.click(cx, cy)
    time.sleep(1.8)
    pyautogui.click(*VIEWER_DL)
    f = wait_dl(before, wait)
    pyautogui.press("esc")
    time.sleep(1.2)
    return f


def md5(path):
    return hashlib.md5(open(path, "rb").read()).hexdigest()


def collect(item_ids):
    """Walk chat bottom->top, downloading each unseen image band."""
    seen_sigs, seen_hashes = [], set()
    for f in glob.glob(os.path.join(OUT_DIR, "*.png")):
        seen_hashes.add(md5(f))
    scroll_back_to_bottom()
    queue = list(item_ids)
    failsafe = 40
    while queue and failsafe > 0:
        failsafe -= 1
        box = bottom_band()
        if not box:
            pyautogui.scroll(700, *SCROLL_PT)
            time.sleep(1.2)
            continue
        sig = band_sig(box)
        if any(_diff(sig, s) <= 200 for s in seen_sigs):
            pyautogui.scroll(500, *SCROLL_PT)
            time.sleep(1.2)
            continue
        item_id = queue.pop(0)
        print(f"[dl  ] {item_id}", flush=True)
        f = download_via_viewer(box)
        if not f:
            print(f"[fail] {item_id}: dl timeout")
            seen_sigs.append(sig)
            continue
        h = md5(f)
        if h in seen_hashes:
            print(f"[fail] {item_id}: dup")
            seen_sigs.append(sig)
            continue
        seen_hashes.add(h)
        seen_sigs.append(sig)
        shutil.copy(f, os.path.join(OUT_DIR, f"decor_{item_id}.png"))
        print(f"[ ok ] {item_id} <- {os.path.basename(f)}", flush=True)


def scroll_back_to_bottom():
    activate()
    pyautogui.moveTo(*SCROLL_PT)
    for _ in range(30):
        pyautogui.scroll(-1000)
        time.sleep(0.15)
    time.sleep(1)


def batch(only):
    prev_box = bottom_band()
    prev_sig = band_sig(prev_box) if prev_box else None
    seen_sigs = [prev_sig] if prev_sig else []
    seen_hashes = set()
    for f in glob.glob(os.path.join(OUT_DIR, "*.png")):
        seen_hashes.add(md5(f))
    for entry in ITEMS:
        item_id, subj = entry[0], entry[1]
        style = entry[2] if len(entry) > 2 else STYLE
        if only and item_id not in only:
            continue
        out = os.path.join(OUT_DIR, f"decor_{item_id}.png")
        if os.path.exists(out):
            print(f"[skip] {item_id}")
            continue
        print(f"[gen ] {item_id}", flush=True)
        ensure_chat()
        send(f"Generate an image: {subj}{style}")
        box, sig = wait_new_image(prev_sig, seen_sigs)
        if not box:
            print(f"[fail] {item_id}: no new image")
            continue
        f = download_via_viewer(box)
        if not f:
            print(f"[fail] {item_id}: dl timeout")
            prev_sig = sig
            continue
        h = md5(f)
        if h in seen_hashes:
            print(f"[fail] {item_id}: dup")
            prev_sig = sig
            continue
        seen_hashes.add(h)
        seen_sigs.append(sig)
        shutil.copy(f, out)
        prev_sig = sig
        print(f"[ ok ] {item_id} <- {os.path.basename(f)}", flush=True)
        time.sleep(2)
    print("DONE", flush=True)


if __name__ == "__main__":
    os.makedirs(OUT_DIR, exist_ok=True)
    args = sys.argv[1:]
    if args and args[0] == "--collect":
        collect(args[1:])
    else:
        batch(set(args))
