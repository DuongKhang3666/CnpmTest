package vn.commentary.backend.admin.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.commentary.backend.admin.exception.ApiExceptionHandler;
import vn.commentary.backend.admin.security.AdminAuthInterceptor;
import vn.commentary.backend.admin.service.AdminAuthService;
import vn.commentary.backend.admin.service.JsonResourceService;

import java.time.Clock;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Kiểm tra cổng bảo vệ: API CRUD cũ và /api/admin/** phải đòi đăng nhập; đăng nhập đúng thì dùng được. */
class AdminSecurityMockMvcTest {
    private JsonResourceService data;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        data = mock(JsonResourceService.class);
        when(data.getCollection(anyString(), anyMap())).thenReturn(List.of(Map.of("id", 1)));
        AdminAuthService auth = new AdminAuthService("admin@example.com", "pw-123456", "0123456789abcdef0123", 12,
                "machine-key", 3, 300, Clock.systemUTC());
        mvc = MockMvcBuilders.standaloneSetup(new JsonServerController(data), new AdminAuthController(auth))
                .setControllerAdvice(new ApiExceptionHandler())
                .addMappedInterceptors(new String[]{"/*", "/*/*", "/api/admin/auth/me"}, new AdminAuthInterceptor(auth)) // giống WebConfig; /login được loại trừ
                .build();
    }

    private String login() throws Exception {
        MvcResult r = mvc.perform(post("/api/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@example.com\",\"password\":\"pw-123456\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty()).andReturn();
        return new com.fasterxml.jackson.databind.ObjectMapper().readTree(r.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void everyLegacyResourceRejectsAnonymousRequests() throws Exception {
        for (String r : new String[]{"sites", "artifacts", "narrations", "languages", "listens", "devices", "history", "likes", "settings"}) {
            mvc.perform(get("/" + r)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.reason").value("admin-auth"));
            mvc.perform(get("/" + r + "/1")).andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/narrations").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(patch("/sites/1").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(put("/settings").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/sites/1")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/auth/me")).andExpect(status().isUnauthorized());
        verifyNoInteractions(data); // không có lệnh nào chạm tới tầng service/DB
    }

    @Test
    void badTokensAreRejected() throws Exception {
        mvc.perform(get("/sites").header("Authorization", "Bearer abc.def")).andExpect(status().isUnauthorized());
        mvc.perform(get("/sites").header("Authorization", "Basic YWRtaW46MTIzNDU2")).andExpect(status().isUnauthorized());
        mvc.perform(get("/sites").header("X-Admin-Key", "wrong")).andExpect(status().isUnauthorized());
        mvc.perform(get("/sites").header("X-Admin-Key", "")).andExpect(status().isUnauthorized());
        verifyNoInteractions(data);
    }

    @Test
    void loginThenAccessWorks() throws Exception {
        String token = login();
        mvc.perform(get("/sites").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(1));
        mvc.perform(get("/api/admin/auth/me").header("Authorization", "bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("admin@example.com"));
        verify(data).getCollection(eq("sites"), anyMap());
    }

    @Test
    void apiKeyAlsoWorks() throws Exception {
        mvc.perform(get("/sites").header("X-Admin-Key", "machine-key")).andExpect(status().isOk());
    }

    @Test
    void loginRejectsBadCredentialsAndMalformedBody() throws Exception {
        mvc.perform(post("/api/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@gmail.com\",\"password\":\"123456\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.reason").value("bad-credentials"));
        mvc.perform(post("/api/admin/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/admin/auth/login").contentType(MediaType.APPLICATION_JSON).content("not json"))
                .andExpect(status().isBadRequest());
    }
}
