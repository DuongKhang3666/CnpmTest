/* =====================================================================
   adminAuth.js — đăng nhập quản trị với Back-end Java (/api/admin/auth).
   - Đăng nhập -> nhận token (ký HMAC, hết hạn sau ADMIN_TOKEN_HOURS giờ).
   - "Ghi nhớ đăng nhập" bật: lưu localStorage (còn sau khi đóng trình duyệt); tắt: sessionStorage.
   - Mọi lời gọi API quản trị gửi header Authorization: Bearer <token> (xem authHeaders()).
   - Server trả 401 (token hết hạn / sai) -> xoá token và báo cho giao diện quay về trang đăng nhập.
   ===================================================================== */
import { JSON_SERVER_URL } from "./config";

const KEY = "adminAuthToken";
const BASE = JSON_SERVER_URL;

const safe = (fn, fallback = null) => {
  try {
    return fn();
  } catch {
    return fallback; // chế độ riêng tư / bị chặn lưu trữ
  }
};

let memoryToken = ""; // dự phòng khi trình duyệt không cho dùng storage

export const getAdminToken = () =>
  memoryToken || safe(() => sessionStorage.getItem(KEY)) || safe(() => localStorage.getItem(KEY)) || "";

function saveToken(token, remember) {
  memoryToken = token;
  safe(() => (remember ? localStorage : sessionStorage).setItem(KEY, token));
  safe(() => (remember ? sessionStorage : localStorage).removeItem(KEY));
}

export function clearAdminToken() {
  memoryToken = "";
  safe(() => sessionStorage.removeItem(KEY));
  safe(() => localStorage.removeItem(KEY));
}

/** Header xác thực để gắn vào mọi lời gọi API quản trị. */
export const authHeaders = () => {
  const token = getAdminToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
};

const lostListeners = new Set();
/** Đăng ký hàm chạy khi server từ chối token (hết hạn / bị đổi khoá). Trả về hàm huỷ đăng ký. */
export function onAdminAuthLost(fn) {
  lostListeners.add(fn);
  return () => lostListeners.delete(fn);
}

/** Gọi khi một lời gọi API nhận 401: xoá token và báo giao diện. */
export function notifyAuthLost() {
  if (!getAdminToken()) return;
  clearAdminToken();
  lostListeners.forEach((fn) => fn());
}

async function post(path, body) {
  let res;
  try {
    res = await fetch(`${BASE}${path}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
      signal: AbortSignal.timeout(10000),
    });
  } catch {
    throw new Error(`Không kết nối được máy chủ ${BASE}. Hãy kiểm tra Back-end đã chạy chưa.`);
  }
  const text = await res.text();
  let data = null;
  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    /* ignore */
  }
  if (!res.ok) throw new Error(data?.message || data?.error || `Lỗi máy chủ ${res.status}`);
  return data;
}

/** Đăng nhập; ném Error với thông báo tiếng Việt nếu sai tài khoản / bị khoá. */
export async function adminLogin(email, password, remember) {
  const data = await post("/api/admin/auth/login", { email, password });
  saveToken(data.token, !!remember);
  return { email: data.email, expiresAt: data.expiresAt };
}

/** Kiểm tra token đã lưu còn dùng được không (khi mở lại trang). Không ném lỗi. */
export async function checkAdminSession() {
  const token = getAdminToken();
  if (!token) return false;
  try {
    const res = await fetch(`${BASE}/api/admin/auth/me`, {
      headers: authHeaders(),
      signal: AbortSignal.timeout(8000),
    });
    if (res.ok) return true;
    if (res.status === 401) clearAdminToken();
    return false;
  } catch {
    return false; // máy chủ chưa chạy: để người dùng đăng nhập lại khi có mạng
  }
}
