ALTER TABLE crm_sales_order ADD COLUMN owner_id BIGINT NULL;
CREATE INDEX idx_order_owner_id ON crm_sales_order(owner_id);
ALTER TABLE crm_sales_order ADD CONSTRAINT fk_order_owner FOREIGN KEY (owner_id) REFERENCES crm_user(id);
-- Resolve only unique legacy references. Ambiguous records remain admin-only.
UPDATE crm_sales_order SET owner_id = (
 SELECT MIN(u.id) FROM crm_user u WHERE u.display_name = crm_sales_order.owner OR u.username = crm_sales_order.owner
) WHERE (SELECT COUNT(*) FROM crm_user u WHERE u.display_name = crm_sales_order.owner OR u.username = crm_sales_order.owner) = 1;
