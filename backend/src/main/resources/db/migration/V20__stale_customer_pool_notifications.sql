CREATE TABLE crm_system_notification (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    notification_key VARCHAR(160) NOT NULL,
    title VARCHAR(120) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    customer_no VARCHAR(32) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_system_notification_key (notification_key),
    KEY idx_system_notification_user_created (username, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
