package backend.commentary;

import backend.commentary.dto.request.ArtifactRequest;
import backend.commentary.dto.request.PlaceRequest;
import backend.commentary.dto.response.DashboardStatsResponse;
import backend.commentary.entity.enums.ContentStatus;
import backend.commentary.entity.enums.PlaceCategory;
import backend.commentary.entity.enums.PlaceType;
import backend.commentary.security.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class AdminDashboardControllerTest {

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

        this.adminToken = "Bearer " + jwtUtils.generateToken("admin@gmail.com", "ROLE_ADMIN");
    }

    private Long createPlace(String name, PlaceType type, PlaceCategory category, ContentStatus status) throws Exception {
        PlaceRequest request = PlaceRequest.builder()
                .name(name)
                .type(type)
                .category(category)
                .description("Mô tả " + name)
                .address("TP. Hồ Chí Minh")
                .latitude(10.7700)
                .longitude(106.6900)
                .status(status)
                .build();

        MvcResult result = mockMvc.perform(post("/api/admin/places")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        Object idObj = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return idObj instanceof Integer ? ((Integer) idObj).longValue() : (Long) idObj;
    }

    private Long createArtifact(String name, Long placeId, ContentStatus status) throws Exception {
        ArtifactRequest request = ArtifactRequest.builder()
                .name(name)
                .description("Mô tả " + name)
                .placeId(placeId)
                .status(status)
                .latitude(21.0285)
                .longitude(105.8542)
                .build();

        MvcResult result = mockMvc.perform(post("/api/admin/artifacts")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        Object idObj = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return idObj instanceof Integer ? ((Integer) idObj).longValue() : (Long) idObj;
    }

    @Test
    @DisplayName("Dashboard stats trả về đầy đủ các trường thống kê")
    void testGetDashboardStats() throws Exception {
        // Tạo dữ liệu test
        Long place1 = createPlace("Hồ Gươm", PlaceType.DIA_DIEM, PlaceCategory.DANH_LAM_THANG_CANH, ContentStatus.PUBLISHED);
        Long place2 = createPlace("Bảo tàng Lịch sử", PlaceType.BAO_TANG, PlaceCategory.BAO_TANG, ContentStatus.PENDING);
        createArtifact("Xe tăng", place1, ContentStatus.PUBLISHED);
        createArtifact("Trống đồng", null, ContentStatus.DRAFT); // độc lập

        mockMvc.perform(get("/api/admin/dashboard/stats")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPlaces", is(2)))
                .andExpect(jsonPath("$.totalArtifacts", is(2)))
                .andExpect(jsonPath("$.totalAdmins", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.totalDiaDiem", is(1)))
                .andExpect(jsonPath("$.totalBaoTang", is(1)))
                .andExpect(jsonPath("$.publishedCount", is(2))) // place1 + artifact1
                .andExpect(jsonPath("$.pendingCount", is(1)))   // place2
                .andExpect(jsonPath("$.draftCount", is(1)))     // artifact2
                .andExpect(jsonPath("$.pendingApproval", is(1)))
                .andExpect(jsonPath("$.recentItems", notNullValue()))
                .andExpect(jsonPath("$.recentItems", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.dailyCreatedLast7Days", aMapWithSize(7)))
                .andExpect(jsonPath("$.dailyCreatedLast30Days", aMapWithSize(30)))
                .andExpect(jsonPath("$.statusDistribution", notNullValue()))
                .andExpect(jsonPath("$.statusDistribution['Đã xuất bản']", is(2)))
                .andExpect(jsonPath("$.statusDistribution['Chờ duyệt']", is(1)))
                .andExpect(jsonPath("$.statusDistribution['Bản nháp']", is(1)));
    }

    @Test
    @DisplayName("Dashboard stats không cần dữ liệu -> vẫn trả về structure đầy đủ")
    void testGetDashboardStatsEmpty() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/stats")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPlaces", is(0)))
                .andExpect(jsonPath("$.totalArtifacts", is(0)))
                .andExpect(jsonPath("$.totalAdmins", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.publishedCount", is(0)))
                .andExpect(jsonPath("$.pendingCount", is(0)))
                .andExpect(jsonPath("$.draftCount", is(0)))
                .andExpect(jsonPath("$.recentItems", hasSize(0)))
                .andExpect(jsonPath("$.dailyCreatedLast7Days", aMapWithSize(7)))
                .andExpect(jsonPath("$.dailyCreatedLast30Days", aMapWithSize(30)));
    }

    @Test
    @DisplayName("Truy cập Dashboard không có token -> 401 Unauthorized")
    void testDashboardUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/stats"))
                .andExpect(status().isUnauthorized());
    }
}