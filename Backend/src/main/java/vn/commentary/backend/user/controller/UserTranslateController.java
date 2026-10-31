package vn.commentary.backend.user.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.commentary.backend.user.dto.Dtos.TranslateRequest;
import vn.commentary.backend.user.dto.Dtos.TranslateResponse;
import vn.commentary.backend.user.dto.Dtos.TranslatedText;
import vn.commentary.backend.user.service.TranslationService;

import java.util.ArrayList;
import java.util.List;

/** Dịch giao diện/văn bản ngắn theo lô (thay cho {@code POST /api/translate} của server.mjs). */
@RestController
@RequestMapping("/api/user")
public class UserTranslateController {
    private static final int MAX_TEXT = 10_000;

    private final TranslationService translation;

    public UserTranslateController(TranslationService translation) {
        this.translation = translation;
    }

    @PostMapping("/translate")
    public TranslateResponse translate(@Valid @RequestBody TranslateRequest request) {
        if (!TranslationService.isValidLanguage(request.target())) {
            throw new IllegalArgumentException("Mã ngôn ngữ đích không hợp lệ.");
        }
        List<TranslatedText> results = new ArrayList<>();
        for (String text : request.texts()) {
            if (text != null && text.length() > MAX_TEXT) {
                throw new IllegalArgumentException("Mỗi văn bản tối đa " + MAX_TEXT + " ký tự.");
            }
            TranslationService.Result r = translation.translate(text, request.source(), request.target());
            results.add(new TranslatedText(text, r.text(), r.translated(), r.error()));
        }
        return new TranslateResponse(request.target(), results);
    }
}
