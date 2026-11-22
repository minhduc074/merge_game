# Decor design spec — "Tiệm của bé"

Mọi thứ cần biết để tự design vị trí/sprite decor.

## Code liên quan

| File | Vai trò |
|---|---|
| `game/MetaCatalog.kt` → `DecorDef` | Data: `id, name, emoji, cost, unlockLevel, buffs, x, y, w` |
| `ui/components/RoomScreen.kt` | Render phòng: bg + decor (sắp xếp theo độ sâu) + pet |
| `res/drawable-nodpi/decor_<id>.png` | Sprite trong suốt, tên file = `id` |
| `res/drawable-nodpi/bg_cafe.png` | Nền phòng (1080×1935) |
| `art_out/furnished_room.png` | Ảnh phòng đầy đồ do AI đặt — nguồn vị trí hiện tại |

## Hệ tọa độ

`x, y, w` là **fraction của room box** (vùng `BoxWithConstraints` trong RoomScreen — full width, phần còn lại của màn hình sau top bar + action bar; tỉ lệ khung ≈ **0.515** (rộng/cao)).

- `x, y` = góc **trên-trái** của sprite (0.0–1.0; âm = lẹm ra ngoài khung, bị clip bo góc — dùng được cho đồ sát mép)
- `w` = **chiều rộng** của sprite tính theo fraction chiều rộng phòng
- Chiều cao tự suy từ aspect sprite: `h_frac = w × (h_img/w_img) × 0.515`
- Điểm đáy (quan trọng cho sort + đứng sàn): `bottom = y + h_frac`

## Render order (độ sâu)

`RoomScreen` vẽ:
1. `decor_rug` **luôn đầu tiên** (nền sàn, dưới mọi đồ — flat item khác thì thêm rule tương tự)
2. Còn lại sort theo `bottom` tăng dần → item đứng trước (bottom lớn) đè lên item đứng sau. Muốn đồ A đứng trước đồ B → cho `bottomA > bottomB`.

## bg_cafe + ContentScale.Crop

bg vẽ `Crop` — aspect bg 0.558 > room 0.515 → fit theo chiều cao, crop ~3.8% mỗi bên trái/phải:

- `x_room = (x_img − 0.0384) / 0.923`
- `y_room = y_img` (giữ nguyên)
- `w_room = w_img / 0.923`

Vùng sàn (wooden floor) bắt đầu ~ `y = 0.67` của bg/room.

## Vị trí hiện tại (từ furnished_room.png)

| id | x | y | w | vùng |
|---|---|---|---|---|
| decor_rug | 0.20 | 0.65 | 0.61 | sàn giữa |
| decor_plant | 0.15 | 0.56 | 0.17 | sàn, trước quầy |
| decor_painting | 0.34 | 0.26 | 0.16 | tường |
| decor_clock | 0.61 | 0.26 | 0.18 | tường |
| decor_lamp | 0.87 | 0.25 | 0.11 | tường phải |
| decor_curtain | -0.03 | 0.28 | 0.29 | tường trái, lẹm mép |
| decor_table | 0.50 | 0.60 | 0.31 | sàn |
| decor_shelf | 0.46 | 0.49 | 0.26 | chân tường |
| decor_flowers | 0.80 | 0.60 | 0.16 | sàn, trước lò |
| decor_counter | 0.00 | 0.42 | 0.49 | chân tường trái |
| decor_oven | 0.73 | 0.50 | 0.26 | chân tường phải |
| decor_piano | -0.04 | 0.63 | 0.24 | sàn, lẹm mép trái |
| decor_aquarium | 0.90 | 0.64 | 0.12 | sàn, sát mép phải |
| decor_goldcat | 0.36 | 0.58 | 0.15 | sàn, trước quầy/kệ |

## Quy tắc đặt đáy cho đồ mới

- Đồ tựa tường (quầy/kệ/lò): `bottom ≈ 0.70` (ngay mép sàn–tường)
- Đồ giữa phòng: `bottom ≈ 0.72–0.82` (càng xa tường càng xuống thấp)
- Đồ treo tường: neo `y` = mép trên khung tranh/đồng hồ
- Kiểm chồng x-overlap giữa các item cùng độ sâu — chồng được nhưng nhớ ai vẽ sau ai (bottom lớn hơn thắng)

## Sprite spec

- PNG **nền trong suốt**, tên file = `decor_<id>.png` → `drawable-nodpi/`
- Kích thước tự do (game scale theo `w`) — hiện dùng ~256px cạnh dài
- Style: cùng prefix trong `art-prompts.md`; gen sheet rồi cắt bằng pipeline flood-fill như các batch trước

## Workflow nhanh để redesign

1. Gen ảnh phòng furnished mới (upload `bg_cafe` + list đồ, hoặc chỉnh tay trên `furnished_room.png`)
2. Đo box từng item theo % ảnh → đổi sang room coords bằng công thức Crop ở trên
3. Floor items: `y = bottom_mong_muốn − 0.515·w·aspect`
4. Đưa ảnh/box cho Devin cũng được — nó tự tính + nhập `DecorDef`

## Buff fields

`orderCoinsPct` (+% vàng order), `genCooldownPct` (−% hồi gen), `energyRegenPct` (+% hồi ⚡) — tổng hợp qua `DecorCatalog.*(owned)`; `cost` và `unlockLevel` như cũ.
