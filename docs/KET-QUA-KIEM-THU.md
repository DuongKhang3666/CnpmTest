# Kết quả kiểm thử

Tài liệu này tóm tắt các lần kiểm thử đã thực hiện (31/10/2026). Không chứa mật khẩu hay địa chỉ mạng riêng. Phần tương ứng trong
`Prd.docx` là mục "VII. Kiểm thử".

## 1. Tổng quan

| Mức kiểm thử | Công cụ / môi trường | Phạm vi | Kết quả |
|---|---|---|---|
| Kiểm thử tự động Backend | JUnit 5, Mockito, Spring MockMvc, H2; `mvn verify` | 105 test, 18 lớp test | 105 đạt, 0 lỗi, 0 bỏ qua |
| Kiểm thử logic app mobile | Node.js (`npm test` trong `Frontend/mobile`) | Tiệm cận beacon, quét Bluetooth, quyền Android | 26 đạt, 0 lỗi |
| Build giao diện web | Vite (`npm ci`, `vite build`) | Trang quản trị và trang hành khách | Thành công |
| Kiểm thử tích hợp (CI) | GitHub Actions, docker compose (MySQL 8.4 + Backend) | Khởi động, quyền truy cập, đăng nhập, luồng QR -> phiên -> nội dung | Đạt |
| Kiểm thử hệ thống với dịch máy thật | Backend + MySQL 8.0 + LibreTranslate 1.9.6 (Python 3.11), Windows 11 | Dịch, cache, đường dự phòng, bản dịch tay | Đạt (xem mục 4) |

Lần chạy CI gần nhất trên nhánh `main` (commit `1fa69d4`): 4/4 job thành công (Frontend, Backend, Integration, Publish image).

## 2. Kiểm thử tự động Backend (`mvn verify`)
Trong CI chạy trên JDK 17 với H2 trong bộ nhớ, không cần MySQL.

| Nhóm | Lớp test | Số test |
|---|---|---|
| Xác thực quản trị | `AdminAuthServiceTest` 6, `AdminTokenServiceTest` 4, `LoginThrottleTest` 2, `AdminSecurityMockMvcTest` 5 | 17 |
| Mạng nội bộ, CORS và interceptor phiên/quản trị | `NetworkPolicyTest` 3, `LanOnlyCorsConfigurationTest` 3, `InterceptorsTest` 2, `TokenHasherTest` 1 | 9 |
| Xử lý lỗi HTTP | `ApiExceptionHandlerTest` | 5 |
| API người dùng (MockMvc) | `UserApiControllerTest` | 9 |
| Truy vấn dữ liệu (H2) | `UserRepositoriesTest` | 13 |
| Nghiệp vụ | `SessionServiceTest` 11, `NarrationServiceTest` 11, `ContentServiceTest` 8, `TranslationServiceTest` 7, `DiscoveryServiceTest` 6, `CatalogServiceTest` 2, `UtilsTest` 7 | 52 |
| **Tổng** | | **105** |

## 3. Smoke test tích hợp trong CI
Job `Integration` dựng MySQL và Backend bằng docker compose rồi kiểm tra:
1. `GET /api/user/health` trả 200.
2. API quản trị cũ (`/sites`, `DELETE /sites/1`, `/api/admin/access/qr`) không kèm token đều trả 401.
3. Tài khoản demo cũ bị từ chối (401); tài khoản cấu hình riêng cho CI đăng nhập được và nhận token.
4. Có token admin thì đọc được `/sites`; chưa có phiên thì `/api/user/sites` trả 401.
5. Lấy token QR -> `POST /api/user/session/join` -> dùng phiên để đọc `/api/user/sites` thành công.

Job này từng đỏ ở lần chạy đầu vì kịch bản smoke test còn gọi API quản trị không kèm token; đã sửa kịch bản cho khớp cơ chế đăng nhập và chạy lại xanh.

## 4. Kiểm thử dịch thuật với LibreTranslate thật (UC7.4)
Môi trường: Backend chạy trực tiếp, MySQL 8.0 cục bộ có dữ liệu mẫu, LibreTranslate 1.9.6 với `--load-only vi,en,fr,ja,ko,zh,th`.
Luồng: đăng nhập admin -> lấy token QR -> `session/join` -> `GET /api/user/narrations/{loại}/{id}?lang=<mã>`.

### 4.1. Dịch bài thuyết minh thật
Văn bản gốc: địa điểm 1 (Dinh Độc Lập) 735 ký tự, hiện vật 1 (Xe tăng) 707 ký tự.

| Đối tượng | Ngôn ngữ | translated | Độ dài bản dịch | Thời gian (giây) |
|---|---|---|---|---|
| địa điểm 1 | en | true | 738 | 0,96 |
| địa điểm 1 | ja | true | 304 | 0,77 |
| địa điểm 1 | zh | true | 263 | 0,91 |
| địa điểm 1 | fr | true | 759 | 0,89 |
| địa điểm 1 | ko | true | 343 | 0,74 |
| địa điểm 1 | th | true | 727 | 0,84 |
| hiện vật 1 | en | true | 638 | 0,60 |
| hiện vật 1 | ko | true | 307 | 0,56 |

