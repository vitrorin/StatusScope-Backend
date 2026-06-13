# Observabilidad — Métricas de desempeño

## Propósito

Establecer métricas de desempeño para garantizar que la base de datos responda
adecuadamente bajo diferentes niveles de carga, tanto para consultas críticas
como para inserciones masivas.

## Instrumentación en Cloud SQL

Cloud SQL for MySQL ofrece dos herramientas integradas que no requieren
instalación de agentes:

1. **Query Insights** (`cloudsql.googleapis.com`): captura automática de
   latencia por consulta, plan de ejecución, frecuencia y tendencias.
2. **Performance Schema** (`performance_schema=ON`): detalle por statement,
   wait events, table IO, index usage.

### Activación recomendada

```sql
-- Performance Schema (vía flags de Cloud SQL)
-- gcloud sql instances patch statusscope-db --database-flags=performance_schema=on

-- Query Insights se activa desde la consola GCP
-- Habilita: "Persist Query Text" y "Track Wait Events"
```

## Métricas objetivo (SLOs)

| Consulta crítica | SLO p95 | Prioridad | Ventana de medición |
|-----------------|---------|-----------|---------------------|
| Autenticación (lookup por external_auth_id + roles) | < 250 ms | P0 | Cada 5 min |
| Dashboard admin (snapshot + recomendaciones + KPIs) | < 800 ms | P0 | Cada 5 min |
| Dashboard doctor (brotes + métricas) | < 600 ms | P0 | Cada 5 min |
| Listado de pacientes por hospital | < 300 ms | P1 | Cada 15 min |
| Búsqueda de enfermedades | < 200 ms | P1 | Cada 15 min |
| Inserción batch de brotes (1000 filas) | < 5 s | P2 | Por ejecución |
| Inserción batch de snapshots (1000 filas) | < 3 s | P2 | Por ejecución |

## Pruebas de carga por concurrencia

### Metodología

1. **Preparación**: Poblar base con datos sintéticos volumétricos
   (10 hospitales, 30 departamentos, 60 inventarios, 10 000 outbreaks,
   5 000 recomendaciones, 1 000 snapshots)
2. **Ejecución**: 100 iteraciones por consulta. Usar los procedimientos SQL
   para medición secuencial y un cliente externo para concurrencia real
   (1/10/25/50/100 usuarios virtuales)
3. **Medición**: Latencia p50/p95/p99, rows examined, tiempos de CPU y IO
4. **Reporte**: Tabla comparativa antes/después de optimización

### Procedimientos almacenados para benchmark

Ver: `scripts/mysql/benchmark/002_load_test_procedures.sql`

Tres procedimientos secuenciales:
- `sp_bench_recommendations_feed(iterations)` — mide feed de recomendaciones
- `sp_bench_outbreaks_by_state(iterations)` — mide brotes por estado
- `sp_bench_latest_snapshot(iterations)` — mide último snapshot

Estos procedimientos no simulan concurrencia real; para 1/10/25/50/100 usuarios
virtuales se debe usar un cliente externo con múltiples conexiones paralelas.

### Concurrencia simulada (script externo sugerido)

Para simular N usuarios concurrentes se recomienda un script Python/k6:

```python
# pseudocódigo — ejecutar N conexiones simultáneas
import threading, mysql.connector, time

def worker(query, results, idx):
    conn = mysql.connector.connect(**config)
    start = time.perf_counter()
    cursor = conn.cursor()
    cursor.execute(query)
    cursor.fetchall()
    elapsed = time.perf_counter() - start
    results[idx] = elapsed
    conn.close()

# N=25 usuarios, cada uno ejecuta la misma consulta
threads = []
results = [0] * 25
for i in range(25):
    t = threading.Thread(target=worker, args=(QUERY, results, i))
    threads.append(t)
for t in threads: t.start()
for t in threads: t.join()
```

## Resultados de pruebas de carga

### Tabla de resultados (plantilla para datos sintéticos)

| Nivel concurrencia | Consulta | p50 (ms) | p95 (ms) | p99 (ms) | Error rate |
|-------------------|----------|---------|---------|---------|------------|
| 1 | Brotes por estado | Pendiente | Pendiente | Pendiente | Pendiente |
| 10 | Brotes por estado | Pendiente | Pendiente | Pendiente | Pendiente |
| 25 | Brotes por estado | Pendiente | Pendiente | Pendiente | Pendiente |
| 50 | Brotes por estado | Pendiente | Pendiente | Pendiente | Pendiente |
| 100 | Brotes por estado | Pendiente | Pendiente | Pendiente | Pendiente |
| 1 | Feed recomendaciones | Pendiente | Pendiente | Pendiente | Pendiente |
| 10 | Feed recomendaciones | Pendiente | Pendiente | Pendiente | Pendiente |
| 25 | Feed recomendaciones | Pendiente | Pendiente | Pendiente | Pendiente |
| 50 | Feed recomendaciones | Pendiente | Pendiente | Pendiente | Pendiente |
| 100 | Feed recomendaciones | Pendiente | Pendiente | Pendiente | Pendiente |
| 1 | Último snapshot | Pendiente | Pendiente | Pendiente | Pendiente |
| 10 | Último snapshot | Pendiente | Pendiente | Pendiente | Pendiente |
| 25 | Último snapshot | Pendiente | Pendiente | Pendiente | Pendiente |
| 50 | Último snapshot | Pendiente | Pendiente | Pendiente | Pendiente |
| 100 | Último snapshot | Pendiente | Pendiente | Pendiente | Pendiente |

### Gráficas de desempeño

> **Nota:** Generar gráficas a partir de los datos de la tabla anterior usando
> Google Sheets, Excel o cualquier herramienta de visualización.
>
> Ejes recomendados:
> - X: Nivel de concurrencia (log scale: 1, 10, 25, 50, 100)
> - Y: Latencia en ms (log scale)
> - Series: p50 y p95 por consulta
> - Dos series por gráfica: "Antes de índices" y "Después de índices"

### Interpretación esperada

1. **Rango lineal hasta 25 usuarios**: El performance se mantiene dentro de SLO.
2. **Degradación progresiva de 50 a 100**: Esperada por contención de conexiones
   y bloqueos de página en InnoDB.
3. **Cuello de botella típico**: En 100 usuarios concurrentes, el cuello de
   botella suele estar en el pool de conexiones (por defecto Cloud SQL tiene
   `max_connections=250` para 4 vCPU).
4. **Recomendación**: Si la aplicación necesita servir > 50 conexiones DB
   simultáneas, escalar Cloud SQL a más vCPU o implementar conexión pool
   con agrupamiento (PgBouncer-style, aunque MySQL ya tiene threading pool).

## Dashboard de monitoreo continuo (GCP)

Después de la entrega, se recomienda configurar:

| Recurso GCP | Métrica | Alerta |
|------------|---------|--------|
| Cloud SQL Insights | Latencia p95 por consulta | > 1 s por más de 5 min |
| Cloud Monitoring | CPU de la instancia | > 80% por más de 10 min |
| Cloud Monitoring | Conexiones activas | > 200 |
| Cloud SQL | Disk utilization | > 85% |
