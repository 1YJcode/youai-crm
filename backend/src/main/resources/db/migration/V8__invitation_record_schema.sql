CREATE TABLE crm_invitation_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    invitation_no VARCHAR(32) NOT NULL,
    customer_no VARCHAR(32) NOT NULL,
    inviter VARCHAR(32) NOT NULL,
    department VARCHAR(64) NOT NULL,
    invitation_method VARCHAR(32) NOT NULL,
    store_name VARCHAR(64) NOT NULL,
    scheduled_at DATETIME(6) NOT NULL,
    arrival_at DATETIME(6) NULL,
    arrival_status VARCHAR(32) NOT NULL,
    marital_status VARCHAR(24) NOT NULL,
    annual_income VARCHAR(32) NOT NULL,
    source VARCHAR(64) NOT NULL,
    referrer VARCHAR(64) NULL,
    related_order_no VARCHAR(32) NULL,
    remark VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_invitation_no (invitation_no),
    KEY idx_invitation_customer_no (customer_no),
    KEY idx_invitation_scheduled_at (scheduled_at),
    KEY idx_invitation_inviter (inviter),
    CONSTRAINT fk_invitation_customer_no FOREIGN KEY (customer_no) REFERENCES crm_customer(customer_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO crm_invitation_record
    (invitation_no, customer_no, inviter, department, invitation_method, store_name, scheduled_at, arrival_at, arrival_status, marital_status, annual_income, source, referrer, related_order_no, remark, created_at, updated_at)
SELECT CONCAT('YQ', DATE_FORMAT(NOW(), '%y%m%d'), LPAD(c.id, 5, '0')),
       c.customer_no, c.owner, '销售部',
       CASE MOD(c.id, 3) WHEN 0 THEN '电话邀约' WHEN 1 THEN '微信邀约' ELSE '自然到店' END,
       '优爱天津店',
       DATE_ADD(DATE(c.created_at), INTERVAL (10 + MOD(c.id, 8)) HOUR),
       CASE WHEN MOD(c.id, 4) IN (0, 1) THEN DATE_ADD(DATE(c.created_at), INTERVAL (11 + MOD(c.id, 7)) HOUR) ELSE NULL END,
       CASE WHEN MOD(c.id, 4) IN (0, 1) THEN '会员已到本店1次' WHEN MOD(c.id, 4) = 2 THEN '待到店' ELSE '已取消' END,
       COALESCE(NULLIF(c.marital_status, ''), '未婚'),
       COALESCE(NULLIF(c.annual_income, ''), '未填写'),
       c.source,
       NULL,
       (SELECT o.order_no FROM crm_sales_order o WHERE o.customer_name = c.name ORDER BY o.id DESC LIMIT 1),
       COALESCE(NULLIF(c.remark, ''), NULLIF(c.note, ''), '—'),
       c.created_at, c.updated_at
FROM crm_customer c;
