ALTER TABLE crm_knowledge_article ADD COLUMN publication_status VARCHAR(24) NOT NULL DEFAULT 'PUBLISHED', ADD COLUMN reviewed_by VARCHAR(128), ADD COLUMN reviewed_at DATETIME(6), ADD COLUMN review_note VARCHAR(1000);
CREATE INDEX idx_knowledge_publication ON crm_knowledge_article(publication_status);
ALTER TABLE crm_knowledge_article ADD COLUMN reviewed_source_version BIGINT;
