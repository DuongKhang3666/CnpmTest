package backend.commentary.service;

import backend.commentary.dto.request.OrderRequest;
import backend.commentary.dto.response.OrderResponse;
import backend.commentary.dto.response.PageResponse;
import backend.commentary.dto.response.SalesStatsResponse;
import backend.commentary.entity.CustomerOrder;
import backend.commentary.entity.OrderItem;
import backend.commentary.entity.Product;
import backend.commentary.entity.enums.OrderStatus;
import backend.commentary.exception.ResourceNotFoundException;
import backend.commentary.repository.OrderItemRepository;
import backend.commentary.repository.OrderRepository;
import backend.commentary.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service quản lý Đơn hàng bán cho khách hàng + thống kê số lượng đã bán
 */
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getOrders(String query, OrderStatus status, int page, int size) {
        int pageIndex = Math.max(0, page - 1);
        int pageSize = Math.max(1, Math.min(size, 100));
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<CustomerOrder> orderPage = orderRepository.searchOrders(query, status, pageable);

        List<OrderResponse> items = orderPage.getContent().stream()
                .map(OrderResponse::fromEntity)
                .toList();

        return new PageResponse<>(items, page, pageSize, orderPage.getTotalElements(),
                Math.max(1, orderPage.getTotalPages()));
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        return orderRepository.findById(id)
                .map(OrderResponse::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với id: " + id));
    }

    /**
     * Tạo đơn bán cho khách hàng: trừ tồn kho, tính tổng tiền, lưu chi tiết đơn
     */
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        CustomerOrder order = new CustomerOrder();
        order.setOrderCode(generateOrderCode());
        order.setCustomerName(request.getCustomerName());
        order.setCustomerPhone(request.getCustomerPhone());
        order.setCustomerEmail(request.getCustomerEmail());
        order.setNote(request.getNote());
        order.setStatus(request.getStatus() != null ? request.getStatus() : OrderStatus.PENDING);

        List<OrderItem> items = resolveItems(request, order);
        double totalAmount = items.stream().mapToDouble(OrderItem::getSubtotal).sum();
        order.setTotalAmount(totalAmount);

        // Trừ tồn kho
        for (OrderItem item : items) {
            item.getProduct().setStock(item.getProduct().getStock() - item.getQuantity());
            productRepository.save(item.getProduct());
        }

        for (OrderItem item : items) {
            order.addItem(item);
        }
        CustomerOrder savedOrder = orderRepository.save(order);
        return OrderResponse.fromEntity(savedOrder);
    }

    /**
     * Đổi trạng thái đơn hàng:
     * - Sang CANCELLED: hoàn lại tồn kho
     * - Từ CANCELLED sang trạng thái khác: trừ lại tồn kho
     */
    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus newStatus) {
        CustomerOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với id: " + id));

        OrderStatus oldStatus = order.getStatus();
        if (oldStatus == newStatus) {
            return OrderResponse.fromEntity(order);
        }

        if (newStatus == OrderStatus.CANCELLED && oldStatus != OrderStatus.CANCELLED) {
            // Hoàn lại tồn kho khi hủy đơn
            for (OrderItem item : order.getItems()) {
                item.getProduct().setStock(item.getProduct().getStock() + item.getQuantity());
                productRepository.save(item.getProduct());
            }
        } else if (oldStatus == OrderStatus.CANCELLED && newStatus != OrderStatus.CANCELLED) {
            // Kích hoạt lại đơn: trừ lại tồn kho
            for (OrderItem item : order.getItems()) {
                item.getProduct().setStock(item.getProduct().getStock() - item.getQuantity());
                productRepository.save(item.getProduct());
            }
        }

        order.setStatus(newStatus);
        return OrderResponse.fromEntity(orderRepository.save(order));
    }

    @Transactional
    public void deleteOrder(Long id) {
        CustomerOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với id: " + id));
        // Hoàn lại tồn kho nếu đơn chưa hủy
        if (order.getStatus() != OrderStatus.CANCELLED) {
            for (OrderItem item : order.getItems()) {
                item.getProduct().setStock(item.getProduct().getStock() + item.getQuantity());
                productRepository.save(item.getProduct());
            }
        }
        orderRepository.delete(order);
    }

    /**
     * Thống kê số lượng đã bán cho khách hàng (dùng cho Dashboard Admin)
     */
    @Transactional(readOnly = true)
    public SalesStatsResponse getSalesStats() {
        OrderStatus cancelled = OrderStatus.CANCELLED;
        LocalDate today = LocalDate.now();
        LocalDate startDate7 = today.minusDays(6);
        LocalDate startDate30 = today.minusDays(29);

        Long totalSold = orderItemRepository.sumSoldQuantity(cancelled);
        Double totalRevenue = orderItemRepository.sumRevenue(cancelled);

        List<Object[]> dailyQtyRows = orderItemRepository.sumSoldQuantityGroupByDate(startDate30.atStartOfDay(), cancelled);
        List<Object[]> dailyRevenueRows = orderItemRepository.sumRevenueGroupByDate(startDate30.atStartOfDay(), cancelled);

        Map<String, Long> qtyByDate = mapDailyQuantity(dailyQtyRows, startDate30, today);
        Map<String, Long> dailyLast7 = filterRange(qtyByDate, startDate7, today);
        Map<String, Long> dailyLast30 = filterRange(qtyByDate, startDate30, today);

        // Doanh thu theo ngày chỉ để tính hôm nay
        Map<String, Long> revenueByDate = mapDailyQuantity(dailyRevenueRows, startDate30, today);
        long soldToday = qtyByDate.getOrDefault(today.toString(), 0L);
        double revenueToday = revenueByDate.getOrDefault(today.toString(), 0L);

        List<Object[]> topRows = orderItemRepository.sumSoldQuantityGroupByProduct(cancelled, PageRequest.of(0, 5));
        List<SalesStatsResponse.TopProduct> topProducts = topRows.stream()
                .map(row -> new SalesStatsResponse.TopProduct(
                        (String) row[0],
                        (Long) row[1],
                        ((Number) row[2]).doubleValue()))
                .collect(Collectors.toList());

        return new SalesStatsResponse(
                totalSold != null ? totalSold : 0L,
                totalRevenue != null ? totalRevenue : 0d,
                soldToday,
                revenueToday,
                dailyLast7,
                dailyLast30,
                topProducts
        );
    }

    private List<OrderItem> resolveItems(OrderRequest request, CustomerOrder order) {
        List<OrderItem> items = new ArrayList<>();
        for (OrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy sản phẩm với id: " + itemRequest.getProductId()));

            if (Boolean.FALSE.equals(product.getActive())) {
                throw new IllegalArgumentException("Sản phẩm '" + product.getName() + "' hiện không được bán.");
            }
            if (itemRequest.getQuantity() > product.getStock()) {
                throw new IllegalArgumentException("Sản phẩm '" + product.getName() + "' chỉ còn " + product.getStock() + " sản phẩm trong kho.");
            }

            items.add(new OrderItem(product, itemRequest.getQuantity(), product.getPrice()));
        }
        return items;
    }

    private Map<String, Long> mapDailyQuantity(List<Object[]> rows, LocalDate startDate, LocalDate today) {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (LocalDate date = startDate; !date.isAfter(today); date = date.plusDays(1)) {
            stats.put(date.toString(), 0L);
        }
        for (Object[] row : rows) {
            LocalDate date = toLocalDate(row[0]);
            Number count = (Number) row[1];
            if (date != null && count != null && stats.containsKey(date.toString())) {
                stats.put(date.toString(), count.longValue());
            }
        }
        return stats;
    }

    /**
     * Chuyển kết quả CAST(x AS date) về LocalDate (H2 trả java.sql.Date, MySQL trả LocalDate)
     */
    private LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) return localDate;
        if (value instanceof java.sql.Date sqlDate) return sqlDate.toLocalDate();
        return null;
    }

    private Map<String, Long> filterRange(Map<String, Long> source, LocalDate startDate, LocalDate endDate) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : source.entrySet()) {
            LocalDate date = LocalDate.parse(entry.getKey());
            if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    private String generateOrderCode() {
        return "DH-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + java.util.UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}
