package vn.commentary.backend.user.service;

import org.springframework.stereotype.Service;
import vn.commentary.backend.common.exception.ResourceNotFoundException;
import vn.commentary.backend.user.dto.Dtos.NearbyMatch;
import vn.commentary.backend.user.dto.Dtos.ResolveResult;
import vn.commentary.backend.user.model.Models.GeoConfig;
import vn.commentary.backend.user.model.Models.Narration;
import vn.commentary.backend.user.model.Models.TargetInfo;
import vn.commentary.backend.user.model.Models.TargetType;
import vn.commentary.backend.user.util.GeoUtils;
import vn.commentary.backend.user.util.TextNormalizer;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Nhận diện đối tượng từ QR, GPS hoặc Beacon Bluetooth (UC7.1.1, UC7.1.2, UC7.2.1). */
@Service
public class DiscoveryService {
    private final ContentService content;

    public DiscoveryService(ContentService content) {
        this.content = content;
    }

    /** Tìm đối tượng theo nội dung quét từ mã QR. */
    public ResolveResult resolveQr(String scanned) {
        String code = scanned == null ? "" : scanned.trim();
        if (code.isEmpty()) throw new IllegalArgumentException("Nội dung mã QR trống.");
        Map<String, TargetInfo> index = content.visibleTargetIndex();

        for (Narration n : content.narrations()) {
            if (!n.qr().enabled()) continue;
            boolean byCode = !n.qr().code().isBlank() && n.qr().code().equalsIgnoreCase(code);
            boolean byLink = !n.qr().link().isBlank() && n.qr().link().trim().equalsIgnoreCase(code);
            TargetInfo target = index.get(ContentService.key(n.targetType(), n.targetId()));
            if ((byCode || byLink) && target != null) return result(target, "qr");
        }

        // Liên kết dạng .../play?type=site&id=1&lang=en (do admin tạo trong QR cũ)
        Optional<TargetInfo> fromLink = parseLink(code).map(k -> index.get(k));
        if (fromLink.isPresent()) return result(fromLink.get(), "qr");

        throw new ResourceNotFoundException("Không nhận diện được mã QR này.");
    }

    /** Các đối tượng có vùng GPS chứa vị trí người dùng, gần nhất trước. */
    public List<NearbyMatch> nearby(double lat, double lng) {
        if (!GeoUtils.isValidLatLng(lat, lng)) throw new IllegalArgumentException("Toạ độ không hợp lệ.");
        Map<String, TargetInfo> index = content.visibleTargetIndex();
        List<NearbyMatch> matches = new ArrayList<>();
        for (Narration n : content.narrations()) {
            GeoConfig gps = n.gps();
            TargetInfo target = index.get(ContentService.key(n.targetType(), n.targetId()));
            if (target == null || !gps.enabled() || gps.radius() <= 0
                    || !GeoUtils.isValidLatLng(gps.latitude(), gps.longitude())) {
                continue;
            }
            double distance = GeoUtils.distanceMeters(lat, lng, gps.latitude(), gps.longitude());
            if (distance <= gps.radius()) {
                matches.add(new NearbyMatch(n.targetType().code(), n.targetId(), target.name(),
                        Math.round(distance * 10) / 10.0, gps.radius()));
            }
        }
        matches.sort(Comparator.comparingDouble(NearbyMatch::distanceMeters));
        return matches;
    }

    /** Tìm đối tượng theo địa chỉ MAC của Beacon. */
    public ResolveResult resolveBeacon(String mac) {
        String wanted = TextNormalizer.normalizeMac(mac);
        if (wanted.isEmpty()) throw new IllegalArgumentException("Thiếu địa chỉ MAC của Beacon.");
        Map<String, TargetInfo> index = content.visibleTargetIndex();
        for (Narration n : content.narrations()) {
            if (!n.bluetooth().enabled()) continue;
            if (TextNormalizer.normalizeMac(n.bluetooth().mac()).equals(wanted)) {
                TargetInfo target = index.get(ContentService.key(n.targetType(), n.targetId()));
                if (target != null) return result(target, "beacon");
            }
        }
        throw new ResourceNotFoundException("Không có thuyết minh nào gắn với Beacon này.");
    }

    private static ResolveResult result(TargetInfo target, String via) {
        return new ResolveResult(target.type().code(), target.id(), target.name(), via);
    }

    /** Trích khoá "site:1" / "artifact:2" từ liên kết có tham số type & id. */
    static Optional<String> parseLink(String text) {
        try {
            URI uri = URI.create(text.trim());
            String query = uri.getRawQuery();
            if (query == null) return Optional.empty();
            String type = null;
            String id = null;
            for (String pair : query.split("&")) {
                int eq = pair.indexOf('=');
                if (eq <= 0) continue;
                String k = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
                String v = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
                if ("type".equalsIgnoreCase(k)) type = v;
                if ("id".equalsIgnoreCase(k)) id = v;
            }
            if (type == null || id == null) return Optional.empty();
            TargetType t = TargetType.from(type);
            return Optional.of(ContentService.key(t, Long.parseLong(id.trim())));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }
}
