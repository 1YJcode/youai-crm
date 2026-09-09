CREATE TABLE crm_ledger_account (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ledger_no VARCHAR(32) NOT NULL,
    store_name VARCHAR(64) NOT NULL,
    source_merchant VARCHAR(64) NOT NULL,
    flow_no VARCHAR(48) NOT NULL,
    order_no VARCHAR(32) NOT NULL,
    order_amount DECIMAL(14, 2) NOT NULL,
    fee_amount DECIMAL(14, 2) NOT NULL,
    ledger_amount DECIMAL(14, 2) NOT NULL,
    beneficiary VARCHAR(64) NOT NULL,
    execution_status VARCHAR(24) NOT NULL,
    allocated_at DATETIME(6) NULL,
    paid_out_at DATETIME(6) NULL,
    transaction_order_no VARCHAR(48) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ledger_no (ledger_no),
    UNIQUE KEY uk_ledger_order_no (order_no),
    KEY idx_ledger_store_status (store_name, execution_status),
    KEY idx_ledger_allocated_at (allocated_at),
    CONSTRAINT fk_ledger_order_no FOREIGN KEY (order_no) REFERENCES crm_sales_order(order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO crm_ledger_account
    (ledger_no, store_name, source_merchant, flow_no, order_no, order_amount, fee_amount, ledger_amount, beneficiary, execution_status, allocated_at, paid_out_at, transaction_order_no, created_at, updated_at)
SELECT CONCAT('FZ', DATE_FORMAT(o.created_at, '%y%m%d'), LPAD(o.id, 6, '0')),
       '优爱天津店', '优爱天津店直营网商户', CONCAT('LS', DATE_FORMAT(o.created_at, '%Y%m%d%H%i%s'), LPAD(o.id, 4, '0')),
       o.order_no, o.amount, ROUND(o.paid_amount * 0.006, 2), ROUND(o.paid_amount * 0.20, 2),
       CONCAT(o.owner, ' · 渠道账户'),
       CASE WHEN o.payment_status = '已支付' THEN '执行成功' WHEN o.paid_amount > 0 THEN '待执行' ELSE '未满足条件' END,
       CASE WHEN o.paid_amount > 0 THEN DATE_ADD(o.created_at, INTERVAL 1 DAY) ELSE NULL END,
       CASE WHEN o.payment_status = '已支付' THEN DATE_ADD(o.created_at, INTERVAL 2 DAY) ELSE NULL END,
       CASE WHEN o.payment_status = '已支付' THEN CONCAT('JY', DATE_FORMAT(o.created_at, '%Y%m%d'), LPAD(o.id, 8, '0')) ELSE NULL END,
       o.created_at, o.created_at
FROM crm_sales_order o;
