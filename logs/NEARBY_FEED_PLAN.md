# Kịch bản: Home Feed theo Vị Trí Người Dùng (Nearby Feed)

## Vấn đề hiện tại
- Home page hiện đang hiển thị **danh sách địa điểm dạng grid** → người dùng phải bấm vào từng địa điểm mới thấy bài viết.
- Không có tính năng vị trí thực tế.
- Trải nghiệm rườm rà: Chọn tỉnh → Chọn địa điểm → Mới xem bài.

## Mục tiêu mới
- Khi vào app: **Lấy GPS của thiết bị** → Hiển thị **list bài viết** (kiểu Instagram/Facebook) từ các địa điểm xung quanh trong **bán kính 100–200km**.
- Không cần qua màn hình chọn địa điểm nữa.

---

## Phần 1: Backend – API mới `/api/v1/posts/nearby`

### Bước 1.1: Thêm method vào `PostService.java`
```java
Page<PostResponse> getNearbyPosts(double lat, double lng, double radiusKm, String viewerId, int page, int size);
```

### Bước 1.2: Implement trong `PostServiceImpl.java`
Logic: Lấy tất cả các `Location` có tọa độ (lat/lng), tính khoảng cách Haversine tới tọa độ người dùng, lọc những địa điểm trong bán kính, sau đó lấy các Post thuộc những địa điểm đó.

```java
// Công thức Haversine tính khoảng cách (km) giữa 2 tọa độ
private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
    double R = 6371;
    double dLat = Math.toRadians(lat2 - lat1);
    double dLon = Math.toRadians(lon2 - lon1);
    double a = Math.sin(dLat/2)*Math.sin(dLat/2)
             + Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))
             * Math.sin(dLon/2)*Math.sin(dLon/2);
    return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
}
```

**Luồng xử lý:**
1. `locationRepository.findAllByLatLngNotNull()` → Lấy danh sách địa điểm có tọa độ.
2. Filter giữ lại những địa điểm có `haversineKm(...) <= radiusKm`.
3. Lấy `locationIds` từ danh sách lọc được.
4. `postRepository.findByLocationIdInAndStatusOrderByCreatedDateDesc(locationIds, "ACTIVE", pageable)` → Trả về Page<Post>.
5. Map sang `PostResponse` như bình thường.

### Bước 1.3: Thêm endpoint vào `PostController.java`
```java
@GetMapping("/nearby")
public Page<PostResponse> getNearbyPosts(
        @RequestParam double lat,
        @RequestParam double lng,
        @RequestParam(defaultValue = "150") double radius,
        @RequestParam(required = false) String viewerId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
) {
    return postService.getNearbyPosts(lat, lng, radius, viewerId, page, size);
}
```

### Bước 1.4: Thêm query vào `PostRepository.java`
```java
@Query("SELECT p FROM Posts p WHERE p.location.id IN :locationIds AND p.status = 'ACTIVE' ORDER BY p.createdDate DESC")
Page<Posts> findByLocationIdInActive(@Param("locationIds") List<String> locationIds, Pageable pageable);
```

---

## Phần 2: Frontend – Viết lại `PostsFeedView`

### Bước 2.1: Luồng khởi động
```
Component mount
  → navigator.geolocation.getCurrentPosition()
    → Thành công: lưu {lat, lng} vào state
      → gọi API /api/v1/posts/nearby?lat=...&lng=...&radius=150
    → Từ chối hoặc lỗi: hiển thị feed mặc định (getAllPosts)
```

### Bước 2.2: Cấu trúc UI mới (thay thế grid địa điểm)

```
┌─────────────────────────────────────────┐
│ Header: "Khám phá gần bạn" + Vị trí    │
├─────────────────────────────────────────┤
│ [Bài viết 1 - dạng card]               │
│  - Ảnh (full width hoặc 1:1)           │
│  - Avatar + Tên tác giả + Địa điểm     │
│  - Caption                              │
│  - Like / Comment / Save                │
├─────────────────────────────────────────┤
│ [Bài viết 2]                           │
├─────────────────────────────────────────┤
│ ...                                     │
│ [Infinite Scroll → load thêm]          │
└─────────────────────────────────────────┘
```

### Bước 2.3: Tạo component `PostCard` (Kiểu Instagram)

Card hiển thị 1 bài viết gồm:
- **Header**: Avatar, Username, tên Location (có link)
- **Image**: Ảnh đầu tiên (hoặc carousel nếu nhiều ảnh)
- **Actions**: Like, Comment, Save, Share
- **Caption**: nội dung bài viết
- **Tags**: #hashtag

### Bước 2.4: Infinite Scroll bằng `IntersectionObserver`
Dùng 1 sentinel element ở cuối danh sách. Khi nó vào viewport → tự động load trang tiếp theo.

---

## Thứ tự thực hiện

| # | Công việc | File |
|---|-----------|------|
| 1 | Thêm query vào PostRepository | `PostRepository.java` |
| 2 | Thêm method vào PostService + Impl | `PostServiceImpl.java` |
| 3 | Thêm endpoint `/nearby` | `PostController.java` |
| 4 | Viết `PostCard` component mới | `post-card.tsx` (mới) |
| 5 | Viết lại `PostsFeedView` | `posts-feed-view.tsx` |

---

## Lưu ý quan trọng
- **Fallback**: Nếu người dùng từ chối GPS → hiển thị feed tất cả bài mới nhất (`/api/v1/posts/getAll`).
- **Bán kính mặc định**: 150km. Có thể cho phép người dùng chỉnh (100 / 150 / 200km).
- **Hiệu năng**: Nếu số lượng Location lớn, có thể dùng MySQL Spatial functions (`ST_Distance_Sphere`) thay vì lọc trong Java để tăng tốc.
