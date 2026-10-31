/* =====================================================================
   userApi.js — toàn bộ lời gọi tới Back-end user (Spring Boot, /api/user/**).
   Thay cho Socket.IO + json-server của trang hành khách:
     - Quét QR  -> POST /session/join  -> nhận sessionToken (lưu localStorage)
     - Mọi lời gọi sau gửi header X-Device-Id + X-Session-Token
     - Heartbeat định kỳ để admin thấy thiết bị đang mở; 401/403 = bị thu hồi/hết hạn
   ===================================================================== */

const PAGE_HOST = typeof window !== "undefined" ? window.location.hostname || "localhost" : "localhost";
const PAGE_PROTOCOL = typeof window !== "undefined" ? window.location.protocol : "http:";

// Mặc định: cùng máy với trang đang mở, cổng 3000 (Back-end Java). Đổi bằng VITE_USER_API_URL.
export const USER_API_BASE = String(
  import.meta.env?.VITE_USER_API_URL || `${PAGE_PROTOCOL}//${PAGE_HOST}:3000`,
).replace(/\/$/, "");
const API = `${USER_API_BASE}/api/user`;

const DEVICE_KEY = "guideDeviceId";
const SESSION_KEY = "guideSessionToken";

const store = {
  get(key) {
    try {
      return localStorage.getItem(key);
    } catch {
      return null;
    }
  },
  set(key, value) {
    try {
      localStorage.setItem(key, value);
    } catch {
      /* chế độ riêng tư: bỏ qua, phiên chỉ sống trong bộ nhớ */
    }
  },
  remove(key) {
    try {
      localStorage.removeItem(key);
    } catch {
      /* ignore */
    }
  },
};
let memorySession = null; // dự phòng khi localStorage không dùng được

export function getDeviceId() {
  let id = store.get(DEVICE_KEY);
  if (!id) {
    id = Array.from(crypto.getRandomValues(new Uint8Array(16)), (x) => x.toString(16).padStart(2, "0")).join("");
    store.set(DEVICE_KEY, id);
  }
  return id;
}

const getSessionToken = () => store.get(SESSION_KEY) || memorySession || "";
const setSessionToken = (token) => {
  memorySession = token;
  store.set(SESSION_KEY, token);
};
export const hasStoredSession = () => !!getSessionToken();
export function clearSession() {
  memorySession = null;
  store.remove(SESSION_KEY);
}

/** Lỗi API có mã lý do ổn định ("expired", "no-session", ...) để giao diện hiển thị đúng thông báo. */
export class ApiError extends Error {
  constructor(status, reason, message) {
    super(message || reason || `Lỗi ${status}`);
    this.status = status;
    this.reason = reason;
  }
}

const accessLostListeners = new Set();
/** Đăng ký hàm được gọi khi server báo thiết bị mất quyền (hết hạn / bị thu hồi). */
export function onAccessLost(fn) {
  accessLostListeners.add(fn);
  return () => accessLostListeners.delete(fn);
}

function authHeaders(extra = {}) {
  return {
    "X-Device-Id": getDeviceId(),
    "X-Session-Token": getSessionToken(),
    ...extra,
  };
}

async function parse(res) {
  const text = await res.text();
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
}

async function request(path, { method = "GET", body, timeoutMs = 15000 } = {}) {
  let res;
  try {
    res = await fetch(`${API}${path}`, {
      method,
      headers: authHeaders(body !== undefined ? { "Content-Type": "application/json" } : {}),
      body: body !== undefined ? JSON.stringify(body) : undefined,
      signal: AbortSignal.timeout(timeoutMs),
    });
  } catch (e) {
    throw new ApiError(0, "offline", "Không kết nối được máy chủ.");
  }
  const data = await parse(res);
  if (!res.ok) {
    const reason = data?.reason || (res.status === 401 || res.status === 403 ? "no-session" : "error");
    if (res.status === 401 || res.status === 403) {
      accessLostListeners.forEach((fn) => fn(reason));
    }
    throw new ApiError(res.status, reason, data?.message || data?.error);
  }
  return data;
}

/* ---------------- Phiên truy cập ---------------- */

/**
 * Vào hệ thống bằng token QR (lần đầu) hoặc bằng phiên đã lưu (mở lại trang không cần quét lại).
 * Không ném lỗi: trả { ok:true } hoặc { ok:false, reason, message }.
 */
let joinInFlight = null;
export function joinSession(qrToken) {
  // Gộp các lời gọi đồng thời (React StrictMode chạy hiệu ứng 2 lần, hoặc nhiều tab mở cùng lúc)
  // thành một request để token phiên không bị ghi đè lẫn nhau.
  if (!joinInFlight) {
    joinInFlight = doJoin(qrToken).finally(() => {
      joinInFlight = null;
    });
  }
  return joinInFlight;
}

