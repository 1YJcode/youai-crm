ALTER TABLE crm_customer
    ADD COLUMN customer_type VARCHAR(32) NULL,
    ADD COLUMN last_login_at DATETIME(6) NULL,
    ADD COLUMN avatar_url VARCHAR(1000) NULL;
