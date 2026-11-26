# Bếp Ngon Merge backend

NestJS + Prisma Postgres backend cho Bếp Ngon Merge. Server phục vụ API game và admin page tối thiểu tại `/admin`.

## Chạy local

```sh
cd server
cp .env.example .env   # Windows: copy .env.example .env
# điền DATABASE_URL/DATABASE_URL_POOLED và secrets vào .env
npm install
npm run prisma:deploy
npm run dev
```

`npm run dev` dùng `ts-node` qua `node --watch`, không dùng `tsx`, vì Nest dependency injection/validation cần TypeScript emit decorator metadata đúng chuẩn.

- API root: `http://localhost:3000/`
- Health: `http://localhost:3000/api/health`
- Admin: `http://localhost:3000/admin`
- Emulator Android trỏ host máy dev bằng `http://10.0.2.2:3000/`

Để test leaderboard bằng APK debug, thêm `ALLOW_DEBUG_SCORES=true` vào `.env` local. Không bật biến này trên production.

## Deploy lên Vercel

Vercel có zero-config deployment cho NestJS: import repo và chọn **Root Directory = `server`**. Không cần custom build command/output directory; Vercel tự detect entrypoint `src/main.ts` và chạy app như một Function trên Fluid compute.

Trước khi deploy:

1. Rotate password Postgres nếu credential đã từng bị paste vào chat/log.
2. Tạo các secret random ít nhất 32 ký tự cho `ADMIN_SESSION_SECRET` và `PLAYER_TOKEN_PEPPER`.
3. Trong Vercel Project Settings → Environment Variables, set cho Production/Preview:

```text
DATABASE_URL=<direct Prisma Postgres URL, host db.prisma.io>
DATABASE_URL_POOLED=<pooled Prisma Postgres URL, host pooled.db.prisma.io>
ADMIN_USERNAME=<admin user>
ADMIN_PASSWORD=<admin password mạnh>
ADMIN_SESSION_SECRET=<random 32+ chars>
PLAYER_TOKEN_PEPPER=<random 32+ chars>
NODE_ENV=production
```

`DATABASE_URL_POOLED` được runtime ưu tiên. `DATABASE_URL` direct dùng cho migration/CLI. Không set `ALLOW_DEBUG_SCORES` trên production.

## Apply database migration

Migration đã được commit trong `prisma/migrations/`, nhưng chưa tự apply lên database cloud. Sau khi điền `.env` local bằng credential mới:

```sh
cd server
npm run prisma:deploy
```

Hoặc chạy từ máy CI/dev có env `DATABASE_URL` direct. Không cần chạy migration trong Vercel build.

## Android API base URL

Android đọc `API_BASE_URL` theo thứ tự:

1. environment variable `API_BASE_URL` khi build
2. `API_BASE_URL` trong root `local.properties`
3. fallback dev emulator `http://10.0.2.2:3000/`

Ví dụ `local.properties` cho production:

```properties
API_BASE_URL=https://your-project.vercel.app/
```

Release build phải dùng HTTPS. HTTP cleartext chỉ được cho phép cho `10.0.2.2`, `localhost`, `127.0.0.1` qua network security config để dev local.

## API chính

Public/player API:

- `POST /api/v1/players/bootstrap` — tạo/tìm player bằng random install UUID, trả bearer token
- `PATCH /api/v1/players/me/nickname` — đổi nickname
- `GET /api/v1/leaderboards/:board` — `main`, `mini`, `block`
- `GET /api/v1/leaderboards/:board/around-me`
- `POST /api/v1/leaderboards/:board/submit`
- `GET /api/v1/config/current`
- `GET/PUT /api/v1/saves/me` — backend cloud-save đã có, Android chưa wire
- `POST /api/v1/analytics/events` — backend analytics đã có, Android chưa wire

Admin:

- `/admin` — server-rendered admin MVP
- `POST /api/admin/login`
- `GET /api/admin/dashboard/summary`
- `GET /api/admin/leaderboards/:board`
- `DELETE /api/admin/scores/:id`
- `GET /api/admin/players`
- `POST /api/admin/players/:id/ban|unban`
- `GET/POST/PUT /api/admin/configs...` — draft/validate/publish/rollback config

## Verification

```sh
npm run typecheck
npm test
npm run build
```

Lưu ý hiện tại: `npm audit` báo vulnerability trong transitive `effect` của Prisma CLI 7.10.1. Đây là dev/CLI dependency, chưa có bản Prisma 7 stable vá; không nên ép downgrade xuống Prisma 6 vì schema/client đang dùng format Prisma 7.
