ALTER TABLE crm_knowledge_article ADD COLUMN company_case BIT NOT NULL DEFAULT 0;
ALTER TABLE crm_knowledge_article ADD COLUMN derived_from_id BIGINT NULL;
CREATE UNIQUE INDEX uk_knowledge_derived_from ON crm_knowledge_article(derived_from_id);
-- Match crm_customer.customer_no regardless of the server's default collation.
ALTER TABLE crm_knowledge_article MODIFY COLUMN customer_no VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL;
