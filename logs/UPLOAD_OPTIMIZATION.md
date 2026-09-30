# Kịch bản tối ưu hóa API Upload Ảnh (VietnamPhoto)

Hiện tại, API upload của bạn mất khoảng **10 - 12 giây** vì các tác vụ đang bị "thắt cổ chai" (bottleneck). Qua kiểm tra mã nguồn, mình phát hiện quy trình cho mỗi bức ảnh đang chạy **tuần tự** như sau:
1. Đẩy file từ Frontend -> Backend (File gốc nặng, tốn băng thông)
2. Backend kiểm tra kiểm duyệt (Moderation) -> *Chờ phản hồi*
3. Backend trích xuất EXIF -> *Nhanh*
4. Backend gọi API Vietmap để lấy địa chỉ -> *Chờ phản hồi*
5. Backend nén ảnh & Đẩy lên Cloudinary -> *Rất nặng & Chờ phản hồi*

Dưới đây là 3 kịch bản tối ưu từ dễ đến khó để bạn lựa chọn:

---

### Kịch bản 1: Tối ưu hóa Luồng chạy song song (Dễ nhất - Đề xuất)
Hiện tại `PhotoUploadServiceImpl` đã dùng luồng xử lý song song cho nhiều ảnh, nhưng **bên trong mỗi ảnh** thì các bước (Kiểm duyệt, Vietmap, Cloudinary) lại phải đợi nhau.

* **Cách làm:** Tách các tác vụ độc lập ra chạy song song (Multithreading) bằng `CompletableFuture`.
  - Luồng 1: Gửi ảnh đi kiểm duyệt (Moderation).
  - Luồng 2: Trích xuất EXIF và gọi API Vietmap.
  - Luồng 3: Nén ảnh và Upload lên Cloudinary.
* **Kết quả:** Tổng thời gian xử lý 1 ảnh sẽ giảm từ `T1 + T2 + T3` xuống chỉ còn bằng tác vụ nào lâu nhất `Max(T1, T2, T3)`. (Có thể giảm thời gian từ 10s xuống còn 3-4s).
* **Đánh giá:** Không cần sửa UI Frontend, triển khai nhanh trên Backend.

### Kịch bản 2: Nén ảnh ngay trên Frontend trước khi gửi (Hiệu quả cao)
Ảnh chụp từ điện thoại hiện nay thường nặng từ 5MB - 15MB. Việc gửi 4 ảnh (40MB) qua mạng tốn rất nhiều thời gian trước khi Backend kịp làm gì.

* **Cách làm:** Sử dụng thư viện như `browser-image-compression` trên Frontend để nén ảnh xuống còn khoảng 1MB - 2MB trước khi gửi request API.
* **Lưu ý quan trọng:** Quá trình nén thường làm mất dữ liệu EXIF (Gps). Do đó, Frontend cần trích xuất EXIF trước, sau đó gửi kèm tọa độ EXIF lên Backend cùng với ảnh đã nén; HOẶC cấu hình thư viện nén giữ lại EXIF.
* **Kết quả:** Tốc độ upload mạng tăng 5-10 lần. Giảm tải CPU cho Backend (không cần nén lại).

### Kịch bản 3: Upload Asynchronous & Polling (Kiến trúc lớn)
Cách này thay đổi hoàn toàn trải nghiệm người dùng, giống với Facebook hoặc Instagram.

* **Cách làm:** 
  1. Frontend gửi ảnh, Backend lưu tạm vào thư mục Local/S3 và trả về `202 Accepted` ngay lập tức (chỉ mất 0.5s).
  2. Một Background Worker (RabbitMQ / Kafka / @Async) sẽ âm thầm xử lý kiểm duyệt, Cloudinary, EXIF ở phía sau.
  3. Frontend dùng WebSockets hoặc Polling để lắng nghe tiến trình, thanh Progress Bar sẽ chạy dần. Khi có EXIF, UI sẽ tự động update vị trí.
* **Kết quả:** Người dùng không bao giờ thấy bị "đơ" hay chờ đợi màn hình loading.
* **Đánh giá:** Khó triển khai nhất, cần viết lại logic hiển thị Loading và chọn địa điểm của Frontend.

---

**💡 Khuyến nghị của mình:**
Chúng ta nên kết hợp **Kịch bản 1** (Sửa lại code Backend chạy song song) và **Kịch bản 2** (Thêm nén ảnh ở Frontend). 
Bạn có muốn mình tiến hành code **Kịch bản 1 (Tối ưu Backend)** ngay bây giờ để thấy tốc độ cải thiện luôn không?
