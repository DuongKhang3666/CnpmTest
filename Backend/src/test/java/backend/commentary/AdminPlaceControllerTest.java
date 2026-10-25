package backend.commentary;

import com.fasterxml.jackson.databind.ObjectMapper;
import backend.commentary.dto.request.PlaceRequest;
import backend.commentary.dto.request.StatusUpdateRequest;
import backend.commentary.entity.enums.ContentStatus;
import backend.commentary.entity.enums.PlaceCategory;
import backend.commentary.entity.enums.PlaceType;
import backend.commentary.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class AdminPlaceControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JwtUtils jwtUtils;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private String adminToken;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        // Tạo JWT token hợp lệ cho admin
        this.adminToken = "Bearer " + jwtUtils.generateToken("admin@gmail.com", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("Tạo mới Địa điểm du lịch -> trả về 201 và tự sinh mã DD-xxx")
    void testCreatePlaceSuccess() throws Exception {
        PlaceRequest request = PlaceRequest.builder()
                .name("Hồ Gươm")
                .type(PlaceType.DIA_DIEM)
                .category(PlaceCategory.DANH_LAM_THANG_CANH)
                .description("Hồ Hoàn Kiếm là biểu tượng của thủ đô Hà Nội.")
                .address("Quận Hoàn Kiếm, Hà Nội")
                .latitude(21.0285)
                .longitude(105.8542)
                .status(ContentStatus.PUBLISHED)
                .image("https://images.unsplash.com/photo-ho-guom")
                .extraImages(List.of("https://images.unsplash.com/photo-1", "https://images.unsplash.com/photo-2"))
                .build();

        mockMvc.perform(post("/api/admin/places")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.code", startsWith("DD-")))
                .andExpect(jsonPath("$.name", is("Hồ Gươm")))
                .andExpect(jsonPath("$.type", is("Địa điểm")))
                .andExpect(jsonPath("$.category", is("Danh lam thắng cảnh")))
                .andExpect(jsonPath("$.status", is("Đã xuất bản")))
                .andExpect(jsonPath("$.extraImages", hasSize(2)));
    }

    @Test
    @DisplayName("Tạo mới Bảo tàng -> trả về 201 và tự sinh mã BT-xxx")
    void testCreateMuseumSuccess() throws Exception {
        PlaceRequest request = PlaceRequest.builder()
                .name("Bảo tàng Chứng tích Chiến tranh")
                .type(PlaceType.BAO_TANG)
                .category(PlaceCategory.BAO_TANG)
                .description("Bảo tàng lưu giữ chứng tích lịch sử chiến tranh tại Việt Nam.")
                .address("28 Võ Văn Tần, Phường 6, Quận 3, TP. Hồ Chí Minh")
                .latitude(10.7794)
                .longitude(106.6923)
                .status(ContentStatus.PUBLISHED)
                .build();

        mockMvc.perform(post("/api/admin/places")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code", startsWith("BT-")))
                .andExpect(jsonPath("$.type", is("Bảo tàng")));
    }

    @Test
    @DisplayName("Lấy danh sách địa điểm có phân trang và tìm kiếm")
    void testGetPlacesWithSearch() throws Exception {
        mockMvc.perform(get("/api/admin/places")
                        .header("Authorization", adminToken)
                        .param("q", "Gươm")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", notNullValue()))
                .andExpect(jsonPath("$.page", is(1)))
                .andExpect(jsonPath("$.size", is(10)));
    }

    @Test
    @DisplayName("Cập nhật địa điểm và đổi trạng thái duyệt (PATCH /status)")
    void testUpdatePlaceAndStatus() throws Exception {
        // 1. Tạo 1 địa điểm trước
        PlaceRequest createReq = PlaceRequest.builder()
                .name("Văn Miếu Quốc Tử Giám")
                .type(PlaceType.DIA_DIEM)
                .category(PlaceCategory.DI_TICH_LICH_SU)
                .description("Trường đại học đầu tiên của Việt Nam.")
                .address("58 Quốc Tử Giám, Văn Miếu, Đống Đa, Hà Nội")
                .latitude(21.0274)
                .longitude(105.8355)
                .status(ContentStatus.PENDING)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/admin/places")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseJson = createResult.getResponse().getContentAsString();
        Integer id = com.jayway.jsonpath.JsonPath.read(responseJson, "$.id");

        // 2. Đổi trạng thái sang 'Đã xuất bản'
        StatusUpdateRequest statusReq = new StatusUpdateRequest(ContentStatus.PUBLISHED);
        mockMvc.perform(patch("/api/admin/places/" + id + "/status")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("Đã xuất bản")));

        // 3. Xóa địa điểm
        mockMvc.perform(delete("/api/admin/places/" + id)
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("thành công")));
    }

    @Test
    @DisplayName("Không có token -> Bị chặn 401 Unauthorized khi gọi API Place")
    void testUnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/admin/places"))
                .andExpect(status().isUnauthorized());
    }
}
