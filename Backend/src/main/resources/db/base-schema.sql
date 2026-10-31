-- Schema nền của hệ thống (các bảng mà API admin đang dùng, tương thích json-server/db.json).
-- Dùng CREATE TABLE IF NOT EXISTS nên chạy lại nhiều lần hoặc trên DB đã có dữ liệu đều an toàn.
-- Các cột JSON (extraImages, gps, bluetooth, qr, translations) lưu dạng LONGTEXT chứa chuỗi JSON.

CREATE TABLE IF NOT EXISTS sites (
  id BIGINT NOT NULL PRIMARY KEY,
  name LONGTEXT,
  classify VARCHAR(100),
  city VARCHAR(255),
  address LONGTEXT,
  description LONGTEXT,
  img LONGTEXT,
  extraImages LONGTEXT,
  latitude DOUBLE,
  longitude DOUBLE,
  status VARCHAR(30) NOT NULL DEFAULT 'draft',
  hidden BOOLEAN NOT NULL DEFAULT FALSE,
  createdAt VARCHAR(40),
  updatedAt VARCHAR(40)
);

CREATE TABLE IF NOT EXISTS artifacts (
  id BIGINT NOT NULL PRIMARY KEY,
  siteId BIGINT,
  name LONGTEXT,
  address LONGTEXT,
  description LONGTEXT,
  img LONGTEXT,
  latitude DOUBLE,
  longitude DOUBLE,
  status VARCHAR(30) NOT NULL DEFAULT 'draft',
  hidden BOOLEAN NOT NULL DEFAULT FALSE,
  createdAt VARCHAR(40),
  updatedAt VARCHAR(40)
);

CREATE TABLE IF NOT EXISTS narrations (
  id BIGINT NOT NULL PRIMARY KEY,
  targetType VARCHAR(20) NOT NULL,
  targetId BIGINT NOT NULL,
  image LONGTEXT,
  text LONGTEXT,
  useDescription BOOLEAN NOT NULL DEFAULT TRUE,
  ttsEnabled BOOLEAN NOT NULL DEFAULT TRUE,
  audioEnabled BOOLEAN NOT NULL DEFAULT FALSE,
  language VARCHAR(20) NOT NULL DEFAULT 'vi',
  speed DOUBLE NOT NULL DEFAULT 1,
  audioName VARCHAR(255),
  audioData LONGTEXT,
  gps LONGTEXT,
  bluetooth LONGTEXT,
  qr LONGTEXT,
  translations LONGTEXT,
  createdAt VARCHAR(40),
  updatedAt VARCHAR(40)
);

CREATE TABLE IF NOT EXISTS languages (
  id BIGINT NOT NULL PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  code VARCHAR(20) NOT NULL,
  ttsKey VARCHAR(20),
  enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS listens (
  id BIGINT NOT NULL PRIMARY KEY,
  date VARCHAR(20) NOT NULL,
  targetType VARCHAR(20) NOT NULL,
  targetId BIGINT NOT NULL,
  language VARCHAR(20) NOT NULL DEFAULT 'vi',
  count BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS devices (
  id BIGINT NOT NULL PRIMARY KEY,
  deviceId VARCHAR(64),
  info LONGTEXT,
  name VARCHAR(100),
  browser VARCHAR(100),
  ip VARCHAR(64),
  firstSeen VARCHAR(40),
  lastSeen VARCHAR(40),
  expiresAt VARCHAR(40),
  online BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS history (
  id BIGINT NOT NULL PRIMARY KEY,
  date VARCHAR(40),
  targetType VARCHAR(20),
  targetId BIGINT,
  name LONGTEXT,
  language VARCHAR(20),
  method VARCHAR(20),
  playedAt VARCHAR(40)
);

CREATE TABLE IF NOT EXISTS likes (
  id BIGINT NOT NULL PRIMARY KEY,
  targetType LONGTEXT NOT NULL,
  targetId BIGINT NOT NULL,
  name LONGTEXT NOT NULL,
  createdAt LONGTEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS settings (
  accessHours INT NOT NULL DEFAULT 24
);
