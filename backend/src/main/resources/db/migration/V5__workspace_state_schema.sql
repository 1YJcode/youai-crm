CREATE TABLE crm_message_template (
    id VARCHAR(40) NOT NULL,
    owner_username VARCHAR(64) NOT NULL,
    template_name VARCHAR(64) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_message_template_owner_updated (owner_username, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE crm_notification_read (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    notification_id VARCHAR(160) NOT NULL,
    read_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_notification_read_user_id (username, notification_id),
    KEY idx_notification_read_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE crm_call_review (
    call_id BIGINT NOT NULL,
    reviewer_username VARCHAR(64) NOT NULL,
    reviewed BIT NOT NULL DEFAULT 0,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (call_id),
    CONSTRAINT fk_call_review_call FOREIGN KEY (call_id) REFERENCES crm_call_record (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
