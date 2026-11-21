# Bếp Ngon Merge

Merge game (kiểu Merge Mansion / Travel Town) cho Android: board 7x9, tap generator để spawn item, kéo 2 item giống nhau để merge lên tier cao hơn, kéo xuống sell zone để bán lấy coins. Theme trái cây + đồ ăn, phong cách cute.

## Stack

- Kotlin + Jetpack Compose (Material3), MVVM
- AGP 8.13.2, Kotlin 2.2.21, Compose BOM 2025.11.01, minSdk 26, target/compileSdk 36
- Save game: DataStore Preferences + kotlinx.serialization (`data/SaveManager.kt`)
- Game logic thuần Kotlin tách riêng trong `game/` (`ItemCatalog`, `GameEngine`, `GameModels`) — có unit test ở `app/src/test/`

## Commands

```sh
./gradlew.bat assembleDebug          # build APK -> app/build/outputs/apk/debug/app-debug.apk
./gradlew.bat testDebugUnitTest      # chạy unit test engine
```

## Emulator (quan trọng)

- AVD tốt: `merge_dev35` (Android 15, image `system-images;android-35;google_apis_playstore;x86_64`, RAM 4G, 6 cores). **ĐỪNG dùng `merge_dev`** — image android-36.1 bị bug package manager (app install xong nhưng `am start` báo "does not exist") + gfxstream hang boot.
- Launch: `emulator -avd merge_dev35 -gpu angle_indirect -memory 4096 -cores 6`
  - `-gpu host` (gfxstream/NVIDIA) làm guest kẹt ở boot; `-gpu guest` boot được nhưng không render. `angle_indirect` là chạy ngon.
- adb shell path bị Git Bash convert (`/sdcard/x` -> `C:/Program Files/Git/...`) — thêm `MSYS_NO_PATHCONV=1` hoặc dùng `adb exec-out screencap -p > file.png` để chụp màn hình.
- Emulator console screenshot: TCP `127.0.0.1:5554`, `auth <token>` trong `~/.emulator_console_auth_token`, lệnh `screenrecord screenshot <path>`.

## Debug gameplay bằng adb

```sh
adb shell input tap <x> <y>              # tap generator
adb shell input swipe x1 y1 x2 y2 1500   # kéo item merge/bán
adb exec-out screencap -p > shot.png     # chụp màn hình
```

## Code conventions

- Merge chains định nghĩa trong `game/ItemCatalog.kt` (`chain()` + `generator()` helpers) — thêm family mới = thêm 1 chain + 1 generator chain, không sửa engine.
- `GameEngine` là pure functions: `(GameState, args) -> Pair<GameState, List<GameEvent>>`; UI animate dựa trên `GameEvent` qua SharedFlow.
- Callbacks truyền vào `pointerInput` phải đi qua `rememberUpdatedState` — pointerInput coroutine capture lambda cũ, không tự refresh.
- Tọa độ drag/drop tính trong window coords qua `boundsInWindow()` của từng cell. `PointerInputChange.position` trong `detectDragGestures.onDrag` là LOCAL coords của tile — phải cộng `bounds.topLeft` trước khi hit-test.
- Orders/XP/inventory sống trong `GameEngine` (order spawn theo timer trong ViewModel, reward level-up map ở `rewardForLevel`). Drag phân biệt nguồn bằng `ItemSource` (BOARD/INVENTORY) trong `DragState`.
