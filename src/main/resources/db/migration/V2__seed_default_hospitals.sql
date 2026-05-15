UPDATE hospitals
SET code = 'HGZ-21',
    name = 'Hospital General Zona 21',
    address = 'Av. Principal 100, Monterrey',
    phone = '+52 81 0000 0001',
    invite_code = 'INVITE-HGZ21',
    active = TRUE,
    postal_code = '64000',
    bed_count = 240,
    doctor_count = 72,
    nurse_count = 180,
    municipality_id = '42000000-0000-0000-0000-000000001003',
    latitude = 25.6866142,
    longitude = -100.3161126,
    updated_at = CURRENT_TIMESTAMP
WHERE id = '30000000-0000-0000-0000-000000000001';

INSERT INTO hospitals (id, code, name, address, phone, invite_code, active, postal_code, bed_count, doctor_count, nurse_count, municipality_id, latitude, longitude, created_at, updated_at)
SELECT '30000000-0000-0000-0000-000000000001', 'HGZ-21', 'Hospital General Zona 21', 'Av. Principal 100, Monterrey', '+52 81 0000 0001', 'INVITE-HGZ21', TRUE, '64000', 240, 72, 180, '42000000-0000-0000-0000-000000001003', 25.6866142, -100.3161126, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM hospitals WHERE id = '30000000-0000-0000-0000-000000000001'
);

UPDATE hospitals
SET code = 'HRE-05',
    name = 'Hospital Regional Este',
    address = 'Calle 5 Ote 200, Xochimilco',
    phone = '+52 55 0000 0002',
    invite_code = 'INVITE-HRE05',
    active = TRUE,
    postal_code = '16000',
    bed_count = 320,
    doctor_count = 96,
    nurse_count = 240,
    municipality_id = '42000000-0000-0000-0000-000000000287',
    latitude = 19.2572310,
    longitude = -99.1037420,
    updated_at = CURRENT_TIMESTAMP
WHERE id = '30000000-0000-0000-0000-000000000002';

INSERT INTO hospitals (id, code, name, address, phone, invite_code, active, postal_code, bed_count, doctor_count, nurse_count, municipality_id, latitude, longitude, created_at, updated_at)
SELECT '30000000-0000-0000-0000-000000000002', 'HRE-05', 'Hospital Regional Este', 'Calle 5 Ote 200, Xochimilco', '+52 55 0000 0002', 'INVITE-HRE05', TRUE, '16000', 320, 96, 240, '42000000-0000-0000-0000-000000000287', 19.2572310, -99.1037420, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM hospitals WHERE id = '30000000-0000-0000-0000-000000000002'
);
