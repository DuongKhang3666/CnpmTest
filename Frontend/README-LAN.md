# Hệ thống thuyết minh đa ngôn ngữ — bản cập nhật

## Chạy trên máy tính và truy cập trong mạng LAN

1. Cài Node.js LTS.
2. Mở terminal tại thư mục dự án và chạy `npm install`.
3. Mở 3 terminal riêng:
   - `npm run json` — json-server ở cổng 3000, bind `0.0.0.0`.
   - `npm run server` — Socket.IO và cấp quyền thiết bị ở cổng 3001.
   - `npm run dev` — giao diện Vite ở cổng 5173, bind `0.0.0.0`.
4. Tìm IPv4 của máy chạy server bằng `ipconfig` trên Windows (ví dụ `192.168.1.10`).
5. Trên điện thoại/máy khác cùng Wi-Fi mở `http://192.168.1.10:5173`. Không dùng `localhost` trên điện thoại.
6. Cho phép Node.js/Vite qua Windows Defender Firewall trên mạng Private. Không cần mở cổng ra Internet.

Các cổng cần truy cập trong LAN: TCP 5173, 3000, 3001. Nếu admin đang mở từ chính máy server, đăng nhập và mở QR; QR sẽ tạo URL dùng IP LAN.

## Dữ liệu JSON

`db.json` là dữ liệu ban đầu lấy từ JSON bạn gửi. json-server đọc/ghi các collection `sites`, `artifacts`, `narrations`, `languages`, `listens`, `devices`, `history`, `likes`, `settings`. Dữ liệu được chia sẻ giữa admin/user qua cùng json-server; phần quản lý thời hạn thiết bị realtime đang được lưu trong `src/devices.json`.

## Quyền truy cập thiết bị

- Thiết bị quét QR được cấp thời hạn mặc định 24 giờ.
- Trong mục thiết bị admin có thể đổi thời hạn mặc định, gia hạn theo số giờ hoặc xóa thiết bị (thu hồi quyền).
- Khi hết hạn, lần kết nối tiếp theo bị từ chối; hãy quét QR mới hoặc nhờ admin gia hạn.

## GPS, QR, dịch và TTS

- Bản đồ chỉ đánh dấu địa điểm từ `sites`; vùng tròn GPS dùng cấu hình `narrations[].gps` của địa điểm, không vẽ hiện vật.
- GPS/camera cần quyền trình duyệt. Với HTTP, một số trình duyệt chỉ cho phép camera/GPS trên `localhost`; điện thoại truy cập bằng IP LAN có thể yêu cầu HTTPS. Dùng HTTPS trong triển khai thực tế.
- Quét QR hỗ trợ `BarcodeDetector` trên Chrome/Edge tương thích. Nội dung QR có thể là `narrations[].qr.code`, ID/tên đối tượng hoặc văn bản thuyết minh.
- TTS sử dụng `speechSynthesis` của trình duyệt.
- Google Translate chính thức cần API key và billing được bật. Tạo `.env` ở thư mục gốc với `VITE_GOOGLE_TRANSLATE_API_KEY=YOUR_API_KEY`, rồi khởi động lại Vite. Nếu chưa cấu hình, code dùng endpoint dịch web công khai chỉ để thử nghiệm, không có cam kết ổn định; không nhúng API key bí mật vào ứng dụng công khai.

## Ghi chú

- CORS ở các endpoint frontend cần dùng cùng hostname LAN. Đảm bảo điện thoại và máy tính cùng mạng, không dùng Wi-Fi khách có client isolation.
- Bộ đếm lượt nghe được ghi vào collection `listens`; lịch sử và yêu thích được lưu trong `history` và `likes`.


## Đăng nhập admin
Đăng nhập do Back-end Java xử lý (`POST /api/admin/auth/login`), **không còn tài khoản `123456` cứng trong giao diện**.
Đặt tài khoản trong `.env` của Back-end: `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `ADMIN_TOKEN_SECRET`. Nếu để trống `ADMIN_PASSWORD`, Back-end
sinh mật khẩu ngẫu nhiên và in ra log khi khởi động (tìm dòng `ADMIN_PASSWORD chưa được cấu hình`). Nhập sai 5 lần trong 5 phút sẽ bị khoá theo IP.


## Cập nhật: trang hành khách và admin dùng Back-end Java (không còn Socket.IO / json-server)

Luồng quét QR giờ chạy qua Back-end Java (`/api/user/**`, `/api/admin/access/**`):

1. Chạy Back-end Java (cổng 3000) và giao diện (`npm run dev`, cổng 5173). **Không cần** `npm run json` và `npm run server` cho trang web
   (`npm run server` chỉ còn cần cho ứng dụng mobile cũ).
2. Admin mở trang `http://<IP-LAN>:5173/`, vào "Mở QR đăng nhập". Link trong QR có dạng `http://<IP-LAN>:5173/user?token=...`.
3. Hành khách quét QR → vào `/user` → trang lưu phiên trong máy và bỏ `?token=` khỏi thanh địa chỉ. Tải lại hoặc mở lại không cần quét QR lại
   cho tới khi hết hạn hoặc admin thu hồi.
4. Trang web gọi Back-end ở `http://<cùng-máy-với-trang>:3000`. Đổi bằng biến `VITE_USER_API_URL` (tạo `.env`, ví dụ `VITE_USER_API_URL=https://api.example.com`).
5. Mọi lời gọi của trang admin kèm token đăng nhập (`Authorization: Bearer`); token hết hạn sau `ADMIN_TOKEN_HOURS` giờ (mặc định 12)
   thì trang tự quay về màn hình đăng nhập. Tick "Ghi nhớ đăng nhập" để giữ phiên sau khi đóng trình duyệt.
6. Mặc định chỉ thiết bị trong mạng nội bộ vào được (`USER_LAN_ONLY=true`). Chi tiết: `Backend/docs/USER-API.md`.
