CREATE TABLE crm_customer_operation_event (
    id BIGINT NOT NULL AUTO_INCREMENT,
    customer_id BIGINT NOT NULL,
    operation_type VARCHAR(64) NOT NULL,
    detail VARCHAR(2000) NOT NULL,
    operator VARCHAR(64) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_customer_operation_event_customer_time (customer_id, created_at),
    CONSTRAINT fk_customer_operation_event_customer FOREIGN KEY (customer_id) REFERENCES crm_customer (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
