# Commentary Backend — Spring Boot / MySQL

Backend viết lại theo hướng đối tượng, chia lớp `Controller -> Service -> Repository`, tương thích các đường dẫn JSON-server mà `Frontend/src/api.jsx` (trang quản trị) đang gọi, kèm nhóm `/api/user/**` cho hành khách. Mặc định chạy cổng **3000**. Hướng dẫn chạy toàn bộ hệ thống: [README gốc](../README.md).

## Yêu cầu
- JDK 17+
- Maven 3.9+ (hoặc dùng `mvnw.cmd` nếu đã có Maven Wrapper)
- MySQL đang chạy và đã import schema/dữ liệu vào database `commentary_db`

## Cấu hình
Mặc định kết nối `jdbc:mysql://localhost:3306/commentary_db`, user `root`, password rỗng. Có thể override bằng biến môi trường:
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `SERVER_PORT` (mặc định 3000)
- `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `ADMIN_TOKEN_SECRET` (>= 16 ký tự ngẫu nhiên): tài khoản và khoá ký token đăng nhập quản trị. Để trống `ADMIN_PASSWORD` thì Backend sinh mật khẩu ngẫu nhiên và in ra log
- `ADMIN_API_KEY` (tuỳ chọn, cho script/CI gọi API quản trị qua header `X-Admin-Key`)
- `PUBLIC_HOST`, `USER_LAN_ONLY`, `CORS_ALLOWED_ORIGINS`: địa chỉ đưa vào link QR, giới hạn mạng nội bộ, nguồn CORS bổ sung
- `TRANSLATE_ENABLED`, `LIBRETRANSLATE_URL`: dịch máy (xem `.env.example` ở thư mục gốc)

## Chạy trên Windows PowerShell
Mở PowerShell tại thư mục có `pom.xml`:

```powershell
mvn clean spring-boot:run
```

Hoặc:

```powershell
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = ""
mvn clean spring-boot:run
```

## Endpoint JSON-server tương thích

| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/sites` | Danh sách; hỗ trợ query lọc |
| POST | `/sites` | Tạo địa điểm |
| GET/PATCH/PUT/DELETE | `/sites/{id}` | Đọc/sửa/xóa địa điểm |
| GET/POST | `/artifacts` | Danh sách/tạo hiện vật |
| GET/PATCH/PUT/DELETE | `/artifacts/{id}` | Đọc/sửa/xóa hiện vật |
| GET/POST | `/narrations` | Danh sách/tạo thuyết minh |
| GET/PUT/PATCH/DELETE | `/narrations/{id}` | Đọc/lưu/xóa thuyết minh |
| GET/POST | `/languages` | Danh sách/tạo ngôn ngữ |
| GET/PATCH/PUT/DELETE | `/languages/{id}` | Đọc/sửa/xóa ngôn ngữ |
| GET/POST | `/listens` | Danh sách/tạo lượt nghe |
| GET/PATCH/PUT/DELETE | `/listens/{id}` | Đọc/cập nhật/xóa lượt nghe |
| GET | `/devices` | Danh sách thiết bị |
| GET/POST | `/history` | Danh sách/tạo lịch sử |
| GET/POST | `/likes` | Danh sách/thêm yêu thích |
| DELETE | `/likes/{id}` | Xóa yêu thích |
| GET | `/settings` | Đọc cấu hình dạng object `{ "accessHours": 24 }` |
| PUT/PATCH | `/settings` | Cập nhật cấu hình singleton |

Các query filter dạng `GET /listens?date=2026-10-25&targetType=site&targetId=1&language=vi` được hỗ trợ. Mọi endpoint CRUD này đều cần đăng nhập quản trị: gọi `POST /api/admin/auth/login` để lấy token rồi gửi `Authorization: Bearer <token>` (hoặc header `X-Admin-Key` nếu đã đặt `ADMIN_API_KEY`); không có token thì trả 401.

