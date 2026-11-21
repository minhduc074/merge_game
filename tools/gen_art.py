"""Generate game art assets via OpenRouter image models.

Usage: python tools/gen_art.py [out_dir] [model]
Reads OPENROUTER_API_KEY from ../test_ai/.env or env.
"""
import base64
import json
import os
import sys
import time
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

def load_key():
    for env_path in [ROOT / "tools" / ".env", ROOT / ".env",
                     Path("G:/AI/test_ai/.env"), Path("G:/AI/free_ai/.env")]:
        if env_path.exists():
            for line in env_path.read_text().splitlines():
                if line.startswith("OPENROUTER_API_KEY="):
                    return line.split("=", 1)[1].strip().strip('"\'')
    return os.environ.get("OPENROUTER_API_KEY")

STYLE = (
    "Cute cartoon game asset, hand-drawn style, thick dark-brown outlines, "
    "soft cel shading with subtle highlights, warm pastel color palette, "
    "kawaii chibi proportions, clean vector look, cozy bakery/cafe theme, "
    "mobile game art, high quality, no text, no watermark, no letters"
)
ISOLATED = ", single object centered, isolated on plain solid white background"

# defId -> prompt subject
ASSETS = {
    # fruit chain
    "fruit_1": "a pair of red cherries with green stem and leaf",
    "fruit_2": "a fresh red strawberry with green leaves and tiny seeds",
    "fruit_3": "a round orange fruit with a small green leaf",
    "fruit_4": "a shiny red apple with a brown stem",
    "fruit_5": "a bunch of purple grapes on a small vine",
    "fruit_6": "a slice of watermelon with red flesh and green rind",
    "fruit_7": "a spiky yellow pineapple with green crown leaves",
    "fruit_8": "a wooden plate of mixed fruit salad, colorful chunks",
    "fruit_9": "a tall glass of tropical fruit smoothie with a straw and tiny umbrella",
    "fruit_10": "a decorated fruit cake with cherries and strawberries on top",
    "fruit_11": "a luxurious golden VIP cake with fruits, cream and a little crown",
    # bakery chain
    "bakery_1": "a small bundle of golden wheat stalks tied with string",
    "bakery_2": "a loaf of white bread, slightly sliced",
    "bakery_3": "a golden flaky croissant",
    "bakery_4": "a round chocolate chip cookie",
    "bakery_5": "a pink frosted donut with colorful sprinkles",
    "bakery_6": "a cute cupcake with swirled cream frosting and a cherry",
    "bakery_7": "a stack of pancakes with dripping honey and a butter cube",
    "bakery_8": "a cream cake slice with strawberries, bakery style",
    "bakery_9": "a birthday cake with lit candles and cream roses",
    "bakery_10": "a royal golden crown-shaped cake with jewels and frosting",
    # drink chain
    "drink_1": "a clear glass of pure water with a tiny droplet sparkle",
    "drink_2": "a small cup of hot green tea with steam swirls",
    "drink_3": "a bottle of fresh milk with a blue cap",
    "drink_4": "a cup of brown coffee with latte art heart, steam",
    "drink_5": "a pearl milk tea boba cup with black pearls and straw",
    "drink_6": "a glass of fresh orange juice with a slice on the rim",
    "drink_7": "a colorful tropical mocktail with ice, mint and umbrella",
    "drink_8": "a fancy cocktail glass with layered colors, party drink",
    # viet chain
    "viet_1": "a small bowl of steamed white rice with chopsticks",
    "viet_2": "a Vietnamese banh mi sandwich with herbs and meat filling",
    "viet_3": "a steaming bowl of pho noodle soup with beef and herbs",
    "viet_4": "fresh spring rolls goi cuon with shrimp and herbs, peanut sauce",
    "viet_5": "a bowl of bun cha with grilled pork and vermicelli",
    "viet_6": "a traditional Vietnamese family meal tray with many small dishes",
    "viet_7": "a grand VIP feast table with full dishes and golden decor",
    # generators
    "gen_fruit_1": "a woven wicker basket full of mixed fruits",
    "gen_fruit_2": "a wooden fruit cart with wheels and a small canopy",
    "gen_fruit_3": "a cute delivery truck loaded with fruit crates",
    "gen_bakery_1": "a cardboard box filled with bread and pastries",
    "gen_bakery_2": "a small cozy bakery shop front with a striped awning",
    "gen_bakery_3": "a whimsical cake factory with a chimney and pipes",
    "gen_drink_1": "a small drink blender juicer machine with a cup",
    "gen_drink_2": "a cute mini fridge with a glass door full of drinks",
    "gen_drink_3": "a wooden bar counter with bottles and shakers",
    "gen_viet_1": "a small gas stove with a pot, simple kitchen burner",
    "gen_viet_2": "a big fiery stove with a wok, flames underneath",
    "gen_viet_3": "a Vietnamese restaurant front with a red lantern",
    # customers (chibi animals)
    "cust_cat": "a cute chibi black cat with white belly and tiny bell collar, sitting, front view",
    "cust_dog": "a cute chibi golden shiba-like puppy with floppy ears, sitting, front view",
    "cust_rabbit": "a cute chibi white fluffy bunny with pink inner ears, sitting, front view",
    "cust_panda": "a cute chibi chubby panda sitting and smiling, front view",
    "cust_fox": "a cute chibi orange fox with fluffy tail and cheek tufts, sitting, front view",
    "cust_frog": "a cute chibi small green frog with big round eyes, sitting, front view",
    "cust_pig": "a cute chibi pink piglet with round snout and rosy cheeks, sitting, front view",
    "cust_koala": "a cute chibi grey koala with big fluffy ears, sitting, front view",
}

