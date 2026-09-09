ALTER TABLE crm_follow_up_task
    MODIFY COLUMN title VARCHAR(2000) NOT NULL,
    ADD COLUMN customer_no VARCHAR(32) NULL AFTER customer_name,
    ADD COLUMN customer_status VARCHAR(32) NULL AFTER customer_no,
    ADD COLUMN followed_at DATETIME(6) NULL AFTER customer_status,
    ADD KEY idx_task_customer_followed_at (customer_no, followed_at),
    ADD CONSTRAINT fk_task_customer FOREIGN KEY (customer_no) REFERENCES crm_customer (customer_no) ON DELETE SET NULL;
