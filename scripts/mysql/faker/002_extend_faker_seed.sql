-- ============================================================================
-- Extensión Faker: Datos sintéticos para pruebas volumétricas
-- ============================================================================
-- Propósito:        Escalar la base sintética de 2 a 10 hospitales con
--                   recursos operativos, inventarios, personal y contactos
--                   para pruebas de carga realistas.
-- Volumen:          10 hospitales, 30 departamentos, 60 perfiles de personal,
--                   60 items de inventario, 30 contactos, 9 grupos,
--                   30 recomendaciones operativas, 10 snapshots.
-- Uso:              Ejecutar DESPUÉS de import.sql (perfil dev/persistent).
-- ============================================================================

-- ============================================================================
-- 1. Hospitales adicionales (8 nuevos, total 10)
-- ============================================================================
INSERT INTO hospitals (id, code, name, address, phone, invite_code, active,
    postal_code, bed_count, doctor_count, nurse_count, municipality_id,
    latitude, longitude, created_at, updated_at)
SELECT
    UUID(),
    CONCAT('HOSP-', LPAD(seq, 3, '0')),
    CONCAT('Hospital ', ELT(seq - 2,
        'Central', 'Norte', 'Sur', 'Oriente', 'Poniente',
        'Universitario', 'Infantil', 'Militar')),
    CONCAT('Direccion ', seq, ', Mexico'),
    CONCAT('+52 55 ', LPAD(FLOOR(RAND() * 10000), 4, '0'), ' ', LPAD(FLOOR(RAND() * 10000), 4, '0')),
    CONCAT('INVITE-', UUID()),
    TRUE,
    LPAD(FLOOR(RAND() * 90000 + 10000), 5, '0'),
    FLOOR(RAND() * 300 + 50),
    FLOOR(RAND() * 80 + 10),
    FLOOR(RAND() * 200 + 30),
    CASE WHEN seq % 2 = 0
         THEN '42000000-0000-0000-0000-000000001003'
         ELSE '42000000-0000-0000-0000-000000000287'
    END,
    CASE WHEN seq % 2 = 0 THEN 25.6866 + RAND() * 0.1 ELSE 19.2572 + RAND() * 0.1 END,
    CASE WHEN seq % 2 = 0 THEN -100.3161 - RAND() * 0.1 ELSE -99.1037 - RAND() * 0.1 END,
    NOW(), NOW()
FROM (SELECT 3 AS seq UNION SELECT 4 UNION SELECT 5 UNION SELECT 6
      UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) t
WHERE NOT EXISTS (
    SELECT 1
    FROM hospitals existing
    WHERE existing.code = CONCAT('HOSP-', LPAD(seq, 3, '0'))
);

-- ============================================================================
-- 2. Snapshots de recursos (1 por hospital)
-- ============================================================================
INSERT INTO hospital_resource_snapshots (id, hospital_id, captured_at,
    total_beds, available_beds, icu_total_beds, icu_available_beds,
    isolation_rooms_total, isolation_rooms_available,
    oxygen_capacity_units, oxygen_available_units,
    doctors_on_shift, nurses_on_shift, specialists_on_shift, source, created_at)
SELECT
    UUID(), h.id, NOW() - INTERVAL FLOOR(RAND() * 48) HOUR,
    h.bed_count, FLOOR(h.bed_count * RAND() * 0.5),
    FLOOR(h.bed_count * 0.1), FLOOR(h.bed_count * 0.1 * RAND()),
    FLOOR(h.bed_count * 0.05), FLOOR(h.bed_count * 0.05 * RAND()),
    FLOOR(RAND() * 500 + 100), FLOOR(RAND() * 300 + 50),
    h.doctor_count, h.nurse_count, FLOOR(RAND() * 10 + 2),
    'MANUAL', NOW()
FROM hospitals h
WHERE h.code BETWEEN 'HOSP-003' AND 'HOSP-010'
  AND NOT EXISTS (
      SELECT 1
      FROM hospital_resource_snapshots existing
      WHERE existing.hospital_id = h.id
  );

