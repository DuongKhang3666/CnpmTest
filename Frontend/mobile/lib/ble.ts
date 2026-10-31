/* =====================================================================
   lib/ble.ts — quét Bluetooth beacon bằng react-native-ble-plx.
   Thư viện này là module native: CHỈ chạy trên bản build riêng (`npx expo run:android` / EAS build),
   KHÔNG chạy trong Expo Go. Ở Expo Go hàm trả về thông báo rõ ràng thay vì làm app lỗi.
   ===================================================================== */
import { PermissionsAndroid, Platform } from "react-native";
import type { BeaconAdvert } from "./proximity";

export type BleScan = { stop: () => void };
export type BleResult = { ok: true; scan: BleScan } | { ok: false; message: string };

type Deps = {
  /** Nạp module native (chèn giả khi kiểm thử). */
  loadModule?: () => any;
  /** Xin quyền Bluetooth (chèn giả khi kiểm thử). */
  requestPermissions?: () => Promise<boolean>;
};

const MISSING_NATIVE =
  "Bản app này chưa có Bluetooth. Quét beacon cần bản cài riêng (chạy `npx expo run:android`), không dùng được trong Expo Go.";

function defaultLoadModule() {
  // require trong hàm + try/catch: Expo Go không có module native nên không được làm hỏng cả app.
  // eslint-disable-next-line @typescript-eslint/no-require-imports
  return require("react-native-ble-plx");
}

/** Android 12+ cần BLUETOOTH_SCAN/CONNECT; Android 11 trở xuống cần quyền vị trí để quét BLE. iOS hỏi khi bắt đầu quét. */
export async function requestBlePermissions(): Promise<boolean> {
  if (Platform.OS !== "android") return true;
  const api = typeof Platform.Version === "number" ? Platform.Version : parseInt(String(Platform.Version), 10);
  const P = PermissionsAndroid.PERMISSIONS as Record<string, any>;
  const wanted =
    api >= 31
      ? [P.BLUETOOTH_SCAN, P.BLUETOOTH_CONNECT, P.ACCESS_FINE_LOCATION]
      : [P.ACCESS_FINE_LOCATION];
  const res = (await PermissionsAndroid.requestMultiple(wanted.filter(Boolean))) as Record<string, string>;
  return wanted.every((p) => res[p] === PermissionsAndroid.RESULTS.GRANTED);
}

/**
 * Bắt đầu quét. onAdvert nhận từng gói quảng bá; onStatus báo tình trạng (tắt Bluetooth, lỗi quét...).
 * Gọi scan.stop() để dừng và giải phóng.
 */
export async function startBeaconScan(
  onAdvert: (ad: BeaconAdvert) => void,
  onStatus: (message: string) => void,
  deps: Deps = {},
): Promise<BleResult> {
  const load = deps.loadModule ?? defaultLoadModule;
  const ask = deps.requestPermissions ?? requestBlePermissions;

  let mod: any;
  let manager: any;
  try {
    mod = load();
    manager = new mod.BleManager();
  } catch {
    return { ok: false, message: MISSING_NATIVE };
  }

  let granted = false;
  try {
    granted = await ask();
  } catch {
    granted = false;
  }
  if (!granted) {
    try { manager.destroy?.(); } catch {}
    return { ok: false, message: "Cần cấp quyền Bluetooth (và Vị trí) để quét beacon. Hãy cho phép trong cài đặt ứng dụng." };
  }

  let scanning = false;
  let stopped = false;
  const startScan = () => {
    if (scanning || stopped) return;
    scanning = true;
    Promise.resolve(
      manager.startDeviceScan(null, { allowDuplicates: true, scanMode: mod.ScanMode?.LowLatency ?? 2 }, (error: any, device: any) => {
        if (error) {
          scanning = false;
          onStatus("Quét Bluetooth bị gián đoạn: " + (error?.message || error));
          return;
        }
        if (!device) return;
        onAdvert({ id: device.id, name: device.name, localName: device.localName, rssi: device.rssi });
      }),
    )
      .catch((e: any) => {
        scanning = false;
        onStatus("Không bắt đầu được quét Bluetooth: " + (e?.message || e));
      });
  };
  const stopScan = () => {
    if (!scanning) return;
    scanning = false;
    try { manager.stopDeviceScan(); } catch {}
  };

  // Bluetooth bật/tắt giữa chừng: tự dừng, bật lại thì quét tiếp.
  const sub = manager.onStateChange((state: string) => {
    if (state === "PoweredOn") {
      onStatus("Đang quét beacon Bluetooth gần bạn...");
      startScan();
    } else {
      stopScan();
      if (state === "PoweredOff") onStatus("Bluetooth đang tắt. Hãy bật Bluetooth để nhận diện beacon.");
      else if (state === "Unauthorized") onStatus("Ứng dụng chưa được phép dùng Bluetooth.");
      else if (state === "Unsupported") onStatus("Thiết bị này không hỗ trợ Bluetooth Low Energy.");
    }
  }, true);

  return {
    ok: true,
    scan: {
      stop: () => {
        stopped = true;
        stopScan();
        try { sub?.remove(); } catch {}
        try { manager.destroy?.(); } catch {}
      },
    },
  };
}
