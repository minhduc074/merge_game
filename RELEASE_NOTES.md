# Bếp Ngon Merge — Release Notes

## v1.1.1 — cân bằng máy sinh 🇻🇳

### Cân bằng

- **Máy sinh không còn ra item tier cao**: máy cấp N chỉ sinh item tier 1..N+1
  (máy lv1 → tier 1-2, lv2 → 1-3, lv3 → 1-4). Tier cao hơn chỉ kiếm được bằng
  merge — đơn hàng phải merge mới xong, không còn ăn may từ bấm máy.
- **Chuỗi ngắn (đồ uống, đồ Việt) cap thấp hơn 1 tier**: máy lv2 chỉ ra tier
  1-2 (bù bằng tỉ lệ tier 2 cao hơn hẳn), máy lv3 tối đa tier 3 — chuỗi ngắn
  chạm sâu quá sẽ phá cân bằng.
- **Hồi lượt nhanh gấp đôi**: +1 lượt mỗi 60s (trước 120s); máy cạn sạch thì
  hồi full sau 30s (trước 45s).

## v1.1.0 — minigame Xếp khối + cân bằng kinh tế 🇻🇳

### Mới

- **Minigame "Xếp khối"** 🧱 (kiểu Block Blast!): đặt khối polyomino lên bàn
  8×8, đầy hàng/cột là nổ. Khối vẽ kiểu jelly bóng bẩy trên nền đêm navy có
  sao; kéo khối nảy lên khỏi ngón tay, hiện bóng mờ vị trí đáp + highlight
  hàng sắp nổ, nổ lan theo gợn sóng kèm mảnh vỡ văng, rung màn hình và âm
  thanh cao dần theo combo. Đặt hết 3 khối trong khay mới ra khay tiếp —
  thuật toán sinh khối ưu tiên khối nhỏ khi bàn chật. Điểm càng cao thưởng ⚡
  càng nhiều (800đ → 20⚡, 2500đ → 46⚡...), lưu kỷ lục riêng + 3 thành tựu
  mới. Mở từ thẻ thông tin ⚡, nút cạnh "Chơi Thả trái".

### Cân bằng

- **Lên cấp chậm lại**, ô bàn cờ mở dàn đều mỗi 2 cấp thay vì dồn sớm.
- **Đơn hàng theo trình độ**: món hiếm tier cao chỉ xuất hiện khi đủ level
  (tier 2 từ Lv.8, tier 3 từ Lv.16), đơn món tier thấp giảm dần khi lên cấp.
- **Bảng spawn cân theo độ dài chuỗi** merge — chuỗi ngắn (đồ uống, đồ Việt)
  không còn lệch reach so với chuỗi dài (trái cây).
- **Item hiếm** giảm weight spawn 5 → 2 (bù vào tier 1), hiếm hơn nhưng tier 1
  dễ kiếm hơn đầu game.
- **Mở đầu game**: mỗi dòng chỉ cho 1 máy sinh (thay vì 2 máy trái cây), máy
  ít lượt hơn, item tier cao hiếm hơn — độ khó early-game cao hơn một chút.
- **Thẻ thông tin máy sinh + hướng dẫn đầu game** hiện tỉ lệ spawn trực tiếp.
- **Minigame Thả trái**: điểm merge theo tier², tổng điểm giảm nửa, chỉ thả 4
  trái nhỏ; thưởng ⚡ không trần nhưng dốc giảm dần (2000đ ≈ 50⚡, 5000đ ≈ 99⚡).
- ⚡ thưởng (minigame, quà) được phép **vượt trần 100**.

## v1.0.0 — bản release đầu tiên 🇻🇳

Merge game kiểu Merge Mansion / Travel Town: bàn cờ 5×7, bấm máy sinh để tạo
nguyên liệu, kéo 2 món giống nhau để ghép lên cấp cao hơn, giao đơn cho khách
kiếm vàng và kinh nghiệm.

### Gameplay chính

- **4 dòng nguyên liệu, 40+ món**: Trái cây 🍒, Tiệm bánh 🥐, Đồ uống 🧋 và
  Đồ ăn Việt 🍜 — mỗi dòng có chuỗi merge 7–10 cấp.
