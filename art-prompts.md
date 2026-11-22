# 🎨 Bếp Ngon Merge — AI Art Prompt Pack

Phong cách mục tiêu: kết hợp 2 ref — item cute cartoon vẽ tay như merge game (ref 1)
+ UI gỗ/lông thú warm cartoon như Viking GUI Pack (ref 2).

## Style prefix — DÁN VÀO ĐẦU MỌI PROMPT

```
Cute cartoon game asset, hand-drawn style, thick dark-brown outlines,
soft cel shading with subtle highlights, warm pastel color palette,
kawaii chibi proportions, clean vector look, cozy bakery/cafe theme,
mobile game art, high quality, no text, no watermark, no letters
```

Với item lẻ (food/gen/customer) thêm đuôi:
```
single object centered, isolated on plain solid white background
```

> 💡 Nếu tool gen hỗ trợ transparent background (Gemini/DALL·E) thì thay
> "isolated on plain solid white background" bằng "isolated, transparent background".

---

## 1. Board items — PNG 256×256, nền trong suốt

### Fruit chain (`fruit_1` → `fruit_11`)
```
fruit_1   → a pair of red cherries with green stem and leaf
fruit_2   → a fresh red strawberry with green leaves and tiny seeds
fruit_3   → a round orange fruit with a small green leaf
fruit_4   → a shiny red apple with a brown stem
fruit_5   → a bunch of purple grapes on a small vine
fruit_6   → a slice of watermelon with red flesh and green rind
fruit_7   → a spiky yellow pineapple with green crown leaves
fruit_8   → a wooden plate of mixed fruit salad, colorful chunks
fruit_9   → a tall glass of tropical fruit smoothie with a straw and tiny umbrella
fruit_10  → a decorated fruit cake with cherries and strawberries on top
fruit_11  → a luxurious golden VIP cake with fruits, cream and a little crown
```

### Bakery chain (`bakery_1` → `bakery_10`)
```
bakery_1  → a small bundle of golden wheat stalks tied with string
bakery_2  → a loaf of white bread, slightly sliced
bakery_3  → a golden flaky croissant
bakery_4  → a round chocolate chip cookie
bakery_5  → a pink frosted donut with colorful sprinkles
bakery_6  → a cute cupcake with swirled cream frosting and a cherry
bakery_7  → a stack of pancakes with dripping honey and a butter cube
bakery_8  → a cream cake slice with strawberries, bakery style
bakery_9  → a birthday cake with lit candles and cream roses
bakery_10 → a royal golden crown-shaped cake with jewels and frosting
```

### Drink chain (`drink_1` → `drink_8`)
```
drink_1   → a clear glass of pure water with a tiny droplet sparkle
drink_2   → a small cup of hot green tea with steam swirls
drink_3   → a bottle of fresh milk with a blue cap
drink_4   → a cup of brown coffee with latte art heart, steam
drink_5   → a pearl milk tea / boba tea cup with black pearls and straw
drink_6   → a glass of fresh orange juice with a slice on the rim
drink_7   → a colorful tropical mocktail with ice, mint and umbrella
drink_8   → a fancy cocktail glass with layered colors, party drink, confetti
```

### Viet chain (`viet_1` → `viet_7`)
```
viet_1    → a small bowl of steamed white rice with chopsticks
viet_2    → a Vietnamese banh mi sandwich with herbs and meat filling
viet_3    → a steaming bowl of pho noodle soup with beef and herbs
viet_4    → fresh spring rolls (goi cuon) with shrimp and herbs, peanut sauce
viet_5    → a bowl of bun cha with grilled pork and vermicelli
viet_6    → a traditional Vietnamese family meal tray with many small dishes
viet_7    → a grand VIP feast table with full dishes and golden decor
```

### Generators (`gen_*`) — nên trông "to hơn/đặc biệt hơn" item thường
```
gen_fruit_1   → a woven wicker basket full of mixed fruits
gen_fruit_2   → a wooden fruit cart with wheels and a small canopy
gen_fruit_3   → a cute delivery truck loaded with fruit crates
gen_bakery_1  → a cardboard box filled with bread and pastries
gen_bakery_2  → a small cozy bakery shop front with a striped awning
gen_bakery_3  → a whimsical cake factory with a chimney and pipes
gen_drink_1   → a small drink blender/juicer machine with a cup
gen_drink_2   → a cute mini fridge with a glass door full of drinks
gen_drink_3   → a wooden bar counter with bottles and shakers
gen_viet_1    → a small gas stove with a pot, simple kitchen burner
gen_viet_2    → a big fiery stove with a wok, flames underneath
gen_viet_3    → a Vietnamese restaurant front with a red lantern
```

---

## 2. Customers — PNG 256×256, chibi animal, nền trong suốt

Style: `cute chibi animal character, round face, big eyes, sitting pose, front view`

