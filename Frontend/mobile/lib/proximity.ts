/* =====================================================================
   lib/proximity.ts — logic thuần (không phụ thuộc React Native) cho thuyết minh tự động:
     - GeofenceTracker : GPS liên tục, đi vào vùng thì kích hoạt, ra khỏi vùng +20 m mới kích hoạt lại
     - BeaconTracker   : Bluetooth beacon, nhận diện theo MAC/tên, ước lượng khoảng cách từ RSSI
   Tách riêng để kiểm thử được bằng Node (xem Frontend/mobile/tests/proximity.test.mjs).
   ===================================================================== */

type AnyRow = Record<string, any>;

/** Ra khỏi vùng thêm chừng này mét thì lần sau đi vào mới đọc lại (giống bản web). */
export const REARM_MARGIN_M = 20;
/** Bỏ qua điểm GPS kém chính xác hơn mức này (mét) để không kích hoạt nhầm. */
export const MAX_ACCURACY_M = 75;
export const DEFAULT_GPS_RADIUS_M = 50;
export const DEFAULT_BEACON_RADIUS_M = 5;

export const targetKey = (targetType: string, targetId: number | string) => `${targetType}:${targetId}`;

const finite = (v: any): number | null => {
  if (v === null || v === undefined || v === "") return null;
  const n = Number(v);
  return Number.isFinite(n) ? n : null;
};

export function distanceMeters(lat1: number, lng1: number, lat2: number, lng2: number): number {
  const r = Math.PI / 180;
  const dLat = (lat2 - lat1) * r;
  const dLng = (lng2 - lng1) * r;
  const x = Math.sin(dLat / 2) ** 2 + Math.cos(lat1 * r) * Math.cos(lat2 * r) * Math.sin(dLng / 2) ** 2;
  return 6371000 * 2 * Math.atan2(Math.sqrt(x), Math.sqrt(1 - x));
}

/* ------------------------------ GPS ------------------------------ */

export type GeoTarget = { key: string; lat: number; lng: number; radius: number };

/** Các vùng GPS đang bật: lấy toạ độ trong cấu hình thuyết minh, thiếu thì dùng toạ độ của địa điểm/hiện vật. */
export function buildGeoTargets(items: AnyRow[], narrations: AnyRow[]): GeoTarget[] {
  const out: GeoTarget[] = [];
  for (const item of items) {
    const n = narrations.find((x) => x.targetType === item.targetType && String(x.targetId) === String(item.targetId));
    if (!n?.gps?.enabled) continue;
    const lat = finite(n.gps.latitude) ?? finite(item.latitude);
    const lng = finite(n.gps.longitude) ?? finite(item.longitude);
    if (lat === null || lng === null || Math.abs(lat) > 90 || Math.abs(lng) > 180) continue;
    const radius = finite(n.gps.radius);
    out.push({
      key: targetKey(item.targetType, item.targetId),
      lat,
      lng,
      radius: radius !== null && radius > 0 ? radius : DEFAULT_GPS_RADIUS_M,
    });
  }
  return out;
}

export class GeofenceTracker {
  private inside = new Set<string>();

  /**
   * Nhận một vị trí mới. Trả về khoá các vùng VỪA đi vào (gần nhất trước).
   * Đứng yên trong vùng thì không trả lại; ra xa hơn bán kính + REARM_MARGIN_M thì được kích hoạt lại.
   */
  update(lat: number, lng: number, targets: GeoTarget[], accuracy?: number | null): string[] {
    if (!Number.isFinite(lat) || !Number.isFinite(lng)) return [];
    if (typeof accuracy === "number" && accuracy > MAX_ACCURACY_M) return [];
    const alive = new Set(targets.map((t) => t.key));
    for (const key of [...this.inside]) if (!alive.has(key)) this.inside.delete(key);
    const entered: { key: string; d: number }[] = [];
    for (const t of targets) {
      const d = distanceMeters(lat, lng, t.lat, t.lng);
      if (d <= t.radius) {
        if (!this.inside.has(t.key)) {
          this.inside.add(t.key);
          entered.push({ key: t.key, d });
        }
      } else if (d > t.radius + REARM_MARGIN_M) {
        this.inside.delete(t.key);
      }
    }
    return entered.sort((a, b) => a.d - b.d).map((e) => e.key);
  }

  reset() {
    this.inside.clear();
  }
}

/* --------------------------- Bluetooth --------------------------- */

export type BeaconTarget = { key: string; name: string; mac: string; radius: number };
export type BeaconAdvert = {
  /** Android: địa chỉ MAC. iOS: mã ngẫu nhiên theo từng máy (không phải MAC) nên cần khớp theo tên. */
  id: string;
  name?: string | null;
  localName?: string | null;
  rssi?: number | null;
};

export const normalizeMac = (s: string | null | undefined) => String(s ?? "").replace(/[:\-\s]/g, "").toUpperCase();
const normalizeName = (s: string | null | undefined) => String(s ?? "").trim().toLowerCase();

