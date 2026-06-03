# Justificación de Stored Procedures, Funciones y Triggers

## Stored Procedures

### sp_create_supply_request_with_movement

**Problema:** La creación de una solicitud de suministro requiere, como efecto secundario, registrar un movimiento de inventario (reabastecimiento). Si se ejecutan como dos operaciones separadas desde la aplicación, un fallo de red o error en la segunda operación deja datos inconsistentes: una solicitud sin movimiento asociado.

**Decisión:** Se implementa como Stored Procedure para garantizar atomicidad transaccional dentro de MySQL. El SP envuelve ambas inserciones en una transacción con manejo de errores (`DECLARE EXIT HANDLER FOR SQLEXCEPTION`), haciendo rollback automático si alguna operación falla.

**Ventaja sobre la aplicación:** La aplicación necesitaría manejar transacciones manualmente con compensación (saga pattern) para garantizar consistencia, lo cual añade complejidad y riesgo de inconsistencia si la conexión se pierde entre operaciones.

**Lógica delegada a la BD:** Transacción atómica de dos inserciones con FK relacionada. El estado de negocio (status REQUESTED, movement_type REPLENISHMENT) se establece por defecto en el SP.

**Lógica que sigue en la aplicación:** La decisión de crear una solicitud (cuándo y por qué) se toma en la aplicación. El SP solo se encarga de la persistencia atómica.

### sp_generate_hospital_operational_summary

**Problema:** El dashboard admin necesita datos consolidados de snapshots de recursos, conteo de inventario crítico/bajo y recomendaciones activas, todo en una sola carga. Sin este SP, el frontend emite 3-4 queries HTTP y el backend ejecuta 3-4 queries SQL, con latencia acumulativa y posible inconsistencia por lecturas no atómicas.

**Decisión:** Se implementa como SP que reúne todos los datos en una tabla temporal de sesión y los retorna como un solo result set. Usa la función `fn_bed_occupancy_pct` para calcular ocupación.

**Ventaja sobre la aplicación:** Reduce latencia de red (una sola llamada vs. 3-4), garantiza consistencia de lectura (misma transacción) y simplifica el endpoint del controlador.

**Lógica delegada a la BD:** Agregación de datos de múltiples tablas (snapshots, inventario, recomendaciones) en un solo result set.

**Lógica que sigue en la aplicación:** La presentación y formateo del resumen se manejan en el frontend/backend.

---

## Funciones Almacenadas

### fn_bed_occupancy_pct

**Problema:** El porcentaje de ocupación de camas se calcula frecuentemente en dashboards, reportes y reglas de negocio (detección de ICU surge). Sin esta función, cada consulta que necesita el porcentaje replica la fórmula `(total - available) * 100 / total`, lo cual es propenso a errores de cálculo y inconsistencia si la lógica cambia.

**Decisión:** Se implementa como función almacenada `DETERMINISTIC READS SQL DATA` para:
1. Centralizar la lógica de cálculo en un solo lugar.
2. Permitir uso en queries, SPs, triggers y vistas sin duplicar código.
3. Garantizar el mismo resultado siempre con los mismos datos de entrada.

**Valor retornado:** `DECIMAL(5,2)` — valor entre 0.00 y 100.00, o NULL si no hay snapshot.

**Uso:** `SELECT fn_bed_occupancy_pct('30000000-...')` o dentro de `sp_generate_hospital_operational_summary`.

**Lógica que sigue en la aplicación:** La presentación del porcentaje (formateo, color de semáforo) se maneja en el frontend.

### fn_inventory_status

**Problema:** El estado de un item de inventario (CRITICAL, LOW, ADEQUATE) se determina comparando `current_quantity` con `critical_threshold`. Esta lógica se replica en:
- El trigger `trg_validate_inventory_before_update` al actualizar cantidades.
- El seed Faker al crear items.
- Potenciales reports y queries.

Sin esta función, cualquier cambio en la regla (por ejemplo, agregar un nivel WARNING) requeriría modificar múltiples lugares.

**Decisión:** Se implementa como función almacenada para centralizar la regla de negocio y permitir reutilización consistente.

**Valor retornado:** `VARCHAR(32)` — CRITICAL, LOW o ADEQUATE.

