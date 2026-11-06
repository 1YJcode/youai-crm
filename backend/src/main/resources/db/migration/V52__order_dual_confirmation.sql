ALTER TABLE crm_sales_order
    ADD COLUMN confirmation_sales_id BIGINT NULL,
    ADD COLUMN confirmation_sales_name VARCHAR(64) NULL,
    ADD COLUMN confirmation_admin_id BIGINT NULL,
    ADD COLUMN confirmation_admin_name VARCHAR(64) NULL,
    ADD COLUMN sales_confirmed_at DATETIME(6) NULL,
    ADD COLUMN admin_confirmed_at DATETIME(6) NULL,
    ADD COLUMN legacy_performance_confirmed BIT NOT NULL DEFAULT b'0';
UPDATE crm_sales_order o LEFT JOIN crm_customer c ON c.customer_no=o.customer_no
LEFT JOIN crm_user s ON s.id=CASE WHEN c.id IS NOT NULL THEN c.owner_id ELSE o.owner_id END
SET o.confirmation_sales_id=s.id, o.confirmation_sales_name=COALESCE(s.display_name, o.owner),
    o.legacy_performance_confirmed=o.performance_confirmed, o.performance_confirmed=b'0';
UPDATE crm_sales_order SET confirmation_admin_id=(SELECT id FROM crm_user WHERE username='admin' AND enabled=b'1'),
    confirmation_admin_name=(SELECT display_name FROM crm_user WHERE username='admin' AND enabled=b'1');
CREATE INDEX idx_order_confirmation_sales ON crm_sales_order(confirmation_sales_id);
