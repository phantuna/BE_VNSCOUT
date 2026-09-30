ALTER TABLE posts
    ADD COLUMN visibility VARCHAR(30) NOT NULL DEFAULT 'PUBLIC';

CREATE INDEX idx_posts_visibility ON posts (visibility);
