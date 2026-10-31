/* =====================================================================
   api.jsx — TOÀN BỘ lời gọi API của trang admin nằm ở file này.
   Trỏ tới Back-end Java (Spring Boot, cổng 3000). Mọi lời gọi gắn token đăng nhập admin
   (adminAuth.js); server trả 401 thì quay về trang đăng nhập.
   ===================================================================== */

import { JSON_SERVER_URL } from "./config";
import { authHeaders, notifyAuthLost } from "./adminAuth";

export const API_BASE = JSON_SERVER_URL;

// Địa chỉ web của app user (dùng để tạo link cho mã QR)
export const USER_APP_URL = `${window.location.protocol}//${window.location.hostname}${window.location.port ? `:${window.location.port}` : ""}`;

async function request(path, options = {}) {
  const res = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: { "Content-Type": "application/json", ...authHeaders() },
  });
  if (res.status === 401) {
    notifyAuthLost();
    throw new Error("Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.");
  }
  if (!res.ok) throw new Error(`Lỗi API ${res.status} (${path})`);
  const text = await res.text();
  return text ? JSON.parse(text) : null;
}

const send = (method, body) => ({ method, body: JSON.stringify(body) });
const same = (a, b) => String(a) === String(b);
const now = () => new Date().toISOString();

/* ---------------- Địa điểm & bảo tàng (sites) ---------------- */
export const getSites = () => request("/sites");
export const createSite = (data) =>
  request("/sites", send("POST", { hidden: false, createdAt: now(), updatedAt: now(), ...data }));
export const updateSite = (id, patch) =>
  request(`/sites/${id}`, send("PATCH", { updatedAt: now(), ...patch }));

// Xóa địa điểm -> xóa luôn hiện vật + thuyết minh thuộc địa điểm đó
export async function deleteSite(id) {
  const [artifacts, narrations] = await Promise.all([getArtifacts(), getNarrations()]);
  const arts = artifacts.filter((a) => same(a.siteId, id));
  for (const a of arts) await deleteArtifact(a.id);
  for (const n of narrations) {
    if (n.targetType === "site" && same(n.targetId, id)) await deleteNarration(n.id);
  }
  return request(`/sites/${id}`, { method: "DELETE" });
}

/* ---------------- Hiện vật (artifacts) ---------------- */
export const getArtifacts = () => request("/artifacts");
export const createArtifact = (data) =>
  request("/artifacts", send("POST", { hidden: false, createdAt: now(), updatedAt: now(), ...data }));
export const updateArtifact = (id, patch) =>
  request(`/artifacts/${id}`, send("PATCH", { updatedAt: now(), ...patch }));

export async function deleteArtifact(id) {
  const narrations = await getNarrations();
  for (const n of narrations) {
    if (n.targetType === "artifact" && same(n.targetId, id)) await deleteNarration(n.id);
  }
  return request(`/artifacts/${id}`, { method: "DELETE" });
}

/* ---------------- Thuyết minh (narrations) ----------------
   Mỗi địa điểm / hiện vật có tối đa 1 thuyết minh (targetType + targetId). */
export const getNarrations = () => request("/narrations");

export async function saveNarration(data) {
  const all = await getNarrations();
  const old = all.find(
    (n) => n.targetType === data.targetType && same(n.targetId, data.targetId),
  );
  if (old) return request(`/narrations/${old.id}`, send("PUT", { ...data, id: old.id, createdAt: old.createdAt, updatedAt: now() }));
  return request("/narrations", send("POST", { ...data, createdAt: now(), updatedAt: now() }));
}
export const deleteNarration = (id) => request(`/narrations/${id}`, { method: "DELETE" });

/* ---------------- Ngôn ngữ (languages) ----------------
   { id, name, code, ttsKey, enabled }
   - code   : mã dùng cho Google Dịch (vi, en, zh-CN...)
   - ttsKey : key ngôn ngữ cho TTS (BCP-47: vi-VN, en-US, ja-JP...) */
export const getLanguages = () => request("/languages");
export const createLanguage = (data) => request("/languages", send("POST", data));
export const updateLanguage = (id, patch) => request(`/languages/${id}`, send("PATCH", patch));
export const deleteLanguage = (id) => request(`/languages/${id}`, { method: "DELETE" });

/* ---------------- Lượt nghe audio (listens) ----------------
   Mỗi bản ghi = số lượt nghe của 1 đối tượng trong 1 ngày:
   { id, date: "2026-10-22", targetType, targetId, count }
   Lượt khám phá = tổng count. */
export const getListens = () => request("/listens");

// App user gọi hàm này mỗi lần người dùng bấm phát audio
export async function recordListen({ targetType, targetId, language }) {
  const d = new Date();
  const date = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
  const rows = await request(`/listens?date=${date}&targetType=${targetType}&targetId=${targetId}&language=${encodeURIComponent(language || "vi")}`);
  if (rows.length) return request(`/listens/${rows[0].id}`, send("PATCH", { count: (Number(rows[0].count) || 0) + 1 }));
  return request("/listens", send("POST", { date, targetType, targetId, language: language || "vi", count: 1 }));
}
export const getDevices = () => request("/devices");
export const getHistory = () => request("/history");
export const createHistory = (data) => request("/history", send("POST", data));
export const getLikes = () => request("/likes");
export const createLike = (data) => request("/likes", send("POST", data));
export const deleteLike = (id) => request(`/likes/${id}`, { method: "DELETE" });

/* ---------------- Tiện ích ---------------- */

// Quy tắc hiển thị bên user: chỉ hiện bản đã xuất bản và không bị ẩn
export const isPublic = (item) => item.status === "published" && !item.hidden;

// Tìm địa điểm theo tên -> { lat, lng, address } (OpenStreetMap Nominatim)
export async function searchPlace(query) {
  const url =
    "https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&accept-language=vi&q=" +
    encodeURIComponent(query);
  const res = await fetch(url);
  if (!res.ok) throw new Error("Không tìm kiếm được địa chỉ.");
  const rows = await res.json();
  if (!rows.length) return null;
  return {
    lat: Number(rows[0].lat),
    lng: Number(rows[0].lon),
    address: rows[0].display_name,
  };
}

// Link mã QR: user quét -> app user mở trang phát và tự bật audio theo ngôn ngữ
export const buildQrLink = ({ targetType, targetId, language }) =>
  `${USER_APP_URL}/play?type=${targetType}&id=${targetId}&lang=${language}&autoplay=1`;

// "Xem ứng dụng": link vào thẳng trang hành khách (/user) bằng mã QR hiện tại của hệ thống
export async function getUserAppLink() {
  const res = await request("/api/admin/access/qr");
  if (!res?.token) throw new Error("Không lấy được mã QR để mở ứng dụng.");
  return `${USER_APP_URL}/user?token=${encodeURIComponent(res.token)}`;
}

// Ảnh QR được vẽ trong admin.jsx bằng qrcode.react (không dùng dịch vụ ngoài, chạy được trong mạng LAN)
