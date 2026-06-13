-- ============================================================================
-- Benchmark: Consultas representativas con EXPLAIN ANALYZE
-- ============================================================================
-- Propósito:        Evaluar el plan de ejecución de las consultas críticas
--                   antes y después de aplicar índices compuestos.
-- Uso:              Conectar a MySQL 8 (no H2) y ejecutar cada bloque.
--                   La base debe tener datos representativos (~10k outbreaks,
--                   ~5k recommendations, ~1k snapshots) para que el optimizador
--                   escoja planes realistas.
-- ============================================================================

-- ============================================================================
-- Configuración previa
-- ============================================================================
-- Activar ANALYZE extendido (MySQL 8.0.18+):
--   SET SESSION optimizer_trace="enabled=on";
--   SET SESSION statistics_persistent_for=ALL;

-- ============================================================================
-- Consulta 1: Autenticación de usuario (AuthN)
-- ============================================================================
-- Original en:      UserRepositoryImpl.findByExternalAuthId()
-- Frecuencia:       Cada llamada API autenticada (cada request)

EXPLAIN ANALYZE
SELECT DISTINCT u.*
FROM users u
LEFT JOIN user_roles ur ON ur.user_id = u.id
LEFT JOIN roles r ON r.id = ur.role_id
LEFT JOIN role_privileges rp ON rp.role_id = r.id
LEFT JOIN privileges p ON p.id = rp.privilege_id
WHERE u.external_auth_id = 'seed-doctor-1';

-- EXPECTATIVA POST-ÍNDICE:
--   type: const (por UNIQUE en external_auth_id)
--   rows: 1
--   Extra: Using index (covering si solo se proyectan columnas indexadas)

-- ============================================================================
-- Consulta 2: Brotes activos por estado (scope = MUNICIPALITY)
-- ============================================================================
-- Original en:      OutbreakRepositoryImpl.findActiveMunicipalByStateId()
-- Frecuencia:       Dashboard de doctor y admin (cada carga de página)

EXPLAIN ANALYZE
SELECT o.id, o.disease_id, o.case_count, o.confirmation_status, o.started_at,
       m.id AS muni_id, m.name AS muni_name,
       d.name AS disease_name
FROM outbreaks o
JOIN municipalities m ON m.id = o.municipality_id
JOIN diseases d ON d.id = o.disease_id
WHERE o.status = 'ACTIVE'
  AND o.scope = 'MUNICIPALITY'
  AND m.state_id = '40000000-0000-0000-0000-000000000019'
ORDER BY o.case_count DESC;

-- EXPECTATIVA SIN ÍNDICE:
--   type: ALL (outbreaks), Using where; Using filesort
--   rows: ~10000

-- EXPECTATIVA CON IDX (idx_outbreaks_status_scope_municipality):
--   type: ref (outbreaks)
--   rows: ~50
--   Extra: Using index condition; el ORDER BY case_count puede requerir filesort

-- ============================================================================
-- Consulta 3: Brotes activos mixtos (municipio + estado)
-- ============================================================================
-- Original en:      OutbreakRepositoryImpl.findActiveByMunicipalityIdsOrStateId()
-- Frecuencia:       Dashboard de doctor con radio geográfico

EXPLAIN ANALYZE
SELECT o.*
FROM outbreaks o
WHERE o.status = 'ACTIVE'
  AND (
      (o.scope = 'MUNICIPALITY' AND o.municipality_id IN ('42000000-0000-0000-0000-000000001003'))
      OR (o.scope = 'STATE' AND o.state_id = '40000000-0000-0000-0000-000000000019')
  );

-- ============================================================================
-- Consulta 4: Feed de recomendaciones operativas (dashboard admin)
-- ============================================================================
-- Original en:      OperationalRecommendationRepository.findByHospitalId()
--                   GetAdminDashboardSummaryUseCase (limit 5, exclude COMPLETED/REJECTED)
-- Frecuencia:       Dashboard admin (cada carga de página)

