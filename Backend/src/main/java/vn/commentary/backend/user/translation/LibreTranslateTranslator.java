package vn.commentary.backend.user.translation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;

/** Gọi dịch vụ LibreTranslate tự triển khai (giống bản Node cũ), có thử lại một lần khi lỗi mạng. */
@Component
public class LibreTranslateTranslator implements Translator {
    private static final Map<String, String> ALIASES = Map.of("zh-cn", "zh", "zh-tw", "zt", "zh-hans", "zh",
            "zh-hant", "zt");

    private final ObjectMapper mapper;
    private final HttpClient http;
    private final boolean enabled;
    private final String baseUrl;
    private final String apiKey;
    private final Duration timeout;

    public LibreTranslateTranslator(ObjectMapper mapper,
                                    @Value("${user.translate.enabled:true}") boolean enabled,
                                    @Value("${user.translate.url:http://127.0.0.1:5000}") String baseUrl,
                                    @Value("${user.translate.api-key:}") String apiKey,
                                    @Value("${user.translate.timeout-seconds:30}") long timeoutSeconds) {
        this.mapper = mapper;
        this.enabled = enabled;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.apiKey = apiKey;
        this.timeout = Duration.ofSeconds(Math.max(1, timeoutSeconds));
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @Override
    public String translate(String text, String source, String target) throws TranslationException {
        if (!enabled) throw new TranslationException("Dịch vụ dịch thuật đang tắt.");
        TranslationException last = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                return call(text, source, target);
            } catch (ClientError e) {
                throw new TranslationException(e.getMessage());
            } catch (TranslationException e) {
                last = e;
                sleepQuietly(500);
            }
        }
        throw last;
    }

    private String call(String text, String source, String target) throws TranslationException {
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("q", text);
            body.put("source", source == null || source.isBlank() || "auto".equalsIgnoreCase(source)
                    ? "auto" : normalize(source));
            body.put("target", normalize(target));
            body.put("format", "text");
            if (apiKey != null && !apiKey.isBlank()) body.put("api_key", apiKey);

            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/translate"))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode payload = response.body() == null || response.body().isBlank()
                    ? mapper.createObjectNode() : mapper.readTree(response.body());
            int status = response.statusCode();
            if (status >= 400 && status < 500) {
                throw new ClientError(payload.path("error").asText("LibreTranslate HTTP " + status));
            }
            if (status >= 300) {
                throw new TranslationException(payload.path("error").asText("LibreTranslate HTTP " + status));
            }
            String translated = payload.path("translatedText").asText("");
            if (translated.isBlank()) throw new TranslationException("LibreTranslate không trả về bản dịch.");
            return translated;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TranslationException("Yêu cầu dịch bị gián đoạn.", e);
        } catch (IOException e) {
            throw new TranslationException("Không kết nối được dịch vụ dịch tại " + baseUrl + ".", e);
        }
    }

    /** Chuyển mã ngôn ngữ về dạng LibreTranslate dùng: zh-CN→zh, zh-TW→zt, en-US→en. */
    static String normalize(String code) {
        String lower = code.trim().toLowerCase(Locale.ROOT);
        if (ALIASES.containsKey(lower)) return ALIASES.get(lower);
        int dash = lower.indexOf('-');
        return dash > 0 ? lower.substring(0, dash) : lower;
    }

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Lỗi 4xx từ dịch vụ: không thử lại. */
    private static final class ClientError extends TranslationException {
        ClientError(String message) {
            super(message);
        }
    }
}
