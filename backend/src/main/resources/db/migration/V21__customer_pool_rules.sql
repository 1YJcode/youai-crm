ALTER TABLE crm_customer
    ADD COLUMN pool_entry_type VARCHAR(64) NULL;

CREATE TABLE crm_system_setting (
    setting_key VARCHAR(100) NOT NULL,
    setting_value VARCHAR(255) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO crm_system_setting (setting_key, setting_value, updated_at)
VALUES ('customer.pool.auto_release.enabled', 'true', CURRENT_TIMESTAMP(6)),
       ('customer.pool.auto_release.days', '7', CURRENT_TIMESTAMP(6));

UPDATE crm_customer c
LEFT JOIN (
    SELECT event.customer_id, event.assignment_type
    FROM crm_customer_assignment_event event
    INNER JOIN (
        SELECT customer_id, MAX(id) AS latest_id
        FROM crm_customer_assignment_event
        WHERE owner = '公海'
        GROUP BY customer_id
    ) latest ON latest.latest_id = event.id
) last_pool_event ON last_pool_event.customer_id = c.id
SET c.pool_entry_type = CASE
    WHEN last_pool_event.assignment_type = '系统自动归海' THEN '未及时跟进，系统推进'
    ELSE '主动放弃'
END
WHERE c.owner = '公海';
