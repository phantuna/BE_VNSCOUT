-- =========================================================================
-- V14: Tối ưu hoá Index cho các truy vấn tần suất cao trên posts và user_follows
-- =========================================================================

-- 1. Index tối ưu cho trang cá nhân Profile (lấy bài viết theo user_id sắp xếp mới nhất)
-- Query: WHERE user_id = ? AND deleted = 0 [AND status = ?] ORDER BY created_date DESC
CREATE INDEX idx_posts_user_created 
    ON posts (user_id, deleted, status, created_date);

-- 2. Index tối ưu cho truy vấn bài viết theo địa điểm
-- Query: WHERE location_id = ? AND deleted = 0 AND status = 'ACTIVE' ORDER BY created_date DESC
CREATE INDEX idx_posts_location_created 
    ON posts (location_id, deleted, status, created_date);

-- 3. Index tối ưu cho trang Admin duyệt bài viết
-- Query: WHERE status = 'PENDING_REVIEW' AND deleted = 0 ORDER BY created_date DESC
CREATE INDEX idx_posts_pending 
    ON posts (status, deleted, created_date);

-- 4. Index tối ưu cho quan hệ follow và kiểm tra mutual follow (2 chiều)
-- Query: WHERE follower_id = ? AND deleted = 0
CREATE INDEX idx_user_follows_follower_deleted 
    ON user_follows (follower_id, deleted, following_id);

-- Query: WHERE following_id = ? AND deleted = 0
CREATE INDEX idx_user_follows_following_deleted 
    ON user_follows (following_id, deleted, follower_id);
