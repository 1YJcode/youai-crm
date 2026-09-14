CREATE TABLE crm_follow_up_annotation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    customer_no VARCHAR(32) NOT NULL,
    record_id VARCHAR(64) NOT NULL,
    record_type VARCHAR(16) NOT NULL,
    author VARCHAR(64) NOT NULL,
    favorite BIT NOT NULL DEFAULT 0,
    comment VARCHAR(2000) NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_follow_up_annotation_owner_record (customer_no, record_id, record_type, author),
    KEY idx_follow_up_annotation_customer (customer_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
