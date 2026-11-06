-- Keep financial references valid when the public order number changes.
ALTER TABLE crm_order_refund DROP FOREIGN KEY fk_refund_order_no;
ALTER TABLE crm_order_refund ADD CONSTRAINT fk_refund_order_no FOREIGN KEY (order_no)
    REFERENCES crm_sales_order(order_no) ON UPDATE CASCADE;
ALTER TABLE crm_ledger_account DROP FOREIGN KEY fk_ledger_order_no;
ALTER TABLE crm_ledger_account ADD CONSTRAINT fk_ledger_order_no FOREIGN KEY (order_no)
    REFERENCES crm_sales_order(order_no) ON UPDATE CASCADE;
CREATE TABLE crm_order_number_mapping (
    old_order_no VARCHAR(32) NOT NULL PRIMARY KEY,
    new_order_no VARCHAR(6) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
