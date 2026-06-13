-- ============================================================================
-- Event Scheduler: ev_snapshot_daily_kpis
-- ============================================================================
-- Objetivo:         Materializar diariamente KPIs agregados de brotes
--                   epidemiológicos y recomendaciones operativas para acelerar
--                   la carga de los dashboards (Doctor y Admin).
-- Proceso:          Calcula totales por estado/municipio y los almacena en
--                   una tabla ligera de resumen.
-- Frecuencia:       Diario a las 05:00 (America/Mexico_City)
-- Tablas origen:    outbreaks, diseases, states, municipalities
-- Tabla destino:    outbreak_daily_kpis (se crea si no existe)
-- Dependencias:     MySQL Event Scheduler habilitado.
-- Impacto esperado: Las consultas de dashboard pasan de recalcular agregados
--                   sobre outbreaks a leer snapshots diarios por ubicación.
-- Rollback:         DROP TABLE outbreak_daily_kpis; los datos se regeneran
--                   en la siguiente ejecución.
-- ============================================================================

CREATE TABLE IF NOT EXISTS outbreak_daily_kpis (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    snapshot_date   DATE         NOT NULL,
    scope           VARCHAR(16)  NOT NULL,
    state_id        VARCHAR(36)  NULL,
    state_name      VARCHAR(64)  NULL,
    municipality_id VARCHAR(36)  NULL,
    municipality_name VARCHAR(128) NULL,
    total_cases     INT          NOT NULL DEFAULT 0,
    active_outbreaks INT         NOT NULL DEFAULT 0,
    suspected       INT          NOT NULL DEFAULT 0,
    confirmed       INT          NOT NULL DEFAULT 0,
    top_disease     VARCHAR(128) NULL,
    calculated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_kpi_snapshot_scope_location (snapshot_date, scope, state_id, municipality_id),
    INDEX idx_kpi_scope_state (scope, state_id),
    INDEX idx_kpi_scope_municipality (scope, municipality_id),
    INDEX idx_kpi_calculated (calculated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP EVENT IF EXISTS ev_snapshot_daily_kpis;

DELIMITER //

CREATE EVENT ev_snapshot_daily_kpis
    ON SCHEDULE
        EVERY 1 DAY
        STARTS TIMESTAMP(CURRENT_DATE, '05:00:00')
    COMMENT 'Materializa KPIs diarios de brotes para dashboards'
    DO
BEGIN
    -- Recalcular solo el snapshot del día actual.
    DELETE FROM outbreak_daily_kpis WHERE snapshot_date = CURRENT_DATE;

    -- KPIs por estado (scope = STATE)
    INSERT INTO outbreak_daily_kpis (
        snapshot_date, scope, state_id, state_name,
        total_cases, active_outbreaks, suspected, confirmed, top_disease
    )
    SELECT
        CURRENT_DATE,
        'STATE',
        s.id,
        s.name,
        COALESCE(SUM(o.case_count), 0),
        COUNT(DISTINCT o.id),
        COALESCE(SUM(CASE WHEN o.confirmation_status = 'SUSPECTED' THEN o.case_count ELSE 0 END), 0),
        COALESCE(SUM(CASE WHEN o.confirmation_status = 'CONFIRMED' THEN o.case_count ELSE 0 END), 0),
        (SELECT d2.name
          FROM outbreaks o2
          JOIN diseases d2 ON d2.id = o2.disease_id
          WHERE o2.status = 'ACTIVE'
            AND o2.scope = 'STATE'
            AND o2.state_id = s.id
          GROUP BY d2.id, d2.name
          ORDER BY SUM(o2.case_count) DESC
          LIMIT 1)
    FROM states s
    LEFT JOIN outbreaks o ON o.state_id = s.id AND o.status = 'ACTIVE' AND o.scope = 'STATE'
    GROUP BY s.id, s.name;

    -- KPIs por municipio (scope = MUNICIPALITY) solo para municipios con brotes activos.
    INSERT INTO outbreak_daily_kpis (
        snapshot_date, scope, state_id, state_name, municipality_id, municipality_name,
        total_cases, active_outbreaks, suspected, confirmed, top_disease
    )
    SELECT
        CURRENT_DATE,
        'MUNICIPALITY',
        st.id,
        st.name,
        m.id,
        m.name,
        COALESCE(SUM(o.case_count), 0),
        COUNT(DISTINCT o.id),
        COALESCE(SUM(CASE WHEN o.confirmation_status = 'SUSPECTED' THEN o.case_count ELSE 0 END), 0),
        COALESCE(SUM(CASE WHEN o.confirmation_status = 'CONFIRMED' THEN o.case_count ELSE 0 END), 0),
        (SELECT d2.name
          FROM outbreaks o2
          JOIN diseases d2 ON d2.id = o2.disease_id
          WHERE o2.status = 'ACTIVE'
            AND o2.scope = 'MUNICIPALITY'
            AND o2.municipality_id = m.id
          GROUP BY d2.id, d2.name
          ORDER BY SUM(o2.case_count) DESC
          LIMIT 1)
    FROM municipalities m
    JOIN states st ON st.id = m.state_id
    JOIN outbreaks o ON o.municipality_id = m.id AND o.status = 'ACTIVE' AND o.scope = 'MUNICIPALITY'
    GROUP BY st.id, st.name, m.id, m.name;
END //

DELIMITER ;
