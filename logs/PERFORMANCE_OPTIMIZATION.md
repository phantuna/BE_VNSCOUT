# Báo Cáo Kế Hoạch Tối Ưu Hiệu Suất Hệ Thống VietnamPhoto

Tài liệu này tổng hợp các vấn đề về hiệu suất (bottlenecks) đã được phát hiện trong quá trình quét hệ thống (Frontend Next.js và Backend Spring Boot) và đề xuất các bước sửa đổi chi tiết để hệ thống hoạt động nhanh và mượt mà hơn.

## 1. Tối Ưu Frontend (Next.js - v0_photo_scount_v1)

### Vấn Đề: Khối lượng payload mạng quá lớn và xử lý nặng ở Client
Tại màn hình `posts-feed-view.tsx`, hệ thống gọi `/api/v1/posts/getAll?size=200` để kéo 200 bài viết về client. Việc này chỉ để:
1. Lấy bài viết nhiều lượt like nhất làm ảnh đại diện (`coverPhoto`) cho từng địa điểm.
2. Đếm số lượng bài viết thực tế (`postCount`) cho địa điểm.

**Hậu quả:** Tải 200 bài viết tốn vài MB băng thông, quá trình lặp qua mảng 200 bài viết cho mỗi địa điểm khiến trình duyệt bị treo nhẹ (lag), làm giảm trải nghiệm người dùng, đặc biệt trên thiết bị di động.

### Giải Pháp Đề Xuất
1. **Frontend:** Xóa bỏ hoàn toàn hàm `fetchRecentPostsForImages` và việc lấy `allPosts`. Dữ liệu render giao diện sẽ phụ thuộc 100% vào API `/api/locations`.
2. **Backend:** Cập nhật API lấy danh sách Location (trong `LocationService` hoặc `LocationsRepositoryCustomImpl`) sao cho nó trả về luôn trường `coverPhoto` và `postCount` trực tiếp trong DTO.
    - Cụ thể: Khi query Location, join hoặc dùng subquery để lấy đường dẫn của bức ảnh đầu tiên thuộc về bài viết có nhiều lượt like nhất của Location đó.

---

## 2. Tối Ưu Backend (Spring Boot - VietnamPhoto)

### 2.1. Vấn Đề N+1 Query Khi Lấy Danh Sách Bài Viết (Posts)
Khi gọi hàm `getAllPosts` hoặc `getPostsByLocation` (trả về 20 bài viết), hệ thống đang thực hiện hàng chục câu query phụ thay vì lấy dữ liệu trong 1 lần.

Tại `PostMapper.java` và `PostServiceImpl.java`, đối với mỗi bài viết trong danh sách:
- Đếm bình luận: `commentRepository.countByPostId(post.getId())` => Tốn thêm 20 queries.
- Kiểm tra trạng thái Like: `postLikeService.isLiked(...)` => Tốn thêm 20 queries.
- Kiểm tra trạng thái Saved: `savedPostRepository.existsByUserIdAndPostIdAndDeleted(...)` => Tốn thêm 20 queries.

**Tổng cộng:** Cần hơn 60 queries cho 1 request lấy trang danh sách bài viết. Điều này sẽ làm Database quá tải khi có nhiều người dùng truy cập.

### Giải Pháp Đề Xuất
**Đối với đếm số lượng bình luận (`commentCount`):**
- Thêm trường `commentCount` vào Entity `Posts` (Tương tự như `likeCount`).
- Khi user thêm 1 Comment -> Tăng `commentCount` lên 1.
- Khi user xoá 1 Comment -> Giảm `commentCount` đi 1.
- Sửa lại `PostMapper` để lấy thẳng `post.getCommentCount()` thay vì gọi `commentRepository`.

**Đối với trạng thái Like và Saved của User:**
- Lấy toàn bộ `List<String> postIds = posts.stream().map(Posts::getId).toList();`.
- Gọi hàm Repository sử dụng câu truy vấn `IN`: 
  ```sql
  SELECT postId FROM Likes WHERE userId = :userId AND postId IN (:postIds)
  ```
- Lưu kết quả trả về vào một `Set<String> likedPostIds`.
- Truyền `likedPostIds` vào trong `PostMapper` để kiểm tra `likedPostIds.contains(post.getId())` ở độ phức tạp O(1) mà không cần gọi thêm Database. Tương tự với trạng thái `Saved`.

### 2.2. Vấn Đề Cache Của Danh Sách Bài Viết
Trong `PostServiceImpl.java`, hệ thống đang cache theo format:
```java
@Cacheable(value = "posts", key = "(#userId != null ? #userId : 'none') + '-' + #page + '-' + #size")
```
Việc nối `#userId` vào key của cache đồng nghĩa với việc: Nếu có 1000 user đang online, Redis sẽ phải chứa 1000 phiên bản giống hệt nhau của Trang 1. Nó không giúp tăng tốc mà còn làm cạn kiệt bộ nhớ Cache.

### Giải Pháp Đề Xuất
- Đổi chiến lược Cache: Chỉ cache danh sách `Page<Posts>` gốc (Không dính dáng đến `userId`). Key cache chỉ là `#page + '-' + #size`.
- Dữ liệu trả về từ cache là danh sách gốc. 
- Sau khi lấy danh sách gốc từ Cache, mới lấy `userId` hiện tại đem đi check `likedPostIds` và `savedPostIds` (bằng cách gọi query `IN` phía trên). 
- Cuối cùng map dữ liệu và trả về cho user. Cách này đảm bảo tính cá nhân hoá mà vẫn dùng chung 1 bản cache bài viết.

---

## 3. Các Bước Thực Hiện Kế Tiếp
Bạn có thể ra lệnh cho mình thực hiện từng bước sau:
1. **Bước 1:** Thay đổi `Posts` entity thêm `commentCount` và update logic trong `CommentService`. Xoá query ở `PostMapper`.
2. **Bước 2:** Cập nhật `PostServiceImpl` dùng Query `IN` để xử lý Like/Saved hàng loạt thay vì gọi N+1.
3. **Bước 3:** Cập nhật API của `Locations` ở Backend để trả về `coverPhoto`, rồi xoá code tính toán dư thừa ở file `posts-feed-view.tsx` của Frontend.
