import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { QRCodeSVG } from "qrcode.react";
import {
  QrCode,
  Users,
  Smartphone,
  Copy,
  Check,
  RefreshCw,
  Trash2,
  X,
  Wifi,
} from "lucide-react";
import { USER_API_BASE } from "./userApi";
import { authHeaders, notifyAuthLost } from "./adminAuth";

/* ===== Hook: lấy danh sách thiết bị từ Back-end Java (/api/admin/access) =====
   Thay cho Socket.IO của server.mjs: hỏi lại định kỳ (3 giây) để số "đang mở" gần như thời gian thực.
   Gửi kèm token đăng nhập admin (Authorization: Bearer) — xem adminAuth.js. */
const ADMIN_API = `${USER_API_BASE}/api/admin/access`;
const POLL_MS = 3000;

async function adminCall(path, method = "GET", body) {
  const headers = { "Content-Type": "application/json", ...authHeaders() };
  let res;
  try {
    res = await fetch(`${ADMIN_API}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal: AbortSignal.timeout(8000),
    });
  } catch {
    throw new Error("Không kết nối được máy chủ (cổng 3000).");
  }
  const text = await res.text();
  let data = null;
  try {
    data = text ? JSON.parse(text) : null;
  } catch {}
  if (res.status === 401) {
    notifyAuthLost();
    throw new Error("Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.");
  }
  if (!res.ok) throw new Error(data?.message || data?.error || `Lỗi máy chủ ${res.status}`);
  return data;
}

// Giữ đúng tên trường mà giao diện bảng thiết bị đang dùng.
const toRow = (d) => ({
  ...d,
  id: d.deviceId,
  firstScanAt: d.firstSeenAt,
  connectedAt: d.firstSeenAt,
});

export function useLiveDevices() {
  const [state, setState] = useState({
    connected: false,
    error: "",
    activeCount: 0,
    totalCount: 0,
    devices: [],
  });

  const refresh = useCallback(async () => {
    try {
      const o = await adminCall("/overview");
      setState((s) => ({
        ...s,
        connected: true,
        error: "",
        activeCount: o.activeCount,
        totalCount: o.totalCount,
        activeAppCount: o.activeAppCount,
        accessHours: o.accessHours,
        maxAppDevices: o.maxAppDevices,
        devices: (o.devices || []).map(toRow).sort(
          (a, b) => Number(b.online) - Number(a.online) || new Date(b.lastSeenAt) - new Date(a.lastSeenAt),
        ),
      }));
    } catch (e) {
      setState((s) => ({ ...s, connected: false, error: e.message }));
    }
  }, []);

  useEffect(() => {
    refresh();
    const timer = setInterval(refresh, POLL_MS);
    return () => clearInterval(timer);
  }, [refresh]);

  // Mọi thao tác ghi xong thì làm mới ngay để bảng cập nhật tức thì.
  const act = useCallback(
    async (fn) => {
      try {
        const res = await fn();
        await refresh();
        return { ok: true, ...(res || {}) };
      } catch (e) {
        return { ok: false, message: e.message };
      }
    },
    [refresh],
  );

  const getQr = useCallback(() => adminCall("/qr"), []);
  const rotateQr = useCallback(() => adminCall("/qr/rotate", "POST", {}), []);
  const clearOffline = useCallback(() => act(() => adminCall("/devices/clear-offline", "POST", {})), [act]);
  const setAccessHours = useCallback((hours) => act(() => adminCall("/config", "PUT", { accessHours: hours })), [act]);
  const setMaxAppDevices = useCallback((max) => act(() => adminCall("/config", "PUT", { maxAppDevices: max })), [act]);
  const extendDevice = useCallback(
    (deviceId, hours) => act(() => adminCall(`/devices/${encodeURIComponent(deviceId)}/extend`, "POST", { hours })),
    [act],
  );
  const deleteDevice = useCallback(
    (deviceId) => act(() => adminCall(`/devices/${encodeURIComponent(deviceId)}`, "DELETE")),
    [act],
  );

  return { ...state, getQr, rotateQr, clearOffline, setAccessHours, setMaxAppDevices, extendDevice, deleteDevice };
}

/* Máy chủ không liệt kê được IP LAN (ví dụ chạy trong Docker): dùng địa chỉ trang admin đang mở,
   miễn là không phải localhost (điện thoại không vào được localhost của máy khác). */
function withFallbackIps(res) {
  const ips = Array.isArray(res?.ips) ? res.ips : [];
  const host = window.location.hostname;
  const isLocal = !host || host === "localhost" || host === "127.0.0.1" || host === "::1";
  if (ips.length === 0 && !isLocal) {
    return { ...res, ips: [{ name: "Địa chỉ trang đang mở", address: host }] };
  }
  return { ...res, ips };
}

/* Link nằm trong QR: dùng IP LAN của máy chủ + cổng đang chạy giao diện */
function buildUserUrl(ip, token) {
  const url = new URL(window.location.origin);
  url.hostname = ip;
  url.pathname = "/user";
  url.search = `?token=${token}`;
  return url.toString();
}

const formatTime = (iso) =>
  iso ? new Date(iso).toLocaleTimeString("vi-VN") : "—";
const formatDateTime = (iso) =>
  iso ? new Date(iso).toLocaleString("vi-VN") : "—";

/* ===== Cửa sổ "Mở QR đăng nhập" ===== */
export function QrLoginModal({ live, onClose }) {
  const [info, setInfo] = useState(null); // { token, ips }
  const [ip, setIp] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [copied, setCopied] = useState(false);
  const [target, setTarget] = useState("web");

  const { connected, getQr, rotateQr } = live;

  useEffect(() => {
    if (!connected) return undefined;
    let cancelled = false;

    getQr()
      .then((res) => {
        if (cancelled) return;
        const fixed = withFallbackIps(res);
        setInfo(fixed);
        setIp((current) => current || fixed.ips[0]?.address || "");
        setError("");
      })
      .catch((e) => !cancelled && setError(e.message));

    return () => {
      cancelled = true;
    };
  }, [connected, getQr]);

  useEffect(() => {
    const onKey = (e) => e.key === "Escape" && onClose();
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);

  const url = useMemo(
    () => (info && ip ? (target === "app"
      ? JSON.stringify({ type: "app-login", token: info.token, server: `http://${ip}:3000` })
      : buildUserUrl(ip, info.token)) : ""),
    [info, ip, target],
  );

  const handleRotate = async () => {
    setBusy(true);
    try {
      setInfo(withFallbackIps(await rotateQr()));
      setError("");
    } catch (e) {
      setError(e.message);
    }
    setBusy(false);
  };

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(url);
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    } catch {
      setError("Không sao chép được, hãy bôi đen đường dẫn và copy thủ công.");
    }
  };

  return (
    <div className="live-overlay" onClick={onClose} role="presentation">
      <div
        className="live-modal"
        role="dialog"
        aria-modal="true"
        aria-label="QR đăng nhập"
        onClick={(e) => e.stopPropagation()}
      >
        <button
          type="button"
          className="live-close"
          onClick={onClose}
          aria-label="Đóng"
        >
          <X size={16} />
        </button>

        <h2>
          <QrCode size={18} /> QR đăng nhập User / App
        </h2>
        <div style={{display:"flex",gap:8,margin:"12px 0"}}>
          <button type="button" className={target === "web" ? "admin-primary" : "live-secondary"} onClick={()=>setTarget("web")}>Đăng nhập Web</button>
          <button type="button" className={target === "app" ? "admin-primary" : "live-secondary"} onClick={()=>setTarget("app")}>Đăng nhập App</button>
        </div>
        <p className="live-muted">
          {target === "app" ? "Mở ứng dụng React Native, chọn Quét QR đăng nhập App và quét mã này." : "Thiết bị quét mã này (cùng mạng Wi-Fi/LAN với máy chủ) sẽ được vào giao diện user."}
        </p>

        {!connected ? (
          <div className="live-alert">
            {live.error || "Đang kết nối máy chủ..."}
            <br />
            <small>
              Hãy chạy máy chủ bằng lệnh <code>npm run server</code>.
            </small>
          </div>
        ) : info && info.ips.length === 0 ? (
          <div className="live-alert">
            Không tìm thấy địa chỉ IP mạng LAN. Hãy kết nối máy chủ với
            Wi-Fi/LAN rồi mở lại.
          </div>
        ) : url ? (
          <>
            <div className="live-qr">
              <QRCodeSVG value={url} size={220} level="M" />
            </div>

            {info.ips.length > 1 && (
              <label className="live-field">
                <span>
                  <Wifi size={13} /> Mạng LAN của máy chủ
                </span>
                <select value={ip} onChange={(e) => setIp(e.target.value)}>
                  {info.ips.map((i) => (
                    <option key={i.address} value={i.address}>
                      {i.address} ({i.name})
                    </option>
                  ))}
                </select>
              </label>
            )}

            <div className="live-url">
              <code>{target === "app" ? "QR dành cho ứng dụng React Native" : url}</code>
              <button type="button" onClick={handleCopy}>
                {copied ? <Check size={14} /> : <Copy size={14} />}
                {copied ? "Đã chép" : "Chép"}
              </button>
            </div>
          </>
        ) : (
          <p className="live-muted">Đang tạo mã...</p>
        )}

        {error && connected && <div className="live-alert">{error}</div>}

        <div className="live-modal-footer">
          <span className="live-badge online">
            <Smartphone size={13} /> {live.activeCount} thiết bị đang mở
          </span>

          <button
            type="button"
            className="live-secondary"
            onClick={handleRotate}
            disabled={busy || !connected}
          >
            <RefreshCw size={14} /> Tạo mã mới
          </button>
        </div>
        <small className="live-muted">
          Tạo mã mới sẽ vô hiệu hóa QR cũ; các thiết bị đã vào vẫn dùng bình
          thường.
        </small>
      </div>
    </div>
  );
}

