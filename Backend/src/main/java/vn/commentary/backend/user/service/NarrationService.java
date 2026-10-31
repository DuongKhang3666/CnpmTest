package vn.commentary.backend.user.service;

import org.springframework.stereotype.Service;
import vn.commentary.backend.exception.ResourceNotFoundException;
import vn.commentary.backend.user.dto.Dtos.NarrationDto;
import vn.commentary.backend.user.model.Models.Language;
import vn.commentary.backend.user.model.Models.Narration;
import vn.commentary.backend.user.model.Models.TargetInfo;
import vn.commentary.backend.user.model.Models.TargetType;

import java.util.Locale;
import java.util.Optional;

/**
 * Tạo nội dung thuyết minh cho một đối tượng theo ngôn ngữ người dùng chọn (UC7.2, UC7.3, UC7.4).
 * Thứ tự ưu tiên bản dịch: bản dịch admin đã nhập → bản dịch trong cache → dịch mới qua dịch vụ dịch.
 */
@Service
public class NarrationService {
    private static final String DEFAULT_LANGUAGE = "vi";

    private final ContentService content;
    private final TranslationService translation;

    public NarrationService(ContentService content, TranslationService translation) {
        this.content = content;
        this.translation = translation;
    }

    public NarrationDto get(TargetType type, long targetId, String requestedLanguage) {
        TargetInfo target = content.visibleTarget(type, targetId).orElseThrow(() ->
                new ResourceNotFoundException("Không tìm thấy " + (type == TargetType.SITE ? "địa điểm" : "hiện vật")
                        + " id=" + targetId));
        Optional<Narration> narration = content.narration(type, targetId);

        String source = sourceText(narration.orElse(null), target);
        if (source.isBlank()) throw new ResourceNotFoundException("Chưa có nội dung thuyết minh.");

        String sourceLang = narration.map(Narration::language).filter(l -> !l.isBlank()).orElse(DEFAULT_LANGUAGE);
        String lang = requestedLanguage == null || requestedLanguage.isBlank() ? sourceLang : requestedLanguage.trim();
        if (!TranslationService.isValidLanguage(lang)) throw new IllegalArgumentException("Mã ngôn ngữ không hợp lệ.");

        String text = source;
        boolean translated = false;
        String error = null;
        if (!TranslationService.sameLanguage(lang, sourceLang)) {
            String stored = narration.map(n -> n.translations().get(lang.toLowerCase(Locale.ROOT))).orElse(null);
            if (stored != null && !stored.isBlank()) {
                text = stored;
                translated = true;
            } else {
                TranslationService.Result r = translation.translate(source, sourceLang, lang);
                text = r.text();
                translated = r.translated();
                error = r.error();
            }
        }

        String ttsKey = content.language(lang).map(Language::ttsKey).orElse(null);
        boolean audioAvailable = narration.map(n -> n.audioEnabled() && n.hasAudio()).orElse(false) && !translated
                && TranslationService.sameLanguage(lang, sourceLang);
        String audioUrl = audioAvailable
                ? "/api/user/narrations/" + type.code() + "/" + targetId + "/audio" : null;

        double speed = narration.map(Narration::speed).orElse(1.0);
        boolean tts = narration.map(Narration::ttsEnabled).orElse(true);
        return new NarrationDto(type.code(), targetId, target.name(), lang, sourceLang, text, translated, error,
                tts, ttsKey, speed, audioAvailable, audioUrl);
    }

    private static String sourceText(Narration narration, TargetInfo target) {
        String description = target.description() == null ? "" : target.description();
        if (narration == null) return description;
        boolean custom = !narration.useDescription() && narration.text() != null && !narration.text().isBlank();
        return custom ? narration.text() : description;
    }
}
