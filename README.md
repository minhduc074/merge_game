# Bếp Ngon Merge 🍒

Merge game kiểu **Merge Mansion / Travel Town** cho Android — bấm generator để sinh
nguyên liệu, kéo 2 item giống nhau merge lên tier cao hơn, giao đơn hàng cho khách
cute, lên level mở khóa bàn chơi, kiếm vàng nâng cấp và trang trí quán. Kèm minigame
thả trái kiểu Suika kiếm thêm năng lượng.

![Kotlin](https://img.shields.io/badge/Kotlin-2.2.21-7F52FF?logo=kotlin)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202025.11.01-4285F4?logo=jetpackcompose)
![minSdk](https://img.shields.io/badge/minSdk-26-brightgreen)
![targetSdk](https://img.shields.io/badge/targetSdk-36-blue)

## Gameplay

- **Board 5×7** — 12 ô cuối bị khóa bằng mạng nhện, mở **1 ô mỗi level** (Lv2–Lv13).
  Mỗi ô khóa giấu sẵn **1 item bậc cao đóng băng** — lên level mở ô là reveal quà.
  Vài item đóng băng cũng rải ngẫu nhiên vùng trên lúc bắt đầu chơi.
- **4 chuỗi merge**: Trái cây (11 tiers), Bánh (10), Đồ uống (8), Món Việt (7)
  + chuỗi **rare** 3 tiers cho mỗi family (gen spawn ~3%, order đôi khi hỏi với giá cao).
- **Generators** — bấm để sinh item (tốn 1⚡/lần), hết lượt phải chờ hồi.
  Long-press để xem spawn table + **nâng cấp bằng vàng** (giá cao để cân bằng).
- **Orders** — khách gọi món theo adaptive pacing (order tới nhanh hơn nếu bạn
  hoàn thành nhanh). Tier được hỏi leo dần theo level; family vừa mở gen chỉ bị
  hỏi item thấp trước. Family unlock sau trả giá cao hơn (bakery +10%, drink +25%, viet +40%).
  Item nào đã có trên bàn được **khoanh xanh**, món đã giao hiện **✓**.
- **Kéo thả**: item khác loại **swap chỗ**, item giống merge, item đóng băng
  cần merge đúng loại để mở băng, kéo xuống túi để cất, kéo lên order để giao.
- **Năng lượng** — tốn 1⚡ mỗi lần gen, hồi +1/60s (trần 100), quà ngày +10,
  minigame cho thêm.
- **Meta**: quán cà phê trang trí (decor buff), thú cưng (cho ăn bằng vàng → buff),
  achievements, quà hàng ngày, magic wand nhân đôi item, QTE gift bubble.

### Minigame: Thả trái 🍉

Suika-like: thả trái vào khung, 2 trái cùng loại chạm nhau merge lên trái to hơn,
tràn khung là thua. Physics = **JBox2D** (port từ
[takempf/subak-game](https://github.com/takempf/subak-game), CC BY-NC 4.0).
Không giới hạn lượt chơi; thưởng `min(20⚡, điểm/10)` — 10 điểm = 1⚡.

## Tech stack

- Kotlin + Jetpack Compose (Material3), MVVM
- AGP 8.13.2, Compose BOM 2025.11.01, minSdk 26, target/compileSdk 36
- Save: DataStore Preferences + kotlinx.serialization
- JBox2D cho physics minigame
- Game logic thuần Kotlin trong `game/` — test được bằng unit test thường (JUnit4)

## Build & test

```sh
./gradlew.bat assembleDebug        # -> app/build/outputs/apk/debug/app-debug.apk
./gradlew.bat assembleRelease      # signed release APK
./gradlew.bat testDebugUnitTest    # 100+ unit test cho engine
```

Release ký bằng `keystore/bepngon.jks` (gitignored), credentials đọc từ
`local.properties`. Thiếu keys thì vẫn build được nhưng APK unsigned.

## Cấu trúc

```
app/src/main/java/com/bepngon/merge/
├── game/            # logic thuần Kotlin, pure functions (state, args) -> (state, events)
│   ├── GameEngine.kt    # merge/move, orders, level/XP, inventory, buffs, cheat
│   ├── GameModels.kt    # GameState, BoardItem, Order, GameEvent
│   ├── ItemCatalog.kt   # toàn bộ merge chains + generators + rare chains
│   ├── MetaCatalog.kt   # decor, pets, achievements, daily rewards
│   └── SubakEngine.kt   # physics minigame thả trái (JBox2D)
├── ui/              # GameScreen, GameViewModel, components/
├── data/            # SaveManager (save/load + migration layout cũ)
└── sound/           # SoundManager
```

Quy ước thêm content mới: thêm merge chain = thêm 1 `chain()` + `generator()`
trong `ItemCatalog` — không cần sửa engine. Chi tiết dev workflow xem `AGENTS.md`.

## Art assets

Ảnh item/generator/khách nằm ở `app/src/main/res/drawable-nodpi/`, tên file =
defId (`fruit_4.png`, `gen_bakery_2.png`, `rare_viet_1.png`, `cust_koala.png`).
`GameArt` tự load theo tên, fallback emoji khi thiếu file. Prompt gen ảnh:
`art-prompts.md`. Script cắt sheet AI-gen: `tools/slice_sheet.py`.

## Debug

Build DEBUG: bấm badge **Lv.xx** 10 lần → nhảy Lv30, mở hết ô, +99,999🪙,
full energy/inventory (đếm lặng, không hiện UI). Reset game: nút ↻ góc trên.

## License

Minigame "Thả trái" port từ [takempf/subak-game](https://github.com/takempf/subak-game)
(**CC BY-NC 4.0** — chỉ dùng phi thương mại).