def gen_one(key, model, prompt, out_file):
    body = json.dumps({
        "model": model,
        "messages": [{"role": "user", "content": prompt}],
        "modalities": ["image", "text"],
    }).encode()
    req = urllib.request.Request(
        "https://openrouter.ai/api/v1/chat/completions",
        data=body,
        headers={
            "Authorization": f"Bearer {key}",
            "Content-Type": "application/json",
        },
    )
    with urllib.request.urlopen(req, timeout=180) as resp:
        data = json.load(resp)
    images = data["choices"][0]["message"].get("images") or []
    if not images:
        raise RuntimeError(f"no image in response: {str(data)[:300]}")
    url = images[0]["image_url"]["url"]
    raw = base64.b64decode(url.split(",", 1)[1])
    out_file.write_bytes(raw)

def main():
    out_dir = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "art_out"
    model = sys.argv[2] if len(sys.argv) > 2 else "google/gemini-2.5-flash-image"
    only = sys.argv[3].split(",") if len(sys.argv) > 3 else None
    out_dir.mkdir(parents=True, exist_ok=True)
    key = load_key()
    if not key:
        sys.exit("no OPENROUTER_API_KEY")
    todo = {k: v for k, v in ASSETS.items() if only is None or k in only}
    for i, (name, subject) in enumerate(todo.items(), 1):
        out_file = out_dir / f"{name}.png"
        if out_file.exists():
            print(f"[{i}/{len(todo)}] skip {name} (exists)")
            continue
        prompt = f"{STYLE}, {subject}{ISOLATED}"
        for attempt in range(3):
            try:
                gen_one(key, model, prompt, out_file)
                print(f"[{i}/{len(todo)}] ok {name} ({out_file.stat().st_size // 1024}KB)")
                break
            except Exception as e:
                print(f"[{i}/{len(todo)}] attempt {attempt + 1} fail {name}: {e}")
                time.sleep(3 * (attempt + 1))
        else:
            print(f"[{i}/{len(todo)}] FAILED {name}")
        time.sleep(1)

if __name__ == "__main__":
    main()
