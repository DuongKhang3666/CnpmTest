-- Bảng riêng của Back-end user. Idempotent: có thể chạy mỗi lần khởi động.
-- Thời gian lưu dạng epoch milliseconds (BIGINT) để không phụ thuộc múi giờ của DB.

-- Token trong mã QR cấp quyền cho thiết bị mới (chỉ một token active tại một thời điểm).
CREATE TABLE IF NOT EXISTS access_tokens (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  token VARCHAR(64) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at BIGINT NOT NULL
);

-- Phiên của từng thiết bị; token phiên chỉ lưu dạng băm SHA-256.
CREATE TABLE IF NOT EXISTS device_sessions (
  device_id VARCHAR(64) NOT NULL PRIMARY KEY,
  session_hash VARCHAR(64) NOT NULL,
  name VARCHAR(100),
  browser VARCHAR(100),
  client_type VARCHAR(10) NOT NULL DEFAULT 'web',
  ip VARCHAR(64),
  platform VARCHAR(60),
  language VARCHAR(20),
  screen VARCHAR(20),
  timezone VARCHAR(60),
  user_agent VARCHAR(400),
  first_seen_at BIGINT NOT NULL,
  last_seen_at BIGINT NOT NULL,
  expires_at BIGINT NOT NULL,
  access_hours INT NOT NULL DEFAULT 24
);

-- Cấu hình dạng khoá/giá trị (ví dụ giới hạn số thiết bị App).
CREATE TABLE IF NOT EXISTS app_config (
  config_key VARCHAR(64) NOT NULL PRIMARY KEY,
  config_value VARCHAR(255)
);

-- Bản dịch đã thực hiện, khoá là SHA-256 của (ngôn ngữ đích + văn bản gốc).
CREATE TABLE IF NOT EXISTS translation_cache (
  cache_key VARCHAR(64) NOT NULL PRIMARY KEY,
  target VARCHAR(20) NOT NULL,
  translated_text LONGTEXT NOT NULL,
  created_at BIGINT NOT NULL
);

-- Lịch sử nghe theo thiết bị.
CREATE TABLE IF NOT EXISTS user_history (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  device_id VARCHAR(64) NOT NULL,
  target_type VARCHAR(20) NOT NULL,
  target_id BIGINT NOT NULL,
  name VARCHAR(255),
  language VARCHAR(20),
  method VARCHAR(20),
  played_at BIGINT NOT NULL
);

-- Yêu thích theo thiết bị.
CREATE TABLE IF NOT EXISTS user_likes (
  device_id VARCHAR(64) NOT NULL,
  target_type VARCHAR(20) NOT NULL,
  target_id BIGINT NOT NULL,
  name VARCHAR(255),
  created_at BIGINT NOT NULL,
  PRIMARY KEY (device_id, target_type, target_id)
);
