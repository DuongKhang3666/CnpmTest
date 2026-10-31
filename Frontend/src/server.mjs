import http from "node:http";
import os from "node:os";
import fs from "node:fs";
import crypto from "node:crypto";
import { fileURLToPath } from "node:url";
import { Server } from "socket.io";

const PORT = Number(process.env.PORT) || 3001;
// Mặc định chỉ cho phép admin kết nối từ CHÍNH máy chủ này (tránh điện thoại tự lấy mã QR).
// Đặt ALLOW_REMOTE_ADMIN=1 nếu bạn muốn mở trang admin từ máy khác trong mạng LAN.
const ALLOW_REMOTE_ADMIN = process.env.ALLOW_REMOTE_ADMIN === "1";
const DATA_FILE = fileURLToPath(new URL("./devices.json", import.meta.url));

/* ---------- Tiện ích mạng ---------- */
const cleanIp = (ip = "") => ip.replace(/^::ffff:/, "");
const isLoopback = (ip) => ip === "::1" || ip.startsWith("127.");
const ipToInt = (ip) =>
  ip.split(".").reduce((acc, part) => (acc << 8) + Number(part), 0) >>> 0;

const isPrivateIPv4 = (ip) => {
  const p = ip.split(".").map(Number);
  if (p.length !== 4 || p.some((n) => Number.isNaN(n))) return false;
  return (
    p[0] === 10 ||
    (p[0] === 172 && p[1] >= 16 && p[1] <= 31) ||
    (p[0] === 192 && p[1] === 168)
  );
};

function lanInterfaces() {
  const result = [];
  for (const [name, list] of Object.entries(os.networkInterfaces())) {
    for (const i of list || []) {
      if (i.family === "IPv4" && !i.internal && isPrivateIPv4(i.address)) {
        result.push({ name, address: i.address, netmask: i.netmask });
      }
    }
  }
  // Ưu tiên 192.168.x.x (Wi-Fi/LAN gia đình, văn phòng) lên đầu
  const rank = (a) => (a.startsWith("192.168.") ? 0 : a.startsWith("10.") ? 1 : 2);
  return result.sort((a, b) => rank(a.address) - rank(b.address));
}

const isSelf = (ip) =>
  isLoopback(ip) || lanInterfaces().some((i) => i.address === ip);

// Thiết bị hợp lệ: chính máy chủ, hoặc IP riêng nằm cùng subnet với 1 card mạng của máy chủ
function isLanClient(ip) {
  if (isSelf(ip)) return true;
  if (!isPrivateIPv4(ip)) return false;
  return lanInterfaces().some(
    (i) =>
      ((ipToInt(ip) & ipToInt(i.netmask)) >>> 0) ===
      ((ipToInt(i.address) & ipToInt(i.netmask)) >>> 0),
  );
}

