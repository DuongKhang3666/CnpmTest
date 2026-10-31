// Kiểm thử logic thuyết minh tự động (GPS + Bluetooth) mà không cần điện thoại.
// Chạy:  npm test   (trong thư mục Frontend/mobile)
import assert from "node:assert/strict";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import Module, { createRequire } from "node:module";
import { fileURLToPath } from "node:url";

const here = path.dirname(fileURLToPath(import.meta.url));
const require = createRequire(import.meta.url);
const ts = require("typescript");

// --- biên dịch lib/*.ts sang CommonJS trong thư mục tạm
const out = fs.mkdtempSync(path.join(os.tmpdir(), "prox-"));
for (const f of ["proximity", "ble"]) {
  const src = fs.readFileSync(path.join(here, "..", "lib", `${f}.ts`), "utf8");
  const js = ts.transpileModule(src, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 } }).outputText;
  fs.writeFileSync(path.join(out, `${f}.js`), js);
}
// --- giả lập react-native cho ble.ts
const rn = {
  Platform: { OS: "android", Version: 33 },
  PermissionsAndroid: {
    PERMISSIONS: {
      BLUETOOTH_SCAN: "android.permission.BLUETOOTH_SCAN",
      BLUETOOTH_CONNECT: "android.permission.BLUETOOTH_CONNECT",
      ACCESS_FINE_LOCATION: "android.permission.ACCESS_FINE_LOCATION",
    },
    RESULTS: { GRANTED: "granted", DENIED: "denied" },
    asked: [],
    deny: false,
    async requestMultiple(list) {
      this.asked = list;
      return Object.fromEntries(list.map((p) => [p, this.deny ? "denied" : "granted"]));
    },
  },
};
const origLoad = Module._load;
Module._load = function (request, ...rest) {
  return request === "react-native" ? rn : origLoad.call(this, request, ...rest);
};
const P = require(path.join(out, "proximity.js"));
const B = require(path.join(out, "ble.js"));

let pass = 0, fail = 0;
const tests = [];
const test = (name, fn) => tests.push([name, fn]);

/* ===================== GPS ===================== */
const DINH = { lat: 10.777108, lng: 106.695441 };
const northOf = (p, m) => ({ lat: p.lat + m / 111320, lng: p.lng }); // đi lên phía bắc m mét
const target = (key, p, radius) => ({ key, lat: p.lat, lng: p.lng, radius });

