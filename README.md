# Hệ thống thuyết minh đa ngôn ngữ

[![CI/CD](https://github.com/DuongKhang3666/CnpmTest/actions/workflows/ci.yml/badge.svg)](https://github.com/DuongKhang3666/CnpmTest/actions/workflows/ci.yml)

Hệ thống thuyết minh tự động cho bảo tàng và di tích (đồ án môn Công nghệ phần mềm). Quản trị viên nhập địa điểm, hiện vật và nội dung
thuyết minh; hành khách quét QR để vào hệ thống bằng trình duyệt hoặc app, rồi nghe thuyết minh theo ngôn ngữ mình chọn. Nội dung được
dịch tự động bằng LibreTranslate (tự lưu trữ) và có thể được thay bằng bản dịch do quản trị viên nhập tay.

## Tính năng chính
Theo tài liệu yêu cầu `Prd.docx` (8 nhóm use case):

| UC | Chức năng |
|---|---|
| UC1–UC2 | Quản lý địa điểm và hiện vật |
| UC3 | Quản lý ngôn ngữ (mã dịch, key đọc TTS) |
| UC4 | Quản lý nội dung thuyết minh (văn bản, audio, bản dịch tay) |
| UC5 | Cấu hình QR, GPS (vùng bán kính) và Bluetooth beacon |
| UC6 | Báo cáo lượt nghe, thiết bị đang dùng |
| UC7 | Thuyết minh tự động theo QR, GPS, Bluetooth; dịch nội dung và giao diện |
| UC8 | Lịch sử nghe và danh sách yêu thích của từng thiết bị |

## Kiến trúc và cấu trúc thư mục
```
Backend/          Spring Boot 3 lớp (Controller -> Service -> Repository), MySQL, cổng 3000; package admin / user / common
  docs/USER-API.md   API cho người dùng cuối và quản trị truy cập
  sql/seed.sql       dữ liệu mẫu (chỉ nạp vào database trống)
  sql/sample-translations.sql   bản dịch tay mẫu (tuỳ chọn)
Frontend/         Web React/Vite: trang quản trị (/) và trang hành khách (/user), cổng 5173
  mobile/            App Expo (React Native) cho hành khách, có GPS và Bluetooth beacon
Drawio/           Sơ đồ use case, hoạt động, tuần tự (Draw.io + nguồn PlantUML)
docs/             Kết quả kiểm thử
Prd.docx          Tài liệu yêu cầu, thiết kế và kiểm thử
docker-compose.yml  MySQL + Backend (+ LibreTranslate khi bật profile "translate")
.github/workflows/ci.yml   CI/CD: build, test, smoke test docker compose, đẩy image lên ghcr.io
```

## Chạy nhanh bằng Docker (Backend + MySQL)
Cần Docker Desktop (hoặc Docker Engine) và Node.js LTS.

1. Tạo file cấu hình rồi chỉnh theo ý (xem ghi chú trong file):
   ```bash
   cp .env.example .env        # Windows: copy .env.example .env
   ```
   Nên đặt `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `ADMIN_TOKEN_SECRET` (>= 16 ký tự ngẫu nhiên) và `PUBLIC_HOST` (IP LAN của máy chạy server, để
   điện thoại quét được QR). Không commit `.env`.
2. Chạy Backend và cơ sở dữ liệu (lần đầu tự tạo schema và nạp dữ liệu mẫu):
   ```bash
   docker compose up -d --build
   # kèm dịch máy LibreTranslate (lần đầu tải mô hình ngôn ngữ, có thể mất vài phút):
   docker compose --profile translate up -d --build
   ```
   Kiểm tra: `http://localhost:3000/api/user/health`. Nếu chưa đặt `ADMIN_PASSWORD`, Backend sinh mật khẩu ngẫu nhiên và in ra log:
   `docker compose logs backend` (tìm dòng `ADMIN_PASSWORD chưa được cấu hình`).
3. Chạy giao diện web (docker compose chưa kèm web):
   ```bash
   cd Frontend
   npm install
   npm run dev
   ```
4. Mở `http://localhost:5173/`, đăng nhập quản trị, vào "Mở QR đăng nhập". Hành khách quét QR để vào `/user`.
   Điện thoại phải cùng Wi-Fi và truy cập bằng IP LAN (không dùng `localhost`); cho phép Node.js/Vite và cổng 3000, 5173 qua Windows Firewall
   trên mạng Private. Chi tiết: [Frontend/README-LAN.md](Frontend/README-LAN.md).

Tắt hệ thống: `docker compose down` (thêm `-v` sẽ xóa cả dữ liệu MySQL).

## Chạy không cần Docker
Cần JDK 17+, Maven 3.9+, MySQL 8 và Node.js LTS.

1. Tạo database: `CREATE DATABASE commentary_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`
2. Chạy Backend (mặc định kết nối `localhost:3306`, user `root`; đổi bằng `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`), cùng các biến
   `ADMIN_*`, `PUBLIC_HOST` như trong `.env.example`:
   ```powershell
   cd Backend
   $env:DB_PASSWORD = "<mật khẩu MySQL>"
   $env:ADMIN_PASSWORD = "<mật khẩu quản trị>"
   mvn spring-boot:run
   ```
   Lần chạy đầu Backend tự tạo bảng. Muốn có dữ liệu mẫu, dừng Backend rồi nạp (chỉ nạp vào database trống):
   `mysql -u root -p --default-character-set=utf8mb4 commentary_db < Backend/sql/seed.sql`
3. (Tuỳ chọn) Dịch máy: cài LibreTranslate bằng Python (khuyến nghị Python 3.11) trong một môi trường ảo riêng:
   ```bash
   pip install libretranslate
   libretranslate --host 127.0.0.1 --port 5000 --load-only vi,en,fr,ja,ko,zh,th
   ```
   rồi đặt `LIBRETRANSLATE_URL=http://127.0.0.1:5000` (mặc định) cho Backend. Không có LibreTranslate thì hệ thống vẫn chạy: nội dung được
   trả bằng tiếng Việt kèm thông báo dịch lỗi.
4. Chạy giao diện web như bước 3 ở trên.

## Tài khoản quản trị
Không có tài khoản cố định trong mã. Đăng nhập do Backend xử lý (`POST /api/admin/auth/login`) với `ADMIN_EMAIL` (mặc định
`admin@gmail.com`) và `ADMIN_PASSWORD` trong cấu hình. Để trống mật khẩu thì Backend sinh một mật khẩu ngẫu nhiên cho mỗi lần chạy và in ra log.
Sai mật khẩu quá 5 lần trong 5 phút sẽ bị khoá theo IP. Mặc định chỉ thiết bị trong mạng nội bộ được vào trang hành khách (`USER_LAN_ONLY=true`).

## Dịch thuật
Thứ tự lấy bản dịch: bản dịch tay của quản trị viên (`narrations.translations`) -> cache (`translation_cache`) -> LibreTranslate.
Dịch máy chỉ đủ để hiểu ý, nhất là tên riêng và tiếng Nhật, Hàn, Thái; với địa điểm quan trọng nên nhập bản dịch tay. File tuỳ chọn
[Backend/sql/sample-translations.sql](Backend/sql/sample-translations.sql) có sẵn 30 bản dịch mẫu (do AI soạn, cần người bản ngữ rà soát):
```bash
mysql -u root -p --default-character-set=utf8mb4 commentary_db < Backend/sql/sample-translations.sql
```

## App mobile (Expo)
```bash
cd Frontend/mobile
npm install
npx expo start        # thử nhanh bằng Expo Go: có GPS, QR, audio, TTS; KHÔNG có Bluetooth
npx expo run:android  # bản build riêng: có cả Bluetooth beacon
npm test              # kiểm thử logic tiệm cận (không cần điện thoại)
```
Trước khi build thật cần thay khóa Google Maps trong `app.json`. Xem [Frontend/mobile/README-BUILD.md](Frontend/mobile/README-BUILD.md).

## Kiểm thử và CI/CD
```bash
cd Backend && mvn verify      # 105 test (đơn vị, MockMvc, repository trên H2), không cần MySQL
```
GitHub Actions (`.github/workflows/ci.yml`) chạy khi push hoặc pull request: build Frontend, build và test Backend, smoke test bằng docker compose;
khi push vào nhánh chính còn đẩy image lên `ghcr.io`. Kết quả kiểm thử chi tiết: [docs/KET-QUA-KIEM-THU.md](docs/KET-QUA-KIEM-THU.md).

## Tài liệu
- [Prd.docx](Prd.docx): use case, sơ đồ thành phần, triển khai, ERD, lớp, hoạt động, tuần tự, CI/CD và kiểm thử.
- [Backend/README.md](Backend/README.md) và [Backend/docs/USER-API.md](Backend/docs/USER-API.md): Backend và API.
- [Frontend/README-LAN.md](Frontend/README-LAN.md): chạy web trong mạng LAN.
- [Frontend/mobile/README-BUILD.md](Frontend/mobile/README-BUILD.md): build app mobile.

## Giới hạn đã biết
- Chất lượng dịch máy hạn chế; bản dịch tay mẫu chưa được người bản ngữ rà soát.
- Bluetooth beacon cần bản build riêng (không chạy trong Expo Go); GPS và Bluetooth chưa được thử trên thiết bị thật trong phạm vi kiểm thử đã ghi.
- `docker compose` chưa kèm giao diện web; camera và GPS trên điện thoại có thể đòi HTTPS khi không dùng `localhost`.
