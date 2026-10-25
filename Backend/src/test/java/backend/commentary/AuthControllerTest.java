package backend.commentary;

import com.fasterxml.jackson.databind.ObjectMapper;
import backend.commentary.dto.request.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class AuthControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("Đăng nhập thành công với admin mặc định -> trả về 200 và JWT token")
    void testLoginSuccess() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("admin@gmail.com")
                .password("123456")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.type", is("Bearer")))
                .andExpect(jsonPath("$.email", is("admin@gmail.com")))
                .andExpect(jsonPath("$.role", is("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("Đăng nhập sai mật khẩu -> trả về 401 Unauthorized")
    void testLoginWrongPassword() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("admin@gmail.com")
                .password("sai_mat_khau")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")))
                .andExpect(jsonPath("$.message", containsString("không chính xác")));
    }

    @Test
    @DisplayName("Đăng nhập với email không hợp lệ -> trả về 400 Bad Request")
    void testLoginInvalidEmail() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("khong-phai-email")
                .password("123456")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("BAD_REQUEST")));
    }

    @Test
    @DisplayName("Truy cập endpoint bảo mật không có JWT token -> trả về 401 Unauthorized")
    void testAccessSecuredEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/admin/places"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")));
    }
}
