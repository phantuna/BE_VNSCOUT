package com.example.backend.enums;

/**
 * Phạm vi hiển thị của bài viết.
 *
 * PUBLIC         — Mọi người đều thấy (mặc định)
 * FOLLOWERS_ONLY — Chỉ những người có quan hệ follow 2 chiều
 *                  (tôi follow họ VÀ họ follow tôi) mới thấy
 * PRIVATE        — Chỉ bản thân người đăng mới thấy
 */
public enum PostVisibility {
    PUBLIC,
    FOLLOWERS_ONLY,
    PRIVATE
}
