# Hệ thống Thuyết minh Đa ngôn ngữ - Backend Admin & Dashboard

Backend REST API cho phần Quản trị (Admin) và Dashboard của dự án "Khám phá Việt Nam".

## 🚀 Công nghệ

- **Java 21**, **Spring Boot 4.1.1**, **Maven**
- **Spring Data JPA** (Hibernate)
- **Spring Security** + **JWT** (jjwt 0.12.6)
- **SpringDoc OpenAPI** (Swagger UI)
- **H2 Database** (dev) / **MySQL** / **PostgreSQL** (prod)
- **Lombok** (optional)

## 📦 Cấu trúc Package

```
backend.commentary
├── config           # Cấu hình (Security, OpenAPI, Web, AdminUserInitializer)
├── controller       # REST Controllers (Auth, AdminPlace, AdminArtifact, AdminMedia, AdminDashboard)
├── dto
│   ├── request      # DTO request (LoginRequest, PlaceRequest, ArtifactRequest, StatusUpdateRequest)
│   └── response     # DTO response (AuthResponse, PlaceResponse, ArtifactResponse, PageResponse, DashboardStatsResponse, ErrorResponse)
├── entity           # JPA Entities (AdminUser, Place, Artifact)
│   └── enums        # Enum (ContentStatus, PlaceType, PlaceCategory)
├── exception        # Exception & GlobalExceptionHandler
├── repository       # JPA Repositories
├── security         # JWT Utils, Filter, EntryPoint, UserDetailsService
└── service          # Business Logic Services
```

## ⚙️ Cài đặt & Chạy

### Yêu cầu
- JDK 21+
- Maven 3.9+ (hoặc dùng `./mvnw` wrapper)

### Chạy nhanh (Dev - H2 in-memory)

```bash
cd Backend
./mvnw spring-boot:run
```

- App chạy tại: **http://localhost:8080**
- Swagger UI: **http://localhost:8080/swagger-ui.html**
- H2 Console: **http://localhost:8080/h2-console** (JDBC URL: `jdbc:h2:mem:commentary_db`, user: `sa`, pass: *rỗng*)

### Chạy Production (MySQL/PostgreSQL)

```bash
# Set biến môi trường
export SPRING_PROFILES_ACTIVE=prod
export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/commentary_db?useSSL=false&serverTimezone=UTC
export SPRING_DATASOURCE_USERNAME=your_user
export SPRING_DATASOURCE_PASSWORD=your_pass
export JWT_SECRET=your-256-bit-secret-key-here
export ADMIN_EMAIL=admin@yourdomain.com
export ADMIN_PASSWORD=strong_password

./mvnw spring-boot:run
```

### Build JAR

```bash
./mvnw clean package -DskipTests
java -jar target/commentary-0.0.1-SNAPSHOT.jar
```

## 🔐 Xác thực

### Đăng nhập
```bash
POST /api/auth/login
Content-Type: application/json

{
  "email": "admin@gmail.com",
  "password": "123456"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "id": 1,
  "email": "admin@gmail.com",
  "role": "ROLE_ADMIN"
}
```

### Sử dụng Token
Thêm header vào mọi request `/api/admin/**`:
```
Authorization: Bearer <token>
```

### Tài khoản mặc định (Dev)
- Email: `admin@gmail.com`
- Mật khẩu: `123456`
- Được seed tự động khi chạy lần đầu

## 📚 API Endpoints

### 1. Địa điểm & Bảo tàng (`/api/admin/places`)

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/api/admin/places` | Danh sách (tìm kiếm `q`, lọc `type`, `status`, phân trang `page`, `size`) |
| GET | `/api/admin/places/{id}` | Chi tiết |
| POST | `/api/admin/places` | Tạo mới (tự sinh mã `DD-xxx` hoặc `BT-xxx`) |
| PUT | `/api/admin/places/{id}` | Cập nhật toàn bộ |
| DELETE | `/api/admin/places/{id}` | Xóa (cascade xóa Artifacts) |
| PATCH | `/api/admin/places/{id}/status` | Đổi trạng thái |

**Query params danh sách:**
- `q` - Từ khóa (tên/mã)
- `type` - `Địa điểm` hoặc `Bảo tàng`
- `status` - `Đã xuất bản`, `Chờ duyệt`, `Bản nháp`
- `page` - Trang (mặc định 1)
- `size` - Kích thước trang (mặc định 10)

**Ví dụ tạo Place:**
```json
POST /api/admin/places
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Hồ Gươm",
  "type": "Địa điểm",
  "category": "Danh lam thắng cảnh",
  "description": "Hồ Hoàn Kiếm là biểu tượng của thủ đô Hà Nội.",
  "address": "Quận Hoàn Kiếm, Hà Nội",
  "latitude": 21.0285,
  "longitude": 105.8542,
  "status": "Đã xuất bản",
  "image": "https://example.com/main.jpg",
  "extraImages": ["https://example.com/extra1.jpg", "https://example.com/extra2.jpg"]
}
```

### 2. Hiện vật (`/api/admin/artifacts`)

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/api/admin/artifacts` | Danh sách (tìm kiếm `q`, lọc `placeId`, `status`, phân trang) |
| GET | `/api/admin/artifacts/{id}` | Chi tiết |
| POST | `/api/admin/artifacts` | Tạo mới (tự sinh mã `HV-xxx`) |
| PUT | `/api/admin/artifacts/{id}` | Cập nhật |
| DELETE | `/api/admin/artifacts/{id}` | Xóa |
| PATCH | `/api/admin/artifacts/{id}/status` | Đổi trạng thái |

