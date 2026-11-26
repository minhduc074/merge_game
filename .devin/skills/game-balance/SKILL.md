---
name: game-balance
description: Tune Bếp Ngon Merge economy/difficulty — spawn weights, timers, order rates, XP/coins, achievement goals. Use when the user asks to adjust tỉ lệ / cân bằng / reward / spawn / XP / giá.
---

# Game balance knobs

All tunables live in `app/src/main/java/com/bepngon/merge/game/`. Engine is pure
Kotlin — always run `./gradlew.bat :app:testDebugUnitTest` after tuning and update
tests that assert old numbers.

## `ItemCatalog.kt`
- Merge chains: `chain("fruit", Family.FRUIT, Triple(emoji, name, sellPrice to score), ...)`.
- Spawn tables: `generator("gen_x_N", tier, emoji, name, next, sellPrice, "item" to weight, ...)`.
  Convention: tier-1 weight luôn cao nhất, mọi bảng tổng = 100. **Gen tier N chỉ
  spawn item tier 1..N+1** — cao hơn là merge-only. Chain dài (fruit 11t, bakery
  10t) full cap: gen_1 `76/22`→t2, gen_2 `58/27/13`→t3, gen_3 `50/26/14/8`→t4.
  Chain ngắn (drink 8t, viet 7t) cap thấp hơn 1 tier: gen_1 `76/22`→t2,
  gen_2 `64/34`→t2 (nhiều t2 hơn gen_1), gen_3 `52/30/16`→t3. + `rare_*_1`
  weight 2 ở mọi gen. Higher gen = longer tail.
- `generator()` sets `maxCharges = 6 * tier` — scarcity knob, cắt số lượt miễn phí
  trước cooldown.

## `GameEngine.kt` constants
- `GEN_COOLDOWN_MS` 30s — depleted gen reset-to-full time.
- `GEN_REGEN_MS` 60s — partial gen +1 charge per interval (`BoardItem.rechargeAtMs`).
- `ENERGY_REGEN_BASE_MS` 60s, `ENERGY_CAP` 100 (regen ceiling only — daily/QTE/
  achievement/minigame rewards overflow past it), `ENERGY_PER_SPAWN` 1.
- `ORDER_INTERVAL_MIN/MAX_MS` 6s/28s — adaptive pacing = `orderPaceMs/4` ±40% jitter.
- `RARE_ORDER_WEIGHT` 1 — flat weight per rare item in `pickOrderItem` (normal items:
  base by tier `40/30/18/9/3` (t1/t2 decay theo level: `40-3L` sàn 5, `30-L` sàn 8)
  + `(tier-1)*(level/3)`).
- Order tier window `orderTierWindow(chainLen, level)` — áp cho CẢ chuỗi thường lẫn
  rare: min = `level * chainLen / ORDER_TIER_LEVEL_SPAN(30) + 1`, max = min +
  `ORDER_TIER_WINDOW(3)`. Window trượt đến đỉnh chuỗi rồi ghim ở đó (fruit len 11:
  Lv1 hỏi t1-4, Lv10 t4-7, Lv20+ t8-11 mãi). Chuỗi ngắn hơn window (rare len 3)
  không trượt được → collapse về đỉnh: rare hỏi t1-3 đầu game, t2-3 từ ~Lv10,
  chỉ t3 từ Lv20. Đã BỎ `BASE/MAX_ORDER_TIER`, `rareOrderTierCap` và family
  unlock ramp (`familyMaxTier`) — không còn bảo hiểm family mới unlock.
- `COMBO_WINDOW_MS` 30s, `COMBO_MIN` 3 — combo coins `10*combo`, xp `2*combo`.
- `QTE_*`, `FRENZY_MS` 90s, `QTE_WILD_WHEN_COOLING` 0.5.
- Order XP: `completeOrder` → `tier + 3` per item.
- `xpNeededForLevel` = `40 + level*10 + level²*4` (quadratic, front-loaded ~+25% early).
- `orderCoinReward` = `sellPrice * familyBonus%` (VIET +40%, DRINK +25%, BAKERY +10%).

## `GameModels.kt` achievements
- `Achievements.ALL` + `CHAINS` (rows reveal next tier on claim).
- New metric: `Metric` enum + `GameState.metricOf`; new counter: `Stats` field +
  `StatsData` in `data/SaveManager.kt` + increment site +
  `checkAchievements` call on that path.

## Verify
- Hard-to-trigger effects: temp-patch the constant (e.g. `GEN_REGEN_MS` → 6s),
  build, screenshot, revert + rebuild clean before committing. See AGENTS.md
  "Quy trình sau mỗi lần sửa".