test("distanceMeters: 100 m về phía bắc ≈ 100", () => {
  const d = P.distanceMeters(DINH.lat, DINH.lng, northOf(DINH, 100).lat, DINH.lng);
  assert.ok(Math.abs(d - 100) < 1, String(d));
});
test("buildGeoTargets: lấy toạ độ cấu hình, thiếu thì dùng toạ độ địa điểm, bỏ qua mục tắt/sai", () => {
  const items = [
    { targetType: "site", targetId: 1, latitude: 1, longitude: 2 },
    { targetType: "site", targetId: 2, latitude: 10.5, longitude: 106.5 },
    { targetType: "site", targetId: 3, latitude: 3, longitude: 4 },
    { targetType: "artifact", targetId: 1 },
    { targetType: "site", targetId: 5 },
  ];
  const narr = [
    { targetType: "site", targetId: 1, gps: { enabled: true, latitude: 10.77, longitude: 106.69, radius: 120 } },
    { targetType: "site", targetId: 2, gps: { enabled: true, latitude: null, longitude: null, radius: 0 } },
    { targetType: "site", targetId: 3, gps: { enabled: false, latitude: 3, longitude: 4, radius: 50 } },
    { targetType: "artifact", targetId: 1, gps: { enabled: true, latitude: 10.1, longitude: 106.1, radius: 30 } },
    { targetType: "site", targetId: 5, gps: { enabled: true, latitude: 999, longitude: 0, radius: 10 } },
  ];
  const t = P.buildGeoTargets(items, narr);
  assert.deepEqual(t.map((x) => x.key), ["site:1", "site:2", "artifact:1"]);
  assert.equal(t[0].radius, 120);
  assert.deepEqual([t[1].lat, t[1].lng, t[1].radius], [10.5, 106.5, 50]); // thiếu toạ độ + bán kính -> mặc định
  assert.equal(t[2].radius, 30); // hiện vật cũng có vùng GPS
});
test("GPS: ở xa không kích hoạt; đi vào vùng kích hoạt đúng một lần", () => {
  const g = new P.GeofenceTracker(), t = [target("site:1", DINH, 120)];
  assert.deepEqual(g.update(21.0285, 105.8542, t), []);
  assert.deepEqual(g.update(northOf(DINH, 300).lat, DINH.lng, t), []);
  assert.deepEqual(g.update(northOf(DINH, 100).lat, DINH.lng, t), ["site:1"]);
  assert.deepEqual(g.update(northOf(DINH, 90).lat, DINH.lng, t), []);
  assert.deepEqual(g.update(DINH.lat, DINH.lng, t), []); // đứng yên trong vùng: không đọc lặp
});
test("GPS: ra khỏi vùng nhưng chưa quá +20 m thì vào lại KHÔNG đọc lại", () => {
  const g = new P.GeofenceTracker(), t = [target("site:1", DINH, 120)];
  g.update(DINH.lat, DINH.lng, t);
  assert.deepEqual(g.update(northOf(DINH, 135).lat, DINH.lng, t), []); // 135 m: ngoài vùng, trong biên 140 m
  assert.deepEqual(g.update(northOf(DINH, 110).lat, DINH.lng, t), []);
});
test("GPS: ra xa hơn bán kính + 20 m rồi vào lại -> đọc lại", () => {
  const g = new P.GeofenceTracker(), t = [target("site:1", DINH, 120)];
  g.update(DINH.lat, DINH.lng, t);
  assert.deepEqual(g.update(northOf(DINH, 145).lat, DINH.lng, t), []); // > 140 m: tái kích hoạt
  assert.deepEqual(g.update(northOf(DINH, 100).lat, DINH.lng, t), ["site:1"]);
});
test("GPS: hai vùng chồng nhau -> vùng gần hơn đứng trước", () => {
  const near = target("artifact:1", northOf(DINH, 10), 100), far = target("site:1", northOf(DINH, 80), 100);
  const g = new P.GeofenceTracker();
  assert.deepEqual(g.update(DINH.lat, DINH.lng, [far, near]), ["artifact:1", "site:1"]);
});
test("GPS: điểm kém chính xác (>75 m) bị bỏ qua, điểm tốt được nhận", () => {
  const g = new P.GeofenceTracker(), t = [target("site:1", DINH, 120)];
  assert.deepEqual(g.update(DINH.lat, DINH.lng, t, 200), []);
  assert.deepEqual(g.update(DINH.lat, DINH.lng, t, 20), ["site:1"]);
});
test("GPS: toạ độ không hợp lệ (NaN) không gây lỗi", () => {
  assert.deepEqual(new P.GeofenceTracker().update(NaN, 1, [target("a", DINH, 50)]), []);
});
test("GPS: vùng bị admin gỡ thì tracker quên trạng thái, bật lại được kích hoạt", () => {
  const g = new P.GeofenceTracker(), t = [target("site:1", DINH, 120)];
  g.update(DINH.lat, DINH.lng, t);
  g.update(DINH.lat, DINH.lng, []);
  assert.deepEqual(g.update(DINH.lat, DINH.lng, t), ["site:1"]);
});