- **Máy sinh nâng cấp được**: mỗi dòng có 3 cấp máy (Hộp bánh → Tiệm bánh →
  Nhà máy bánh). Máy cấp cao sinh ra được item tier cao hơn nhưng tier 1 luôn
  là tỉ lệ phổ biến nhất để giữ nhịp game cân bằng.
- **Item hiếm** ✨: 4 chuỗi phụ 3 cấp (xoài cát, bánh tart trứng, trà matcha,
  tôm hùm bơ tỏi...) — xác suất thấp từ máy, giá trị cao, đơn hàng thỉnh thoảng
  yêu cầu. Info máy hiện đầy đủ cả chuỗi merge thường lẫn chuỗi hiếm.
- **Lượt máy sinh**: máy dùng chưa hết thì hồi +1 lượt mỗi 2 phút; dùng sạch
  thì nghỉ 45 giây rồi hồi đầy. Đồng hồ hồi lượt vẫn chạy khi tắt app.
- **Đơn hàng từ thú cưng khách** 🐷🐱🐸: giao đơn nhận vàng + XP; giao nhiều
  đơn liên tiếp trong 30 giây được thưởng combo. Có thể giao tự động khi đủ món.
- **Năng lượng ⚡**: mỗi lần sinh tốn 1⚡, hồi dần theo thời gian (tối đa 100).
- **Lên cấp**: XP từ đơn hàng mở khoá thêm ô bàn cờ (đến Lv.13 mở hết 35 ô),
  thưởng vàng/máy sinh/ô kho mỗi cấp.
- **Sự kiện trong game**: bong bóng QTE bất ngờ (bấm đúng lúc nhận quà), giai
  đoạn Frenzy "GEN MIỄN PHÍ", quà điểm danh hằng ngày theo streak.
- **46 thành tựu / 15 chuỗi** 🏆: merge, đơn hàng, cấp độ, vàng, item hiếm,
  điểm số, minigame, đồ trang trí... claim theo bậc, nhận thưởng vàng.

### Nội dung phụ

- **Minigame "Thả trái"** 🍉: thả trái rơi vào hộp, trái giống chạm nhau tự
  ghép lớn dần (physics jbox2d) — kiếm thêm ⚡.
- **Phòng trang trí** 🛋️: góc nhìn isometric, mua và đặt nội thất, xoay/đổi
  kích thước, sơn tường/sàn theo pattern + màu, chọn bầu trời, undo/redo.
- **Kho đồ**: cất item quý vào túi, mở rộng thêm ô bằng vàng.
- **Lưu offline**: toàn bộ tiến trình lưu tự động trên máy, không cần mạng.

### Kỹ thuật

- Kotlin + Jetpack Compose (Material3), minSdk 26, targetSdk 36.
- Engine game thuần Kotlin tách riêng (`game/`), có unit test.
- R8 minify + shrinkResources bật, APK ký bằng keystore riêng.
- Hiển thị version ở góc dưới-phải màn hình (`v1.0.0`).

---

## v1.1.1 — generator rebalance 🇬🇧

### Balance

- **Generators no longer drop high-tier items**: a level-N generator only
  spawns item tiers 1..N+1 (lv1 → t1-2, lv2 → t1-3, lv3 → t1-4). Higher tiers
  are merge-only — orders are filled by merging, not lucky taps.
- **Short chains (drinks, Vietnamese food) cap one tier lower**: lv2 gens
  keep the t1-2 reach (but pay much more t2), lv3 gens max out at t3 —
  short chains break if taps reach too deep.
- **Twice-as-fast recharge**: +1 charge every 60s (was 120s); a fully
  drained generator resets to full after 30s (was 45s).

## v1.1.0 — Block Puzzle minigame + economy rebalance 🇬🇧

### New

