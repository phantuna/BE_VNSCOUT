# 📋 TỔNG HỢP NGHIỆP VỤ ĐỒ ÁN VIETNAM PHOTO SCOUT

> **Mạng xã hội chia sẻ địa điểm chụp ảnh và khám phá Việt Nam (VNSCOUT)**
>
> Ngày tổng hợp: 08/06/2026

---

## MỤC LỤC

1. [Tổng quan hệ thống](#1-tổng-quan-hệ-thống)
2. [Đối tượng người dùng](#2-đối-tượng-người-dùng)
3. [Mô hình dữ liệu](#3-mô-hình-dữ-liệu)
4. [Danh sách Use Case tổng quát](#4-danh-sách-use-case-tổng-quát)
5. [Nghiệp vụ chi tiết từng module](#5-nghiệp-vụ-chi-tiết-từng-module)
   - 5.1 [Module Xác thực (Auth)](#51-module-xác-thực-auth)
   - 5.2 [Module Người dùng & Quyền (Users & RBAC)](#52-module-người-dùng--quyền-users--rbac)
   - 5.3 [Module Bài viết & Ảnh (Posts & Photos)](#53-module-bài-viết--ảnh-posts--photos)
   - 5.4 [Module Địa điểm & Bản đồ (Locations & Map)](#54-module-địa-điểm--bản-đồ-locations--map)
   - 5.5 [Module Tương tác xã hội (Social)](#55-module-tương-tác-xã-hội-social)
   - 5.6 [Module Chat thời gian thực (Chat)](#56-module-chat-thời-gian-thực-chat)
   - 5.7 [Module Báo cáo & Kiểm duyệt (Reports & Moderation)](#57-module-báo-cáo--kiểm-duyệt-reports--moderation)
6. [Tính năng Kỹ thuật/Cốt lõi chi tiết](#6-tính-năng-kỹ-thuậtcốt-lõi-chi-tiết)
   - 6.1 [Bộ lọc từ ngữ cấm (Bad Word Filter)](#61-bộ-lọc-từ-ngữ-cấm-bad-word-filter)
   - 6.2 [Hệ thống Gamification](#62-hệ-thống-gamification)
   - 6.3 [Tích hợp VietMap API](#63-tích-hợp-vietmap-api)
   - 6.4 [Tối ưu hóa truy vấn QueryDSL](#64-tối-ưu-hóa-truy-vấn-querydsl)
7. [Luồng nghiệp vụ chính](#7-luồng-nghiệp-vụ-chính)
8. [Quy tắc nghiệp vụ tổng hợp](#8-quy-tắc-nghiệp-vụ-tổng-hợp)
9. [Công nghệ sử dụng](#9-công-nghệ-sử-dụng)

---

## 1. TỔNG QUAN HỆ THỐNG

**Vietnam Photo Scout (VNSCOUT)** là một nền tảng mạng xã hội chuyên biệt dành cho cộng đồng nhiếp ảnh và những người đam mê du lịch tại Việt Nam. Hệ thống cho phép người dùng chia sẻ những địa điểm chụp ảnh đẹp, kèm theo tọa độ chính xác, hình ảnh thực tế và các thông số kỹ thuật (EXIF) để người khác có thể tham khảo và trải nghiệm.

Hệ thống hỗ trợ:

- Chia sẻ bài viết, hình ảnh kèm theo siêu dữ liệu ảnh (EXIF).
- Khám phá địa điểm qua bản đồ số tích hợp VietMap.
- Tương tác xã hội: Thích, bình luận, chia sẻ, lưu bài viết, theo dõi người dùng.
- Chat thời gian thực (1-1) qua WebSocket.
- Hệ thống quản trị mạnh mẽ: Quản lý báo cáo vi phạm, phân quyền RBAC, kiểm duyệt nội dung tự động với bộ lọc Bad Word.
- Hệ thống Gamification: Tính điểm, thăng cấp, trao huy hiệu cho người dùng tích cực.

**Kiến trúc hệ thống:**

| Thành phần | Công nghệ |
|---|---|
| Frontend | Next.js + React + TypeScript + TailwindCSS |
| Backend | Spring Boot + Spring Data JPA (QueryDSL) + Spring Security (JWT) + WebSocket |
| Database | MySQL |
| Dịch vụ Bản đồ | VietMap API |
| Storage | (Tích hợp Cloud Storage/Local) |

---

## 2. ĐỐI TƯỢNG NGƯỜI DÙNG

Hệ thống quản lý người dùng thông qua cơ chế **RBAC (Role-Based Access Control)**, gồm 3 nhóm đối tượng chính:

| Đối tượng | Vai trò (Role) | Mô tả | Quyền hạn chính |
|---|---|---|---|
| **Khách vãng lai (Guest)** | Không có tài khoản | Người truy cập website chưa đăng nhập | Lướt new feed, xem bài viết, bản đồ (giới hạn), đăng ký, đăng nhập. |
| **Người dùng (User)** | `USER` | Người dùng đã đăng ký | Đăng bài, check-in, bình luận, thả tim, nhắn tin realtime, report bài viết xấu. |
| **Quản trị viên (Admin)** | `ADMIN` | Quản trị viên hệ thống | Quản lý người dùng (cấp/thu hồi quyền Admin), xử lý Report, ẩn bài, khóa tài khoản, quản lý Bad Word. |

---

## 3. MÔ HÌNH DỮ LIỆU

Hệ thống bao gồm các Entity chính sau:

### 3.1. Nhóm Người dùng & Phân quyền
- **`Users`**: Lưu trữ thông tin người dùng (id, username, email, password, avatar, bio, điểm số gamification).
- **`Role` / `Permission`**: Quản lý vai trò (`ADMIN`, `USER`) và quyền truy cập chi tiết.
- **`UserFollow`**: Quản lý quan hệ theo dõi (follower - following).

### 3.2. Nhóm Nội dung (Posts)
- **`Posts`**: Bài viết check-in (Nội dung, trạng thái ẩn/hiện, user_id, location_id).
- **`Photos`**: Hình ảnh đính kèm bài viết.
- **`PhotoMetadata`**: Siêu dữ liệu của ảnh (Camera model, khẩu độ, tốc độ màn trập, ISO, tiêu cự) trích xuất từ EXIF.
- **`Tags`**: Thẻ phân loại nội dung bài viết.
- **`Locations`**: Tọa độ địa lý (Lat, Lng, địa chỉ, tên địa điểm, Level 2 check-in points) liên kết với VietMap.

### 3.3. Nhóm Tương tác
- **`Likes`**: Lượt thích bài viết.
- **`Comment`**: Bình luận trong bài viết.
- **`SavedPost`**: Chức năng lưu bài viết vào bookmark cá nhân.
- **`PostRating`**: Đánh giá bài viết/địa điểm.

### 3.4. Nhóm Giao tiếp & Hệ thống
- **`Conversation` / `ChatMessage`**: Quản lý hội thoại và tin nhắn realtime giữa 2 người dùng.
- **`Notification`**: Thông báo in-app (Like, Comment, Follow, System Alerts).
- **`Report`**: Báo cáo vi phạm (Lý do, trạng thái xử lý, bài viết/user bị report).
- **`BannedWord`**: Danh sách từ ngữ cấm dùng cho thuật toán Regex lọc nội dung.

---

## 4. DANH SÁCH USE CASE TỔNG QUÁT

| Mã UC | Tên Use Case | Actor | Mô tả |
|---|---|---|---|
| UC1 | Xác thực tài khoản | Guest, User | Đăng ký, đăng nhập (JWT), đổi mật khẩu. |
| UC2 | Quản lý Profile cá nhân | User | Cập nhật thông tin, avatar, xem danh sách bài đã lưu/đăng. |
| UC3 | Đăng bài Check-in | User | Tải ảnh lên, tự động trích xuất EXIF, gắn thẻ vị trí trên bản đồ. |
| UC4 | Tương tác xã hội | User | Like, Comment, Share, Follow/Unfollow user khác. |
| UC5 | Khám phá (Explore Feed) | Guest, User | Xem feed bài viết, lọc theo tags, xem danh sách Recent Check-ins (Locations cấp 2). |
| UC6 | Nhắn tin Realtime | User | Chat trực tiếp với người dùng khác qua WebSocket. |
| UC7 | Báo cáo vi phạm (Report) | User | Report bài viết hoặc bình luận có nội dung độc hại. |
| UC8 | Quản lý Phân quyền | Admin | Thay đổi vai trò (Promote/Demote) của người dùng khác. |
| UC9 | Kiểm duyệt & Xử lý Report | Admin | Xem danh sách report, quyết định ẩn bài, cảnh cáo hoặc ban user, dọn dẹp report. |
| UC10| Quản lý Từ khóa cấm | Admin | Thêm/Xóa từ khóa độc hại vào hệ thống Bad Word Filter. |

---

## 5. NGHIỆP VỤ CHI TIẾT TỪNG MODULE

### 5.1. Module Xác thực (Auth)
- **Đăng nhập/Đăng ký:** Sử dụng Spring Security + JWT. Mật khẩu được mã hóa an toàn (BCrypt).
- **Đổi mật khẩu:** Yêu cầu xác nhận mật khẩu cũ trước khi đổi sang mật khẩu mới.
- **Bảo vệ API:** Các API chỉnh sửa dữ liệu yêu cầu Bearer Token hợp lệ.

### 5.2. Module Người dùng & Quyền (Users & RBAC)
- **Quản lý Role:** Admin có thể thay đổi Role của User thông qua Admin Dashboard. Cập nhật `role_id` và áp dụng ngay lập tức cho các phiên đăng nhập sau.
- **Gamification Profile:** Khi user nhận điểm từ các hoạt động (đăng bài, nhận like), hệ thống tự động cập nhật hạng/huy hiệu hiển thị trên trang cá nhân.

### 5.3. Module Bài viết & Ảnh (Posts & Photos)
- **Đăng bài:**
  - Nội dung text sẽ chạy qua **Bad Word Filter** trước khi lưu vào DB. Nếu phát hiện từ cấm → Chuyển thành ký tự `***` hoặc chặn đăng bài (tùy cấu hình).
  - Ảnh tải lên sẽ được đọc siêu dữ liệu EXIF tự động và lưu vào `PhotoMetadata` (giúp người dùng khác học hỏi thông số máy ảnh).
- **Khám phá (Explore):** Lấy danh sách bài viết theo phân trang (Pagination) sử dụng QueryDSL để tối ưu performance, lọc những bài viết bị ẩn (`isHidden = false`).

### 5.4. Module Địa điểm & Bản đồ (Locations & Map)
- **Tích hợp VietMap:** Hiển thị bản đồ trong Explore và trang chi tiết.
- **Bảo mật Bản đồ:** API Key của VietMap được bảo vệ qua cấu hình Whitelist Domain trong production (ví dụ: `vvnscout.io.vn`) để tránh CORS và 401 Unauthorized.
- **Recent Check-ins:** Truy vấn QueryDSL để nhóm các `Locations` có bài viết gần đây, chỉ lấy địa điểm hợp lệ (không bị ẩn) và ưu tiên Level 2 (điểm check-in cụ thể thay vì cấp thành phố/tỉnh).

### 5.5. Module Tương tác xã hội (Social)
- **Bình luận:** User có quyền Xóa bình luận của chính mình. Admin có quyền Xóa bất kỳ bình luận nào.
- **Thông báo (Notification):** Khi có tương tác (Like, Comment, Follow), hệ thống tạo bản ghi `Notification` và đẩy real-time xuống client (nếu đang online) hoặc hiển thị khi user mở app.

### 5.6. Module Chat thời gian thực (Chat)
- **Giao thức:** Sử dụng Spring WebSocket (STOMP protocol).
- **Nghiệp vụ:**
  - Khởi tạo `Conversation` giữa 2 user.
  - Các `ChatMessage` được lưu DB và broadcast lập tức tới user kia.
  - Hiển thị trạng thái "đã xem" (Read Receipts).

### 5.7. Module Báo cáo & Kiểm duyệt (Reports & Moderation)
- **Gửi Report:** User gửi report → Lưu vào DB với trạng thái `PENDING`.
- **Xử lý (Admin Dashboard):**
  - Mở Modal Report Detail: Admin xem thông tin bài viết bị report và số lượng report.
  - **Single-Transaction Moderation:** Trong một thao tác, Admin có thể đồng thời: 
    1) Ẩn bài viết (`isHidden = true`). 
    2) Trừ điểm/Phạt user vi phạm (Penalize). 
    3) Chuyển toàn bộ Report liên quan sang trạng thái `RESOLVED`.

---

## 6. TÍNH NĂNG KỸ THUẬT/CỐT LÕI CHI TIẾT

### 6.1. Bộ lọc từ ngữ cấm (Bad Word Filter)
- **Công nghệ:** Regular Expression (Regex) nâng cao.
- **Cơ chế hoạt động:**
  - Không chỉ tìm chuỗi chính xác (exact match) mà còn xử lý **Leet Speak** (vd: `v!cl`, `đ.k.m`, `sh1t`).
  - Xử lý các ký tự bị làm méo, dấu cách vô nghĩa, hoặc ký tự có dấu/không dấu tiếng Việt.
  - Ứng dụng khi tạo Bài viết, tạo Bình luận, cập nhật Bio.

### 6.2. Hệ thống Gamification
- Khuyến khích tương tác bằng cách cộng điểm:
  - Đăng bài mới: +10 điểm
  - Nhận 1 lượt Like: +1 điểm
  - Check-in địa điểm mới: +5 điểm
- Hệ thống trừ điểm khi user bị Admin phạt do vi phạm nội dung.
- Các mốc điểm tương ứng với Huy hiệu (Scout Badges) hiển thị trên avatar.

### 6.3. Tích hợp VietMap API
- Xử lý hiển thị bản đồ mượt mà trên React/Next.js.
- Cấu hình Domain whitelist và Referer headers nghiêm ngặt để đảm bảo an toàn cho token VietMap trên môi trường Production.

### 6.4. Tối ưu hóa truy vấn QueryDSL
- Hệ thống tránh n+1 query problem trong JPA bằng cách sử dụng QueryDSL.
- Fetch Join các bảng phụ (Photos, Tags, Locations) khi lấy danh sách Explore Feed.
- Deduplication (Loại bỏ trùng lặp) cho Recent Check-ins ở tầng Database thay vì tầng RAM.

---

## 7. LUỒNG NGHIỆP VỤ CHÍNH

### 7.1. Luồng Đăng bài (Post Creation)
```
┌─────────────┐     ┌──────────────────┐     ┌────────────────┐     ┌──────────────┐
│ Chọn ảnh &   │────▶│ Extract EXIF     │────▶│ Lọc Bad Word   │────▶│ Lưu DB (Post,│
│ Tag vị trí   │     │ (Frontend/Back)  │     │ Nội dung text  │     │ Photos, Meta)│
└─────────────┘     └──────────────────┘     └────────────────┘     └──────────────┘
```

### 7.2. Luồng Kiểm duyệt Report (Admin Moderation)
```
┌─────────────┐     ┌──────────────────┐     ┌────────────────┐     ┌──────────────┐
│ User A      │────▶│ Admin mở Report  │────▶│ Nhấn "Xử lý"   │────▶│ 1. Ẩn bài    │
│ Report Bài  │     │ Dashboard        │     │ (Transaction)  │     │ 2. Phạt user │
└─────────────┘     └──────────────────┘     └────────────────┘     │ 3. Resolve   │
                                                                    └──────────────┘
```

---

## 8. QUY TẮC NGHIỆP VỤ TỔNG HỢP

| # | Quy tắc | Chi tiết |
|---|---|---|
| 01 | **RBAC Xóa bình luận** | Admin xóa được mọi bình luận. User chỉ xóa được bình luận của chính mình. |
| 02 | **Kiểm duyệt tự động** | Bất kỳ text nào vào DB (Post, Comment) đều phải đi qua Service Bad Word Filter. |
| 03 | **Hiển thị Feed** | Chỉ lấy bài viết có `isHidden = false` và user sở hữu không bị khóa (Banned). |
| 04 | **Recent Locations** | Cột Check-ins bên phải chỉ hiển thị Location Level 2 (Điểm cụ thể) và nhóm lại (GROUP BY) để tránh trùng lặp. |
| 05 | **Quyền Admin** | Admin không thể tự xóa quyền Admin của chính mình (chống self-lockout). |

---

## 9. CÔNG NGHỆ SỬ DỤNG
- **Frontend:** Next.js 14, React, Tailwind CSS, Shadcn/UI, Axios, WebSocket Client.
- **Backend:** Java 17+, Spring Boot 3, Spring Security, Spring Data JPA, QueryDSL, WebSocket STOMP.
- **Database:** MySQL 8+.
- **Third-party APIs:** VietMap (Vector Maps).