async function doJoin(qrToken) {
  const hasSession = hasStoredSession();
  if (!qrToken && !hasSession) {
    return { ok: false, reason: "no-token", message: "Bạn cần quét QR đăng nhập do quản trị viên cung cấp." };
  }
  let res;
  try {
    res = await fetch(`${API}/session/join`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        token: qrToken || null,
        deviceId: getDeviceId(),
        sessionToken: getSessionToken() || null,
        info: {
          userAgent: navigator.userAgent,
          platform: navigator.platform,
          language: navigator.language,
          screen: `${screen.width}x${screen.height}`,
          timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
          clientType: "web",
        },
      }),
      signal: AbortSignal.timeout(8000),
    });
  } catch {
    return { ok: false, reason: "offline", message: "Không kết nối được máy chủ." };
  }
  const data = await parse(res);
  if (res.ok && data?.sessionToken) {
    setSessionToken(data.sessionToken);
    return { ok: true, expiresAt: data.expiresAt, accessHours: data.accessHours };
  }
  const reason = data?.reason || "token-invalid";
  // Phiên lưu trong máy không còn dùng được: xoá để lần sau không gửi lại vô ích.
  if (reason !== "app-limit" && reason !== "not-lan" && reason !== "bad-device") clearSession();
  return { ok: false, reason, message: data?.message };
}

export const heartbeat = () => request("/session/heartbeat", { method: "POST", body: {} });

/** Báo rời trang (chạy được cả khi tab đang đóng nhờ keepalive). */
export function leaveSession() {
  try {
    fetch(`${API}/session/leave`, {
      method: "POST",
      headers: authHeaders({ "Content-Type": "application/json" }),
      body: "{}",
      keepalive: true,
    }).catch(() => {});
  } catch {
    /* ignore */
  }
}

/* ---------------- Dữ liệu ---------------- */

const absolute = (url) => (url ? `${USER_API_BASE}${url}` : "");

/**
 * Gói dữ liệu khởi tạo, quy về đúng hình dạng mà user.jsx đã dùng với json-server
 * (img, status, hidden, enabled, id của yêu thích...) để phần giao diện không phải đổi.
 */
export async function loadCatalog() {
  const c = await request("/catalog");
  const place = (x) => ({ ...x, img: absolute(x.imageUrl), status: "published", hidden: false });
  return {
    languages: (c.languages || []).map((l) => ({ ...l, enabled: true })),
    sites: (c.sites || []).map(place),
    artifacts: (c.artifacts || []).map(place),
    narrations: (c.narrations || []).map((n) => ({ ...n, audioUrl: absolute(n.audioUrl) })),
    listens: c.listens || [],
    history: (c.history || []).map((h) => ({ ...h, date: h.playedAt })),
    likes: (c.likes || []).map((l) => ({ ...l, id: `${l.targetType}:${l.targetId}` })),
  };
}

export const recordListen = ({ targetType, targetId, language }) =>
  request("/listens", { method: "POST", body: { targetType, targetId: Number(targetId), language } });

export const addHistory = ({ targetType, targetId, language, method }) =>
  request("/history", { method: "POST", body: { targetType, targetId: Number(targetId), language, method } });

export const addLike = ({ targetType, targetId }) =>
  request("/likes", { method: "POST", body: { targetType, targetId: Number(targetId) } });

export const removeLike = ({ targetType, targetId }) =>
  request(`/likes/${encodeURIComponent(targetType)}/${Number(targetId)}`, { method: "DELETE" });

/** Dịch một đoạn văn bản (server có cache DB nên các lần sau rất nhanh). */
export async function translateText(text, target, source = "vi") {
  const res = await request("/translate", {
    method: "POST",
    body: { texts: [text], target, source },
    timeoutMs: 40000,
  });
  const item = res?.results?.[0];
  if (!item?.translated || !String(item.text || "").trim()) {
    throw new ApiError(200, "translate-failed", item?.error || "Không dịch được văn bản.");
  }
  return item.text;
}

/** Dịch nhiều đoạn ngắn trong một lần gọi (tối đa 50 đoạn, mỗi đoạn <= 10.000 ký tự). Phần tử không dịch được là null. */
export async function translateBatch(texts, target, source = "vi") {
  const res = await request("/translate", {
    method: "POST",
    body: { texts, target, source },
    timeoutMs: 120000,
  });
  return (res?.results || []).map((it) => (it?.translated && String(it.text || "").trim() ? it.text : null));
}

/**
 * Thuyết minh của một địa điểm/hiện vật theo ngôn ngữ: bản admin nhập > cache > dịch máy.
 * Có audioUrl (đã thành URL đầy đủ) khi admin tải audio lên và ngôn ngữ nghe trùng ngôn ngữ của bản thu.
 */
export async function getNarration(targetType, targetId, lang) {
  const r = await request(
    `/narrations/${encodeURIComponent(targetType)}/${Number(targetId)}?lang=${encodeURIComponent(lang || "")}`,
    { timeoutMs: 60000 },
  );
  return { ...r, audioUrl: absolute(r?.audioUrl) };
}