-- ============================================================================
-- 3. Departamentos (3 por hospital)
-- ============================================================================
INSERT INTO hospital_department_resources (id, hospital_id, department_code,
    department_name, total_beds, occupied_beds, status, updated_at)
SELECT
    UUID(), h.id, dept.code, dept.name,
    dept.beds, FLOOR(dept.beds * RAND()), dept.default_status, NOW()
FROM hospitals h
CROSS JOIN (
    SELECT 'EMERGENCY' AS code, 'Emergency Department' AS name, 40 AS beds, 'HIGH_LOAD' AS default_status
    UNION SELECT 'GENERAL', 'General Ward', 80, 'NORMAL'
    UNION SELECT 'ICU', 'Intensive Care Unit', 20, 'HIGH_LOAD'
) dept
WHERE h.code BETWEEN 'HOSP-003' AND 'HOSP-010'
  AND NOT EXISTS (
      SELECT 1
      FROM hospital_department_resources existing
      WHERE existing.hospital_id = h.id
        AND existing.department_code = dept.code
  );

-- ============================================================================
-- 4. Perfiles de personal (6 por hospital)
-- ============================================================================
INSERT INTO hospital_staffing_profiles (id, hospital_id, role_code, role_name,
    headcount, on_shift_count, on_call_count, standby_count, updated_at)
SELECT
    UUID(), h.id, staff.code, staff.name,
    staff.headcount, FLOOR(staff.headcount * 0.4), FLOOR(staff.headcount * 0.3), FLOOR(staff.headcount * 0.3), NOW()
FROM hospitals h
CROSS JOIN (
    SELECT 'EMERGENCY_PHYSICIAN' AS code, 'Emergency Physicians' AS name, 10 AS headcount
    UNION SELECT 'ICU_NURSE', 'ICU Nurses', 18
    UNION SELECT 'PULMONOLOGIST', 'Pulmonologists', 5
    UNION SELECT 'INFECTIOUS_DISEASE', 'Infectious Disease Specialists', 4
    UNION SELECT 'GENERAL_PRACTITIONER', 'General Practitioners', 30
    UNION SELECT 'NURSING_STAFF', 'Nursing Staff', 80
) staff
WHERE h.code BETWEEN 'HOSP-003' AND 'HOSP-010'
  AND NOT EXISTS (
      SELECT 1
      FROM hospital_staffing_profiles existing
      WHERE existing.hospital_id = h.id
        AND existing.role_code = staff.code
  );

-- ============================================================================
-- 5. Inventarios (6 items por hospital)
-- ============================================================================
INSERT INTO hospital_inventory_items (id, hospital_id, item_code, item_name,
    category, current_quantity, capacity_quantity, unit,
    critical_threshold, target_quantity, status, updated_at)
SELECT
    UUID(), h.id, inv.code, inv.name, inv.category,
    inv.current, inv.capacity, inv.unit, inv.threshold, inv.target,
    CASE WHEN inv.current <= inv.threshold THEN 'CRITICAL'
         WHEN inv.current <= inv.threshold * 2 THEN 'LOW'
         ELSE 'ADEQUATE'
    END,
    NOW()
FROM hospitals h
CROSS JOIN (
    SELECT 'N95_MASK' AS code, 'N95 Respirator Masks' AS name, 'PPE' AS category,
           500 AS current, 2000 AS capacity, 'units' AS unit, 200 AS threshold, 1500 AS target
    UNION SELECT 'VENTILATOR', 'Mechanical Ventilators', 'EQUIPMENT',
           8, 20, 'units', 3, 15
    UNION SELECT 'O2_CYLINDER', 'Oxygen Cylinders', 'SUPPLY',
           80, 200, 'cylinders', 40, 150
    UNION SELECT 'IV_FLUID', 'IV Saline Solution', 'PHARMACEUTICAL',
           300, 1000, 'bags', 100, 800
    UNION SELECT 'ISO_GOWN', 'Isolation Gowns', 'PPE',
           150, 1000, 'units', 200, 700
    UNION SELECT 'ANTIVIRAL', 'Antiviral Medications', 'PHARMACEUTICAL',
           60, 300, 'doses', 50, 250
) inv
WHERE h.code BETWEEN 'HOSP-003' AND 'HOSP-010'
  AND NOT EXISTS (
      SELECT 1
      FROM hospital_inventory_items existing
      WHERE existing.hospital_id = h.id
        AND existing.item_code = inv.code
  );

