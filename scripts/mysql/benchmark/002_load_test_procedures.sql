-- ============================================================================
-- Benchmark: Prueba de carga secuencial
-- ============================================================================
-- Proposito:        Medir latencia de consultas criticas bajo carga repetida
--                   en una sesion. La concurrencia real debe medirse con
--                   clientes paralelos externos (k6/Python/JMeter).
-- Uso:              Ejecutar en MySQL 8.0+ con datos representativos.
-- ============================================================================

DROP TEMPORARY TABLE IF EXISTS benchmark_results;
CREATE TEMPORARY TABLE benchmark_results (
    query_label   VARCHAR(64),
    run_id        INT,
    started_at    DATETIME(6),
    ended_at      DATETIME(6),
    duration_us   BIGINT
) ENGINE=Memory;

DELIMITER //

DROP PROCEDURE IF EXISTS sp_bench_recommendations_feed //
DROP PROCEDURE IF EXISTS sp_bench_outbreaks_by_state //
DROP PROCEDURE IF EXISTS sp_bench_latest_snapshot //

CREATE PROCEDURE sp_bench_recommendations_feed(IN iterations INT)
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE start_ts DATETIME(6);
    DECLARE end_ts DATETIME(6);
    DECLARE dummy INT;

    WHILE i <= iterations DO
        SET start_ts = NOW(6);
        SELECT COUNT(*) INTO dummy
        FROM (
            SELECT id, type, severity, status, title, confidence_score, created_at
            FROM operational_recommendations
            WHERE hospital_id = '30000000-0000-0000-0000-000000000001'
              AND status NOT IN ('COMPLETED', 'REJECTED')
            ORDER BY created_at DESC
            LIMIT 5
        ) q;
        SET end_ts = NOW(6);
        INSERT INTO benchmark_results
        VALUES ('recommendations_feed', i, start_ts, end_ts,
                TIMESTAMPDIFF(MICROSECOND, start_ts, end_ts));
        SET i = i + 1;
    END WHILE;
END //

CREATE PROCEDURE sp_bench_outbreaks_by_state(IN iterations INT)
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE start_ts DATETIME(6);
    DECLARE end_ts DATETIME(6);
    DECLARE dummy INT;

    WHILE i <= iterations DO
        SET start_ts = NOW(6);
        SELECT COUNT(*) INTO dummy
        FROM (
            SELECT o.id, o.case_count, o.confirmation_status, o.started_at
            FROM outbreaks o
            JOIN municipalities m ON m.id = o.municipality_id
            WHERE o.status = 'ACTIVE'
              AND o.scope = 'MUNICIPALITY'
              AND m.state_id = '40000000-0000-0000-0000-000000000019'
            ORDER BY o.case_count DESC
        ) q;
        SET end_ts = NOW(6);
        INSERT INTO benchmark_results
        VALUES ('outbreaks_by_state', i, start_ts, end_ts,
                TIMESTAMPDIFF(MICROSECOND, start_ts, end_ts));
        SET i = i + 1;
    END WHILE;
END //

CREATE PROCEDURE sp_bench_latest_snapshot(IN iterations INT)
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE start_ts DATETIME(6);
    DECLARE end_ts DATETIME(6);
    DECLARE dummy INT;

    WHILE i <= iterations DO
        SET start_ts = NOW(6);
        SELECT COUNT(*) INTO dummy
        FROM (
            SELECT id, captured_at, total_beds, available_beds, icu_total_beds,
                   icu_available_beds, doctors_on_shift, nurses_on_shift
            FROM hospital_resource_snapshots
            WHERE hospital_id = '30000000-0000-0000-0000-000000000001'
            ORDER BY captured_at DESC
            LIMIT 1
        ) q;
        SET end_ts = NOW(6);
        INSERT INTO benchmark_results
        VALUES ('latest_snapshot', i, start_ts, end_ts,
                TIMESTAMPDIFF(MICROSECOND, start_ts, end_ts));
        SET i = i + 1;
    END WHILE;
END //

DELIMITER ;

-- CALL sp_bench_recommendations_feed(100);
-- CALL sp_bench_outbreaks_by_state(100);
-- CALL sp_bench_latest_snapshot(100);

-- Reporte MySQL 8 (sin PERCENTILE_CONT)
SELECT
    query_label,
    COUNT(*) AS samples,
    ROUND(AVG(duration_us) / 1000, 2) AS avg_ms,
    ROUND(MIN(duration_us) / 1000, 2) AS min_ms,
    ROUND(MAX(duration_us) / 1000, 2) AS max_ms
FROM benchmark_results
GROUP BY query_label
ORDER BY query_label;

-- DROP PROCEDURE IF EXISTS sp_bench_recommendations_feed;
-- DROP PROCEDURE IF EXISTS sp_bench_outbreaks_by_state;
-- DROP PROCEDURE IF EXISTS sp_bench_latest_snapshot;