/* ---------- Nhận diện thiết bị từ User-Agent ---------- */
function describeDevice(ua = "") {
  let osName = "Thiết bị";
  if (/iPhone/.test(ua)) osName = "iPhone";
  else if (/iPad/.test(ua)) osName = "iPad";
  else if (/Android/.test(ua)) osName = "Android";
  else if (/Windows/.test(ua)) osName = "Windows";
  else if (/Mac OS X|Macintosh/.test(ua)) osName = "macOS";
  else if (/Linux/.test(ua)) osName = "Linux";

  const model = (ua.match(/Android [\d.]+; ([^;)]+)/) || [])[1];
  if (model && model.trim().length > 1 && model.trim() !== "K") {
    osName = model.trim();
  }

  let browser = "Trình duyệt";
  if (/Edg\//.test(ua)) browser = "Edge";
  else if (/OPR\/|Opera/.test(ua)) browser = "Opera";
  else if (/CriOS|Chrome\//.test(ua)) browser = "Chrome";
  else if (/FxiOS|Firefox\//.test(ua)) browser = "Firefox";
  else if (/Safari\//.test(ua)) browser = "Safari";

  return { name: osName, browser };
}

const str = (v, max = 200) => (typeof v === "string" ? v.slice(0, max) : "");

/* ---------- Trạng thái ---------- */
let qrToken = crypto.randomBytes(16).toString("hex");
let accessHours = 24;
let maxAppDevices = 5; // số thiết bị App được phép đăng nhập đồng thời
const authorized = new Set(); // deviceId đã quét QR hợp lệ (mất khi khởi động lại máy chủ)
const live = new Map(); //       deviceId -> Set(socket.id) đang mở giao diện user

let devices = {}; // lịch sử các thiết bị đã quét (lưu file)
try {
  devices = JSON.parse(fs.readFileSync(DATA_FILE, "utf8")) || {};
} catch {
  devices = {};
}

// Bổ sung thời hạn cho các bản ghi thiết bị từ phiên bản cũ.
for (const device of Object.values(devices)) {
  if (!device.expiresAt) {
    const start = device.firstScanAt || device.connectedAt || new Date().toISOString();
    device.expiresAt = new Date(new Date(start).getTime() + accessHours * 3600000).toISOString();
    device.accessHours = accessHours;
  }
}

let saveTimer = null;
async function syncDeviceToJsonServer(deviceId, device) {
  try {
    const base = "http://127.0.0.1:3000/devices";
    const response = await fetch(base);
    if (!response.ok) return;
    const rows = await response.json();
    const old = rows.find((row) => String(row.deviceId || row.id) === String(deviceId));
    if (!device) {
      if (old?.id != null) await fetch(`${base}/${old.id}`, { method: "DELETE" });
      return;
    }
    const body = {
      deviceId,
      info: { userAgent: device.userAgent || "", platform: device.platform || "", language: device.language || "", screen: device.screen || "", timezone: device.timezone || "" },
      name: device.name || "Thiết bị", browser: device.browser || "",
      ip: device.ip || "", firstSeen: device.firstScanAt, lastSeen: device.lastSeenAt,
      expiresAt: device.expiresAt, online: live.has(deviceId)
    };
    if (old?.id != null) await fetch(`${base}/${old.id}`, { method: "PATCH", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) });
    else await fetch(base, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) });
  } catch { /* json-server có thể chưa khởi động; dữ liệu vẫn được lưu local */ }
}
function saveDevices() {
  clearTimeout(saveTimer);
  saveTimer = setTimeout(() => {
    fs.writeFile(DATA_FILE, JSON.stringify(devices, null, 2), () => {
      for (const [id, device] of Object.entries(devices)) syncDeviceToJsonServer(id, device);
    });
  }, 400);
}

function snapshot() {
  const list = Object.values(devices)
    .map((d) => ({ ...d, online: live.has(d.id) }))
    .sort(
      (a, b) =>
        Number(b.online) - Number(a.online) ||
        new Date(b.lastSeenAt) - new Date(a.lastSeenAt),
    );
  return {
    activeCount: list.filter(d => d.online && !d.expired).length,
    totalCount: list.length,
    accessHours,
    maxAppDevices,
    activeAppCount: list.filter(d => d.online && !d.expired && d.clientType === "app").length,
    devices: list
  };
}

/* ---------- HTTP API: health + dịch thuật (cùng cổng Socket.IO) ---------- */
const sendJson = (res, status, data) => {
  res.writeHead(status, {
    "Content-Type": "application/json; charset=utf-8",
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Methods": "GET,POST,OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type, Authorization",
    "Cache-Control": "no-store",
  });
  res.end(JSON.stringify(data));
};

async function readRequestJson(req, maxBytes = 1024 * 1024) {
  const chunks = [];
  let size = 0;
  for await (const chunk of req) {
    size += chunk.length;
    if (size > maxBytes) throw Object.assign(new Error("Nội dung yêu cầu quá lớn."), { statusCode: 413 });
    chunks.push(chunk);
  }
  try {
    return JSON.parse(Buffer.concat(chunks).toString("utf8") || "{}");
  } catch {
    throw Object.assign(new Error("Dữ liệu gửi lên không phải JSON hợp lệ."), { statusCode: 400 });
  }
}

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

const translationCache = new Map();
const MAX_TRANSLATION_CACHE = 2000;

function normalizeLibreLanguage(target) {
  const aliases = {
    "zh-CN": "zh",
    "zh-cn": "zh",
    "zh-TW": "zt",
    "zh-tw": "zt",
  };
  return aliases[target] || target;
}

