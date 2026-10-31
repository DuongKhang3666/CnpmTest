package vn.commentary.backend.user.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Nhận diện tên thiết bị/trình duyệt từ User-Agent (port từ server.mjs cũ). */
public final class DeviceDescriber {
    public record Description(String name, String browser) {
    }

    private static final Pattern ANDROID_MODEL = Pattern.compile("Android [\\d.]+; ([^;)]+)");

    private DeviceDescriber() {
    }

    public static Description describe(String ua) {
        String agent = ua == null ? "" : ua;
        String os = "Thiết bị";
        if (agent.contains("iPhone")) os = "iPhone";
        else if (agent.contains("iPad")) os = "iPad";
        else if (agent.contains("Android")) os = "Android";
        else if (agent.contains("Windows")) os = "Windows";
        else if (agent.contains("Mac OS X") || agent.contains("Macintosh")) os = "macOS";
        else if (agent.contains("Linux")) os = "Linux";

        Matcher m = ANDROID_MODEL.matcher(agent);
        if (m.find()) {
            String model = m.group(1).trim();
            if (model.length() > 1 && !"K".equals(model)) os = model;
        }

        String browser = "Trình duyệt";
        if (agent.contains("Edg/")) browser = "Edge";
        else if (agent.contains("OPR/") || agent.contains("Opera")) browser = "Opera";
        else if (agent.contains("CriOS") || agent.contains("Chrome/")) browser = "Chrome";
        else if (agent.contains("FxiOS") || agent.contains("Firefox/")) browser = "Firefox";
        else if (agent.contains("Safari/")) browser = "Safari";
        return new Description(os, browser);
    }
}
