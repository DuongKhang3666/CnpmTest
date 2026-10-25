package backend.commentary.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Cấu trúc phản hồi danh sách phân trang thống nhất")
public class PageResponse<T> {

    @Schema(description = "Danh sách các phần tử của trang hiện tại")
    private List<T> items;

    @Schema(description = "Trang hiện tại (bắt đầu từ 1)")
    private int page;

    @Schema(description = "Số phần tử trên mỗi trang")
    private int size;

    @Schema(description = "Tổng số phần tử trên toàn bộ hệ thống")
    private long totalItems;

    @Schema(description = "Tổng số trang")
    private int totalPages;

    public PageResponse() {
    }

    public PageResponse(List<T> items, int page, int size, long totalItems, int totalPages) {
        this.items = items;
        this.page = page;
        this.size = size;
        this.totalItems = totalItems;
        this.totalPages = totalPages;
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(long totalItems) {
        this.totalItems = totalItems;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}
