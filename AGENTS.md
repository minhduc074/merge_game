# Bếp Ngon Merge

Merge game (kiểu Merge Mansion / Travel Town) cho Android: board 5x7, tap generator để spawn item, kéo 2 item giống nhau để merge lên tier cao hơn, kéo xuống sell zone để bán lấy coins. Theme trái cây + đồ ăn, phong cách cute.

## Stack

- Kotlin + Jetpack Compose (Material3), MVVM
- AGP 8.13.2, Kotlin 2.2.21, Compose BOM 2025.11.01, minSdk 26, target/compileSdk 36
- Save game: DataStore Preferences + kotlinx.serialization (`data/SaveManager.kt`)
- Game logic thuần Kotlin tách riêng trong `game/` (`ItemCatalog`, `GameEngine`, `GameModels`) — có unit test ở `app/src/test/`

## Commands

```sh
./gradlew.bat assembleDebug          # build APK -> app/build/outputs/apk/debug/app-debug.apk
./gradlew.bat assembleRelease        # release signed APK -> app/build/outputs/apk/release/app-release.apk
./gradlew.bat testDebugUnitTest      # chạy unit test engine
```

Release: ký bằng `keystore/bepngon.jks` (gitignored), credentials đọc từ `local.properties`
(`RELEASE_STORE_FILE/RELEASE_STORE_PASSWORD/RELEASE_KEY_ALIAS/RELEASE_KEY_PASSWORD`). Nếu thiếu
keys đó, release build vẫn chạy nhưng APK không được ký (unsigned). minify R8 + shrinkResources bật —
proguard rules giữ kotlinx.serialization serializers.

## Emulator (quan trọng)

- AVD tốt: `merge_dev35` (Android 15, image `system-images;android-35;google_apis_playstore;x86_64`, RAM 4G, 6 cores). **ĐỪNG dùng `merge_dev`** — image android-36.1 bị bug package manager (app install xong nhưng `am start` báo "does not exist") + gfxstream hang boot.
- Launch: `emulator -avd merge_dev35 -gpu angle_indirect -memory 4096 -cores 6`
  - `-gpu host` (gfxstream/NVIDIA) làm guest kẹt ở boot; `-gpu guest` boot được nhưng không render. `angle_indirect` là chạy ngon.
- adb shell path bị Git Bash convert (`/sdcard/x` -> `C:/Program Files/Git/...`) — thêm `MSYS_NO_PATHCONV=1` hoặc dùng `adb exec-out screencap -p > file.png` để chụp màn hình.
- Emulator console screenshot: TCP `127.0.0.1:5554`, `auth <token>` trong `~/.emulator_console_auth_token`, lệnh `screenrecord screenshot <path>`.

## Debug gameplay

- Cheat: bấm badge `Lv.xx` trên TopBar 10 lần → `GameEngine.debugMaxOut` (Lv30, mở hết ô, +99,999🪙,
  full energy/kho/onboarding). Đếm lặng, không hiện UI — đủ 10 tự kích hoạt.
  Chỉ hoạt động trong build DEBUG (`BuildConfig.DEBUG`).
- Minigame thả trái: không giới hạn lượt; thưởng = `min(20, score/10)` ⚡ (`finishMiniGame`).

### Quy trình sau mỗi lần sửa (user yêu cầu)

1. `./gradlew.bat :app:assembleDebug` → `adb install -r app/build/outputs/apk/debug/app-debug.apk`
2. `adb shell monkey -p com.bepngon.merge -c android.intent.category.LAUNCHER 1` → `adb exec-out screencap -p > shot.png` để verify bằng mắt.
3. Hiệu ứng khó trigger (combo x3, lên cấp...) → tạm patch điều kiện hiển thị, chụp màn hình, rồi revert + build lại bản sạch trước khi báo xong.
4. Xoá file screenshot/preview tạm sau khi verify xong.
5. Sau mỗi lần fix xong + verify OK thì commit luôn (user yêu cầu).

### adb

