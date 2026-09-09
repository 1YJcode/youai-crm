CREATE TABLE crm_conversation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    customer_no VARCHAR(32) NOT NULL,
    customer_name VARCHAR(64) NOT NULL,
    company VARCHAR(128) NOT NULL,
    owner VARCHAR(32) NOT NULL,
    last_message_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_conversation_customer_no (customer_no),
    KEY idx_conversation_owner_updated (owner, last_message_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE crm_message (
    id BIGINT NOT NULL AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    sender VARCHAR(64) NOT NULL,
    direction VARCHAR(16) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    sent_at DATETIME(6) NOT NULL,
    read_flag BIT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_message_conversation_sent (conversation_id, sent_at),
    CONSTRAINT fk_message_conversation FOREIGN KEY (conversation_id) REFERENCES crm_conversation (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE crm_call_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    customer_no VARCHAR(32) NOT NULL,
    customer_name VARCHAR(64) NOT NULL,
    phone VARCHAR(24) NOT NULL,
    owner VARCHAR(32) NOT NULL,
    agent VARCHAR(64) NOT NULL,
    direction VARCHAR(16) NOT NULL,
    call_status VARCHAR(24) NOT NULL,
    duration_seconds INT NOT NULL DEFAULT 0,
    note VARCHAR(1000) NULL,
    started_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_call_owner_started (owner, started_at),
    KEY idx_call_customer_no (customer_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE crm_sales_order
    ADD COLUMN performance_confirmed BIT NOT NULL DEFAULT 0 AFTER service_status;
