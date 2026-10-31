import React, { useEffect, useMemo, useRef, useState } from "react";
import {
  View,
  Image,
  Text,
  TextInput,
  TouchableOpacity,
  ScrollView,
  StyleSheet,
  ActivityIndicator,
  Alert,
  Platform,
  AppState,
} from "react-native";
import MapView, { Marker, Callout, Circle, Region } from "react-native-maps";
import * as Location from "expo-location";
import { CameraView, useCameraPermissions } from "expo-camera";
import * as Speech from "expo-speech";
import { createAudioPlayer, setAudioModeAsync, type AudioPlayer } from "expo-audio";
import AsyncStorage from "@react-native-async-storage/async-storage";
import * as userApi from "../lib/userApi";
import * as Ble from "../lib/ble";
import { BeaconTracker, GeofenceTracker, buildBeaconTargets, buildGeoTargets, targetKey } from "../lib/proximity";

type AnyRow = Record<string, any>;
const langs = [
  { code: "vi", name: "Tiếng Việt", voice: "vi-VN" },
  { code: "en", name: "English", voice: "en-US" },
  { code: "zh-CN", name: "中文", voice: "zh-CN" },
  { code: "ja", name: "日本語", voice: "ja-JP" },
  { code: "ko", name: "한국어", voice: "ko-KR" },
  { code: "fr", name: "Français", voice: "fr-FR" },
];
const same = (a: any, b: any) => String(a) === String(b);
const UI_TEXT: Record<string, Record<string, string>> = {
  vi: { gpsAuto: "GPS tự động", btAuto: "Bluetooth", language: "Ngôn ngữ", search: "Tìm địa điểm, hiện vật...", qr: "Quét QR", mapOn: "Bản đồ đang hiển thị", mapOff: "Bản đồ đang ẩn", hideMap: "Ẩn bản đồ", showMap: "Hiện bản đồ", myLocation: "Vị trí của tôi", listen: "Nghe thuyết minh", details: "Xem chi tiết", view: "Xem", close: "Đóng", pause: "Tạm dừng", resume: "Tiếp tục", skip: "Bỏ qua", places: "Địa điểm nổi bật", artifacts: "Hiện vật nổi bật", all: "Xem tất cả", searchPlaces: "Tìm tên địa điểm...", searchArtifacts: "Tìm tên hiện vật...", city: "Tìm theo thành phố...", containingSite: "Tìm theo địa điểm chứa hiện vật...", description: "Mô tả", address: "Địa chỉ", history: "Lịch sử thuyết minh", favorites: "Yêu thích", explore: "Khám phá", noResults: "Không tìm thấy nội dung phù hợp." },
  en: { gpsAuto: "Auto GPS", btAuto: "Bluetooth beacon", language: "Language", search: "Search places and artifacts...", qr: "Scan QR", mapOn: "Map is visible", mapOff: "Map is hidden", hideMap: "Hide map", showMap: "Show map", myLocation: "My location", listen: "Listen to narration", details: "View details", view: "View", close: "Close", pause: "Pause", resume: "Resume", skip: "Skip", places: "Featured places", artifacts: "Featured artifacts", all: "View all", searchPlaces: "Search place name...", searchArtifacts: "Search artifact name...", city: "Filter by city...", containingSite: "Filter by containing place...", description: "Description", address: "Address", history: "Narration history", favorites: "Favorites", explore: "Explore", noResults: "No matching content found." },
  "zh-CN": { gpsAuto: "自动定位", btAuto: "蓝牙信标", language: "语言", search: "搜索地点和文物...", qr: "扫描二维码", mapOn: "地图已显示", mapOff: "地图已隐藏", hideMap: "隐藏地图", showMap: "显示地图", myLocation: "我的位置", listen: "收听讲解", details: "查看详情", view: "查看", close: "关闭", pause: "暂停", resume: "继续", skip: "跳过", places: "推荐地点", artifacts: "推荐文物", all: "查看全部", searchPlaces: "搜索地点名称...", searchArtifacts: "搜索文物名称...", city: "按城市筛选...", containingSite: "按所属地点筛选...", description: "描述", address: "地址", history: "讲解历史", favorites: "收藏", explore: "探索", noResults: "没有找到匹配内容。" },
  ja: { gpsAuto: "自動GPS", btAuto: "Bluetoothビーコン", language: "言語", search: "場所・展示品を検索...", qr: "QRをスキャン", mapOn: "地図を表示中", mapOff: "地図を非表示", hideMap: "地図を隠す", showMap: "地図を表示", myLocation: "現在地", listen: "音声ガイド", details: "詳細を見る", view: "見る", close: "閉じる", pause: "一時停止", resume: "再開", skip: "スキップ", places: "おすすめの場所", artifacts: "おすすめの展示品", all: "すべて見る", searchPlaces: "場所名を検索...", searchArtifacts: "展示品名を検索...", city: "都市で絞り込む...", containingSite: "所属する場所で絞り込む...", description: "説明", address: "住所", history: "音声ガイド履歴", favorites: "お気に入り", explore: "探索", noResults: "該当する内容がありません。" },
  ko: { gpsAuto: "자동 GPS", btAuto: "블루투스 비콘", language: "언어", search: "장소 및 유물 검색...", qr: "QR 스캔", mapOn: "지도 표시 중", mapOff: "지도 숨김", hideMap: "지도 숨기기", showMap: "지도 표시", myLocation: "내 위치", listen: "해설 듣기", details: "상세 보기", view: "보기", close: "닫기", pause: "일시정지", resume: "계속", skip: "건너뛰기", places: "추천 장소", artifacts: "추천 유물", all: "모두 보기", searchPlaces: "장소 이름 검색...", searchArtifacts: "유물 이름 검색...", city: "도시별 검색...", containingSite: "소장 장소별 검색...", description: "설명", address: "주소", history: "해설 기록", favorites: "즐겨찾기", explore: "탐색", noResults: "일치하는 콘텐츠가 없습니다." },
  fr: { gpsAuto: "GPS auto", btAuto: "Balise Bluetooth", language: "Langue", search: "Rechercher lieux et objets...", qr: "Scanner QR", mapOn: "Carte visible", mapOff: "Carte masquée", hideMap: "Masquer la carte", showMap: "Afficher la carte", myLocation: "Ma position", listen: "Écouter le commentaire", details: "Voir les détails", view: "Voir", close: "Fermer", pause: "Pause", resume: "Reprendre", skip: "Passer", places: "Lieux à découvrir", artifacts: "Objets remarquables", all: "Tout afficher", searchPlaces: "Rechercher un lieu...", searchArtifacts: "Rechercher un objet...", city: "Filtrer par ville...", containingSite: "Filtrer par lieu associé...", description: "Description", address: "Adresse", history: "Historique audio", favorites: "Favoris", explore: "Explorer", noResults: "Aucun résultat correspondant." },
};
export default function Home() {
  const [sites, setSites] = useState<AnyRow[]>([]),
    [arts, setArts] = useState<AnyRow[]>([]),
    [narr, setNarr] = useState<AnyRow[]>([]),
    [languages, setLanguages] = useState<AnyRow[]>([]);
  const [tab, setTab] = useState("home"),
    [query, setQuery] = useState(""),
    [lang, setLang] = useState("vi"),
    [selected, setSelected] = useState<AnyRow | null>(null),
    [browseType, setBrowseType] = useState<"site" | "artifact">("site"),
    [browseQuery, setBrowseQuery] = useState(""),
    [browseCity, setBrowseCity] = useState(""),
    [browseSite, setBrowseSite] = useState(""),
    [loading, setLoading] = useState(true),
    [message, setMessage] = useState(""),
    [gps, setGps] = useState<any>(null),
    [qr, setQr] = useState(false),
    [permission, requestPermission] = useCameraPermissions(),
    [likes, setLikes] = useState<AnyRow[]>([]),
    [history, setHistory] = useState<AnyRow[]>([]);
  const [translated, setTranslated] = useState<Record<string, string>>({});
  const [translatedUi, setTranslatedUi] = useState<Record<string, string>>({});
  const [translatedLanguageNames, setTranslatedLanguageNames] = useState<Record<string, string>>({});
  const [showMap, setShowMap] = useState(true);
  const [isSpeaking, setIsSpeaking] = useState(false);
  const [isPaused, setIsPaused] = useState(false);
  const [audioQueue, setAudioQueue] = useState<AnyRow[]>([]);
  const [activeNarration, setActiveNarration] = useState<AnyRow | null>(null);
  const currentNarration = React.useRef<AnyRow | null>(null);
  const queueRef = React.useRef<AnyRow[]>([]);
  const processingRef = React.useRef(false);
  const audioPlayerRef = React.useRef<AudioPlayer | null>(null);
  useEffect(() => () => {
    const p = audioPlayerRef.current;
    audioPlayerRef.current = null;
    try { p?.remove(); } catch {}
  }, []);
  // Thuyết minh tự động: GPS liên tục (vào vùng) và Bluetooth beacon (đến gần).
  const [gpsAuto, setGpsAuto] = useState(false);
  const [btAuto, setBtAuto] = useState(false);
  const mapRef = useRef<MapView | null>(null);
  const watchRef = useRef<Location.LocationSubscription | null>(null);
  const bleScanRef = useRef<Ble.BleScan | null>(null);
  const geoRef = useRef(new GeofenceTracker());
  const beaconRef = useRef(new BeaconTracker());
  const firstFixRef = useRef(false);
  const stopAutoAll = () => {
    try { watchRef.current?.remove(); } catch {}
    watchRef.current = null;
    try { bleScanRef.current?.stop(); } catch {}
    bleScanRef.current = null;
    geoRef.current.reset();
    beaconRef.current.reset();
    setGpsAuto(false);
    setBtAuto(false);
  };
  useEffect(() => () => {
    try { watchRef.current?.remove(); } catch {}
    try { bleScanRef.current?.stop(); } catch {}
  }, []);
  const [authenticated, setAuthenticated] = useState(false);
  const [authLoading, setAuthLoading] = useState(true);
  const [loginQr, setLoginQr] = useState(false);
  const [loginError, setLoginError] = useState("");
  const [deviceId, setDeviceId] = useState("");
  const heartbeatTimer = React.useRef<ReturnType<typeof setInterval> | null>(null);
  const tx = (key: string) => UI_TEXT[lang]?.[key] || translatedUi[key] || UI_TEXT.vi[key] || key;
  const languageLabel = (language: AnyRow) => translatedLanguageNames[String(language.code)] || language.name || language.code;
  const refresh = async () => {
    try {
      const c = await userApi.loadCatalog();
      setSites(c.sites);
      setArts(c.artifacts);
      setNarr(c.narrations);
      setLanguages(c.languages);
      setLikes(c.likes);
      setHistory(c.history);
      setMessage("");
    } catch (e: any) {
      // 401/403 đã được xử lý ở onAccessLost (quay lại màn hình đăng nhập).
      if (e?.status === 401 || e?.status === 403) return;
      setMessage(
        "Không kết nối được máy chủ. Kiểm tra API: " +
          userApi.getApiBase() +
          " và bảo đảm điện thoại cùng Wi-Fi với máy tính. " +
          (e?.message || ""),
      );
    } finally {
      setLoading(false);
    }
  };
  const stopHeartbeat = () => {
    if (heartbeatTimer.current) clearInterval(heartbeatTimer.current);
    heartbeatTimer.current = null;
  };
  const beat = async () => {
    try {
      await userApi.heartbeat();
    } catch {
      // Mất mạng tạm thời thì bỏ qua; 401/403 đã được onAccessLost xử lý.
    }
  };
  const startHeartbeat = () => {
    stopHeartbeat();
    heartbeatTimer.current = setInterval(beat, 20000);
  };
  // Vào hệ thống bằng token QR (lần đầu) hoặc phiên đã lưu (mở lại app không cần quét lại).
  const connectDevice = async (tokenValue?: string) => {
    setAuthLoading(true);
    setLoginError("");
    const id = await userApi.ensureDeviceId();
    setDeviceId(id);
    const result = await userApi.joinSession(tokenValue, lang);
    setAuthLoading(false);
    if (!result.ok) {
      stopHeartbeat();
      setAuthenticated(false);
      setLoginError(
        result.reason === "offline"
          ? (result.message || "Không kết nối được máy chủ: " + userApi.getApiBase())
          : result.reason === "expired"
            ? "Mã truy cập đã hết hạn. Hãy quét QR đăng nhập mới."
            : result.reason === "not-lan"
              ? "Hãy kết nối cùng mạng Wi-Fi/LAN với máy chủ."
              : result.reason === "no-token"
                ? "Thiết bị chưa được cấp quyền. Hãy quét QR do Admin tạo."
                : result.message || "Thiết bị chưa được cấp quyền. Hãy quét QR do Admin tạo.",
      );
      return;
    }
    setAuthenticated(true);
    setLoginQr(false);
    setLoginError("");
    startHeartbeat();
    refresh();
  };
  useEffect(() => {
    let mounted = true;
    // Quyền bị thu hồi / hết hạn: server trả 401/403 ở bất kỳ lời gọi nào -> về màn hình đăng nhập.
    const offAccessLost = userApi.onAccessLost((reason) => {
      stopAutoAll();
      stopHeartbeat();
      setAuthenticated(false);
      setLoginError(
        reason === "expired"
          ? "Quyền truy cập đã hết hạn. Hãy quét QR đăng nhập mới."
          : "Quyền truy cập thiết bị đã bị thu hồi. Hãy quét QR đăng nhập mới.",
      );
      userApi.clearSession();
    });
    // Quay lại app từ nền: báo ngay để admin thấy thiết bị đang mở.
    const sub = AppState.addEventListener("change", (state) => {
      if (state === "active") beat();
    });
    (async () => {
      const stored = await userApi.loadStored();
      if (!mounted) return;
      if (stored.deviceId && stored.hasSession) {
        await connectDevice();
      } else {
        setAuthLoading(false);
      }
    })();
    return () => {
      mounted = false;
      offAccessLost();
      sub.remove();
      stopHeartbeat();
    };
  }, []);
  const items = useMemo<AnyRow[]>(
    () => [
      ...sites.map((x) => ({
        ...x,
        targetType: "site",
        targetId: x.id,
        displayAddress: x.address || x.city,
      })),
      ...arts.map((x) => ({
        ...x,
        targetType: "artifact",
        targetId: x.id,
        displayAddress: x.address,
        siteName: sites.find((s) => same(s.id, x.siteId))?.name,
      })),
    ],
    [sites, arts],
  );
  const getNarr = (item: AnyRow) =>
    narr.find((n) => n.targetType === item.targetType && same(n.targetId, item.targetId));
  const sourceTextFor = (item: AnyRow) => {
    const n = getNarr(item);
    return n?.useDescription === false ? n?.text || item.description || "" : item.description || n?.text || "";
  };
  const translatedField = (item: AnyRow, field: string, source: string) => lang === "vi" ? source : translated[`${item.targetType}:${item.targetId}:${field}:${lang}`] || source;
  // Tìm theo cả dữ liệu gốc lẫn nội dung đã dịch sang ngôn ngữ đang chọn.
  // Vì vậy người dùng có thể nhập tên tiếng Anh/Thái/... khi giao diện đang ở ngôn ngữ đó.
  const normalizeSearch = (value: any) => String(value ?? "").normalize("NFC").toLocaleLowerCase();
  const visible = items.filter((item) => {
    const searchable = [
      item.name,
      item.description,
      item.city,
      item.address,
      item.siteName,
      translatedField(item, "name", item.name || ""),
      translatedField(item, "description", item.description || ""),
      translatedField(item, "address", item.displayAddress || item.address || item.city || item.siteName || ""),
    ].map(normalizeSearch).join(" ");
    return normalizeSearch(searchable).includes(normalizeSearch(query));
  });
  const textFor = (item: AnyRow) => {
    const n = getNarr(item);
    const stored = n?.translations?.[lang] || n?.texts?.[lang] || (n?.language === lang ? n?.text : "");
    return stored || translatedField(item, "narration", sourceTextFor(item));
  };
  const translateGoogle = async (text: string, target: string) => {
    if (!text || target === "vi") return text;
    // TTS dùng locale như th-TH, nhưng Google Translate cần mã ngôn ngữ như th.
    // Ưu tiên dịch qua máy chủ (có cache DB, không phụ thuộc Internet của điện thoại).
    try { return await userApi.translateText(text, target.split("-")[0] === "zh" ? "zh-CN" : target.split("-")[0]); } catch (e: any) {
      if (e?.status === 401 || e?.status === 403) throw e;
    }
    const translateCode: Record<string, string> = { "th-TH": "th", "en-US": "en", "zh-CN": "zh-CN", "ja-JP": "ja", "ko-KR": "ko", "fr-FR": "fr", "vi-VN": "vi" };
    const targetCode = translateCode[target] || target.split("-")[0];
    const endpoint = `https://translate.googleapis.com/translate_a/single?client=gtx&sl=vi&tl=${encodeURIComponent(targetCode)}&dt=t&q=${encodeURIComponent(text)}`;
    const response = await fetch(endpoint);
    if (!response.ok) throw new Error("Google Translate không khả dụng");
    const data = await response.json();
    const result = (data[0] || []).map((part: any[]) => part[0] || "").join("");
    if (!result) throw new Error("Không nhận được bản dịch");
    return result;
  };
  // Tự dịch các nhãn giao diện và tên ngôn ngữ khi API có ngôn ngữ mới.
  // Các ngôn ngữ đã có bản dịch thủ công trong UI_TEXT vẫn ưu tiên bản dịch đó.
  useEffect(() => {
    let alive = true;
    (async () => {
      if (lang === "vi") {
        setTranslatedUi({});
        setTranslatedLanguageNames({});
        return;
      }
      const uiJobs: Record<string, string> = {};
      const nameJobs: Record<string, string> = {};
      const uiSource = UI_TEXT.vi;
      for (const [key, source] of Object.entries(uiSource)) {
        if (UI_TEXT[lang]?.[key]) continue;
        try { uiJobs[key] = await translateGoogle(source, lang); } catch { /* dùng tiếng Việt dự phòng */ }
      }
      for (const language of (languages.length ? languages : langs)) {
        const code = String(language.code || "");
        const sourceName = String(language.name || code);
        try {
          // Tên trong admin đang được lưu bằng tiếng Việt, nên dịch về ngôn ngữ đang chọn.
          nameJobs[code] = code === lang ? await translateGoogle(sourceName, lang) : await translateGoogle(sourceName, lang);
        } catch { nameJobs[code] = sourceName; }
      }
      if (alive) {
        setTranslatedUi(uiJobs);
        setTranslatedLanguageNames(nameJobs);
      }
    })();
    return () => { alive = false; };
  }, [lang, languages]);

  useEffect(() => {
    let alive = true;
    (async () => {
      const jobs: Record<string, string> = {};
      for (const item of items) {
        const fields: Record<string, string> = { name: item.name || "", description: item.description || "", address: item.displayAddress || item.address || item.city || "", narration: sourceTextFor(item) };
        for (const [field, source] of Object.entries(fields)) {
          const key = `${item.targetType}:${item.targetId}:${field}:${lang}`;
          if (lang === "vi") jobs[key] = source;
          else if (source) {
            try { jobs[key] = await translateGoogle(source, lang); } catch { jobs[key] = source; }
          }
        }
      }
      if (alive) setTranslated((prev) => ({ ...prev, ...jobs }));
    })();
    return () => { alive = false; };
  }, [items, lang, narr]);
  const stopAudioFile = () => {
    const p = audioPlayerRef.current;
    audioPlayerRef.current = null;
    if (p) {
      try { p.pause(); } catch {}
      try { p.remove(); } catch {}
    }
  };
  // Ưu tiên tệp audio admin đã tải lên (máy chủ chỉ trả audioUrl khi ngôn ngữ nghe trùng ngôn ngữ của bản thu).
  // Không phát được thì quay về giọng đọc của thiết bị.
  const tryPlayAudioFile = async (item: AnyRow, method: string): Promise<boolean> => {
    const cat = getNarr(item);
    if (!cat?.audioEnabled || !cat?.audioUrl) return false;
    try {
      const r = await userApi.getNarration(item.targetType, item.targetId, lang);
      const url = r?.audioUrl;
      if (!url) return false;
      await setAudioModeAsync({ playsInSilentMode: true }).catch(() => {});
      stopAudioFile();
      const player = createAudioPlayer({ uri: url });
      audioPlayerRef.current = player;
      let started = false;
      const timer = setTimeout(() => {
        if (audioPlayerRef.current === player && !started) {
          stopAudioFile();
          speakWithTts(item, method);
        }
      }, 10000);
      player.addListener("playbackStatusUpdate", (status) => {
        if (audioPlayerRef.current !== player) return;
        if (status.playing) started = true;
        if (status.didJustFinish) {
          clearTimeout(timer);
          stopAudioFile();
          setIsSpeaking(false); setIsPaused(false);
          processingRef.current = false; currentNarration.current = null; setActiveNarration(null);
          setMessage("Đã thuyết minh xong: " + item.name);
          runNext();
        }
      });
      setSelected(item);
      setIsPaused(false);
      setIsSpeaking(true);
      setMessage("Đang thuyết minh (audio): " + (translatedField(item, "name", item.name) || item.name));
      player.play();
      try {
        await userApi.recordListen(item.targetType, item.targetId, lang);
        await userApi.addHistory(item.targetType, item.targetId, lang, method);
        setHistory(await userApi.getHistory());
      } catch {}
      return true;
    } catch {
      stopAudioFile();
      return false;
    }
  };
  // method: manual | qr | gps | beacon | auto (hàng đợi) — được ghi vào lịch sử nghe.
  const playNow = async (item: AnyRow, method = "manual") => {
    processingRef.current = true;
    currentNarration.current = item;
    setActiveNarration(item);
    if (await tryPlayAudioFile(item, method)) return;
    await speakWithTts(item, method);
  };
  const speakWithTts = async (item: AnyRow, method = "manual") => {
    let text = textFor(item);
    if (lang !== "vi" && !getNarr(item)?.translations?.[lang] && !getNarr(item)?.texts?.[lang]) {
      try {
        // Ưu tiên bản thuyết minh đã dịch của máy chủ (bản admin nhập > cache > dịch máy).
        let fromServer = "";
        try {
          const r = await userApi.getNarration(item.targetType, item.targetId, lang);
          if (r?.translated && String(r.text || "").trim()) fromServer = String(r.text);
        } catch {}
        text = fromServer || await translateGoogle(sourceTextFor(item), lang);
      } catch {
        processingRef.current = false;
        currentNarration.current = null;
        setActiveNarration(null);
        Alert.alert("Translation unavailable", "Không thể dịch nội dung sang ngôn ngữ đã chọn. Vui lòng kiểm tra kết nối mạng.");
        runNext();
        return;
      }
    }
    if (!text.trim()) {
      processingRef.current = false;
      currentNarration.current = null;
      setActiveNarration(null);
      Alert.alert("Chưa có nội dung", "Admin cần bổ sung nội dung thuyết minh cho đối tượng này.");
      runNext();
      return;
    }
    // Chọn giọng đọc thực sự có trên thiết bị. Nếu Android chưa cài giọng của
    // ngôn ngữ này thì không phát bằng giọng mặc định (thường là tiếng Việt).
    const languageConfig = langs.find((x) => x.code === lang)
      || languages.find((x) => x.code === lang)
      || { code: lang, voice: lang };
    const requestedLocale = String(languageConfig.voice || languageConfig.code || lang).replace(/_/g, "-");
    let availableVoices: Speech.Voice[] = [];
    try { availableVoices = await Speech.getAvailableVoicesAsync(); } catch {}
    const normalizeLocale = (value: string) => String(value || "").replace(/_/g, "-").toLowerCase();
    const requestedNormalized = normalizeLocale(requestedLocale);
    const requestedBase = requestedNormalized.split("-")[0];
    const matchingVoice = availableVoices.find((voice) => normalizeLocale(voice.language) === requestedNormalized)
      || availableVoices.find((voice) => normalizeLocale(voice.language).split("-")[0] === requestedBase);
    if (lang !== "vi" && !matchingVoice) {
      processingRef.current = false;
      currentNarration.current = null;
      setActiveNarration(null);
      setIsSpeaking(false);
      Alert.alert(
        "Chưa có giọng đọc phù hợp",
        `Thiết bị chưa có giọng đọc ${languageConfig.name || lang}. Hãy cài dữ liệu giọng nói cho ngôn ngữ này trong cài đặt chuyển văn bản thành giọng nói (Text-to-speech), sau đó thử lại. Ứng dụng không phát bằng giọng tiếng Việt thay thế.`,
      );
      runNext();
      return;
    }
    setSelected(item);
    setIsPaused(false);
    setIsSpeaking(true);
    setMessage("Đang thuyết minh: " + (translatedField(item, "name", item.name) || item.name));
    Speech.speak(text, {
      language: matchingVoice?.language || requestedLocale,
      ...(matchingVoice?.identifier ? { voice: matchingVoice.identifier } : {}),
      rate: Number(getNarr(item)?.speed) || 1,
      onStart: () => { setIsSpeaking(true); setIsPaused(false); },
      onDone: () => {
        setIsSpeaking(false); setIsPaused(false);
        processingRef.current = false; currentNarration.current = null; setActiveNarration(null);
        setMessage("Đã thuyết minh xong: " + item.name);
        runNext();
      },
      onStopped: () => {
        setIsSpeaking(false); setIsPaused(false);
        // Queue advances only when narration finishes normally or the user explicitly skips.
      },
      onError: () => {
        setIsSpeaking(false); setIsPaused(false);
        processingRef.current = false; currentNarration.current = null; setActiveNarration(null);
        setMessage("Không thể phát thuyết minh.");
        runNext();
      },
    });
    try {
      await userApi.recordListen(item.targetType, item.targetId, lang);
      await userApi.addHistory(item.targetType, item.targetId, lang, method);
      setHistory(await userApi.getHistory());
    } catch {}
  };
  const runNext = () => {
    if (processingRef.current || isPaused) return;
    const next = queueRef.current.shift();
    setAudioQueue([...queueRef.current]);
    if (next) setTimeout(() => playNow(next, next._method || "auto"), 120);
  };
  const play = async (item: AnyRow, method = "manual") => {
    if (!item) return;
    if (processingRef.current || isSpeaking) {
      const exists = queueRef.current.some((x) => x.targetType === item.targetType && same(x.targetId, item.targetId));
      if (!exists && !(currentNarration.current && currentNarration.current.targetType === item.targetType && same(currentNarration.current.targetId, item.targetId))) {
        queueRef.current = [...queueRef.current, { ...item, _method: method }];
        setAudioQueue([...queueRef.current]);
        setMessage(`Đã thêm “${item.name}” vào hàng đợi thuyết minh.`);
      }
      return;
    }
    await playNow(item, method);
  };
  const pauseOrResume = async () => {
    try {
      const ap = audioPlayerRef.current;
      if (ap) {
        if (isPaused) { ap.play(); setIsPaused(false); setMessage("Đang tiếp tục thuyết minh."); }
        else { ap.pause(); setIsPaused(true); setMessage("Đã tạm dừng. Các bài tiếp theo sẽ chờ bài hiện tại phát xong."); }
        return;
      }
      if (isPaused) { await Speech.resume(); setIsPaused(false); setMessage("Đang tiếp tục thuyết minh."); }
      else { await Speech.pause(); setIsPaused(true); setMessage("Đã tạm dừng. Các bài tiếp theo sẽ chờ bài hiện tại phát xong."); }
    } catch { Alert.alert("Không hỗ trợ", "Thiết bị hiện không hỗ trợ tạm dừng giọng đọc."); }
  };
  const skipNarration = async () => {
    const next = queueRef.current.shift();
    setAudioQueue([...queueRef.current]);
    // Prevent onStopped from consuming a second queue item.
    processingRef.current = true;
    stopAudioFile();
    await Speech.stop();
    processingRef.current = false;
    currentNarration.current = null;
    setActiveNarration(null);
    setIsSpeaking(false); setIsPaused(false);
    if (next) setTimeout(() => playNow(next, next._method || "auto"), 120);
    else setMessage("");
  };
  // Dữ liệu mới nhất cho các callback chạy lâu (GPS/Bluetooth) — tránh dùng state cũ trong closure.
  const geoTargets = useMemo(() => buildGeoTargets(items, narr), [items, narr]);
  const beaconTargets = useMemo(() => buildBeaconTargets(items, narr), [items, narr]);
  const latest = useRef({ items, geoTargets, beaconTargets, play });
  latest.current = { items, geoTargets, beaconTargets, play };
  const itemByKey = (key: string) => latest.current.items.find((x) => targetKey(x.targetType, x.targetId) === key);
  const animateTo = (latitude: number, longitude: number) =>
    mapRef.current?.animateToRegion({ latitude, longitude, latitudeDelta: 0.01, longitudeDelta: 0.01 }, 600);

  const onPosition = (c: { latitude: number; longitude: number; accuracy?: number | null }) => {
    setGps({ latitude: c.latitude, longitude: c.longitude });
    if (!firstFixRef.current) {
      firstFixRef.current = true;
      animateTo(c.latitude, c.longitude);
    }
    const entered = geoRef.current.update(c.latitude, c.longitude, latest.current.geoTargets, c.accuracy);
    for (const key of entered) {
      const item = itemByKey(key);
      if (item) latest.current.play(item, "gps");
    }
  };
  const startGpsAuto = async () => {
    try {
      const perm = await Location.requestForegroundPermissionsAsync();
      if (!perm.granted) {
        Alert.alert("Cần quyền vị trí", "Hãy cho phép ứng dụng truy cập vị trí khi sử dụng.");
        return;
      }
      if (!(await Location.hasServicesEnabledAsync())) {
        Alert.alert("GPS đang tắt", "Hãy bật Vị trí (GPS) của thiết bị rồi thử lại.");
        return;
      }
      try { watchRef.current?.remove(); } catch {}
      geoRef.current.reset();
      firstFixRef.current = false;
      watchRef.current = await Location.watchPositionAsync(
        { accuracy: Location.Accuracy.High, distanceInterval: 5, timeInterval: 2000 },
        (pos) => onPosition(pos.coords),
      );
      setGpsAuto(true);
      setMessage("Đã bật GPS tự động. Ứng dụng sẽ tự thuyết minh khi bạn đi vào vùng của địa điểm/hiện vật.");
    } catch (e: any) {
      setGpsAuto(false);
      setMessage("Không bật được GPS tự động: " + (e?.message || e));
    }
  };
  const stopGpsAuto = () => {
    try { watchRef.current?.remove(); } catch {}
    watchRef.current = null;
    geoRef.current.reset();
    setGpsAuto(false);
    setMessage("Đã tắt GPS tự động.");
  };
  const toggleGpsAuto = () => (gpsAuto ? stopGpsAuto() : startGpsAuto());

  const onBeaconAdvert = (ad: { id: string; name?: string | null; localName?: string | null; rssi?: number | null }) => {
    const hit = beaconRef.current.onAdvert(ad, latest.current.beaconTargets);
    if (!hit) return;
    const item = itemByKey(hit.key);
    if (item) latest.current.play(item, "beacon");
  };
  const startBtAuto = async () => {
    beaconRef.current.reset();
    const r = await Ble.startBeaconScan(onBeaconAdvert, (m) => setMessage(m));
    if (!r.ok) {
      setBtAuto(false);
      Alert.alert("Bluetooth", r.message);
      return;
    }
    bleScanRef.current = r.scan;
    setBtAuto(true);
    setMessage(
      latest.current.beaconTargets.length
        ? "Đã bật Bluetooth. Ứng dụng sẽ tự thuyết minh khi bạn đến gần beacon."
        : "Đã bật Bluetooth, nhưng chưa có địa điểm/hiện vật nào được gắn beacon trong Admin.",
    );
  };
  const stopBtAuto = () => {
    try { bleScanRef.current?.stop(); } catch {}
    bleScanRef.current = null;
    beaconRef.current.reset();
    setBtAuto(false);
    setMessage("Đã tắt Bluetooth beacon.");
  };
  const toggleBtAuto = () => (btAuto ? stopBtAuto() : startBtAuto());

  // Nút "Vị trí của tôi": lấy vị trí một lần, đưa bản đồ tới đó; nếu GPS tự động đang tắt thì kiểm tra luôn vùng thuyết minh.
  const locateMe = async () => {
    try {
      const p = await Location.requestForegroundPermissionsAsync();
      if (!p.granted) {
        Alert.alert("Cần quyền vị trí", "Hãy cho phép ứng dụng truy cập vị trí khi sử dụng.");
        return;
      }
      const pos = await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.Balanced });
      const { latitude, longitude, accuracy } = pos.coords;
      setGps({ latitude, longitude });
      animateTo(latitude, longitude);
      setMessage("Đã xác định vị trí hiện tại.");
      if (!gpsAuto) {
        const entered = new GeofenceTracker().update(latitude, longitude, geoTargets, accuracy);
        const item = entered.length ? itemByKey(entered[0]) : undefined;
        if (item) play(item, "gps");
      }
    } catch (e: any) {
      setMessage("Không lấy được vị trí: " + (e?.message || e));
    }
  };
  const toggleLike = async (item: AnyRow) => {
    const old = likes.find(
      (x) =>
        x.targetType === item.targetType && same(x.targetId, item.targetId),
    );
    try {
      if (old) {
        await userApi.removeLike(item.targetType, item.targetId);
        setLikes((v) => v.filter((x) => x.id !== old.id));
      } else {
        await userApi.addLike(item.targetType, item.targetId);
        const row = { targetType: item.targetType, targetId: item.targetId, name: item.name, id: `${item.targetType}:${item.targetId}` };
        setLikes((v) => [...v, row]);
      }
    } catch {
      Alert.alert("Lỗi", "Không thể cập nhật yêu thích.");
    }
  };
  const openQr = async () => {
    if (!permission?.granted) {
      const p = await requestPermission();
      if (!p.granted) return;
    }
    setQr(true);
  };
  const onBarcode = async ({ data }: any) => {
    if (loginQr) {
      let tokenValue = "";
      try {
        const parsed = JSON.parse(data);
        if (parsed?.type === "app-login" && parsed.token) {
          tokenValue = String(parsed.token);
          if (parsed.server) await userApi.setApiBase(String(parsed.server));
        }
      } catch {
        // QR dạng đường link web: http://<ip>:5173/user?token=... -> lấy token và đoán máy chủ API cùng IP (cổng 3000).
        try {
          const u = new URL(data);
          tokenValue = u.searchParams.get("token") || "";
          if (tokenValue && u.hostname) await userApi.setApiBase(`${u.protocol}//${u.hostname}:3000`);
        } catch {}
      }
      if (!tokenValue) {
        setLoginQr(false);
        setLoginError("QR không hợp lệ. Hãy quét mã đăng nhập App được tạo từ Admin.");
        return;
      }
      setLoginQr(false);
      await connectDevice(tokenValue);
      return;
    }
    setQr(false);
    let found = items.find(
      (x) =>
        same(x.id, data) ||
        same(x.name, data) ||
        same(getNarr(x)?.qr?.code, data) ||
        String(data).includes("id=" + x.id),
    );
    if (!found) {
      Alert.alert("Không tìm thấy", `Không tìm thấy địa điểm/hiện vật cho mã: ${data}`);
      return;
    }
    await play(found, "qr");
  };
  const region: Region = {
    latitude: gps?.latitude ?? 10.7771,
    longitude: gps?.longitude ?? 106.6954,
    latitudeDelta: 0.08,
    longitudeDelta: 0.08,
  };
  if (loginQr)
    return (
      <View style={s.camera}>
        <TouchableOpacity style={s.close} onPress={() => setLoginQr(false)}>
          <Text style={s.closeText}>Đóng ✕</Text>
        </TouchableOpacity>
        <CameraView style={{ flex: 1 }} facing="back" barcodeScannerSettings={{ barcodeTypes: ["qr"] }} onBarcodeScanned={onBarcode} />
        <Text style={s.cameraHint}>Quét QR đăng nhập App do Admin tạo</Text>
      </View>
    );
  if (authLoading)
    return <View style={s.center}><ActivityIndicator size="large" /><Text style={s.muted}>Đang xác thực thiết bị...</Text></View>;
  if (!authenticated)
    return (
      <View style={[s.root, { justifyContent: "center", padding: 24 }]}>
        <Text style={s.pageTitle}>Đăng nhập ứng dụng</Text>
        <Text style={s.description}>Quét mã QR do Admin tạo để liên kết thiết bị. Những lần mở sau ứng dụng sẽ tự xác thực thiết bị đã được cấp quyền.</Text>
        {loginError ? <Text style={s.notice}>{loginError}</Text> : null}
        <TouchableOpacity style={[s.playButton, { marginTop: 20 }]} onPress={async () => {
          if (!permission?.granted) { const p = await requestPermission(); if (!p.granted) return; }
          setLoginError(""); setLoginQr(true);
        }}><Text style={s.playText}>▦ Quét QR đăng nhập</Text></TouchableOpacity>
        <Text style={s.cardSub}>Máy chủ: {userApi.getApiBase()}</Text>
      </View>
    );
  if (loading)
    return (
      <View style={s.center}>
        <ActivityIndicator size="large" />
        <Text style={s.muted}>Đang tải dữ liệu...</Text>
      </View>
    );
  if (qr)
    return (
      <View style={s.camera}>
        <TouchableOpacity style={s.close} onPress={() => setQr(false)}>
          <Text style={s.closeText}>Đóng ✕</Text>
        </TouchableOpacity>
        <CameraView
          style={{ flex: 1 }}
          facing="back"
          barcodeScannerSettings={{ barcodeTypes: ["qr"] }}
          onBarcodeScanned={onBarcode}
        />
        <Text style={s.cameraHint}>Đưa mã QR vào khung hình</Text>
      </View>
    );
  return (
    <View style={s.root}>
      <View style={s.header}>
        <Text style={s.brand}>DI SẢN VIỆT</Text>
        <Text style={s.headerSub}>{lang === "en" ? "Multilingual audio guide" : lang === "zh-CN" ? "多语言语音导览" : lang === "ja" ? "多言語音声ガイド" : lang === "ko" ? "다국어 오디오 가이드" : lang === "fr" ? "Guide audio multilingue" : "Thuyết minh đa ngôn ngữ"}</Text>
      </View>
      <View style={s.language}>
        <Text style={s.label}>{tx("language")}</Text>
        <ScrollView horizontal showsHorizontalScrollIndicator={false}>
          {(languages.length ? languages : langs).map((l) => (
            <TouchableOpacity
              key={l.code}
              onPress={() => { setLang(l.code); AsyncStorage.setItem("guideLanguage", l.code); }}
              style={[s.langChip, lang === l.code && s.langActive]}
            >
              <Text style={[s.langText, lang === l.code && s.langTextActive]}>
                {languageLabel(l)}
              </Text>
            </TouchableOpacity>
          ))}
        </ScrollView>
      </View>
      {message ? <Text style={s.notice}>{message}</Text> : null}
      {tab === "home" ? (
        <>
          <View style={s.searchRow}>
            <TextInput
              value={query}
              onChangeText={setQuery}
              placeholder={tx("search")}
              style={s.search}
            />
            <TouchableOpacity style={s.qrButton} onPress={openQr}>
              <Text style={s.qrText}>{tx("qr")}</Text>
            </TouchableOpacity>
          </View>
          <View style={s.mapToggleRow}>
            <Text style={s.mapToggleHint}>
              {showMap ? tx("mapOn") : tx("mapOff")}
            </Text>
            <TouchableOpacity
              style={s.mapToggle}
              onPress={() => setShowMap((current) => !current)}
              accessibilityRole="button"
              accessibilityLabel={showMap ? tx("hideMap") : tx("showMap")}
            >
              <Text style={s.mapToggleText}>
                {showMap ? tx("hideMap") + " －" : "＋ " + tx("showMap")}
              </Text>
            </TouchableOpacity>
          </View>
          <View style={s.autoRow}>
            <TouchableOpacity
              style={[s.autoChip, gpsAuto && s.autoChipOn]}
              onPress={toggleGpsAuto}
              accessibilityRole="switch"
              accessibilityState={{ checked: gpsAuto }}
              accessibilityLabel={tx("gpsAuto")}
            >
              <Text style={[s.autoChipText, gpsAuto && s.autoChipTextOn]}>{gpsAuto ? "●" : "○"} ◎ {tx("gpsAuto")}</Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[s.autoChip, btAuto && s.autoChipOn]}
              onPress={toggleBtAuto}
              accessibilityRole="switch"
              accessibilityState={{ checked: btAuto }}
              accessibilityLabel={tx("btAuto")}
            >
              <Text style={[s.autoChipText, btAuto && s.autoChipTextOn]}>{btAuto ? "●" : "○"} ᛒ {tx("btAuto")}</Text>
            </TouchableOpacity>
          </View>
          {showMap ? <View style={s.mapWrap}>
            <MapView
              ref={mapRef}
              style={s.map}
              initialRegion={region}
              showsUserLocation={!!gps}
              onUserLocationChange={(e) => {
                if (e.nativeEvent.coordinate && !gps)
                  setGps({
                    latitude: e.nativeEvent.coordinate.latitude,
                    longitude: e.nativeEvent.coordinate.longitude,
                  });
              }}
            >
              <>
                {visible.map((item) => {
                  const lat = Number(item.latitude),
                    lng = Number(item.longitude);
                  if (!Number.isFinite(lat) || !Number.isFinite(lng))
                    return null;
                  const n = getNarr(item);
                  return (
                    <React.Fragment key={item.targetType + "-" + item.id}>
                      <Marker
                        coordinate={{ latitude: lat, longitude: lng }}
                        title={translatedField(item, "name", item.name || "")}
                        description={translatedField(item, "address", item.displayAddress || "")}
                      >
                        <Callout onPress={() => setSelected(item)}>
                          <View style={{ padding: 8, minWidth: 120 }}>
                            <Text style={{ fontWeight: "800", marginBottom: 5 }}>{translatedField(item, "name", item.name || "")}</Text>
                            <Text style={{ color: "#2563eb", fontWeight: "800", textAlign: "center" }}>{tx("view")}</Text>
                          </View>
                        </Callout>
                      </Marker>
                      {n?.gps?.enabled ? (
                        <Circle
                          center={{
                            latitude: Number(n.gps.latitude ?? lat),
                            longitude: Number(n.gps.longitude ?? lng),
                          }}
                          radius={Number(n.gps.radius || 50)}
                          strokeColor="#8b5cf6"
                          fillColor="rgba(139,92,246,0.12)"
                        />
                      ) : null}
                    </React.Fragment>
                  );
                })}
              </>
            </MapView>
            <TouchableOpacity style={s.locate} onPress={locateMe}>
              <Text style={s.locateText}>◎ {tx("myLocation")}</Text>
            </TouchableOpacity>
          </View> : null}
          {!query.trim() ? <>
          <View style={s.featureSection}>
            <View style={s.sectionHeader}>
              <Text style={s.sectionTitle}>{tx("places")}</Text>
              <TouchableOpacity onPress={() => { setBrowseType("site"); setBrowseQuery(""); setBrowseCity(""); setBrowseSite(""); setTab("browse"); }}><Text style={s.viewAll}>{tx("all")} →</Text></TouchableOpacity>
            </View>
            <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={s.horizontalList}>
              {items.filter((x) => x.targetType === "site").map((item) => (
                <TouchableOpacity key={"featured-site-" + item.id} style={s.horizontalCard} onPress={() => setSelected(item)}>
                  <Image source={{ uri: item.img || item.image || `https://placehold.co/640x420?text=${encodeURIComponent(item.name || "Place")}` }} style={s.horizontalImage} />
                  <Text numberOfLines={2} style={s.cardTitle}>{translatedField(item, "name", item.name || "")}</Text>
                  <Text numberOfLines={1} style={s.cardSub}>{translatedField(item, "address", item.city || "")}</Text>
                </TouchableOpacity>
              ))}
            </ScrollView>
          </View>
          <View style={s.featureSection}>
            <View style={s.sectionHeader}>
              <Text style={s.sectionTitle}>{tx("artifacts")}</Text>
              <TouchableOpacity onPress={() => { setBrowseType("artifact"); setBrowseQuery(""); setBrowseCity(""); setBrowseSite(""); setTab("browse"); }}><Text style={s.viewAll}>{tx("all")} →</Text></TouchableOpacity>
            </View>
            <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={s.horizontalList}>
              {items.filter((x) => x.targetType === "artifact").map((item) => (
                <TouchableOpacity key={"featured-artifact-" + item.id} style={s.horizontalCard} onPress={() => setSelected(item)}>
                  <Image source={{ uri: item.img || item.image || `https://placehold.co/640x420?text=${encodeURIComponent(item.name || "Artifact")}` }} style={s.horizontalImage} />
                  <Text numberOfLines={2} style={s.cardTitle}>{translatedField(item, "name", item.name || "")}</Text>
                  <Text numberOfLines={1} style={s.cardSub}>{translatedField(item, "address", item.siteName || item.city || "")}</Text>
                </TouchableOpacity>
              ))}
            </ScrollView>
          </View>
          </> : null}
          {activeNarration ? <View style={s.queueBanner}>
            <View style={{ flex: 1 }}>
              <Text style={s.queueTitle}>Đang thuyết minh: {translatedField(activeNarration, "name", activeNarration.name || "")}</Text>
              <Text style={s.cardSub}>{audioQueue.length ? `${audioQueue.length} bài đang chờ` : "Không có bài chờ"}</Text>
            </View>
            <TouchableOpacity style={s.audioControlButton} onPress={pauseOrResume}><Text style={s.audioControlText}>{isPaused ? tx("resume") : tx("pause")}</Text></TouchableOpacity>
            <TouchableOpacity style={[s.audioControlButton, s.skipButton]} onPress={skipNarration}><Text style={s.audioControlText}>{tx("skip")}</Text></TouchableOpacity>
          </View> : null}
          <ScrollView contentContainerStyle={s.list}>
            {visible.map((item) => (
              <View key={item.targetType + "-" + item.id} style={s.card}>
                <View style={s.cardTop}>
                  <View style={{ flex: 1 }}>
                    <Text style={s.cardTitle}>{translatedField(item, "name", item.name || "")}</Text>
                    <Text style={s.cardSub}>
                      {translatedField(item, "address", item.displayAddress || item.siteName || item.city || "Địa điểm tham quan")}
                    </Text>
                  </View>
                  <TouchableOpacity onPress={() => toggleLike(item)}>
                    <Text style={s.heart}>
                      {likes.some(
                        (x) =>
                          x.targetType === item.targetType &&
                          same(x.targetId, item.targetId),
                      )
                        ? "♥"
                        : "♡"}
                    </Text>
                  </TouchableOpacity>
                </View>
                <Text numberOfLines={3} style={s.description}>
                  {translatedField(item, "description", item.description || "")}
                </Text>
                <View style={s.actions}>
                  <TouchableOpacity
                    style={s.playButton}
                    onPress={() => play(item)}
                  >
                    <Text style={s.playText}>▶ {tx("listen")}</Text>
                  </TouchableOpacity>
                  <TouchableOpacity
                    style={s.detailButton}
                    onPress={() => setSelected(item)}
                  >
                    <Text style={s.detailText}>{tx("details")}</Text>
                  </TouchableOpacity>
                </View>
              </View>
            ))}
            {!visible.length && (
              <Text style={s.muted}>{tx("noResults")}</Text>
            )}
          </ScrollView>
        </>
      ) : tab === "browse" ? (
        <ScrollView contentContainerStyle={s.list}>
          <TouchableOpacity onPress={() => setTab("home")}><Text style={s.viewAll}>← {tx("explore")}</Text></TouchableOpacity>
          <Text style={s.pageTitle}>{browseType === "site" ? tx("places") : tx("artifacts")}</Text>
          <TextInput value={browseQuery} onChangeText={setBrowseQuery} placeholder={browseType === "site" ? tx("searchPlaces") : tx("searchArtifacts")} style={s.search} />
          <TextInput value={browseCity} onChangeText={setBrowseCity} placeholder={tx("city")} style={[s.search, { marginTop: 8 }]} />
          {browseType === "artifact" ? <TextInput value={browseSite} onChangeText={setBrowseSite} placeholder={tx("containingSite")} style={[s.search, { marginTop: 8 }]} /> : null}
          {items.filter((item) => {
            if (item.targetType !== browseType) return false;
            const nameAndDescription = [
              item.name,
              item.description,
              translatedField(item, "name", item.name || ""),
              translatedField(item, "description", item.description || ""),
            ].map(normalizeSearch).join(" ");
            const cityAndAddress = [
              item.city,
              item.address,
              translatedField(item, "address", item.displayAddress || item.address || item.city || ""),
            ].map(normalizeSearch).join(" ");
            const parentSite = sites.find((site) => same(site.id, item.siteId));
            const siteNames = [item.siteName, parentSite?.name,
              parentSite ? translatedField({ ...parentSite, targetType: "site", targetId: parentSite.id }, "name", parentSite.name || "") : "",
            ].map(normalizeSearch).join(" ");
            return nameAndDescription.includes(normalizeSearch(browseQuery))
              && cityAndAddress.includes(normalizeSearch(browseCity))
              && (browseType !== "artifact" || siteNames.includes(normalizeSearch(browseSite)));
          }).map((item) => <TouchableOpacity key={"browse-" + item.targetType + "-" + item.id} style={s.card} onPress={() => setSelected(item)}>
            <Text style={s.cardTitle}>{translatedField(item, "name", item.name || "")}</Text>
            <Text style={s.cardSub}>{translatedField(item, "address", item.siteName || item.city || item.address || "")}</Text>
            <Text style={s.viewAll}>{tx("details")} →</Text>
          </TouchableOpacity>)}
        </ScrollView>
      ) : tab === "history" ? (
        <ScrollView contentContainerStyle={s.list}>
          <Text style={s.pageTitle}>{tx("history")}</Text>
          {history
            .slice()
            .reverse()
            .map((h, i) => {
              const item = items.find(
                (x) =>
                  x.targetType === h.targetType && same(x.targetId, h.targetId),
              );
              return (
                <TouchableOpacity
                  key={h.id ?? i}
                  style={s.card}
                  onPress={() => item && play(item)}
                >
                  <Text style={s.cardTitle}>
                    {h.name || item?.name || "Nội dung đã nghe"}
                  </Text>
                  <Text style={s.cardSub}>
                    {methodLabel(h.method)} · {h.language || ""}
                  </Text>
                  <Text style={s.cardSub}>
                    {h.playedAt ? new Date(h.playedAt).toLocaleString() : ""}
                  </Text>
                </TouchableOpacity>
              );
            })}
        </ScrollView>
      ) : (
        <ScrollView contentContainerStyle={s.list}>
          <Text style={s.pageTitle}>{tx("favorites")}</Text>
          {likes.map((l) => {
            const item = items.find(
              (x) =>
                x.targetType === l.targetType && same(x.targetId, l.targetId),
            );
            return item ? (
              <View key={l.id} style={s.card}>
                <Text style={s.cardTitle}>{item.name}</Text>
                <View style={s.actions}>
                  <TouchableOpacity
                    style={s.playButton}
                    onPress={() => play(item)}
                  >
                    <Text style={s.playText}>▶ Nghe</Text>
                  </TouchableOpacity>
                  <TouchableOpacity
                    style={s.detailButton}
                    onPress={() => toggleLike(item)}
                  >
                    <Text style={s.detailText}>Bỏ thích</Text>
                  </TouchableOpacity>
                </View>
              </View>
            ) : null;
          })}
        </ScrollView>
      )}
      <View style={s.nav}>
        {[
          ["home", "⌂", tx("explore")],
          ["history", "◷", tx("history")],
          ["likes", "♡", tx("favorites")],
        ].map(([key, icon, label]) => (
          <TouchableOpacity
            key={key}
            style={s.navItem}
            onPress={() => {
              setTab(key);
            }}
          >
            <Text style={[s.navIcon, tab === key && s.navSelected]}>
              {icon}
            </Text>
            <Text style={[s.navLabel, tab === key && s.navSelected]}>
              {label}
            </Text>
          </TouchableOpacity>
        ))}
      </View>
      {selected && (
        <View style={s.modalBackdrop}>
          <View style={s.modal}>
            <TouchableOpacity
              style={s.modalClose}
              onPress={() => setSelected(null)}
            >
              <Text style={s.detailText}>{tx("close")} ✕</Text>
            </TouchableOpacity>
            <ScrollView>
              <Image source={{ uri: selected.img || selected.image || `https://placehold.co/640x420?text=${encodeURIComponent(selected.name || "Place")}` }} style={{ width: "100%", height: 190, borderRadius: 12, marginBottom: 14 }} resizeMode="cover" />
              <Text style={s.pageTitle}>{translatedField(selected, "name", selected.name || "")}</Text>
              <Text style={s.description}>{translatedField(selected, "description", selected.description || "")}</Text>
              <Text style={s.cardSub}>{translatedField(selected, "address", selected.displayAddress || selected.address || selected.siteName || "")}</Text>
              <Text style={[s.sectionTitle, { marginTop: 18 }]}>{tx("description")}</Text>
              {selected.targetType === "site" ? (
                <>
                  <Text style={s.sectionTitle}>{lang === "en" ? "Artifacts at this place" : lang === "zh-CN" ? "此地点的文物" : lang === "ja" ? "この場所の展示品" : lang === "ko" ? "이 장소의 유물" : lang === "fr" ? "Objets de ce lieu" : "Hiện vật tại địa điểm"}</Text>
                  <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={s.horizontalList}>
                    {arts.filter((a) => same(a.siteId, selected.targetId)).map((a) => (
                      <TouchableOpacity key={"detail-art-" + a.id} style={s.horizontalCard} onPress={() => setSelected({ ...a, targetType: "artifact", targetId: a.id, siteName: selected.name })}>
                        <Image source={{ uri: a.img || `https://placehold.co/640x420?text=${encodeURIComponent(a.name || "Artifact")}` }} style={s.horizontalImage} />
                        <Text style={s.cardTitle}>{translatedField({ ...a, targetType: "artifact", targetId: a.id }, "name", a.name || "")}</Text>
                        <Text style={s.viewAll}>{tx("details")} →</Text>
                      </TouchableOpacity>
                    ))}
                  </ScrollView>
                </>
              ) : null}
            </ScrollView>
            <TouchableOpacity
              style={s.playButton}
              onPress={() => play(selected)}
            >
              <Text style={s.playText}>▶ {tx("listen")}</Text>
            </TouchableOpacity>
            {isSpeaking && (
              <View style={s.audioControls}>
                <TouchableOpacity style={s.audioControlButton} onPress={pauseOrResume}>
                  <Text style={s.audioControlText}>{isPaused ? "▶ " + tx("resume") : "Ⅱ " + tx("pause")}</Text>
                </TouchableOpacity>
                <TouchableOpacity style={[s.audioControlButton, s.skipButton]} onPress={skipNarration}>
                  <Text style={s.audioControlText}>⏭ {tx("skip")}</Text>
                </TouchableOpacity>
              </View>
            )}
          </View>
        </View>
      )}
    </View>
  );
}
const METHOD_LABELS: Record<string, string> = { gps: "GPS", beacon: "Bluetooth", qr: "QR", manual: "Thuyết minh", auto: "Hàng đợi" };
const methodLabel = (m?: string) => METHOD_LABELS[String(m || "").toLowerCase()] || m || "Thuyết minh";
const s = StyleSheet.create({
  root: { flex: 1, backgroundColor: "#f7f8fc" },
  header: {
    backgroundColor: "#152b4a",
    paddingTop: 54,
    paddingBottom: 18,
    paddingHorizontal: 20,
  },
  brand: { fontSize: 21, fontWeight: "900", letterSpacing: 2, color: "#fff" },
  headerSub: { color: "#d3deee", marginTop: 3 },
  language: {
    paddingHorizontal: 14,
    paddingVertical: 10,
    backgroundColor: "#fff",
  },
  label: { fontSize: 12, color: "#64748b", marginBottom: 7 },
  langChip: {
    paddingVertical: 7,
    paddingHorizontal: 12,
    borderRadius: 18,
    backgroundColor: "#f1f5f9",
    marginRight: 7,
  },
  langActive: { backgroundColor: "#2563eb" },
  langText: { fontSize: 12, color: "#334155" },
  langTextActive: { color: "#fff", fontWeight: "700" },
  notice: {
    marginHorizontal: 12,
    marginTop: 8,
    color: "#9a3412",
    fontSize: 12,
  },
  searchRow: { flexDirection: "row", padding: 12, gap: 8 },
  search: {
    flex: 1,
    backgroundColor: "#fff",
    borderWidth: 1,
    borderColor: "#e2e8f0",
    borderRadius: 12,
    paddingHorizontal: 14,
    height: 44,
  },
  qrButton: {
    backgroundColor: "#0f766e",
    borderRadius: 12,
    justifyContent: "center",
    paddingHorizontal: 14,
  },
  qrText: { color: "#fff", fontWeight: "700" },
  mapWrap: {
    height: 250,
    marginHorizontal: 12,
    borderRadius: 16,
    overflow: "hidden",
  },
  map: { flex: 1 },
  mapToggleRow: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    marginHorizontal: 12,
    marginTop: 2,
    marginBottom: 8,
  },
  autoRow: { flexDirection: "row", gap: 8, marginHorizontal: 12, marginBottom: 8 },
  autoChip: { flex: 1, backgroundColor: "#fff", borderWidth: 1, borderColor: "#d5dbe7", borderRadius: 10, paddingVertical: 9, alignItems: "center" },
  autoChipOn: { backgroundColor: "#dcfce7", borderColor: "#86efac" },
  autoChipText: { color: "#475569", fontSize: 12, fontWeight: "800" },
  autoChipTextOn: { color: "#166534" },
  mapToggleHint: { color: "#64748b", fontSize: 12 },
  mapToggle: {
    backgroundColor: "#e8efff",
    borderWidth: 1,
    borderColor: "#c7d7fe",
    borderRadius: 10,
    paddingVertical: 8,
    paddingHorizontal: 12,
  },
  mapToggleText: { color: "#1d4ed8", fontSize: 12, fontWeight: "800" },
  locate: {
    position: "absolute",
    right: 10,
    bottom: 10,
    backgroundColor: "#fff",
    paddingVertical: 9,
    paddingHorizontal: 12,
    borderRadius: 20,
    elevation: 3,
  },
  locateText: { color: "#1d4ed8", fontWeight: "700" },
  list: { padding: 12, paddingBottom: 30 },
  featureSection: { paddingTop: 12, paddingHorizontal: 12 },
  sectionHeader: { flexDirection: "row", alignItems: "center", justifyContent: "space-between", marginBottom: 8 },
  sectionTitle: { fontSize: 16, fontWeight: "900", color: "#172b4d" },
  viewAll: { color: "#2563eb", fontWeight: "800", fontSize: 12, padding: 4 },
  horizontalList: { gap: 10, paddingBottom: 6 },
  horizontalCard: { width: 180, backgroundColor: "#fff", borderRadius: 12, padding: 9, borderWidth: 1, borderColor: "#edf0f5" },
  horizontalImage: { width: "100%", height: 100, borderRadius: 9, marginBottom: 7, backgroundColor: "#eef2f7" },
  queueBanner: { marginHorizontal: 12, marginTop: 8, padding: 10, backgroundColor: "#eaf1ff", borderRadius: 12, flexDirection: "row", alignItems: "center", gap: 6 },
  queueTitle: { color: "#172b4d", fontWeight: "800", fontSize: 12 },
  card: {
    backgroundColor: "#fff",
    padding: 15,
    borderRadius: 16,
    marginBottom: 11,
    borderWidth: 1,
    borderColor: "#edf0f5",
  },
  cardTop: { flexDirection: "row", alignItems: "flex-start" },
  cardTitle: { fontSize: 16, fontWeight: "800", color: "#172b4d" },
  cardSub: { fontSize: 12, color: "#64748b", marginTop: 5 },
  heart: { fontSize: 27, color: "#e11d48", paddingHorizontal: 5 },
  description: {
    fontSize: 13,
    color: "#475569",
    lineHeight: 20,
    marginTop: 10,
  },
  actions: { flexDirection: "row", gap: 8, marginTop: 12 },
  playButton: {
    backgroundColor: "#2563eb",
    paddingVertical: 10,
    paddingHorizontal: 13,
    borderRadius: 10,
    alignItems: "center",
  },
  playText: { color: "#fff", fontWeight: "700", fontSize: 13 },
  detailButton: {
    borderWidth: 1,
    borderColor: "#cbd5e1",
    paddingVertical: 10,
    paddingHorizontal: 14,
    borderRadius: 10,
  },
  detailText: { color: "#334155", fontWeight: "700" },
  nav: {
    flexDirection: "row",
    backgroundColor: "#fff",
    borderTopWidth: 1,
    borderTopColor: "#e2e8f0",
    paddingVertical: 9,
    paddingBottom: Platform.OS === "ios" ? 24 : 10,
  },
  navItem: { flex: 1, alignItems: "center" },
  navIcon: { fontSize: 22, color: "#64748b" },
  navLabel: { fontSize: 11, color: "#64748b", marginTop: 2 },
  navSelected: { color: "#2563eb", fontWeight: "800" },
  center: { flex: 1, alignItems: "center", justifyContent: "center", gap: 12 },
  muted: { color: "#64748b", textAlign: "center", margin: 18 },
  pageTitle: {
    fontSize: 21,
    fontWeight: "900",
    color: "#172b4d",
    marginBottom: 12,
  },
  camera: { flex: 1, backgroundColor: "#000" },
  close: {
    position: "absolute",
    zIndex: 2,
    top: 55,
    right: 16,
    backgroundColor: "#fff",
    padding: 10,
    borderRadius: 10,
  },
  closeText: { fontWeight: "700" },
  cameraHint: {
    position: "absolute",
    bottom: 55,
    alignSelf: "center",
    color: "#fff",
    backgroundColor: "#0009",
    padding: 12,
    borderRadius: 10,
  },
  modalBackdrop: {
    position: "absolute",
    top: 0,
    bottom: 0,
    left: 0,
    right: 0,
    backgroundColor: "#0008",
    justifyContent: "center",
    padding: 20,
  },
  modal: {
    backgroundColor: "#fff",
    padding: 20,
    borderRadius: 18,
    maxHeight: "80%",
  },
  modalClose: { alignSelf: "flex-end", paddingBottom: 12 },
  audioControls: { flexDirection: "row", gap: 10, marginTop: 10 },
  audioControlButton: { flex: 1, backgroundColor: "#0f766e", paddingVertical: 11, borderRadius: 10, alignItems: "center" },
  skipButton: { backgroundColor: "#b45309" },
  audioControlText: { color: "#fff", fontWeight: "800", fontSize: 13 },
});
