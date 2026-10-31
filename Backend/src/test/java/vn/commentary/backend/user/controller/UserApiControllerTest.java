package vn.commentary.backend.user.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.commentary.backend.exception.ResourceNotFoundException;
import vn.commentary.backend.user.dto.Dtos.HistoryItem;
import vn.commentary.backend.user.dto.Dtos.JoinResponse;
import vn.commentary.backend.user.dto.Dtos.NarrationDto;
import vn.commentary.backend.user.dto.Dtos.NearbyMatch;
import vn.commentary.backend.user.dto.Dtos.ResolveResult;
import vn.commentary.backend.user.exception.UserAccessException;
import vn.commentary.backend.user.exception.UserExceptionHandler;
import vn.commentary.backend.user.model.Models.DeviceSession;
import vn.commentary.backend.user.model.Models.Site;
import vn.commentary.backend.user.model.Models.TargetType;
import vn.commentary.backend.user.security.SessionInterceptor;
import vn.commentary.backend.user.service.ActivityService;
import vn.commentary.backend.user.service.ContentService;
import vn.commentary.backend.user.service.DiscoveryService;
import vn.commentary.backend.user.service.NarrationService;
import vn.commentary.backend.user.service.SessionService;
import vn.commentary.backend.user.service.TranslationService;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Kiểm thử HTTP của các controller user (MockMvc độc lập, không cần DB). */
class UserApiControllerTest {
    private SessionService sessionService;
    private ContentService content;
    private NarrationService narrations;
    private DiscoveryService discovery;
    private ActivityService activity;
    private TranslationService translation;
    private MockMvc mvc;

    private static final DeviceSession SESSION = new DeviceSession("device-12345678", "h", "W", "C", "web", "ip", "", "",
            "", "", "", 0, 0, 0, 24);

    @BeforeEach
    void setUp() {
        sessionService = mock(SessionService.class);
        content = mock(ContentService.class);
        narrations = mock(NarrationService.class);
        discovery = mock(DiscoveryService.class);
        activity = mock(ActivityService.class);
        translation = mock(TranslationService.class);
        Clock clock = Clock.fixed(Instant.parse("2026-10-30T10:00:00Z"), ZoneOffset.UTC);
        mvc = MockMvcBuilders.standaloneSetup(
                        new UserSessionController(sessionService, clock),
                        new UserContentController(content),
                        new UserNarrationController(narrations, content),
                        new UserDiscoveryController(discovery),
                        new UserActivityController(activity),
                        new UserTranslateController(translation))
                .setControllerAdvice(new UserExceptionHandler())
                .build();
    }

    @Test
    void healthIsOk() throws Exception {
        mvc.perform(get("/api/user/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.time").value("2026-10-30T10:00:00Z"));
    }

