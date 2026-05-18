-- Add the ADMIN role with full privileges across all platform features.
INSERT INTO roles (id, code, name, description, created_at, updated_at) VALUES
('00000000-0000-0000-0000-000000000004', 'ADMIN', 'Administrator', 'Full administrative access across all platform features', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Assign every privilege in the table to the ADMIN role.
INSERT INTO role_privileges (role_id, privilege_id)
SELECT '00000000-0000-0000-0000-000000000004', id FROM privileges;