-- ============================================================================
-- 6. Contactos operativos (3 por hospital nuevo)
-- ============================================================================
INSERT INTO hospital_operational_contacts (id, hospital_id, display_name,
    role_label, department_code, contact_channel, contact_value,
    availability_status, is_assignable, is_notifiable, updated_at)
SELECT
    UUID(), h.id,
    ELT(seq, 'Dr. Contacto Urgencias', 'Coord. Operativo', 'Encargado Suministros'),
    ELT(seq, 'Jefe de Urgencias', 'Lider Operativo', 'Coordinador de Suministros'),
    ELT(seq, 'EMERGENCY', 'ADMIN', 'SUPPLY'),
    'PHONE', CONCAT('+52 55 9', LPAD(FLOOR(RAND() * 10000000), 7, '0')),
    'ON_SHIFT', TRUE, TRUE, NOW()
FROM hospitals h
CROSS JOIN (SELECT 1 AS seq UNION SELECT 2 UNION SELECT 3) t
WHERE h.code BETWEEN 'HOSP-003' AND 'HOSP-010'
  AND NOT EXISTS (
      SELECT 1
      FROM hospital_operational_contacts existing
      WHERE existing.hospital_id = h.id
        AND existing.role_label = ELT(seq, 'Jefe de Urgencias', 'Lider Operativo', 'Coordinador de Suministros')
  );

-- ============================================================================
-- 7. Recomendaciones operativas (3 por hospital nuevo)
-- ============================================================================
INSERT INTO operational_recommendations (id, hospital_id, type, severity, status,
    category, title, description, expected_impact, urgency_window,
    confidence_score, created_by_mode, created_at, updated_at)
SELECT
    UUID(), h.id,
    ELT(seq, 'BED_CAPACITY', 'STAFFING', 'ISOLATION'),
    ELT(seq, 'CRITICAL', 'HIGH', 'MEDIUM'),
    ELT(seq, 'NEW', 'NEW', 'NEW'),
    ELT(seq, 'BED_CAPACITY', 'STAFFING', 'ISOLATION'),
    ELT(seq,
        'ICU Capacity Critical',
        'Increase Emergency Physician Staffing',
        'Review PPE Stock Levels'),
    ELT(seq,
        'ICU occupancy critical due to outbreak surge',
        'Doctor-to-bed ratio below recommended threshold',
        'PPE inventory below minimum threshold'),
    ELT(seq,
        'Prevent ICU overflow',
        'Improve patient throughput',
        'Maintain staff protection'),
    ELT(seq, 'Immediately', 'Next rotation', 'Within 48 hours'),
    ELT(seq, 0.95, 0.85, 0.78),
    'RULE_ENGINE', NOW(), NOW()
FROM hospitals h
CROSS JOIN (SELECT 1 AS seq UNION SELECT 2 UNION SELECT 3) t
WHERE h.code BETWEEN 'HOSP-003' AND 'HOSP-010'
  AND NOT EXISTS (
      SELECT 1
      FROM operational_recommendations existing
      WHERE existing.hospital_id = h.id
        AND existing.type = ELT(seq, 'BED_CAPACITY', 'STAFFING', 'ISOLATION')
        AND existing.title = ELT(seq,
            'ICU Capacity Critical',
            'Increase Emergency Physician Staffing',
            'Review PPE Stock Levels')
  );