/* ================== Bluetooth: khớp & khoảng cách ================== */
const bt = (key, name, mac, radius = 5) => ({ key, name, mac: P.normalizeMac(mac), radius });
test("normalizeMac bỏ dấu ':' '-' khoảng trắng và viết hoa", () => {
  assert.equal(P.normalizeMac("aa:bb-cc dd:ee:ff"), "AABBCCDDEEFF");
  assert.equal(P.normalizeMac(null), "");
});
test("buildBeaconTargets: bán kính mặc định 5 m; bỏ mục tắt hoặc không có tên lẫn MAC", () => {
  const items = [1, 2, 3, 4].map((id) => ({ targetType: "artifact", targetId: id }));
  const narr = [
    { targetType: "artifact", targetId: 1, bluetooth: { enabled: true, deviceName: "Xe Tăng", mac: "", radius: 0 } },
    { targetType: "artifact", targetId: 2, bluetooth: { enabled: false, deviceName: "x", mac: "", radius: 5 } },
    { targetType: "artifact", targetId: 3, bluetooth: { enabled: true, deviceName: "", mac: "", radius: 5 } },
    { targetType: "artifact", targetId: 4, bluetooth: { enabled: true, deviceName: "", mac: "AA:BB:CC:DD:EE:FF", radius: 8 } },
  ];
  const t = P.buildBeaconTargets(items, narr);
  assert.deepEqual(t.map((x) => x.key), ["artifact:1", "artifact:4"]);
  assert.deepEqual([t[0].name, t[0].radius, t[1].mac, t[1].radius], ["xe tăng", 5, "AABBCCDDEEFF", 8]);
});
test("matchBeacon: theo MAC (ưu tiên), theo tên (không phân biệt hoa thường), theo localName, không khớp", () => {
  const targets = [bt("a", "xe tăng", ""), bt("b", "", "AA:BB:CC:DD:EE:FF")];
  assert.equal(P.matchBeacon({ id: "aa:bb:cc:dd:ee:ff", name: null }, targets).key, "b");
  assert.equal(P.matchBeacon({ id: "11:22", name: "XE TĂNG " }, targets).key, "a");
  assert.equal(P.matchBeacon({ id: "11:22", name: null, localName: "Xe Tăng" }, targets).key, "a");
  assert.equal(P.matchBeacon({ id: "11:22", name: "Loa JBL" }, targets), null);
  assert.equal(P.matchBeacon({ id: "", name: "" }, [bt("c", "", "")]), null);
});
test("estimateDistanceMeters: -59 dBm ≈ 1 m, yếu hơn thì xa hơn, ~5 m quanh -74 dBm", () => {
  assert.ok(Math.abs(P.estimateDistanceMeters(-59) - 1) < 0.01);
  assert.ok(P.estimateDistanceMeters(-80) > P.estimateDistanceMeters(-70));
  const d = P.estimateDistanceMeters(-74);
  assert.ok(d > 4.5 && d < 5.5, String(d));
});

