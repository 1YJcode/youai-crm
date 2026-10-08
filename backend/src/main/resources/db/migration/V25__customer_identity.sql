-- Names are display snapshots only. Unresolved legacy records remain admin-only.
ALTER TABLE crm_customer ADD COLUMN owner_id BIGINT NULL;
CREATE INDEX idx_customer_owner_id ON crm_customer(owner_id);
ALTER TABLE crm_customer ADD CONSTRAINT fk_customer_owner FOREIGN KEY (owner_id) REFERENCES crm_user(id);

CREATE TABLE crm_customer_collaborator (
    customer_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (customer_id, user_id),
    CONSTRAINT fk_customer_collaborator_customer FOREIGN KEY (customer_id) REFERENCES crm_customer(id) ON DELETE CASCADE,
    CONSTRAINT fk_customer_collaborator_user FOREIGN KEY (user_id) REFERENCES crm_user(id) ON DELETE CASCADE
);
