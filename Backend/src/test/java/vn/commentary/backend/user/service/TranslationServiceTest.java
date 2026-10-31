package vn.commentary.backend.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.commentary.backend.user.repository.TranslationCacheRepository;
import vn.commentary.backend.user.translation.TranslationException;
import vn.commentary.backend.user.translation.Translator;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TranslationServiceTest {
    private TranslationCacheRepository cache;
    private final List<String> calls = new ArrayList<>();

    @BeforeEach
    void setUp() {
        cache = mock(TranslationCacheRepository.class);
        when(cache.find(anyString())).thenReturn(Optional.empty());
    }

    private TranslationService service(Translator translator) {
        return new TranslationService(translator, cache, Clock.fixed(Instant.ofEpochMilli(1000), ZoneOffset.UTC));
    }

    @Test
    void translatesAndCachesResult() {
        TranslationService s = service((text, source, target) -> {
            calls.add(text);
            return "EN:" + text;
        });
        TranslationService.Result r = s.translate("xin chào", "vi", "en");
        assertTrue(r.translated());
        assertEquals("EN:xin chào", r.text());
        verify(cache).save(anyString(), eq("en"), eq("EN:xin chào"), eq(1000L));
    }

    @Test
    void cacheHitSkipsTranslator() {
        when(cache.find(anyString())).thenReturn(Optional.of("đã dịch"));
        TranslationService s = service((t, a, b) -> {
            throw new TranslationException("không được gọi");
        });
        TranslationService.Result r = s.translate("hello", "vi", "en");
        assertTrue(r.translated());
        assertEquals("đã dịch", r.text());
    }

    @Test
    void sameLanguageReturnsOriginalWithoutCalling() {
        TranslationService s = service((t, a, b) -> {
            calls.add(t);
            return "x";
        });
        TranslationService.Result r = s.translate("xin chào", "vi", "vi-VN");
        assertFalse(r.translated());
        assertEquals("xin chào", r.text());
        assertTrue(calls.isEmpty());
    }

    @Test
    void failureFallsBackToOriginalAndDoesNotCache() {
        TranslationService s = service((t, a, b) -> {
            throw new TranslationException("dịch vụ sập");
        });
        TranslationService.Result r = s.translate("xin chào", "vi", "en");
        assertFalse(r.translated());
        assertEquals("xin chào", r.text());
        assertEquals("dịch vụ sập", r.error());
        verify(cache, never()).save(anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    void longTextIsSplitIntoChunks() {
        TranslationService s = service((t, a, b) -> {
            calls.add(t);
            return t.toUpperCase();
        });
        String text = ("tu ".repeat(1500)).trim(); // ~4499 ký tự > 4000
        TranslationService.Result r = s.translate(text, "vi", "en");
        assertTrue(r.translated());
        assertTrue(calls.size() >= 2);
        assertTrue(calls.stream().allMatch(c -> c.length() <= TranslationService.CHUNK_SIZE));
    }

    @Test
    void invalidTargetIsRejected() {
        TranslationService s = service((t, a, b) -> t);
        assertThrows(IllegalArgumentException.class, () -> s.translate("x", "vi", "../etc"));
    }

    @Test
    void blankTextIsReturnedAsIs() {
        TranslationService s = service((t, a, b) -> "x");
        assertEquals("", s.translate("", "vi", "en").text());
        assertFalse(s.translate("   ", "vi", "en").translated());
    }
}