```
cat     → a black cat with white belly, tiny bell collar          (Mèo Mun)
dog     → a golden shiba-like puppy, floppy ears                  (Cún Vàng)
rabbit  → a white fluffy bunny with pink inner ears               (Thỏ Bông)
panda   → a chubby panda sitting and smiling                      (Gấu Trúc)
fox     → an orange fox with fluffy tail and cheek tufts          (Cáo Cam)
frog    → a small green frog with big round eyes                  (Ếch Con)
pig     → a pink piglet with round snout, rosy cheeks             (Heo Hồng)
koala   → a grey koala with big fluffy ears                       (Koala)
```

Bonus (cho đẹp, có thể dùng làm mascot trên title):
```
mascot  → a cute chibi chef girl with orange-brown hair, small chef hat,
          holding a wooden spoon, warm smile, bakery outfit
```

---

## 3. UI kit — theo phong cách Viking GUI Pack (gỗ + lông + bo tròn)

### Panels & khung
```
ui_panel      → a wooden plank panel frame, horizontal boards, carved edges,
                fur/leather trim on top, warm brown color, game UI panel,
                rounded corners, empty center
ui_dialog     → a rounded wooden sign board hanging on ropes, carved frame,
                blank cream parchment center, warm cartoon style
ui_cell       → a single rounded square tile for a match board, cream
                parchment texture, subtle inner shadow, thin brown border,
                empty center
ui_ordercard  → a small rounded card frame for an order ticket, paper
                parchment with a pinned corner, warm cream color
```

### Buttons
```
ui_btn_green  → a chunky rounded wooden button painted green, cartoon
                glossy highlight, thick outline, game UI button, blank
ui_btn_gold   → same style but golden/yellow
ui_btn_red    → same style but warm red/orange
ui_btn_round  → a small round wooden icon button, blank center for emoji
```

### Icons (nhỏ, trong suốt)
```
icon_coin     → a shiny golden coin with a star engraving
icon_energy   → a cartoon lightning bolt, yellow with orange outline
icon_star     → a puffy golden star with a smile, kawaii
icon_xp       → a small blue-purple experience gem crystal
icon_bag      → a cute small backpack/pouch, brown leather with straps
icon_gift     → a wrapped gift box with red ribbon bow
icon_trophy   → a golden trophy cup with handles, cartoon
icon_question → a rounded wooden help button with a question mark carved
icon_snow     → a blue snowflake, icy crystal, cartoon
icon_web      → a cute spiderweb, thin white strands, corner web
```

### Overlays & effects
```
fx_frozen     → a transparent ice cube block covering a square tile, frosty
                blue tint, frozen glass with small cracks and snow corners,
                see-through center
fx_webtile    → a tile covered by a cute white spiderweb, thin silky strands,
                slight transparency
fx_sparkle    → a burst of cartoon sparkles, stars and glow, golden
fx_coinfly    → a small flying golden coin with motion sparkle trail
```

---

## 4. Backgrounds — 1080×1920 (dọc)

```
bg_main       → a cozy warm bakery cafe interior, soft cream walls, wooden
                counter at top, shelves with bread and plants, warm sunlight
                from a window, very soft and slightly blurred so UI pops,
                no characters, no text, vertical phone wallpaper

bg_topbar     → a horizontal strip of warm wooden planks with a cream paper
                strip, soft cartoon texture, 1080×300
```

---

## 5. Tips khi gen

- **Consistency**: luôn dán đúng STYLE PREFIX. Nếu tool có "style reference"
  (Midjourney `--sref`, Imagen image-guide) thì dùng ref ảnh 1 cho items,
  ref ảnh 2 cho UI.
- **Sprite sheet** (nhanh hơn nhiều nếu tool gen tốt grid):
  thêm vào prompt `"a 4x3 grid sprite sheet of different cute cartoon food
  icons, each icon in its own cell, even spacing, same art style"` rồi cắt.
- **Naming**: lưu file đúng theo `defId` (vd `fruit_4.png`, `gen_bakery_2.png`,
  `cat.png`) — bỏ vào `app/src/main/res/drawable-nodpi/` là mình wire vào
  code thay emoji ngay.
- **Nếu AI hay thêm chữ**: thêm `no text, no words, no letters` vào prompt,
  hoặc negative prompt `text, watermark, signature, blurry, realistic`.

---

## 6. Combo fire animation 🔥

### Option A — sprite sheet (animation thật, khuyên dùng)

Gemini gen 1 ảnh nhiều frame, mình cắt từng frame bằng `tools/slice_sheet.py`:

```
Cute cartoon fire flame animation sprite sheet for a mobile merge game,
6 frames arranged in a single horizontal row, evenly spaced, each frame
shows THE SAME flame character at a different flicker pose
(frame 1 tall stretched flame, frame 2 leaning left, frame 3 short round
flame, frame 4 leaning right, frame 5 tall again with small sparks,
frame 6 mid flicker), kawaii hand-drawn style, thick dark-brown outlines,
soft cel shading, warm orange core fading to red-orange tips with a
bright yellow inner glow, tiny cartoon sparkle accents, consistent size
and baseline across all frames, white background, no text, no watermark,
no letters, mobile game art
```

