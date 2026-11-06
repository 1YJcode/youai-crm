-- Bind the salesperson recorded on the order, independently of customer pool/admin ownership.
UPDATE crm_sales_order o JOIN crm_user s ON s.id=o.owner_id AND s.enabled=b'1'
SET o.sales_confirmed_at=NULL, o.admin_confirmed_at=NULL, o.performance_confirmed=b'0',
    o.confirmation_sales_id=s.id, o.confirmation_sales_name=s.display_name
WHERE NOT EXISTS (SELECT 1 FROM crm_user_role ur JOIN crm_role r ON r.id=ur.role_id WHERE ur.user_id=s.id AND r.code='ADMIN');