- **"Block Puzzle" minigame** 🧱 (Block Blast!-style): drop polyomino pieces
  onto an 8×8 board — full rows/columns pop. Glossy jelly blocks over a
  starry navy night backdrop; dragged pieces lift above your finger with a
  landing ghost + line-clear highlight, wave-style clears with shatter
  particles, screen shake, and combo-pitched sound effects. Refills only
  after all 3 tray pieces are placed, with smart piece generation favoring
  small shapes when the board gets crowded. Higher scores pay more ⚡
  (800 pts → 20⚡, 2500 pts → 46⚡...), with a separate best score + 3 new
  achievements. Open it from the ⚡ info card, next to "Fruit Drop".

### Balance

- **Slower leveling**, board cells now spread across every 2 levels instead
  of front-loading.
- **Level-aware orders**: higher rare tiers only appear once you're leveled
  enough (tier 2 from Lv.8, tier 3 from Lv.16), and low-tier order weight
  decays as you level up.
- **Spawn tables equalized by chain depth** — short chains (drinks,
  Vietnamese food) no longer have skewed reach vs. long ones (fruit).
- **Rare items**: spawn weight cut 5 → 2 (fed back into tier-1), so tier-1
  is easier to come by early on.
- **Early game**: one generator per family at start (instead of two fruit
  gens), fewer charges, rarer high-tier spawns — slightly tougher opening.
- **Generator info cards + the first-run guide** now show live spawn rates.
- **Fruit Drop minigame**: merge points scale by tier², overall scores
  halved, only the 4 smallest fruits drop; ⚡ payout is uncapped but flattens
  out (~2000 pts ≈ 50⚡, ~5000 pts ≈ 99⚡).
- Reward ⚡ (minigames, gifts) can now **overflow the 100 cap**.

## v1.0.0 — first release 🇬🇧

A Merge Mansion / Travel Town-style merge game: a 5×7 board, tap generators
to spawn ingredients, drag two matching items together to merge up, and serve
customer orders for coins and XP.

### Core gameplay

- **4 ingredient families, 40+ items**: Fruit 🍒, Bakery 🥐, Drinks 🧋 and
  Vietnamese food 🍜 — each chain has 7–10 merge tiers.
- **Upgradeable generators**: 3 machine tiers per family (Snack Box → Bakery
  Shop → Bakery Factory). Higher tiers can spawn higher-tier items, but tier-1
  always stays the most common drop to keep the merge economy balanced.
- **Rare items** ✨: four 3-tier side chains (premium mango, egg tart, matcha,
  garlic-butter lobster...) — low spawn odds, high value, occasionally asked
  for by orders. Generator info shows both the normal and rare merge chains.
- **Generator charges**: a partially used generator regains +1 charge every
  2 minutes; a fully drained one takes a 45-second cooldown before refilling.
  Recharge timers keep ticking while the app is closed.
- **Animal customer orders** 🐷🐱🐸: deliver orders for coins + XP; chaining
  orders within 30 seconds earns combo bonuses. Auto-deliver is available when
  all required items are ready.
- **Energy ⚡**: each spawn costs 1⚡, regenerating over time (cap 100).
- **Leveling**: XP from orders unlocks more board cells (all 35 cells open by
  Lv.13), with coins/generators/inventory slots as per-level rewards.
- **Live events**: surprise QTE bubbles (tap in time for gifts), "FREE GEN"
  Frenzy windows, daily login rewards with streaks.
- **46 achievements / 15 chains** 🏆: merges, orders, levels, coins, rare
  finds, score, minigame, decor... tiered claims with coin rewards.

### Extras

- **"Fruit Drop" minigame** 🍉: drop fruit into the box — matching fruits
  merge on contact (jbox2d physics) — earn extra ⚡.
- **Decor room** 🛋️: isometric view, buy and place furniture, rotate/resize,
  paint walls/floors with patterns + tints, pick the sky, undo/redo.
- **Storage bag**: stash precious items, expand slots with coins.
- **Offline save**: all progress is stored on-device, no network needed.

### Technical

- Kotlin + Jetpack Compose (Material3), minSdk 26, targetSdk 36.
- Pure-Kotlin game engine (`game/`) covered by unit tests.
- R8 minify + shrinkResources enabled, APK signed with a release keystore.
- Build version shown at the bottom-right corner (`v1.0.0`).
