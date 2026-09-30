package com.example.backend.dto.response.location;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Thống kê cluster địa điểm theo tỉnh/thành hoặc quận/huyện
 * Dùng cho bản đồ ở zoom thấp — thay vì vẽ hàng trăm marker rối mắt,
 * gộp lại thành một bubble hiển thị tổng số điểm trong khu vực đó.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LocationClusterResponse {
    /** ID của node cha (tỉnh/huyện) */
    private String id;

    /** Tên tỉnh/thành hoặc quận/huyện */
    private String name;

    /** Tọa độ trung tâm cluster (centroid) */
    private BigDecimal latitude;
    private BigDecimal longitude;

    /** Tổng số địa điểm SPOT trong cluster */
    private Long spotCount;

    /** Tổng số dịch vụ SERVICE trong cluster */
    private Long serviceCount;

    /** Tổng bài viết của tất cả địa điểm trong cluster */
    private Long totalPostCount;

    /** Level của node cha: 0 = tỉnh, 1 = huyện */
    private Integer level;

    /** Slug hoặc code định danh */
    private String code;
}
