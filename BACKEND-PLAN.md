# Bếp Ngon Merge — Backend Plan & Tiến độ

> Cập nhật lần cuối: 2026-11-26. Tick vào `[x]` khi xong từng mục.

## Trạng thái tổng quan

| Hạng mục | Trạng thái |
|---|---|
| Backend API (NestJS + Prisma Postgres) | ✅ Live production |
| Leaderboard end-to-end | ✅ Xong |
| Deploy Vercel | ✅ Ổn định |
| Admin page | ✅ Hoạt động |
| Remote config — server | ✅ Xong |
| Remote config — Android apply | 🚧 Generators đã apply; economy/orders/rewards chưa |
| Cloud save — server | ✅ Xong |
| Cloud save — Android | ❌ Chưa làm |
| Analytics — server | ✅ Xong |
| Analytics — Android gửi events | ✅ Xong |
| Hardening | ❌ Chưa làm |

**Production URL**: <https://server-olive-five-65.vercel.app>
**Admin**: <https://server-olive-five-65.vercel.app/admin> (user `admin`, pass trong `server/.env`)

---

## Việc cần làm NGAY (manual, của user)

- [ ] **Rotate database credentials** (đã lộ trong chat):
  1. Prisma Console hoặc Vercel → Storage → rotate/reset connection string
  2. Paste string mới trực tiếp vào `server/.env` (KHÔNG gửi trong chat)
  3. Báo Devin → push lên Vercel env bằng CLI + redeploy + verify

---

## ✅ Đã xong

- [x] Backend NestJS skeleton + Prisma schema + migration lên Prisma Postgres cloud
- [x] Player identity: random install UUID + bearer token (server chỉ lưu hash)
- [x] Leaderboard 3 bảng (`main`/`mini`/`block`): submit, top 100, around-me, best monotonic, score history
- [x] Anti-cheat cơ bản: rate limit, score caps per board, debug build bị loại khỏi production leaderboard
- [x] Admin auth (cookie HMAC) + admin page: dashboard, leaderboard (xoá/ban), config editor JSON, players, analytics
- [x] Android: Retrofit layer, identity manager, LeaderboardScreen 3 tabs + nickname editor + highlight "Bạn"
- [x] Submit score: main (throttle) + mini/block khi xong minigame, retry in-memory
- [x] Deploy Vercel production, fix ERR_REQUIRE_ESM, fix env vars rỗng, fix admin page blank (JS `\n` escape)
- [x] Verify end-to-end emulator → production
- [x] Dọn test data trong DB

### Quyết định kỹ thuật quan trọng (nhớ khi maintain)

- **Phải dùng NestJS 11.x** — NestJS 12 là ESM-only, Vercel zero-config compile ra CommonJS → crash `ERR_REQUIRE_ESM`. Không upgrade Nest 12 trừ khi Vercel đổi cách build.
- **Không set env `PORT` trên Vercel** — nó override port Vercel inject, function không nhận traffic.
- **Không set `ALLOW_DEBUG_SCORES` trên production** — debug build sẽ không rank (đúng chính sách).
- Env vars quản lý qua Vercel CLI: `vercel env ls/pull/add/rm` trong `server/` (đã `vercel link`).
- Generated Prisma client **được commit vào git** (`server/src/generated/`) cho deterministic deploy.
- Dev script là `node --watch -r ts-node/register` — KHÔNG dùng `tsx` (không emit decorator metadata → DI inject undefined).

---

## Phase 2 — Remote config (ưu tiên 1, việc lớn nhất)

Mục tiêu: chỉnh balance trong admin → app áp dụng không cần release.

### Server (✅ đã xong)

- [x] Schema + validation (spawn weights tổng 100, item id hợp lệ, timer ranges)
- [x] Draft/publish/rollback + audit log
- [x] `GET /api/v1/config/current?sinceVersion=`
- [x] Publish config v1 làm mốc (production hiện có config v2)

### Android — chưa làm (phần cốt lõi)

- [ ] Tạo `game/GameBalance.kt` chứa data class config, default = đúng hardcode hiện tại:
  - `MAX_ACTIVE_ORDERS`, `ORDER_INTERVAL_MIN/MAX_MS`, `BASE_ORDER_TIER`, `MAX_ORDER_TIER`, `RARE_ORDER_WEIGHT`
  - `ENERGY_CAP`, energy per spawn, regen ms
  - `GEN_COOLDOWN_MS`, `GEN_REGEN_MS`, `maxCharges` per gen
  - spawn table override per generator
  - XP curve, combo/QTE rewards, minigame energy brackets
  - order count weights (1/2/3 items) — lưu ý engine hiện dùng weight **động theo level** (xem `pickOrderItem`), config cần thiết kế cho khớp hoặc chấp nhận đơn giản hoá
  - family unlock levels
