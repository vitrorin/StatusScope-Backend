-- Remove demo operational recommendations that used fixed IDs and made the
-- admin queue look populated even before the rule engine had generated items.

DELETE FROM supply_requests
WHERE recommendation_id IN (
    '24000000-0000-0000-0000-000000000001',
    '24000000-0000-0000-0000-000000000002',
    '24000000-0000-0000-0000-000000000003'
);

DELETE FROM operational_notifications
WHERE recommendation_id IN (
    '24000000-0000-0000-0000-000000000001',
    '24000000-0000-0000-0000-000000000002',
    '24000000-0000-0000-0000-000000000003'
);

DELETE FROM operational_tasks
WHERE recommendation_id IN (
    '24000000-0000-0000-0000-000000000001',
    '24000000-0000-0000-0000-000000000002',
    '24000000-0000-0000-0000-000000000003'
);

DELETE FROM operational_recommendation_audit
WHERE recommendation_id IN (
    '24000000-0000-0000-0000-000000000001',
    '24000000-0000-0000-0000-000000000002',
    '24000000-0000-0000-0000-000000000003'
);

DELETE FROM operational_recommendations
WHERE id IN (
    '24000000-0000-0000-0000-000000000001',
    '24000000-0000-0000-0000-000000000002',
    '24000000-0000-0000-0000-000000000003'
)
AND created_by_mode = 'RULE_ENGINE';