async function translateText(text, target) {
  const cacheKey = `${target}::${text}`;
  if (translationCache.has(cacheKey)) return translationCache.get(cacheKey);

  const baseUrl = (process.env.LIBRETRANSLATE_URL || "http://127.0.0.1:5000").replace(/\/+$/, "");
  const apiKey = process.env.LIBRETRANSLATE_API_KEY;
  const libreTarget = normalizeLibreLanguage(target);
  let lastError;

  // LibreTranslate tự triển khai: không cần API key mặc định.
  // Có thể đặt LIBRETRANSLATE_URL nếu dịch vụ chạy trên máy chủ khác.
  for (let attempt = 0; attempt < 2; attempt++) {
    try {
      const response = await fetch(`${baseUrl}/translate`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          q: text,
          source: "auto",
          target: libreTarget,
          format: "text",
          ...(apiKey ? { api_key: apiKey } : {}),
        }),
        signal: AbortSignal.timeout(30000),
      });

      const payload = await response.json().catch(() => ({}));
      if (!response.ok) {
        const detail = payload?.error || `LibreTranslate HTTP ${response.status}`;
        throw new Error(detail);
      }

      const translated = payload?.translatedText;
      if (typeof translated !== "string" || !translated.trim()) {
        throw new Error("LibreTranslate không trả về bản dịch.");
      }

      translationCache.set(cacheKey, translated);
      if (translationCache.size > MAX_TRANSLATION_CACHE) {
        const oldestKey = translationCache.keys().next().value;
        if (oldestKey) translationCache.delete(oldestKey);
      }
      return translated;
    } catch (error) {
      lastError = error;
      // Thử lại một lần với lỗi mạng/timeout; không retry lỗi trả về từ API nếu rõ ràng là 4xx.
      if (attempt === 0 && !/LibreTranslate HTTP 4\d\d/.test(error?.message || "")) {
        await sleep(700);
      } else {
        break;
      }
    }
  }

  throw new Error(
    `LibreTranslate không phản hồi tại ${baseUrl}: ${lastError?.message || "lỗi mạng"}. ` +
    "Hãy kiểm tra dịch vụ LibreTranslate đang chạy và có hỗ trợ ngôn ngữ đích."
  );
}

const server = http.createServer(async (req, res) => {
  const requestUrl = new URL(req.url || "/", `http://${req.headers.host || "localhost"}`);
  if (req.method === "OPTIONS") {
    res.writeHead(204, {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "GET,POST,OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type, Authorization",
      "Access-Control-Max-Age": "86400",
    });
    return res.end();
  }

  if (req.method === "GET" && (requestUrl.pathname === "/health" || requestUrl.pathname === "/api/health")) {
    return sendJson(res, 200, {
      ok: true,
      service: "multilingual-narration-server",
      translation: true,
      translationMode: "libretranslate",
      translationUrl: (process.env.LIBRETRANSLATE_URL || "http://127.0.0.1:5000").replace(/\/+$/, ""),
      port: PORT,
    });
  }

  if (req.method === "POST" && requestUrl.pathname === "/api/translate") {
    try {
      const body = await readRequestJson(req);
      const text = typeof body.text === "string" ? body.text.trim() : "";
      const target = typeof body.target === "string" ? body.target.trim() : "";
      if (!text) return sendJson(res, 400, { error: "Thiếu văn bản cần dịch." });
      if (text.length > 10000) return sendJson(res, 413, { error: "Văn bản quá dài (tối đa 10.000 ký tự mỗi lần dịch)." });
      if (!/^[a-zA-Z]{2,3}(?:-[a-zA-Z0-9]{2,8})?$/.test(target)) {
        return sendJson(res, 400, { error: "Mã ngôn ngữ đích không hợp lệ." });
      }
      if (target.toLowerCase() === "vi") return sendJson(res, 200, { translatedText: text, target });
      const translatedText = await translateText(text, target);
      return sendJson(res, 200, { translatedText, target });
    } catch (error) {
      const status = Number(error?.statusCode) || 502;
      console.error("[translate]", error?.message || error);
      return sendJson(res, status, { error: error?.message || "Không dịch được văn bản.", translatedText: null });
    }
  }

  if (req.method === "GET" && requestUrl.pathname === "/") {
    return sendJson(res, 200, { ok: true, message: "Máy chủ thuyết minh đang chạy.", endpoints: ["/health", "POST /api/translate", "Socket.IO"] });
  }

  // Không can thiệp vào các request Socket.IO; các đường dẫn HTTP không biết trả về 404.
  return sendJson(res, 404, { error: "Không tìm thấy endpoint." });
});

const io = new Server(server, {
  cors: { origin: true },
  pingInterval: 8000, // phát hiện thiết bị thoát nhanh hơn mặc định
  pingTimeout: 6000,
});

const broadcast = () => io.to("admin").emit("devices", snapshot());

io.use((socket, next) => {
  const role = socket.handshake.auth?.role === "admin" ? "admin" : "user";
  const ip = cleanIp(socket.handshake.address);

  if (role === "admin" && !ALLOW_REMOTE_ADMIN && !isSelf(ip)) {
    return next(new Error("forbidden"));
  }
  socket.data.role = role;
  socket.data.ip = ip;
  next();
});

