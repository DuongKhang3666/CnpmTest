# Hệ thống thuyết minh đa ngôn ngữ — Admin ReactJS + User React Native

## Cấu trúc
- Thư mục gốc: Admin ReactJS (Vite), API dữ liệu JSON Server, Socket.IO server và dữ liệu `db.json` giữ từ project gốc.
- `mobile/`: ứng dụng User React Native dùng Expo, dành cho Android.

## 1. Chạy Admin và backend
Mở terminal ở thư mục gốc:
```powershell
npm install
```
Mở 3 terminal:
```powershell
npm run json
```
```powershell
npm run server
```
```powershell
npm run dev
```
Admin mở ở `http://localhost:5173`. Backend JSON chạy cổng 3000, Socket.IO cổng 3001. Dữ liệu gốc nằm trong `db.json`.

## 2. Cấu hình ứng dụng Android
Điện thoại phải cùng Wi-Fi với máy tính chạy backend. Lấy IPv4 của máy tính bằng `ipconfig` (ví dụ `192.168.1.10`).

Trong thư mục `mobile`, tạo file `.env`:
```env
EXPO_PUBLIC_API_BASE=http://192.168.1.10:3000
```
Thay IP mẫu bằng IPv4 thật của máy bạn. Không dùng `localhost` trong app trên điện thoại.

## 3. Chạy app User
Mở terminal:
```powershell
cd mobile
npm install
npx expo start
```
Có thể quét QR bằng Expo Go cho các chức năng tương thích. Để build ứng dụng Android native:
```powershell
npx expo run:android
```
Cần cài Android Studio, Android SDK và JDK tương thích với Expo/Android Gradle Plugin. Nên chọn JDK 17 nếu phiên bản AGP trong project yêu cầu JDK 17.

## Tính năng trong bản User React Native
- Lấy dữ liệu địa điểm, hiện vật, thuyết minh và ngôn ngữ từ backend hiện có.
- Bản đồ native, marker địa điểm, vùng bán kính GPS từ cấu hình thuyết minh.
- Tìm kiếm nội dung, chọn ngôn ngữ, quét QR, phát giọng nói bằng Expo Speech.
- Ghi lịch sử, lượt nghe và yêu thích vào JSON Server.
- Nút vị trí lấy GPS và thử phát nội dung có cấu hình GPS khi người dùng nhấn nút.

## Lưu ý quan trọng
- Đây là bản tách frontend theo đúng kiến trúc Admin web + User mobile, không thay đổi schema dữ liệu ban đầu.
- Kích hoạt tự động liên tục khi đi vào vùng GPS nền và quét Bluetooth Beacon cần cấu hình quyền nền/native, kiểm thử trên điện thoại thật và chưa được coi là hoàn tất trong bản này.
- Chức năng QR tìm mục tiêu theo ID/tên/mã `narrations[].qr.code`; cần đảm bảo mã QR được tạo từ Admin khớp với dữ liệu backend.
- Thuyết minh đa ngôn ngữ dùng nội dung tương ứng nếu dữ liệu narration có `translations`/`texts`; nếu chưa có, app dùng mô tả đối tượng làm nội dung đọc. Cần bổ sung bản dịch thực tế trong dữ liệu để bảo đảm chính xác.
- Đổi tài khoản demo admin trước khi triển khai thật; không đưa API key bí mật vào app mobile.