EXPLAIN ANALYZE
SELECT id, type, severity, status, title, description, confidence_score,
       created_at
FROM operational_recommendations
WHERE hospital_id = '30000000-0000-0000-0000-000000000001'
  AND status NOT IN ('COMPLETED', 'REJECTED')
ORDER BY created_at DESC
LIMIT 5;

-- EXPECTATIVA SIN ÍNDICE:
--   type: ref (por FK hospital_id)
--   Extra: Using where; Using filesort (recorre ~50 filas, ordena en memoria)

-- EXPECTATIVA CON IDX (idx_recs_hospital_status_created):
--   type: range (por hospital_id + status NOT IN)
--   Extra: Using index condition; Using where
--   Validar filesort con EXPLAIN: status NOT IN puede requerir ordenar segmentos.

-- ============================================================================
-- Consulta 5: Último snapshot de recursos por hospital
-- ============================================================================
-- Original en:      HospitalResourceRepository.findLatestSnapshotByHospitalId()
-- Frecuencia:       Dashboard admin (cada carga de página)

EXPLAIN ANALYZE
SELECT id, captured_at, total_beds, available_beds, icu_total_beds,
       icu_available_beds, doctors_on_shift, nurses_on_shift
FROM hospital_resource_snapshots
WHERE hospital_id = '30000000-0000-0000-0000-000000000001'
ORDER BY captured_at DESC
LIMIT 1;

-- EXPECTATIVA SIN ÍNDICE:
--   type: ref (por FK hospital_id)
--   Extra: Using where; Using filesort

-- EXPECTATIVA CON IDX (idx_snapshots_hospital_captured):
--   type: ref
--   Extra: recorrido por idx_snapshots_hospital_captured; LIMIT 1 → pocas lecturas

-- ============================================================================
-- Consulta 6: Inserción masiva (brotes desde CSV)
-- ============================================================================
-- Original en:      V4__seed_outbreaks.java insertOutbreaks()
--                     (PreparedStatement con executeBatch de 1000+ filas)
-- Métrica:          Rows/second y tiempo total de inserción

-- Medir antes:
SET @start = NOW();
-- ... ejecutar batch de 1000 inserciones ...
SET @end = NOW();
SELECT TIMEDIFF(@end, @start) AS batch_duration,
       1000 / TIMESTAMPDIFF(MICROSECOND, @start, @end) * 1000000 AS rows_per_second;

-- ============================================================================
-- Consulta 7: Búsqueda de pacientes (diagnosis assistant)
-- ============================================================================
-- Original en:      Frontend search + ListDiagnosisDiseasesUseCase (filtro LIKE)

EXPLAIN ANALYZE
SELECT id, code, name
FROM diseases
WHERE LOWER(name) LIKE '%covid%'
   OR LOWER(code) LIKE '%covid%'
ORDER BY name ASC
LIMIT 12;

-- EXPECTATIVA SIN ÍNDICE:
--   type: ALL (full scan)
--   Extra: Using where; Using filesort

-- MEJORA PROPONIBLE: Índice FULLTEXT en name y code para búsqueda tipo LIKE.
-- ALTERNATIVA: Índice compuesto (code, name) para cubrir el ORDER BY.

-- ============================================================================
-- Consulta 8: Pacientes por hospital
-- ============================================================================
-- Frecuencia:       Listado de pacientes en el módulo clínico

EXPLAIN ANALYZE
SELECT id, full_name, sex, birth_date, postal_code
FROM patients
WHERE hospital_id = '30000000-0000-0000-0000-000000000001'
ORDER BY full_name ASC;

-- EXPECTATIVA SIN ÍNDICE:
--   type: ALL o ref (según FK)

-- EXPECTATIVA CON IDX (idx_patients_hospital_name):
--   type: ref
--   Extra: Using index condition (covering)
