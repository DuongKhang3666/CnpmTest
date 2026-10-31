package vn.commentary.backend.config;

import org.springframework.web.cors.CorsConfiguration;
import vn.commentary.backend.user.util.NetworkPolicy;

import java.net.URI;
import java.util.List;

/**
 * Chỉ chấp nhận origin có host là localhost / IP nội bộ (10/8, 172.16/12, 192.168/16...) hoặc nằm trong danh sách
 * cho phép (nhập đúng như trình duyệt gửi, ví dụ {@code https://admin.example.com}); danh sách chứa {@code *} = cho tất cả.
 */
public class LanOnlyCorsConfiguration extends CorsConfiguration {
    private final List<String> extraOrigins;

    public LanOnlyCorsConfiguration(List<String> extraOrigins) {
        this.extraOrigins = extraOrigins.stream().map(o -> o.replaceAll("/+$", "")).toList();
    }

    @Override
    public String checkOrigin(String origin) {
        if (origin == null) return null;
        if (extraOrigins.contains("*")) return origin;
        if (extraOrigins.stream().anyMatch(o -> o.equalsIgnoreCase(origin))) return origin;
        try {
            URI uri = URI.create(origin);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (host == null || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) return null;
            if (host.startsWith("[") && host.endsWith("]")) host = host.substring(1, host.length() - 1);
            return NetworkPolicy.isPrivateOrLoopback(host) ? origin : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
