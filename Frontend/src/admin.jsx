import { QRCodeCanvas } from "qrcode.react";
import React, {
  useState,
  useEffect,
  useRef,
  useMemo,
  useCallback,
} from "react";
import {
  Building2,
  ShieldCheck,
  MapPin,
  Star,
  Eye,
  EyeOff,
  ArrowRight,
  LockKeyhole,
  Check,
  X,
  LayoutDashboard,
  Landmark,
  Radio,
  Users,
  Settings,
  Info,
  LogOut,
  Bell,
  CalendarDays,
  Plus,
  TrendingUp,
  Search,
  Compass,
  ChevronDown,
  Pencil,
  MapPinned,
  UploadCloud,
  Trash2,
  Save,
  ImagePlus,
  ArrowLeft,
  QrCode,
  Bluetooth,
  Volume2,
  Play,
  Square,
  Languages,
  Copy,
  Download,
} from "lucide-react";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import "./App.css";
import "./admin-live.css";
import { useLiveDevices, QrLoginModal, ActiveUsersPage } from "./ActiveUsers";
import * as api from "./api";
import { adminLogin, checkAdminSession, clearAdminToken, getAdminToken, onAdminAuthLost } from "./adminAuth";

export default function Admin() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [remember, setRemember] = useState(false);
  const [message, setMessage] = useState("");
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [busy, setBusy] = useState(false);
  // Có token đã lưu thì kiểm tra với máy chủ trước khi hiện trang đăng nhập (tránh nháy form).
  const [checking, setChecking] = useState(() => !!getAdminToken());

  useEffect(() => {
    let alive = true;
    if (getAdminToken()) {
      checkAdminSession().then((ok) => {
        if (!alive) return;
        setIsLoggedIn(ok);
        setChecking(false);
      });
    }
    // Server từ chối token (hết hạn / đổi khoá): quay về trang đăng nhập
    const off = onAdminAuthLost(() => {
      setIsLoggedIn(false);
      setPassword("");
      setMessage("Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.");
    });
    return () => {
      alive = false;
      off();
    };
  }, []);

  const handleLogin = async (e) => {
    e.preventDefault();

    if (!email.trim() || !password) {
      setMessage("Vui lòng nhập email và mật khẩu.");
      return;
    }

    setBusy(true);
    try {
      await adminLogin(email.trim(), password, remember);
      setIsLoggedIn(true);
      setMessage("");
    } catch (err) {
      setMessage(err.message || "Không đăng nhập được.");
    } finally {
      setBusy(false);
      setPassword("");
    }
  };

  const handleLogout = () => {
    clearAdminToken();
    setIsLoggedIn(false);
    setEmail("");
    setPassword("");
    setMessage("");
  };
  if (checking) return null;
  if (isLoggedIn) {
    return <AdminDashboard onLogout={handleLogout} />;
  }
  return (
    <main className="page-shell">
      {/* BÊN TRÁI: GIỚI THIỆU */}
      <section className="intro-panel">
        <header className="brand">
          <Building2 size={21} />

          <div>
            <div className="brand-name">Khám phá Việt Nam</div>
            <div className="brand-subtitle">CỔNG QUẢN TRỊ</div>
          </div>
        </header>

        <div className="intro-copy">
          <p className="eyebrow">KẾT NỐI ĐỊA ĐIỂM · LAN TỎA DI SẢN</p>

          <h1>
            Mỗi điểm đến,
            <br />
            một câu chuyện.
          </h1>

          <p className="intro-description">
            Cùng quản lý những địa điểm, bảo tàng và hiện vật để mang đến trải
            nghiệm khám phá trọn vẹn hơn.
          </p>
        </div>

        <article className="destination-card">
          <img
            src="https://images.unsplash.com/photo-1565967511849-76a60a516170?auto=format&fit=crop&w=1000&q=85"
            alt="Địa điểm kiến trúc và di sản"
          />

          <div className="destination-info">
            <div>
              <h2>Independence Palace</h2>

              <p className="location">
                <MapPin size={12} />
                Thành phố Hồ Chí Minh
              </p>
            </div>

            <div className="rating">
              <Star size={14} />
              <strong>4.8</strong>
            </div>
          </div>
        </article>

        <nav className="category-links">
          <span>Địa điểm du lịch</span>
          <i />
          <span>Bảo tàng</span>
          <i />
          <span>Hiện vật</span>
        </nav>
      </section>

      {/* BÊN PHẢI: ĐĂNG NHẬP */}
      <section className="login-panel">
        <div className="admin-note">
          <ShieldCheck size={14} />
          Không gian dành cho quản trị viên
        </div>

        <div className="login-content">
          <p className="eyebrow">CHÀO MỪNG ĐẾN VỚI ỨNG DỤNG</p>

          <h2>Đăng nhập quản trị</h2>

          <p className="login-description">
            Đăng nhập để bắt đầu quản lý và cập nhật nội dung khám phá.
          </p>

          <form onSubmit={handleLogin}>
            <label htmlFor="email">Email</label>

            <input
              id="email"
              type="email"
              placeholder="your_email@mail.com"
              value={email}
              onChange={(e) => {
                setEmail(e.target.value);
                setMessage("");
              }}
              required
            />

            <label htmlFor="password" className="password-label">
              Mật khẩu
            </label>

            <div className="password-wrap">
              <input
                id="password"
                type={showPassword ? "text" : "password"}
                placeholder="6+ ký tự"
                value={password}
                onChange={(e) => {
                  setPassword(e.target.value);
                  setMessage("");
                }}
                required
              />

              <button
                type="button"
                className="eye-button"
                onClick={() => setShowPassword(!showPassword)}
                aria-label="Hiện hoặc ẩn mật khẩu"
              >
                {showPassword ? <EyeOff size={17} /> : <Eye size={17} />}
              </button>
            </div>

            <div className="form-options">
              <label className="remember-option">
                <input
                  type="checkbox"
                  checked={remember}
                  onChange={(e) => setRemember(e.target.checked)}
                />
                <span className="custom-checkbox">
                  {remember && <Check size={11} />}
                </span>
                Ghi nhớ đăng nhập
              </label>

              <button
                type="button"
                className="text-button"
                onClick={() =>
                  setMessage(
                    "Vui lòng liên hệ quản trị viên để đặt lại mật khẩu.",
                  )
                }
              >
                Quên mật khẩu?
              </button>
            </div>

            <button type="submit" className="submit-button" disabled={busy}>
              {busy ? "Đang đăng nhập..." : "Đăng nhập"}
              <ArrowRight size={15} />
            </button>

            {message && (
              <p className="notice" role="status">
                {message}
              </p>
            )}
          </form>

          <p className="register-prompt">
            Chưa có tài khoản?
            <button
              type="button"
              className="text-button"
              onClick={() =>
                setMessage("Tính năng đăng ký cần kết nối hệ thống tài khoản.")
              }
            >
              Đăng ký
            </button>
          </p>

          <div className="security-message">
            <LockKeyhole size={15} />

            <span>
              Chỉ tài khoản được cấp quyền mới có thể truy cập hệ thống quản
              trị.
            </span>
          </div>
        </div>

        <footer className="page-footer">
          <span>© 2026 · Khám phá Việt Nam</span>

          <button
            type="button"
            onClick={() => setMessage("Vui lòng liên hệ bộ phận hỗ trợ.")}
          >
            Trợ giúp
          </button>
        </footer>
      </section>
    </main>
  );
}

/* =====================================================================
   TIỆN ÍCH DÙNG CHUNG
   ===================================================================== */
const same = (a, b) => String(a) === String(b);

const norm = (s) =>
  String(s || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/đ/g, "d")
    .toLowerCase();

