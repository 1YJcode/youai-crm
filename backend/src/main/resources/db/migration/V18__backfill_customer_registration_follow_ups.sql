INSERT INTO crm_customer_registration_event (customer_id, registration_number, source, operator, created_at)
SELECT customer.id, 1, '历史数据迁移', '系统', COALESCE(customer.created_at, CURRENT_TIMESTAMP(6))
FROM crm_customer customer
LEFT JOIN crm_customer_registration_event event ON event.customer_id = customer.id
WHERE event.id IS NULL;