    @Test
    void joinReturnsSessionToken() throws Exception {
        when(sessionService.join(any(), anyString())).thenReturn(
                new JoinResponse(true, "device-12345678", "tok", "2026-10-31T10:00:00Z", 24));
        mvc.perform(post("/api/user/session/join").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"qr\",\"deviceId\":\"device-12345678\",\"info\":{\"clientType\":\"web\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionToken").value("tok"))
                .andExpect(jsonPath("$.accessHours").value(24));
    }

    @Test
    void accessErrorsCarryStableReasonCodes() throws Exception {
        when(sessionService.join(any(), anyString()))
                .thenThrow(new UserAccessException(HttpStatus.UNAUTHORIZED, "token-invalid", "Mã QR không hợp lệ"));
        mvc.perform(post("/api/user/session/join").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.reason").value("token-invalid"));
    }

    @Test
    void narrationEndpointPassesLanguageAndMapsErrors() throws Exception {
        when(narrations.get(TargetType.SITE, 1, "en")).thenReturn(new NarrationDto("site", 1, "Dinh", "en", "vi",
                "Hello", true, null, true, "en-US", 1.0, false, null));
        mvc.perform(get("/api/user/narrations/site/1").param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Hello"))
                .andExpect(jsonPath("$.translated").value(true));

        mvc.perform(get("/api/user/narrations/planet/1")).andExpect(status().isBadRequest());

        when(narrations.get(TargetType.SITE, 9, null)).thenThrow(new ResourceNotFoundException("Không tìm thấy"));
        mvc.perform(get("/api/user/narrations/site/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Không tìm thấy"));
    }

    @Test
    void sitesListExposesImageUrlInsteadOfBase64() throws Exception {
        Site site = new Site(1, "Dinh Độc Lập", "Di tích", "HCM", "đc", "mô tả", 10.0, 106.0, true, List.of(), false, "");
        when(content.sites(null, null)).thenReturn(List.of(site));
        mvc.perform(get("/api/user/sites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Dinh Độc Lập"))
                .andExpect(jsonPath("$[0].imageUrl").value("/api/user/sites/1/image"));
    }

    @Test
    void imageEndpointDecodesDataUrlAndRedirectsExternalUrls() throws Exception {
        when(content.image(TargetType.SITE, 1)).thenReturn("data:image/png;base64,aGVsbG8=");
        mvc.perform(get("/api/user/sites/1/image"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes("hello".getBytes()));

        when(content.image(TargetType.ARTIFACT, 2)).thenReturn("https://cdn.example/a.png");
        mvc.perform(get("/api/user/artifacts/2/image"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://cdn.example/a.png"));
    }

    @Test
    void discoveryEndpoints() throws Exception {
        when(discovery.resolveQr("DD-1")).thenReturn(new ResolveResult("site", 1, "Dinh", "qr"));
        mvc.perform(get("/api/user/resolve/qr").param("code", "DD-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetId").value(1));

        when(discovery.nearby(10.77, 106.69)).thenReturn(List.of(new NearbyMatch("site", 1, "Dinh", 12.5, 120)));
        mvc.perform(get("/api/user/nearby").param("lat", "10.77").param("lng", "106.69"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].distanceMeters").value(12.5));

        mvc.perform(get("/api/user/nearby").param("lat", "abc").param("lng", "1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/user/resolve/qr")).andExpect(status().isBadRequest());
    }

    @Test
    void activityEndpointsValidateBodyAndUseSessionAttribute() throws Exception {
        mvc.perform(post("/api/user/listens").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetType\":\"site\",\"targetId\":1,\"language\":\"en\"}"))
                .andExpect(status().isCreated());
        verify(activity).recordListen(any());

        mvc.perform(post("/api/user/listens").contentType(MediaType.APPLICATION_JSON).content("{\"targetType\":\"site\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/user/listens").contentType(MediaType.APPLICATION_JSON).content("khong-phai-json"))
                .andExpect(status().isBadRequest());

        when(activity.history(SESSION, null)).thenReturn(
                List.of(new HistoryItem(1, "site", 1, "Dinh", "vi", "manual", "2026-10-30T10:00:00Z")));
        mvc.perform(get("/api/user/history").requestAttr(SessionInterceptor.ATTR, SESSION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Dinh"));

        mvc.perform(delete("/api/user/likes/site/1").requestAttr(SessionInterceptor.ATTR, SESSION))
                .andExpect(status().isOk());
        verify(activity).unlike(SESSION, "site", 1);
    }

    @Test
    void translateBatch() throws Exception {
        when(translation.translate("xin chào", null, "en"))
                .thenReturn(new TranslationService.Result("hello", true, null));
        mvc.perform(post("/api/user/translate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texts\":[\"xin chào\"],\"target\":\"en\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].text").value("hello"))
                .andExpect(jsonPath("$.results[0].translated").value(true));

        mvc.perform(post("/api/user/translate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texts\":[],\"target\":\"en\"}"))
                .andExpect(status().isBadRequest());
    }
}