Cả 8 lần đều HTTP 200, không lỗi, chậm nhất 0,96 giây (ngưỡng chờ cấu hình là 30 giây). Không có bản dịch nào còn nguyên tiếng Việt hay bị cắt cụt.

### 4.2. Cache bản dịch
Gọi lại đúng 8 yêu cầu trên: nhanh hơn 10–50 lần (0,017–0,070 giây so với 0,56–0,96 giây), nội dung giống hệt, và bảng `translation_cache`
chỉ thêm đúng 8 dòng (không ghi trùng).

### 4.3. Đường dự phòng khi dịch lỗi
Tắt riêng LibreTranslate rồi gọi các yêu cầu chưa có trong cache:

| Yêu cầu | HTTP | translated | Nội dung | Thời gian (giây) |
|---|---|---|---|---|
| địa điểm 2, `lang=ko` | 200 | false | văn bản gốc tiếng Việt, kèm `translationError` | 1,09 |
| địa điểm 2, `lang=en` | 200 | false | văn bản gốc tiếng Việt, kèm `translationError` | 1,03 |
| địa điểm 1, `lang=en` (đã có cache) | 200 | true | bản dịch cũ | 0,02 |
| địa điểm 1, `lang=xx` (mã không hỗ trợ) | 200 | false | văn bản gốc | - |

Không có lỗi 500; lỗi dịch không được lưu vào cache; nội dung đã cache vẫn phục vụ được khi LibreTranslate ngừng.

### 4.4. Chất lượng dịch máy
Đủ để đoán ý nhưng chưa đủ làm bản thuyết minh chính thức:
- Tên riêng bị dịch từng chữ ("Dinh Độc Lập" ra "Independent Land", tên kiến trúc sư bị dịch sai).
- Tiếng Nhật, Hàn, Trung kém hơn tiếng Anh, Pháp (có bản bỏ mất một mệnh đề); tiếng Thái bị lặp từ và chèn dấu cách giữa các từ.
- Câu rất ngắn dịch dở (ví dụ lời chào).

Biện pháp: bản dịch tay của admin (`narrations.translations`) được ưu tiên trước cache và dịch máy.

### 4.5. Bản dịch tay
File `Backend/sql/sample-translations.sql` nạp 30 bản dịch (địa điểm 1, 2, 3, 6 và hiện vật 1, mỗi mục en/fr/ja/zh-CN/ko/th). Nạp hai lần vẫn cho
kết quả như một lần. Khi LibreTranslate tắt và cache rỗng: 30/30 yêu cầu trả HTTP 200, `translated=true`, nội dung trùng từng ký tự với file SQL,
mỗi lần dưới 1 giây. Bản dịch do AI soạn, cần người bản ngữ rà soát trước khi công bố.

## 5. Lỗi phát hiện và đã xử lý
| # | Lỗi | Xử lý |
|---|---|---|
| 1 | Đường dẫn không tồn tại hoặc sai phương thức HTTP bị trả 500 (nuốt ngoại lệ chuẩn của Spring), và không ghi log | Trả đúng 404/405 (kèm `Allow`); lỗi thật được ghi log; có test |
| 2 | Lỗi 500 và lỗi cơ sở dữ liệu trả chi tiết ngoại lệ/SQL cho client | Chỉ trả thông báo chung, chi tiết ghi vào log; có test |
| 3 | Backend coi `narrations.language` là ngôn ngữ nguồn của văn bản, trong khi giao diện admin dùng nó như ngôn ngữ cấu hình; admin chọn "en" thì người nghe chọn tiếng Anh nhận lại tiếng Việt chưa dịch | Văn bản luôn là tiếng Việt, `language` chỉ là ngôn ngữ cấu hình (mặc định khi nghe, audio); có test và đã kiểm tra trên dữ liệu thật |
| 4 | Smoke test CI cũ gọi API quản trị không token nên đỏ | Cập nhật kịch bản theo cơ chế đăng nhập |

## 6. Chưa kiểm thử và giới hạn
- GPS và Bluetooth beacon trên điện thoại thật (cần bản build riêng, không dùng Expo Go) và Beacon thật.
- Chạy `docker compose --profile translate` (CI chạy compose với dịch tắt); mô hình dịch được thử bằng LibreTranslate chạy trực tiếp.
- Backend mới chỉ chạy tay trên JDK 25 ở máy phát triển; CI dùng JDK 17.
- Camera và GPS trên điện thoại qua HTTP trong LAN có thể bị trình duyệt chặn (cần HTTPS khi không dùng `localhost`).
- Bản dịch tiếng Nhật, Hàn, Thái chưa được người bản ngữ rà soát.
