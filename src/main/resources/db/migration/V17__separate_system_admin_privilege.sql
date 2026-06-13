INSERT IGNORE INTO privileges (id, code, description, created_at, updated_at) VALUES
('10000000-0000-0000-0000-000000000021', 'isSystemAdmin', 'Access system administrator features', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT IGNORE INTO role_privileges (role_id, privilege_id) VALUES
('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000021');

DELETE FROM role_privileges
WHERE role_id = '00000000-0000-0000-0000-000000000001'
  AND privilege_id = '10000000-0000-0000-0000-000000000020';
