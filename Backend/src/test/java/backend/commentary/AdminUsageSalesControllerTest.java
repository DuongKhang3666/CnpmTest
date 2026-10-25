package backend.commentary;

import backend.commentary.dto.request.OrderRequest;
import backend.commentary.dto.request.ProductRequest;
import backend.commentary.entity.enums.OrderStatus;
import backend.commentary.security.JwtUtils;
import backend.commentary.service.VisitorTrackingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class AdminUsageSalesControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private VisitorTrackingService visitorTrackingService;

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

    private String todayKey() {
        return LocalDate.now().toString();
    }

    private Long createProduct(String name, Double price, Long stock) throws Exception {
        ProductRequest request = ProductRequest.builder()
                .name(name)
                .price(price)
                .stock(stock)
                .active(true)
                .build();

        MvcResult result = mockMvc.perform(post("/api/admin/products")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        Object idObj = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return idObj instanceof Integer ? ((Integer) idObj).longValue() : (Long) idObj;
    }

    // ==================== Phần 1: Số người sử dụng trong ngày ====================

    @Test
    @DisplayName("Thống kê số người sử dụng: ghi 2 visitor khác nhau -> todayUniqueUsers = 2")
    void testUsageStats() throws Exception {
        MockHttpServletRequest request1 = new MockHttpServletRequest("GET", "/api/auth/login");
        request1.setRequestURI("/api/auth/login");
        request1.addHeader(VisitorTrackingService.VISITOR_ID_HEADER, "visitor-one");
        visitorTrackingService.recordVisit(request1);

        MockHttpServletRequest request2 = new MockHttpServletRequest("GET", "/api/auth/login");
        request2.setRequestURI("/api/auth/login");
        request2.addHeader(VisitorTrackingService.VISITOR_ID_HEADER, "visitor-two");
        visitorTrackingService.recordVisit(request2);

        // Cùng visitor ghi thêm lần nữa -> người dùng duy nhất không tăng
        MockHttpServletRequest request3 = new MockHttpServletRequest("GET", "/api/auth/login");
        request3.setRequestURI("/api/auth/login");
        request3.addHeader(VisitorTrackingService.VISITOR_ID_HEADER, "visitor-one");
        visitorTrackingService.recordVisit(request3);

        mockMvc.perform(get("/api/admin/dashboard/usage")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todayUniqueUsers", is(2)))
                .andExpect(jsonPath("$.todayVisits", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.dailyUniqueLast7Days", notNullValue()))
                .andExpect(jsonPath("$.dailyUniqueLast7Days['" + todayKey() + "']", is(2)))
                .andExpect(jsonPath("$.dailyUniqueLast30Days['" + todayKey() + "']", is(2)));
    }

    @Test
    @DisplayName("Thống kê số người sử dụng không có dữ liệu -> vẫn trả về structure đầy đủ")
    void testUsageStatsEmpty() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/usage")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todayUniqueUsers", is(0)))
                .andExpect(jsonPath("$.todayVisits", is(0)))
                .andExpect(jsonPath("$.dailyUniqueLast7Days['" + todayKey() + "']", is(0)))
                .andExpect(jsonPath("$.dailyUniqueLast30Days['" + todayKey() + "']", is(0)));
    }

    // ==================== Phần 2: Số lượng đã bán cho khách hàng ====================

    @Test
    @DisplayName("Tạo sản phẩm Thành công -> trả về 201 và mã tự sinh")
    void testCreateProduct() throws Exception {
        mockMvc.perform(post("/api/admin/products")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ProductRequest.builder()
                                .name("Vé tham quan")
                                .price(100000.0)
                                .stock(100L)
                                .build())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code", startsWith("SP-")))
                .andExpect(jsonPath("$.stock", is(100)))
                .andExpect(jsonPath("$.active", is(true)));
    }

    @Test
    @DisplayName("Tạo đơn bán hàng: trừ tồn kho, tổng tiền đúng")
    void testCreateOrderDeductsStock() throws Exception {
        Long productId = createProduct("Vé tham quan", 100000.0, 100L);

        MvcResult result = mockMvc.perform(post("/api/admin/orders")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(OrderRequest.builder()
                                .customerName("Nguyễn Văn A")
                                .customerPhone("0901234567")
                                .items(List.of(new OrderRequest.OrderItemRequest(productId, 3L)))
                                .build())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderCode", startsWith("DH-")))
                .andExpect(jsonPath("$.totalAmount", is(300000.0)))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.items[0].quantity", is(3)))
                .andReturn();

        String orderId = JsonPath.read(result.getResponse().getContentAsString(), "$.id").toString();

        // Tồn kho đã trừ 100 - 3 = 97
        mockMvc.perform(get("/api/admin/products/" + productId)
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock", is(97)));
    }

    @Test
    @DisplayName("Thống kê số lượng đã bán trên Dashboard")
    void testSalesStats() throws Exception {
        Long product1 = createProduct("Vé tham quan", 100000.0, 100L);
        Long product2 = createProduct("Sách hướng dẫn", 50000.0, 100L);

        mockMvc.perform(post("/api/admin/orders")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(OrderRequest.builder()
                                .customerName("Khách 1")
                                .items(List.of(new OrderRequest.OrderItemRequest(product1, 3L)))
                                .build())))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/admin/orders")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(OrderRequest.builder()
                                .customerName("Khách 2")
                                .items(List.of(new OrderRequest.OrderItemRequest(product2, 2L)))
                                .build())))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/admin/dashboard/sales")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSoldQuantity", is(5)))
                .andExpect(jsonPath("$.totalRevenue", is(400000.0)))
                .andExpect(jsonPath("$.soldToday", is(5)))
                .andExpect(jsonPath("$.revenueToday", is(400000.0)))
                .andExpect(jsonPath("$.dailySoldLast7Days", notNullValue()))
                .andExpect(jsonPath("$.dailySoldLast7Days['" + todayKey() + "']", is(5)))
                .andExpect(jsonPath("$.topProducts", hasSize(2)))
                .andExpect(jsonPath("$.topProducts[0].productName", is("Vé tham quan")))
                .andExpect(jsonPath("$.topProducts[0].soldQuantity", is(3)));
    }

    @Test
    @DisplayName("Hủy đơn hàng: hoàn lại tồn kho và không tính vào số đã bán")
    void testCancelOrderRestoresStockAndStats() throws Exception {
        Long productId = createProduct("Vé tham quan", 100000.0, 100L);

        MvcResult result = mockMvc.perform(post("/api/admin/orders")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(OrderRequest.builder()
                                .customerName("Khách 1")
                                .items(List.of(new OrderRequest.OrderItemRequest(productId, 3L)))
                                .build())))
                .andExpect(status().isCreated())
                .andReturn();

        String orderId = JsonPath.read(result.getResponse().getContentAsString(), "$.id").toString();

        mockMvc.perform(patch("/api/admin/orders/" + orderId + "/status")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new backend.commentary.dto.request.OrderStatusUpdateRequest(OrderStatus.CANCELLED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));

        // Tồn kho hoàn lại 100
        mockMvc.perform(get("/api/admin/products/" + productId)
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock", is(100)));

        // Số đã bán = 0 sau khi hủy
        mockMvc.perform(get("/api/admin/dashboard/sales")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSoldQuantity", is(0)))
                .andExpect(jsonPath("$.totalRevenue", is(0.0)));
    }

    @Test
    @DisplayName("Tạo đơn vượt tồn kho -> 400")
    void testCreateOrderInsufficientStock() throws Exception {
        Long productId = createProduct("Vé tham quan", 100000.0, 5L);

        mockMvc.perform(post("/api/admin/orders")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(OrderRequest.builder()
                                .customerName("Khách 1")
                                .items(List.of(new OrderRequest.OrderItemRequest(productId, 10L)))
                                .build())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Truy cập thống kê không có token -> 401")
    void testUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/usage"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/dashboard/sales"))
                .andExpect(status().isUnauthorized());
    }
}
