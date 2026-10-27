ALTER TABLE crm_call_record ADD COLUMN uploaded_at DATETIME NULL;
UPDATE crm_call_record c SET uploaded_at = (SELECT r.uploaded_at FROM crm_call_recording r WHERE r.call_id = c.id)
WHERE EXISTS (SELECT 1 FROM crm_call_recording r WHERE r.call_id = c.id);