io.on("connection", (socket) => {
  /* ===== ADMIN ===== */
  if (socket.data.role === "admin") {
    socket.join("admin");
    socket.emit("devices", snapshot());

    socket.on("admin:getQr", (cb) => {
      cb?.({
        token: qrToken,
        ips: lanInterfaces().map(({ name, address }) => ({ name, address })),
      });
    });

    socket.on("admin:rotateQr", (cb) => {
      qrToken = crypto.randomBytes(16).toString("hex");
      cb?.({
        token: qrToken,
        ips: lanInterfaces().map(({ name, address }) => ({ name, address })),
      });
    });

    socket.on("admin:setAccessHours", (payload, cb) => {
      const hours = Number(payload?.hours);
      if (!Number.isFinite(hours) || hours < 1 || hours > 8760) return cb?.({ ok:false, message:"Thời hạn phải từ 1 đến 8760 giờ." });
      accessHours = hours;
      saveDevices(); broadcast(); cb?.({ok:true, accessHours});
    });
    socket.on("admin:setMaxAppDevices", (payload, cb) => {
      const max = Number(payload?.max);
      if (!Number.isInteger(max) || max < 1 || max > 1000) {
        return cb?.({ ok: false, message: "Giới hạn App phải từ 1 đến 1000 thiết bị." });
      }
      maxAppDevices = max;
      broadcast();
      cb?.({ ok: true, maxAppDevices });
    });

    socket.on("admin:extendDevice", (payload, cb) => {
      const id = String(payload?.deviceId || "");
      const hours = Number(payload?.hours || accessHours);
      const d = devices[id];
      if (!d) return cb?.({ok:false,message:"Không tìm thấy thiết bị."});
      if (!Number.isFinite(hours) || hours < 1 || hours > 8760) return cb?.({ok:false,message:"Số giờ gia hạn không hợp lệ."});
      const base = Math.max(Date.now(), d.expiresAt ? new Date(d.expiresAt).getTime() : 0);
      d.expiresAt = new Date(base + hours * 3600000).toISOString();
      d.accessHours = hours; devices[id] = d; authorized.add(id);
      saveDevices(); broadcast(); cb?.({ok:true, device:d});
    });
    socket.on("admin:deleteDevice", (payload, cb) => {
      const id = String(payload?.deviceId || "");
      delete devices[id]; authorized.delete(id);
      syncDeviceToJsonServer(id, null);
      qrToken = crypto.randomBytes(16).toString("hex");
      io.to("admin").emit("qr:updated", { token: qrToken, ips: lanInterfaces().map(({ name, address }) => ({ name, address })) });
      for (const sid of live.get(id) || []) {
        io.to(sid).emit("access:revoked", {reason:"deleted"});
        io.sockets.sockets.get(sid)?.disconnect(true);
      }
      live.delete(id); saveDevices(); broadcast(); cb?.({ok:true});
    });

    // Xóa lịch sử các thiết bị đã thoát
    socket.on("admin:clearOffline", (cb) => {
      for (const id of Object.keys(devices)) {
        if (!live.has(id)) {
          delete devices[id];
          authorized.delete(id);
          syncDeviceToJsonServer(id, null);
        }
      }
      saveDevices();
      broadcast();
      cb?.({ ok: true });
    });
    return;
  }

  /* ===== USER ===== */
  socket.on("user:join", (payload, cb) => {
    const reply = (data) => typeof cb === "function" && cb(data);
    const { token, deviceId, info } = payload || {};
    const ip = socket.data.ip;

    if (!isLanClient(ip)) return reply({ ok: false, reason: "not-lan" });

    if (typeof deviceId !== "string" || !/^[A-Za-z0-9_-]{8,64}$/.test(deviceId)) {
      return reply({ ok: false, reason: "bad-device" });
    }
    if (socket.data.deviceId && socket.data.deviceId !== deviceId) {
      return reply({ ok: false, reason: "bad-device" });
    }

    const oldDevice = devices[deviceId];
    if (oldDevice?.expiresAt && Date.now() >= new Date(oldDevice.expiresAt).getTime()) {
      authorized.delete(deviceId);
      return reply({ ok: false, reason: "expired" });
    }
    const clientType = info?.clientType === "app" ? "app" : "web";
    // Quyền đã cấp được lưu trên bản ghi, nên vẫn hợp lệ sau khi máy chủ khởi động lại.
    const hasAccess =
      ((authorized.has(deviceId) || oldDevice?.authorized === true) && !!oldDevice) ||
      (typeof token === "string" && token === qrToken);
    if (clientType === "app" && !live.has(deviceId) && !oldDevice?.online) {
      const activeApps = [...live.keys()].filter((id) => devices[id]?.clientType === "app" &&
        devices[id]?.expiresAt && Date.now() < new Date(devices[id].expiresAt).getTime()).length;
      if (activeApps >= maxAppDevices) {
        return reply({ ok: false, reason: "app-limit", message: `Đã đạt giới hạn ${maxAppDevices} thiết bị App đang đăng nhập.` });
      }
    }
    if (!hasAccess) {
      return reply({ ok: false, reason: token ? "token-invalid" : "no-token" });
    }

    authorized.add(deviceId);
    socket.data.deviceId = deviceId;

    const now = new Date().toISOString();
    const ua = str(info?.userAgent, 400);
    const { name, browser } = describeDevice(ua);
    const old = devices[deviceId];

    devices[deviceId] = {
      id: deviceId,
      name: str(info?.deviceName, 100) || name,
      browser: clientType === "app" ? "Ứng dụng React Native" : browser,
      clientType,
      authorized: true,
      ip,
      platform: str(info?.platform, 60),
      language: str(info?.language, 20),
      screen: str(info?.screen, 20),
      timezone: str(info?.timezone, 60),
      userAgent: ua,
      firstScanAt: old?.firstScanAt || now,
      expiresAt: old?.expiresAt || new Date(Date.now() + accessHours * 3600000).toISOString(),
      accessHours: old?.accessHours || accessHours,
      connectedAt: live.has(deviceId) ? old?.connectedAt || now : now,
      lastSeenAt: now,
    };

    if (!live.has(deviceId)) live.set(deviceId, new Set());
    live.get(deviceId).add(socket.id);

    saveDevices();
    broadcast();
    reply({ ok: true });
  });

  socket.on("disconnect", () => {
    const id = socket.data.deviceId;
    if (!id) return;

    const sockets = live.get(id);
    sockets?.delete(socket.id);
    if (sockets && sockets.size === 0) {
      live.delete(id);
      if (devices[id]) devices[id].lastSeenAt = new Date().toISOString();
      saveDevices();
    }
    broadcast();
  });
});

