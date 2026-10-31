// Dùng hostname hiện tại để thiết bị trong mạng LAN gọi đúng máy chủ.
const host = window.location.hostname || "localhost";
export const JSON_SERVER_URL = `${window.location.protocol}//${host}:3000`;
