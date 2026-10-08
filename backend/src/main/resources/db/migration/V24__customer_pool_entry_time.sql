ALTER TABLE crm_customer ADD COLUMN pool_entered_at DATETIME(6) NULL;
CREATE INDEX idx_customer_pool_entry_time ON crm_customer (owner, pool_entered_at);

-- Only explicit pool events are evidence of entry. Never infer from profile dates.
UPDATE crm_customer c
JOIN (
    SELECT customer_id, MAX(entered_at) AS entered_at
    FROM (
        SELECT customer_id, assigned_at AS entered_at
        FROM crm_customer_assignment_event
        WHERE owner = '公海' AND previous_owner <> '公海'
        UNION ALL
        SELECT customer_id, created_at AS entered_at
        FROM crm_customer_operation_event
        WHERE operation_type IN ('移入公海', '系统自动归海')
    ) pool_events
    GROUP BY customer_id
) history ON history.customer_id = c.id
SET c.pool_entered_at = history.entered_at;
