ALTER TABLE extraction_sessions
    ADD COLUMN max_posts INTEGER NOT NULL DEFAULT 30,
    ADD COLUMN min_macedonian_confidence DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    ADD COLUMN include_text BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN include_images BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN include_videos BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE extraction_sessions
    ADD CONSTRAINT chk_extraction_sessions_max_posts
        CHECK (max_posts BETWEEN 1 AND 100),
    ADD CONSTRAINT chk_extraction_sessions_confidence
        CHECK (min_macedonian_confidence BETWEEN 0.0 AND 1.0);
