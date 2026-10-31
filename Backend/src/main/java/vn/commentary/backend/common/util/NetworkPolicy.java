package vn.commentary.backend.common.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Tiện ích mạng cho luồng quét QR: kiểm tra thiết bị có nằm trong mạng nội bộ (LAN) hay không
 * và liệt kê địa chỉ LAN của máy chủ để admin tạo mã QR (thay cho {@code isLanClient}/{@code lanInterfaces}
 * của server.mjs).
 */
public final class NetworkPolicy {
    private NetworkPolicy() {
    }

    /** Một địa chỉ IPv4 LAN của máy chủ. */
    public record LanAddress(String name, String address) {
    }

    /** IP của thiết bị thuộc mạng nội bộ: loopback, IPv4 riêng (10/8, 172.16/12, 192.168/16), link-local, IPv6 ULA. */
    public static boolean isPrivateOrLoopback(String rawIp) {
        if (rawIp == null || rawIp.isBlank()) return false;
        String ip = rawIp.trim().toLowerCase(Locale.ROOT).replaceFirst("^::ffff:", "");
        int zone = ip.indexOf('%');
        if (zone >= 0) ip = ip.substring(0, zone);
        if (ip.equals("::1") || ip.equals("0:0:0:0:0:0:0:1") || ip.equals("localhost")) return true;
        if (ip.startsWith("fe80:") || ip.startsWith("fc") || ip.startsWith("fd")) return ip.contains(":");
        String[] parts = ip.split("\\.");
        if (parts.length != 4) return false;
        int[] p = new int[4];
        try {
            for (int i = 0; i < 4; i++) {
                p[i] = Integer.parseInt(parts[i]);
                if (p[i] < 0 || p[i] > 255) return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
        return p[0] == 127
                || p[0] == 10
                || (p[0] == 172 && p[1] >= 16 && p[1] <= 31)
                || (p[0] == 192 && p[1] == 168)
                || (p[0] == 169 && p[1] == 254);
    }

    /** Địa chỉ IPv4 riêng của máy chủ, ưu tiên 192.168.x.x rồi 10.x.x.x. */
    public static List<LanAddress> lanAddresses() {
        List<LanAddress> result = new ArrayList<>();
        try {
            for (NetworkInterface nic : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!nic.isUp() || nic.isLoopback() || nic.isVirtual()) continue;
                for (InetAddress addr : Collections.list(nic.getInetAddresses())) {
                    if (addr instanceof Inet4Address && isPrivateOrLoopback(addr.getHostAddress())
                            && !addr.isLoopbackAddress() && !addr.isLinkLocalAddress()) {
                        result.add(new LanAddress(nic.getDisplayName(), addr.getHostAddress()));
                    }
                }
            }
        } catch (Exception ignored) {
            // không liệt kê được card mạng: trả danh sách rỗng, admin dùng địa chỉ trang đang mở
        }
        result.sort((a, b) -> Integer.compare(rank(a.address()), rank(b.address())));
        return result;
    }

    private static int rank(String ip) {
        return ip.startsWith("192.168.") ? 0 : ip.startsWith("10.") ? 1 : 2;
    }
}
