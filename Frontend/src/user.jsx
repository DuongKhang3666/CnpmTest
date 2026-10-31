import React, {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import L from "leaflet";
import "leaflet/dist/leaflet.css";

import * as userApi from "./userApi";
import "./user.css";
import "./user-extra.css";
import {
  Home,
  Map as MapIcon,
  Heart,
  Search,
  Navigation,
  MapPin,
  Volume2,
  Camera,
  X,
  Globe,
  History,
  LoaderCircle,
  Pause,
  Play,
  Bluetooth,
  Languages,
  Square,
} from "lucide-react";

const translateCacheMemory = new Map();
const translatePending = new Map();
const imageFallback = (name) =>
  `https://placehold.co/640x420/e9dfd2/5c392b?text=${encodeURIComponent(name || "Vietnam")}`;
const distance = (a, b, c, d) => {
  const rad = (x) => (x * Math.PI) / 180,
    R = 6371000;
  const x = rad(c - a),
    y = rad(d - b);
  const h =
    Math.sin(x / 2) ** 2 +
    Math.cos(rad(a)) * Math.cos(rad(c)) * Math.sin(y / 2) ** 2;
  return 2 * R * Math.asin(Math.sqrt(h));
};
const UI_TEXT = {
  vi: { explore: "Khám phá", history: "Lịch sử", likes: "Yêu thích", search: "Tìm địa điểm, hiện vật...", gpsOn: "Tắt GPS", gpsOff: "Bật GPS", bluetoothOn: "Tắt Bluetooth", bluetoothOff: "Bật Bluetooth", myLocation: "Vị trí tôi", scanQr: "Quét QR", listen: "Nghe thuyết minh", details: "Chi tiết", allSites: "Xem tất cả địa điểm", allArtifacts: "Xem tất cả hiện vật", siteHeading: "Địa điểm nổi bật", artifactHeading: "Hiện vật nổi bật", description: "Mô tả", address: "Địa chỉ", city: "Thành phố", close: "Đóng", pause: "Tạm dừng", resume: "Tiếp tục", skip: "Bỏ qua" , loading: 'Đang tải dữ liệu và xác thực thiết bị...', retry: 'Thử lại', accessDenied: 'Bạn không được phép truy cập', serverOffline: 'Không kết nối được máy chủ', expired: 'Thời hạn truy cập thiết bị đã hết. Hãy quét QR mới hoặc nhờ admin gia hạn.', needQr: 'Bạn cần quét QR đăng nhập do quản trị viên cung cấp.', invalidQr: 'QR không hợp lệ hoặc quyền truy cập đã hết hạn.', qrTitle: 'Quét QR thuyết minh', qrHint: 'Đưa mã QR vào giữa khung hình. Camera cần quyền truy cập.', closeCamera: 'Đóng camera', lostConnection: 'Mất kết nối máy chủ realtime.', findLocation: 'Tìm địa điểm', findNow: 'Tìm ngay', back: 'Quay lại', allPlaces: 'Tất cả địa điểm', allObjects: 'Tất cả hiện vật', searchResults: 'Kết quả tìm kiếm', searchArtifact: 'Tìm tên hiện vật...', searchAll: 'Tìm tên địa điểm / hiện vật...', filterCity: 'Lọc theo thành phố...', filterSite: 'Lọc theo địa điểm chứa hiện vật...', clearFilter: 'Xóa lọc', all: 'Tất cả', artifacts: 'Hiện vật', historyEmpty: 'Chưa có lịch sử nghe.', likesEmpty: 'Bạn chưa có đối tượng yêu thích.', liked: 'Yêu thích', removeLike: 'Bỏ yêu thích', listenCount: 'lượt nghe', atLocation: 'Hiện vật tại địa điểm', narration: 'Nghe thuyết minh', currentlyNarrating: 'Đang thuyết minh', queued: 'bài đang chờ', pause: 'Tạm dừng', resume: 'Tiếp tục', skip: 'Bỏ qua', addressLabel: 'Địa chỉ:', cityLabel: 'Thành phố:', description: 'Mô tả', plays: 'Lượt nghe:', updated: 'Đang cập nhật', view: 'Xem', currentLocation: 'Vị trí hiện tại của bạn', methodNarration: 'Thuyết minh', agoJust: 'Vừa nghe', minutesAgo: 'phút trước', hoursAgo: 'giờ trước', daysAgo: 'ngày trước', languageLabel: 'Ngôn ngữ giao diện / thuyết minh'},
  en: { explore: "Explore", history: "History", likes: "Favorites", search: "Search places and artifacts...", gpsOn: "Turn GPS off", gpsOff: "Turn GPS on", bluetoothOn: "Turn Bluetooth off", bluetoothOff: "Turn Bluetooth on", myLocation: "My location", scanQr: "Scan QR", listen: "Listen to narration", details: "Details", allSites: "View all places", allArtifacts: "View all artifacts", siteHeading: "Featured places", artifactHeading: "Featured artifacts", description: "Description", address: "Address", city: "City", close: "Close", pause: "Pause", resume: "Resume", skip: "Skip" , loading: 'Loading data and verifying device...', retry: 'Try again', accessDenied: 'Access denied', serverOffline: 'Cannot connect to server', expired: "This device's access has expired. Scan a new QR code or ask the administrator to extend access.", needQr: 'Scan the login QR code provided by the administrator.', invalidQr: 'Invalid QR code or access has expired.', qrTitle: 'Scan narration QR code', qrHint: 'Place the QR code in the center of the frame. Camera access is required.', closeCamera: 'Close camera', lostConnection: 'Realtime server connection lost.', findLocation: 'Find a location', findNow: 'Find now', back: 'Back', allPlaces: 'All places', allObjects: 'All artifacts', searchResults: 'Search results', searchArtifact: 'Search artifact names...', searchAll: 'Search places and artifacts...', filterCity: 'Filter by city...', filterSite: 'Filter by location containing artifact...', clearFilter: 'Clear filters', all: 'All', artifacts: 'Artifacts', historyEmpty: 'No listening history yet.', likesEmpty: 'You have no favorites yet.', liked: 'Favorites', removeLike: 'Remove favorite', listenCount: 'listens', atLocation: 'Artifacts at this location', narration: 'Listen to narration', currentlyNarrating: 'Narrating', queued: 'queued', pause: 'Pause', resume: 'Resume', skip: 'Skip', addressLabel: 'Address:', cityLabel: 'City:', description: 'Description', plays: 'Listens:', updated: 'Updating', view: 'View', currentLocation: 'Your current location', methodNarration: 'Narration', agoJust: 'Just now', minutesAgo: 'minutes ago', hoursAgo: 'hours ago', daysAgo: 'days ago', languageLabel: 'Interface / narration language'},
  "zh-CN": { explore: "探索", history: "历史", likes: "收藏", search: "搜索地点和文物...", gpsOn: "关闭 GPS", gpsOff: "开启 GPS", bluetoothOn: "关闭蓝牙", bluetoothOff: "开启蓝牙", myLocation: "我的位置", scanQr: "扫描二维码", listen: "收听讲解", details: "详情", allSites: "查看全部地点", allArtifacts: "查看全部文物", siteHeading: "精选地点", artifactHeading: "精选文物", description: "描述", address: "地址", city: "城市", close: "关闭", pause: "暂停", resume: "继续", skip: "跳过" , loading: '正在加载数据并验证设备...', retry: '重试', accessDenied: '无权访问', serverOffline: '无法连接服务器', expired: '此设备的访问期限已过。请扫描新的二维码或联系管理员延期。', needQr: '请扫描管理员提供的登录二维码。', invalidQr: '二维码无效或访问权限已过期。', qrTitle: '扫描讲解二维码', qrHint: '将二维码放在画面中央。需要允许使用摄像头。', closeCamera: '关闭摄像头', lostConnection: '实时服务器连接已断开。', findLocation: '查找地点', findNow: '立即查找', back: '返回', allPlaces: '所有地点', allObjects: '所有文物', searchResults: '搜索结果', searchArtifact: '搜索文物名称...', searchAll: '搜索地点和文物...', filterCity: '按城市筛选...', filterSite: '按文物所在地点筛选...', clearFilter: '清除筛选', all: '全部', artifacts: '文物', historyEmpty: '暂无收听记录。', likesEmpty: '暂无收藏。', liked: '收藏', removeLike: '取消收藏', listenCount: '次收听', atLocation: '该地点的文物', narration: '收听讲解', currentlyNarrating: '正在讲解', queued: '个待播放', pause: '暂停', resume: '继续', skip: '跳过', addressLabel: '地址：', cityLabel: '城市：', description: '描述', plays: '收听次数：', updated: '更新中', view: '查看', currentLocation: '您的当前位置', methodNarration: '讲解', agoJust: '刚刚收听', minutesAgo: '分钟前', hoursAgo: '小时前', daysAgo: '天前', languageLabel: '界面 / 讲解语言'},
  ja: { explore: "探索", history: "履歴", likes: "お気に入り", search: "場所・展示品を検索...", gpsOn: "GPSをオフ", gpsOff: "GPSをオン", bluetoothOn: "Bluetoothをオフ", bluetoothOff: "Bluetoothをオン", myLocation: "現在地", scanQr: "QRをスキャン", listen: "音声ガイド", details: "詳細", allSites: "すべての場所を見る", allArtifacts: "すべての展示品を見る", siteHeading: "おすすめの場所", artifactHeading: "おすすめの展示品", description: "説明", address: "住所", city: "都市", close: "閉じる", pause: "一時停止", resume: "再開", skip: "スキップ" , loading: 'データを読み込み、端末を確認しています...', retry: '再試行', accessDenied: 'アクセスできません', serverOffline: 'サーバーに接続できません', expired: 'この端末のアクセス期限が切れました。新しいQRコードをスキャンするか、管理者に延長を依頼してください。', needQr: '管理者が提供したログインQRコードをスキャンしてください。', invalidQr: 'QRコードが無効か、アクセス期限が切れています。', qrTitle: '音声ガイドQRをスキャン', qrHint: 'QRコードを画面中央に置いてください。カメラへのアクセスが必要です。', closeCamera: 'カメラを閉じる', lostConnection: 'リアルタイムサーバーへの接続が切れました。', findLocation: '場所を検索', findNow: '今すぐ検索', back: '戻る', allPlaces: 'すべての場所', allObjects: 'すべての展示品', searchResults: '検索結果', searchArtifact: '展示品名を検索...', searchAll: '場所・展示品を検索...', filterCity: '都市で絞り込む...', filterSite: '展示品のある場所で絞り込む...', clearFilter: '絞り込みを解除', all: 'すべて', artifacts: '展示品', historyEmpty: '再生履歴はありません。', likesEmpty: 'お気に入りはありません。', liked: 'お気に入り', removeLike: 'お気に入りを解除', listenCount: '回再生', atLocation: 'この場所の展示品', narration: '音声ガイドを聞く', currentlyNarrating: 'ガイド再生中', queued: '件待機中', pause: '一時停止', resume: '再開', skip: 'スキップ', addressLabel: '住所：', cityLabel: '都市：', description: '説明', plays: '再生回数：', updated: '更新中', view: '見る', currentLocation: '現在地', methodNarration: '音声ガイド', agoJust: 'たった今', minutesAgo: '分前', hoursAgo: '時間前', daysAgo: '日前', languageLabel: '画面 / 音声ガイドの言語'},
  ko: { explore: "탐색", history: "기록", likes: "즐겨찾기", search: "장소 및 유물 검색...", gpsOn: "GPS 끄기", gpsOff: "GPS 켜기", bluetoothOn: "블루투스 끄기", bluetoothOff: "블루투스 켜기", myLocation: "내 위치", scanQr: "QR 스캔", listen: "해설 듣기", details: "상세 정보", allSites: "모든 장소 보기", allArtifacts: "모든 유물 보기", siteHeading: "추천 장소", artifactHeading: "추천 유물", description: "설명", address: "주소", city: "도시", close: "닫기", pause: "일시정지", resume: "계속", skip: "건너뛰기" , loading: '데이터를 불러오고 기기를 확인하는 중...', retry: '다시 시도', accessDenied: '접근할 수 없습니다', serverOffline: '서버에 연결할 수 없습니다', expired: '이 기기의 접근 기간이 만료되었습니다. 새 QR 코드를 스캔하거나 관리자에게 연장을 요청하세요.', needQr: '관리자가 제공한 로그인 QR 코드를 스캔하세요.', invalidQr: 'QR 코드가 유효하지 않거나 접근 기간이 만료되었습니다.', qrTitle: '해설 QR 스캔', qrHint: 'QR 코드를 화면 중앙에 놓으세요. 카메라 권한이 필요합니다.', closeCamera: '카메라 닫기', lostConnection: '실시간 서버 연결이 끊겼습니다.', findLocation: '장소 찾기', findNow: '지금 찾기', back: '뒤로', allPlaces: '모든 장소', allObjects: '모든 유물', searchResults: '검색 결과', searchArtifact: '유물 이름 검색...', searchAll: '장소 및 유물 검색...', filterCity: '도시별 필터...', filterSite: '유물이 있는 장소별 필터...', clearFilter: '필터 지우기', all: '전체', artifacts: '유물', historyEmpty: '청취 기록이 없습니다.', likesEmpty: '즐겨찾기가 없습니다.', liked: '즐겨찾기', removeLike: '즐겨찾기 해제', listenCount: '회 청취', atLocation: '이 장소의 유물', narration: '해설 듣기', currentlyNarrating: '해설 재생 중', queued: '개 대기 중', pause: '일시정지', resume: '계속', skip: '건너뛰기', addressLabel: '주소:', cityLabel: '도시:', description: '설명', plays: '청취 횟수:', updated: '업데이트 중', view: '보기', currentLocation: '현재 위치', methodNarration: '해설', agoJust: '방금', minutesAgo: '분 전', hoursAgo: '시간 전', daysAgo: '일 전', languageLabel: '화면 / 해설 언어'},
  fr: { explore: "Explorer", history: "Historique", likes: "Favoris", search: "Rechercher lieux et objets...", gpsOn: "Désactiver GPS", gpsOff: "Activer GPS", bluetoothOn: "Désactiver Bluetooth", bluetoothOff: "Activer Bluetooth", myLocation: "Ma position", scanQr: "Scanner QR", listen: "Écouter le commentaire", details: "Détails", allSites: "Voir tous les lieux", allArtifacts: "Voir tous les objets", siteHeading: "Lieux à découvrir", artifactHeading: "Objets remarquables", description: "Description", address: "Adresse", city: "Ville", close: "Fermer", pause: "Pause", resume: "Reprendre", skip: "Passer" , loading: 'Chargement des données et vérification de l’appareil...', retry: 'Réessayer', accessDenied: 'Accès refusé', serverOffline: 'Connexion au serveur impossible', expired: 'L’accès de cet appareil a expiré. Scannez un nouveau QR code ou demandez une prolongation à l’administrateur.', needQr: 'Scannez le QR code de connexion fourni par l’administrateur.', invalidQr: 'QR code invalide ou accès expiré.', qrTitle: 'Scanner le QR code audio', qrHint: 'Placez le QR code au centre de l’image. L’accès à la caméra est nécessaire.', closeCamera: 'Fermer la caméra', lostConnection: 'Connexion au serveur temps réel perdue.', findLocation: 'Trouver un lieu', findNow: 'Rechercher', back: 'Retour', allPlaces: 'Tous les lieux', allObjects: 'Tous les objets', searchResults: 'Résultats de recherche', searchArtifact: 'Rechercher des objets...', searchAll: 'Rechercher des lieux et objets...', filterCity: 'Filtrer par ville...', filterSite: 'Filtrer par lieu de l’objet...', clearFilter: 'Effacer les filtres', all: 'Tout', artifacts: 'Objets', historyEmpty: 'Aucun historique d’écoute.', likesEmpty: 'Aucun favori.', liked: 'Favoris', removeLike: 'Retirer des favoris', listenCount: 'écoutes', atLocation: 'Objets de ce lieu', narration: 'Écouter le commentaire', currentlyNarrating: 'Commentaire en cours', queued: 'en attente', pause: 'Pause', resume: 'Reprendre', skip: 'Passer', addressLabel: 'Adresse :', cityLabel: 'Ville :', description: 'Description', plays: 'Écoutes :', updated: 'Mise à jour', view: 'Voir', currentLocation: 'Votre position actuelle', methodNarration: 'Commentaire', agoJust: 'À l’instant', minutesAgo: 'min', hoursAgo: 'h', daysAgo: 'j', languageLabel: 'Langue de l’interface / du commentaire'},
};
const timeAgo = (date, language = "vi") => {
  const mins = Math.max(0, Math.floor((Date.now() - new Date(date).getTime()) / 60000));
  const t = UI_TEXT[language] || UI_TEXT.vi;
  if (mins < 1) return t.agoJust || "Just now";
  if (mins < 60) return `${mins} ${t.minutesAgo || "minutes ago"}`;
  const h = Math.floor(mins / 60);
  if (h < 24) return `${h} ${t.hoursAgo || "hours ago"}`;
  return `${Math.floor(h / 24)} ${t.daysAgo || "days ago"}`;
};

export default function User() {
  const [position, setPosition] = useState(null);
  const [view, setView] = useState({ status: "checking" });
  const [connected, setConnected] = useState(true);
  const [sites, setSites] = useState([]);
  const [artifacts, setArtifacts] = useState([]);
  const [narrations, setNarrations] = useState([]);
  const [languages, setLanguages] = useState([]);
  const [language, setLanguage] = useState(
    localStorage.getItem("guideLanguage") || "vi",
  );
  // Giao diện cho ngôn ngữ mới do admin thêm: dịch động thay vì chỉ dùng UI_TEXT cố định.
  const [dynamicUiText, setDynamicUiText] = useState({});
  const ui = (key) =>
    UI_TEXT[language]?.[key] ||
    dynamicUiText[language]?.[key] ||
    UI_TEXT.vi[key] ||
    key;
  const [listens, setListens] = useState([]);
  const [history, setHistory] = useState([]);
  const [likes, setLikes] = useState([]);
  const [tab, setTab] = useState("home");
  const [query, setQuery] = useState("");
  const [detail, setDetail] = useState(null);
  const [message, setMessage] = useState("");
  const [gpsOn, setGpsOn] = useState(false);
  const [bluetoothOn, setBluetoothOn] = useState(false);
  const [activeNarration, setActiveNarration] = useState(null);
  const [queue, setQueue] = useState([]);
  const [cameraOn, setCameraOn] = useState(false);
  const [scanning, setScanning] = useState(false);
  const [playing, setPlaying] = useState(false);
  const [isPaused, setIsPaused] = useState(false);
  const [searchType, setSearchType] = useState("all");
  const [cityFilter, setCityFilter] = useState("");
  const [siteFilter, setSiteFilter] = useState("");
  const [loading, setLoading] = useState(true);
  const [translatedFields, setTranslatedFields] = useState({});
  const videoRef = useRef(null),
    streamRef = useRef(null),
    scanTimer = useRef(null),
    mapRef = useRef(null),
    mapNode = useRef(null),
    mapContainerRef = useRef(null),
    gpsWatch = useRef(null),
    entered = useRef(new Set()),
    currentAudio = useRef(null),
    audioEl = useRef(null),
    narrationMemory = useRef(new Map()),
    speakingRef = useRef(false),
    skipCurrentRef = useRef(false),
    queueRef = useRef([]),
    bluetoothDeviceRef = useRef(null);

  const loadData = useCallback(async () => {
    try {
      const c = await userApi.loadCatalog();
      setSites(c.sites);
      setArtifacts(c.artifacts);
      setNarrations(c.narrations);
      setLanguages(c.languages);
      setListens(c.listens);
      setHistory(c.history);
      setLikes(c.likes);
      setConnected(true);
    } catch (e) {
      // 401/403 đã được xử lý ở onAccessLost; ở đây chỉ báo mất kết nối.
      if (e?.reason === "offline") setConnected(false);
      else if (!e?.status || e.status >= 500) {
        setMessage("Không tải được dữ liệu từ máy chủ. Kiểm tra máy chủ và mạng.");
      }
    } finally {
      setLoading(false);
    }
  }, []);
  const accessGranted = view.status === "allowed";
  useEffect(() => {
    if (!accessGranted) return undefined;
    loadData();
    const t = setInterval(loadData, 30000);
    return () => clearInterval(t);
  }, [loadData, accessGranted]);

  // Vào hệ thống: token trên link QR (lần đầu) hoặc phiên đã lưu (mở lại trang không cần quét lại).
  useEffect(() => {
    let alive = true;
    let beat = null;
    const params = new URLSearchParams(location.search);
    const qrToken = params.get("token");

    const deny = (reason) => {
      if (!alive) return;
      setView({ status: "denied", reason });
      clearInterval(beat);
    };
    const offAccessLost = userApi.onAccessLost((reason) => deny(reason === "expired" ? "expired" : reason));

    const tick = async () => {
      try {
        await userApi.heartbeat();
        if (alive) setConnected(true);
      } catch (e) {
        if (alive && e?.reason === "offline") setConnected(false);
        // 401/403: onAccessLost đã đổi sang màn hình từ chối
      }
    };
    const onVisible = () => document.visibilityState === "visible" && tick();
    const onLeave = () => userApi.leaveSession();

    (async () => {
      const res = await userApi.joinSession(qrToken);
      if (!alive) return;
      if (res.ok) {
        // Bỏ token khỏi thanh địa chỉ: tránh lộ khi chia sẻ link/chụp màn hình; mở lại vẫn vào được nhờ phiên đã lưu.
        if (qrToken) {
          params.delete("token");
          const qs = params.toString();
          window.history.replaceState(null, "", `${location.pathname}${qs ? `?${qs}` : ""}${location.hash}`);
        }
        setConnected(true);
        setView({ status: "allowed" });
        beat = setInterval(tick, 20000);
        document.addEventListener("visibilitychange", onVisible);
        window.addEventListener("pagehide", onLeave);
      } else if (res.reason === "offline") {
        setView({ status: "offline" });
      } else {
        deny(res.reason || "token-invalid");
      }
    })();

    return () => {
      alive = false;
      clearInterval(beat);
      offAccessLost();
      document.removeEventListener("visibilitychange", onVisible);
      window.removeEventListener("pagehide", onLeave);
    };
  }, []);

  const allItems = useMemo(
    () => [
      ...sites.map((s) => ({
        ...s,
        targetType: "site",
        targetId: s.id,
        city: s.city || "Khác",
        image: s.img || imageFallback(s.name),
        totalListens: listens
          .filter(
            (l) =>
              l.targetType === "site" && String(l.targetId) === String(s.id),
          )
          .reduce((n, l) => n + Number(l.count || 0), 0),
      })),
      ...artifacts.map((a) => ({
        ...a,
        targetType: "artifact",
        targetId: a.id,
        city:
          sites.find((s) => String(s.id) === String(a.siteId))?.city || "Khác",
        siteName:
          sites.find((s) => String(s.id) === String(a.siteId))?.name || "",
        image: a.img || imageFallback(a.name),
        totalListens: listens
          .filter(
            (l) =>
              l.targetType === "artifact" &&
              String(l.targetId) === String(a.id),
          )
          .reduce((n, l) => n + Number(l.count || 0), 0),
      })),
    ],
    [sites, artifacts, listens],
  );
  const popularSites = useMemo(
    () =>
      allItems
        .filter((x) => x.targetType === "site")
        .sort((a, b) => b.totalListens - a.totalListens),
    [allItems],
  );
  const popularArtifacts = useMemo(
    () =>
      allItems
        .filter((x) => x.targetType === "artifact")
        .sort((a, b) => b.totalListens - a.totalListens),
    [allItems],
  );
  const narrFor = (item) =>
    narrations.find(
      (n) =>
        n.targetType === item.targetType &&
        String(n.targetId) === String(item.targetId),
    );
  const textFor = (item) => {
    const n = narrFor(item);
    return n?.useDescription === false && n?.text
      ? n.text
      : item.description ||
          n?.text ||
          `Thông tin về ${item.name} đang được cập nhật.`;
  };
  const openDetail = (item) => {
    setDetail(item);
    setTab("detail");
    setMessage("");
  };
  const translate = async (text, target) => {
    if (!text) return text;
    if (target === "vi") return text;

    // Cache bản dịch để hạn chế gọi dịch vụ lặp lại khi đổi màn hình/ngôn ngữ.
    const cacheKey = `${target}::${text}`;
    const memoryCache = translateCacheMemory.get(cacheKey);
    if (memoryCache) return memoryCache;
    try {
      const saved = JSON.parse(localStorage.getItem("guideTranslationCache") || "{}");
      if (saved[cacheKey]) {
        translateCacheMemory.set(cacheKey, saved[cacheKey]);
        return saved[cacheKey];
      }
    } catch {}

    // Gộp các yêu cầu trùng nhau đang chạy để tránh gửi nhiều request cùng lúc.
    if (translatePending.has(cacheKey)) return translatePending.get(cacheKey);

    const task = (async () => {
      let lastError;
      for (let attempt = 0; attempt < 2; attempt++) {
        try {
          const translated = await userApi.translateText(text, target);
          translateCacheMemory.set(cacheKey, translated);
          try {
            const saved = JSON.parse(localStorage.getItem("guideTranslationCache") || "{}");
            saved[cacheKey] = translated;
            const entries = Object.entries(saved);
            localStorage.setItem(
              "guideTranslationCache",
              JSON.stringify(Object.fromEntries(entries.slice(-1000))),
            );
          } catch {}
          return translated;
        } catch (error) {
          lastError = error;
          if (error?.status === 401 || error?.status === 403) break;
          if (attempt === 0) await new Promise((resolve) => setTimeout(resolve, 500));
        }
      }
      throw new Error(
        `Không dịch được: ${lastError?.message || "lỗi kết nối"}. Hãy kiểm tra dịch vụ dịch (LibreTranslate) của máy chủ.`,
      );
    })();

    translatePending.set(cacheKey, task);
    try {
      return await task;
    } finally {
      translatePending.delete(cacheKey);
    }
  };
  // Dịch nhiều đoạn ngắn cùng lúc: dùng cache trình duyệt, phần còn thiếu gửi theo lô (tối đa 40 đoạn/lần gọi)
  // thay vì mỗi đoạn một yêu cầu. Đoạn không dịch được giữ nguyên bản gốc.
  const translateMany = async (texts, target) => {
    if (target === "vi") return texts.map((t) => t || "");
    const out = new Array(texts.length);
    let saved = {};
    try { saved = JSON.parse(localStorage.getItem("guideTranslationCache") || "{}"); } catch {}
    const misses = new Map();
    texts.forEach((t, i) => {
      if (!t) { out[i] = t || ""; return; }
      const key = `${target}::${t}`;
      const hit = translateCacheMemory.get(key) || saved[key];
      if (hit) { translateCacheMemory.set(key, hit); out[i] = hit; return; }
      if (t.length > 10000) { out[i] = t; return; }
      if (!misses.has(t)) misses.set(t, []);
      misses.get(t).push(i);
    });
    const list = [...misses.keys()];
    let dirty = false;
    for (let i = 0; i < list.length; i += 40) {
      const chunk = list.slice(i, i + 40);
      let results;
      try { results = await userApi.translateBatch(chunk, target); }
      catch (e) { if (e?.status === 401 || e?.status === 403) break; results = []; }
      chunk.forEach((t, j) => {
        const r = results[j];
        if (r) { translateCacheMemory.set(`${target}::${t}`, r); saved[`${target}::${t}`] = r; dirty = true; }
        misses.get(t).forEach((idx) => { out[idx] = r || t; });
      });
    }
    if (dirty) {
      try { localStorage.setItem("guideTranslationCache", JSON.stringify(Object.fromEntries(Object.entries(saved).slice(-1000)))); } catch {}
    }
    texts.forEach((t, i) => { if (out[i] === undefined) out[i] = t || ""; });
    return out;
  };
  // Chỉ cần dịch động những ngôn ngữ chưa có bộ UI_TEXT viết sẵn.
  // Giới hạn số request đồng thời để không làm LibreTranslate quá tải.
  useEffect(() => {
    let cancelled = false;
    const run = async () => {
      if (language === "vi" || UI_TEXT[language]) return;
      const uiEntries = Object.entries(UI_TEXT.vi);
      const values = await translateMany(uiEntries.map(([, v]) => v), language);
      if (cancelled) return;
      const uiResult = Object.fromEntries(uiEntries.map(([k], i) => [k, values[i]]));
      setDynamicUiText((old) => ({ ...old, [language]: uiResult }));
    };
    run();
    return () => { cancelled = true; };
  }, [language]);

  const translatedField = (item, field, value) => {
    if (language === "vi") return value || "";
    const type = item?.targetType || (item?.siteId != null ? "artifact" : "site");
    return translatedFields[`${type}:${item?.id}:${field}:${language}`] || value || "";
  };
  useEffect(() => {
    let alive = true;
    const run = async () => {
      if (language === "vi") { setTranslatedFields({}); return; }
      const next = {};
      const slots = [];
      for (const item of allItems) {
        const fields = {
          name: item.name,
          description: item.description,
          address: item.address || "",
          city: item.city || "",
          siteName: item.siteName || "",
        };
        for (const [field, value] of Object.entries(fields)) {
          if (!value) continue;
          slots.push([`${item.targetType}:${item.id}:${field}:${language}`, value]);
        }
      }
      const values = await translateMany(slots.map(([, v]) => v), language);
      slots.forEach(([key], i) => { next[key] = values[i]; });
      if (alive) setTranslatedFields((old) => ({ ...old, ...next }));
    };
    run();
    return () => { alive = false; };
  }, [language, allItems]);
  // Lời thuyết minh đầy đủ chỉ tải khi mở trang chi tiết (qua /narrations/{loại}/{id}?lang=), không dịch hàng loạt.
  useEffect(() => {
    if (!detail || language === "vi") return undefined;
    let alive = true;
    (async () => {
      const dto = await loadNarration(detail, language).catch(() => null);
      if (alive && dto?.translated && String(dto.text || "").trim()) {
        setTranslatedFields((old) => ({ ...old, [`${detail.targetType}:${detail.id}:narration:${language}`]: dto.text }));
      }
    })();
    return () => { alive = false; };
  }, [language, detail]);

  // Thuyết minh theo đối tượng từ máy chủ (bản admin nhập > cache DB > dịch máy), nhớ trong bộ nhớ theo ngôn ngữ.
  const loadNarration = async (item, lang) => {
    const key = `${item.targetType}:${item.id ?? item.targetId}:${lang}`;
    const hit = narrationMemory.current.get(key);
    if (hit) return hit;
    try {
      const dto = await userApi.getNarration(item.targetType, item.id ?? item.targetId, lang);
      narrationMemory.current.set(key, dto);
      return dto;
    } catch (e) {
      if (e?.status === 401 || e?.status === 403) throw e;
      return null; // 404 (chưa có nội dung) hoặc lỗi mạng: dùng đường dự phòng phía trình duyệt
    }
  };
  // Phát tệp audio admin đã tải lên. Trả về true khi đã bắt đầu phát; false thì người gọi chuyển sang giọng đọc.
  const playAudioFile = (url, item, finish) =>
    new Promise((resolve) => {
      const el = new Audio(url);
      el.preload = "auto";
      audioEl.current = el;
      let settled = false;
      const done = (ok) => { if (!settled) { settled = true; resolve(ok); } };
      const release = () => { if (audioEl.current === el) audioEl.current = null; };
      el.onplaying = () => {
        speakingRef.current = true;
        setPlaying(true);
        setIsPaused(false);
        setActiveNarration(item);
        done(true);
      };
      el.onended = () => { release(); finish(); };
      el.onerror = () => {
        release();
        if (settled) finish(); else done(false);
      };
      el.play().catch(() => { release(); done(false); });
      setTimeout(() => {
        if (!settled) { el.onerror = null; el.onended = null; el.pause(); el.removeAttribute("src"); release(); done(false); }
      }, 8000);
    });
  const speak = async (item, method = "manual") => {
    if (!item) return;
    // Nếu đang đọc thì xếp bài mới vào hàng đợi, không ngắt bài hiện tại.
    if (speakingRef.current || speechSynthesis.speaking) {
      setQueue((q) => {
        if (
          q.some(
            (x) =>
              x.targetType === item.targetType &&
              String(x.targetId) === String(item.targetId),
          )
        )
          return q;
        const next = [...q, { ...item, _method: method }];
        queueRef.current = next;
        return next;
      });
      setMessage(`Đã thêm “${item.name}” vào hàng đợi thuyết minh.`);
      return;
    }
    speakingRef.current = true; // giữ chỗ trong lúc tải thuyết minh, tránh hai bài phát chồng nhau
    try {
      const dto = await loadNarration(item, language);
      const sameBase = (a, b) => String(a || "").toLowerCase().split("-")[0] === String(b || "").toLowerCase().split("-")[0];
      if (dto && !dto.translated && dto.sourceLanguage && !sameBase(dto.requestedLanguage, dto.sourceLanguage)) {
        throw new Error(`Không dịch được: ${dto.translationError || "dịch vụ dịch chưa sẵn sàng"}. Hãy kiểm tra dịch vụ dịch (LibreTranslate) của máy chủ.`);
      }
      const translated = dto && String(dto.text || "").trim()
        ? dto.text
        : await translate(textFor(item), language);
      const lang = languages.find((l) => l.code === language);
      const finish = () => {
        speakingRef.current = false;
        setPlaying(false);
        setIsPaused(false);
        setActiveNarration(null);
        if (skipCurrentRef.current) { skipCurrentRef.current = false; return; }
        const next = queueRef.current.shift();
        setQueue([...queueRef.current]);
        if (next)
          setTimeout(() => speak(next, next._method || "Hàng đợi"), 150);
        else setMessage("");
      };
      // Admin đã tải audio và ngôn ngữ nghe trùng ngôn ngữ bản thu -> phát audio; không được thì đọc bằng giọng tổng hợp.
      const playedFile = dto?.audioEnabled && dto?.audioUrl ? await playAudioFile(dto.audioUrl, item, finish) : false;
      if (!playedFile) {
        const utter = new SpeechSynthesisUtterance(translated);
        utter.lang = dto?.ttsKey || lang?.ttsKey || language;
        utter.rate = Number(dto?.speed || narrFor(item)?.speed || 1);
        utter.onstart = () => {
          speakingRef.current = true;
          setPlaying(true);
          setIsPaused(false);
          setActiveNarration(item);
        };
        utter.onend = finish;
        utter.onerror = finish;
        currentAudio.current = utter;
        speechSynthesis.speak(utter);
      }
      speakingRef.current = true;
      setPlaying(true);
      setActiveNarration(item);
      const payload = {
        date: new Date().toISOString(),
        targetType: item.targetType,
        targetId: item.targetId,
        name: item.name,
        language,
        method,
        playedAt: new Date().toISOString(),
      };
      try {
        await userApi.recordListen({
          targetType: item.targetType,
          targetId: item.targetId,
          language,
        });
        await userApi.addHistory(payload);
      } catch {}
      setHistory((h) => [...h, { ...payload, id: `local-${Date.now()}` }]);
      setListens((ls) => {
        const d = new Date().toISOString().slice(0, 10);
        const i = ls.findIndex(
          (x) =>
            x.date === d &&
            x.targetType === item.targetType &&
            String(x.targetId) === String(item.targetId) &&
            x.language === language,
        );
        if (i < 0) return [...ls, { ...payload, date: d, count: 1 }];
        return ls.map((x, j) =>
          j === i ? { ...x, count: Number(x.count || 0) + 1 } : x,
        );
      });
      setMessage(
        `Đang thuyết minh ${item.name} bằng ${lang?.name || language}${playedFile ? " (audio)" : ""}.`,
      );
    } catch (e) {
      speakingRef.current = false;
      setMessage(e.message || "Không thể phát thuyết minh.");
    }
  };
  const pauseNarration = () => {
    const a = audioEl.current;
    if (a) {
      if (a.paused) {
        a.play().catch(() => {});
        setIsPaused(false);
        setMessage("Đang tiếp tục thuyết minh.");
      } else {
        a.pause();
        setIsPaused(true);
        setMessage("Đã tạm dừng. Các bài tiếp theo sẽ chờ bài hiện tại phát xong.");
      }
      return;
    }
    if (!("speechSynthesis" in window)) return;
    if (speechSynthesis.paused) {
      speechSynthesis.resume();
      setIsPaused(false);
      setMessage("Đang tiếp tục thuyết minh.");
    } else {
      speechSynthesis.pause();
      setIsPaused(true);
      setMessage("Đã tạm dừng. Các bài tiếp theo sẽ chờ bài hiện tại phát xong.");
    }
  };

  const stopNarration = (skip = false) => {
    if (skip) skipCurrentRef.current = true;
    if (audioEl.current) {
      const a = audioEl.current;
      audioEl.current = null;
      a.onended = null;
      a.onerror = null;
      a.pause();
    }
    speechSynthesis.cancel();
    speakingRef.current = false;
    setPlaying(false);
    setIsPaused(false);
    setActiveNarration(null);
    if (skip) {
      const next = queueRef.current.shift();
      setQueue([...queueRef.current]);
      setTimeout(() => {
        skipCurrentRef.current = false;
        if (next) speak(next, next._method || "Hàng đợi");
        else setMessage("");
      }, 120);
    } else {
      queueRef.current = [];
      setQueue([]);
      setMessage("");
    }
  };
  const toggleBluetooth = async () => {
    if (bluetoothOn) {
      bluetoothDeviceRef.current?.gatt?.disconnect();
      bluetoothDeviceRef.current = null;
      setBluetoothOn(false);
      setMessage("Đã tắt kết nối Bluetooth.");
      return;
    }
    if (!navigator.bluetooth) {
      setMessage(
        "Trình duyệt này không hỗ trợ Web Bluetooth. Hãy dùng Chrome/Edge trên HTTPS hoặc localhost.",
      );
      return;
    }
    try {
      const device = await navigator.bluetooth.requestDevice({
        acceptAllDevices: true,
        optionalServices: ["battery_service", "generic_access"],
      });
      bluetoothDeviceRef.current = device;
      device.addEventListener("gattserverdisconnected", () => {
        setBluetoothOn(false);
      });
      if (device.gatt) {
        try {
          await device.gatt.connect();
        } catch {}
      }
      setBluetoothOn(true);
      const name = (device.name || "").toLowerCase();
      const n = narrations.find(
        (x) =>
          x.bluetooth?.enabled &&
          ((x.bluetooth.deviceName || "").toLowerCase() ===
            (device.name || "").toLowerCase() ||
            (x.bluetooth.mac || "").toLowerCase() ===
              (device.id || "").toLowerCase()),
      );
      const item =
        n &&
        allItems.find(
          (x) =>
            x.targetType === n.targetType &&
            String(x.targetId) === String(n.targetId),
        );
      setMessage(
        `Đã chọn thiết bị Bluetooth: ${device.name || "Thiết bị không có tên"}. ${item ? "Đang mở thuyết minh phù hợp." : "Chưa có nội dung được gắn với tên thiết bị này."}`,
      );
      if (item) await speak(item, "Bluetooth");
    } catch (e) {
      setMessage(
        e.name === "NotFoundError"
          ? "Không chọn thiết bị Bluetooth nào."
          : `Không kết nối được Bluetooth: ${e.message || e}`,
      );
    }
  };
  const toggleLike = async (item) => {
    const old = likes.find(
      (x) =>
        x.targetType === item.targetType &&
        String(x.targetId) === String(item.targetId),
    );
    if (old) {
      try {
        await userApi.removeLike(old);
      } catch {}
      setLikes((v) => v.filter((x) => x.id !== old.id));
    } else {
      const row = {
        targetType: item.targetType,
        targetId: item.targetId,
        name: item.name,
        createdAt: new Date().toISOString(),
      };
      try {
        await userApi.addLike(row);
        setLikes((v) => [...v, { ...row, id: `${row.targetType}:${row.targetId}` }]);
      } catch {
        setLikes((v) => [...v, { ...row, id: `local-${Date.now()}` }]);
      }
    }
  };
  const likedItems = likes
    .map((l) =>
      allItems.find(
        (i) =>
          i.targetType === l.targetType &&
          String(i.targetId) === String(l.targetId),
      ),
    )
    .filter(Boolean);
  const historyItems = history
    .slice()
    .sort(
      (a, b) => new Date(b.playedAt || b.date) - new Date(a.playedAt || a.date),
    )
    .map((h) => ({
      ...h,
      item: allItems.find(
        (i) =>
          i.targetType === h.targetType &&
          String(i.targetId) === String(h.targetId),
      ),
    }))
    .filter((h) => h.item);

  useEffect(() => {
    if (tab !== "home" || !mapNode.current) return;
    const currentContainer = mapNode.current;

    // "Xem tất cả" tháo phần bản đồ khỏi DOM. Khi quay lại, Leaflet phải
    // được gắn với DOM node mới thay vì giữ tham chiếu đến node cũ đã bị tháo.
    if (mapRef.current && mapContainerRef.current !== currentContainer) {
      mapRef.current.remove();
      mapRef.current = null;
      mapContainerRef.current = null;
    }
    if (!mapRef.current) {
      mapRef.current = L.map(currentContainer).setView([10.7771, 106.6954], 13);
      mapContainerRef.current = currentContainer;
      L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        attribution: "© OpenStreetMap",
      }).addTo(mapRef.current);
    }
    const map = mapRef.current;
    const handlePopupOpen = (event) => {
      const button = event.popup?.getElement?.()?.querySelector("[data-open-site]");
      if (!button) return;
      button.onclick = () => {
        const id = button.getAttribute("data-open-site");
        const item = allItems.find((x) => x.targetType === "site" && String(x.id).replace(/[^a-zA-Z0-9_-]/g, "") === id);
        if (item) openDetail(item);
        map.closePopup();
      };
    };
    map.on("popupopen", handlePopupOpen);
    map.eachLayer((layer) => {
      if (layer instanceof L.Marker || layer instanceof L.Circle)
        map.removeLayer(layer);
    });
    sites.forEach((site) => {
      if (
        Number.isFinite(Number(site.latitude)) &&
        Number.isFinite(Number(site.longitude))
      ) {
        L.marker([Number(site.latitude), Number(site.longitude)])
          .addTo(map)
          .bindPopup(
            `<b>${String(site.name || "").replace(/[<>]/g, "")}</b><br/>${String(site.city || "").replace(/[<>]/g, "")}<br/><button type="button" data-open-site="${String(site.id).replace(/[^a-zA-Z0-9_-]/g, "")}" style="margin-top:6px;padding:5px 12px;border:0;border-radius:6px;background:#2563eb;color:white;cursor:pointer">${ui("view")}</button>`,
          )
          .bindTooltip(String(site.name || "Địa điểm").replace(/[<>]/g, ""), {
            permanent: true,
            direction: "top",
            offset: [0, -10],
            className: "site-name-tooltip",
          });
        const n = narrations.find(
          (x) =>
            x.targetType === "site" && String(x.targetId) === String(site.id),
        );
        if (n?.gps?.enabled)
          L.circle(
            [
              Number(n.gps.latitude || site.latitude),
              Number(n.gps.longitude || site.longitude),
            ],
            { radius: Number(n.gps.radius || 50) },
          ).addTo(map);
      }
    });
    // Dấu vị trí hiện tại của người dùng, khác màu với các địa điểm tham quan.
    if (
      position &&
      Number.isFinite(position.lat) &&
      Number.isFinite(position.lng)
    ) {
      const userIcon = L.divIcon({
        className: "user-location-marker",
        html: '<div style="width:22px;height:22px;border-radius:50%;background:#2563eb;border:3px solid #fff;box-shadow:0 0 0 7px rgba(37,99,235,.22),0 2px 8px rgba(0,0,0,.35)"></div>',
        iconSize: [22, 22],
        iconAnchor: [11, 11],
      });
      L.marker([position.lat, position.lng], {
        icon: userIcon,
        zIndexOffset: 1000,
      })
        .addTo(map)
        .bindPopup(`<b>${ui("currentLocation")}</b>`);
    }
    setTimeout(() => map.invalidateSize(), 100);
    return () => { map.off("popupopen", handlePopupOpen); };
  }, [tab, sites, narrations, position, allItems, language]);

  const enableGps = () => {
    // Trình duyệt hiện đại chỉ cấp Geolocation trong secure context (HTTPS hoặc localhost).
    // HTTP://IP-LAN không phải secure context trên phần lớn trình duyệt; không thể tự bỏ qua hạn chế này.
    
    if (gpsWatch.current !== null) {
      navigator.geolocation?.clearWatch(gpsWatch.current);
      gpsWatch.current = null;
      setGpsOn(false);
      setMessage("Đã tắt GPS tự động.");
      return;
    }
    if (!navigator.geolocation) {
      setMessage("Trình duyệt không hỗ trợ GPS.");
      return;
    }
    // Không tạo nhiều GPS watcher nếu người dùng bấm nút nhiều lần.
    if (gpsWatch.current !== null) {
      if (position && mapRef.current)
        mapRef.current.setView([position.lat, position.lng], 16, {
          animate: true,
        });
      return;
    }
    setMessage("Trình duyệt sẽ hỏi quyền vị trí. Hãy chọn Cho phép.");
    gpsWatch.current = navigator.geolocation.watchPosition(
      async (pos) => {
        const p = { lat: pos.coords.latitude, lng: pos.coords.longitude };
        setPosition(p);
        setGpsOn(true);
        for (const site of sites) {
          const n = narrations.find(
            (x) =>
              x.targetType === "site" &&
              String(x.targetId) === String(site.id) &&
              x.gps?.enabled,
          );
          if (!n) continue;
          const d = distance(
            p.lat,
            p.lng,
            Number(n.gps.latitude || site.latitude),
            Number(n.gps.longitude || site.longitude),
          );
          const key = `site-${site.id}`;
          if (d <= Number(n.gps.radius || 50) && !entered.current.has(key)) {
            entered.current.add(key);
            await speak(
              { ...site, targetType: "site", targetId: site.id },
              "GPS",
            );
          }
          if (d > Number(n.gps.radius || 50) + 20) entered.current.delete(key);
        }
      },
      (err) => {
        if (err.code === 1) {
          // Bị từ chối quyền: dừng hẳn, giải phóng watcher để lần bấm sau thử lại từ đầu.
          if (gpsWatch.current !== null) navigator.geolocation?.clearWatch(gpsWatch.current);
          gpsWatch.current = null;
          setGpsOn(false);
          setMessage("Bạn chưa cấp quyền vị trí. Hãy bật quyền Location cho trình duyệt.");
          return;
        }
        // Mất tín hiệu tạm thời (code 2) hoặc quá thời gian (code 3): trình duyệt vẫn giữ watcher và sẽ
        // báo lại khi có vị trí. Không được bỏ tay cầm watcher, nếu không nút "Tắt GPS" sẽ không còn tắt
        // được mà chỉ tạo thêm một watcher thứ hai.
        setMessage("Tạm thời chưa lấy được vị trí GPS, đang thử lại...");
      },
      { enableHighAccuracy: true, maximumAge: 1500, timeout: 15000 },
    );
    // Nút phản ánh việc đang theo dõi vị trí (watcher còn sống), không đợi tới lần có vị trí đầu tiên.
    setGpsOn(true);
  };
  const locateUser = () => {
    if (!position) {
      enableGps();
      return;
    }
    if (mapRef.current) {
      mapRef.current.setView([position.lat, position.lng], 16, {
        animate: true,
      });
      // Mở popup vị trí sau khi bản đồ chuyển đến vị trí hiện tại.
      mapRef.current.eachLayer((layer) => {
        if (
          layer instanceof L.Marker &&
          layer.getPopup()?.getContent() === `<b>${ui("currentLocation")}</b>`
        )
          layer.openPopup();
      });
    }
  };
  useEffect(
    () => () => {
      if (gpsWatch.current !== null)
        navigator.geolocation?.clearWatch(gpsWatch.current);
      stopCamera();
      speechSynthesis.cancel();
      audioEl.current?.pause();
      if (mapRef.current) mapRef.current.remove();
      mapRef.current = null;
      mapContainerRef.current = null;
    },
    [],
  );
  function stopCamera() {
    setCameraOn(false);
    setScanning(false);
    if (scanTimer.current) clearTimeout(scanTimer.current);
    streamRef.current?.getTracks().forEach((t) => t.stop());
    streamRef.current = null;
  }
  const startCamera = async () => {
    try {
      if (!window.isSecureContext) {
        setMessage(
          "Camera chỉ hoạt động trên HTTPS hoặc localhost. Hãy mở ứng dụng bằng localhost/HTTPS, không dùng địa chỉ HTTP thường.",
        );
        return;
      }
      if (!("BarcodeDetector" in window)) {
        setMessage(
          "Trình duyệt này chưa có BarcodeDetector. Hãy cập nhật Chrome/Edge hoặc quét QR bằng ứng dụng camera rồi mở liên kết QR.",
        );
        return;
      }
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: { ideal: "environment" } },
        audio: false,
      });
      streamRef.current = stream;
      setCameraOn(true);
      setScanning(true);
      setTimeout(() => {
        if (videoRef.current) {
          videoRef.current.srcObject = stream;
          videoRef.current.play();
          scanLoop();
        }
      }, 100);
    } catch {
      setMessage(
        "Không mở được camera. Hãy cấp quyền camera và dùng HTTPS hoặc localhost.",
      );
    }
  };
  async function scanLoop() {
    if (!videoRef.current || !streamRef.current) return;
    try {
      const detector = new BarcodeDetector({ formats: ["qr_code"] });
      const codes = await detector.detect(videoRef.current);
      if (codes.length) {
        const raw = codes[0].rawValue;
        stopCamera();
        let item = null;
        try {
          const qrUrl = new URL(raw);
          const type = qrUrl.searchParams.get("type"),
            id = qrUrl.searchParams.get("id"),
            lang = qrUrl.searchParams.get("lang");
          if (type && id)
            item = allItems.find(
              (x) => x.targetType === type && String(x.targetId) === String(id),
            );
          if (lang && languages.some((l) => l.code === lang)) {
            setLanguage(lang);
            localStorage.setItem("guideLanguage", lang);
          }
        } catch {}
        if (!item)
          item = allItems.find(
            (x) =>
              String(narrFor(x)?.qr?.code || "") === raw ||
              String(x.id) === raw ||
              String(x.name).toLowerCase() === raw.toLowerCase(),
          );
        if (item) {
          openDetail(item);
          await speak(item, "QR code");
        } else {
          try {
            const translated = await translate(raw, language);
            const utter = new SpeechSynthesisUtterance(translated);
            utter.lang =
              languages.find((l) => l.code === language)?.ttsKey || language;
            speechSynthesis.speak(utter);
            setMessage("Đã phát nội dung đọc được từ QR.");
          } catch (e) {
            setMessage(e.message || `Nội dung QR: ${raw}`);
          }
        }
        return;
      }
    } catch {}
    scanTimer.current = setTimeout(scanLoop, 350);
  }
  const filtered = allItems.filter((x) => {
    const typeMatches = searchType === "all" || x.targetType === searchType;
    const searchable = [
      x.name, x.city, x.address, x.siteName, x.description,
      translatedField(x, "name", x.name),
      translatedField(x, "city", x.city),
      translatedField(x, "address", x.address),
      translatedField(x, "siteName", x.siteName),
      translatedField(x, "description", x.description),
    ].filter(Boolean).join(" ").toLocaleLowerCase();
    const queryMatches = searchable.includes(query.toLocaleLowerCase());
    const citySearch = `${x.city || ""} ${translatedField(x, "city", x.city)}`.toLocaleLowerCase();
    const siteSearch = `${x.siteName || ""} ${translatedField(x, "siteName", x.siteName)}`.toLocaleLowerCase();
    const cityMatches = !cityFilter || citySearch.includes(cityFilter.toLocaleLowerCase());
    const siteMatches = !siteFilter || siteSearch.includes(siteFilter.toLocaleLowerCase());
    return typeMatches && queryMatches && cityMatches && (x.targetType !== "artifact" || siteMatches);
  });
  const cityGroups = Object.entries(
    filtered
      .filter((x) => x.targetType === "site")
      .reduce((o, x) => {
        (o[x.city || "Khác"] ||= []).push(x);
        return o;
      }, {}),
  );
  const expired = view.reason === "expired";
  if (view.status === "checking" || (view.status === "allowed" && loading))
    return (
      <div className="user-state">
        <LoaderCircle className="user-spinner" />
        <p>{ui("loading")}</p>
      </div>
    );
  if (view.status !== "allowed")
    return (
      <div className="user-state">
        <h2>
          {view.status === "offline"
            ? "Không kết nối được máy chủ"
            : "Bạn không được phép truy cập"}
        </h2>
        <p>
          {expired
            ? "Thời hạn truy cập thiết bị đã hết. Hãy quét QR mới hoặc nhờ admin gia hạn."
            : view.reason === "no-token"
              ? "Bạn cần quét QR đăng nhập do quản trị viên cung cấp."
              : view.reason === "not-lan"
                ? "Hãy kết nối cùng mạng Wi-Fi/LAN với máy chủ."
                : view.reason === "no-session" || view.reason === "invalid-session"
                  ? "Quyền truy cập của thiết bị đã bị thu hồi. Hãy quét lại QR mới."
                  : view.reason === "app-limit"
                    ? "Đã đạt giới hạn số thiết bị App đăng nhập cùng lúc."
                    : ui("invalidQr")}
        </p>
        <button
          className="user-primary-button"
          onClick={() => location.reload()}
        >
          Thử lại
        </button>
      </div>
    );

  if (detail) {
    const n = narrFor(detail),
      isArtifact = detail.targetType === "artifact";
    const children = isArtifact
      ? []
      : artifacts.filter((a) => String(a.siteId) === String(detail.id));
    return (
      <main className="place-detail-screen">
        <header className="place-detail-header">
          <button
            className="place-detail-back"
            onClick={() => {
              setDetail(null);
              setTab("home");
            }}
          >
            <MapIcon size={20} />
          </button>
          <button
            className="place-detail-audio"
            onClick={() => speak(detail)}
            title={ui("listen")}
          >
            {playing ? <Pause size={23} /> : <Volume2 size={23} />}
          </button>
          {activeNarration && (
            <div style={{ display: "flex", gap: 6, alignItems: "center", marginLeft: "auto" }}>
              <button className="map-action" onClick={pauseNarration}>{isPaused ? ui("resume") : ui("pause")}</button>
              <button className="map-action" onClick={() => stopNarration(true)}>{ui("skip")}</button>
            </div>
          )}
        </header>
        {message && <p className="place-detail-audio-note">{message}</p>}
        <section className="place-detail-main">
          <div className="place-detail-overview">
            <img
              className="place-detail-image"
              src={detail.image || imageFallback(detail.name)}
              alt={detail.name}
            />
            <div className="place-detail-facts">
              <h1>{translatedField(detail, "name", detail.name)}</h1>
              <p>
                <b>{ui("addressLabel")}</b> {translatedField(detail, "address", detail.address || "Đang cập nhật")}
              </p>
              <p>
                <b>{ui("cityLabel")}</b> {translatedField(detail, "address", detail.city || "Đang cập nhật")}
              </p>
              <p>
                <b>{ui("plays")}</b> {detail.totalListens || 0}
              </p>
            </div>
          </div>
          <section className="place-detail-description">
            <h2>{ui("description")}</h2>
            <p>{translatedField(detail, "narration", textFor(detail))}</p>
          </section>
          <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
            <button
              className="place-detail-map-button"
              onClick={() => toggleLike(detail)}
            >
              <Heart size={16} />{" "}
              {likes.some(
                (x) =>
                  x.targetType === detail.targetType &&
                  String(x.targetId) === String(detail.targetId),
              )
                ? ui("removeLike")
                : ui("liked")}
            </button>
            <button
              className="place-detail-map-button"
              onClick={() => speak(detail)}
            >
              <Volume2 size={16} /> {ui("narration")}
            </button>
          </div>
          {!isArtifact && children.length > 0 && (
            <section className="tour-section">
              <h2>{ui("atLocation")}</h2>
              <div className="artifact-grid">
                {children.map((a) => (
                  <article className="artifact-card" key={a.id}>
                    <button
                      className="artifact-image"
                      onClick={() =>
                        openDetail(
                          allItems.find(
                            (x) =>
                              x.targetType === "artifact" &&
                              String(x.id) === String(a.id),
                          ),
                        )
                      }
                    >
                      <img src={a.img || imageFallback(a.name)} alt={a.name} />
                    </button>
                    <div className="artifact-body">
                      <h3>{translatedField(a, "name", a.name)}</h3>
                      <p>{translatedField(a, "description", a.description)}</p>
                      <button
                        className="artifact-listen"
                        onClick={() =>
                          speak(
                            allItems.find(
                              (x) =>
                                x.targetType === "artifact" &&
                                String(x.id) === String(a.id),
                            ),
                          )
                        }
                      >
                        <Volume2 size={14} /> {ui("narration")}
                      </button>
                    </div>
                  </article>
                ))}
              </div>
            </section>
          )}
        </section>
        <nav className="tour-bottom-nav">
          <button
            className="nav-item"
            onClick={() => {
              setDetail(null);
              setTab("home");
            }}
          >
            <Home />
            <span>{ui("explore")}</span>
          </button>
          <button
            className="nav-item"
            onClick={() => {
              setDetail(null);
              setTab("history");
            }}
          >
            <History />
            <span>{ui("history")}</span>
          </button>
          <button
            className="nav-item"
            onClick={() => {
              setDetail(null);
              setTab("like");
            }}
          >
            <Heart />
            <span>{ui("likes")}</span>
          </button>
        </nav>
      </main>
    );
  }
  if (cameraOn)
    return (
      <main className="location-picker-screen">
        <header className="location-picker-header">
          <button className="location-picker-back" onClick={stopCamera}>
            <X />
          </button>
          <h2>{ui("qrTitle")}</h2>
        </header>
        <div style={{ padding: 20 }}>
          <video
            ref={videoRef}
            style={{
              width: "100%",
              maxHeight: "55vh",
              borderRadius: 16,
              background: "#111",
            }}
            playsInline
            muted
          />
          <p>{ui("qrHint")}</p>
          <button className="find-button" onClick={stopCamera}>
            {ui("closeCamera")}
          </button>
        </div>
      </main>
    );

  return (
    <main className="tour-app">
      {!connected && (
        <div className="user-connection-banner">
          {ui("lostConnection")}
        </div>
      )}
      {message && (
        <div className="tour-message" role="status">
          <span>{message}</span>
          <button onClick={() => setMessage("")}>×</button>
        </div>
      )}
      {tab === "home" && (
        <>
          <section className="tour-map">
            <div className="map-top-controls">
              <div className="map-language">
                <Languages size={15} />
                <select
                  aria-label={ui("languageLabel")}
                  value={language}
                  onChange={(e) => {
                    setLanguage(e.target.value);
                    localStorage.setItem("guideLanguage", e.target.value);
                  }}
                >
                  {languages.map((l) => (
                    <option key={l.code} value={l.code}>
                      {l.name}
                    </option>
                  ))}
                </select>
              </div>
              <div className="map-actions">
                <button
                  className={gpsOn ? "map-action active" : "map-action"}
                  onClick={enableGps}
                >
                  <Navigation size={14} />
                  {gpsOn ? ui("gpsOn") : ui("gpsOff")}
                </button>
                <button
                  className={bluetoothOn ? "map-action active" : "map-action"}
                  onClick={toggleBluetooth}
                >
                  <Bluetooth size={14} />
                  {bluetoothOn ? ui("bluetoothOn") : ui("bluetoothOff")}
                </button>
                <button className="map-action" onClick={locateUser}>
                  <MapPin size={14} /> {ui("myLocation")}
                </button>
                <button className="map-action" onClick={startCamera}>
                  <Camera size={14} /> {ui("scanQr")}
                </button>
              </div>
            </div>
            <div
              ref={mapNode}
              style={{ width: "100%", height: "100%", minHeight: 330 }}
            />
          </section>
          {activeNarration && (
            <section className="active-narration">
              <div>
                <strong>{ui("currentlyNarrating")}: {translatedField(activeNarration, "name", activeNarration.name)}</strong>
                <span>
                  {queue.length ? ` · ${queue.length} ${ui("queued")}` : ""}
                </span>
              </div>
              <button onClick={pauseNarration}>
                {isPaused ? <Play size={14} /> : <Pause size={14} />} {isPaused ? ui("resume") : ui("pause")}
              </button>
              <button onClick={() => stopNarration(true)}>{ui("skip")}</button>
            </section>
          )}
          <section className="tour-content">
            <section className="location-search">
              <h2>{ui("findLocation")}</h2>
              <div className="location-input">
                <Search size={17} />
                <input
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  placeholder={ui("search")}
                />
              </div>
              <button className="find-button" onClick={() => setTab("search")}>
                {ui("findNow")}
              </button>
              {position && (
                <p className="current-location">
                  <MapPin size={14} />
                  {position.lat.toFixed(5)}, {position.lng.toFixed(5)}
                </p>
              )}
            </section>
            <section className="tour-section">
              <div className="tour-section-heading" style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 12 }}><h2>{ui("siteHeading")}</h2><button type="button" onClick={() => { setSearchType("site"); setQuery(""); setCityFilter(""); setSiteFilter(""); setTab("search"); }}>{ui("allSites")} →</button></div>
              <div className="tour-horizontal-list">
                {popularSites.map((item) => (
                  <article className="tour-card" key={item.id}>
                    <button
                      className="tour-card-image"
                      onClick={() => openDetail(item)}
                    >
                      <img src={item.image} alt={item.name} />
                    </button>
                    <div className="tour-card-body">
                      <h3>{translatedField(item, "name", item.name)}</h3>
                      <p className="tour-category">{translatedField(item, "city", item.city)}</p>
                      <p>♧ {item.totalListens} {ui("listenCount")}</p>
                      <button
                        className="tour-favorite"
                        onClick={() => toggleLike(item)}
                      >
                        <Heart
                          fill={
                            likes.some(
                              (x) =>
                                x.targetType === "site" &&
                                String(x.targetId) === String(item.id),
                            )
                              ? "#ef4444"
                              : "none"
                          }
                        />
                      </button>
                    </div>
                  </article>
                ))}
              </div>
            </section>
            <section className="tour-section artifacts-section">
              <div className="tour-section-heading" style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 12 }}><h2>{ui("artifactHeading")}</h2><button type="button" onClick={() => { setSearchType("artifact"); setQuery(""); setCityFilter(""); setSiteFilter(""); setTab("search"); }}>{ui("allArtifacts")} →</button></div>
              <div className="tour-horizontal-list artifact-horizontal-list" style={{ display: "flex", overflowX: "auto", gap: 14, paddingBottom: 8 }}>
                {popularArtifacts.map((item) => (
                  <article className="artifact-card" key={item.id}>
                    <button
                      className="artifact-image"
                      onClick={() => openDetail(item)}
                    >
                      <img src={item.image} alt={item.name} />
                    </button>
                    <div className="artifact-body">
                      <h3>{translatedField(item, "name", item.name)}</h3>
                      <p>{translatedField(item, "siteName", item.siteName)}</p>
                      <p>{item.totalListens} {ui("listenCount")}</p>
                      <button
                        className="artifact-listen"
                        onClick={() => speak(item)}
                      >
                        <Volume2 size={14} /> {ui("narration")}
                      </button>
                      <button
                        className="artifact-favorite"
                        onClick={() => toggleLike(item)}
                      >
                        <Heart
                          fill={
                            likes.some(
                              (x) =>
                                x.targetType === "artifact" &&
                                String(x.targetId) === String(item.id),
                            )
                              ? "#ef4444"
                              : "none"
                          }
                        />
                      </button>
                    </div>
                  </article>
                ))}
              </div>
            </section>
          </section>
        </>
      )}
      {tab === "search" && (
        <section className="tour-content">
          <button className="place-detail-back" onClick={() => setTab("home")}>
            ← {ui("back")}
          </button>
          <h1>{searchType === "site" ? ui("allPlaces") : searchType === "artifact" ? ui("allObjects") : ui("searchResults")}</h1>
          <div className="location-input" style={{ marginBottom: 10 }}>
            <Search size={17} />
            <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder={searchType === "artifact" ? ui("searchArtifact") : ui("searchAll")} />
          </div>
          <div className="search-filter-row">
            <input value={cityFilter} onChange={(e) => setCityFilter(e.target.value)} placeholder={ui("filterCity")} />
            {searchType === "artifact" && <input value={siteFilter} onChange={(e) => setSiteFilter(e.target.value)} placeholder={ui("filterSite")} />}
            <button type="button" onClick={() => { setQuery(""); setCityFilter(""); setSiteFilter(""); }}>{ui("clearFilter")}</button>
            <button type="button" onClick={() => setSearchType("all")}>{ui("all")}</button>
          </div>
          {searchType !== "artifact" && cityGroups.map(([city, items]) => (
            <section className="tour-section" key={city}>
              <h2>{city}</h2>
              {items.map((item) => (
                <button
                  className="history-card"
                  key={item.id}
                  onClick={() => openDetail(item)}
                >
                  <img
                    className="history-card-image"
                    src={item.image}
                    alt={item.name}
                  />
                  <span className="history-card-content">
                    <strong>{translatedField(item, "name", item.name)}</strong>
                    <span>{translatedField(item, "address", item.address || item.city)}</span>
                    <span>{item.totalListens} {ui("listenCount")}</span>
                  </span>
                </button>
              ))}
            </section>
          ))}
          {searchType !== "site" && <h2>{ui("artifacts")}</h2>}
          {searchType !== "site" && filtered
            .filter((x) => x.targetType === "artifact")
            .map((item) => (
              <button
                className="history-card"
                key={item.id}
                onClick={() => openDetail(item)}
              >
                <img
                  className="history-card-image"
                  src={item.image}
                  alt={item.name}
                />
                <span className="history-card-content">
                  <strong>{translatedField(item, "name", item.name)}</strong>
                  <span>{translatedField(item, "siteName", item.siteName)}</span>
                </span>
              </button>
            ))}
        </section>
      )}
      {tab === "history" && (
        <section className="tour-content history-page">
          <h1>{ui("history")}</h1>
          {historyItems.length === 0 ? (
            <p>{ui("historyEmpty")}</p>
          ) : (
            historyItems.map((h) => (
              <button
                className="history-card"
                key={h.id}
                onClick={() => openDetail(h.item)}
              >
                <img
                  className="history-card-image"
                  src={h.item.image}
                  alt={h.item.name}
                />
                <span className="history-card-content">
                  <strong>{h.item.name}</strong>
                  <span>
                    {h.method || ui("methodNarration")} ·{" "}
                    {timeAgo(h.playedAt || h.date, language)}
                  </span>
                  <span>{translatedField(h.item, "city", h.item.city)}</span>
                </span>
              </button>
            ))
          )}
        </section>
      )}
      {tab === "like" && (
        <section className="tour-content liked-page">
          <h1>{ui("likes")}</h1>
          {likedItems.length === 0 ? (
            <p>{ui("likesEmpty")}</p>
          ) : (
            likedItems.map((item) => (
              <button
                className="history-card"
                key={`${item.targetType}-${item.id}`}
                onClick={() => openDetail(item)}
              >
                <img
                  className="history-card-image"
                  src={item.image}
                  alt={item.name}
                />
                <span className="history-card-content">
                  <strong>{translatedField(item, "name", item.name)}</strong>
                  <span>{translatedField(item, "address", item.address || item.siteName)}</span>
                  <span>{item.totalListens} {ui("listenCount")}</span>
                </span>
              </button>
            ))
          )}
        </section>
      )}

      <nav className="tour-bottom-nav">
        <button
          className={tab === "home" ? "nav-item selected" : "nav-item"}
          onClick={() => setTab("home")}
        >
          <Home />
          <span>{ui("explore")}</span>
        </button>
        <button
          className={tab === "history" ? "nav-item selected" : "nav-item"}
          onClick={() => setTab("history")}
        >
          <History />
          <span>{ui("history")}</span>
        </button>
        <button
          className={tab === "like" ? "nav-item selected" : "nav-item"}
          onClick={() => setTab("like")}
        >
          <Heart />
          <span>{ui("likes")}</span>
        </button>
      </nav>
    </main>
  );
}