## Kiến trúc mã nguồn
Gốc package `vn.commentary.backend` chia thành ba nhánh ngang hàng; mỗi nhánh tự chia lớp `controller → service → repository`.
Thư mục test (`src/test/java`) phản chiếu đúng cấu trúc này.

```
vn.commentary.backend
├─ CommentaryBackendApplication
├─ admin/    Phía quản trị
│   ├─ controller/  JsonServerController (CRUD kiểu json-server), AdminAuthController (đăng nhập), AdminAccessController (QR, thiết bị)
│   ├─ service/     JsonResourceService, AdminAuthService, AccessAdminService
│   ├─ repository/  JsonResourceRepository (truy vấn MySQL theo whitelist bảng và metadata cột)
│   ├─ entity/      ColumnDefinition (metadata cột bảng)
│   ├─ security/    AdminAuthInterceptor, AdminTokenService (token HMAC), LoginThrottle, AdminAuthException
│   ├─ exception/   ApiExceptionHandler (bộ xử lý lỗi mặc định của hệ thống)
│   └─ config/      AdminWebConfig (bắt buộc đăng nhập admin), DatabaseCompatibilityInitializer
├─ user/     Phía người dùng cuối (`/api/user/**`), xem docs/USER-API.md
│   ├─ controller/ service/ repository/ model/ dto/ security/ exception/ translation/ config/ util/
└─ common/   Dùng chung cho cả hai phía
    ├─ config/     CorsConfig + LanOnlyCorsConfiguration (CORS chỉ cho mạng nội bộ), JacksonConfig
    ├─ exception/  ResourceNotFoundException
    └─ util/       NetworkPolicy (nhận diện mạng nội bộ), TokenHasher (sinh/băm/so sánh token)
```

Quy ước phụ thuộc: `admin` và `user` không gọi vào nhau trừ việc `admin` quản lý dữ liệu truy cập của `user` (`AccessAdminService` dùng
repository và `AccessTokenService` của `user`); cả hai chỉ dùng thêm `common`.
- `common/config/JacksonConfig.java` khai báo `ObjectMapper` rõ ràng để tránh lỗi bean Jackson.
- `admin/config/DatabaseCompatibilityInitializer.java` chỉ chuyển bảng `likes` từ placeholder sang các cột cần thiết nếu schema hiện tại đúng là bảng placeholder do JSON array rỗng tạo ra; đồng thời tạo setting mặc định nếu bảng rỗng.

## Kiểm tra nhanh
Sau khi app báo `Started CommentaryBackendApplication`, mở `http://localhost:3000/api/user/health` (không cần đăng nhập, trả `{"ok":true,...}`). Các đường dẫn như `/sites`, `/languages` cần token quản trị nên mở trực tiếp trên trình duyệt sẽ trả 401.

Nếu app không khởi động, xem dòng `Caused by` đầu tiên trong log. Nếu báo `Access denied` hoặc `Unknown database`, kiểm tra MySQL và `commentary_db`; đây là lỗi cấu hình/kết nối DB, không phải lỗi biên dịch Java.

## Back-end user (API cho người dùng cuối)
Ngoài API CRUD kiểu json-server ở trên, Backend có thêm nhóm `/api/user/**` (thuyết minh, QR/GPS/Beacon, dịch, lịch sử, yêu thích)
và `/api/admin/access/**` (QR, thiết bị, người đang dùng). Xem **[docs/USER-API.md](docs/USER-API.md)**.

## Test, Docker, CI/CD
```powershell
mvn verify                    # chạy toàn bộ test (dùng H2, không cần MySQL)
docker compose up -d --build  # ở thư mục gốc project: MySQL + schema + dữ liệu mẫu + backend (cổng 3000)
```
Pipeline GitHub Actions nằm ở `.github/workflows/ci.yml`: build + test → smoke test bằng docker compose → đẩy image lên GHCR (nhánh chính) → triển khai SSH (tuỳ chọn).
Schema tự tạo khi khởi động từ `src/main/resources/db/*.sql` (đặt `SQL_INIT_MODE=never` để tắt).
