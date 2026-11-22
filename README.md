# Bếp Ngon Merge 🍒

**English** · [Tiếng Việt](README.vi.md)

A **Merge Mansion / Travel Town** style merge game for Android — tap generators
to spawn ingredients, drag two matching items together to merge them up a tier,
serve cute customers their orders, level up to unlock the board, earn coins for
upgrades and cafe decor. Comes with a Suika-style fruit-drop minigame for bonus
energy.

![Kotlin](https://img.shields.io/badge/Kotlin-2.2.21-7F52FF?logo=kotlin)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202025.11.01-4285F4?logo=jetpackcompose)
![minSdk](https://img.shields.io/badge/minSdk-26-brightgreen)
![targetSdk](https://img.shields.io/badge/targetSdk-36-blue)

## Gameplay

- **5×7 board** — the bottom 12 cells are webbed shut and unlock **one per level**
  (Lv2–Lv13). Every locked cell hides a **frozen high-tier item** — leveling up
  always reveals a prize. A few frozen items also scatter randomly across the
  upper board on a fresh run.
- **4 merge chains**: Fruit (11 tiers), Bakery (10), Drinks (8), Vietnamese
  dishes (7) — plus a short 3-tier **rare chain** per family (generators spawn
  them ~3% of the time, orders occasionally ask for them at premium prices).
- **Generators** — tap to spawn an item (costs 1⚡), recharge when depleted.
  Long-press to view the spawn table or **upgrade with coins** (pricey, for balance).
- **Orders** — customers request dishes at an adaptive pace (faster if you
  complete orders quickly). Requested tiers scale with level; a freshly unlocked
  family is only asked for low tiers at first. Later families pay more
  (bakery +10%, drink +25%, viet +40%). Items you already own get a **green
  ring**, delivered items show a **✓**.
- **Drag & drop**: mismatched items **swap places**, matching items merge,
  frozen items need an exact match to unfreeze, drag to the bag to store, drag
  onto an order card to deliver.
- **Energy** — 1⚡ per generator tap, regenerates +1/60s (cap 100), +10 from
  daily rewards, plus the minigame.
- **Meta**: cafe room with decor buffs, pets (feed with coins → buffs),
  achievements, daily rewards, magic wand item duplicator, QTE gift bubble.

### Minigame: Fruit Drop 🍉

Suika-style: drop fruits into a box, identical fruits merge into bigger ones,
overflow and it's game over. Physics powered by **JBox2D**. Unlimited plays;
reward is `min(20⚡, score/10)` — 10 points = 1⚡.

## Tech stack

- Kotlin + Jetpack Compose (Material3), MVVM
- AGP 8.13.2, Compose BOM 2025.11.01, minSdk 26, target/compileSdk 36
- Save games: DataStore Preferences + kotlinx.serialization
- JBox2D for the minigame physics
- Game logic is pure Kotlin in `game/` — covered by plain JUnit4 tests

## Build & test

```sh
./gradlew.bat assembleDebug        # -> app/build/outputs/apk/debug/app-debug.apk
./gradlew.bat assembleRelease      # signed release APK
./gradlew.bat testDebugUnitTest    # 100+ engine unit tests
```

Release builds sign with `keystore/bepngon.jks` (gitignored), credentials read
from `local.properties`. Without them the build still works but produces an
unsigned APK.

## Project layout

```
app/src/main/java/com/bepngon/merge/
├── game/            # pure-Kotlin logic: (state, args) -> (state, events)
│   ├── GameEngine.kt    # merge/move, orders, level/XP, inventory, buffs, cheat
│   ├── GameModels.kt    # GameState, BoardItem, Order, GameEvent
│   ├── ItemCatalog.kt   # every merge chain + generator + rare chain
│   ├── MetaCatalog.kt   # decor, pets, achievements, daily rewards
│   └── SubakEngine.kt   # fruit-drop minigame physics (JBox2D)
├── ui/              # GameScreen, GameViewModel, components/
├── data/            # SaveManager (save/load + legacy layout migration)
└── sound/           # SoundManager
```

Convention for new content: adding a merge chain is one `chain()` + one
`generator()` entry in `ItemCatalog` — no engine changes needed. See `AGENTS.md`
for the full dev workflow.

## Art assets

Item/generator/customer art lives in `app/src/main/res/drawable-nodpi/`, file
name = defId (`fruit_4.png`, `gen_bakery_2.png`, `rare_viet_1.png`,
`cust_koala.png`). `GameArt` resolves drawables by name and falls back to emoji.
Generation prompts: `art-prompts.md`. AI-sheet slicer: `tools/slice_sheet.py`.

## Debug

DEBUG builds: tap the **Lv.xx** badge 10 times → jump to Lv30, all cells
unlocked, +99,999🪙, full energy/inventory (silent, no UI). Reset run: ↻ top
right.
