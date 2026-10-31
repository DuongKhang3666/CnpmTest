# Back-end user — tài liệu API

Back-end user là phần Spring Boot phục vụ **người dùng cuối** (web `/user`, app mobile), tách khỏi API CRUD của admin
(`/sites`, `/artifacts`... kiểu json-server). Thay thế hoàn toàn phần "user" của máy chủ Node cũ `server.mjs`
(Socket.IO cổng 3001 + `/api/translate`; đã gỡ khỏi repo).

## Kiến trúc 3 lớp

```
Controller (user/controller)  →  Service (user/service)  →  Repository (user/repository)  →  MySQL
   HTTP, DTO, validate              nghiệp vụ, quy tắc            JdbcTemplate, SQL
```

| Lớp | Thành phần chính |
|---|---|
| Controller | `UserSessionController`, `UserContentController`, `UserNarrationController`, `UserDiscoveryController`, `UserActivityController`, `UserTranslateController` |
| Service | `SessionService`, `AccessTokenService`, `ContentService`, `NarrationService`, `DiscoveryService`, `ActivityService`, `TranslationService` |
| Repository | `DeviceSessionRepository`, `AccessTokenRepository`, `ContentRepository`, `ListenRepository`, `UserHistoryRepository`, `UserLikeRepository`, `TranslationCacheRepository`, `SettingsRepository` |
| Khác | `model/Models` (entity), `dto/Dtos`, `security/*` (interceptor), `translation/*` (cổng dịch), `exception/*` |

