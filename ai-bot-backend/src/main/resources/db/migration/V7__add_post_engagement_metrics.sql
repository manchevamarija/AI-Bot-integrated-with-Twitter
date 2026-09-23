ALTER TABLE extracted_posts
    ADD COLUMN reply_count BIGINT,
    ADD COLUMN repost_count BIGINT,
    ADD COLUMN like_count BIGINT,
    ADD COLUMN view_count BIGINT,
    ADD COLUMN engagement_score BIGINT;

CREATE INDEX idx_extracted_posts_engagement_score ON extracted_posts (engagement_score);
CREATE INDEX idx_extracted_posts_like_count ON extracted_posts (like_count);