/* ================== Bluetooth: BeaconTracker ================== */
const T0 = 1_000_000;
const ad = (rssi, name = "xe tăng", id = "AA:BB") => ({ id, name, rssi });
test("Beacon: một gói gần chưa đủ, hai gói liên tiếp gần -> kích hoạt một lần", () => {
  const tr = new P.BeaconTracker(), t = [bt("artifact:1", "xe tăng", "")];
  assert.equal(tr.onAdvert(ad(-60), t, T0), null);
  const hit = tr.onAdvert(ad(-60), t, T0 + 500);
  assert.equal(hit.key, "artifact:1");
  assert.ok(hit.distance <= 5);
  assert.equal(tr.onAdvert(ad(-60), t, T0 + 1000), null); // đứng gần: không lặp
});
test("Beacon: ở xa (-90 dBm) không kích hoạt", () => {
  const tr = new P.BeaconTracker(), t = [bt("a", "xe tăng", "")];
  for (let i = 0; i < 6; i++) assert.equal(tr.onAdvert(ad(-90), t, T0 + i * 500), null);
});
test("Beacon: thiết bị lạ, RSSI sai (0, dương, null) bị bỏ qua", () => {
  const tr = new P.BeaconTracker(), t = [bt("a", "xe tăng", "")];
  assert.equal(tr.onAdvert(ad(-50, "Loa JBL"), t, T0), null);
  for (const r of [0, 5, null, undefined, NaN, -200]) assert.equal(tr.onAdvert(ad(r), t, T0), null);
});
test("Beacon: một gói nhiễu yếu giữa chuỗi gói mạnh không làm mất trạng thái 'đang ở gần'", () => {
  const tr = new P.BeaconTracker(), t = [bt("a", "xe tăng", "")];
  tr.onAdvert(ad(-60), t, T0); tr.onAdvert(ad(-60), t, T0 + 500);
  assert.equal(tr.onAdvert(ad(-85), t, T0 + 1000), null); // outlier: trung bình vẫn gần
  assert.equal(tr.onAdvert(ad(-60), t, T0 + 1500), null); // không bị đọc lại
});
test("Beacon: đi xa (> bán kính × 1.5) rồi quay lại -> đọc lại", () => {
  const tr = new P.BeaconTracker({ window: 2 }), t = [bt("a", "xe tăng", "")];
  tr.onAdvert(ad(-60), t, T0); assert.ok(tr.onAdvert(ad(-60), t, T0 + 500));
  tr.onAdvert(ad(-95), t, T0 + 1000); tr.onAdvert(ad(-95), t, T0 + 1500); // đã xa
  assert.equal(tr.onAdvert(ad(-60), t, T0 + 2000), null); // trung bình còn kéo theo gói yếu trước đó
  assert.equal(tr.onAdvert(ad(-60), t, T0 + 2500), null); // mới 1 lần liên tiếp trong bán kính
  assert.ok(tr.onAdvert(ad(-60), t, T0 + 3000), "phải kích hoạt lại sau khi đã rời xa");
});
test("Beacon: mất tín hiệu > 15 giây rồi thấy lại -> đọc lại", () => {
  const tr = new P.BeaconTracker(), t = [bt("a", "xe tăng", "")];
  tr.onAdvert(ad(-60), t, T0); assert.ok(tr.onAdvert(ad(-60), t, T0 + 500));
  tr.onAdvert(ad(-60), t, T0 + 30_000);
  assert.ok(tr.onAdvert(ad(-60), t, T0 + 30_500));
});
test("Beacon: hai beacon khác nhau theo dõi độc lập", () => {
  const tr = new P.BeaconTracker(), t = [bt("a", "xe tăng", ""), bt("b", "máy bay", "")];
  tr.onAdvert(ad(-60, "xe tăng"), t, T0);
  tr.onAdvert(ad(-60, "máy bay", "CC"), t, T0);
  assert.equal(tr.onAdvert(ad(-60, "xe tăng"), t, T0 + 500).key, "a");
  assert.equal(tr.onAdvert(ad(-60, "máy bay", "CC"), t, T0 + 500).key, "b");
});

/* ================== ble.ts với module giả ================== */
function fakeModule({ initial = "PoweredOn", throwOnNew = false } = {}) {
  const calls = { scan: 0, stop: 0, destroy: 0, removed: 0, opts: null };
  let cb = null, stateCb = null;
  class BleManager {
    constructor() { if (throwOnNew) throw new Error("native module missing"); }
    onStateChange(fn, emit) { stateCb = fn; if (emit) fn(initial); return { remove: () => { calls.removed++; } }; }
    startDeviceScan(uuids, opts, fn) { calls.scan++; calls.opts = opts; cb = fn; return Promise.resolve(); }
    stopDeviceScan() { calls.stop++; }
    destroy() { calls.destroy++; }
  }
  return { mod: { BleManager, ScanMode: { LowLatency: 2 } }, calls, emit: (e, d) => cb && cb(e, d), setState: (s) => stateCb(s) };
}
const flush = () => new Promise((r) => setTimeout(r, 0));

