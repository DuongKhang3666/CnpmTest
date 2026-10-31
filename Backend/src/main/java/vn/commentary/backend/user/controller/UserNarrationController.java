package vn.commentary.backend.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.commentary.backend.user.dto.Dtos.NarrationDto;
import vn.commentary.backend.user.model.Models.TargetType;
import vn.commentary.backend.user.service.ContentService;
import vn.commentary.backend.user.service.NarrationService;

/** Nội dung thuyết minh theo ngôn ngữ (UC7.2, UC7.4). */
@RestController
@RequestMapping("/api/user/narrations")
public class UserNarrationController {
    private final NarrationService narrations;
    private final ContentService content;

    public UserNarrationController(NarrationService narrations, ContentService content) {
        this.narrations = narrations;
        this.content = content;
    }

    @GetMapping("/{type}/{id}")
    public NarrationDto get(@PathVariable String type, @PathVariable long id,
                            @RequestParam(required = false) String lang) {
        return narrations.get(TargetType.from(type), id, lang);
    }

    /** Tệp âm thanh do admin tải lên; công khai để thẻ &lt;audio&gt; phát được. */
    @GetMapping("/{type}/{id}/audio")
    public ResponseEntity<byte[]> audio(@PathVariable String type, @PathVariable long id) {
        return MediaResponses.of(content.audio(TargetType.from(type), id));
    }
}
