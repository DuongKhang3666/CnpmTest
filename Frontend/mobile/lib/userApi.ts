/* =====================================================================
   lib/userApi.ts — toàn bộ lời gọi tới Back-end user (Spring Boot, /api/user/**) của app.
   Thay cho Socket.IO + json-server cũ:
     - Quét QR "Đăng nhập App"  -> POST /session/join -> nhận sessionToken (lưu AsyncStorage)
     - Mọi lời gọi sau gửi header X-Device-Id + X-Session-Token
     - Heartbeat định kỳ để admin thấy thiết bị đang mở; 401/403 = bị thu hồi / hết hạn
   (Đặt ngoài thư mục app/ vì expo-router coi mọi file trong app/ là một màn hình.)
   ===================================================================== */
import AsyncStorage from "@react-native-async-storage/async-storage";
import { Platform } from "react-native";

type AnyRow = Record<string, any>;

const KEY_API = "narration_api_base";
const KEY_DEVICE = "narration_device_id";
const KEY_SESSION = "narration_session_token";

const DEFAULT_BASE = (process.env.EXPO_PUBLIC_API_BASE || "http://192.168.1.10:3000").replace(/\/$/, "");
let apiBase = DEFAULT_BASE;
let deviceId = "";
let sessionToken = "";

export class ApiError extends Error {
  status: number;
  reason: string;
  constructor(status: number, reason: string, message?: string) {
    super(message || reason || `Lỗi ${status}`);
    this.status = status;
    this.reason = reason;
  }
}

/** Chuẩn hoá địa chỉ máy chủ lấy từ QR: bỏ "/" cuối, đổi cổng Socket.IO cũ (:3001) sang cổng API (:3000). */
export function normalizeBase(url: string): string {
  return String(url || "").trim().replace(/\/$/, "").replace(/:3001$/, ":3000");
}

export const getApiBase = () => apiBase;
export const getDeviceId = () => deviceId;
export const hasSession = () => !!sessionToken;

export async function setApiBase(url: string) {
  apiBase = normalizeBase(url) || DEFAULT_BASE;
  await AsyncStorage.setItem(KEY_API, apiBase);
}

/** Nạp địa chỉ máy chủ, mã thiết bị và phiên đã lưu (gọi một lần khi mở app). */
export async function loadStored() {
  const [savedBase, savedId, savedSession] = await Promise.all([
    AsyncStorage.getItem(KEY_API),
    AsyncStorage.getItem(KEY_DEVICE),
    AsyncStorage.getItem(KEY_SESSION),
  ]);
  if (savedBase) apiBase = normalizeBase(savedBase);
  deviceId = savedId || "";
  sessionToken = savedSession || "";
  return { deviceId, hasSession: !!sessionToken };
}

/** Mã thiết bị 8–64 ký tự [A-Za-z0-9_-], sinh một lần và lưu lại. */
export async function ensureDeviceId() {
  if (!deviceId) {
    deviceId = "app_" + Date.now().toString(36) + "_" + Math.random().toString(36).slice(2, 10);
    await AsyncStorage.setItem(KEY_DEVICE, deviceId);
  }
  return deviceId;
}

export async function clearSession() {
  sessionToken = "";
  await AsyncStorage.multiRemove([KEY_SESSION, "narration_authorized"]);
}

const accessLostListeners = new Set<(reason: string) => void>();
/** Đăng ký hàm được gọi khi server báo thiết bị mất quyền (hết hạn / bị thu hồi). */
export function onAccessLost(fn: (reason: string) => void) {
  accessLostListeners.add(fn);
  return () => {
    accessLostListeners.delete(fn);
  };
}

/** fetch có giới hạn thời gian (React Native không có AbortSignal.timeout). */
async function fetchWithTimeout(url: string, init: RequestInit, timeoutMs: number) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    return await fetch(url, { ...init, signal: controller.signal });
  } finally {
    clearTimeout(timer);
  }
}

async function parse(res: Response): Promise<AnyRow | null> {
  const text = await res.text();
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
}

