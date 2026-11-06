CREATE TABLE crm_sales_contract (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 contract_no VARCHAR(64) NOT NULL UNIQUE,
 order_no VARCHAR(32) NOT NULL UNIQUE,
 status VARCHAR(24) NOT NULL DEFAULT '待审核',
 created_at DATETIME(6) NOT NULL,
 reviewed_at DATETIME(6) NULL,
 reviewed_by BIGINT NULL,
 review_note VARCHAR(500) NULL,
 CONSTRAINT fk_contract_order FOREIGN KEY (order_no) REFERENCES crm_sales_order(order_no) ON UPDATE CASCADE,
 CONSTRAINT fk_contract_reviewer FOREIGN KEY (reviewed_by) REFERENCES crm_user(id),
 KEY idx_contract_status_created(status,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO crm_sales_contract(contract_no,order_no,status,created_at)
 SELECT CONCAT('HT-',order_no),order_no,'待审核',created_at FROM crm_sales_order;
