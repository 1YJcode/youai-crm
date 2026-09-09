ALTER TABLE crm_customer
    ADD COLUMN first_allocation_at DATETIME(6) NULL,
    ADD COLUMN last_allocation_at DATETIME(6) NULL,
    ADD COLUMN previous_owner VARCHAR(32) NULL;

CREATE TABLE crm_customer_assignment_event (
    id BIGINT NOT NULL AUTO_INCREMENT,
    customer_id BIGINT NOT NULL,
    previous_owner VARCHAR(32) NOT NULL,
    owner VARCHAR(32) NOT NULL,
    operator VARCHAR(64) NOT NULL,
    assigned_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_assignment_event_customer_time (customer_id, assigned_at),
    CONSTRAINT fk_assignment_event_customer FOREIGN KEY (customer_id) REFERENCES crm_customer (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