**Relación con trigger:** El trigger `trg_validate_inventory_before_update` implementa la misma lógica inline por restricciones de MySQL (no se pueden llamar funciones desde triggers BEFORE en algunos escenarios), pero la función sirve como referencia documentada de la regla y se usa en queries y SPs.

---

## Triggers

### trg_validate_inventory_before_insert (BEFORE INSERT on hospital_inventory_movements)

**Problema:** Un movimiento de inventario con `quantity_delta` negativo (consumo o ajuste negativo) podría reducir la cantidad disponible por debajo de cero, creando datos inconsistentes. La aplicación puede validar esto, pero si se inserta directamente en la BD (ETL, migración, script de prueba), la validación se salta.

**Decisión:** Se implementa como trigger BEFORE INSERT para garantizar que ningún movimiento de inventario deje cantidad negativa, independientemente de la fuente de la inserción.

**Lógica:** Si `quantity_delta < 0`, consulta `current_quantity` del item y verifica que `current_quantity + quantity_delta >= 0`. Si no, lanza `SIGNAL SQLSTATE '45000'` para rechazar la inserción.

**Ventaja:** Protege integridad de datos a nivel de BD. La aplicación no puede evadir esta restricción.

### trg_validate_inventory_before_update (BEFORE UPDATE on hospital_inventory_items)

**Problema:** El campo `status` de `hospital_inventory_items` debe reflejar consistentemente la relación entre `current_quantity` y `critical_threshold`. Si la aplicación o un script actualiza `current_quantity` sin recalcular `status`, el estado queda desincronizado con la realidad.

**Decisión:** Se implementa como trigger BEFORE UPDATE para recalcular automáticamente `status` cada vez que se actualiza una fila de inventario.

**Lógica:** `CRITICAL` si `current_quantity <= critical_threshold`, `LOW` si `current_quantity <= critical_threshold * 2`, `ADEQUATE` en otro caso.

**Ventaja:** Garantiza consistencia automática sin intervención de la aplicación. Incluso actualizaciones directas en BD reflejarán el estado correcto.

### trg_audit_recommendation_change (AFTER UPDATE on operational_recommendations)

**Problema:** Los cambios de estado en recomendaciones operativas deben ser auditados para trazabilidad (regulatoria y operativa). La aplicación puede registrar auditoría, pero si se actualiza la BD directamente, el cambio no se registra.

**Decisión:** Se implementa como trigger AFTER UPDATE para insertar automáticamente un registro en `operational_recommendation_audit` cada vez que cambia el `status` de una recomendación.

**Lógica:** Si `OLD.status != NEW.status`, inserta un registro con `event_type = 'STATUS_CHANGE'`, `event_label = NEW.status` y `event_payload_json` con el estado anterior, nuevo, severidad y timestamp.

**Ventaja:** Trazabilidad completa sin depender de la aplicación. Los registros de auditoría se generan automáticamente para cualquier fuente de cambio.

---

## Resumen: Lógica en la aplicación vs. en la base de datos

| Componente | Ubicación | Justificación |
|------------|-----------|---------------|
| Autenticación y autorización | Aplicación | Firebase Auth gestiona tokens; la app valida y mapea roles |
| Generación de recomendaciones IA | Aplicación | Requiere llamadas HTTP a OpenAI/Gemini |
| Ingesta de brotes CSV | Aplicación | Parseo de archivos, validación de formato, deduplicación |
| Transacción solicitud + movimiento | BD (SP) | Atomicidad garantizada; la app no gestiona rollback parcial |
| Resumen operativo consolidado | BD (SP) | Reduce latencia de red y garantiza consistencia de lectura |
| Cálculo de ocupación | BD (función) | Centraliza lógica reutilizada; evita duplicación |
| Determinación de estado de inventario | BD (función + trigger) | Consistencia automática; la BD recalcula sin intervención |
| Validación de inventario negativo | BD (trigger) | Restricción de integridad que la app no puede evadir |
| Auditoría de cambios de estado | BD (trigger) | Trazabilidad automática para cualquier fuente de cambio |
| Depuración de registros antiguos | BD (Event Scheduler) | Operación 100% interna; no depende de la app |
| Materialización de KPIs | BD (Event Scheduler) | Agregaciones sobre tablas internas |
| Rotación de snapshots | BD (Event Scheduler) | Limpieza interna de tabla temporal |