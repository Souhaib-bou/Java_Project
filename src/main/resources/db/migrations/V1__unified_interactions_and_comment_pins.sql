-- Unified interaction table (post + comment) and comment pin metadata.
-- MySQL/XAMPP-compatible migration script.

CREATE TABLE IF NOT EXISTS forum_interaction (
    id BIGINT NOT NULL AUTO_INCREMENT,
    target_type ENUM('POST','COMMENT') NOT NULL DEFAULT 'POST',
    target_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    interaction_type VARCHAR(10) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_forum_interaction_target_user_type (target_type, target_id, user_id, interaction_type),
    KEY idx_forum_interaction_target (target_type, target_id, interaction_type),
    KEY idx_forum_interaction_user (user_id, target_type, interaction_type)
);

-- Backfill old post-only interactions if legacy table exists.
SET @legacy_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'forum_post_interaction'
);

SET @copy_sql := IF(@legacy_exists > 0,
    'INSERT IGNORE INTO forum_interaction (target_type, target_id, user_id, interaction_type, created_at)
     SELECT ''POST'', post_id, user_id, type, COALESCE(created_at, CURRENT_TIMESTAMP)
     FROM forum_post_interaction',
    'SELECT 1');
PREPARE stmt_copy FROM @copy_sql;
EXECUTE stmt_copy;
DEALLOCATE PREPARE stmt_copy;

SET @drop_sql := IF(@legacy_exists > 0, 'DROP TABLE forum_post_interaction', 'SELECT 1');
PREPARE stmt_drop FROM @drop_sql;
EXECUTE stmt_drop;
DEALLOCATE PREPARE stmt_drop;

ALTER TABLE forum_comment
    ADD COLUMN IF NOT EXISTS is_pinned TINYINT(1) NOT NULL DEFAULT 0;