// Thu hồi cả các phiên đang mở ngay khi hết hạn.
setInterval(() => {
  let changed = false;
  for (const [id, device] of Object.entries(devices)) {
    if (!device.expiresAt || Date.now() < new Date(device.expiresAt).getTime()) continue;
    if (authorized.has(id)) { authorized.delete(id); changed = true; }
    const sockets = live.get(id);
    if (sockets?.size) {
      for (const sid of sockets) {
        io.to(sid).emit("access:revoked", { reason: "expired" });
        io.sockets.sockets.get(sid)?.disconnect(true);
      }
      live.delete(id); changed = true;
    }
  }
  if (changed) { saveDevices(); broadcast(); }
}, 15000).unref();

server.listen(PORT, "0.0.0.0", async () => {
  // Nạp các thiết bị đã được ghi trong collection devices của json-server.
  try {
    const r = await fetch("http://127.0.0.1:3000/devices");
    if (r.ok) {
      const rows = await r.json();
      for (const row of rows) {
        const id = String(row.deviceId || "");
        if (!id || devices[id]) continue;
        devices[id] = {
          id, name: row.name || row.info?.platform || "Thiết bị", browser: row.browser || "",
          ip: row.ip || "", platform: row.info?.platform || "", language: row.info?.language || "",
          screen: row.info?.screen || "", timezone: row.info?.timezone || "", userAgent: row.info?.userAgent || "",
          firstScanAt: row.firstSeen || new Date().toISOString(), lastSeenAt: row.lastSeen || row.firstSeen || new Date().toISOString(),
          expiresAt: row.expiresAt || new Date(Date.now() + accessHours * 3600000).toISOString(), accessHours
        };
      }
      saveDevices();
    }
  } catch { /* json-server chưa chạy; có thể khởi động sau */ }
  console.log(`\nMáy chủ thuyết minh chạy tại cổng ${PORT}`);
  const ips = lanInterfaces();
  if (ips.length === 0) {
    console.log("Chưa thấy địa chỉ mạng LAN. Hãy kết nối Wi-Fi/LAN.");
  }
  for (const i of ips) console.log(`  LAN: ${i.address}  (${i.name})`);
  console.log(
    ALLOW_REMOTE_ADMIN
      ? "Admin: cho phép kết nối từ máy khác trong LAN." 
      : "Admin: chỉ cho phép từ chính máy chủ này.",
  );
});
