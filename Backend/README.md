# Commentary Backend — Spring Boot / MySQL

Backend viết lại theo hướng đối tượng, chia lớp `Controller -> Service -> Repository`, tương thích các đường dẫn JSON-server mà `Frontend/src/api.jsx` đang gọi. Mặc định chạy cổng **3000** để không cần sửa `JSON_SERVER_URL` trong `src/config.js` và không đụng đến Socket.IO hiện có ở cổng 3001.

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

Các query filter dạng `GET /listens?date=2026-10-25&targetType=site&targetId=1&language=vi` được hỗ trợ. Endpoint CRUD không yêu cầu JWT/token.

## Kiến trúc mã nguồn
- `controller/JsonServerController.java`: adapter HTTP theo JSON-server.
- `service/JsonResourceService.java`: xử lý nghiệp vụ và hình dạng dữ liệu API.
- `repository/JsonResourceRepository.java`: truy vấn MySQL an toàn theo whitelist bảng và metadata cột.
- `entity/JsonResource.java`, `entity/ColumnDefinition.java`: mô hình dữ liệu.
- `config/JacksonConfig.java`: khai báo `ObjectMapper` rõ ràng để tránh lỗi bean Jackson.
- `config/DatabaseCompatibilityInitializer.java`: chỉ chuyển bảng `likes` từ placeholder sang các cột cần thiết nếu schema hiện tại đúng là bảng placeholder do JSON array rỗng tạo ra; đồng thời tạo setting mặc định nếu bảng rỗng.

## Kiểm tra nhanh
Sau khi app báo `Started CommentaryBackendApplication`, mở:
- `http://localhost:3000/sites`
- `http://localhost:3000/languages`
- `http://localhost:3000/listens?date=2026-10-25&targetType=site&targetId=1&language=vi`

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