const dateKey = (d) =>
  `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;

const fmtDate = (iso) =>
  iso ? new Date(iso).toLocaleDateString("vi-VN") : "—";

const statusInfo = (it) =>
  it.hidden
    ? { label: "Đã ẩn", cls: "pending" }
    : it.status === "published"
      ? { label: "Đã xuất bản", cls: "published" }
      : { label: "Bản nháp", cls: "draft" };

const parseCoord = (value, min, max) => {
  if (value === "" || value === null || value === undefined) return null;
  const n = Number(value);
  return Number.isFinite(n) && n >= min && n <= max ? n : null;
};

const readFile = (
  file,
  { prefix = "image/", maxMB = 10, label = "ảnh" } = {},
) =>
  new Promise((resolve, reject) => {
    if (!file || !file.type.startsWith(prefix))
      return reject(new Error(`Vui lòng chọn tệp ${label} hợp lệ.`));
    if (file.size > maxMB * 1024 * 1024)
      return reject(new Error(`Tệp ${label} không được vượt quá ${maxMB} MB.`));
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result));
    reader.onerror = () => reject(new Error(`Không thể đọc tệp ${label}.`));
    reader.readAsDataURL(file);
  });

const RADIUS_SUGGESTIONS = [10, 20, 30, 50, 100, 200, 500];

/* =====================================================================
   CSS BỔ SUNG CHO CÁC MÀN HÌNH MỚI (tiền tố "ad-")
   ===================================================================== */
const ADMIN_CSS = `
.ad-tabs{display:flex;gap:6px;margin-bottom:16px;border-bottom:1px solid #e5eaf1;flex-wrap:wrap}
.ad-tabs button{border:0;background:transparent;padding:10px 14px;font-size:12px;color:#718096;cursor:pointer;border-bottom:2px solid transparent;margin-bottom:-1px}
.ad-tabs button.on{color:#3978f6;border-color:#3978f6;font-weight:650}
.ad-panel{background:#fff;border:1px solid #e5eaf1;border-radius:10px;padding:16px;margin-bottom:14px}
.ad-panel h3{margin:0 0 4px;font-size:14px;color:#1f2d42}
.ad-panel>p{margin:0 0 12px;font-size:11px;color:#8190a5}
.ad-filters{display:flex;gap:8px;margin-bottom:12px;flex-wrap:wrap}
.ad-filters .ad-input{flex:1;min-width:200px}
.ad-input{border:1px solid #dfe5ed;border-radius:6px;padding:9px 10px;font:inherit;font-size:12px;background:#fff;color:#344054;outline:none;min-width:0;width:100%}
.ad-filters select.ad-input{flex:0 0 auto;width:auto;min-width:150px}
.ad-input:focus{border-color:#8bb0ff;box-shadow:0 0 0 2px #eaf1ff}
.ad-list{display:grid;gap:8px}
.ad-scroll{max-height:560px;overflow:auto;padding-right:4px}
.ad-item{border:1px solid #e5eaf1;border-radius:8px;background:#fff}
.ad-row{display:flex;align-items:center;gap:12px;padding:10px 12px;cursor:pointer;flex-wrap:wrap}
.ad-thumb{width:46px;height:40px;border-radius:6px;object-fit:cover;background:#eef2f7;display:grid;place-items:center;color:#8a99ae;flex:0 0 46px}
.ad-info{flex:1;min-width:160px;display:grid;gap:3px}
.ad-info b{font-size:12px;color:#1f2d42}
.ad-info small{font-size:10px;color:#7c8ba0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.ad-actions{display:flex;gap:5px;flex-wrap:wrap}
.ad-actions button,.ad-btn{display:inline-flex;align-items:center;gap:5px;border:1px solid #dfe5ed;background:#fff;color:#526176;border-radius:5px;padding:6px 9px;font-size:10px;cursor:pointer}
.ad-actions button:hover,.ad-btn:hover{background:#f3f6fb}
.ad-actions button:disabled,.ad-btn:disabled{opacity:.5;cursor:not-allowed}
.ad-actions .danger,.ad-btn.danger{color:#c0392b;border-color:#f3d2cf}
.ad-actions .ad-pub,.ad-btn.primary{background:#3978f6;color:#fff;border-color:#3978f6}
.ad-detail{display:grid;grid-template-columns:200px 1fr;gap:16px;padding:12px 14px 14px;border-top:1px solid #eef1f5;background:#fafbfd;border-radius:0 0 8px 8px}
.ad-detail img,.ad-detail .ad-noimg{width:100%;border-radius:8px;object-fit:cover;max-height:160px;min-height:100px;background:#eef2f7;display:grid;place-items:center;color:#8a99ae;font-size:10px}
.ad-detail dl{margin:0;display:grid;gap:8px;font-size:11px}
.ad-detail dt{color:#8190a5;font-size:10px;margin-bottom:2px}
.ad-detail dd{margin:0;color:#2c3a50;line-height:1.6}
.ad-sub{border-top:1px solid #eef1f5;padding:8px 12px 12px 36px;display:grid;gap:6px;background:#fafbfd;border-radius:0 0 8px 8px}
.ad-sub .ad-row{padding:6px 8px;border:1px solid #e9edf3;border-radius:6px;background:#fff;cursor:default}
.ad-empty{text-align:center;color:#8190a5;font-size:11px;padding:22px}
.ad-more{font-size:10px;color:#8190a5;margin:8px 0 0;text-align:center}
.ad-modal-bg{position:fixed;inset:0;background:#10182866;z-index:900;display:flex;justify-content:center;align-items:flex-start;padding:30px 16px;overflow:auto}
.ad-modal-box{background:#f6f8fb;border-radius:12px;width:100%;max-width:1100px;padding:18px;box-shadow:0 20px 60px #0004}
.ad-modal-head{display:flex;justify-content:space-between;align-items:center;margin-bottom:12px}
.ad-modal-head h2{margin:0;font-size:16px}
.ad-modal-head button{border:0;background:transparent;cursor:pointer}
.ad-form-actions{display:flex;justify-content:flex-end;gap:8px;margin-top:14px}
.ad-form-actions button{display:inline-flex;align-items:center;gap:6px;border:1px solid #dfe5ed;background:#fff;border-radius:6px;padding:9px 14px;font-size:12px;cursor:pointer;color:#526176}
.ad-form-actions .admin-primary{color:#fff}
.ad-imgbox{position:relative;display:grid;place-items:center;min-height:130px;border:1px dashed #cbd5e1;border-radius:8px;background:#f8fafc;overflow:hidden;cursor:pointer;color:#7b8ba1;font-size:11px;gap:6px;text-align:center}
.ad-imgbox img{width:100%;max-height:240px;object-fit:cover;display:block}
.ad-imgbox input{position:absolute;inset:0;opacity:0;cursor:pointer;width:100%}
.ad-extra{display:grid;grid-template-columns:repeat(auto-fill,minmax(80px,1fr));gap:8px;margin-top:8px}
.ad-extra div{position:relative}
.ad-extra img{width:100%;height:64px;object-fit:cover;border-radius:6px}
.ad-extra button{position:absolute;top:3px;right:3px;border:0;background:#fffc;border-radius:4px;cursor:pointer;padding:2px}
.ad-toggle{display:flex;align-items:center;gap:10px;cursor:pointer;font-size:12px;font-weight:650;color:#2c3a50}
.ad-toggle input{display:none}
.ad-toggle i{width:34px;height:19px;border-radius:12px;background:#cfd8e3;position:relative;flex:0 0 34px;transition:.15s}
.ad-toggle i:after{content:"";position:absolute;left:2px;top:2px;width:15px;height:15px;border-radius:50%;background:#fff;transition:.15s}
.ad-toggle input:checked+i{background:#3978f6}
.ad-toggle input:checked+i:after{left:17px}
.ad-sec{border:1px solid #e5eaf1;border-radius:8px;padding:12px;margin-top:10px;background:#fbfcfe}
.ad-sec.off{opacity:.55}
.ad-chips{display:flex;flex-wrap:wrap;gap:6px;margin-top:6px}
.ad-chips button{border:1px solid #dfe5ed;background:#fff;border-radius:14px;padding:4px 10px;font-size:11px;cursor:pointer;color:#526176}
.ad-chips button.on{background:#3978f6;border-color:#3978f6;color:#fff}
.ad-linked{display:flex;gap:12px;align-items:center;margin-top:10px;padding:10px;border:1px solid #dce8ff;border-radius:8px;background:#eff5ff}
.ad-linked img{width:120px;height:80px;object-fit:cover;border-radius:6px}
.ad-linked b{font-size:12px}.ad-linked small{display:block;color:#78869a;font-size:10px;margin-top:3px}
.ad-play{display:flex;align-items:center;gap:10px;padding:10px;border:1px solid #e0e8f4;border-radius:8px;background:#f7faff;margin-top:12px;flex-wrap:wrap}
.ad-play button.round{width:32px;height:32px;border-radius:50%;border:0;background:#3978f6;color:#fff;display:grid;place-items:center;cursor:pointer}
.ad-seg{display:inline-flex;border:1px solid #dfe5ed;border-radius:6px;overflow:hidden}
.ad-seg button{border:0;background:#fff;padding:5px 10px;font-size:11px;cursor:pointer}
.ad-seg button.on{background:#3978f6;color:#fff}
.ad-qr{display:flex;gap:14px;align-items:center;flex-wrap:wrap}
.ad-qr img,.ad-qr canvas{width:150px;height:150px;border:1px solid #e2e7ed;border-radius:8px;background:#fff}
.ad-qr small{display:block;word-break:break-all;color:#7c8ba0;font-size:10px;margin-top:6px}
.ad-chart{width:100%;height:auto;display:block}
.ad-chart text{font-size:10px;fill:#8190a5}
.ad-lang-form{display:grid;grid-template-columns:1.2fr .8fr .8fr auto auto;gap:10px;align-items:end}
.ad-lang-form label{display:grid;gap:5px;font-size:10px;font-weight:650;color:#45546a}
.ad-table{width:100%;border-collapse:collapse;font-size:12px}
.ad-table th{text-align:left;font-size:10px;color:#8190a5;padding:8px;border-bottom:1px solid #e5eaf1}
.ad-table td{padding:9px 8px;border-bottom:1px solid #f0f3f7}
.ad-tag{display:inline-block;padding:3px 7px;border-radius:4px;background:#edf4ff;color:#3472e8;font-size:10px;margin-right:4px}
.ad-warn{padding:10px 12px;border-radius:6px;background:#fff6e5;color:#9a6200;font-size:11px;margin-bottom:12px}
.ad-api-error{padding:12px;border-radius:8px;background:#fff0f0;color:#bd3030;font-size:12px;margin-bottom:14px}
@media(max-width:760px){.ad-detail{grid-template-columns:1fr}.ad-lang-form{grid-template-columns:1fr}.ad-sub{padding-left:12px}}
`;

/* =====================================================================
   BẢN ĐỒ THẬT (Leaflet + OpenStreetMap)
   - Có tọa độ hợp lệ  -> ghim đúng vị trí và tự zoom tới đó.
   - Truyền onPick     -> bấm lên bản đồ / kéo ghim để cập nhật tọa độ.
   - Truyền radius     -> vẽ vòng tròn vùng kích hoạt (mét).
   ===================================================================== */
const PIN_HTML =
  '<svg width="32" height="42" viewBox="0 0 32 42" xmlns="http://www.w3.org/2000/svg">' +
  '<path d="M16 0C7.2 0 0 7 0 15.7 0 27.5 16 42 16 42s16-14.5 16-26.3C32 7 24.8 0 16 0z" fill="#d93025" stroke="#fff" stroke-width="2"/>' +
  '<circle cx="16" cy="15.5" r="5.5" fill="#fff"/></svg>';

function RealMap({
  latitude,
  longitude,
  onPick,
  radius,
  emptyText,
  height = 280,
}) {
  const containerRef = useRef(null);
  const mapRef = useRef(null);
  const markerRef = useRef(null);
  const circleRef = useRef(null);
  const onPickRef = useRef(onPick);
  onPickRef.current = onPick;

  const lat = parseCoord(latitude, -90, 90);
  const lng = parseCoord(longitude, -180, 180);
  const valid = lat !== null && lng !== null;
  const rad = Number(radius) > 0 ? Number(radius) : 0;

  useEffect(() => {
    const map = L.map(containerRef.current, {
      center: [16.0, 106.0],
      zoom: 5,
      scrollWheelZoom: true,
    });
    L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
      maxZoom: 19,
      attribution:
        '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
    }).addTo(map);
    map.on("click", (e) => {
      if (onPickRef.current) onPickRef.current(e.latlng.lat, e.latlng.lng);
    });
    mapRef.current = map;
    const observer = new ResizeObserver(() => map.invalidateSize());
    observer.observe(containerRef.current);
    return () => {
      observer.disconnect();
      map.remove();
      mapRef.current = null;
      markerRef.current = null;
      circleRef.current = null;
    };
  }, []);

  useEffect(() => {
    const map = mapRef.current;
    if (!map) return;

    if (!valid) {
      markerRef.current?.remove();
      circleRef.current?.remove();
      markerRef.current = null;
      circleRef.current = null;
      return;
    }

    if (!markerRef.current) {
      const marker = L.marker([lat, lng], {
        draggable: Boolean(onPickRef.current),
        icon: L.divIcon({
          className: "",
          html: PIN_HTML,
          iconSize: [32, 42],
          iconAnchor: [16, 42],
        }),
      }).addTo(map);
      marker.on("dragend", () => {
        const pos = marker.getLatLng();
        if (onPickRef.current) onPickRef.current(pos.lat, pos.lng);
      });
      markerRef.current = marker;
    } else {
      markerRef.current.setLatLng([lat, lng]);
    }

    if (rad > 0) {
      if (!circleRef.current) {
        circleRef.current = L.circle([lat, lng], {
          radius: rad,
          color: "#3978f6",
          weight: 2,
          fillColor: "#3978f6",
          fillOpacity: 0.18,
        }).addTo(map);
      } else {
        circleRef.current.setLatLng([lat, lng]);
        circleRef.current.setRadius(rad);
      }
      map.fitBounds(circleRef.current.getBounds(), {
        maxZoom: 18,
        padding: [24, 24],
      });
    } else {
      circleRef.current?.remove();
      circleRef.current = null;
      map.setView([lat, lng], map.getZoom() < 14 ? 16 : map.getZoom());
    }
  }, [valid, lat, lng, rad]);

  const badge = {
    position: "absolute",
    left: 10,
    bottom: 10,
    zIndex: 500,
    padding: "6px 10px",
    borderRadius: 8,
    background: "rgba(255,255,255,0.95)",
    fontSize: 12,
    boxShadow: "0 1px 4px rgba(0,0,0,0.2)",
  };

  return (
    <div style={{ position: "relative" }}>
      <div
        ref={containerRef}
        style={{
          height,
          width: "100%",
          borderRadius: 12,
          overflow: "hidden",
          border: "1px solid #dfe3e8",
          zIndex: 0,
        }}
      />
      <div style={badge}>
        {valid
          ? `${lat.toFixed(6)}, ${lng.toFixed(6)}${rad ? ` · bán kính ${rad} m` : ""}`
          : emptyText || "Chưa có tọa độ hợp lệ"}
      </div>
    </div>
  );
}

/* =====================================================================
   COMPONENT NHỎ DÙNG CHUNG
   ===================================================================== */
function Thumb({ src }) {
  return src ? (
    <img className="ad-thumb" src={src} alt="" />
  ) : (
    <span className="ad-thumb">
      <Building2 size={16} />
    </span>
  );
}

function ImagePicker({
  value,
  onChange,
  onError,
  label = "Chọn ảnh từ thiết bị",
}) {
  const pick = async (e) => {
    const file = e.target.files?.[0];
    e.target.value = "";
    if (!file) return;
    try {
      onChange(await readFile(file));
      onError("");
    } catch (err) {
      onError(err.message);
    }
  };
  return (
    <>
      <label className="ad-imgbox">
        {value ? (
          <img src={value} alt="Ảnh xem trước" />
        ) : (
          <span style={{ display: "grid", justifyItems: "center", gap: 6 }}>
            <ImagePlus size={24} />
            <b>{label}</b>
            <small>JPG, PNG, WEBP · tối đa 10 MB</small>
          </span>
        )}
        <input type="file" accept="image/*" onChange={pick} />
      </label>
      {value && (
        <button
          type="button"
          className="ad-btn danger"
          style={{ marginTop: 8 }}
          onClick={() => onChange("")}
        >
          <Trash2 size={12} /> Xóa ảnh
        </button>
      )}
    </>
  );
}

function FormActions({ mode, status, busy, onCancel, onSubmit }) {
  if (mode === "edit") {
    return (
      <div className="ad-form-actions">
        <button type="button" onClick={onCancel} disabled={busy}>
          Hủy
        </button>
        <button type="button" onClick={() => onSubmit("save")} disabled={busy}>
          <Save size={14} /> Lưu
        </button>
        {status !== "published" && (
          <button
            type="button"
            className="admin-primary"
            onClick={() => onSubmit("publish")}
            disabled={busy}
          >
            <Check size={14} /> Xuất bản
          </button>
        )}
      </div>
    );
  }
  return (
    <div className="editor-bottom-bar">
      <div>
        <b>Sẵn sàng chia sẻ nội dung?</b>
        <p>Lưu nháp (ẩn với người dùng) hoặc xuất bản lên ứng dụng.</p>
      </div>
      <div className="editor-bottom-actions">
        <button type="button" onClick={onCancel} disabled={busy}>
          Hủy
        </button>
        <button
          type="button"
          className="editor-save-draft"
          onClick={() => onSubmit("draft")}
          disabled={busy}
        >
          <Save size={14} /> Bản nháp
        </button>
        <button
          type="button"
          className="admin-primary"
          onClick={() => onSubmit("publish")}
          disabled={busy}
        >
          <Check size={14} /> Xuất bản
        </button>
      </div>
    </div>
  );
}

/* =====================================================================
   FORM ĐỊA ĐIỂM / BẢO TÀNG (dùng cho cả Thêm mới và Sửa)
   ===================================================================== */
const CLASSIFY_OPTIONS = [
  "Bảo tàng",
  "Di tích",
  "Địa điểm du lịch",
  "Di sản văn hóa",
  "Danh lam thắng cảnh",
];

function SiteForm({ initial, mode = "create", onSave, onCancel }) {
  const normalizeExtraImages = (value) => {
    if (Array.isArray(value)) return value;

    if (typeof value === "string") {
      try {
        const parsed = JSON.parse(value);
        return Array.isArray(parsed) ? parsed : [];
      } catch {
        return value.trim() ? [value] : [];
      }
    }

    return [];
  };
  const [name, setName] = useState(initial?.name || "");
  const [classify, setClassify] = useState(initial?.classify || "Bảo tàng");
  const [description, setDescription] = useState(initial?.description || "");
  const [address, setAddress] = useState(initial?.address || "");
  const [latitude, setLatitude] = useState(
    initial?.latitude != null ? String(initial.latitude) : "",
  );
  const [longitude, setLongitude] = useState(
    initial?.longitude != null ? String(initial.longitude) : "",
  );
  const [image, setImage] = useState(initial?.img || "");
  const [extraImages, setExtraImages] = useState(() =>
    normalizeExtraImages(initial?.extraImages),
  );
  const [search, setSearch] = useState("");
  const [searchMsg, setSearchMsg] = useState("");
  const [searching, setSearching] = useState(false);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const seq = useRef(0);

  // Tìm địa điểm trên bản đồ: ghim + tự điền địa chỉ và tọa độ
  const runSearch = useCallback(async (q) => {
    const id = ++seq.current;
    setSearching(true);
    try {
      const r = await api.searchPlace(q);
      if (id !== seq.current) return;
      if (r) {
        setAddress(r.address);
        setLatitude(r.lat.toFixed(6));
        setLongitude(r.lng.toFixed(6));
        setSearchMsg(
          "✓ Đã tìm thấy vị trí, địa chỉ và tọa độ đã được điền tự động.",
        );
      } else {
        setSearchMsg(
          "Không tìm thấy địa điểm này, hãy nhập địa chỉ và tọa độ thủ công.",
        );
      }
    } catch (err) {
      if (id === seq.current) setSearchMsg(err.message);
    } finally {
      if (id === seq.current) setSearching(false);
    }
  }, []);

  useEffect(() => {
    const q = search.trim();
    if (q.length < 3) {
      setSearchMsg("");
      return undefined;
    }
    const t = setTimeout(() => runSearch(q), 900);
    return () => clearTimeout(t);
  }, [search, runSearch]);

  const addExtra = async (e) => {
    const files = Array.from(e.target.files || []).slice(
      0,
      9 - extraImages.length,
    );
    e.target.value = "";
    try {
      const results = await Promise.all(files.map((f) => readFile(f)));
      setExtraImages((prev) => [...prev, ...results].slice(0, 9));
      setError("");
    } catch (err) {
      setError(err.message);
    }
  };

  const submit = async (action) => {
    if (!name.trim() || !address.trim() || !description.trim()) {
      setError("Vui lòng nhập tên, địa chỉ và mô tả.");
      return;
    }
    const lat = parseCoord(latitude, -90, 90);
    const lng = parseCoord(longitude, -180, 180);
    if (lat === null || lng === null) {
      setError("Vĩ độ hoặc kinh độ không hợp lệ.");
      return;
    }
    setError("");
    setBusy(true);
    try {
      await onSave(
        {
          name: name.trim(),
          classify,
          description: description.trim(),
          address: address.trim(),
          latitude: lat,
          longitude: lng,
          img: image,
          extraImages,
        },
        action,
      );
    } finally {
      setBusy(false);
    }
  };

  const body = (
    <div className="editor-columns">
      <div className="editor-left-column">
        <section className="editor-card">
          <div className="editor-card-heading">
            <h3>Thông tin cơ bản</h3>
            <p>Nội dung hiển thị trên trang chi tiết trong ứng dụng.</p>
          </div>
          <label className="editor-field">
            Tên địa điểm / bảo tàng *
            <input
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Nhập tên địa điểm"
            />
          </label>
          <label className="editor-field">
            Phân loại *
            <select
              value={classify}
              onChange={(e) => setClassify(e.target.value)}
            >
              {CLASSIFY_OPTIONS.map((o) => (
                <option key={o}>{o}</option>
              ))}
            </select>
          </label>
          <label className="editor-field">
            Mô tả *
            <textarea
              rows={6}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Giới thiệu lịch sử, ý nghĩa và thông tin tham quan..."
            />
          </label>
        </section>

        <section className="editor-card">
          <div className="editor-card-heading">
            <h3>Hình ảnh địa điểm</h3>
            <p>Ảnh đại diện và các ảnh bổ sung (tối đa 9).</p>
          </div>
          <ImagePicker
            value={image}
            onChange={setImage}
            onError={setError}
            label="Chọn ảnh đại diện"
          />
          <div style={{ marginTop: 14, fontSize: 11, fontWeight: 650 }}>
            Ảnh bổ sung ({extraImages.length}/9)
          </div>
          <label
            className="ad-btn"
            style={{ marginTop: 8, position: "relative" }}
          >
            <UploadCloud size={13} /> Thêm ảnh
            <input
              type="file"
              accept="image/*"
              multiple
              onChange={addExtra}
              disabled={extraImages.length >= 9}
              style={{
                position: "absolute",
                inset: 0,
                opacity: 0,
                cursor: "pointer",
              }}
            />
          </label>
          {extraImages.length > 0 && (
            <div className="ad-extra">
              {extraImages.map((src, i) => (
                <div key={i}>
                  <img src={src} alt="" />
                  <button
                    type="button"
                    onClick={() =>
                      setExtraImages((p) => p.filter((_, k) => k !== i))
                    }
                  >
                    <Trash2 size={11} />
                  </button>
                </div>
              ))}
            </div>
          )}
        </section>
      </div>

      <div className="editor-right-column">
        <section className="editor-card">
          <div className="editor-card-heading">
            <h3>Địa chỉ &amp; tọa độ GPS</h3>
            <p>Nhập tên địa điểm để bản đồ tự định vị, hoặc nhập thủ công.</p>
          </div>

          <label className="editor-field">
            Tìm địa chỉ trên bản đồ
            <div className="editor-address-search">
              <input
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter" && search.trim())
                    runSearch(search.trim());
                }}
                placeholder="Nhập tên địa điểm, ví dụ: Dinh Độc Lập..."
              />
              <button
                type="button"
                onClick={() => search.trim() && runSearch(search.trim())}
                aria-label="Tìm trên bản đồ"
              >
                <Search size={15} />
              </button>
            </div>
          </label>
          {(searching || searchMsg) && (
            <small className="editor-help">
              {searching ? "Đang tìm trên bản đồ..." : searchMsg}
            </small>
          )}

          <label className="editor-field">
            Địa chỉ *
            <textarea
              rows={2}
              value={address}
              onChange={(e) => setAddress(e.target.value)}
              placeholder="Số nhà, đường, phường, tỉnh/thành phố..."
            />
          </label>

          <div className="editor-coordinate-grid">
            <label className="editor-field">
              Vĩ độ (Latitude) *
              <input
                type="number"
                step="any"
                value={latitude}
                onChange={(e) => setLatitude(e.target.value)}
              />
            </label>
            <label className="editor-field">
              Kinh độ (Longitude) *
              <input
                type="number"
                step="any"
                value={longitude}
                onChange={(e) => setLongitude(e.target.value)}
              />
            </label>
          </div>

          <div className="editor-map-heading">
            <b>Vị trí trên bản đồ</b>
            <span>
              <MapPinned size={12} /> Bấm hoặc kéo ghim để chọn tọa độ
            </span>
          </div>
          <RealMap
            latitude={latitude}
            longitude={longitude}
            onPick={(la, lo) => {
              setLatitude(la.toFixed(6));
              setLongitude(lo.toFixed(6));
            }}
            emptyText="Tìm địa điểm hoặc nhập tọa độ để hiện ghim"
          />
        </section>
      </div>
    </div>
  );

  const errorBox = error && (
    <div className="editor-error" role="alert">
      {error}
    </div>
  );

  if (mode === "edit") {
    return (
      <div>
        {body}
        {errorBox}
        <FormActions
          mode="edit"
          status={initial?.status}
          busy={busy}
          onCancel={onCancel}
          onSubmit={submit}
        />
      </div>
    );
  }

  return (
    <div className="content-editor">
      <div className="editor-heading">
        <div>
          <h1>Thêm địa điểm / bảo tàng</h1>
          <p>Tạo địa điểm khám phá mới với hình ảnh và vị trí chính xác.</p>
        </div>
        <span className="editor-draft-label">● Chưa lưu</span>
      </div>
      {body}
      {errorBox}
      <FormActions
        mode="create"
        busy={busy}
        onCancel={onCancel}
        onSubmit={submit}
      />
    </div>
  );
}

/* =====================================================================
   FORM HIỆN VẬT (dùng cho cả Thêm mới và Sửa)
   ===================================================================== */
function ArtifactForm({
  sites = [],
  initial,
  mode = "create",
  onSave,
  onCancel,
}) {
  const [name, setName] = useState(initial?.name || "");
  const [description, setDescription] = useState(initial?.description || "");
  const [siteId, setSiteId] = useState(
    initial?.siteId != null ? String(initial.siteId) : "",
  );
  const [address, setAddress] = useState(initial?.address || "");
  const [latitude, setLatitude] = useState(
    initial?.latitude != null ? String(initial.latitude) : "",
  );
  const [longitude, setLongitude] = useState(
    initial?.longitude != null ? String(initial.longitude) : "",
  );
  const [image, setImage] = useState(initial?.img || "");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  // Chọn địa điểm -> tự điền địa chỉ + tọa độ, bản đồ chỉ đúng vị trí đó
  const selectSite = (id) => {
    setSiteId(id);
    const s = sites.find((x) => same(x.id, id));
    if (s) {
      setAddress(s.address || "");
      setLatitude(s.latitude != null ? String(s.latitude) : "");
      setLongitude(s.longitude != null ? String(s.longitude) : "");
    } else {
      setAddress("");
      setLatitude("");
      setLongitude("");
    }
  };

  const selectedSite = sites.find((x) => same(x.id, siteId));

  const submit = async (action) => {
    if (!name.trim() || !description.trim()) {
      setError("Vui lòng nhập tên hiện vật và mô tả.");
      return;
    }
    if (!selectedSite) {
      setError("Vui lòng chọn địa điểm chứa hiện vật.");
      return;
    }
    const lat = parseCoord(latitude, -90, 90);
    const lng = parseCoord(longitude, -180, 180);
    if (lat === null || lng === null) {
      setError("Tọa độ không hợp lệ.");
      return;
    }
    setError("");
    setBusy(true);
    try {
      await onSave(
        {
          name: name.trim(),
          description: description.trim(),
          siteId: selectedSite.id,
          address: address.trim(),
          latitude: lat,
          longitude: lng,
          img: image,
        },
        action,
      );
    } finally {
      setBusy(false);
    }
  };

  const body = (
    <div className="editor-columns">
      <div className="editor-left-column">
        <section className="editor-card">
          <div className="editor-card-heading">
            <h3>Thông tin cơ bản</h3>
            <p>Nội dung hiển thị trong ứng dụng khám phá.</p>
          </div>
          <label className="editor-field">
            Tên hiện vật *
            <input
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Ví dụ: Xe tăng T-54"
            />
          </label>
          <label className="editor-field">
            Mô tả *
            <textarea
              rows={6}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Nhập thông tin lịch sử, nguồn gốc và ý nghĩa hiện vật..."
            />
          </label>
        </section>
        <section className="editor-card">
          <div className="editor-card-heading">
            <h3>Hình ảnh hiện vật</h3>
            <p>Ảnh nhận diện hiện vật trong ứng dụng.</p>
          </div>
          <ImagePicker value={image} onChange={setImage} onError={setError} />
        </section>
      </div>

      <div className="editor-right-column">
        <section className="editor-card">
          <div className="editor-card-heading">
            <h3>Địa điểm chứa hiện vật *</h3>
            <p>Chọn trong danh sách địa điểm &amp; bảo tàng đã có.</p>
          </div>
          <label className="editor-field">
            Chọn địa điểm
            <select value={siteId} onChange={(e) => selectSite(e.target.value)}>
              <option value="">— Chọn địa điểm —</option>
              {sites.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name}
                  {s.status !== "published" ? " (bản nháp)" : ""}
                </option>
              ))}
            </select>
          </label>
          {sites.length === 0 && (
            <p className="editor-map-note">
              Chưa có địa điểm nào. Hãy tạo địa điểm trước khi thêm hiện vật.
            </p>
          )}
          <label className="editor-field">
            Địa chỉ
            <textarea
              rows={2}
              readOnly
              value={address}
              placeholder="Tự động điền theo địa điểm đã chọn"
            />
          </label>
          <div className="editor-coordinate-grid">
            <label className="editor-field">
              Vĩ độ (Latitude)
              <input
                type="number"
                step="any"
                value={latitude}
                onChange={(e) => setLatitude(e.target.value)}
              />
            </label>
            <label className="editor-field">
              Kinh độ (Longitude)
              <input
                type="number"
                step="any"
                value={longitude}
                onChange={(e) => setLongitude(e.target.value)}
              />
            </label>
          </div>
          <RealMap
            latitude={latitude}
            longitude={longitude}
            onPick={(la, lo) => {
              setLatitude(la.toFixed(6));
              setLongitude(lo.toFixed(6));
            }}
            emptyText="Chọn địa điểm để hiện ghim"
          />
        </section>
      </div>
    </div>
  );

  const errorBox = error && (
    <div className="editor-error" role="alert">
      {error}
    </div>
  );

  if (mode === "edit") {
    return (
      <div>
        {body}
        {errorBox}
        <FormActions
          mode="edit"
          status={initial?.status}
          busy={busy}
          onCancel={onCancel}
          onSubmit={submit}
        />
      </div>
    );
  }

  return (
    <div className="content-editor">
      <div className="editor-heading">
        <div>
          <h1>Thêm hiện vật</h1>
          <p>Thêm hiện vật và liên kết với địa điểm hoặc bảo tàng trưng bày.</p>
        </div>
        <span className="editor-draft-label">● Chưa lưu</span>
      </div>
      {body}
      {errorBox}
      <FormActions
        mode="create"
        busy={busy}
        onCancel={onCancel}
        onSubmit={submit}
      />
    </div>
  );
}

/* =====================================================================
   DANH SÁCH NỘI DUNG (địa điểm + hiện vật) — Ẩn/Hiện · Sửa · Xóa · Xuất bản
   - limit: dùng ở Tổng quan (xem nhanh). Không có limit -> list cuộn đầy đủ.
   ===================================================================== */
function ItemDetail({ it }) {
  return (
    <div className="ad-detail">
      {it.img ? (
        <img src={it.img} alt={it.name} />
      ) : (
        <div className="ad-noimg">Chưa có hình ảnh</div>
      )}
      <dl>
        <div>
          <dt>Tên</dt>
          <dd>{it.name}</dd>
        </div>
        <div>
          <dt>{it.kind === "artifact" ? "Thuộc địa điểm" : "Phân loại"}</dt>
          <dd>{it.kind === "artifact" ? it.siteName : it.classify}</dd>
        </div>
        <div>
          <dt>Địa chỉ</dt>
          <dd>{it.address || "—"}</dd>
        </div>
        <div>
          <dt>Tọa độ</dt>
          <dd>
            {it.latitude != null && it.longitude != null
              ? `${it.latitude}, ${it.longitude}`
              : "—"}
          </dd>
        </div>
        <div>
          <dt>Mô tả</dt>
          <dd>{it.description || "—"}</dd>
        </div>
        {(it.hidden || it.status !== "published") && (
          <div>
            <dd style={{ color: "#9a6200" }}>
              {it.hidden ? "Đang bị ẩn" : "Bản nháp"} — người dùng chưa nhìn
              thấy nội dung này.
            </dd>
          </div>
        )}
      </dl>
    </div>
  );
}

function ContentList({
  items,
  limit,
  onToggleHide,
  onPublish,
  onEdit,
  onDelete,
}) {
  const [q, setQ] = useState("");
  const [type, setType] = useState("all");
  const [open, setOpen] = useState(null);

  const filtered = items.filter(
    (it) =>
      (type === "all" || it.kind === type) &&
      (norm(it.name).includes(norm(q)) || norm(it.address).includes(norm(q))),
  );
  const shown = limit ? filtered.slice(0, limit) : filtered;

  return (
    <>
      <div className="ad-filters">
        <input
          className="ad-input"
          placeholder="Tìm theo tên hoặc địa chỉ..."
          value={q}
          onChange={(e) => setQ(e.target.value)}
        />
        <select
          className="ad-input"
          value={type}
          onChange={(e) => setType(e.target.value)}
        >
          <option value="all">Tất cả loại</option>
          <option value="site">Địa điểm &amp; bảo tàng</option>
          <option value="artifact">Hiện vật</option>
        </select>
      </div>

      <div className={limit ? "ad-list" : "ad-list ad-scroll"}>
        {shown.map((it) => {
          const key = `${it.kind}-${it.id}`;
          const st = statusInfo(it);
          return (
            <div className="ad-item" key={key}>
              <div
                className="ad-row"
                onClick={() => setOpen(open === key ? null : key)}
              >
                <Thumb src={it.img} />
                <div className="ad-info">
                  <b>{it.name}</b>
                  <small>
                    {it.kindLabel} ·{" "}
                    {it.kind === "artifact" ? it.siteName : it.address}
                  </small>
                </div>
                <span className={"admin-status " + st.cls}>{st.label}</span>
                <div
                  className="ad-actions"
                  onClick={(e) => e.stopPropagation()}
                >
                  {it.status !== "published" && (
                    <button
                      type="button"
                      className="ad-pub"
                      onClick={() => onPublish(it)}
                    >
                      <Check size={12} /> Xuất bản
                    </button>
                  )}
                  <button type="button" onClick={() => onToggleHide(it)}>
                    {it.hidden ? <Eye size={12} /> : <EyeOff size={12} />}{" "}
                    {it.hidden ? "Hiện" : "Ẩn"}
                  </button>
                  <button type="button" onClick={() => onEdit(it)}>
                    <Pencil size={12} /> Sửa
                  </button>
                  <button
                    type="button"
                    className="danger"
                    onClick={() => onDelete(it)}
                  >
                    <Trash2 size={12} /> Xóa
                  </button>
                </div>
              </div>
              {open === key && <ItemDetail it={it} />}
            </div>
          );
        })}
        {shown.length === 0 && (
          <p className="ad-empty">Không tìm thấy nội dung phù hợp.</p>
        )}
      </div>
      {limit && filtered.length > limit && (
        <p className="ad-more">
          Còn {filtered.length - limit} nội dung khác — bấm “Xem tất cả”.
        </p>
      )}
    </>
  );
}

/* =====================================================================
   TRANG "ĐỊA ĐIỂM & BẢO TÀNG": Thêm mới | Danh sách địa điểm & hiện vật
   ===================================================================== */
function PlacesPage({
  tab,
  setTab,
  sites,
  artifacts,
  listens = [],
  onCreateSite,
  onEdit,
  onDelete,
}) {
  const [q, setQ] = useState("");
  const [open, setOpen] = useState({});

  const filtered = sites.filter(
    (s) =>
      norm(s.name).includes(norm(q)) ||
      artifacts.some(
        (a) => same(a.siteId, s.id) && norm(a.name).includes(norm(q)),
      ),
  );

  return (
    <div>
      <div className="ad-tabs">
        <button
          type="button"
          className={tab === "add" ? "on" : ""}
          onClick={() => setTab("add")}
        >
          Thêm địa điểm / bảo tàng
        </button>
        <button
          type="button"
          className={tab === "list" ? "on" : ""}
          onClick={() => setTab("list")}
        >
          Danh sách địa điểm &amp; hiện vật
        </button>
      </div>

      {tab === "add" ? (
        <SiteForm
          mode="create"
          onCancel={() => setTab("list")}
          onSave={async (payload, action) => {
            const ok = await onCreateSite(payload, action);
            if (ok) setTab("list");
          }}
        />
      ) : (
        <div className="ad-panel">
          <h3>Danh sách địa điểm &amp; hiện vật</h3>
          <p>
            Bấm “Hiện vật” ở mỗi địa điểm để xem các hiện vật đang được chứa.
          </p>
          <div className="ad-filters">
            <input
              className="ad-input"
              placeholder="Tìm địa điểm hoặc hiện vật..."
              value={q}
              onChange={(e) => setQ(e.target.value)}
            />
          </div>
          <div className="ad-list ad-scroll">
            {filtered.map((s) => {
              const arts = artifacts.filter((a) => same(a.siteId, s.id));
              const siteListens = listens
                .filter(
                  (l) => l.targetType === "site" && same(l.targetId, s.id),
                )
                .reduce((n, l) => n + Number(l.count || 0), 0);
              const st = statusInfo(s);
              const isOpen = Boolean(open[s.id]);
              const siteItem = { ...s, kind: "site", kindLabel: s.classify };
              return (
                <div className="ad-item" key={s.id}>
                  <div className="ad-row" style={{ cursor: "default" }}>
                    <Thumb src={s.img} />
                    <div className="ad-info">
                      <b>{s.name}</b>
                      <small>
                        {s.classify} · {s.address} ·{" "}
                        {siteListens.toLocaleString("vi-VN")} lượt nghe
                      </small>
                    </div>
                    <span className={"admin-status " + st.cls}>{st.label}</span>
                    <div className="ad-actions">
                      <button
                        type="button"
                        onClick={() =>
                          setOpen((p) => ({ ...p, [s.id]: !p[s.id] }))
                        }
                      >
                        <Landmark size={12} /> {isOpen ? "Ẩn" : "Hiện"} hiện vật
                        ({arts.length})
                        <ChevronDown
                          size={12}
                          style={{
                            transform: isOpen ? "rotate(180deg)" : "none",
                            transition: ".15s",
                          }}
                        />
                      </button>
                      <button type="button" onClick={() => onEdit(siteItem)}>
                        <Pencil size={12} /> Sửa
                      </button>
                      <button
                        type="button"
                        className="danger"
                        onClick={() => onDelete(siteItem)}
                      >
                        <Trash2 size={12} /> Xóa
                      </button>
                    </div>
                  </div>
                  {isOpen && (
                    <div className="ad-sub">
                      {arts.length === 0 && (
                        <p className="ad-empty">
                          Địa điểm này chưa có hiện vật.
                        </p>
                      )}
                      {arts.map((a) => {
                        const artListens = listens
                          .filter(
                            (l) =>
                              l.targetType === "artifact" &&
                              same(l.targetId, a.id),
                          )
                          .reduce((n, l) => n + Number(l.count || 0), 0);
                        const ast = statusInfo(a);
                        const artItem = {
                          ...a,
                          kind: "artifact",
                          kindLabel: "Hiện vật",
                          siteName: s.name,
                        };
                        return (
                          <div className="ad-row" key={a.id}>
                            <Thumb src={a.img} />
                            <div className="ad-info">
                              <b>{a.name}</b>
                              <small>
                                {a.description} ·{" "}
                                {artListens.toLocaleString("vi-VN")} lượt nghe
                              </small>
                            </div>
                            <span className={"admin-status " + ast.cls}>
                              {ast.label}
                            </span>
                            <div className="ad-actions">
                              <button
                                type="button"
                                onClick={() => onEdit(artItem)}
                              >
                                <Pencil size={12} /> Sửa
                              </button>
                              <button
                                type="button"
                                className="danger"
                                onClick={() => onDelete(artItem)}
                              >
                                <Trash2 size={12} /> Xóa
                              </button>
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>
              );
            })}
            {filtered.length === 0 && (
              <p className="ad-empty">Chưa có địa điểm nào.</p>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

/* =====================================================================
   BIỂU ĐỒ LƯỢT KHÁM PHÁ THEO NGÀY (cột + đường) — SVG thuần
   ===================================================================== */
function ListenChart({ listens, days = 14 }) {
  const series = useMemo(() => {
    const byDate = {};
    listens.forEach((l) => {
      byDate[l.date] = (byDate[l.date] || 0) + (Number(l.count) || 0);
    });
    const out = [];
    const today = new Date();
    for (let i = days - 1; i >= 0; i--) {
      const d = new Date(today);
      d.setDate(today.getDate() - i);
      const key = dateKey(d);
      out.push({
        key,
        label: `${String(d.getDate()).padStart(2, "0")}/${String(d.getMonth() + 1).padStart(2, "0")}`,
        value: byDate[key] || 0,
      });
    }
    return out;
  }, [listens, days]);

  const max = Math.max(...series.map((s) => s.value), 1);
  const raw = max / 4;
  const pow = Math.pow(10, Math.floor(Math.log10(raw)));
  const n = raw / pow;
  const step = (n <= 1 ? 1 : n <= 2 ? 2 : n <= 5 ? 5 : 10) * pow;
  const top = step * 4;

  const W = 700;
  const H = 230;
  const pl = 46;
  const pr = 12;
  const pt = 14;
  const pb = 28;
  const pw = W - pl - pr;
  const ph = H - pt - pb;
  const slot = pw / series.length;
  const x = (i) => pl + slot * (i + 0.5);
  const y = (v) => pt + ph * (1 - v / top);
  const line = series
    .map(
      (s, i) => `${i ? "L" : "M"}${x(i).toFixed(1)} ${y(s.value).toFixed(1)}`,
    )
    .join(" ");

  return (
    <svg
      className="ad-chart"
      viewBox={`0 0 ${W} ${H}`}
      role="img"
      aria-label="Biểu đồ lượt khám phá theo ngày"
    >
      {[0, 1, 2, 3, 4].map((k) => (
        <g key={k}>
          <line
            x1={pl}
            x2={W - pr}
            y1={y(step * k)}
            y2={y(step * k)}
            stroke="#e8edf3"
          />
          <text x={pl - 6} y={y(step * k) + 3} textAnchor="end">
            {(step * k).toLocaleString("vi-VN")}
          </text>
        </g>
      ))}
      {series.map((s, i) => (
        <rect
          key={s.key}
          x={x(i) - slot * 0.28}
          y={y(s.value)}
          width={slot * 0.56}
          height={Math.max(0, pt + ph - y(s.value))}
          rx="3"
          fill="#cfe0ff"
        >
          <title>
            {s.label}: {s.value.toLocaleString("vi-VN")} lượt
          </title>
        </rect>
      ))}
      {series.map(
        (s, i) =>
          s.value > 0 && (
            <text
              key={`bar-label-${s.key}`}
              x={x(i)}
              y={
                y(s.value) +
                Math.max(12, Math.min(20, (pt + ph - y(s.value)) / 2))
              }
              textAnchor="middle"
              fontSize="10"
              fontWeight="700"
              fill="#344054"
            >
              {s.value.toLocaleString("vi-VN")}
            </text>
          ),
      )}
      <path
        d={line}
        fill="none"
        stroke="#3978f6"
        strokeWidth="2.2"
        strokeLinejoin="round"
      />
      {series.map((s, i) => (
        <circle
          key={s.key}
          cx={x(i)}
          cy={y(s.value)}
          r="3"
          fill="#fff"
          stroke="#3978f6"
          strokeWidth="2"
        />
      ))}
      {series.map(
        (s, i) =>
          i % 2 === 0 && (
            <text key={s.key} x={x(i)} y={H - 8} textAnchor="middle">
              {s.label}
            </text>
          ),
      )}
    </svg>
  );
}

/* =====================================================================
   BIÊN TẬP THUYẾT MINH
   ===================================================================== */
function Toggle({ checked, onChange, children }) {
  return (
    <label className="ad-toggle">
      <input
        type="checkbox"
        checked={checked}
        onChange={(e) => onChange(e.target.checked)}
      />
      <i />
      {children}
    </label>
  );
}

function RadiusInput({ value, onChange, max = 2000 }) {
  return (
    <div>
      <input
        className="ad-input"
        type="number"
        min="1"
        max={max}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder="Nhập bán kính (mét)"
      />
      <div className="ad-chips">
        {RADIUS_SUGGESTIONS.map((r) => (
          <button
            key={r}
            type="button"
            className={String(value) === String(r) ? "on" : ""}
            onClick={() => onChange(String(r))}
          >
            {r} m
          </button>
        ))}
      </div>
    </div>
  );
}

const defaultNarration = (target, langs) => ({
  image: target.img || "",
  text: "",
  useDescription: false,
  ttsEnabled: true,
  audioEnabled: false,
  language: langs[0]?.code || "vi",
  speed: 1,
  audioName: "",
  audioData: "",
  gps: {
    enabled: true,
    latitude: target.latitude != null ? String(target.latitude) : "",
    longitude: target.longitude != null ? String(target.longitude) : "",
    radius: "50",
  },
  bluetooth: {
    enabled: false,
    deviceName: `Beacon - ${target.name}`,
    mac: "",
    radius: "5",
  },
  qr: { enabled: true, language: langs[0]?.code || "vi" },
});

function NarrationEditor({
  sites,
  artifacts,
  narrations,
  languages,
  initial,
  onSave,
  onCancel,
}) {
  const [targetType, setTargetType] = useState(initial?.type || "artifact");
  const [targetId, setTargetId] = useState(initial?.id ?? null);
  const [f, setF] = useState(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [previewSrc, setPreviewSrc] = useState("tts");
  const [speaking, setSpeaking] = useState(false);
  const [previewText, setPreviewText] = useState("");
  const [previewLoading, setPreviewLoading] = useState(false);
  const [translations, setTranslations] = useState({});
  const [bluetoothDevices, setBluetoothDevices] = useState([]);
  const [bluetoothScanning, setBluetoothScanning] = useState(false);
  const audioRef = useRef(null);
  const qrCanvasRef = useRef(null); // hook phải nằm trước các lệnh return sớm bên dưới

  const langs = languages.filter((l) => l.enabled !== false);
  const list = targetType === "site" ? sites : artifacts;
  const target = list.find((t) => same(t.id, targetId));
  const set = (patch) => setF((p) => ({ ...p, ...patch }));
  const setIn = (key, patch) =>
    setF((p) => ({ ...p, [key]: { ...p[key], ...patch } }));

  useEffect(() => {
    if (targetId == null && list.length) setTargetId(list[0].id);
  }, [list, targetId]);

  // Đổi đối tượng -> nạp thuyết minh đã có hoặc giá trị mặc định
  useEffect(() => {
    window.speechSynthesis?.cancel();
    setSpeaking(false);
    if (!target) {
      setF(null);
      return;
    }
    const d = defaultNarration(target, langs);
    const n = narrations.find(
      (x) => x.targetType === targetType && same(x.targetId, target.id),
    );
    setTranslations(n?.translations || n?.texts || {});
    setPreviewText("");
    if (n) {
      setF({
        ...d,
        ...n,
        gps: {
          ...d.gps,
          ...n.gps,
          latitude: String(n.gps?.latitude ?? d.gps.latitude),
          longitude: String(n.gps?.longitude ?? d.gps.longitude),
          radius: String(n.gps?.radius ?? 50),
        },
        bluetooth: {
          ...d.bluetooth,
          ...n.bluetooth,
          radius: String(n.bluetooth?.radius ?? 5),
        },
        qr: { ...d.qr, ...n.qr },
      });
    } else {
      setF(d);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [targetType, targetId, Boolean(target)]);

  useEffect(() => () => window.speechSynthesis?.cancel(), []);

  useEffect(() => {
    let alive = true;
    const source = f
      ? f.useDescription
        ? target?.description || ""
        : f.text || ""
      : "";
    if (!source) {
      setPreviewText("");
      return () => {
        alive = false;
      };
    }
    if (f.language === "vi") {
      setPreviewText(source);
      return () => {
        alive = false;
      };
    }
    const saved = translations?.[f.language];
    if (saved && saved.trim()) {
      setPreviewText(saved);
      return () => {
        alive = false;
      };
    }
    setPreviewLoading(true);
    const url = `https://translate.googleapis.com/translate_a/single?client=gtx&sl=vi&tl=${encodeURIComponent(f.language)}&dt=t&q=${encodeURIComponent(source)}`;
    fetch(url)
      .then((r) => {
        if (!r.ok) throw new Error("Không thể dịch văn bản");
        return r.json();
      })
      .then((data) => {
        const translated = (data[0] || [])
          .map((part) => part[0] || "")
          .join("");
        if (!translated) throw new Error("Bản dịch rỗng");
        if (alive) {
          setPreviewText(translated);
          setTranslations((old) => ({ ...old, [f.language]: translated }));
        }
      })
      .catch(() => {
        if (alive) setPreviewText("");
      })
      .finally(() => {
        if (alive) setPreviewLoading(false);
      });
    return () => {
      alive = false;
    };
  }, [
    f?.language,
    f?.text,
    f?.useDescription,
    target?.description,
    target?.id,
    translations,
  ]);

  if (!target || !f) {
    return (
      <div className="ad-panel">
        <h3>Thông tin thuyết minh</h3>
        <label className="editor-field">
          Loại nội dung
          <select
            value={targetType}
            onChange={(e) => {
              setTargetType(e.target.value);
              setTargetId(null);
            }}
          >
            <option value="site">Địa điểm</option>
            <option value="artifact">Hiện vật</option>
          </select>
        </label>
        <p className="ad-empty">
          Chưa có {targetType === "site" ? "địa điểm" : "hiện vật"} nào để gắn
          thuyết minh.
        </p>
      </div>
    );
  }

  const content = f.useDescription ? target.description || "" : f.text;
  const langObj = languages.find((l) => l.code === f.language);
  const hasNarrated = narrations.some(
    (x) => x.targetType === targetType && same(x.targetId, target.id),
  );

  const discoverBluetoothDevice = async () => {
    if (!navigator.bluetooth) {
      setError(
        "Trình duyệt không hỗ trợ Web Bluetooth. Hãy dùng Chrome/Edge mới trên HTTPS hoặc localhost.",
      );
      return;
    }
    setBluetoothScanning(true);
    try {
      // Web Bluetooth chỉ cho người dùng chọn thiết bị được trình duyệt hỗ trợ;
      // không thể quét thụ động toàn bộ beacon/MAC như ứng dụng native.
      const device = await navigator.bluetooth.requestDevice({
        acceptAllDevices: true,
        optionalServices: ["generic_access"],
      });
      const row = {
        name: device.name || "Thiết bị không có tên",
        id: device.id || "",
      };
      setBluetoothDevices((old) =>
        old.some((x) => x.id === row.id) ? old : [...old, row],
      );
      setIn("bluetooth", { deviceName: row.name, mac: row.id });
      setError("");
    } catch (e) {
      if (e.name !== "NotFoundError")
        setError(`Không chọn được thiết bị Bluetooth: ${e.message || e}`);
    } finally {
      setBluetoothScanning(false);
    }
  };

  const stop = () => {
    window.speechSynthesis?.cancel();
    audioRef.current?.pause();
    setSpeaking(false);
  };

  const play = () => {
    if (speaking) return stop();
    const src =
      f.ttsEnabled && f.audioEnabled
        ? previewSrc
        : f.audioEnabled
          ? "audio"
          : "tts";
    if (src === "audio") {
      if (!f.audioData) return setError("Chưa có tệp âm thanh để nghe thử.");
      setError("");
      audioRef.current.currentTime = 0;
      audioRef.current.play();
      return setSpeaking(true);
    }
    if (!("speechSynthesis" in window))
      return setError("Trình duyệt không hỗ trợ đọc văn bản (TTS).");
    if (!content.trim())
      return setError(
        "Hãy nhập lời thuyết minh hoặc chọn dùng mô tả để nghe thử.",
      );
    if (f.language !== "vi" && previewLoading)
      return setError(
        "Đang dịch nội dung, vui lòng đợi một chút rồi nghe thử.",
      );
    if (f.language !== "vi" && !previewText.trim())
      return setError(
        "Chưa dịch được nội dung. Kiểm tra kết nối mạng rồi chọn lại ngôn ngữ.",
      );
    setError("");
    const u = new SpeechSynthesisUtterance(
      f.language === "vi" ? content : previewText,
    );
    u.lang = langObj?.ttsKey || "vi-VN";
    u.rate = Number(f.speed) || 1;
    u.onend = () => setSpeaking(false);
    u.onerror = () => setSpeaking(false);
    window.speechSynthesis.cancel();
    window.speechSynthesis.speak(u);
    setSpeaking(true);
  };

  const pickAudio = async (e) => {
    const file = e.target.files?.[0];
    e.target.value = "";
    if (!file) return;
    try {
      const data = await readFile(file, {
        prefix: "audio/",
        maxMB: 8,
        label: "âm thanh",
      });
      set({ audioData: data, audioName: file.name });
      setError("");
    } catch (err) {
      setError(err.message);
    }
  };

  const qrCode = `${targetType === "site" ? "DD" : "HV"}-${target.id}`;
  const qrLink = api.buildQrLink({
    targetType,
    targetId: target.id,
    language: f.qr.language,
  });

  // QR vẽ ngay trên trình duyệt bằng qrcode.react (canvas 600 px), không cần Internet.
  const downloadQr = () => {
    const canvas = qrCanvasRef.current;
    if (!canvas) return setError("Chưa có ảnh QR để tải.");
    canvas.toBlob((blob) => {
      if (!blob) return setError("Không tạo được ảnh QR.");
      const a = document.createElement("a");
      a.href = URL.createObjectURL(blob);
      a.download = `${qrCode}-${f.qr.language}.png`;
      a.click();
      URL.revokeObjectURL(a.href);
    }, "image/png");
  };

  const save = async () => {
    if (!f.ttsEnabled && !f.audioEnabled)
      return setError(
        "Hãy bật ít nhất một cách thuyết minh: TTS hoặc tải audio.",
      );
    if (f.ttsEnabled && !content.trim())
      return setError("Vui lòng nhập lời thuyết minh hoặc chọn dùng mô tả.");
    if (f.audioEnabled && !f.audioData)
      return setError("Vui lòng tải lên tệp audio hoặc tắt chế độ này.");
    const lat = parseCoord(f.gps.latitude, -90, 90);
    const lng = parseCoord(f.gps.longitude, -180, 180);
    const gpsRadius = Number(f.gps.radius);
    if (f.gps.enabled && (lat === null || lng === null || !(gpsRadius > 0)))
      return setError("GPS: tọa độ hoặc bán kính không hợp lệ.");
    if (f.bluetooth.enabled && !f.bluetooth.deviceName.trim())
      return setError("Bluetooth: vui lòng nhập tên thiết bị.");
    setError("");
    setBusy(true);
    try {
      await onSave({
        targetType,
        targetId: target.id,
        image: f.image,
        text: f.text,
        useDescription: f.useDescription,
        ttsEnabled: f.ttsEnabled,
        audioEnabled: f.audioEnabled,
        language: f.language,
        translations: {
          ...(f.translations || {}),
          ...(translations || {}),
          ...(f.language && f.language !== "vi" && previewText
            ? { [f.language]: previewText }
            : {}),
        },
        speed: Number(f.speed) || 1,
        audioName: f.audioName,
        audioData: f.audioData,
        gps: {
          enabled: f.gps.enabled,
          latitude: lat,
          longitude: lng,
          radius: gpsRadius || 0,
        },
        bluetooth: {
          enabled: f.bluetooth.enabled,
          deviceName: f.bluetooth.deviceName.trim(),
          mac: f.bluetooth.mac.trim(),
          radius: Number(f.bluetooth.radius) || 0,
        },
        qr: {
          enabled: f.qr.enabled,
          code: qrCode,
          language: f.qr.language,
          link: qrLink,
        },
      });
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="content-editor">
      <div className="editor-columns">
        <div className="editor-left-column">
          <section className="editor-card">
            <div className="editor-card-heading">
              <h3>Thông tin thuyết minh</h3>
              <p>Chọn đối tượng cần gắn nội dung thuyết minh.</p>
            </div>
            <div className="editor-coordinate-grid">
              <label className="editor-field">
                Loại nội dung
                <select
                  value={targetType}
                  onChange={(e) => {
                    setTargetType(e.target.value);
                    setTargetId(null);
                  }}
                >
                  <option value="site">Địa điểm</option>
                  <option value="artifact">Hiện vật</option>
                </select>
              </label>
              <label className="editor-field">
                Nội dung cần gắn *
                <select
                  value={target.id}
                  onChange={(e) =>
                    setTargetId(
                      list.find((t) => same(t.id, e.target.value))?.id,
                    )
                  }
                >
                  {list.map((t) => (
                    <option key={t.id} value={t.id}>
                      {t.name}
                      {narrations.some(
                        (n) =>
                          n.targetType === targetType && same(n.targetId, t.id),
                      )
                        ? " ✓"
                        : ""}
                    </option>
                  ))}
                </select>
              </label>
            </div>
            <div className="ad-linked">
              {f.image || target.img ? (
                <img src={f.image || target.img} alt={target.name} />
              ) : (
                <Thumb />
              )}
              <div>
                <b>{target.name}</b>
                <small>
                  {targetType === "site" ? "Địa điểm" : "Hiện vật"} ·{" "}
                  {hasNarrated
                    ? "Đã có thuyết minh (đang chỉnh sửa)"
                    : "Chưa có thuyết minh"}
                </small>
              </div>
            </div>
          </section>

          <section className="editor-card">
            <div className="editor-card-heading">
              <h3>Ảnh minh họa</h3>
              <p>Mặc định lấy ảnh của đối tượng, có thể thay ảnh khác.</p>
            </div>
            <ImagePicker
              value={f.image}
              onChange={(v) => set({ image: v })}
              onError={setError}
            />
          </section>

          <section className="editor-card">
            <div className="editor-card-heading">
              <h3>Lời thuyết minh &amp; audio</h3>
              <p>
                Có thể bật một hoặc cả hai: chuyển văn bản thành giọng nói (TTS)
                và/hoặc tải audio.
              </p>
            </div>

            <div className={"ad-sec" + (f.ttsEnabled ? "" : " off")}>
              <Toggle
                checked={f.ttsEnabled}
                onChange={(v) => set({ ttsEnabled: v })}
              >
                Chuyển văn bản thành giọng nói (TTS)
              </Toggle>
              {f.ttsEnabled && (
                <div style={{ marginTop: 12 }}>
                  <Toggle
                    checked={f.useDescription}
                    onChange={(v) => set({ useDescription: v })}
                  >
                    Dùng mô tả của{" "}
                    {targetType === "site" ? "địa điểm" : "hiện vật"} làm thuyết
                    minh
                  </Toggle>
                  <label className="editor-field" style={{ marginTop: 10 }}>
                    Lời thuyết minh *
                    <textarea
                      rows={5}
                      value={content}
                      readOnly={f.useDescription}
                      onChange={(e) => set({ text: e.target.value })}
                      placeholder="Nhập văn bản cần thuyết minh..."
                    />
                  </label>
                  <div className="editor-coordinate-grid">
                    <label className="editor-field">
                      Ngôn ngữ (key TTS)
                      <select
                        value={f.language}
                        onChange={(e) => set({ language: e.target.value })}
                      >
                        {langs.map((l) => (
                          <option key={l.id} value={l.code}>
                            {l.name} ({l.ttsKey})
                          </option>
                        ))}
                      </select>
                    </label>
                    <label className="editor-field">
                      Tốc độ đọc: {Number(f.speed).toFixed(2)}x
                      <input
                        type="range"
                        min="0.5"
                        max="2"
                        step="0.25"
                        value={f.speed}
                        onChange={(e) => set({ speed: Number(e.target.value) })}
                      />
                    </label>
                  </div>
                  <div
                    className="ad-sec"
                    style={{ marginTop: 10, background: "#f7faff" }}
                  >
                    <b style={{ fontSize: 12 }}>
                      Bản dịch nghe thử · {langObj?.name || f.language}
                    </b>
                    <p
                      style={{
                        fontSize: 11,
                        color: "#7c8ba0",
                        margin: "5px 0 8px",
                      }}
                    >
                      {f.language === "vi"
                        ? "Đang dùng nguyên văn tiếng Việt."
                        : previewLoading
                          ? "Đang dịch nội dung sang ngôn ngữ đã chọn…"
                          : "Nội dung bên dưới là bản sẽ được đọc khi nghe thử."}
                    </p>
                    <div
                      style={{
                        fontSize: 12,
                        lineHeight: 1.6,
                        whiteSpace: "pre-wrap",
                      }}
                    >
                      {f.language === "vi"
                        ? content
                        : previewText ||
                          (previewLoading
                            ? "Đang dịch…"
                            : "Chưa có bản dịch xem trước.")}
                    </div>
                  </div>
                </div>
              )}
            </div>

            <div className={"ad-sec" + (f.audioEnabled ? "" : " off")}>
              <Toggle
                checked={f.audioEnabled}
                onChange={(v) => set({ audioEnabled: v })}
              >
                Đăng tải audio thuyết minh
              </Toggle>
              {f.audioEnabled && (
                <div style={{ marginTop: 12 }}>
                  <label className="ad-btn" style={{ position: "relative" }}>
                    <UploadCloud size={13} />{" "}
                    {f.audioName || "Chọn tệp MP3, WAV, M4A"}
                    <input
                      type="file"
                      accept="audio/*"
                      onChange={pickAudio}
                      style={{
                        position: "absolute",
                        inset: 0,
                        opacity: 0,
                        cursor: "pointer",
                      }}
                    />
                  </label>
                  {f.audioData && (
                    <button
                      type="button"
                      className="ad-btn danger"
                      style={{ marginLeft: 8 }}
                      onClick={() => set({ audioData: "", audioName: "" })}
                    >
                      <Trash2 size={12} /> Gỡ tệp
                    </button>
                  )}
                </div>
              )}
            </div>

            <div className="ad-play">
              <button
                type="button"
                className="round"
                onClick={play}
                aria-label={speaking ? "Dừng" : "Nghe thử"}
              >
                {speaking ? <Square size={12} /> : <Play size={13} />}
              </button>
              <div style={{ flex: 1, minWidth: 140 }}>
                <b style={{ fontSize: 12 }}>Nghe thử thuyết minh</b>
                <small
                  style={{ display: "block", color: "#7c8ba0", fontSize: 10 }}
                >
                  {f.ttsEnabled && f.audioEnabled
                    ? "Chọn nguồn để nghe thử"
                    : f.audioEnabled
                      ? "Phát tệp audio đã tải lên"
                      : `Đọc văn bản bằng giọng ${langObj?.name || f.language} (dịch tự động trước khi phát)`}
                </small>
              </div>
              {f.ttsEnabled && f.audioEnabled && (
                <div className="ad-seg">
                  <button
                    type="button"
                    className={previewSrc === "tts" ? "on" : ""}
                    onClick={() => {
                      stop();
                      setPreviewSrc("tts");
                    }}
                  >
                    TTS
                  </button>
                  <button
                    type="button"
                    className={previewSrc === "audio" ? "on" : ""}
                    onClick={() => {
                      stop();
                      setPreviewSrc("audio");
                    }}
                  >
                    Audio
                  </button>
                </div>
              )}
              <Volume2 size={16} color="#6b7a90" />
              <audio
                ref={audioRef}
                src={f.audioData || undefined}
                onEnded={() => setSpeaking(false)}
                style={{ display: "none" }}
              />
            </div>
          </section>
        </div>

        <div className="editor-right-column">
          <section className="editor-card">
            <div className="editor-card-heading">
              <h3>GPS · Vùng kích hoạt</h3>
              <p>Tự phát thuyết minh khi người dùng bước vào vùng ảnh hưởng.</p>
            </div>
            <Toggle
              checked={f.gps.enabled}
              onChange={(v) => setIn("gps", { enabled: v })}
            >
              Bật GPS
            </Toggle>
            <label className="editor-field" style={{ marginTop: 12 }}>
              {targetType === "site" ? "Địa điểm" : "Hiện vật"} liên kết
              <input value={target.name} readOnly />
            </label>
            <div className="editor-coordinate-grid">
              <label className="editor-field">
                Vĩ độ
                <input
                  type="number"
                  step="any"
                  value={f.gps.latitude}
                  onChange={(e) => setIn("gps", { latitude: e.target.value })}
                />
              </label>
              <label className="editor-field">
                Kinh độ
                <input
                  type="number"
                  step="any"
                  value={f.gps.longitude}
                  onChange={(e) => setIn("gps", { longitude: e.target.value })}
                />
              </label>
            </div>
            <label className="editor-field">
              Bán kính kích hoạt (mét)
              <RadiusInput
                value={f.gps.radius}
                onChange={(v) => setIn("gps", { radius: v })}
              />
            </label>
            <RealMap
              latitude={f.gps.latitude}
              longitude={f.gps.longitude}
              radius={f.gps.enabled ? f.gps.radius : 0}
              emptyText="Đối tượng chưa có tọa độ"
              height={300}
            />
            <p className="editor-map-note">
              Ghim đỏ là vị trí đối tượng, vòng tròn xanh là vùng sẽ phát audio
              khi người dùng bước vào.
            </p>
          </section>

          <section className="editor-card">
            <div className="editor-card-heading">
              <h3>Bluetooth · Thiết bị phát</h3>
              <p>Phát khi phát hiện đúng thiết bị beacon gần đối tượng.</p>
            </div>
            <Toggle
              checked={f.bluetooth.enabled}
              onChange={(v) => setIn("bluetooth", { enabled: v })}
            >
              Bật Bluetooth
            </Toggle>
            <button
              type="button"
              className="admin-secondary-button"
              style={{ marginTop: 10 }}
              onClick={discoverBluetoothDevice}
              disabled={bluetoothScanning}
            >
              <Bluetooth size={15} />{" "}
              {bluetoothScanning
                ? "Đang mở danh sách thiết bị…"
                : "Tìm / chọn thiết bị Bluetooth"}
            </button>
            <p className="editor-map-note">
              Lưu ý: Chrome/Edge không cho trang web quét thụ động tất cả sóng
              beacon hoặc đọc RSSI/MAC tùy ý. Nút này mở hộp thoại chọn thiết bị
              mà trình duyệt cho phép.
            </p>
            {bluetoothDevices.length > 0 && (
              <label className="editor-field">
                Thiết bị đã chọn
                <select
                  value={f.bluetooth.mac}
                  onChange={(e) => {
                    const d = bluetoothDevices.find(
                      (x) => x.id === e.target.value,
                    );
                    if (d)
                      setIn("bluetooth", { deviceName: d.name, mac: d.id });
                  }}
                >
                  {bluetoothDevices.map((d) => (
                    <option key={d.id} value={d.id}>
                      {d.name} {d.id ? `(${d.id})` : ""}
                    </option>
                  ))}
                </select>
              </label>
            )}
            <label className="editor-field" style={{ marginTop: 12 }}>
              Tên thiết bị beacon
              <input
                value={f.bluetooth.deviceName}
                onChange={(e) =>
                  setIn("bluetooth", { deviceName: e.target.value })
                }
                placeholder="Ví dụ: Beacon - Xe tăng"
              />
            </label>
            <div className="editor-coordinate-grid">
              <label className="editor-field">
                Địa chỉ MAC
                <input
                  value={f.bluetooth.mac}
                  onChange={(e) => setIn("bluetooth", { mac: e.target.value })}
                  placeholder="AA:BB:CC:DD:EE:FF"
                />
              </label>
              <label className="editor-field">
                Phạm vi nhận (mét)
                <input
                  type="number"
                  min="1"
                  max="100"
                  value={f.bluetooth.radius}
                  onChange={(e) =>
                    setIn("bluetooth", { radius: e.target.value })
                  }
                />
              </label>
            </div>
          </section>

          <section className="editor-card">
            <div className="editor-card-heading">
              <h3>QR · Phát thuyết minh tức thì</h3>
              <p>
                Người dùng quét mã sẽ tự bật audio ngay theo ngôn ngữ của mã.
              </p>
            </div>
            <Toggle
              checked={f.qr.enabled}
              onChange={(v) => setIn("qr", { enabled: v })}
            >
              Bật QR
            </Toggle>
            {f.qr.enabled && (
              <>
                <label className="editor-field" style={{ marginTop: 12 }}>
                  Ngôn ngữ của mã QR
                  <select
                    value={f.qr.language}
                    onChange={(e) => setIn("qr", { language: e.target.value })}
                  >
                    {langs.map((l) => (
                      <option key={l.id} value={l.code}>
                        {l.name}
                      </option>
                    ))}
                  </select>
                </label>
                <div className="ad-qr">
                  <QRCodeCanvas ref={qrCanvasRef} value={qrLink} size={600} level="M" marginSize={4} aria-label={`QR ${qrCode}`} />
                  <div style={{ flex: 1, minWidth: 160 }}>
                    <b style={{ fontSize: 12 }}>{target.name}</b>
                    <small>Mã nội dung: {qrCode}</small>
                    <small>{qrLink}</small>
                    <div className="ad-actions" style={{ marginTop: 10 }}>
                      <button type="button" onClick={downloadQr}>
                        <Download size={12} /> Tải PNG
                      </button>
                      <button
                        type="button"
                        onClick={() =>
                          navigator.clipboard
                            ?.writeText(qrLink)
                            .then(() => setError(""))
                        }
                      >
                        <Copy size={12} /> Sao chép link
                      </button>
                    </div>
                  </div>
                </div>
                <p className="editor-map-note">
                  Mỗi ngôn ngữ có một mã QR riêng. Hãy in mã đúng ngôn ngữ đặt
                  cạnh {targetType === "site" ? "địa điểm" : "hiện vật"}.
                </p>
              </>
            )}
          </section>
        </div>
      </div>

      {error && (
        <div className="editor-error" role="alert">
          {error}
        </div>
      )}

      <div className="editor-bottom-bar">
        <div>
          <b>Lưu thuyết minh cho “{target.name}”</b>
          <p>
            Nội dung sẽ được Google Dịch sang ngôn ngữ người dùng chọn ở app
            user.
          </p>
        </div>
        <div className="editor-bottom-actions">
          <button type="button" onClick={onCancel} disabled={busy}>
            Hủy
          </button>
          <button
            type="button"
            className="admin-primary"
            onClick={save}
            disabled={busy}
          >
            <Save size={14} /> Lưu thuyết minh
          </button>
        </div>
      </div>
    </div>
  );
}

/* =====================================================================
   DANH SÁCH THUYẾT MINH (đã / chưa thuyết minh)
   ===================================================================== */
function NarrationList({
  sites,
  artifacts,
  narrations,
  languages,
  onEdit,
  onDelete,
}) {
  const [q, setQ] = useState("");
  const [type, setType] = useState("all");
  const [state, setState] = useState("all");
  const [open, setOpen] = useState(null);

  const rows = [
    ...sites.map((s) => ({
      ...s,
      targetType: "site",
      label: s.classify || "Địa điểm",
    })),
    ...artifacts.map((a) => ({
      ...a,
      targetType: "artifact",
      label: "Hiện vật",
    })),
  ]
    .map((t) => ({
      ...t,
      narration: narrations.find(
        (n) => n.targetType === t.targetType && same(n.targetId, t.id),
      ),
    }))
    .filter(
      (t) =>
        norm(t.name).includes(norm(q)) &&
        (type === "all" || t.targetType === type) &&
        (state === "all" || (state === "done" ? t.narration : !t.narration)),
    );

  const langName = (code) =>
    languages.find((l) => l.code === code)?.name || code;

  return (
    <div className="ad-panel">
      <h3>Danh sách thuyết minh</h3>
      <p>Các địa điểm và hiện vật đã hoặc chưa được cài đặt thuyết minh.</p>
      <div className="ad-filters">
        <input
          className="ad-input"
          placeholder="Tìm theo tên..."
          value={q}
          onChange={(e) => setQ(e.target.value)}
        />
        <select
          className="ad-input"
          value={type}
          onChange={(e) => setType(e.target.value)}
        >
          <option value="all">Tất cả loại</option>
          <option value="site">Địa điểm</option>
          <option value="artifact">Hiện vật</option>
        </select>
        <select
          className="ad-input"
          value={state}
          onChange={(e) => setState(e.target.value)}
        >
          <option value="all">Tất cả trạng thái</option>
          <option value="done">Đã thuyết minh</option>
          <option value="none">Chưa thuyết minh</option>
        </select>
      </div>

      <div className="ad-list ad-scroll">
        {rows.map((t) => {
          const key = `${t.targetType}-${t.id}`;
          const n = t.narration;
          return (
            <div className="ad-item" key={key}>
              <div
                className="ad-row"
                onClick={() => setOpen(open === key ? null : key)}
              >
                <Thumb src={t.img} />
                <div className="ad-info">
                  <b>{t.name}</b>
                  <small>{t.label}</small>
                </div>
                <span className={"admin-status " + (n ? "published" : "draft")}>
                  {n ? "Đã thuyết minh" : "Chưa thuyết minh"}
                </span>
                <div
                  className="ad-actions"
                  onClick={(e) => e.stopPropagation()}
                >
                  <button
                    type="button"
                    onClick={() => onEdit(t.targetType, t.id)}
                  >
                    <Pencil size={12} /> {n ? "Sửa" : "Thiết lập"}
                  </button>
                  {n && (
                    <button
                      type="button"
                      className="danger"
                      onClick={() => onDelete(n, t)}
                    >
                      <Trash2 size={12} /> Gỡ thuyết minh
                    </button>
                  )}
                </div>
              </div>
              {open === key && (
                <div className="ad-detail">
                  {n?.image || t.img ? (
                    <img src={n?.image || t.img} alt={t.name} />
                  ) : (
                    <div className="ad-noimg">Chưa có hình ảnh</div>
                  )}
                  <dl>
                    <div>
                      <dt>Đối tượng</dt>
                      <dd>
                        {t.name} · {t.label}
                      </dd>
                    </div>
                    {n ? (
                      <>
                        <div>
                          <dt>Cách thuyết minh</dt>
                          <dd>
                            {n.ttsEnabled && (
                              <span className="ad-tag">
                                TTS · {langName(n.language)} · {n.speed}x
                              </span>
                            )}
                            {n.audioEnabled && (
                              <span className="ad-tag">
                                Audio: {n.audioName || "đã tải lên"}
                              </span>
                            )}
                          </dd>
                        </div>
                        <div>
                          <dt>Phương thức kích hoạt</dt>
                          <dd>
                            {n.gps?.enabled && (
                              <span className="ad-tag">
                                GPS · {n.gps.radius} m
                              </span>
                            )}
                            {n.bluetooth?.enabled && (
                              <span className="ad-tag">
                                Bluetooth · {n.bluetooth.deviceName}
                              </span>
                            )}
                            {n.qr?.enabled && (
                              <span className="ad-tag">QR · {n.qr.code}</span>
                            )}
                            {!n.gps?.enabled &&
                              !n.bluetooth?.enabled &&
                              !n.qr?.enabled &&
                              "Chưa bật phương thức nào"}
                          </dd>
                        </div>
                        <div>
                          <dt>Lời thuyết minh</dt>
                          <dd>
                            {n.useDescription ? t.description : n.text || "—"}
                          </dd>
                        </div>
                      </>
                    ) : (
                      <div>
                        <dd>
                          Đối tượng này chưa có thuyết minh. Bấm “Thiết lập” để
                          thêm.
                        </dd>
                      </div>
                    )}
                  </dl>
                </div>
              )}
            </div>
          );
        })}
        {rows.length === 0 && (
          <p className="ad-empty">Không có đối tượng phù hợp.</p>
        )}
      </div>
    </div>
  );
}

function NarrationPage({
  sites,
  artifacts,
  narrations,
  languages,
  onSave,
  onDelete,
}) {
  const [tab, setTab] = useState("edit");
  const [target, setTarget] = useState(null);
  const openEditor = (type, id) => {
    setTarget({ type, id });
    setTab("edit");
  };

  return (
    <div>
      <div className="admin-page-heading">
        <div>
          <h1>Quản lý nội dung thuyết minh</h1>
          <p>
            Cài đặt lời thuyết minh, audio và các phương thức kích hoạt cho từng
            địa điểm / hiện vật.
          </p>
        </div>
      </div>
      <div className="ad-tabs">
        <button
          type="button"
          className={tab === "edit" ? "on" : ""}
          onClick={() => {
            setTarget(null);
            setTab("edit");
          }}
        >
          Thiết lập thuyết minh
        </button>
        <button
          type="button"
          className={tab === "list" ? "on" : ""}
          onClick={() => setTab("list")}
        >
          Danh sách thuyết minh
        </button>
      </div>
      {tab === "edit" ? (
        <NarrationEditor
          key={target ? `${target.type}-${target.id}` : "new"}
          sites={sites}
          artifacts={artifacts}
          narrations={narrations}
          languages={languages}
          initial={target}
          onCancel={() => setTab("list")}
          onSave={async (payload) => {
            const ok = await onSave(payload);
            if (ok) setTab("list");
          }}
        />
      ) : (
        <NarrationList
          sites={sites}
          artifacts={artifacts}
          narrations={narrations}
          languages={languages}
          onEdit={openEditor}
          onDelete={onDelete}
        />
      )}
    </div>
  );
}

/* =====================================================================
   QUẢN LÝ NGÔN NGỮ
   ===================================================================== */
function LanguagePage({ languages, onCreate, onUpdate, onDelete }) {
  const empty = { name: "", code: "", ttsKey: "", enabled: true };
  const [form, setForm] = useState(empty);
  const [editId, setEditId] = useState(null);
  const [error, setError] = useState("");

  const submit = async () => {
    const payload = {
      name: form.name.trim(),
      code: form.code.trim(),
      ttsKey: form.ttsKey.trim(),
      enabled: form.enabled,
    };
    if (!payload.name || !payload.code || !payload.ttsKey)
      return setError("Vui lòng nhập đủ tên, mã ngôn ngữ và key TTS.");
    if (languages.some((l) => l.code === payload.code && !same(l.id, editId)))
      return setError("Mã ngôn ngữ này đã tồn tại.");
    setError("");
    const ok = editId
      ? await onUpdate(editId, payload)
      : await onCreate(payload);
    if (ok) {
      setForm(empty);
      setEditId(null);
    }
  };

  const startEdit = (l) => {
    setEditId(l.id);
    setForm({
      name: l.name,
      code: l.code,
      ttsKey: l.ttsKey,
      enabled: l.enabled !== false,
    });
    setError("");
  };

  return (
    <div>
      <div className="admin-page-heading">
        <div>
          <h1>Quản lý ngôn ngữ</h1>
          <p>
            Thêm, sửa, xóa ngôn ngữ. Key TTS dùng để lấy giọng đọc và phát audio
            thuyết minh.
          </p>
        </div>
      </div>

      <div className="ad-panel">
        <h3>{editId ? "Sửa ngôn ngữ" : "Thêm ngôn ngữ"}</h3>
        <p>
          Mã ngôn ngữ dùng cho Google Dịch (vd: en, ja, zh-CN). Key TTS theo
          chuẩn BCP-47 (vd: en-US, ja-JP).
        </p>
        <div className="ad-lang-form">
          <label>
            Tên ngôn ngữ
            <input
              className="ad-input"
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              placeholder="Tiếng Anh"
            />
          </label>
          <label>
            Mã ngôn ngữ
            <input
              className="ad-input"
              value={form.code}
              onChange={(e) => setForm({ ...form, code: e.target.value })}
              placeholder="en"
            />
          </label>
          <label>
            Key TTS
            <input
              className="ad-input"
              value={form.ttsKey}
              onChange={(e) => setForm({ ...form, ttsKey: e.target.value })}
              placeholder="en-US"
            />
          </label>
          <Toggle
            checked={form.enabled}
            onChange={(v) => setForm({ ...form, enabled: v })}
          >
            Bật
          </Toggle>
          <div className="ad-actions">
            <button type="button" className="ad-pub" onClick={submit}>
              {editId ? <Save size={12} /> : <Plus size={12} />}{" "}
              {editId ? "Lưu" : "Thêm"}
            </button>
            {editId && (
              <button
                type="button"
                onClick={() => {
                  setEditId(null);
                  setForm(empty);
                  setError("");
                }}
              >
                Hủy
              </button>
            )}
          </div>
        </div>
        {error && (
          <div className="editor-error" role="alert" style={{ marginTop: 12 }}>
            {error}
          </div>
        )}
      </div>

      <div className="ad-panel" style={{ overflowX: "auto" }}>
        <table className="ad-table">
          <thead>
            <tr>
              <th>NGÔN NGỮ</th>
              <th>MÃ</th>
              <th>KEY TTS</th>
              <th>TRẠNG THÁI</th>
              <th>THAO TÁC</th>
            </tr>
          </thead>
          <tbody>
            {languages.map((l) => (
              <tr key={l.id}>
                <td>
                  <b>{l.name}</b>
                </td>
                <td>{l.code}</td>
                <td>{l.ttsKey}</td>
                <td>
                  <span
                    className={
                      "admin-status " +
                      (l.enabled !== false ? "published" : "draft")
                    }
                  >
                    {l.enabled !== false ? "Đang bật" : "Đang tắt"}
                  </span>
                </td>
                <td>
                  <div className="ad-actions">
                    <button type="button" onClick={() => startEdit(l)}>
                      <Pencil size={12} /> Sửa
                    </button>
                    <button
                      type="button"
                      className="danger"
                      onClick={() => onDelete(l)}
                    >
                      <Trash2 size={12} /> Xóa
                    </button>
                  </div>
                </td>
              </tr>
            ))}
            {languages.length === 0 && (
              <tr>
                <td colSpan="5" className="ad-empty">
                  Chưa có ngôn ngữ nào.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

/* =====================================================================
   KHUNG QUẢN TRỊ CHÍNH
   ===================================================================== */
function AdminDashboard({ onLogout }) {
  const [activeMenu, setActiveMenu] = useState("Tổng quan");
  const [placesTab, setPlacesTab] = useState("add");
  const [notice, setNotice] = useState("");
  const [showQr, setShowQr] = useState(false);
  const [editing, setEditing] = useState(null);
  const live = useLiveDevices();

  const [data, setData] = useState({
    sites: [],
    artifacts: [],
    narrations: [],
    languages: [],
    listens: [],
  });
  const [loading, setLoading] = useState(true);
  const [apiError, setApiError] = useState("");

  const reload = useCallback(async () => {
    try {
      const [sites, artifacts, narrations, languages, listens] =
        await Promise.all([
          api.getSites(),
          api.getArtifacts(),
          api.getNarrations(),
          api.getLanguages(),
          api.getListens(),
        ]);
      setData({ sites, artifacts, narrations, languages, listens });
      setApiError("");
    } catch (err) {
      setApiError(
        `Không kết nối được máy chủ dữ liệu tại ${api.API_BASE}. Hãy chạy Back-end Java (Spring Boot, cổng 3000).`,
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    reload();
    const refreshTimer = setInterval(reload, 5000);
    return () => clearInterval(refreshTimer);
  }, [reload]);

  const { sites, artifacts, narrations, languages, listens } = data;

  // "Xem ứng dụng": mở trang hành khách (/user) ở tab mới, tự vào bằng mã QR hiện tại.
  // Phải mở tab ngay trong cú nhấp chuột, nếu không trình duyệt sẽ chặn cửa sổ bật lên.
  const openUserApp = async () => {
    const tab = window.open("about:blank", "_blank");
    if (!tab) {
      setNotice("Trình duyệt đã chặn cửa sổ mới. Hãy cho phép cửa sổ bật lên cho trang này rồi thử lại.");
      return;
    }
    try {
      tab.location.href = await api.getUserAppLink();
    } catch (err) {
      tab.close();
      setNotice(`Lỗi: ${err.message}`);
    }
  };

  // Chạy 1 thao tác API -> nạp lại dữ liệu -> báo kết quả. Trả về true/false.
  const run = async (fn, okMsg) => {
    try {
      await fn();
      await reload();
      if (okMsg) setNotice(okMsg);
      return true;
    } catch (err) {
      setNotice(`Lỗi: ${err.message}`);
      return false;
    }
  };

  /* ---------- Địa điểm / hiện vật ---------- */
  const statusOf = (action) => (action === "publish" ? "published" : "draft");

  const handleCreateSite = (payload, action) =>
    run(
      () => api.createSite({ ...payload, status: statusOf(action) }),
      action === "publish"
        ? "Đã xuất bản địa điểm."
        : "Đã lưu bản nháp (ẩn với người dùng).",
    );

  const handleCreateArtifact = async (payload, action) => {
    const ok = await run(
      () => api.createArtifact({ ...payload, status: statusOf(action) }),
      action === "publish"
        ? "Đã xuất bản hiện vật."
        : "Đã lưu bản nháp (ẩn với người dùng).",
    );
    if (ok) {
      setPlacesTab("list");
      setActiveMenu("Địa điểm & bảo tàng");
    }
    return ok;
  };

  const handleUpdate = async (item, payload, action) => {
    const patch = { ...payload };
    if (action === "publish") {
      patch.status = "published";
      patch.hidden = false;
    }
    const ok = await run(
      () =>
        item.kind === "site"
          ? api.updateSite(item.id, patch)
          : api.updateArtifact(item.id, patch),
      action === "publish" ? "Đã xuất bản." : "Đã lưu thay đổi.",
    );
    if (ok) setEditing(null);
    return ok;
  };

  const patchItem = (item, patch, msg) =>
    run(
      () =>
        item.kind === "site"
          ? api.updateSite(item.id, patch)
          : api.updateArtifact(item.id, patch),
      msg,
    );

  const handleToggleHide = (item) =>
    patchItem(
      item,
      { hidden: !item.hidden },
      item.hidden
        ? `Đã hiện “${item.name}” với người dùng.`
        : `Đã ẩn “${item.name}” khỏi người dùng.`,
    );

  const handlePublish = (item) =>
    patchItem(
      item,
      { status: "published", hidden: false },
      `Đã xuất bản “${item.name}”.`,
    );

  const handleDelete = (item) => {
    const count = artifacts.filter((a) => same(a.siteId, item.id)).length;
    const extra =
      item.kind === "site" && count
        ? `\nCác hiện vật thuộc địa điểm này (${count}) cũng sẽ bị xóa.`
        : "";
    if (
      !window.confirm(
        `Xóa “${item.name}”?${extra}\nHành động này không thể hoàn tác.`,
      )
    )
      return;
    run(
      () =>
        item.kind === "site"
          ? api.deleteSite(item.id)
          : api.deleteArtifact(item.id),
      "Đã xóa.",
    );
  };

  /* ---------- Thuyết minh ---------- */
  const handleSaveNarration = (payload) =>
    run(() => api.saveNarration(payload), "Đã lưu thuyết minh.");
  const handleDeleteNarration = (n, target) => {
    if (!window.confirm(`Gỡ nội dung thuyết minh của “${target.name}”?`))
      return;
    run(() => api.deleteNarration(n.id), "Đã gỡ thuyết minh.");
  };

  /* ---------- Ngôn ngữ ---------- */
  const handleCreateLanguage = (p) =>
    run(() => api.createLanguage(p), "Đã thêm ngôn ngữ.");
  const handleUpdateLanguage = (id, p) =>
    run(() => api.updateLanguage(id, p), "Đã cập nhật ngôn ngữ.");
  const handleDeleteLanguage = (l) => {
    if (!window.confirm(`Xóa ngôn ngữ “${l.name}”?`)) return;
    run(() => api.deleteLanguage(l.id), "Đã xóa ngôn ngữ.");
  };

  /* ---------- Dữ liệu tổng hợp ---------- */
  const siteById = useMemo(
    () => Object.fromEntries(sites.map((s) => [String(s.id), s])),
    [sites],
  );

  const allItems = useMemo(
    () =>
      [
        ...sites.map((s) => ({
          ...s,
          kind: "site",
          kindLabel: s.classify || "Địa điểm",
        })),
        ...artifacts.map((a) => ({
          ...a,
          kind: "artifact",
          kindLabel: "Hiện vật",
          siteName: siteById[String(a.siteId)]?.name || "Chưa gắn địa điểm",
        })),
      ].sort((a, b) => (b.updatedAt || "").localeCompare(a.updatedAt || "")),
    [sites, artifacts, siteById],
  );

  const stats = useMemo(() => {
    const d = new Date();
    const thisMonth = dateKey(d).slice(0, 7);
    const prev = new Date(d.getFullYear(), d.getMonth() - 1, 1);
    const lastMonth = dateKey(prev).slice(0, 7);
    const newIn = (list) =>
      list.filter((x) => (x.createdAt || "").slice(0, 7) === thisMonth).length;
    const sumMonth = (m) =>
      listens
        .filter((l) => (l.date || "").slice(0, 7) === m)
        .reduce((s, l) => s + (Number(l.count) || 0), 0);
    const total = listens.reduce((s, l) => s + (Number(l.count) || 0), 0);
    const cur = sumMonth(thisMonth);
    const last = sumMonth(lastMonth);
    const growth = last
      ? `${cur >= last ? "+" : ""}${(((cur - last) / last) * 100).toFixed(1).replace(".", ",")}%`
      : cur
        ? "Mới"
        : "0%";
    return {
      sites: sites.length,
      artifacts: artifacts.length,
      total,
      newSites: newIn(sites),
      newArtifacts: newIn(artifacts),
      growth,
    };
  }, [sites, artifacts, listens]);

  const menuGroups = [
    {
      title: "KHÔNG GIAN QUẢN TRỊ",
      links: [
        { name: "Tổng quan", icon: LayoutDashboard },
        { name: "Địa điểm & bảo tàng", icon: Building2 },
        { name: "Hiện vật", icon: Landmark },
        { name: "Quản lý nội dung thuyết minh", icon: Radio },
        { name: "Quản lý ngôn ngữ", icon: Languages },
        { name: "Những người đang dùng", icon: Users },
      ],
    },
    {
      title: "HỆ THỐNG",
      links: [
        { name: "Cài đặt", icon: Settings },
        { name: "Trung tâm trợ giúp", icon: Info },
      ],
    },
  ];

  const statCards = [
    {
      title: "Địa điểm & bảo tàng",
      value: stats.sites.toLocaleString("vi-VN"),
      growth: `+${stats.newSites} địa điểm`,
      icon: MapPin,
    },
    {
      title: "Hiện vật",
      value: stats.artifacts.toLocaleString("vi-VN"),
      growth: `+${stats.newArtifacts} hiện vật`,
      icon: Landmark,
    },
    {
      title: "Lượt khám phá",
      value: stats.total.toLocaleString("vi-VN"),
      growth: stats.growth,
      icon: TrendingUp,
    },
    {
      title: "Những người đang dùng",
      value: String(live.activeCount),
      note: live.connected
        ? `${live.activeCount} thiết bị đang mở dịch vụ`
        : "Chưa kết nối máy chủ",
      icon: Users,
      onClick: () => {
        setActiveMenu("Những người đang dùng");
        setNotice("");
      },
    },
  ];

  const goAdd = () => {
    setPlacesTab("add");
    setActiveMenu("Địa điểm & bảo tàng");
  };

  const monthLabel = `Tháng ${new Date().getMonth() + 1}, ${new Date().getFullYear()}`;

  const listProps = {
    onToggleHide: handleToggleHide,
    onPublish: handlePublish,
    onEdit: setEditing,
    onDelete: handleDelete,
  };

  const renderContent = () => {
    if (activeMenu === "Hiện vật")
      return (
        <ArtifactForm
          key="artifact-new"
          sites={sites}
          onCancel={() => setActiveMenu("Tổng quan")}
          onSave={handleCreateArtifact}
        />
      );
    if (activeMenu === "Địa điểm & bảo tàng")
      return (
        <PlacesPage
          tab={placesTab}
          setTab={setPlacesTab}
          sites={sites}
          artifacts={artifacts}
          listens={listens}
          onCreateSite={handleCreateSite}
          onEdit={setEditing}
          onDelete={handleDelete}
        />
      );
    if (activeMenu === "Quản lý nội dung thuyết minh")
      return (
        <NarrationPage
          sites={sites}
          artifacts={artifacts}
          narrations={narrations}
          languages={languages}
          onSave={handleSaveNarration}
          onDelete={handleDeleteNarration}
        />
      );
    if (activeMenu === "Quản lý ngôn ngữ")
      return (
        <LanguagePage
          languages={languages}
          onCreate={handleCreateLanguage}
          onUpdate={handleUpdateLanguage}
          onDelete={handleDeleteLanguage}
        />
      );
    if (activeMenu === "Những người đang dùng")
      return <ActiveUsersPage live={live} onOpenQr={() => setShowQr(true)} />;

    if (activeMenu === "Tất cả nội dung")
      return (
        <div>
          <div className="editor-back">
            <button type="button" onClick={() => setActiveMenu("Tổng quan")}>
              <ArrowLeft size={14} /> Quay lại tổng quan
            </button>
          </div>
          <section className="ad-panel">
            <h3>Tất cả địa điểm &amp; hiện vật ({allItems.length})</h3>
            <p>
              Bấm vào một dòng để xem chi tiết. Ẩn / Hiện, Sửa, Xóa ngay trên
              danh sách.
            </p>
            <ContentList items={allItems} {...listProps} />
          </section>
        </div>
      );

    // ===== TỔNG QUAN =====
    return (
      <>
        <div className="admin-page-heading">
          <div>
            <h1>{activeMenu}</h1>
            <p>Chào Minh Anh, cùng nhìn lại hoạt động khám phá hôm nay.</p>
          </div>
          <div className="admin-heading-actions">
            <button className="admin-date" onClick={() => setShowQr(true)}>
              <QrCode size={14} />
              Mở QR đăng nhập
            </button>
            <button
              className="admin-date"
              onClick={() =>
                setNotice(`Dữ liệu thống kê ${monthLabel.toLowerCase()}.`)
              }
            >
              <CalendarDays size={14} />
              {monthLabel}
            </button>
            <button className="admin-primary" onClick={goAdd}>
              <Plus size={15} />
              Thêm nội dung
            </button>
          </div>
        </div>

        <div className="admin-stats">
          {statCards.map((stat) => {
            const Icon = stat.icon;
            return (
              <article
                className={
                  stat.onClick
                    ? "admin-stat-card live-clickable"
                    : "admin-stat-card"
                }
                key={stat.title}
                onClick={stat.onClick}
              >
                <div className="admin-stat-title">
                  {stat.title}
                  <Icon size={15} />
                </div>
                <strong>{stat.value}</strong>
                {stat.note ? (
                  <p>{stat.note}</p>
                ) : (
                  <p>
                    <b>{stat.growth}</b>{" "}
                    {stat.title === "Lượt khám phá"
                      ? "so với tháng trước"
                      : "trong tháng này"}
                  </p>
                )}
              </article>
            );
          })}
        </div>

        <section className="admin-panel admin-chart-panel">
          <div className="admin-panel-heading">
            <div>
              <b>Lượt khám phá</b>
              <p>
                Tổng lượt nghe audio thuyết minh theo ngày · 14 ngày gần nhất
              </p>
            </div>
            <span>
              ● Cột &amp; đường: {stats.total.toLocaleString("vi-VN")} lượt
            </span>
          </div>
          <ListenChart listens={listens} days={14} />
        </section>

        <section
          className="admin-panel admin-table-panel"
          style={{ padding: 16 }}
        >
          <div className="admin-table-heading">
            <b>
              Quản lý địa điểm &amp; hiện vật
              <span className="admin-count">{allItems.length} nội dung</span>
            </b>
            <button
              type="button"
              onClick={() => setActiveMenu("Tất cả nội dung")}
            >
              Xem tất cả →
            </button>
          </div>
          <ContentList items={allItems} limit={5} {...listProps} />
        </section>

        <footer className="admin-footer">
          <span>● Dữ liệu từ {api.API_BASE}</span>
          <span>© 2026 · Khám phá Việt Nam</span>
        </footer>
      </>
    );
  };

  return (
    <main className="admin-layout">
      <style>{ADMIN_CSS}</style>

      <aside className="admin-sidebar">
        <div className="admin-brand">
          <Building2 size={27} />
          <div>
            <b>Khám phá Việt Nam</b>
            <small>CỔNG QUẢN TRỊ</small>
          </div>
        </div>

        <nav className="admin-navigation">
          {menuGroups.map((group) => (
            <div className="admin-menu-group" key={group.title}>
              <p>{group.title}</p>
              {group.links.map(({ name, icon: Icon }) => (
                <button
                  key={name}
                  type="button"
                  className={
                    activeMenu === name ? "admin-menu active" : "admin-menu"
                  }
                  onClick={() => {
                    if (name === "Địa điểm & bảo tàng") setPlacesTab("add");
                    setActiveMenu(name);
                    setNotice("");
                  }}
                >
                  <Icon size={15} />
                  <span>{name}</span>
                  {name === "Địa điểm & bảo tàng" && (
                    <small>{sites.length}</small>
                  )}
                  {name === "Hiện vật" && <small>{artifacts.length}</small>}
                  {name === "Những người đang dùng" && (
                    <small>{live.activeCount}</small>
                  )}
                </button>
              ))}
            </div>
          ))}
        </nav>

        <div className="admin-promo">
          <Compass size={18} />
          <b>Trải nghiệm khám phá</b>
          <p>Xem nội dung đang hiển thị trên ứng dụng.</p>
          <button
            type="button"
            onClick={openUserApp}
          >
            Mở ứng dụng ↗
          </button>
        </div>

        <button className="admin-logout" onClick={onLogout}>
          <LogOut size={15} />
          Đăng xuất
        </button>
      </aside>

      <section className="admin-main">
        <header className="admin-topbar">
          <div className="admin-breadcrumb">
            Quản trị <span>›</span> <b>{activeMenu}</b>
          </div>
          <div className="admin-user">
            <button
              type="button"
              onClick={openUserApp}
            >
              Xem ứng dụng ↗
            </button>
            <Bell size={16} />
            <span className="admin-avatar">MA</span>
            <div>
              <b>Minh Anh</b>
              <small>Quản trị viên</small>
            </div>
            <ChevronDown size={13} />
          </div>
        </header>

        <div className="admin-content">
          {apiError && <div className="ad-api-error">{apiError}</div>}
          {loading ? (
            <p className="ad-empty">Đang tải dữ liệu...</p>
          ) : (
            renderContent()
          )}
        </div>
      </section>

      {editing && (
        <div className="ad-modal-bg">
          <div className="ad-modal-box">
            <div className="ad-modal-head">
              <h2>
                Sửa{" "}
                {editing.kind === "site" ? "địa điểm / bảo tàng" : "hiện vật"}
              </h2>
              <button
                type="button"
                onClick={() => setEditing(null)}
                aria-label="Đóng"
              >
                <X size={18} />
              </button>
            </div>
            {editing.kind === "site" ? (
              <SiteForm
                mode="edit"
                initial={editing}
                onCancel={() => setEditing(null)}
                onSave={(payload, action) =>
                  handleUpdate(editing, payload, action)
                }
              />
            ) : (
              <ArtifactForm
                mode="edit"
                sites={sites}
                initial={editing}
                onCancel={() => setEditing(null)}
                onSave={(payload, action) =>
                  handleUpdate(editing, payload, action)
                }
              />
            )}
          </div>
        </div>
      )}

      {showQr && <QrLoginModal live={live} onClose={() => setShowQr(false)} />}

      {notice && (
        <div className="admin-toast" role="status">
          <Check size={15} />
          <span>{notice}</span>
          <button onClick={() => setNotice("")}>
            <X size={14} />
          </button>
        </div>
      )}
    </main>
  );
}
