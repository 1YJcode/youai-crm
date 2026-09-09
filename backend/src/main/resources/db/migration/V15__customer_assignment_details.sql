ALTER TABLE crm_customer_assignment_event
    ADD COLUMN assignment_type VARCHAR(32) NULL,
    ADD COLUMN maturity VARCHAR(128) NULL,
    ADD COLUMN reason VARCHAR(500) NULL;