**Ví dụ tạo Artifact:**
```json
POST /api/admin/artifacts
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Xe Tăng 390",
  "description": "Xe tăng húc đổ cổng Dinh Độc Lập ngày 30/4/1975.",
  "placeId": 1,
  "status": "Đã xuất bản",
  "image": "https://example.com/tank.jpg",
  "latitude": 10.7770,
  "longitude": 106.6953
}
```

### 3. Upload Ảnh (`/api/admin/media`)

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `/api/admin/media` | Upload nhiều (form field: `files`, max 10 file) |
| POST | `/api/admin/media/single` | Upload 1 file (form field: `file`) |
| DELETE | `/api/admin/media?url=` | Xóa ảnh theo URL |

**Quy tắc:**
- Chỉ nhận: JPEG, PNG, WebP, GIF
- Tối đa 10MB/file
- Tối đa 10 file/lần (1 chính + 9 phụ)
- Trả về URL dạng: `/uploads/2026/10/22/abc123.jpg`
- Truy cập ảnh: `http://localhost:8080/uploads/2026/10/22/abc123.jpg`

### 4. Dashboard (`/api/admin/dashboard`)

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/api/admin/dashboard/stats` | Thống kê tổng quan |

**Response Dashboard:**
```json
{
  "totalPlaces": 25,
  "totalArtifacts": 120,
  "totalAdmins": 1,
  "totalDiaDiem": 18,
  "totalBaoTang": 7,
  "publishedCount": 100,
  "pendingCount": 30,
  "draftCount": 15,
  "pendingApproval": 30,
  "recentItems": [
    {"id": 5, "type": "Hiện vật", "name": "Trống đồng", "status": "Đã xuất bản", "createdAt": "2026-10-22T10:30:00"}
  ],
  "dailyCreatedLast7Days": {"2026-10-16": 2, "2026-10-17": 5, ...},
  "dailyCreatedLast30Days": {"2026-09-22": 1, ...},
  "statusDistribution": {"Đã xuất bản": 100, "Chờ duyệt": 30, "Bản nháp": 15}
}
```

## 📝 Mã trạng thái (ContentStatus)

| Enum | Giá trị JSON | Mô tả |
|------|--------------|-------|
| `PUBLISHED` | `"Đã xuất bản"` | Đã duyệt, hiển thị công khai |
| `PENDING` | `"Chờ duyệt"` | Đang chờ admin duyệt |
| `DRAFT` | `"Bản nháp"` | Chưa hoàn thiện, lưu nháp |

## 🏷️ Loại & Phân loại Place

| PlaceType | Prefix mã | PlaceCategory |
|-----------|-----------|---------------|
| `Địa điểm` | `DD-` | `Địa điểm du lịch`, `Di tích lịch sử`, `Di sản văn hóa`, `Danh lam thắng cảnh` |
| `Bảo tàng` | `BT-` | `Bảo tàng` |

## 🧪 Chạy Test

```bash
# Chạy tất cả test
./mvnw test

# Chạy test cụ thể
./mvnw test -Dtest=AuthControllerTest
./mvnw test -Dtest=AdminPlaceControllerTest
./mvnw test -Dtest=AdminArtifactControllerTest
./mvnw test -Dtest=AdminMediaControllerTest
./mvnw test -Dtest=AdminDashboardControllerTest
./mvnw test -Dtest=MediaServiceTest
```

## 📁 Cấu hình quan trọng (`application.properties`)

```properties
# Profile aktif (dev/prod)
spring.profiles.active=${SPRING_PROFILES_ACTIVE:dev}

# Port server
server.port=${SERVER_PORT:8080}

# Upload config
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=100MB
app.upload.dir=${UPLOAD_DIR:uploads}

# JWT (lấy từ env, không hardcode)
app.jwt.secret=${JWT_SECRET:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}
app.jwt.expiration-ms=${JWT_EXPIRATION_MS:86400000}  # 24h

# Admin seed
app.admin.email=${ADMIN_EMAIL:admin@gmail.com}
app.admin.password=${ADMIN_PASSWORD:123456}
```

## 🔧 Mở rộng sau này

Schema đã sẵn sàng cho:
- **Commentary** (Thuyết minh): liên kết Place/Artifact, nhiều ngôn ngữ
- **CommentaryTranslation**: nội dung đa ngôn ngữ (vi, en, fr, jp, kr, cn...)
- **Audit Log**: bảng `admin_audit_logs` (ai, hành động, entity, thời gian)
- **Soft Delete**: thêm cột `deleted_at` thay vì xóa hẳn

## 📄 License

Internal project - Hệ thống thuyết minh đa ngôn ngữ "Khám phá Việt Nam".