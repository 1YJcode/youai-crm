-- Convert legacy customer identifiers to compact 10-digit numbers.
-- Keep denormalized communication references aligned with crm_customer.
UPDATE crm_call_record call_record
JOIN crm_customer customer ON customer.customer_no = call_record.customer_no
SET call_record.customer_no = CAST(1430038037 + customer.id AS CHAR);

UPDATE crm_conversation conversation
JOIN crm_customer customer ON customer.customer_no = conversation.customer_no
SET conversation.customer_no = CAST(1430038037 + customer.id AS CHAR);

UPDATE crm_customer
SET customer_no = CAST(1430038037 + id AS CHAR);