async function request(path: string, method = "GET", body?: any, timeoutMs = 15000): Promise<any> {
  const headers: Record<string, string> = {
    "X-Device-Id": deviceId,
    "X-Session-Token": sessionToken,
  };
  if (body !== undefined) headers["Content-Type"] = "application/json";
  let res: Response;
  try {
    res = await fetchWithTimeout(
      `${apiBase}/api/user${path}`,
      { method, headers, body: body !== undefined ? JSON.stringify(body) : undefined },
      timeoutMs,
    );
  } catch {
    throw new ApiError(0, "offline", "Không kết nối được máy chủ: " + apiBase);
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

export type JoinResult = { ok: boolean; reason?: string; message?: string };

let joinInFlight: Promise<JoinResult> | null = null;

/**
 * Vào hệ thống bằng token QR (lần đầu) hoặc bằng phiên đã lưu (mở lại app không cần quét lại).
 * Không ném lỗi. Các lời gọi đồng thời được gộp thành một để token phiên không bị ghi đè lẫn nhau.
 */
export function joinSession(qrToken?: string, language = "vi"): Promise<JoinResult> {
  if (!joinInFlight) {
    joinInFlight = doJoin(qrToken, language).finally(() => {
      joinInFlight = null;
    });
  }
  return joinInFlight;
}

async function doJoin(qrToken: string | undefined, language: string): Promise<JoinResult> {
  await ensureDeviceId();
  if (!qrToken && !sessionToken) {
    return { ok: false, reason: "no-token", message: "Thiết bị chưa được cấp quyền. Hãy quét QR do Admin tạo." };
  }
  let res: Response;
  try {
    res = await fetchWithTimeout(
      `${apiBase}/api/user/session/join`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          token: qrToken || null,
          deviceId,
          sessionToken: sessionToken || null,
          info: {
            clientType: "app",
            deviceName: Platform.OS === "ios" ? "iPhone/iPad App" : "Android App",
            platform: Platform.OS,
            language,
            screen: "React Native",
            timezone: Intl.DateTimeFormat().resolvedOptions().timeZone || "",
            userAgent: "ReactNative/" + Platform.OS,
          },
        }),
      },
      10000,
    );
  } catch {
    return { ok: false, reason: "offline", message: "Không kết nối được máy chủ: " + apiBase };
  }
  const data = await parse(res);
  if (res.ok && data?.sessionToken) {
    sessionToken = String(data.sessionToken);
    await AsyncStorage.multiSet([[KEY_SESSION, sessionToken], ["narration_authorized", "1"]]);
    return { ok: true };
  }
  const reason = String(data?.reason || "token-invalid");
  // Phiên đã lưu không còn dùng được: xoá để lần sau không gửi lại vô ích.
  if (reason !== "app-limit" && reason !== "not-lan" && reason !== "bad-device") await clearSession();
  return { ok: false, reason, message: data?.message };
}

export const heartbeat = () => request("/session/heartbeat", "POST", {});

export async function leaveSession() {
  try {
    await fetchWithTimeout(
      `${apiBase}/api/user/session/leave`,
      { method: "POST", headers: { "Content-Type": "application/json", "X-Device-Id": deviceId, "X-Session-Token": sessionToken }, body: "{}" },
      4000,
    );
  } catch {
    /* ignore */
  }
}

/* ---------------- Dữ liệu ---------------- */

const absolute = (url?: string | null) => (url ? `${apiBase}${url}` : "");

/**
 * Gói dữ liệu khởi tạo, quy về đúng hình dạng mà app đã dùng với json-server
 * (img, status, hidden, enabled, id của yêu thích...) để phần giao diện không phải đổi.
 */
export async function loadCatalog() {
  const c = await request("/catalog");
  const place = (x: AnyRow) => ({ ...x, img: absolute(x.imageUrl), status: "published", hidden: false });
  return {
    languages: (c.languages || []).map((l: AnyRow) => ({ ...l, enabled: true })),
    sites: (c.sites || []).map(place),
    artifacts: (c.artifacts || []).map(place),
    narrations: (c.narrations || []).map((n: AnyRow) => ({ ...n, audioUrl: absolute(n.audioUrl) })),
    history: mapHistory(c.history),
    likes: mapLikes(c.likes),
  };
}

const mapHistory = (rows: AnyRow[] | undefined) => (rows || []).map((h) => ({ ...h, date: h.playedAt }));
const mapLikes = (rows: AnyRow[] | undefined) =>
  (rows || []).map((l) => ({ ...l, id: `${l.targetType}:${l.targetId}` }));

export async function getHistory() {
  return mapHistory(await request("/history"));
}

export const recordListen = (targetType: string, targetId: number | string, language: string) =>
  request("/listens", "POST", { targetType, targetId: Number(targetId), language });

export const addHistory = (targetType: string, targetId: number | string, language: string, method: string) =>
  request("/history", "POST", { targetType, targetId: Number(targetId), language, method });

export const addLike = (targetType: string, targetId: number | string) =>
  request("/likes", "POST", { targetType, targetId: Number(targetId) });

export const removeLike = (targetType: string, targetId: number | string) =>
  request(`/likes/${encodeURIComponent(targetType)}/${Number(targetId)}`, "DELETE");

/** Dịch một đoạn văn bản qua máy chủ (có cache DB). Ném lỗi nếu máy chủ không dịch được. */
export async function translateText(text: string, target: string, source = "vi"): Promise<string> {
  const res = await request("/translate", "POST", { texts: [text], target, source }, 40000);
  const item = res?.results?.[0];
  if (!item?.translated || !String(item.text || "").trim()) {
    throw new ApiError(200, "translate-failed", item?.error || "Không dịch được văn bản.");
  }
  return String(item.text);
}

/** Nội dung thuyết minh đã dịch theo ngôn ngữ (ưu tiên bản dịch admin nhập, rồi cache, rồi dịch máy). */
export async function getNarration(targetType: string, targetId: number | string, lang: string) {
  const r = await request(`/narrations/${encodeURIComponent(targetType)}/${Number(targetId)}?lang=${encodeURIComponent(lang)}`);
  // audioUrl chỉ có khi admin tải audio lên và ngôn ngữ nghe trùng ngôn ngữ của bản thu; đổi sang URL đầy đủ để phát.
  return { ...r, audioUrl: absolute(r?.audioUrl) };
}
