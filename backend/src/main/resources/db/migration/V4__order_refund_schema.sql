CREATE TABLE crm_order_refund (
    id VARCHAR(40) NOT NULL,
    order_no VARCHAR(32) NOT NULL,
    customer_name VARCHAR(64) NOT NULL,
    amount DECIMAL(14, 2) NOT NULL,
    reason VARCHAR(160) NOT NULL,
    applicant VARCHAR(64) NOT NULL,
    owner VARCHAR(32) NOT NULL,
    status VARCHAR(24) NOT NULL,
    reviewer VARCHAR(64) NULL,
    created_at DATETIME(6) NOT NULL,
    reviewed_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    KEY idx_refund_owner_created (owner, created_at),
    KEY idx_refund_order (order_no),
    CONSTRAINT fk_refund_order_no FOREIGN KEY (order_no) REFERENCES crm_sales_order (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