```sh
adb shell input tap <x> <y>              # tap generator
adb shell input swipe x1 y1 x2 y2 1500   # kéo item merge/bán
adb exec-out screencap -p > shot.png     # chụp màn hình
```

## Decor editor

- `game/DecorModels.kt` + `DecorCatalog.kt` (DSL giống ItemCatalog: thêm đồ = 1 `item()` + `V()` variants) + `DecorEngine.kt` (pure, mọi op đi qua `GameState.decor`) — test `DecorEngineTest.kt`.
- Mở từ chip "🛋️ Trang trí" trong `RoomScreen` → flag `showDecor` → `ui/components/DecorScreen.kt`.
- Phòng = grid `RoomDef.cols`×`rows`, `wallRows` đầu là vùng treo tường (WALL item). Va chạy chỉ trong cùng `DecorLayer` → thảm nằm dưới bàn OK. Paint = `Placement.PAINT`, mua là phủ cả mặt (`wallPaint`/`floorPaint`, màu = `variant.tint`).
- **View isometric** (kiểu góc phòng 2 tường + sàn hình thoi): `IsoMap` trong DecorScreen ánh xạ grid ↔ toạ độ theo landmarks (fractions trong `Iso` object, đo tay từ ảnh gốc). Sàn: gx theo trục `ex` (xuống-phải), hàng gy−wallRows theo `ey` (xuống-trái). Tường: `rot` mã hoá phía — `rot=0` = tường phải (+x), `rot=90` = tường trái (+y); gx = ô dọc tường, gy = hàng theo chiều cao. `snapToCell` trả (gx,gy,rot) — đồ tường tự chọn phía theo con trỏ. `canPlace` skip overlap khi khác phía tường.
- Variant `priceCoins = 0` = starter (luôn dùng được); còn lại vào `ownedVariants` khi mua — `placeItem`/`setVariant`/`setPaint` mua+áp dụng atomic (trừ coin 1 lần).
- Art = sprite `decor_<itemId>.png` trong drawable-nodpi render qua `DecorArt` (DecorScreen) + recolor variant bằng `ColorFilter.tint(tint, BlendMode.Modulate)` (API-safe minSdk 26 — `BlendMode.Color` cần API 29); fallback emoji khi thiếu PNG.
- "Xoay" có 2 nghĩa theo `DecorItem.topDown`: item nhìn từ trên (thảm) → xoay bitmap 90° + đổi footprint; item oblique (nội thất) → lật ngang `flip` (sprite nghiêng xoay 90° nhìn rất kì). `footprint()` bỏ qua `rot` cho item không topDown.
- Nền phòng = `RoomShell` **procedural** (Canvas): trời vẽ gradient + mây/trăng/sao (`drawSky`, type lấy từ `tex` của item trời), tường/sàn = fill tint + tile pattern `wtex_*`/`ftex_*` (line-art trong suốt, `tools/gen_patterns.py`) clip theo quad + wainscot band khi `DecorItem.panel`, viền nâu + váy độ dày sàn. Variant paint quyết định cả pattern (qua `item.tex`) lẫn màu — `DecorItem.tex`/`panel` chỉ dùng cho `Placement.PAINT`. Tab "Phòng" = `RoomStyleTray`: pattern tile + hàng chấm màu của pattern đang dùng (đổi pattern giữ suffix màu). `paint_wall`/`paint_floor` cũ giữ ở category `legacy` (không hiện tray) để save cũ resolve được — render ra tường/sàn trơn đúng như trước.
- Undo/redo = snapshot `RoomSave` trong `GameViewModel` (session-only, 50 bước; undo KHÔNG hoàn coin đã mua variant — ownership giữ lại).
- Save trong `SaveData.decor`; `RoomSave.version` riêng để migrate (version lạ → reset phòng đó về trống).
- Phase sau chưa có: surface decor trên bàn (`hostUid`), brief/rating thưởng, sell, camera zoom/pan, room thứ 2.

## Code conventions

