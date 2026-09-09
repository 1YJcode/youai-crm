CREATE TABLE crm_customer (
    id BIGINT NOT NULL AUTO_INCREMENT,
    customer_no VARCHAR(32) NOT NULL,
    name VARCHAR(64) NOT NULL,
    phone VARCHAR(24) NOT NULL,
    company VARCHAR(128) NOT NULL,
    source VARCHAR(32) NOT NULL,
    owner VARCHAR(32) NOT NULL,
    stage VARCHAR(32) NOT NULL,
    level VARCHAR(32) NOT NULL,
    expected_amount DECIMAL(14, 2) NOT NULL DEFAULT 0,
    city VARCHAR(64) NULL,
    note VARCHAR(1000) NULL,
    last_contact_at DATETIME(6) NULL,
    next_follow_at DATETIME(6) NULL,
    version BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_customer_no (customer_no),
    UNIQUE KEY uk_customer_phone (phone),
    KEY idx_customer_stage (stage),
    KEY idx_customer_owner (owner),
    KEY idx_customer_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE crm_customer_tag (
    customer_id BIGINT NOT NULL,
    tag VARCHAR(32) NOT NULL,
    sort_order INT NOT NULL,
    PRIMARY KEY (customer_id, sort_order),
    CONSTRAINT fk_customer_tag_customer FOREIGN KEY (customer_id) REFERENCES crm_customer (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE crm_follow_up_task (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(160) NOT NULL,
    customer_name VARCHAR(64) NOT NULL,
    owner VARCHAR(32) NOT NULL,
    due_at DATETIME(6) NOT NULL,
    task_type VARCHAR(32) NOT NULL,
    status VARCHAR(24) NOT NULL,
    priority VARCHAR(24) NOT NULL,
    completed BIT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_task_due_at (due_at),
    KEY idx_task_owner_status (owner, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE crm_sales_order (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_no VARCHAR(32) NOT NULL,
    customer_name VARCHAR(64) NOT NULL,
    product VARCHAR(128) NOT NULL,
    amount DECIMAL(14, 2) NOT NULL,
    paid_amount DECIMAL(14, 2) NOT NULL DEFAULT 0,
    payment_status VARCHAR(24) NOT NULL,
    service_status VARCHAR(24) NOT NULL,
    owner VARCHAR(32) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_order_created_at (created_at),
    KEY idx_order_payment_status (payment_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