export function buildBeaconTargets(items: AnyRow[], narrations: AnyRow[]): BeaconTarget[] {
  const out: BeaconTarget[] = [];
  for (const item of items) {
    const n = narrations.find((x) => x.targetType === item.targetType && String(x.targetId) === String(item.targetId));
    const b = n?.bluetooth;
    if (!b?.enabled) continue;
    const name = normalizeName(b.deviceName);
    const mac = normalizeMac(b.mac);
    if (!name && !mac) continue;
    const radius = finite(b.radius);
    out.push({
      key: targetKey(item.targetType, item.targetId),
      name,
      mac,
      radius: radius !== null && radius > 0 ? radius : DEFAULT_BEACON_RADIUS_M,
    });
  }
  return out;
}

/** Khớp gói quảng bá với cấu hình: ưu tiên MAC, sau đó tên thiết bị (không phân biệt hoa thường). */
export function matchBeacon(ad: BeaconAdvert, targets: BeaconTarget[]): BeaconTarget | null {
  const id = normalizeMac(ad.id);
  if (id) {
    const byMac = targets.find((t) => t.mac && t.mac === id);
    if (byMac) return byMac;
  }
  const names = [normalizeName(ad.name), normalizeName(ad.localName)].filter(Boolean);
  if (names.length) {
    const byName = targets.find((t) => t.name && names.includes(t.name));
    if (byName) return byName;
  }
  return null;
}

/**
 * Ước lượng khoảng cách (mét) theo mô hình suy hao: d = 10 ^ ((txPower - rssi) / (10 * n)).
 * txPower là RSSI đo ở 1 m (mặc định -59 dBm, phổ biến với beacon công suất thường), n ≈ 2 ngoài trời, 2.2–3 trong nhà.
 * Chỉ là ước lượng thô: tường, người, hướng ăng-ten làm lệch kết quả nhiều mét.
 */
export function estimateDistanceMeters(rssi: number, txPowerAt1m = -59, pathLoss = 2.2): number {
  return Math.pow(10, (txPowerAt1m - rssi) / (10 * pathLoss));
}

type BeaconState = { rssis: number[]; hits: number; lastSeen: number; inside: boolean };

export type BeaconOptions = {
  /** Số mẫu RSSI gần nhất dùng để lấy trung bình. */
  window?: number;
  /** Số gói liên tiếp phải nằm trong bán kính mới kích hoạt (lọc nhiễu). */
  enterHits?: number;
  /** Quá thời gian này (ms) không thấy beacon thì coi như đã rời đi. */
  lostAfterMs?: number;
  /** Khoảng cách ra khỏi vùng = bán kính × hệ số này mới cho kích hoạt lại. */
  exitFactor?: number;
  txPowerAt1m?: number;
  pathLoss?: number;
};

export class BeaconTracker {
  private states = new Map<string, BeaconState>();
  private o: Required<BeaconOptions>;

  constructor(options: BeaconOptions = {}) {
    this.o = {
      window: options.window ?? 4,
      enterHits: options.enterHits ?? 2,
      lostAfterMs: options.lostAfterMs ?? 15000,
      exitFactor: options.exitFactor ?? 1.5,
      txPowerAt1m: options.txPowerAt1m ?? -59,
      pathLoss: options.pathLoss ?? 2.2,
    };
  }

  /**
   * Xử lý một gói quảng bá. Trả về { key, distance } khi beacon VỪA đi vào vùng (cần phát thuyết minh),
   * ngược lại trả về null (không khớp, còn ở xa, hoặc đã kích hoạt rồi và vẫn đứng gần).
   */
  onAdvert(ad: BeaconAdvert, targets: BeaconTarget[], now = Date.now()): { key: string; distance: number } | null {
    this.expire(now);
    if (typeof ad.rssi !== "number" || !Number.isFinite(ad.rssi) || ad.rssi >= 0 || ad.rssi < -127) return null;
    const target = matchBeacon(ad, targets);
    if (!target) return null;
    let st = this.states.get(target.key);
    if (!st) {
      st = { rssis: [], hits: 0, lastSeen: now, inside: false };
      this.states.set(target.key, st);
    }
    st.lastSeen = now;
    st.rssis.push(ad.rssi);
    if (st.rssis.length > this.o.window) st.rssis.shift();
    const avg = st.rssis.reduce((a, b) => a + b, 0) / st.rssis.length;
    const distance = estimateDistanceMeters(avg, this.o.txPowerAt1m, this.o.pathLoss);
    if (distance <= target.radius) {
      st.hits += 1;
      if (!st.inside && st.hits >= this.o.enterHits) {
        st.inside = true;
        return { key: target.key, distance: Math.round(distance * 10) / 10 };
      }
    } else {
      st.hits = 0;
      if (distance > target.radius * this.o.exitFactor) st.inside = false;
    }
    return null;
  }

  private expire(now: number) {
    for (const [key, st] of this.states) if (now - st.lastSeen > this.o.lostAfterMs) this.states.delete(key);
  }

  reset() {
    this.states.clear();
  }
}