test("BLE: không có module native (Expo Go) -> thông báo rõ ràng, không ném lỗi", async () => {
  const r = await B.startBeaconScan(() => {}, () => {}, { loadModule: () => { throw new Error("x"); }, requestPermissions: async () => true });
  assert.equal(r.ok, false);
  assert.match(r.message, /Expo Go/);
});
test("BLE: new BleManager() ném lỗi (module native thiếu) -> báo không hỗ trợ", async () => {
  const f = fakeModule({ throwOnNew: true });
  const r = await B.startBeaconScan(() => {}, () => {}, { loadModule: () => f.mod, requestPermissions: async () => true });
  assert.equal(r.ok, false);
  assert.match(r.message, /expo run:android/);
});
test("BLE: bị từ chối quyền -> không quét, giải phóng manager", async () => {
  const f = fakeModule();
  const r = await B.startBeaconScan(() => {}, () => {}, { loadModule: () => f.mod, requestPermissions: async () => false });
  assert.equal(r.ok, false);
  assert.match(r.message, /quyền/i);
  assert.equal(f.calls.scan, 0);
  assert.equal(f.calls.destroy, 1);
});
test("BLE: Bluetooth tắt -> báo, không quét; bật lại -> quét và chuyển tiếp gói quảng bá", async () => {
  const f = fakeModule({ initial: "PoweredOff" }), status = [], ads = [];
  const r = await B.startBeaconScan((a) => ads.push(a), (m) => status.push(m), { loadModule: () => f.mod, requestPermissions: async () => true });
  assert.equal(r.ok, true);
  assert.equal(f.calls.scan, 0);
  assert.match(status.join("|"), /Bluetooth đang tắt/);
  f.setState("PoweredOn"); await flush();
  assert.equal(f.calls.scan, 1);
  assert.equal(f.calls.opts.allowDuplicates, true);
  assert.equal(f.calls.opts.scanMode, 2);
  f.emit(null, { id: "AA:BB", name: "Xe Tăng", localName: null, rssi: -62 });
  f.emit(null, null);
  assert.deepEqual(ads, [{ id: "AA:BB", name: "Xe Tăng", localName: null, rssi: -62 }]);
  f.setState("PoweredOff"); assert.equal(f.calls.stop, 1);
  f.setState("PoweredOn"); await flush(); assert.equal(f.calls.scan, 2);
});
test("BLE: lỗi giữa chừng được báo; stop() dừng quét, gỡ lắng nghe, huỷ manager", async () => {
  const f = fakeModule(), status = [];
  const r = await B.startBeaconScan(() => {}, (m) => status.push(m), { loadModule: () => f.mod, requestPermissions: async () => true });
  f.emit({ message: "scan failed" }, null);
  assert.match(status.join("|"), /scan failed/);
  r.scan.stop();
  assert.equal(f.calls.removed, 1);
  assert.equal(f.calls.destroy, 1);
  r.scan.stop(); // gọi lần hai không gây lỗi
});
test("BLE: quyền Android 12+ (BLUETOOTH_SCAN/CONNECT + vị trí) và Android ≤11 (chỉ vị trí)", async () => {
  rn.Platform.Version = 33; rn.PermissionsAndroid.deny = false;
  assert.equal(await B.requestBlePermissions(), true);
  assert.deepEqual(rn.PermissionsAndroid.asked.map((p) => p.split(".").pop()), ["BLUETOOTH_SCAN", "BLUETOOTH_CONNECT", "ACCESS_FINE_LOCATION"]);
  rn.Platform.Version = 30;
  assert.equal(await B.requestBlePermissions(), true);
  assert.deepEqual(rn.PermissionsAndroid.asked.map((p) => p.split(".").pop()), ["ACCESS_FINE_LOCATION"]);
  rn.PermissionsAndroid.deny = true;
  assert.equal(await B.requestBlePermissions(), false);
  rn.Platform.OS = "ios";
  assert.equal(await B.requestBlePermissions(), true);
  rn.Platform.OS = "android"; rn.PermissionsAndroid.deny = false;
});

for (const [name, fn] of tests) {
  try { await fn(); pass++; console.log("PASS  " + name); }
  catch (e) { fail++; console.log("FAIL  " + name + "\n      " + (e.message || e).split("\n")[0]); }
}
console.log(`\n${pass} đạt, ${fail} lỗi`);
fs.rmSync(out, { recursive: true, force: true });
process.exit(fail ? 1 : 0);
