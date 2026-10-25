package backend.commentary;

import com.fasterxml.jackson.databind.ObjectMapper;
import backend.commentary.dto.request.ArtifactRequest;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class AdminArtifactControllerTest {

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

    private Long createTestPlace(String name) throws Exception {
        PlaceRequest placeReq = PlaceRequest.builder()
                .name(name)
                .type(PlaceType.BAO_TANG)
                .category(PlaceCategory.BAO_TANG)
                .description("Mô tả bảo tàng test")
                .address("TP. Hồ Chí Minh")
                .latitude(10.7700)
                .longitude(106.6900)
                .status(ContentStatus.PUBLISHED)
                .build();

        MvcResult result = mockMvc.perform(post("/api/admin/places")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(placeReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        Integer id = com.jayway.jsonpath.JsonPath.read(responseJson, "$.id");
        return id.longValue();
    }

    @Test
    @DisplayName("Tạo hiện vật thuộc Địa điểm -> 201 Created và tự sinh mã HV-xxx")
    void testCreateArtifactWithPlace() throws Exception {
        Long placeId = createTestPlace("Bảo tàng Lịch sử Quân sự");

        ArtifactRequest request = ArtifactRequest.builder()
                .name("Máy bay MiG-21")
                .description("Chiếc máy bay số hiệu 5121 bắn rơi pháo đài bay B-52 năm 1972.")
                .placeId(placeId)
                .status(ContentStatus.PUBLISHED)
                .image("https://images.unsplash.com/photo-plane")
                .build();

        mockMvc.perform(post("/api/admin/artifacts")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.code", startsWith("HV-")))
                .andExpect(jsonPath("$.name", is("Máy bay MiG-21")))
                .andExpect(jsonPath("$.type", is("Hiện vật")))
                .andExpect(jsonPath("$.placeId", is(placeId.intValue())))
                .andExpect(jsonPath("$.placeName", is("Bảo tàng Lịch sử Quân sự")))
                .andExpect(jsonPath("$.status", is("Đã xuất bản")));
    }

    @Test
    @DisplayName("Tạo hiện vật độc lập (placeId = null) -> 201 Created")
    void testCreateStandaloneArtifact() throws Exception {
        ArtifactRequest request = ArtifactRequest.builder()
                .name("Trống đồng Đông Sơn")
                .description("Cổ vật văn hóa Đông Sơn thời kỳ đồ đồng.")
                .placeId(null)
                .status(ContentStatus.PUBLISHED)
                .latitude(21.0285)
                .longitude(105.8542)
                .build();

        mockMvc.perform(post("/api/admin/artifacts")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code", startsWith("HV-")))
                .andExpect(jsonPath("$.placeId", nullValue()))
                .andExpect(jsonPath("$.location", is("Hiện vật độc lập")));
    }

    @Test
    @DisplayName("Lọc danh sách hiện vật theo placeId")
    void testGetArtifactsFilterByPlace() throws Exception {
        Long placeId = createTestPlace("Bảo tàng Cổ vật Huế");

        ArtifactRequest req = ArtifactRequest.builder()
                .name("Ngai vàng triều Nguyễn")
                .description("Ngai vàng của các vua triều Nguyễn tại Điện Thái Hòa.")
                .placeId(placeId)
                .status(ContentStatus.PUBLISHED)
                .build();

        mockMvc.perform(post("/api/admin/artifacts")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/admin/artifacts")
                        .header("Authorization", adminToken)
                        .param("placeId", String.valueOf(placeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.items[0].placeId", is(placeId.intValue())));
    }

    @Test
    @DisplayName("Cập nhật hiện vật và đổi trạng thái duyệt (PATCH /status)")
    void testUpdateAndPatchStatus() throws Exception {
        ArtifactRequest createReq = ArtifactRequest.builder()
                .name("Súng thần công")
                .description("Vũ khí thời phong kiến")
                .status(ContentStatus.PENDING)
                .latitude(21.0285)
                .longitude(105.8542)
                .build();

        MvcResult createRes = mockMvc.perform(post("/api/admin/artifacts")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Integer artifactId = com.jayway.jsonpath.JsonPath.read(createRes.getResponse().getContentAsString(), "$.id");

        // Cập nhật trạng thái
        StatusUpdateRequest statusReq = new StatusUpdateRequest(ContentStatus.PUBLISHED);
        mockMvc.perform(patch("/api/admin/artifacts/" + artifactId + "/status")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("Đã xuất bản")));

        // Xóa hiện vật
        mockMvc.perform(delete("/api/admin/artifacts/" + artifactId)
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("thành công")));
    }

    @Test
    @DisplayName("Xóa Địa điểm thì các Hiện vật thuộc về nó cũng tự động bị xóa (Cascade Delete)")
    void testCascadeDeletePlaceAndArtifacts() throws Exception {
        Long placeId = createTestPlace("Bảo tàng Mỹ thuật");

        ArtifactRequest req = ArtifactRequest.builder()
                .name("Bức tranh sơn mài")
                .description("Tác phẩm nghệ thuật đỉnh cao")
                .placeId(placeId)
                .status(ContentStatus.PUBLISHED)
                .build();

        MvcResult artResult = mockMvc.perform(post("/api/admin/artifacts")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        Integer artId = com.jayway.jsonpath.JsonPath.read(artResult.getResponse().getContentAsString(), "$.id");

        // Xóa địa điểm
        mockMvc.perform(delete("/api/admin/places/" + placeId)
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());

        // Hiện vật thuộc địa điểm đó cũng không còn tồn tại -> 404 Not Found
        mockMvc.perform(get("/api/admin/artifacts/" + artId)
                        .header("Authorization", adminToken))
                .andExpect(status().isNotFound());
    }
}
