package backend.commentary;

import backend.commentary.security.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class AdminMediaControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JwtUtils jwtUtils;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private String adminToken;

    private byte[] validJpegBytes() {
        return new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00};
    }

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        this.adminToken = "Bearer " + jwtUtils.generateToken("admin@gmail.com", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("Upload nhiều ảnh hợp lệ -> trả về 200 và danh sách URL")
    void testUploadMultipleImages() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile("files", "photo1.jpg", "image/jpeg", validJpegBytes());
        MockMultipartFile file2 = new MockMultipartFile("files", "photo2.png", "image/png", validJpegBytes());

        mockMvc.perform(multipart("/api/admin/media")
                        .file(file1)
                        .file(file2)
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("Upload thành công")))
                .andExpect(jsonPath("$.urls", hasSize(2)))
                .andExpect(jsonPath("$.mainImage", notNullValue()))
                .andExpect(jsonPath("$.extraImages", hasSize(1)));
    }

    @Test
    @DisplayName("Upload 1 ảnh đơn lẻ -> trả về URL")
    void testUploadSingleImage() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "single.jpg", "image/jpeg", validJpegBytes());

        mockMvc.perform(multipart("/api/admin/media/single")
                        .file(file)
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Upload thành công.")))
                .andExpect(jsonPath("$.url", startsWith("/uploads/")))
                .andExpect(jsonPath("$.url", endsWith(".jpg")));
    }

    @Test
    @DisplayName("Upload file không phải ảnh -> 400 Bad Request")
    void testUploadNonImage() throws Exception {
        MockMultipartFile file = new MockMultipartFile("files", "doc.txt", "text/plain", "hello".getBytes());

        mockMvc.perform(multipart("/api/admin/media")
                        .file(file)
                        .header("Authorization", adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("Chỉ chấp nhận file ảnh")));
    }

    @Test
    @DisplayName("Upload file quá 10MB -> 400 Bad Request")
    void testUploadOversized() throws Exception {
        byte[] large = new byte[11 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile("files", "large.jpg", "image/jpeg", large);

        mockMvc.perform(multipart("/api/admin/media")
                        .file(file)
                        .header("Authorization", adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("vượt quá 10MB")));
    }

    @Test
    @DisplayName("Upload quá 10 file cùng lúc -> 400 Bad Request")
    void testUploadTooManyFiles() throws Exception {
        org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder builder = multipart("/api/admin/media");
        for (int i = 0; i < 11; i++) {
            builder.file(new MockMultipartFile("files", "img" + i + ".jpg", "image/jpeg", validJpegBytes()));
        }

        mockMvc.perform(builder
                        .header("Authorization", adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("tối đa 10 ảnh")));
    }

    @Test
    @DisplayName("Xóa ảnh bằng query param -> 200 OK")
    void testDeleteImageByQueryParam() throws Exception {
        // Upload trước
        MockMultipartFile file = new MockMultipartFile("file", "to_delete.jpg", "image/jpeg", validJpegBytes());
        MvcResult uploadResult = mockMvc.perform(multipart("/api/admin/media/single")
                        .file(file)
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andReturn();

        String url = com.jayway.jsonpath.JsonPath.read(uploadResult.getResponse().getContentAsString(), "$.url");

        // Xóa bằng query param
        mockMvc.perform(delete("/api/admin/media")
                        .header("Authorization", adminToken)
                        .param("url", url))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Đã xóa ảnh thành công.")));

        // Xóa lại -> thông báo không tìm thấy
        mockMvc.perform(delete("/api/admin/media")
                        .header("Authorization", adminToken)
                        .param("url", url))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Không tìm thấy ảnh hoặc không thể xóa.")));
    }

    @Test
    @DisplayName("Truy cập API media không có token -> 401 Unauthorized")
    void testMediaUnauthorized() throws Exception {
        MockMultipartFile file = new MockMultipartFile("files", "test.jpg", "image/jpeg", validJpegBytes());

        mockMvc.perform(multipart("/api/admin/media")
                        .file(file))
                .andExpect(status().isUnauthorized());
    }
}