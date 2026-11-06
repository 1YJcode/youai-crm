ALTER TABLE crm_sales_order ADD COLUMN customer_no VARCHAR(32) NULL;
CREATE INDEX idx_order_customer_no ON crm_sales_order(customer_no);
-- Only unambiguous legacy customer references may be linked automatically.
UPDATE crm_sales_order SET customer_no = (
    SELECT MIN(c.customer_no) FROM crm_customer c WHERE c.name = crm_sales_order.customer_name
) WHERE (SELECT COUNT(*) FROM crm_customer c WHERE c.name = crm_sales_order.customer_name) = 1;
-- For namesakes, require a unique matching customer with the same stable owner identity.
UPDATE crm_sales_order SET customer_no = (
    SELECT MIN(c.customer_no) FROM crm_customer c
    WHERE c.name = crm_sales_order.customer_name AND c.owner_id = crm_sales_order.owner_id
) WHERE customer_no IS NULL AND owner_id IS NOT NULL AND (
    SELECT COUNT(*) FROM crm_customer c
    WHERE c.name = crm_sales_order.customer_name AND c.owner_id = crm_sales_order.owner_id
) = 1;
