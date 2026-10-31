package vn.commentary.backend.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.commentary.backend.exception.ResourceNotFoundException;
import vn.commentary.backend.user.dto.Dtos.NarrationDto;
import vn.commentary.backend.user.model.Models.BeaconConfig;
import vn.commentary.backend.user.model.Models.GeoConfig;
import vn.commentary.backend.user.model.Models.Language;
import vn.commentary.backend.user.model.Models.Narration;
import vn.commentary.backend.user.model.Models.QrConfig;
import vn.commentary.backend.user.model.Models.TargetInfo;
import vn.commentary.backend.user.model.Models.TargetType;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NarrationServiceTest {
    private ContentService content;
    private TranslationService translation;
    private NarrationService service;

    private static Narration narration(boolean useDescription, String text, boolean audio, Map<String, String> tr) {
        return new Narration(1, TargetType.SITE, 1, text, useDescription, true, audio, "a.mp3", audio, "vi", 1.5,
                GeoConfig.disabled(), BeaconConfig.disabled(), QrConfig.disabled(), tr);
    }

    @BeforeEach
    void setUp() {
        content = mock(ContentService.class);
        translation = mock(TranslationService.class);
        service = new NarrationService(content, translation);
        when(content.visibleTarget(TargetType.SITE, 1L)).thenReturn(
                Optional.of(new TargetInfo(TargetType.SITE, 1, "Dinh Độc Lập", "Mô tả địa điểm", null, null)));
        when(content.language(anyString())).thenReturn(Optional.empty());
        when(content.language("en")).thenReturn(Optional.of(new Language(2, "English", "en", "en-US", true)));
    }

    @Test
    void usesDescriptionWhenConfiguredAndDoesNotTranslateSameLanguage() {
        when(content.narration(TargetType.SITE, 1L)).thenReturn(Optional.of(narration(true, "bỏ qua", false, Map.of())));
        NarrationDto dto = service.get(TargetType.SITE, 1, "vi");
        assertEquals("Mô tả địa điểm", dto.text());
        assertFalse(dto.translated());
        assertEquals(1.5, dto.speed());
        verifyNoInteractions(translation);
    }

    @Test
    void usesCustomTextAndDefaultsToSourceLanguage() {
        when(content.narration(TargetType.SITE, 1L)).thenReturn(Optional.of(narration(false, "Nội dung riêng", false, Map.of())));
        NarrationDto dto = service.get(TargetType.SITE, 1, null);
        assertEquals("Nội dung riêng", dto.text());
        assertEquals("vi", dto.requestedLanguage());
    }

    @Test
    void translatesWhenAnotherLanguageIsRequested() {
        when(content.narration(TargetType.SITE, 1L)).thenReturn(Optional.of(narration(true, "", false, Map.of())));
        when(translation.translate("Mô tả địa điểm", "vi", "en"))
                .thenReturn(new TranslationService.Result("Site description", true, null));
        NarrationDto dto = service.get(TargetType.SITE, 1, "en");
        assertEquals("Site description", dto.text());
        assertTrue(dto.translated());
        assertEquals("en-US", dto.ttsKey());
        assertEquals("vi", dto.sourceLanguage());
    }

    @Test
    void adminProvidedTranslationBeatsMachineTranslation() {
        when(content.narration(TargetType.SITE, 1L)).thenReturn(
                Optional.of(narration(true, "", false, Map.of("en", "Hand written English"))));
        NarrationDto dto = service.get(TargetType.SITE, 1, "en");
        assertEquals("Hand written English", dto.text());
        assertTrue(dto.translated());
        verifyNoInteractions(translation);
    }

    @Test
    void translationFailureKeepsSourceTextAndReportsError() {
        when(content.narration(TargetType.SITE, 1L)).thenReturn(Optional.of(narration(true, "", false, Map.of())));
        when(translation.translate(anyString(), anyString(), anyString()))
                .thenReturn(new TranslationService.Result("Mô tả địa điểm", false, "dịch vụ dịch không phản hồi"));
        NarrationDto dto = service.get(TargetType.SITE, 1, "en");
        assertFalse(dto.translated());
        assertEquals("Mô tả địa điểm", dto.text());
        assertEquals("dịch vụ dịch không phản hồi", dto.translationError());
    }

    @Test
    void audioIsOnlyOfferedInTheSourceLanguage() {
        when(content.narration(TargetType.SITE, 1L)).thenReturn(Optional.of(narration(true, "", true, Map.of())));
        when(translation.translate(anyString(), anyString(), anyString()))
                .thenReturn(new TranslationService.Result("Hello", true, null));
        assertEquals("/api/user/narrations/site/1/audio", service.get(TargetType.SITE, 1, "vi").audioUrl());
        assertNull(service.get(TargetType.SITE, 1, "en").audioUrl());
    }

    @Test
    void targetWithoutAnyTextIsNotFound() {
        when(content.visibleTarget(TargetType.SITE, 5L)).thenReturn(
                Optional.of(new TargetInfo(TargetType.SITE, 5, "Trống", "", null, null)));
        when(content.narration(TargetType.SITE, 5L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.get(TargetType.SITE, 5, "vi"));
    }

    @Test
    void invisibleTargetAndBadLanguageAreRejected() {
        assertThrows(ResourceNotFoundException.class, () -> service.get(TargetType.SITE, 404, "vi"));
        when(content.narration(TargetType.SITE, 1L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.get(TargetType.SITE, 1, "not a lang!"));
    }
}
