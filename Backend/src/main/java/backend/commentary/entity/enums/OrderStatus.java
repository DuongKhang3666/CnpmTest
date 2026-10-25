package backend.commentary.entity.enums;

/**
 * Trạng thái đơn hàng bán cho khách hàng
 */
public enum OrderStatus {
    PENDING,     // Chờ xử lý
    PROCESSING,  // Đang chuẩn bị
    COMPLETED,   // Hoàn tất (đã bán thành công)
    CANCELLED    // Đã hủy (hoàn lại tồn kho, không tính vào số đã bán)
}