Các endpoint `/api/admin/access/**` (QR, thiết bị, người đang dùng) do `AdminAccessController` / `AccessAdminService` phục vụ, nằm ở nhánh `admin` của Backend
(cùng bộ đăng nhập quản trị); chúng đọc/ghi dữ liệu truy cập của nhánh `user`. Mã dùng chung (`NetworkPolicy`, `TokenHasher`, CORS) nằm ở `common`.
Sơ đồ package đầy đủ: [../README.md](../README.md#kiến-trúc-mã-nguồn).

## Luồng truy cập (thay cho Socket.IO)

1. Admin lấy mã QR: `GET /api/admin/access/qr` → `{ "token": "...", "ips": [{name, address}] }`. Link trong QR là
   `http://<ip>:5173/user?token=<token>`; `ips` là IP LAN của máy chủ (hoặc `PUBLIC_HOST` nếu đặt). Nếu `ips` rỗng
   (thường gặp khi chạy trong Docker) giao diện admin dùng địa chỉ trang admin đang mở.
2. Thiết bị quét QR rồi gọi `POST /api/user/session/join` với `token` và `deviceId` (8–64 ký tự `A-Za-z0-9_-`, client tự sinh và lưu).
3. Server trả `sessionToken`. **Mọi lời gọi `/api/user/**` sau đó phải gửi 2 header**:
   - `X-Device-Id: <deviceId>`
   - `X-Session-Token: <sessionToken>`
4. Client gọi `POST /api/user/session/heartbeat` định kỳ (15–20 giây) để được tính là "đang dùng". Quá 45 giây không có lời gọi nào thì bị coi là offline.
5. Hết hạn (`accessHours`, mặc định lấy từ bảng `settings`) → mọi lời gọi trả 403 `expired` cho tới khi admin gia hạn.
6. Vào lại sau khi mất phiên: gửi `deviceId` + `sessionToken` cũ (không cần quét lại QR). Chỉ biết `deviceId` là **không đủ**.
   Khi vào lại bằng phiên còn hợp lệ, server **giữ nguyên** token (không xoay) để nhiều tab hoặc nhiều lời gọi đồng thời
   không ghi đè nhau. Token chỉ được cấp mới khi vào bằng token QR.
7. Trang web (`user.jsx`) lưu `sessionToken` trong `localStorage`, **bỏ `?token=` khỏi thanh địa chỉ** sau khi vào được,
   nên tải lại trang hoặc mở lại sau này không cần quét QR lại (cho tới khi hết hạn / bị thu hồi).
8. `user.lan-only=true` (mặc định, biến `USER_LAN_ONLY`): chỉ nhận thiết bị có IP loopback, `10.*`, `172.16–31.*`,
   `192.168.*`, link-local hoặc IPv6 nội bộ; ngoài ra trả 403 `not-lan`. Đặt `false` khi triển khai lên Internet.

Lỗi truy cập luôn có dạng `{ "ok": false, "reason": "...", "message": "..." }`:

| reason | HTTP | Ý nghĩa |
|---|---|---|
| `bad-device` | 400 | `deviceId` sai định dạng |
| `no-token` / `token-invalid` | 401 | thiếu / sai token QR |
| `no-session` / `invalid-session` | 401 | thiếu / sai header phiên → quét QR lại |
| `expired` | 403 | thiết bị hết hạn |
| `not-lan` | 403 | thiết bị không nằm trong mạng nội bộ (khi `USER_LAN_ONLY=true`) |
| `app-limit` | 403 | đã đủ số thiết bị App đồng thời |
| `admin-auth` | 401 | thiếu / sai / hết hạn token đăng nhập admin (mọi API admin) |
| `bad-credentials` / `too-many-attempts` | 401 / 429 | sai email-mật khẩu admin / bị khoá tạm vì nhập sai nhiều lần |

## Endpoint cho người dùng

Công khai (không cần header phiên): `GET /api/user/health`, `POST /api/user/session/join`, và ảnh/audio
(`/sites/{id}/image`, `/sites/{id}/images/{n}`, `/artifacts/{id}/image`, `/narrations/{type}/{id}/audio`) để nhúng được vào `<img>`/`<audio>`.

| Method | Đường dẫn | Mô tả |
|---|---|---|
| POST | `/api/user/session/join` | Vào hệ thống `{token, deviceId, sessionToken?, info{userAgent, platform, language, screen, timezone, deviceName, clientType: "web"\|"app"}}` |
| POST | `/api/user/session/heartbeat` | Giữ phiên, trả thời gian còn lại |
| GET | `/api/user/session/me` | Trạng thái phiên |
| POST | `/api/user/session/leave` | Rời hệ thống (đánh dấu offline) |
| GET | `/api/user/catalog` | **Gói khởi tạo cho trang web**: ngôn ngữ, địa điểm, hiện vật, cấu hình thuyết minh (GPS/QR/Beacon), tổng lượt nghe, lịch sử và yêu thích của thiết bị — một lời gọi thay cho 7 lời gọi |
| GET | `/api/user/languages` | Ngôn ngữ đang bật `[ {code, name, ttsKey} ]` |
| GET | `/api/user/sites?q=&city=` | Địa điểm đã xuất bản, không ẩn; tìm không phân biệt dấu |
| GET | `/api/user/sites/{id}` | Chi tiết + hiện vật + danh sách ảnh phụ |
| GET | `/api/user/artifacts?siteId=&q=` | Hiện vật (chỉ khi địa điểm chứa nó đang hiển thị) |
| GET | `/api/user/artifacts/{id}` | Chi tiết hiện vật |
| GET | `/api/user/narrations/{site\|artifact}/{id}?lang=en` | Nội dung thuyết minh đã dịch |
| GET | `/api/user/resolve/qr?code=` | Nhận diện từ nội dung QR (mã `qr.code`, link, hoặc `.../play?type=site&id=1`) |
| GET | `/api/user/nearby?lat=&lng=` | Đối tượng có vùng GPS chứa vị trí, gần nhất trước |
| GET | `/api/user/resolve/beacon?mac=` | Nhận diện từ Beacon Bluetooth |
| POST | `/api/user/translate` | Dịch hàng loạt `{texts:[...], target, source?}` |
| POST | `/api/user/listens` | Cộng 1 lượt nghe `{targetType, targetId, language}` (dữ liệu cho báo cáo admin) |
| GET/POST/DELETE | `/api/user/history` | Lịch sử nghe của thiết bị (`limit`, mặc định 50) |
| GET/POST | `/api/user/likes` | Yêu thích của thiết bị |
| DELETE | `/api/user/likes/{type}/{id}` | Bỏ yêu thích |

Ví dụ nhận thuyết minh:

```bash
curl http://localhost:3000/api/user/narrations/site/1?lang=en \
  -H "X-Device-Id: my-device-0001" -H "X-Session-Token: <sessionToken>"
```

```json
{ "targetType": "site", "targetId": 1, "name": "Dinh độc lập", "requestedLanguage": "en", "sourceLanguage": "vi",
  "text": "Independence Palace ...", "translated": true, "translationError": null,
  "ttsEnabled": true, "ttsKey": "en-US", "speed": 1.0, "audioEnabled": false, "audioUrl": null }
```

Thứ tự lấy bản dịch: bản dịch admin nhập trong `narrations.translations` → cache `translation_cache` → gọi LibreTranslate.
Văn bản thuyết minh (mô tả hoặc nội dung riêng) luôn là tiếng Việt (`sourceLanguage` = `vi`). Cột `narrations.language` là ngôn ngữ cấu hình của thuyết minh
(ngôn ngữ mặc định khi không truyền `lang`, key TTS, ngôn ngữ của file audio), không phải ngôn ngữ của văn bản: người nghe chọn khác `vi` thì luôn được dịch,
còn audio chỉ phát khi chọn đúng ngôn ngữ của thuyết minh.
Bản dịch tay mẫu (địa điểm 1, 2, 3, 6 và hiện vật 1, mỗi mục en/fr/ja/zh-CN/ko/th) nằm ở `Backend/sql/sample-translations.sql` (tuỳ chọn, không tự nạp, do AI soạn nên cần người bản ngữ rà soát):
`mysql -u root -p --default-character-set=utf8mb4 commentary_db < Backend/sql/sample-translations.sql`. Đã kiểm tra với Backend + MySQL 8: cả 30 bản dịch trả đúng từng ký tự, `translated=true`, kể cả khi LibreTranslate tắt.
Nếu dịch lỗi vẫn trả văn bản gốc với `translated=false` và `translationError` (đúng luồng phụ của UC7.4).
`audioUrl` chỉ có khi nghe đúng ngôn ngữ gốc của file audio.

## Endpoint quản trị truy cập (`/api/admin/access/**`)

Thay cho các sự kiện Socket.IO `admin:*`. Cần đăng nhập admin (xem mục *Bảo mật API quản trị* bên dưới): gửi `Authorization: Bearer <token>`.

| Method | Đường dẫn | Tương đương cũ |
|---|---|---|
| GET | `/overview` | `devices` snapshot (QR, số đang dùng, danh sách thiết bị) |
| GET | `/qr` | `admin:getQr` |
| POST | `/qr/rotate` | `admin:rotateQr` |
| POST | `/devices/{deviceId}/extend` `{hours}` | `admin:extendDevice` |
| DELETE | `/devices/{deviceId}` | `admin:deleteDevice` (thu hồi + đổi QR) |
| POST | `/devices/clear-offline` | `admin:clearOffline` |
| PUT | `/config` `{maxAppDevices?, accessHours?}` | `admin:setMaxAppDevices` / `admin:setAccessHours` (hạn mặc định cho thiết bị mới) |

`GET /qr` và `POST /qr/rotate` trả thêm `ips` (IP LAN của máy chủ).

## Bảo mật API quản trị

Mặc định **mọi đường dẫn đều cần đăng nhập admin**, trừ `/api/user/**` (có cơ chế phiên thiết bị riêng) và `POST /api/admin/auth/login`.
Gồm cả API CRUD cũ (`/sites`, `/artifacts`, `/narrations`, `/languages`, `/listens`, `/devices`, `/history`, `/likes`, `/settings`) và `/api/admin/**`.
Thiếu hoặc sai token → `401 {"reason":"admin-auth"}`; chưa có lệnh nào chạm tới tầng service/DB.

| Method | Đường dẫn | Mô tả |
|---|---|---|
| POST | `/api/admin/auth/login` `{email, password}` | → `{token, email, expiresAt}`. Sai → 401 `bad-credentials`; quá 5 lần sai/5 phút theo IP → 429 `too-many-attempts` |
| GET | `/api/admin/auth/me` | kiểm tra token còn dùng được (cần token) |

- Token không trạng thái, ký HMAC-SHA256 bằng `ADMIN_TOKEN_SECRET`, hết hạn sau `ADMIN_TOKEN_HOURS` (12 giờ). Đổi `ADMIN_TOKEN_SECRET` = thu hồi mọi token. Logout chỉ cần xoá token ở client.
- Tài khoản lấy từ cấu hình `ADMIN_EMAIL` / `ADMIN_PASSWORD`. Để trống mật khẩu → sinh ngẫu nhiên và in ra log (chỉ nên dùng khi chạy thử).
- Đường dành cho máy: nếu đặt `ADMIN_API_KEY`, header `X-Admin-Key: <khoá>` cũng được chấp nhận (script/CI).
- CORS: chỉ nhận trang web từ `localhost` / IP mạng nội bộ; thêm origin khác bằng `CORS_ALLOWED_ORIGINS` (cách nhau dấu phẩy, `*` = tất cả).
- Còn công khai có chủ đích: `GET /api/user/health`, `POST /api/user/session/join` (cần token QR) và các endpoint ảnh/audio `/api/user/.../image|audio`
  (thẻ `<img>`/`<audio>` không gửi được header). Vì vậy URL ảnh/audio ai biết id đều tải được; nếu cần chặt hơn, dùng URL ký có hạn.

## Cơ sở dữ liệu

- `db/base-schema.sql`: các bảng cũ (`sites`, `artifacts`, `narrations`, `languages`, `listens`, `devices`, `history`, `likes`, `settings`) — `CREATE TABLE IF NOT EXISTS`, an toàn với DB đã có dữ liệu.
- `db/user-schema.sql`: bảng mới `access_tokens`, `device_sessions`, `app_config`, `translation_cache`, `user_history`, `user_likes`.
- Ứng dụng tự chạy hai file này khi khởi động (`SQL_INIT_MODE=always`, mặc định). Đặt `never` để tắt.
- `Backend/sql/seed.sql`: dữ liệu mẫu sinh từ `Frontend/db.json` (6 địa điểm, 1 hiện vật, 7 thuyết minh, 7 ngôn ngữ...). Docker Compose tự nạp khi tạo volume lần đầu.
- Ảnh/audio lưu trong DB dạng data URL base64 (rất lớn) nên API **không** trả base64 trong danh sách: chỉ trả `imageUrl`, ảnh thật lấy qua endpoint ảnh (có cache 1 giờ).

## Cấu hình

| Biến môi trường | Mặc định | Ý nghĩa |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SERVER_PORT` | như README | kết nối DB, cổng |
| `SQL_INIT_MODE` | `always` | `never` để không tự tạo bảng |
| `ADMIN_EMAIL` | `admin@gmail.com` | email đăng nhập admin |
| `ADMIN_PASSWORD` | trống | mật khẩu admin (trống = sinh ngẫu nhiên, in ra log) |
| `ADMIN_TOKEN_SECRET` | trống | khoá ký token (≥ 16 ký tự; trống = ngẫu nhiên mỗi lần chạy) |
| `ADMIN_TOKEN_HOURS` | 12 | thời hạn token đăng nhập |
| `ADMIN_LOGIN_MAX_FAILURES`, `ADMIN_LOGIN_WINDOW_SECONDS` | 5, 300 | khoá theo IP khi nhập sai quá số lần |
| `ADMIN_API_KEY` | trống | khoá máy-với-máy qua header `X-Admin-Key` (tuỳ chọn) |
| `CORS_ALLOWED_ORIGINS` | trống | origin được phép ngoài localhost/mạng nội bộ |
| `USER_ACCESS_HOURS` | 24 | thời hạn khi bảng `settings` trống |
| `USER_MAX_APP_DEVICES` | 5 | số thiết bị App đồng thời mặc định |
| `USER_ONLINE_WINDOW_SECONDS` | 45 | thời gian không hoạt động để tính offline |
| `USER_LAN_ONLY` | `true` | chỉ cho thiết bị trong mạng nội bộ vào hệ thống |
| `PUBLIC_HOST` | trống | IP/tên miền đưa vào link QR của admin (trống = IP LAN tự dò) |
| `TRANSLATE_ENABLED`, `LIBRETRANSLATE_URL`, `LIBRETRANSLATE_API_KEY`, `TRANSLATE_TIMEOUT_SECONDS` | bật, `http://127.0.0.1:5000` | dịch vụ dịch |

## Chạy test, build, Docker

```bash
cd Backend
mvn verify                    # biên dịch + chạy toàn bộ test (H2, không cần MySQL)
docker compose up -d --build  # ở thư mục gốc: MySQL + backend (cổng 3000)
```

## Trạng thái nối client

- **Web (đã nối):** `Frontend/src/user.jsx` dùng `src/userApi.js` (REST + heartbeat 20 giây, bỏ Socket.IO và json-server);
  trang admin "Những người đang dùng" (`ActiveUsers.jsx`) hỏi `GET /api/admin/access/overview` mỗi 3 giây.
  Kiểm thử bằng trình duyệt thật (Playwright): quét QR đúng/sai/không token, tải lại không cần quét lại, thu hồi thiết bị,
  QR cũ bị từ chối, QR mới vào được, ảnh, thích, lịch sử.
- **App React Native (đã nối, chưa thử trên điện thoại):** `mobile/lib/userApi.ts` + `mobile/app/index.tsx`.
  Quét QR "Đăng nhập App" (JSON `{type:"app-login", token, server}`; cổng `:3001` cũ trong QR tự đổi sang `:3000`) hoặc QR link web
  (`http://<ip>:5173/user?token=...` → app đoán máy chủ `http://<ip>:3000`) → `POST /session/join` với `info.clientType = "app"`
  (tính vào giới hạn thiết bị App) → lưu `sessionToken` + mã thiết bị trong AsyncStorage → heartbeat 20 giây và khi app quay lại từ nền.
  Mở lại app không cần quét lại; 401/403 ở bất kỳ lời gọi nào → về màn hình đăng nhập. Dịch văn bản/thuyết minh ưu tiên qua máy chủ
  (`/translate`, `/narrations/...?lang=`), lỗi thì dùng Google Translate như cũ. Đã kiểm tra bằng `tsc` (không thêm lỗi mới so với bản gốc)
  và script mô phỏng các lời gọi của app (25/25 bước); **chưa chạy Expo/emulator** nên giao diện cần thử tay.

## Lưu ý còn lại

- Trang web và app đã dùng `GET /narrations/{type}/{id}?lang=` cho lời thuyết minh đầy đủ (bản admin nhập > cache DB > dịch máy) và
  `POST /translate` theo lô (tối đa 50 đoạn/lần) cho giao diện, tên, địa chỉ. Nếu admin đã tải audio và ngôn ngữ nghe trùng ngôn ngữ
  bản thu thì `audioUrl` được trả về và web/app phát file audio đó; ngôn ngữ khác hoặc lỗi audio thì đọc bằng giọng tổng hợp (TTS).
- Mã QR trong trang admin được vẽ ngay trên trình duyệt (`qrcode.react`), không gọi `api.qrserver.com`, dùng được khi mạng LAN không có Internet.
- Đã thử với LibreTranslate thật (bản 1.9.6 cài bằng `pip` trên Python 3.11, `--load-only vi,en,fr,ja,ko,zh,th`) qua `POST /api/user/translate`:
  vi→en, ja, zh (và `zh-Hans`) đều trả 200 với `translated=true`; ví dụ "Bảo tàng Lịch sử" → "History Museum" / 歴史博物館 / 历史博物馆.
  LibreTranslate dùng mã `zh-Hans`, Back-end nhận cả `zh` lẫn `zh-Hans`. Câu rất ngắn dịch chưa tốt (vd "Xin chào" → ja ra "お問い合わせ").
  Cách chạy bằng Docker (`docker compose --profile translate up`) chưa được thử trọn vẹn: cấu hình `LT_LOAD_ONLY` đã đối chiếu với mã nguồn LibreTranslate 1.9.6
  (biến môi trường = tiền tố `LT_` + tên cờ, cùng danh sách mã ngôn ngữ như cờ `--load-only` đã chạy thật), còn job CI chạy compose với `TRANSLATE_ENABLED=false`.
  Không có LibreTranslate thì trả bản gốc kèm `translated=false`.
- Bảng `devices` cũ (json-server) không còn được cập nhật; máy chủ Node cũ `server.mjs` đã được gỡ khỏi repo vì không còn cần cho trang web lẫn app mobile.
- API admin CRUD cũ đã được bảo vệ bằng đăng nhập admin (xem *Bảo mật API quản trị*). Mật khẩu admin hiện đọc từ biến môi trường (chưa có bảng người dùng / đổi mật khẩu trên giao diện).
