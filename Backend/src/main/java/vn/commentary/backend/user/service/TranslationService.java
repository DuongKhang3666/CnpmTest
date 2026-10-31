package vn.commentary.backend.user.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.commentary.backend.user.repository.TranslationCacheRepository;
import vn.commentary.backend.user.translation.TranslationException;
import vn.commentary.backend.user.translation.Translator;
import vn.commentary.backend.user.util.TextChunker;
import vn.commentary.backend.user.util.TokenHasher;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/** Dịch nội dung, có cache trong DB; khi dịch lỗi vẫn trả về văn bản gốc kèm cờ {@code translated=false}. */
@Service
public class TranslationService {
    private static final Logger log = LoggerFactory.getLogger(TranslationService.class);
    private static final Pattern LANG = Pattern.compile("^[a-zA-Z]{2,3}(?:-[a-zA-Z0-9]{2,8})?$");
    static final int CHUNK_SIZE = 4000;

    /** Kết quả dịch: {@code translated=false} nghĩa là {@code text} là bản gốc. */
    public record Result(String text, boolean translated, String error) {
    }

    private final Translator translator;
    private final TranslationCacheRepository cache;
    private final Clock clock;

    public TranslationService(Translator translator, TranslationCacheRepository cache, Clock clock) {
        this.translator = translator;
        this.cache = cache;
        this.clock = clock;
    }

    public static boolean isValidLanguage(String code) {
        return code != null && LANG.matcher(code.trim()).matches();
    }

    public Result translate(String text, String source, String target) {
        if (text == null || text.isBlank()) return new Result(text == null ? "" : text, false, null);
        if (!isValidLanguage(target)) throw new IllegalArgumentException("Mã ngôn ngữ đích không hợp lệ.");
        String tgt = target.trim();
        if (source != null && sameLanguage(source, tgt)) return new Result(text, false, null);

        String key = TokenHasher.sha256(tgt.toLowerCase(Locale.ROOT) + "\n" + text);
        Optional<String> cached = cache.find(key);
        if (cached.isPresent()) return new Result(cached.get(), true, null);

        try {
            List<String> parts = new ArrayList<>();
            for (String chunk : TextChunker.split(text, CHUNK_SIZE)) {
                parts.add(translator.translate(chunk, source, tgt));
            }
            String translated = String.join("\n", parts);
            cache.save(key, tgt, translated, clock.millis());
            return new Result(translated, true, null);
        } catch (TranslationException e) {
            log.warn("Dịch thất bại sang '{}': {}", tgt, e.getMessage());
            return new Result(text, false, e.getMessage());
        }
    }

    /** So sánh hai mã ngôn ngữ theo phần gốc: vi == vi-VN, en != vi. */
    public static boolean sameLanguage(String a, String b) {
        return base(a).equals(base(b));
    }

    private static String base(String code) {
        String lower = code.trim().toLowerCase(Locale.ROOT);
        int dash = lower.indexOf('-');
        return dash > 0 ? lower.substring(0, dash) : lower;
    }
}
