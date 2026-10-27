-- Only unambiguous historical caller names can establish a stable identity.
UPDATE crm_call_record
SET agent_user_id = (SELECT MIN(u.id) FROM crm_user u
                    WHERE u.display_name = crm_call_record.agent)
WHERE agent_user_id IS NULL
  AND (SELECT COUNT(*) FROM crm_user u WHERE u.display_name = crm_call_record.agent) = 1;
CREATE INDEX idx_call_agent_started ON crm_call_record(agent_user_id, started_at);
