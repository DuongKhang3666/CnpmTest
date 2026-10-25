import { useState, useEffect, useRef, useMemo, useCallback } from "react";
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
  SlidersHorizontal,
  ChevronLeft,
  ChevronRight,
  Compass,
  ChevronDown,
  MoreHorizontal,
  Pencil,
  MapPinned,
  UploadCloud,
  Trash2,
  Save,
  ImagePlus,
  ArrowLeft,
} from "lucide-react";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import "./App.css";
import { useAuth } from "./AuthContext.jsx";
import { placeApi, artifactApi, mediaApi, dashboardApi } from "./api.js";

// Chuyển ảnh dạng data URL (FileReader) thành File để upload lên Backend
const dataUrlToFile = (dataUrl, fileName = "image.png") => {
  const parts = dataUrl.split(",");
  const mime = parts[0].match(/:(.*?);/)?.[1] || "image/png";
  const binary = atob(parts[1]);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
  return new File([bytes], fileName, { type: mime });
};

export default function App() {
  const { user, login, logout, isAuthenticated, loading } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [remember, setRemember] = useState(false);
  const [message, setMessage] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const handleLogin = async (e) => {
    e.preventDefault();

    if (!email.trim() || !password) {
      setMessage("Vui lòng nhập email và mật khẩu.");
      return;
    }

    setSubmitting(true);
    try {
      await login(email.trim(), password);
      setMessage("");
    } catch (err) {
      const apiMessage = err?.response?.data?.message;
      if (err?.response?.status === 401) {
        setMessage(apiMessage || "Email hoặc mật khẩu không chính xác.");
      } else if (err?.code === "ECONNABORTED" || !err?.response) {
        setMessage("Không thể kết nối đến máy chủ. Vui lòng thử lại sau.");
      } else {
        setMessage(apiMessage || "Đăng nhập không thành công, vui lòng thử lại.");
      }
    } finally {
      setSubmitting(false);
    }
  };

  const handleLogout = () => {
    logout();
    setEmail("");
    setPassword("");
    setMessage("");
  };

  if (loading) {
    return null;
  }

  if (isAuthenticated) {
    return <AdminDashboard onLogout={handleLogout} user={user} />;
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

            <button type="submit" className="submit-button" disabled={submitting}>
              {submitting ? "Đang đăng nhập..." : "Đăng nhập"}
              {!submitting && <ArrowRight size={15} />}
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

function AdminDashboard({ onLogout, user }) {
  const [activeMenu, setActiveMenu] = useState("Tổng quan");
  const [search, setSearch] = useState("");
  const [filterType, setFilterType] = useState("Tất cả loại");
  const [filterStatus, setFilterStatus] = useState("Tất cả trạng thái");
  const [page, setPage] = useState(1);
  const [notice, setNotice] = useState("");
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [statsData, setStatsData] = useState(null);
  const [saving, setSaving] = useState(false);
  const [editingItem, setEditingItem] = useState(null);

  const notify = (text) => {
    setNotice(text);
  };

  const handleError = (err, fallback) => {
    const apiMessage = err?.response?.data?.message;
    setNotice(`Lỗi: ${apiMessage || fallback}`);
  };

  const loadContent = useCallback(async () => {
    try {
      const [placeRes, artifactRes] = await Promise.all([
        placeApi.list({ page: 1, size: 500 }),
        artifactApi.list({ page: 1, size: 500 }),
      ]);

      const places = (placeRes.data.items || []).map((item) => ({
        dbId: item.id,
        id: item.code || `#${item.id}`,
        kind: "place",
        name: item.name,
        type: item.type,
        category: item.category,
        location: item.location,
        status: item.status,
        date: item.date,
        image: item.image || "",
        description: item.description,
        latitude: item.latitude,
        longitude: item.longitude,
        extraImages: item.extraImages || [],
      }));

      const artifacts = (artifactRes.data.items || []).map((item) => ({
        dbId: item.id,
        id: item.code || `#${item.id}`,
        kind: "artifact",
        name: item.name,
        type: "Hiện vật",
        placeId: item.placeId,
        placeName: item.placeName,
        location: item.location,
        status: item.status,
        date: item.date,
        image: item.image || "",
        description: item.description,
        latitude: item.latitude,
        longitude: item.longitude,
      }));

      setItems([...places, ...artifacts]);
      setLoadError("");
    } catch (err) {
      setLoadError(
        err?.response
          ? `Không thể tải dữ liệu từ máy chủ (${err.response.status}).`
          : "Không thể kết nối đến máy chủ. Vui lòng kiểm tra Backend đã chạy.",
      );
    } finally {
      setLoading(false);
    }
  }, []);

  const loadStats = useCallback(async () => {
    try {
      const res = await dashboardApi.stats();
      setStatsData(res.data);
    } catch {
      // Giữ thống kê cũ (nếu có) khi lần đầu tải lỗi
    }
  }, []);

  useEffect(() => {
    // Tải dữ liệu list + thống kê một lần khi vào dashboard
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadContent();
    loadStats();
  }, [loadContent, loadStats]);

  const openArtifactForm = () => {
    setEditingItem(null);
    setActiveMenu("Hiện vật");
  };
  const openPlaceForm = () => {
    setEditingItem(null);
    setActiveMenu("Địa điểm & bảo tàng");
    setNotice("");
  };

  // Ảnh dạng data URL cần upload; URL http(s) là ảnh đã tải lên trước đó (khi sửa) -> giữ nguyên
  const uploadImageIfNeeded = async (img) => {
    if (!img || !img.startsWith("data:")) return img || "";
    const res = await mediaApi.uploadSingle(dataUrlToFile(img));
    return res.data.url;
  };

  const uploadImagesIfNeeded = async (imgs) => {
    const urlList = [];
    const newFiles = [];
    for (const img of imgs || []) {
      if (img && img.startsWith("data:")) newFiles.push(dataUrlToFile(img));
      else if (img) urlList.push(img);
    }
    if (newFiles.length > 0) {
      const res = await mediaApi.uploadMultiple(newFiles);
      urlList.unshift(...res.data.urls);
    }
    return urlList;
  };

  const handleSavePlace = async (content, action) => {
    if (saving) return;
    setSaving(true);
    try {
      const image = await uploadImageIfNeeded(content.image);
      const extraImages = await uploadImagesIfNeeded(content.extraImages);

      const payload = {
        name: content.name,
        // content.type giữ phân loại chi tiết (Bảo tàng / Địa điểm du lịch / ...)
        type: content.type === "Bảo tàng" ? "Bảo tàng" : "Địa điểm",
        category: content.type,
        description: content.description,
        address: content.location,
        latitude: content.latitude,
        longitude: content.longitude,
        image,
        extraImages,
        status: action === "publish" ? "Đã xuất bản" : "Bản nháp",
      };

      if (editingItem) {
        await placeApi.update(editingItem.dbId, payload);
        setNotice("Đã cập nhật địa điểm / bảo tàng.");
      } else {
        await placeApi.create(payload);
        setNotice(
          action === "publish" ? "Đã xuất bản nội dung." : "Đã lưu bản nháp.",
        );
      }

      setEditingItem(null);
      setActiveMenu("Tổng quan");
      await Promise.all([loadContent(), loadStats()]);
    } catch (err) {
      handleError(err, "Không thể lưu địa điểm / bảo tàng.");
    } finally {
      setSaving(false);
    }
  };

  const handleSaveArtifact = async (content, action) => {
    if (saving) return;
    setSaving(true);
    try {
      const image = await uploadImageIfNeeded(content.image);

      const payload = {
        name: content.name,
        description: content.description,
        placeId: content.placeId ? Number(content.placeId) : null,
        image,
        latitude: content.latitude,
        longitude: content.longitude,
        status: action === "publish" ? "Đã xuất bản" : "Bản nháp",
      };

      if (editingItem) {
        await artifactApi.update(editingItem.dbId, payload);
        setNotice("Đã cập nhật hiện vật.");
      } else {
        await artifactApi.create(payload);
        setNotice(
          action === "publish" ? "Đã xuất bản nội dung." : "Đã lưu bản nháp.",
        );
      }

      setEditingItem(null);
      setActiveMenu("Tổng quan");
      await Promise.all([loadContent(), loadStats()]);
    } catch (err) {
      handleError(err, "Không thể lưu hiện vật.");
    } finally {
      setSaving(false);
    }
  };

  const openEditItem = (item) => {
    setEditingItem(item);
    setActiveMenu(item.kind === "place" ? "Địa điểm & bảo tàng" : "Hiện vật");
  };

  const handleDeleteItem = async (item) => {
    const confirmText =
      item.kind === "place"
        ? `Xóa "${item.name}"? Tất cả hiện vật thuộc địa điểm này cũng sẽ bị xóa.`
        : `Xóa "${item.name}"? Hành động không thể hoàn tác.`;

    if (!window.confirm(confirmText)) return;

    try {
      if (item.kind === "place") {
        await placeApi.delete(item.dbId);
      } else {
        await artifactApi.delete(item.dbId);
      }
      setNotice(`Đã xóa "${item.name}".`);
      await Promise.all([loadContent(), loadStats()]);
    } catch (err) {
      handleError(err, "Không thể xóa nội dung.");
    }
  };

  const menuGroups = [
    {
      title: "KHÔNG GIAN QUẢN TRỊ",
      links: [
        { name: "Tổng quan", icon: LayoutDashboard },
        { name: "Địa điểm & bảo tàng", icon: Building2 },
        { name: "Hiện vật", icon: Landmark },
        { name: "Bài viết & thuyết minh", icon: Radio },
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

  const filteredItems = items.filter((item) => {
    const matchSearch =
      item.name.toLowerCase().includes(search.toLowerCase()) ||
      item.id.toLowerCase().includes(search.toLowerCase());

    const matchType = filterType === "Tất cả loại" || item.type === filterType;

    const matchStatus =
      filterStatus === "Tất cả trạng thái" || item.status === filterStatus;

    return matchSearch && matchType && matchStatus;
  });

  const pageSize = 4;
  const totalPages = Math.max(1, Math.ceil(filteredItems.length / pageSize));

  const visibleItems = filteredItems.slice(
    (page - 1) * pageSize,
    page * pageSize,
  );
  const stats = statsData
    ? [
        {
          title: "Địa điểm & bảo tàng",
          value: String(statsData.totalPlaces),
          growth: `${statsData.totalDiaDiem} địa điểm · ${statsData.totalBaoTang} bảo tàng`,
          icon: MapPin,
        },
        {
          title: "Hiện vật",
          value: String(statsData.totalArtifacts),
          growth: `${statsData.publishedCount} nội dung đã xuất bản`,
          icon: Landmark,
        },
        {
          title: "Chờ duyệt",
          value: String(statsData.pendingCount),
          growth: `${statsData.draftCount} bản nháp đang chờ xử lý`,
          icon: TrendingUp,
        },
        {
          title: "Người dùng",
          value: String(statsData.totalAdmins),
          growth: "Tài khoản quản trị hệ thống",
          icon: Users,
        },
      ]
    : [];

  const displayName = user?.email?.split("@")[0] || "Quản trị viên";
  const avatarInitials = displayName.slice(0, 2).toUpperCase();

  // Danh sách địa điểm cho dropdown chọn nơi trưng bày hiện vật
  const publishedPlaces = items
    .filter(
      (item) =>
        item.kind === "place" &&
        item.type !== "Hiện vật" &&
        item.status !== "Bản nháp",
    )
    .map((place) => ({
      id: place.dbId,
      name: place.name,
      location: place.location,
      latitude: place.latitude,
      longitude: place.longitude,
      image: place.image,
    }));

  // Dữ liệu biểu đồ: số nội dung tạo mới theo ngày (7 ngày, lấy từ Backend)
  const chartData = useMemo(() => {
    const daily = statsData?.dailyCreatedLast7Days || {};
    const entries = [];
    for (let i = 6; i >= 0; i--) {
      const day = new Date();
      day.setDate(day.getDate() - i);
      const key = `${day.getFullYear()}-${String(day.getMonth() + 1).padStart(2, "0")}-${String(day.getDate()).padStart(2, "0")}`;
      entries.push({
        key,
        label: `${String(day.getDate()).padStart(2, "0")}/${String(day.getMonth() + 1).padStart(2, "0")}`,
        value: daily[key] || 0,
      });
    }
    const maxValue = Math.max(1, ...entries.map((entry) => entry.value));
    return { entries, maxValue };
  }, [statsData]);

  return (
    <main className="admin-layout">
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
                    setActiveMenu(name);
                    setEditingItem(null);
                    setNotice(
                      name === "Tổng quan" ? "" : `Bạn đã chọn mục ${name}.`,
                    );
                  }}
                >
                  <Icon size={15} />
                  <span>{name}</span>

                  {name === "Hiện vật" && loading ? null : name === "Hiện vật" && (
                    <small>
                      {items.filter((item) => item.kind === "artifact").length}
                    </small>
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
            onClick={() => notify("Chưa cấu hình liên kết ứng dụng.")}
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
              onClick={() => notify("Chưa cấu hình liên kết ứng dụng.")}
            >
              Xem ứng dụng ↗
            </button>

            <Bell size={16} />

            <span className="admin-avatar">
              {avatarInitials}
            </span>

            <div>
              <b>{user?.email || displayName}</b>
              <small>{user?.role || "Quản trị viên"}</small>
            </div>

            <ChevronDown size={13} />
          </div>
        </header>

        <div className="admin-content">
          {activeMenu === "Hiện vật" ? (
            <ArtifactForm
              places={publishedPlaces}
              onCancel={() => setActiveMenu("Tổng quan")}
              onSwitchToPlace={openPlaceForm}
              onSave={handleSaveArtifact}
              busy={saving}
              initial={editingItem?.kind === "artifact" ? editingItem : null}
            />
          ) : activeMenu === "Địa điểm & bảo tàng" ? (
            <AdminContentForm
              onCancel={() => setActiveMenu("Tổng quan")}
              onSave={handleSavePlace}
              onCreateArtifact={openArtifactForm}
              busy={saving}
              initial={editingItem?.kind === "place" ? editingItem : null}
            />
          ) : (
            <>
              <div className="admin-page-heading">
                <div>
                  <h1>{activeMenu}</h1>
                  <p>
                    Chào {displayName}, cùng nhìn lại hoạt động khám phá hôm
                    nay.
                  </p>
                </div>

                <div className="admin-heading-actions">
                  <button
                    className="admin-date"
                    onClick={() => notify("Dữ liệu thống kê tháng 10/2026.")}
                  >
                    <CalendarDays size={14} />
                    Tháng 10, 2026
                  </button>

                  <button
                    className="admin-primary"
                    onClick={openPlaceForm}
                  >
                    <Plus size={15} />
                    Thêm nội dung
                  </button>
                </div>
              </div>

              <div className="admin-stats">
                {!statsData && (
                  <article className="admin-stat-card">
                    <div className="admin-stat-title">Đang tải thống kê…</div>
                  </article>
                )}
                {stats.map((stat) => {
                  const Icon = stat.icon;

                  return (
                    <article className="admin-stat-card" key={stat.title}>
                      <div className="admin-stat-title">
                        {stat.title}
                        <Icon size={15} />
                      </div>

                      <strong>{stat.value}</strong>

                      <p>
                        <b>{stat.growth}</b>
                      </p>
                    </article>
                  );
                })}
              </div>

              <section className="admin-panel admin-chart-panel">
                <div className="admin-panel-heading">
                  <div>
                    <b>Nội dung tạo mới</b>
                    <p>Số địa điểm / hiện vật được thêm theo ngày (7 ngày gần nhất)</p>
                  </div>

                  <span>● 7 ngày gần nhất</span>
                </div>

                <div className="admin-chart">
                  <div
                    style={{
                      display: "flex",
                      alignItems: "flex-end",
                      gap: 10,
                      height: 150,
                      padding: "12px 4px 4px",
                      minHeight: 0,
                    }}
                  >
                    {chartData.entries.map((entry) => (
                      <div
                        key={entry.key}
                        style={{
                          flex: 1,
                          display: "flex",
                          flexDirection: "column",
                          alignItems: "center",
                          gap: 6,
                          height: "100%",
                          justifyContent: "flex-end",
                        }}
                      >
                        <small
                          style={{
                            fontSize: 11,
                            color: "#5a6474",
                            visibility: entry.value > 0 ? "visible" : "hidden",
                          }}
                        >
                          {entry.value}
                        </small>

                        <div
                          title={`${entry.label}: ${entry.value} nội dung`}
                          style={{
                            width: "70%",
                            maxWidth: 36,
                            height: `${Math.max(
                              entry.value > 0 ? 6 : 0,
                              (entry.value / chartData.maxValue) * 100,
                            )}%`,
                            background: "#4780ff",
                            borderRadius: 4,
                          }}
                        />

                        <span style={{ fontSize: 11, color: "#5a6474" }}>
                          {entry.label}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              </section>

              <section className="admin-panel admin-table-panel">
                <div className="admin-table-heading">
                  <b>
                    Quản lý địa điểm &amp; hiện vật
                    <span className="admin-count">{items.length} nội dung</span>
                  </b>

                  <button
                    type="button"
                    onClick={() => {
                      setEditingItem(null);
                      setActiveMenu("Địa điểm & bảo tàng");
                      setNotice("");
                    }}
                  >
                    Xem tất cả →
                  </button>
                </div>

                <div className="admin-filters">
                  <div className="admin-search">
                    <Search size={14} />
                    <input
                      placeholder="Tìm tên địa điểm, hiện vật hoặc mã nội dung..."
                      value={search}
                      onChange={(e) => {
                        setSearch(e.target.value);
                        setPage(1);
                      }}
                    />
                  </div>

                  <select
                    value={filterType}
                    onChange={(e) => {
                      setFilterType(e.target.value);
                      setPage(1);
                    }}
                  >
                    <option>Tất cả loại</option>
                    <option>Địa điểm</option>
                    <option>Bảo tàng</option>
                    <option>Hiện vật</option>
                  </select>

                  <select
                    value={filterStatus}
                    onChange={(e) => {
                      setFilterStatus(e.target.value);
                      setPage(1);
                    }}
                  >
                    <option>Tất cả trạng thái</option>
                    <option>Đã xuất bản</option>
                    <option>Chờ duyệt</option>
                    <option>Bản nháp</option>
                  </select>

                  <button
                    className="admin-filter-reset"
                    onClick={() => {
                      setSearch("");
                      setFilterType("Tất cả loại");
                      setFilterStatus("Tất cả trạng thái");
                      setPage(1);
                    }}
                  >
                    <SlidersHorizontal size={13} />
                    Bộ lọc
                  </button>
                </div>

                <div className="admin-table-scroll">
                  <table className="admin-table">
                    <thead>
                      <tr>
                        <th></th>
                        <th>TÊN NỘI DUNG</th>
                        <th>PHÂN LOẠI</th>
                        <th>ĐỊA ĐIỂM / BẢO TÀNG</th>
                        <th>TRẠNG THÁI</th>
                        <th>CẬP NHẬT</th>
                        <th>THAO TÁC</th>
                      </tr>
                    </thead>

                    <tbody>
                      {visibleItems.map((item) => (
                        <tr key={item.id}>
                          <td>
                            <input type="checkbox" />
                          </td>

                          <td>
                            <div className="admin-item-info">
                              {item.image ? (
                                <img src={item.image} alt="" />
                              ) : (
                                <div className="admin-item-placeholder">
                                  <Building2 size={16} />
                                </div>
                              )}

                              <div>
                                <b>{item.name}</b>
                                <small>{item.id}</small>
                              </div>
                            </div>
                          </td>

                          <td>{item.type}</td>
                          <td>{item.location}</td>

                          <td>
                            <span
                              className={
                                "admin-status " +
                                (item.status === "Đã xuất bản"
                                  ? "published"
                                  : item.status === "Chờ duyệt"
                                    ? "pending"
                                    : "draft")
                              }
                            >
                              {item.status}
                            </span>
                          </td>

                          <td>{item.date}</td>

                          <td>
                            <div className="admin-row-actions">
                              <button
                                title="Xem chi tiết"
                                onClick={() =>
                                  notify(
                                    `${item.name} (${item.id}) · ${item.status}` +
                                      (item.description
                                        ? ` — ${item.description.slice(0, 80)}${item.description.length > 80 ? "…" : ""}`
                                        : ""),
                                  )
                                }
                              >
                                <Eye size={14} />
                              </button>

                              {item.kind === "place" && (
                                <button
                                  title="Xem hiện vật của địa điểm"
                                  onClick={() => {
                                    setSearch(item.name);
                                    setFilterType("Tất cả loại");
                                    setFilterStatus("Tất cả trạng thái");
                                    setPage(1);
                                  }}
                                >
                                  <MoreHorizontal size={15} />
                                </button>
                              )}

                              <button title="Sửa" onClick={() => openEditItem(item)}>
                                <Pencil size={14} />
                              </button>

                              <button
                                title="Xóa"
                                onClick={() => handleDeleteItem(item)}
                              >
                                <Trash2 size={14} />
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))}

                      {loading && (
                        <tr>
                          <td colSpan="7" className="admin-empty">
                            Đang tải dữ liệu...
                          </td>
                        </tr>
                      )}

                      {loadError && !loading && (
                        <tr>
                          <td colSpan="7" className="admin-empty">
                            {loadError}
                          </td>
                        </tr>
                      )}

                      {!loading && !loadError && visibleItems.length === 0 && (
                        <tr>
                          <td colSpan="7" className="admin-empty">
                            Không tìm thấy nội dung phù hợp.
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>

                <div className="admin-table-bottom">
                  <span>
                    Hiển thị{" "}
                    {filteredItems.length === 0 ? 0 : (page - 1) * pageSize + 1}
                    –{Math.min(page * pageSize, filteredItems.length)} trong{" "}
                    {filteredItems.length} nội dung
                  </span>

                  <div>
                    <button
                      disabled={page === 1}
                      onClick={() => setPage((p) => p - 1)}
                    >
                      <ChevronLeft size={14} />
                    </button>

                    <span>
                      {page}/{totalPages}
                    </span>

                    <button
                      disabled={page >= totalPages}
                      onClick={() => setPage((p) => p + 1)}
                    >
                      <ChevronRight size={14} />
                    </button>
                  </div>
                </div>
              </section>

              <footer className="admin-footer">
                <span>● Dữ liệu trực tiếp từ máy chủ · Khám phá Việt Nam</span>
                <span>© 2026 · Khám phá Việt Nam</span>
              </footer>
            </>
          )}
        </div>
      </section>

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

/* ===== Bản đồ thật (Leaflet + OpenStreetMap) =====
   - Có tọa độ hợp lệ  -> ghim đúng vị trí và tự zoom tới đó.
   - Truyền onPick     -> bấm lên bản đồ hoặc kéo ghim sẽ cập nhật tọa độ.
   - Không truyền onPick -> chỉ xem (dùng cho form Hiện vật). */
const PIN_HTML =
  '<svg width="32" height="42" viewBox="0 0 32 42" xmlns="http://www.w3.org/2000/svg">' +
  '<path d="M16 0C7.2 0 0 7 0 15.7 0 27.5 16 42 16 42s16-14.5 16-26.3C32 7 24.8 0 16 0z" fill="#d93025" stroke="#fff" stroke-width="2"/>' +
  '<circle cx="16" cy="15.5" r="5.5" fill="#fff"/></svg>';

const parseCoord = (value, min, max) => {
  if (value === "" || value === null || value === undefined) return null;
  const n = Number(value);
  return Number.isFinite(n) && n >= min && n <= max ? n : null;
};

function RealMap({ latitude, longitude, onPick, emptyText, height = 280 }) {
  const containerRef = useRef(null);
  const mapRef = useRef(null);
  const markerRef = useRef(null);
  const onPickRef = useRef(onPick);

  useEffect(() => {
    onPickRef.current = onPick;
  }, [onPick]);

  const lat = parseCoord(latitude, -90, 90);
  const lng = parseCoord(longitude, -180, 180);
  const valid = lat !== null && lng !== null;

  // Khởi tạo bản đồ một lần
  useEffect(() => {
    const map = L.map(containerRef.current, {
      center: [16.0, 106.0], // Việt Nam
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

    // Khung có thể đổi kích thước sau khi render -> cập nhật lại ô bản đồ
    const observer = new ResizeObserver(() => map.invalidateSize());
    observer.observe(containerRef.current);

    return () => {
      observer.disconnect();
      map.remove();
      mapRef.current = null;
      markerRef.current = null;
    };
  }, []);

  // Đồng bộ ghim + vị trí với tọa độ đang nhập
  useEffect(() => {
    const map = mapRef.current;
    if (!map) return;

    if (!valid) {
      if (markerRef.current) {
        markerRef.current.remove();
        markerRef.current = null;
      }
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

    map.setView([lat, lng], map.getZoom() < 14 ? 16 : map.getZoom());
  }, [valid, lat, lng]);

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

      {!valid && (
        <div
          style={{
            position: "absolute",
            left: 10,
            bottom: 10,
            zIndex: 500,
            padding: "6px 10px",
            borderRadius: 8,
            background: "rgba(255,255,255,0.95)",
            fontSize: 12,
            boxShadow: "0 1px 4px rgba(0,0,0,0.2)",
          }}
        >
          {emptyText || "Chưa có tọa độ hợp lệ"}
        </div>
      )}

      {valid && (
        <div
          style={{
            position: "absolute",
            left: 10,
            bottom: 10,
            zIndex: 500,
            padding: "6px 10px",
            borderRadius: 8,
            background: "rgba(255,255,255,0.95)",
            fontSize: 12,
            boxShadow: "0 1px 4px rgba(0,0,0,0.2)",
          }}
        >
          {lat.toFixed(6)}, {lng.toFixed(6)}
        </div>
      )}
    </div>
  );
}


function AdminContentForm({ onCancel, onSave, onCreateArtifact, busy, initial }) {
  // Form này chỉ dành cho Địa điểm / Bảo tàng (nút "Hiện vật" chuyển sang ArtifactForm)
  const type = "Địa điểm";
  const [name, setName] = useState(initial?.name || "");
  const [category, setCategory] = useState(initial?.category || "Bảo tàng");
  const [description, setDescription] = useState(initial?.description || "");
  const [address, setAddress] = useState(initial?.location || "");
  const [locationName, setLocationName] = useState("");
  const [latitude, setLatitude] = useState(
    initial?.latitude != null ? String(initial.latitude) : "10.7794",
  );
  const [longitude, setLongitude] = useState(
    initial?.longitude != null ? String(initial.longitude) : "106.6921",
  );
  const [image, setImage] = useState(initial?.image || "");
  const [extraImages, setExtraImages] = useState(
    Array.isArray(initial?.extraImages) ? initial.extraImages : [],
  );
  const [errors, setErrors] = useState("");
  const isEditing = Boolean(initial);

  const readImage = (file, callback) => {
    if (!file || !file.type.startsWith("image/")) {
      setErrors("Vui lòng chọn tệp hình ảnh hợp lệ.");
      return;
    }

    if (file.size > 10 * 1024 * 1024) {
      setErrors("Ảnh không được vượt quá 10 MB.");
      return;
    }

    const reader = new FileReader();

    reader.onload = () => callback(reader.result);
    reader.onerror = () => setErrors("Không thể đọc ảnh đã chọn.");
    reader.readAsDataURL(file);
  };

  const handleMainImage = (e) => {
    const file = e.target.files?.[0];
    if (file) {
      readImage(file, (result) => {
        setImage(result);
        setErrors("");
      });
    }

    e.target.value = "";
  };

  const handleExtraImages = (e) => {
    const files = Array.from(e.target.files || []);
    const remaining = 9 - extraImages.length;

    if (files.length > remaining) {
      setErrors("Bạn chỉ có thể thêm tối đa 9 ảnh bổ sung.");
    }

    files.slice(0, remaining).forEach((file) => {
      readImage(file, (result) => {
        setExtraImages((previous) =>
          previous.length < 9 ? [...previous, result] : previous,
        );
      });
    });

    e.target.value = "";
  };

  const submit = (action) => {
    if (!name.trim() || !address.trim() || !description.trim()) {
      setErrors("Vui lòng nhập tên, địa chỉ và mô tả nội dung.");
      return;
    }

    const lat = Number(latitude);
    const lng = Number(longitude);

    if (
      !Number.isFinite(lat) ||
      lat < -90 ||
      lat > 90 ||
      !Number.isFinite(lng) ||
      lng < -180 ||
      lng > 180
    ) {
      setErrors("Vĩ độ hoặc kinh độ không hợp lệ.");
      return;
    }

    setErrors("");

    onSave(
      {
        name: name.trim(),
        type: type === "Hiện vật" ? "Hiện vật" : category,
        description: description.trim(),
        location: address.trim(),
        latitude: lat,
        longitude: lng,
        image,
        extraImages,
      },
      action,
    );
  };

  return (
    <div className="content-editor">
      <div className="editor-back">
        <button type="button" onClick={onCancel}>
          <ArrowLeft size={14} />
          Quay lại quản lý nội dung
        </button>
      </div>

      <div className="editor-heading">
        <div>
          <h1>{isEditing ? "Chỉnh sửa nội dung" : "Thêm nội dung"}</h1>
          <p>
            {isEditing
              ? `Cập nhật địa điểm / bảo tàng, mã ${initial.id}.`
              : "Tạo địa điểm khám phá mới với hình ảnh và vị trí chính xác."}
          </p>
        </div>

        <span className="editor-draft-label">
          ● {isEditing ? "Đang chỉnh sửa" : "Chưa lưu"}
        </span>
      </div>

      <form
        onSubmit={(e) => {
          e.preventDefault();
          submit("publish");
        }}
      >
        <section className="editor-card editor-type-card">
          <div className="editor-section-title">
            <b>Loại nội dung *</b>
            <span>* Thông tin bắt buộc</span>
          </div>

          <div className="editor-type-options">
            <button
              type="button"
              className="editor-type-option selected"
            >
              <Building2 size={19} />

              <span>
                <b>Địa điểm / bảo tàng</b>
                <small>Điểm tham quan, không gian khám phá</small>
              </span>

              <i />
            </button>

            <button
              type="button"
              className="editor-type-option"
              onClick={onCreateArtifact}
            >
              <Landmark size={19} />

              <span>
                <b>Hiện vật</b>
                <small>Đối tượng, hiện vật được trưng bày</small>
              </span>

              <i />
            </button>
          </div>
        </section>

        <div className="editor-columns">
          <div className="editor-left-column">
            <section className="editor-card">
              <div className="editor-card-heading">
                <h3>Thông tin cơ bản</h3>
                <p>Nội dung hiển thị trên trang chi tiết trong ứng dụng.</p>
              </div>

              <label className="editor-field">
                Tên {type === "Hiện vật" ? "hiện vật" : "địa điểm / bảo tàng"} *
                <input
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="Nhập tên nội dung"
                  required
                />
              </label>

              {type === "Địa điểm" && (
                <label className="editor-field">
                  Phân loại *
                  <select
                    value={category}
                    onChange={(e) => setCategory(e.target.value)}
                  >
                    <option>Bảo tàng</option>
                    <option>Địa điểm du lịch</option>
                    <option>Di tích lịch sử</option>
                    <option>Di sản văn hóa</option>
                    <option>Danh lam thắng cảnh</option>
                  </select>
                </label>
              )}

              <label className="editor-field">
                Mô tả *
                <textarea
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="Giới thiệu lịch sử, ý nghĩa và thông tin tham quan..."
                  rows={5}
                  required
                />
              </label>

              <small className="editor-help">
                Viết mô tả dễ hiểu, kiểm tra nguồn trước khi bổ sung thông tin
                lịch sử.
              </small>
            </section>

            <section className="editor-card">
              <div className="editor-card-heading editor-image-heading">
                <div>
                  <h3>Hình ảnh địa điểm</h3>
                  <p>Ảnh nét giúp người khám phá nhận diện nội dung.</p>
                </div>
                <span>{image ? "1 ảnh đã tải" : "0 ảnh đã tải"}</span>
              </div>

              <label className="editor-field">
                Ảnh đại diện *
                <span className="editor-upload-main">
                  {image ? (
                    <img src={image} alt="Ảnh đại diện xem trước" />
                  ) : (
                    <span className="editor-image-placeholder">
                      <ImagePlus size={25} />
                      <b>Chưa có ảnh đại diện</b>
                      <small>Chọn ảnh từ thiết bị của bạn</small>
                    </span>
                  )}

                  <input
                    type="file"
                    accept="image/*"
                    onChange={handleMainImage}
                  />
                </span>
              </label>

              {image && (
                <div className="editor-image-actions">
                  <span>✓ Ảnh đã được chọn</span>

                  <label>
                    Thay ảnh
                    <input
                      type="file"
                      accept="image/*"
                      onChange={handleMainImage}
                    />
                  </label>

                  <button type="button" onClick={() => setImage("")}>
                    <Trash2 size={13} />
                  </button>
                </div>
              )}

              <div className="editor-extra-heading">
                <b>Ảnh bổ sung</b>
                <span>{extraImages.length} / 9 ảnh</span>
              </div>

              <label className="editor-upload-extra">
                <UploadCloud size={23} />
                <b>Chọn ảnh hoặc kéo thả vào đây</b>
                <small>Thêm các góc chụp khác để hoàn thiện bộ ảnh.</small>
                <input
                  type="file"
                  accept="image/*"
                  multiple
                  onChange={handleExtraImages}
                  disabled={extraImages.length >= 9}
                />
              </label>

              {extraImages.length > 0 && (
                <div className="editor-extra-grid">
                  {extraImages.map((src, index) => (
                    <div key={`${index}-${src.slice(0, 20)}`}>
                      <img src={src} alt={`Ảnh bổ sung ${index + 1}`} />
                      <button
                        type="button"
                        onClick={() =>
                          setExtraImages((previous) =>
                            previous.filter((_, i) => i !== index),
                          )
                        }
                      >
                        <Trash2 size={12} />
                      </button>
                    </div>
                  ))}
                </div>
              )}

              <small className="editor-help">
                Hỗ trợ ảnh JPG, PNG hoặc WEBP, tối đa 10 MB mỗi ảnh.
              </small>
            </section>
          </div>

          <div className="editor-right-column">
            <section className="editor-card">
              <div className="editor-card-heading">
                <h3>Địa chỉ &amp; tọa độ GPS</h3>
                <p>Tìm địa chỉ hoặc nhập tọa độ để xác định vị trí.</p>
              </div>

              <label className="editor-field">
                Tìm địa chỉ trên bản đồ
                <div className="editor-address-search">
                  <input
                    value={locationName}
                    onChange={(e) => setLocationName(e.target.value)}
                    placeholder="Nhập địa chỉ, tên đường hoặc địa điểm..."
                  />

                  <button
                    type="button"
                    onClick={() => {
                      if (locationName.trim()) {
                        setAddress(locationName.trim());
                        setErrors("");
                      } else {
                        setErrors("Vui lòng nhập địa chỉ cần tìm.");
                      }
                    }}
                    aria-label="Sử dụng địa chỉ đã nhập"
                  >
                    <Search size={15} />
                  </button>
                </div>
              </label>

              <label className="editor-field">
                Địa chỉ *
                <textarea
                  rows={2}
                  value={address}
                  onChange={(e) => setAddress(e.target.value)}
                  placeholder="Số nhà, đường, phường, tỉnh/thành phố..."
                  required
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
                    required
                  />
                </label>

                <label className="editor-field">
                  Kinh độ (Longitude) *
                  <input
                    type="number"
                    step="any"
                    value={longitude}
                    onChange={(e) => setLongitude(e.target.value)}
                    required
                  />
                </label>
              </div>

              <small className="editor-help">
                Hệ tọa độ WGS84 · Kinh độ và vĩ độ thập phân.
              </small>

              <div className="editor-map-heading">
                <b>Vị trí trên bản đồ</b>
                <span>
                  <MapPinned size={12} /> Tọa độ đã nhập
                </span>
              </div>

              <RealMap
                latitude={latitude}
                longitude={longitude}
                onPick={(la, lo) => {
                  setLatitude(la.toFixed(6));
                  setLongitude(lo.toFixed(6));
                }}
                emptyText="Nhập vĩ độ và kinh độ hợp lệ để hiện ghim"
              />

              <p className="editor-map-note">
                Bản đồ OpenStreetMap thật. Nhập vĩ độ/kinh độ để ghim tự di
                chuyển tới đúng vị trí, hoặc bấm/kéo ghim trên bản đồ để chọn
                tọa độ.
              </p>
            </section>

            <div className="editor-gps-notice">
              <Info size={15} />
              <span>
                Vị trí này sẽ được dùng cho các hiện vật gắn với địa điểm. Hãy
                kiểm tra tọa độ trước khi lưu.
              </span>
            </div>
          </div>
        </div>

        {errors && (
          <div className="editor-error" role="alert">
            {errors}
          </div>
        )}

        <div className="editor-bottom-bar">
          <div>
            <b>Sẵn sàng chia sẻ nội dung?</b>
            <p>Lưu nháp để tiếp tục sau hoặc xuất bản lên ứng dụng.</p>
          </div>

          <div className="editor-bottom-actions">
            <button type="button" onClick={onCancel} disabled={busy}>
              Hủy
            </button>

            <button
              type="button"
              className="editor-save-draft"
              onClick={() => submit("draft")}
              disabled={busy}
            >
              <Save size={14} />
              {busy ? "Đang lưu..." : "Lưu nháp"}
            </button>

            <button className="admin-primary" type="submit" disabled={busy}>
              <Check size={14} />
              {busy ? "Đang xử lý..." : "Xuất bản"}
            </button>
          </div>
        </div>
      </form>
    </div>
  );
}


function ArtifactForm({ places = [], onCancel, onSave, onSwitchToPlace, busy, initial }) {
  const [name, setName] = useState(initial?.name || "");
  const [description, setDescription] = useState(initial?.description || "");
  const [placeId, setPlaceId] = useState(
    initial?.placeId != null ? String(initial.placeId) : "",
  );
  const [standalone, setStandalone] = useState(
    initial ? initial.placeId == null : false,
  );
  const [image, setImage] = useState(initial?.image || "");
  const [error, setError] = useState("");
  const [artLat, setArtLat] = useState(
    initial?.latitude != null ? String(initial.latitude) : "",
  );
  const [artLng, setArtLng] = useState(
    initial?.longitude != null ? String(initial.longitude) : "",
  );
  const isEditing = Boolean(initial);

  const selectedPlace = places.find(
    (place) => String(place.id) === String(placeId)
  );

  // Chọn địa điểm -> tự điền tọa độ của địa điểm, người dùng vẫn sửa được.
  const handleSelectPlace = (id) => {
    setPlaceId(id);
    const place = places.find((item) => String(item.id) === String(id));
    if (place) {
      setArtLat(place.latitude != null ? String(place.latitude) : "");
      setArtLng(place.longitude != null ? String(place.longitude) : "");
    }
  };

  const handleImage = (e) => {
    const file = e.target.files?.[0];

    if (!file) return;

    if (!file.type.startsWith("image/")) {
      setError("Vui lòng chọn tệp hình ảnh hợp lệ.");
      return;
    }

    if (file.size > 10 * 1024 * 1024) {
      setError("Ảnh không được vượt quá 10 MB.");
      return;
    }

    const reader = new FileReader();

    reader.onload = () => {
      setImage(reader.result);
      setError("");
    };

    reader.readAsDataURL(file);
    e.target.value = "";
  };

  const saveArtifact = (action) => {
    if (!name.trim() || !description.trim()) {
      setError("Vui lòng nhập tên hiện vật và mô tả.");
      return;
    }

    if (!standalone && !selectedPlace) {
      setError("Vui lòng chọn địa điểm chứa hiện vật.");
      return;
    }

    const hasCoords = artLat.trim() !== "" || artLng.trim() !== "";
    const latNum = parseCoord(artLat, -90, 90);
    const lngNum = parseCoord(artLng, -180, 180);

    if (hasCoords && (latNum === null || lngNum === null)) {
      setError(
        "Tọa độ không hợp lệ: vĩ độ từ -90 đến 90, kinh độ từ -180 đến 180."
      );
      return;
    }

    setError("");

    onSave(
      {
        name: name.trim(),
        type: "Hiện vật",
        description: description.trim(),
        image,

        // Lưu quan hệ hiện vật - địa điểm.
        placeId: selectedPlace?.id ?? null,
        placeName: selectedPlace?.name ?? "",
        location: selectedPlace?.location ?? "",
        latitude: hasCoords ? latNum : null,
        longitude: hasCoords ? lngNum : null,
      },
      action
    );
  };

  return (
    <div className="content-editor">
      <div className="editor-back">
        <button type="button" onClick={onCancel}>
          <ArrowLeft size={14} />
          Quay lại quản lý nội dung
        </button>
      </div>

      <div className="editor-heading">
        <div>
          <h1>{isEditing ? "Chỉnh sửa hiện vật" : "Thêm hiện vật"}</h1>
          <p>
            {isEditing
              ? `Cập nhật hiện vật, mã ${initial.id}.`
              : "Thêm hiện vật và liên kết với địa điểm hoặc bảo tàng trưng bày."}
          </p>
        </div>

        <span className="editor-draft-label">
          ● {isEditing ? "Đang chỉnh sửa" : "Chưa lưu"}
        </span>
      </div>

      <div className="editor-card editor-type-card">
        <div className="editor-section-title">
          <b>Loại nội dung *</b>
          <span>* Thông tin bắt buộc</span>
        </div>

        <div className="editor-type-options">
          <button
            type="button"
            className="editor-type-option"
            onClick={onSwitchToPlace}
          >
            <Building2 size={19} />
            <span>
              <b>Địa điểm / bảo tàng</b>
              <small>Điểm tham quan, không gian khám phá</small>
            </span>
            <i />
          </button>

          <button
            type="button"
            className="editor-type-option selected"
          >
            <Landmark size={19} />
            <span>
              <b>Hiện vật</b>
              <small>Đối tượng, hiện vật được trưng bày</small>
            </span>
            <i />
          </button>
        </div>
      </div>

      <form
        onSubmit={(e) => {
          e.preventDefault();
          saveArtifact("publish");
        }}
      >
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
                  required
                />
              </label>

              <label className="editor-field">
                Mô tả *
                <textarea
                  rows={5}
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="Nhập thông tin lịch sử, nguồn gốc và ý nghĩa hiện vật..."
                  required
                />
              </label>
            </section>

            <section className="editor-card">
              <div className="editor-card-heading">
                <h3>Hình ảnh hiện vật</h3>
                <p>Ảnh nhận diện hiện vật trong ứng dụng.</p>
              </div>

              <label className="editor-field">
                Ảnh đại diện
                <span className="editor-upload-main">
                  {image ? (
                    <img src={image} alt="Ảnh hiện vật" />
                  ) : (
                    <span className="editor-image-placeholder">
                      <ImagePlus size={25} />
                      <b>Chọn ảnh hiện vật</b>
                      <small>JPG, PNG hoặc WEBP · Tối đa 10 MB</small>
                    </span>
                  )}

                  <input
                    type="file"
                    accept="image/*"
                    onChange={handleImage}
                  />
                </span>
              </label>

              {image && (
                <button
                  type="button"
                  className="text-button"
                  onClick={() => setImage("")}
                >
                  Xóa ảnh
                </button>
              )}
            </section>
          </div>

          <div className="editor-right-column">
            <section className="editor-card">
              <div className="editor-card-heading">
                <h3>Địa điểm chứa hiện vật</h3>
                <p>Chọn nơi hiện vật đang được trưng bày.</p>
              </div>

              <label className="editor-field">
                Chọn địa điểm / bảo tàng *
                <select
                  value={placeId}
                  disabled={standalone}
                  onChange={(e) => handleSelectPlace(e.target.value)}
                >
                  <option value="">-- Chọn địa điểm --</option>

                  {places.map((place) => (
                    <option key={place.id} value={String(place.id)}>
                      {place.name}
                    </option>
                  ))}
                </select>
              </label>

              {selectedPlace && !standalone && (
                <div className="editor-selected-place">
                  {selectedPlace.image ? (
                    <img
                      src={selectedPlace.image}
                      alt={selectedPlace.name}
                    />
                  ) : (
                    <Landmark size={25} />
                  )}

                  <div>
                    <b>{selectedPlace.name}</b>
                    <small>
                      {selectedPlace.location || "Chưa có địa chỉ"}
                    </small>
                    <small>
                      GPS: {selectedPlace.latitude ?? "—"},{" "}
                      {selectedPlace.longitude ?? "—"}
                    </small>
                  </div>

                  <button
                    type="button"
                    onClick={() => setPlaceId("")}
                    aria-label="Bỏ chọn địa điểm"
                  >
                    ×
                  </button>
                </div>
              )}

              {places.length === 0 && (
                <p className="editor-map-note">
                  Chưa có địa điểm đã xuất bản. Hãy tạo địa điểm trước khi
                  liên kết hiện vật.
                </p>
              )}

              <label className="editor-standalone-option">
                <input
                  type="checkbox"
                  checked={standalone}
                  onChange={(e) => {
                    setStandalone(e.target.checked);
                    setPlaceId("");
                  }}
                />
                Hiện vật chưa gắn địa điểm
              </label>
            </section>

            <section className="editor-card">
              <div className="editor-card-heading">
                <h3>Địa chỉ & tọa độ GPS</h3>
                <p>Tự điền từ địa điểm đã chọn, bạn có thể nhập lại tọa độ riêng.</p>
              </div>

              <label className="editor-field">
                Địa chỉ
                <textarea
                  rows={2}
                  readOnly
                  value={selectedPlace?.location || ""}
                  placeholder="Chọn địa điểm để tự động điền địa chỉ"
                />
              </label>

              <div className="editor-coordinate-grid">
                <label className="editor-field">
                  Vĩ độ (Latitude)
                  <input
                    type="number"
                    step="any"
                    value={artLat}
                    onChange={(e) => setArtLat(e.target.value)}
                    placeholder="Ví dụ: 10.7794"
                  />
                </label>

                <label className="editor-field">
                  Kinh độ (Longitude)
                  <input
                    type="number"
                    step="any"
                    value={artLng}
                    onChange={(e) => setArtLng(e.target.value)}
                    placeholder="Ví dụ: 106.6921"
                  />
                </label>
              </div>

              <RealMap
                latitude={artLat}
                longitude={artLng}
                onPick={(la, lo) => {
                  setArtLat(la.toFixed(6));
                  setArtLng(lo.toFixed(6));
                }}
                emptyText="Nhập vĩ độ và kinh độ hợp lệ để hiện ghim"
              />

              <p className="editor-map-note">
                Nhập vĩ độ/kinh độ để ghim di chuyển tới đúng vị trí, hoặc
                bấm/kéo ghim trên bản đồ. Để trống nếu chưa có tọa độ.
              </p>
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
            <b>Sẵn sàng chia sẻ nội dung?</b>
            <p>Lưu nháp để tiếp tục sau hoặc xuất bản lên ứng dụng.</p>
          </div>

          <div className="editor-bottom-actions">
            <button type="button" onClick={onCancel} disabled={busy}>
              Hủy
            </button>

            <button
              type="button"
              className="editor-save-draft"
              onClick={() => saveArtifact("draft")}
              disabled={busy}
            >
              <Save size={14} />
              {busy ? "Đang lưu..." : "Lưu nháp"}
            </button>

            <button type="submit" className="admin-primary" disabled={busy}>
              <Check size={14} />
              {busy ? "Đang xử lý..." : "Xuất bản"}
            </button>
          </div>
        </div>
      </form>
    </div>
  );
}

