-- Chỉ chạy thủ công nếu bảng likes đang có duy nhất cột _empty_placeholder.
-- Nếu backend đã chạy thành công, DatabaseCompatibilityInitializer tự xử lý trường hợp này.
ALTER TABLE likes DROP COLUMN _empty_placeholder;
ALTER TABLE likes
  ADD COLUMN id BIGINT NOT NULL PRIMARY KEY,
  ADD COLUMN targetType LONGTEXT NOT NULL,
  ADD COLUMN targetId BIGINT NOT NULL,
  ADD COLUMN name LONGTEXT NOT NULL,
  ADD COLUMN createdAt LONGTEXT NOT NULL;