/* ===== Trang "Những người đang dùng" ===== */
export function ActiveUsersPage({ live, onOpenQr }) {
  const offlineCount = live.devices.filter((d) => !d.online).length;
  const [hours, setHours] = useState(live.accessHours || 24);
  const [maxApps, setMaxApps] = useState(live.maxAppDevices || 5);
  const [deviceHours, setDeviceHours] = useState({});
  const [actionMessage, setActionMessage] = useState("");
  useEffect(() => setHours(live.accessHours || 24), [live.accessHours]);
  useEffect(() => setMaxApps(live.maxAppDevices || 5), [live.maxAppDevices]);
  const fmtExpiry = (value) => value ? new Date(value).toLocaleString("vi-VN") : "—";

  return (
    <>
      <div className="admin-page-heading">
        <div>
          <h1>Những người đang dùng</h1>
          <p>Các thiết bị đã quét QR và đang mở dịch vụ thuyết minh.</p>
        </div>

        <div className="admin-heading-actions">
          <button className="admin-primary" type="button" onClick={onOpenQr}>
            <QrCode size={15} />
            Mở QR đăng nhập
          </button>
        </div>
      </div>

      <div className="live-summary">
        <article className="admin-stat-card">
          <div className="admin-stat-title">
            Thiết bị đang mở
            <Users size={15} />
          </div>
          <strong>{live.activeCount}</strong>
          <p>
            {live.connected ? "Cập nhật theo thời gian thực" : live.error}
          </p>
        </article>

        <article className="admin-stat-card">
          <div className="admin-stat-title">
            Tổng thiết bị đã quét
            <Smartphone size={15} />
          </div>
          <strong>{live.totalCount}</strong>
          <p>Gồm cả thiết bị đã thoát</p>
        </article>
      </div>

      <section className="admin-panel live-table-panel">
        <div className="live-table-heading">
          <b>Giới hạn đăng nhập ứng dụng</b>
          <span className="live-badge online">{live.activeAppCount || 0} / {live.maxAppDevices || maxApps} App đang đăng nhập</span>
        </div>
        <div style={{display:"flex",gap:8,alignItems:"center",flexWrap:"wrap",padding:"12px 0"}}>
          <label htmlFor="max-app-devices">Số thiết bị App đăng nhập đồng thời tối đa</label>
          <input id="max-app-devices" type="number" min="1" max="1000" value={maxApps} onChange={e=>setMaxApps(e.target.value)} style={{width:100}} />
          <button className="live-secondary" type="button" onClick={async()=>{try{const r=await live.setMaxAppDevices(Number(maxApps));setActionMessage(r?.ok?"Đã cập nhật giới hạn đăng nhập App.":r?.message||"Không cập nhật được.");}catch(e){setActionMessage(e.message)}}}>Lưu giới hạn App</button>
        </div>
      </section>

      <section className="admin-panel live-table-panel">
        <div className="live-table-heading">
          <b>Thời hạn truy cập thiết bị</b>
          <div style={{display:"flex",gap:8,alignItems:"center",flexWrap:"wrap"}}>
            <label htmlFor="device-access-hours">Thời hạn mặc định (giờ)</label>
            <input id="device-access-hours" type="number" min="1" max="8760" value={hours} onChange={e=>setHours(e.target.value)} style={{width:90}} />
            <button className="live-secondary" type="button" onClick={async()=>{try{const r=await live.setAccessHours(Number(hours));setActionMessage(r?.ok?"Đã cập nhật thời hạn mặc định.":r?.message||"Không cập nhật được.");}catch(e){setActionMessage(e.message)}}}>Lưu thời hạn</button>
          </div>
        </div>
        {actionMessage && <p className="live-muted" role="status">{actionMessage}</p>}
        <div className="live-table-heading">
          <b>Danh sách thiết bị</b>

          <button
            type="button"
            className="live-secondary"
            disabled={offlineCount === 0 || !live.connected}
            onClick={() => live.clearOffline().catch(() => {})}
          >
            <Trash2 size={14} /> Xóa thiết bị đã thoát ({offlineCount})
          </button>
        </div>

        {live.devices.length === 0 ? (
          <div className="live-empty">
            Chưa có thiết bị nào quét QR. Bấm “Mở QR đăng nhập” để bắt đầu.
          </div>
        ) : (
          <div className="live-table-scroll">
            <table className="live-table">
              <thead>
                <tr>
                  <th>Thiết bị</th>
                  <th>Địa chỉ IP</th>
                  <th>Màn hình</th>
                  <th>Quét lần đầu</th>
                  <th>Hết hạn truy cập</th>
                  <th>Trạng thái / thao tác</th>
                </tr>
              </thead>
              <tbody>
                {live.devices.map((d) => (
                  <tr key={d.id}>
                    <td>
                      <b>{d.name}</b>
                      <small>
                        {d.browser}
                        {d.language ? ` · ${d.language}` : ""}
                      </small>
                    </td>
                    <td>{d.ip}</td>
                    <td>{d.screen || "—"}</td>
                    <td>{formatDateTime(d.firstScanAt)}</td>
                    <td>
                      <span className={d.expired ? "live-badge offline" : "live-badge online"}>{fmtExpiry(d.expiresAt)}{d.expired ? " · Hết hạn" : ""}</span>
                    </td>
                    <td>
                      {d.online ? (
                        <span className="live-badge online">
                          Đang mở · từ {formatTime(d.connectedAt)}
                        </span>
                      ) : (
                        <span className="live-badge offline">
                          Đã thoát · {formatTime(d.lastSeenAt)}
                        </span>
                      )}
                      <div style={{display:"flex",gap:6,marginTop:8,flexWrap:"wrap"}}>
                        <input aria-label="Số giờ gia hạn" type="number" min="1" max="8760" value={deviceHours[d.id] ?? (live.accessHours || 24)} onChange={e=>setDeviceHours(v=>({...v,[d.id]:e.target.value}))} style={{width:70}} />
                        <button type="button" className="live-secondary" onClick={async()=>{const r=await live.extendDevice(d.id,Number(deviceHours[d.id] ?? live.accessHours ?? 24));setActionMessage(r?.ok?"Đã gia hạn thiết bị.":r?.message||"Không gia hạn được.");}}>Gia hạn</button>
                        <button type="button" className="live-secondary" onClick={async()=>{if(window.confirm("Xóa thiết bị này và thu hồi quyền truy cập?")){await live.deleteDevice(d.id);setActionMessage("Đã xóa thiết bị và thu hồi quyền.");}}}><Trash2 size={13}/> Xóa</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </>
  );
}
