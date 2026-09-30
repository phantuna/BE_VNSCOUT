# DANH SÁCH TỔNG HỢP CÁC CHỨC NĂNG HỆ THỐNG
*(Phục vụ trực tiếp cho việc Phân rã Use Case trong Báo cáo Đồ án)*

Dưới đây là danh sách phân rã toàn bộ các chức năng (Use Case Cấp 1 và Cấp 2) của dự án **Vietnam Photo Scout (VNSCOUT)**, được ánh xạ chuẩn xác với 10 Use Case tổng quát mà bạn đã định nghĩa trong biểu đồ UML.

---

## PHÂN HỆ TÀI KHOẢN

### UC1: Xác thực tài khoản
* **UC 1.1:** Đăng nhập hệ thống (Bằng Email và Mật khẩu).
* **UC 1.2:** Đăng ký tài khoản mới (Dành cho Khách vãng lai).
* **UC 1.3:** Đăng xuất khỏi hệ thống (Xóa JWT Token ở phía Frontend).

### UC2: Quản lý Profile cá nhân
* **UC 2.1:** Xem trang thông tin cá nhân (Profile View).
* **UC 2.2:** Cập nhật thông tin cơ bản (Đổi tên hiển thị, Tiểu sử).
* **UC 2.3:** Thay đổi ảnh đại diện (Avatar).
* **UC 2.4:** Đổi mật khẩu tài khoản.
* **UC 2.5:** Xem thống kê cá nhân (Cấp độ/Level, Điểm uy tín/Reputation Score).

---

## PHÂN HỆ NỘI DUNG & TƯƠNG TÁC

### UC3: Đăng bài Check-in
* **UC 3.1:** Tạo bài viết mới (Đăng ảnh, viết Caption, thêm Shooting Tips).
* **UC 3.2:** Gắn thẻ địa điểm (Chọn từ danh sách hoặc đề xuất địa điểm mới lên Bản đồ).
* **UC 3.3:** Sửa bài viết (Chỉnh sửa nội dung văn bản của bài đã đăng).
* **UC 3.4:** Xóa bài viết cá nhân (Soft delete).

### UC4: Tương tác xã hội
* **UC 4.1:** Xem Bảng tin (New Feed) tổng hợp.
* **UC 4.2:** Đánh giá sao (Rate Post 1-5 sao).
* **UC 4.3:** Thích bài viết (Like Post) - Tính năng này tự động kích hoạt tiến trình gửi thông báo.
* **UC 4.4:** Thêm Bình luận mới vào bài viết.
* **UC 4.5:** Thu hồi/Xóa bình luận của chính mình.
* **UC 4.6:** Theo dõi (Follow) / Bỏ theo dõi (Unfollow) người dùng khác.
* **UC 4.7:** Lưu bài viết (Bookmark/Saved Posts) vào bộ sưu tập cá nhân.
* **UC 4.8:** Nhận thông báo hệ thống thời gian thực (Push Notification qua giao thức SSE - Server-Sent Events) khi có người Like, Comment hoặc Follow.

### UC5: Khám phá (Explore Feed & Bản đồ)
* **UC 5.1:** Lướt xem danh sách các bài viết nổi bật.
* **UC 5.2:** Tìm kiếm bài viết/người dùng/địa điểm theo từ khóa.
* **UC 5.3:** Xem Bản đồ số (VietMap) với các điểm ghim (Marker Clustering).
* **UC 5.4:** Xem gợi ý danh sách Top địa điểm check-in trong tuần.

### UC6: Nhắn tin Realtime (Giao tiếp trực tuyến)
* **UC 6.1:** Xem danh sách các hộp thoại trò chuyện gần đây.
* **UC 6.2:** Gửi và Nhận tin nhắn văn bản tức thời (1:1 thông qua giao thức STOMP / WebSocket).
* **UC 6.3:** Nhận thông báo tự động khi có tin nhắn mới.

---

## PHÂN HỆ QUẢN TRỊ & KIỂM DUYỆT (DÀNH CHO ADMIN)

### UC7: Báo cáo vi phạm (Phía User & Admin)
* **UC 7.1 (User):** Gửi báo cáo (Report) một bài viết hoặc bình luận có chứa nội dung xấu.
* **UC 7.2 (Admin):** Xem danh sách tổng hợp các báo cáo vi phạm đang chờ duyệt (Pending).
* **UC 7.3 (Admin):** Xem lịch sử chi tiết các lần bị báo cáo của một bài viết.

### UC8: Quản lý Phân quyền (RBAC) & Thành viên
* **UC 8.1:** Xem danh sách toàn bộ người dùng trong hệ thống.
* **UC 8.2:** Cấp quyền Quản trị viên (Promote to ADMIN) cho một User.
* **UC 8.3:** Thu hồi quyền Quản trị viên (Demote).
* **UC 8.4:** Khóa tài khoản (Ban User) đối với người dùng vi phạm nghiêm trọng.

### UC9: Kiểm duyệt & Xử lý Report
* **UC 9.1:** Bỏ qua báo cáo (Dismiss) nếu bài viết không vi phạm (Chuyển report thành Dismissed).
* **UC 9.2:** Chấp nhận báo cáo (Resolve) đối với bài viết có tội.
* **UC 9.3:** Ẩn bài viết (Hành động tự động khi Resolve Report).
* **UC 9.4:** Phạt điểm uy tín của Tác giả bài viết (Hệ thống tự động trừ 20 điểm Reputation khi bài bị Ẩn do vi phạm).
* **UC 9.5:** Duyệt hiển thị hoặc Ẩn các Địa điểm (Location) do người dùng đề xuất.

### UC10: Quản lý Từ khóa cấm
* **UC 10.1:** Xem danh sách các Từ khóa / Biểu thức chính quy (Regex) bị cấm trên hệ thống.
* **UC 10.2:** Thêm Từ cấm mới (Phân loại theo ngôn ngữ: EN/VI, hoặc mức độ nghiêm trọng).
* **UC 10.3:** Xóa Từ cấm khỏi bộ lọc tự động.
* **UC 10.4:** Bộ lọc tự động chặn bài viết (Hệ thống ngầm tự chặn nội dung có chứa từ cấm lúc User bấm nút Đăng bài).

---
*Ghi chú: Danh sách này đã được tinh chỉnh cấu trúc (Cấp 1 & Cấp 2) rất chuẩn mực để làm cơ sở cho việc vẽ 10 Biểu đồ Use Case con trong chương Phân tích Thiết kế Hệ thống.*
