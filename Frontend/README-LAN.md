# Giao diện web — chạy trong mạng LAN

Giao diện web (React/Vite) gồm trang quản trị (`/`) và trang hành khách (`/user`). Cả hai gọi Back-end Java (Spring Boot, cổng 3000);
không còn json-server hay Socket.IO. Cách chạy toàn bộ hệ thống (Docker hoặc không Docker) xem [README gốc](../README.md).

## Chạy giao diện và truy cập từ điện thoại

1. Chạy Back-end (cổng 3000) trước, ví dụ `docker compose up -d --build` ở thư mục gốc.
2. Trong thư mục `Frontend`: `npm install` rồi `npm run dev` (Vite, cổng 5173, bind `0.0.0.0`).
3. Tìm IPv4 của máy chạy server bằng `ipconfig` trên Windows (ví dụ `192.168.1.10`) và đặt `PUBLIC_HOST` trong `.env` của Back-end
   (hoặc để trống để QR lấy địa chỉ trang admin đang mở).
4. Admin mở `http://<IP-LAN>:5173/`, đăng nhập, vào "Mở QR đăng nhập". Link trong QR có dạng `http://<IP-LAN>:5173/user?token=...`.
5. Hành khách (điện thoại cùng Wi-Fi) quét QR → vào `/user`. Trang lưu phiên trong máy và bỏ `?token=` khỏi thanh địa chỉ; tải lại hoặc mở lại
   không cần quét lại cho tới khi hết hạn hoặc admin thu hồi. Không dùng `localhost` trên điện thoại.
6. Cho phép Node.js/Vite (cổng 5173) và Back-end (cổng 3000) qua Windows Defender Firewall trên mạng Private. Không cần mở cổng ra Internet.

Trang web gọi Back-end ở `http://<cùng-máy-với-trang>:3000`. Đổi bằng biến `VITE_USER_API_URL` (tạo `Frontend/.env`, ví dụ
`VITE_USER_API_URL=https://api.example.com`).

## Đăng nhập quản trị

Đăng nhập do Back-end xử lý (`POST /api/admin/auth/login`); không có tài khoản cố định trong giao diện. Đặt tài khoản trong `.env` của Back-end:
`ADMIN_EMAIL`, `ADMIN_PASSWORD`, `ADMIN_TOKEN_SECRET`. Nếu để trống `ADMIN_PASSWORD`, Back-end sinh mật khẩu ngẫu nhiên và in ra log khi khởi động
(tìm dòng `ADMIN_PASSWORD chưa được cấu hình`). Nhập sai 5 lần trong 5 phút sẽ bị khoá theo IP.

Mọi lời gọi của trang admin kèm token (`Authorization: Bearer`); token hết hạn sau `ADMIN_TOKEN_HOURS` giờ (mặc định 12) thì trang tự quay về màn
hình đăng nhập. Tick "Ghi nhớ đăng nhập" để giữ phiên sau khi đóng trình duyệt.

## Quyền truy cập thiết bị

- Thiết bị quét QR được cấp thời hạn mặc định 24 giờ (`USER_ACCESS_HOURS`).
- Trong mục thiết bị, admin có thể đổi thời hạn mặc định, gia hạn theo số giờ hoặc xóa thiết bị (thu hồi quyền, đồng thời đổi QR).
- Khi hết hạn, lần kết nối tiếp theo bị từ chối; hãy quét QR mới hoặc nhờ admin gia hạn.
- Mặc định chỉ thiết bị trong mạng nội bộ vào được (`USER_LAN_ONLY=true`). Chi tiết: [Backend/docs/USER-API.md](../Backend/docs/USER-API.md).

## GPS, QR, dịch và TTS

- Bản đồ chỉ đánh dấu địa điểm; vùng tròn GPS dùng cấu hình GPS của thuyết minh địa điểm, không vẽ hiện vật.
- GPS/camera cần quyền trình duyệt. Với HTTP, một số trình duyệt chỉ cho phép camera/GPS trên `localhost`; điện thoại truy cập bằng IP LAN có thể
  yêu cầu HTTPS. Dùng HTTPS trong triển khai thực tế.
- Quét QR hỗ trợ `BarcodeDetector` trên Chrome/Edge tương thích. Nội dung QR có thể là mã `qr.code` của thuyết minh, link hoặc ID đối tượng.
- TTS dùng `speechSynthesis` của trình duyệt.
- Dịch cho trang hành khách do Back-end xử lý: bản dịch tay của admin → cache → LibreTranslate (xem [README gốc](../README.md#dịch-thuật)).
  Không cần khoá Google Translate.
- Trong màn hình soạn thuyết minh của admin, phần xem trước bản dịch gọi endpoint Google Translate công khai (không chính thức, không cam kết ổn định,
  văn bản được gửi tới Google). Nếu cần bản dịch chính xác, hãy nhập bản dịch tay.

## Ghi chú

- Đảm bảo điện thoại và máy tính cùng mạng, không dùng Wi-Fi khách có client isolation.
- Bộ đếm lượt nghe, lịch sử nghe và yêu thích được Back-end lưu trong MySQL (`listens`, `user_history`, `user_likes`).
