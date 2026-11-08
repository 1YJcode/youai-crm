ALTER TABLE crm_message ADD COLUMN delivery_status VARCHAR(16) NOT NULL DEFAULT 'LOCAL';
ALTER TABLE crm_message ADD COLUMN recipient_phone VARCHAR(32);
ALTER TABLE crm_message ADD COLUMN request_key VARCHAR(64);
ALTER TABLE crm_message ADD COLUMN gateway_message_id VARCHAR(128);
ALTER TABLE crm_message ADD COLUMN delivery_error VARCHAR(200);
ALTER TABLE crm_message ADD COLUMN delivery_updated_at DATETIME(6);
CREATE UNIQUE INDEX uk_message_request ON crm_message (conversation_id, request_key);
CREATE INDEX idx_message_delivery ON crm_message (delivery_status, delivery_updated_at, id);