- Merge chains định nghĩa trong `game/ItemCatalog.kt` (`chain()` + `generator()` helpers) — thêm family mới = thêm 1 chain + 1 generator chain, không sửa engine. Chuỗi `rare_*` (3 tiers) tự động gắn `isRare` — gen spawn với weight 3, orders hỏi với `RARE_ORDER_WEIGHT`.
- `GameEngine` là pure functions: `(GameState, args) -> Pair<GameState, List<GameEvent>>`; UI animate dựa trên `GameEvent` qua SharedFlow.
- Callbacks truyền vào `pointerInput` phải đi qua `rememberUpdatedState` — pointerInput coroutine capture lambda cũ, không tự refresh.
- Tọa độ drag/drop tính trong window coords qua `boundsInWindow()` của từng cell. `PointerInputChange.position` trong `detectDragGestures.onDrag` là LOCAL coords của tile — phải cộng `bounds.topLeft` trước khi hit-test.
- Orders/XP/inventory sống trong `GameEngine` (order spawn theo timer trong ViewModel, reward level-up map ở `rewardForLevel`). Drag phân biệt nguồn bằng `ItemSource` (BOARD/INVENTORY) trong `DragState`.

## Art assets

- Art thật nằm trong `app/src/main/res/drawable-nodpi/`, tên file = `defId` (`fruit_4.png`, `gen_bakery_2.png`), customer = `cust_<id>`, icon/UI = `icon_*`/`ui_*`/`fx_*`.
- Render qua `ui/components/GameArt.kt` — lookup drawable theo tên (cache `getIdentifier`), fallback emoji nếu thiếu file. Muốn đổi art cho item chỉ cần thay PNG cùng tên.
- Decor editor: backdrop vườn của sky `"garden"` = `bg_garden.png` (cover-fit toàn canvas, phòng iso vẽ đè lên — giữ vùng giữa ảnh thoáng). Thiếu file thì fallback cảnh vẽ code `drawGarden`. Texture tường/sàn = `wtex_*`/`ftex_*`.
- Source sheet gốc (AI gen, 1696×2528): `art_out/Gemini_Generated_Image_*.png`. Script cắt: `python tools/slice_sheet.py` → `art_out/slices/` (gitignored, đã copy vào drawable-nodpi). Prompt pack: `art-prompts.md`.
- Minigame "Thả trái": physics = jbox2d (`game/SubakEngine.kt`, thuần JVM → unit test ở `SubakEngineTest.kt`), art = `subak_*.png` render từ SVG (resvg, convert hsl()→hex trước khi rasterize).
- Slicer: band y-range hardcode theo layout sheet (divider line full-width làm row-projection merge), cột dùng gap detection hoặc even-split snap về cột ít pixel nhất, nền trắng xóa bằng flood-fill từ mép (giữ trắng nội bộ như chai sữa).
- Decor sprites (`decor_*.png`): gen 1 item/prompt qua Gemini web trong Edge — `python -u tools/gen_decor_browser.py` (UI automation: paste prompt → chờ ảnh → click ảnh mở Edge image viewer → ⬇ full-size ≈ 50s → `~/Downloads`), rồi `python tools/process_decor.py` (nền trắng→alpha, trim, 512px). Raw gốc: `art_out/decor_raw/` (gitignored). Lưu ý: OpenRouter key trong `.env` hết hạn → gen_art.py (API) không chạy được; download lag 1 nhịp nếu detect trượt → luôn check contact sheet sau batch.
- Bộ sprite furniture cung cấp sẵn (5 sheet JPG/PNG: `furniture_1`, `furniture_2_walls`, `things_1`, `things_2_people`, `people` — gốc ở `art_out/furniture_pack/`): `python tools/slice_furniture.py` → `art_out/furniture_pack/slices/` (366 sprite, flood-fill nền kem + connected components; gitignored). Map slice→catalog id trong `tools/import_furniture.py` (dict `MAP`) rồi chạy để copy vào drawable-nodpi. Thêm đồ mới = thêm `item()` trong DecorCatalog + 1 dòng MAP.
