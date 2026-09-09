UPDATE crm_customer
SET registration_count = 1
WHERE registration_count IS NULL OR registration_count < 1;

CREATE TABLE crm_customer_registration_event (
    id BIGINT NOT NULL AUTO_INCREMENT,
    customer_id BIGINT NOT NULL,
    registration_number INT NOT NULL,
    source VARCHAR(32) NOT NULL,
    operator VARCHAR(64) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_registration_event_customer_time (customer_id, created_at),
    CONSTRAINT fk_registration_event_customer FOREIGN KEY (customer_id) REFERENCES crm_customer (id) ON DELETE CASCADE
);
