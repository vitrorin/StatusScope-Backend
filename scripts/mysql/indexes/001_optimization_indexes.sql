-- ============================================================================
-- Optimización: Índices compuestos para tablas críticas
-- ============================================================================
-- Propósito:        Reducir full table scans en consultas frecuentes de la
--                   aplicación (dashboards, autenticación, reportería).
-- Estrategia:       Índices de cobertura (covering indexes) y compuestos que
--                   alinean columnas de filtro + orden para evitar filesort.
-- Entorno:          MySQL 8.0+, InnoDB, Cloud SQL for MySQL.
-- ============================================================================

DELIMITER //

DROP PROCEDURE IF EXISTS add_index_if_missing //

CREATE PROCEDURE add_index_if_missing(
    IN p_table_name VARCHAR(64),
    IN p_index_name VARCHAR(64),
    IN p_create_sql TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = p_table_name
          AND index_name = p_index_name
    ) THEN
        SET @ddl = p_create_sql;
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //

DELIMITER ;

-- ============================================================================
-- 1. outbreaks — filtro por (status, scope) + ubicación geográfica
-- ============================================================================
-- Consultas impactadas:
--   - findActiveMunicipalByStateId     → WHERE status='ACTIVE' AND scope='MUNICIPALITY' AND state_id=?
--   - findActiveStateByStateId         → WHERE status='ACTIVE' AND scope='STATE' AND state_id=?
--   - findActiveByMunicipalityIdsOrStateId → WHERE status='ACTIVE' AND (scope='MUNICIPALITY' AND municipality_id IN ...)
--                                            OR (scope='STATE' AND state_id=?)
-- Sin índice:       Full scan + filesort en 10 000+ filas de outbreak.
-- Con índice:       Index seek por status+scope, luego lookup por ubicación.
-- ============================================================================

CALL add_index_if_missing(
    'outbreaks',
    'idx_outbreaks_status_scope_municipality',
    'CREATE INDEX idx_outbreaks_status_scope_municipality ON outbreaks (status, scope, municipality_id)'
);

CALL add_index_if_missing(
    'outbreaks',
    'idx_outbreaks_status_scope_state',
    'CREATE INDEX idx_outbreaks_status_scope_state ON outbreaks (status, scope, state_id)'
);

-- ============================================================================
-- 2. operational_recommendations — feed de admin por hospital
-- ============================================================================
-- Consultas impactadas:
--   - findByHospitalId       → WHERE hospital_id=? ORDER BY created_at DESC
--   - findByHospitalIdAndStatus  → WHERE hospital_id=? AND status=? ORDER BY created_at DESC
--   - findByHospitalIdAndSeverity → WHERE hospital_id=? AND severity=? ORDER BY created_at DESC
--   - GetAdminDashboardSummaryUseCase → WHERE hospital_id=? AND status NOT IN ('COMPLETED','REJECTED') LIMIT 5
-- Sin índice:       Filtro por hospital_id usa FK index (~50 filas) pero
--                   ORDER BY created_at causa filesort.
-- Con índice:       Index scan ordenado sin filesort, early exit con LIMIT.
-- ============================================================================

CALL add_index_if_missing(
    'operational_recommendations',
    'idx_recs_hospital_created',
    'CREATE INDEX idx_recs_hospital_created ON operational_recommendations (hospital_id, created_at DESC)'
);

CALL add_index_if_missing(
    'operational_recommendations',
    'idx_recs_hospital_status_created',
    'CREATE INDEX idx_recs_hospital_status_created ON operational_recommendations (hospital_id, status, created_at DESC)'
);

CALL add_index_if_missing(
    'operational_recommendations',
    'idx_recs_hospital_severity_created',
    'CREATE INDEX idx_recs_hospital_severity_created ON operational_recommendations (hospital_id, severity, created_at DESC)'
);

-- ============================================================================
-- 3. hospital_resource_snapshots — último snapshot por hospital
-- ============================================================================
-- Consulta impactada:
--   - findLatestSnapshotByHospitalId → WHERE hospital_id=? ORDER BY captured_at DESC LIMIT 1
-- Sin índice:       Filtra por hospital_id y luego recorre todas las filas
--                   ordenando por fecha.
-- Con índice:       (hospital_id, captured_at DESC) → B-tree descendente,
--                   LIMIT 1 es una sola lectura.
-- ============================================================================

CALL add_index_if_missing(
    'hospital_resource_snapshots',
    'idx_snapshots_hospital_captured',
    'CREATE INDEX idx_snapshots_hospital_captured ON hospital_resource_snapshots (hospital_id, captured_at DESC)'
);

-- ============================================================================
-- 4. hospital_department_resources — listado por hospital + status
-- ============================================================================
-- Consulta impactada:
--   - findByHospitalId → WHERE hospital_id=? ORDER BY department_name ASC
-- Dashboard admin usa status para filtrar departamentos con HIGH_LOAD.
-- ============================================================================

