# Module 4 Database Benchmark Runner

This folder contains the automated evidence runner for the advanced database
module. It compares a disposable baseline schema against an optimized schema and
generates CSV, JSON, charts, EXPLAIN evidence, and a formatted PDF report.

## Install

```powershell
python -m venv .venv-module4
.\.venv-module4\Scripts\Activate.ps1
python -m pip install -r scripts\mysql\benchmark\requirements.txt
```

If a virtual environment is not desired, install the same requirements with
`python -m pip install --user -r scripts\mysql\benchmark\requirements.txt`.

## Run

By default, the runner reads MySQL settings from the backend `.env`:

```env
QUARKUS_DATASOURCE_USERNAME=root
QUARKUS_DATASOURCE_PASSWORD=your-mysql-password
QUARKUS_DATASOURCE_JDBC_URL=jdbc:mysql://localhost:3306/statusscope
```

You can also override only the benchmark parameters in `.env`:

```env
MODULE4_BENCHMARK_PRESET=safe
MODULE4_BENCHMARK_CONCURRENCY=1,3
MODULE4_BENCHMARK_ITERATIONS_PER_USER=2
MODULE4_BENCHMARK_DATA_SCALE=500
```

Check the resolved configuration without touching MySQL:

```powershell
python scripts\mysql\benchmark\run_module4_benchmark.py --print-config
```

Run a safe local benchmark:

```powershell
python scripts\mysql\benchmark\run_module4_benchmark.py
```

Run the full rubric workload only when the machine can handle it:

```powershell
python scripts\mysql\benchmark\run_module4_benchmark.py `
  --preset rubric
```

The runner only recreates schemas whose names start with
`statusscope_module4_`:

```text
statusscope_module4_before
statusscope_module4_after
```

It never drops or mutates the `--source-db` schema.

## Output

Each run writes a timestamped folder under the workspace root:

```text
report-captures/module4/<run_id>/
```

Expected artifacts:

```text
module4_database_report.pdf
metrics_summary.csv
metrics_raw.json
explain_plans/
charts/
```

The PDF remains the main deliverable. CSV, JSON, PNG charts, and EXPLAIN files
are supporting evidence for auditability.

## What It Measures

- Before/after latency and throughput for critical backend queries.
- Concurrency levels from the selected preset or `MODULE4_BENCHMARK_CONCURRENCY`.
- EXPLAIN ANALYZE plans for baseline and optimized schemas.
- Stored procedures, functions, triggers, and the event materialization path.
- Compliance matrix for the Module 4 advanced database rubric.

Presets:

| Preset | Concurrency | Iterations/user | Data scale | Use |
| --- | --- | ---: | ---: | --- |
| `safe` | `1,3` | 2 | 500 | Laptop smoke run |
| `standard` | `5,10` | 5 | 2000 | Local evidence pass |
| `rubric` | `10,50,100` | 20 | 10000 | Final Module 4 evidence |

If a benchmark step fails, the runner continues and records the incident in the
PDF and raw JSON so the report is still generated.
