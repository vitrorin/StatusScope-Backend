-- Add the ADMIN role with full privileges across all platform features.
-- Uses WHERE NOT EXISTS so the migration is idempotent when import.sql already seeded the row.
INSERT INTO roles (id, code, name, description, created_at, updated_at)
SELECT '00000000-0000-0000-0000-000000000004', 'ADMIN', 'Administrator',
       'Full administrative access across all platform features',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'ADMIN');

-- Assign every privilege in the table to the ADMIN role (skip if already assigned).
INSERT INTO role_privileges (role_id, privilege_id)
SELECT '00000000-0000-0000-0000-000000000004', id FROM privileges
WHERE NOT EXISTS (
    SELECT 1 FROM role_privileges WHERE role_id = '00000000-0000-0000-0000-000000000004'
);