CALL add_index_if_missing(
    'hospital_department_resources',
    'idx_dept_hospital_status',
    'CREATE INDEX idx_dept_hospital_status ON hospital_department_resources (hospital_id, status)'
);

-- ============================================================================
-- 5. hospital_inventory_items — listado por hospital + status/category
-- ============================================================================
-- Consulta impactada:
--   - findByHospitalId → WHERE hospital_id=? ORDER BY category ASC, item_name ASC
-- ============================================================================

CALL add_index_if_missing(
    'hospital_inventory_items',
    'idx_inventory_hospital_category',
    'CREATE INDEX idx_inventory_hospital_category ON hospital_inventory_items (hospital_id, category, item_name)'
);

CALL add_index_if_missing(
    'hospital_inventory_items',
    'idx_inventory_hospital_status',
    'CREATE INDEX idx_inventory_hospital_status ON hospital_inventory_items (hospital_id, status)'
);

-- ============================================================================
-- 6. hospital_staffing_profiles — listado por hospital
-- ============================================================================
-- Consulta impactada: WHERE hospital_id=? ORDER BY role_name ASC
-- ============================================================================

CALL add_index_if_missing(
    'hospital_staffing_profiles',
    'idx_staff_hospital_role',
    'CREATE INDEX idx_staff_hospital_role ON hospital_staffing_profiles (hospital_id, role_name)'
);

-- ============================================================================
-- 7. patients — búsqueda por hospital + nombre
-- ============================================================================
-- Consulta impactada: búsqueda de pacientes en el asistente de diagnóstico.
-- ============================================================================

CALL add_index_if_missing(
    'patients',
    'idx_patients_hospital_name',
    'CREATE INDEX idx_patients_hospital_name ON patients (hospital_id, full_name)'
);

-- ============================================================================
-- 8. operational_recommendation_audit — auditoría por recomendación + fecha
-- ============================================================================
-- Consulta impactada: WHERE recommendation_id=? ORDER BY created_at ASC
-- ============================================================================

CALL add_index_if_missing(
    'operational_recommendation_audit',
    'idx_audit_recommendation_created',
    'CREATE INDEX idx_audit_recommendation_created ON operational_recommendation_audit (recommendation_id, created_at ASC)'
);

-- ============================================================================
-- 9. hospital_inventory_movements — historial por ítem
-- ============================================================================
-- Consulta impactada: JOIN con inventory_item_id para historial de movimientos.
-- ============================================================================

CALL add_index_if_missing(
    'hospital_inventory_movements',
    'idx_movements_item_created',
    'CREATE INDEX idx_movements_item_created ON hospital_inventory_movements (inventory_item_id, created_at DESC)'
);

DROP PROCEDURE IF EXISTS add_index_if_missing;

-- ============================================================================
-- Justificación técnica
-- ============================================================================
-- 1. Todos los índices compuestos siguen el principio "equality primero,
--    luego range/sort". Las columnas de filtro exacto (status, hospital_id,
--    scope) van primero, seguidas de columnas de ordenamiento (created_at).
-- 2. Se evitan índices redundantes: outbreaks ya tiene FK en disease_id,
--    municipality_id, state_id (creados por Hibernate). Los nuevos índices
--    cubren combinaciones de filtro que las FK no servían.
-- 3. Ningún índice excede 3 columnas; esto mantiene bajo el overhead de
--    escritura (write amplification) en tablas de alta inserción.
-- 4. Los índices DESC en created_at/captured_at permiten que InnoDB lea las
--    filas recientes primero. El optimizador aún puede requerir filesort en
--    predicados de rango como status NOT IN; validar siempre con EXPLAIN.
-- ============================================================================

-- ============================================================================
-- Rollback
-- ============================================================================
-- DROP INDEX IF EXISTS idx_outbreaks_status_scope_municipality ON outbreaks;
-- DROP INDEX IF EXISTS idx_outbreaks_status_scope_state ON outbreaks;
-- DROP INDEX IF EXISTS idx_recs_hospital_created ON operational_recommendations;
-- DROP INDEX IF EXISTS idx_recs_hospital_status_created ON operational_recommendations;
-- DROP INDEX IF EXISTS idx_recs_hospital_severity_created ON operational_recommendations;
-- DROP INDEX IF EXISTS idx_snapshots_hospital_captured ON hospital_resource_snapshots;
-- DROP INDEX IF EXISTS idx_dept_hospital_status ON hospital_department_resources;
-- DROP INDEX IF EXISTS idx_inventory_hospital_category ON hospital_inventory_items;
-- DROP INDEX IF EXISTS idx_inventory_hospital_status ON hospital_inventory_items;
-- DROP INDEX IF EXISTS idx_staff_hospital_role ON hospital_staffing_profiles;
-- DROP INDEX IF EXISTS idx_patients_hospital_name ON patients;
-- DROP INDEX IF EXISTS idx_audit_recommendation_created ON operational_recommendation_audit;
-- DROP INDEX IF EXISTS idx_movements_item_created ON hospital_inventory_movements;
