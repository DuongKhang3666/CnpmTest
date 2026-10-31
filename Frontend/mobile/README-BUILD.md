# Build và chạy app mobile

App có hai cách chạy, khác nhau ở **Bluetooth**:

| Cách chạy | Lệnh | GPS tự động | Quét QR, audio, TTS | Bluetooth beacon |
|---|---|---|---|---|
| Expo Go (thử nhanh) | `npx expo start` | Có | Có | **Không** (hiện thông báo) |
| Bản build riêng (development/preview/release) | `npx expo run:android` hoặc EAS | Có | Có | Có |

Bluetooth dùng thư viện native `react-native-ble-plx` nên Expo Go không có.

## Chuẩn bị một lần
```bash
npm install
```

## Build bản chạy thử trên máy Android (có Bluetooth)
```bash
npx expo run:android          # cắm điện thoại bật USB debugging, hoặc dùng máy ảo
```
Lệnh này tự sinh thư mục `android/` từ `app.json` (quyền Bluetooth/vị trí/camera, cho phép HTTP trong LAN).

## Build file APK/AAB để cài
```bash
npm install -g eas-cli
eas build --platform android --profile preview      # ra file .apk để cài trực tiếp
eas build --platform android --profile production   # ra file .aab để lên Google Play
```

## Việc cần làm trước khi build thật
1. **Google Maps API key (Android).** Mở `app.json`, thay `THAY_BANG_GOOGLE_MAPS_API_KEY` bằng khoá của bạn
   (Google Cloud Console → bật *Maps SDK for Android*). Không có khoá thì bản build riêng hiện bản đồ trống
   (Expo Go vẫn có bản đồ vì dùng khoá của Expo).
2. **Địa chỉ máy chủ.** Đặt `EXPO_PUBLIC_API_BASE` (xem `.env.example`) hoặc quét QR "Đăng nhập App" do Admin tạo —
   QR đã chứa địa chỉ máy chủ.
3. **HTTP trong mạng LAN.** `app.json` bật `usesCleartextTraffic` (Android) và `NSAllowsLocalNetworking` (iOS) để app gọi
   `http://<IP-LAN>:3000`. Hệ thống chỉ dùng trong LAN nên chấp nhận được; nếu sau này đưa lên Internet hãy dùng HTTPS.

## Thuyết minh tự động
- **GPS tự động**: bấm nút "GPS tự động". Vào vùng bán kính đã cấu hình thì app tự thuyết minh; ra khỏi vùng thêm 20 m
  rồi vào lại thì đọc lại. Chỉ hoạt động khi app đang mở (không chạy nền).
- **Bluetooth**: bấm nút "Bluetooth". App quét beacon, khớp theo **địa chỉ MAC** hoặc **tên thiết bị** Admin đã nhập, và
  ước lượng khoảng cách từ cường độ sóng (RSSI) so với bán kính đã cấu hình (mặc định 5 m).
  - Android thấy được MAC. **iPhone không cho ứng dụng đọc MAC** nên với iOS hãy cấu hình theo *tên thiết bị*.
  - Khoảng cách theo RSSI chỉ là ước lượng (tường, người, hướng ăng-ten làm lệch vài mét). Nên thử tại hiện trường và chỉnh bán kính.
  - Android 12+ hỏi quyền "Thiết bị ở gần"; Android 11 trở xuống hỏi quyền Vị trí (bắt buộc của hệ điều hành khi quét BLE).

## Kiểm thử logic (không cần điện thoại)
```bash
npm test
```
