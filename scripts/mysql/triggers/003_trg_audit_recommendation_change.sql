-- ============================================================================
-- Trigger: trg_audit_recommendation_change
-- ============================================================================
-- Tipo:              AFTER UPDATE
-- Tabla afectada:    operational_recommendations
-- Proposito:         Registrar automaticamente en operational_recommendation_audit
--                    cada cambio de estado (status) de una recomendacion operativa.
--                    Esto garantiza trazabilidad completa sin depender de la app.
-- Logica de negocio:
--   Si OLD.status != NEW.status, insertar un registro en
--   operational_recommendation_audit con:
--     - event_type = 'STATUS_CHANGE'
--     - event_label = NEW.status
--     - event_payload_json con status anterior y nuevo
-- Casos de prueba:
--   1. UPDATE status de NEW a ACCEPTED -> inserta audit con STATUS_CHANGE
--   2. UPDATE severity sin cambiar status -> no inserta audit
--   3. UPDATE status de ACCEPTED a COMPLETED -> inserta audit con transition
-- ============================================================================

DROP TRIGGER IF EXISTS trg_audit_recommendation_change;

DELIMITER //

CREATE TRIGGER trg_audit_recommendation_change
AFTER UPDATE ON operational_recommendations
FOR EACH ROW
BEGIN
    IF OLD.status != NEW.status THEN
        INSERT INTO operational_recommendation_audit (
            id, recommendation_id, actor_user_id, event_type, event_label,
            event_payload_json, created_at
        ) VALUES (
            UUID(),
            NEW.id,
            NEW.assigned_owner_user_id,
            'STATUS_CHANGE',
            NEW.status,
            JSON_OBJECT(
                'from_status', OLD.status,
                'to_status', NEW.status,
                'severity', NEW.severity,
                'changed_at', NOW()
            ),
            NOW()
        );
    END IF;
END //

DELIMITER ;