→ Lưu `fx_flame_sheet.png` bỏ vào `art_out/`, mình sẽ cắt 6 frame → `fx_flame_0..5.png` và wire vào `ComboFlames` (cycle frames theo thời gian).

### Option B — flame đơn (đơn giản, thay emoji luôn được)

```
Cute cartoon fire flame game asset, a single kawaii flame character,
hand-drawn style, thick dark-brown outlines, soft cel shading, warm
bright yellow core fading to vivid orange and soft red tips, subtle
inner glow, a few tiny floating spark dots, cozy cartoon look matching
a bakery game, mobile game art, high quality, white background,
no text, no watermark
```

---

## 7. Tiệm của bé — phòng, thú cưng, mini game 🏠

### `bg_cafe` — nền phòng (1080×1400+, vùng phòng trong RoomScreen)

Nền phòng trống để decor đặt lên — cần sàn rộng, tường đơn giản:

```
Cute cartoon empty cafe room interior for a mobile game, warm cream
walls with a subtle wainscot line, light wooden plank floor taking the
bottom third, soft warm daylight from an unseen window, a few very
subtle wall details only (tiny shelf shadow lines), cozy pastel tones,
hand-drawn style, thick outlines kept minimal, slightly desaturated so
furniture placed on top will pop, mobile game background, vertical
phone wallpaper, no characters, no furniture in the middle of the room,
no text, no watermark
```

→ Lưu `bg_cafe.png` → `drawable-nodpi/` — `RoomScreen` tự load (lookup `bg_cafe`) thay gradient.

### Pet sprites — nâng cấp (hiện tái dùng `cust_*.png`)

Các `cust_cat/dog/...` vốn là chibi đứng làm khách — cho phòng nên gen bản
"pet" nhỏ hơn, pose ngồi/đi, đuôi vẫy. Nếu muốn gen mới:

```
pet_cat    → a tiny chibi black cat sitting and wagging its tail,
             curious eyes, pet-size proportions (same style as the
             customer avatars but smaller and cuter)
pet_dog    → a tiny chibi golden puppy sitting with tongue out
pet_rabbit → a tiny chibi cream bunny with long ears, holding a carrot
pet_frog   → a tiny chibi green frog sitting cross-legged, happy
pet_pig    → a tiny chibi pink piglet with a curly tail
pet_fox    → a tiny chibi orange fox with a fluffy tail
pet_koala  → a tiny chibi koala hugging a eucalyptus leaf
pet_panda  → a tiny chibi panda sitting and munching bamboo
```

→ Lưu `pet_*.png` — mình sẽ đổi `PetSprite` dùng `pet_$id` với fallback `cust_$id`.
Bonus nếu muốn walk cycle: gen sprite sheet 4 frame `pet_<id>_walk_sheet.png`
(4 poses: stand → step left → stand → step right) như kiểu flame ở §6,
mình cắt + animate khi thú đi lại.

### `bg_mini` — nền mini game thả trái (optional)

```
a cute wooden barrel or woven basket container seen from the front,
empty inside, warm cream background with subtle fruit doodles in the
corners, cozy cartoon style, vertical phone wallpaper, no text
```

→ `bg_mini.png` — thay nền phẳng + viền nâu hiện tại của hộp chứa trái.

> 💡 Option B của §6: lưu `fx_flame.png` vào `app/src/main/res/drawable-nodpi/` — mình thay thế emoji 🔥 trong `ComboFlames` (giữ animation flicker scale/alpha hiện có).

### Rare side-chains (`rare_*`) — item hiếm, nên trông "premium/lấp lánh" hơn item thường

Cùng style prefix ở đầu file; thêm cụm `premium golden sparkle aura, subtle
shine effects, looks rare and luxurious` vào cuối prompt để phân biệt với
bản thường. Kích thước 256×256, nền trong suốt.

### Rare fruit (`rare_fruit_1` → `rare_fruit_3`)
```
rare_fruit_1  → a ripe golden mango with glossy skin and a small green leaf
rare_fruit_2  → a heavenly pink peach with a soft glow and delicate leaf
rare_fruit_3  → a luxurious Japanese crown melon with netted skin and a tiny crown
```

### Rare bakery (`rare_bakery_1` → `rare_bakery_3`)
```
rare_bakery_1 → a glossy egg tart with caramelized top in a fluted cup
rare_bakery_2 → a golden belgian waffle with dripping honey and butter
rare_bakery_3 → an elegant caramel flan pudding on a small plate, glossy top
```

### Rare drink (`rare_drink_1` → `rare_drink_3`)
```
rare_drink_1  → a matcha green tea in a traditional cup with whisk, soft foam
rare_drink_2  → a glass of ruby sangria with floating fruit slices
rare_drink_3  → a champagne bottle with a popping cork and golden bubbles
```

### Rare viet (`rare_viet_1` → `rare_viet_3`)
```
rare_viet_1   → a bowl of spicy red-chili instant noodles with chili flakes and steam
rare_viet_2   → a grilled jumbo prawn on a skewer, char marks, herbs
rare_viet_3   → a whole garlic-butter lobster on a plate, glossy red shell
```
