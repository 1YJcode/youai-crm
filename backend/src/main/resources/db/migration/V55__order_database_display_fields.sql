-- Store affiliation comes from the same departments used by administrator/sales accounts.
ALTER TABLE crm_department ADD COLUMN store_name VARCHAR(64) NULL;
UPDATE crm_department SET store_name='优爱天津店' WHERE code IN ('ADMIN','SALES');
ALTER TABLE crm_sales_order ADD COLUMN store_name VARCHAR(64) NULL;
UPDATE crm_sales_order o JOIN crm_user u ON u.id=o.owner_id JOIN crm_department d ON d.id=u.department_id
SET o.store_name=d.store_name;
-- Persist the product and service rules previously applied only when drawing the order table.
UPDATE crm_sales_order SET product='线下VIP', service_status=CASE WHEN payment_status='已支付' THEN '待启用' ELSE '' END;
CREATE INDEX idx_invitation_related_order ON crm_invitation_record(related_order_no,id);