- [ ] Refactor `GameEngine` nhận balance thay vì đọc const (engine vẫn pure)
- [ ] UI đang đọc trực tiếp constants (`ENERGY_CAP`, `GEN_REGEN_MS`...) → chuyển sang active config
- [x] `ConfigRepository`: fetch không blocking, cache DataStore, fallback bundled default khi offline/invalid — hiện mới apply generator section
- [ ] Validate phía client trước khi apply (cùng rules server)
- [ ] Gửi `configVersion` kèm score submit + analytics
- [ ] Unit test: default balance ≡ behavior hiện tại; override spawn table deterministic bằng seed
- [x] Verify trên emulator: publish config thử → app nhận config mới (generator maxCharges)

### Admin UI nâng cao (làm sau phần cốt lõi)

- [ ] Form editor theo section (Generators/Economy/Orders/XP/Minigames) thay textarea JSON
- [ ] Hiện % xác suất spawn thay weight thô; cảnh báo tổng ≠ 100
- [ ] Diff config trước khi publish
- [ ] Preset Easy/Normal/Hard
- [ ] (xa) Mini simulator Monte Carlo ước lượng pacing

---

## Phase 3 — Cloud save/sync (ưu tiên 3)

### Server (✅ đã xong, đã test PUT/GET production)

### Android — chưa làm

- [ ] Tách export format từ `SaveManager` (quan trọng: dùng format countdown-based, KHÔNG upload raw GameState vì `nowMs` là session-relative)
- [ ] Upload debounce khi state đổi + cố upload khi app pause
- [ ] `lastSyncedRevision` local; optimistic concurrency `baseRevision`, xử lý 409
- [ ] Download khi mở app nếu cloud mới hơn
- [ ] Conflict UI: "Dùng bản cloud hay giữ bản máy này?" — không auto overwrite
- [ ] Sync status icon nhỏ (synced/syncing/offline/conflict)
- [ ] Test: reinstall/đổi máy giữ nguyên progress; checksum SHA-256 corrupt detection

---

## Phase 4 — Analytics (ưu tiên 2, nhỏ + nhanh)

### Server (✅ đã xong)

### Android — chưa làm

- [x] Client queue offline + batch (tối đa ~50 events/request) + retry background, không block gameplay
- [x] Events tối thiểu: `app_open`, `onboarding_finished`, `config_loaded`/`config_fallback`, `generator_tapped`, `order_spawned`/`order_completed`, `level_up`, `mini_finished`, `block_finished`, `leaderboard_viewed` — `save_synced` chờ Cloud save
- [x] Kèm `configVersion` + `appVersion` trong mọi event
- [ ] Admin dashboard v2: DAU/WAU, level distribution, funnel cơ bản (server-side aggregation)

---

## Phase 5 — Hardening (làm dần)

- [ ] Rate limit hiện in-memory per-instance (serverless đếm riêng từng instance) → cân nhắc store chung nếu cần thật
- [ ] `npm audit`: 3 high severity trong dep chain `@prisma/config` → `effect` — chưa có fix an toàn (fix đề xuất = downgrade prisma 6, không tương thích), theo dõi prisma update
- [ ] Retention policy analytics events (cron xoá events > 90 ngày?) tránh phình DB free tier
- [ ] Backup/restore drill cho Postgres
- [ ] Custom domain (tuỳ chọn)
- [ ] Chống cheat nâng cao chỉ làm khi leaderboard cần competitive thật (server validate/replay) — v1 chấp nhận giảm thiểu

## Known issues nhỏ

- Debug build (emulator) submit điểm bị production reject đúng chính sách → test leaderboard trên emulator cần release build hoặc tạm bật `ALLOW_DEBUG_SCORES`
- `mini`/`block` boards chưa có data thật
- Engine order-count weight động theo level chưa khớp 1-1 với `countWeights` tĩnh trong config — quyết định khi làm Phase 2

## Quy trình làm việc

- Sau mỗi thay đổi server: `npm run typecheck && npm test && npm run build` trong `server/` → commit → push (Vercel auto-deploy) → curl verify `/api/health`
- Sau mỗi thay đổi app: theo quy trình trong `AGENTS.md` (build → install emulator → screenshot verify → xoá file tạm → commit)
- Xem logs production: `vercel inspect <url>` hoặc dashboard → Runtime Logs
