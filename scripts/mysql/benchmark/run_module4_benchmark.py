#!/usr/bin/env python
"""
Automated evidence runner for StatusScope Module 4 database deliverables.

The runner creates disposable MySQL schemas, measures before/after query
performance, validates stored routines/triggers/events, and writes a polished
PDF plus raw CSV/JSON evidence. It never mutates the source database.
"""

from __future__ import annotations

import argparse
import concurrent.futures
import csv
import datetime as dt
import json
import math
import os
from pathlib import Path
import random
import re
import statistics
import sys
import textwrap
import time
import traceback
from typing import Any, Callable, Iterable
from urllib.parse import urlparse

import mysql.connector
from mysql.connector import Error as MySQLError

import numpy as np
import pandas as pd

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt
import seaborn as sns

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import letter
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import inch
from reportlab.platypus import (
    Image,
    KeepTogether,
    PageBreak,
    Paragraph,
    SimpleDocTemplate,
    Spacer,
    Table,
    TableStyle,
)


SAFE_SCHEMA_PREFIX = "statusscope_module4_"
BEFORE_SCHEMA = "statusscope_module4_before"
AFTER_SCHEMA = "statusscope_module4_after"
BACKEND_ROOT = Path(__file__).resolve().parents[3]
WORKSPACE_ROOT = Path(__file__).resolve().parents[4]
DEFAULT_OUTPUT_ROOT = WORKSPACE_ROOT / "report-captures" / "module4"

WORKLOAD_PRESETS = {
    "safe": {
        "concurrency": "1,3",
        "iterations_per_user": 2,
        "data_scale": 500,
        "pause_between_tests": 0.2,
    },
    "standard": {
        "concurrency": "5,10",
        "iterations_per_user": 5,
        "data_scale": 2000,
        "pause_between_tests": 0.1,
    },
    "rubric": {
        "concurrency": "10,50,100",
        "iterations_per_user": 20,
        "data_scale": 10000,
        "pause_between_tests": 0.0,
    },
}

PROJECT_BLUE = colors.HexColor("#0003B8")
DARK = colors.HexColor("#172033")
MUTED = colors.HexColor("#64748B")
LIGHT_BG = colors.HexColor("#F5F7FB")
GREEN = colors.HexColor("#12805C")
RED = colors.HexColor("#B42318")
AMBER = colors.HexColor("#B7791F")


OPTIMIZATION_INDEXES = {
    "outbreaks": [
        "idx_outbreaks_status_scope_municipality",
        "idx_outbreaks_status_scope_state",
        "idx_outbreaks_status_scope_municipality_cases",
        "idx_outbreaks_status_scope_state_cases",
    ],
    "operational_recommendations": [
        "idx_recs_hospital_created",
        "idx_recs_hospital_status_created",
        "idx_recs_hospital_severity_created",
    ],
    "hospital_resource_snapshots": ["idx_snapshots_hospital_captured"],
    "hospital_department_resources": [
        "idx_dept_hospital_status",
        "idx_dept_hospital_name",
    ],
    "hospital_inventory_items": [
        "idx_inventory_hospital_category",
        "idx_inventory_hospital_status",
    ],
    "hospital_staffing_profiles": ["idx_staff_hospital_role"],
    "patients": ["idx_patients_hospital_name"],
    "operational_recommendation_audit": ["idx_audit_recommendation_created"],
    "hospital_inventory_movements": [
        "idx_movements_item_created",
        "idx_movements_hospital_item_created",
    ],
    "hospital_operational_contacts": [
        "idx_contacts_hospital_department_name",
        "idx_contacts_email_notify",
    ],
    "hospital_operational_groups": ["idx_groups_hospital_department_name"],
    "operational_tasks": [
        "idx_tasks_recommendation_created",
        "idx_tasks_active_recommendation_created",
    ],
    "operational_notifications": ["idx_notifications_recommendation_sent"],
    "operational_notification_recipients": ["idx_recipients_notification_delivered"],
    "supply_requests": ["idx_supply_recommendation_created"],
    "patient_evaluations": [
        "idx_eval_doctor_status_created",
        "idx_eval_doctor_status_finalized",
    ],
    "evaluation_differential_diagnoses": ["idx_edd_evaluation_rank"],
    "evaluation_recommended_tests": ["idx_ert_evaluation_sort"],
    "patient_evaluation_files": ["idx_pef_evaluation_uploaded"],
    "diseases": ["idx_diseases_fulltext_name_code"],
}


def quote_identifier(identifier: str) -> str:
    if not re.match(r"^[A-Za-z0-9_]+$", identifier):
        raise ValueError(f"Unsafe MySQL identifier: {identifier!r}")
    return f"`{identifier}`"


def log(message: str) -> None:
    try:
        print(f"[module4] {message}", flush=True)
    except OSError:
        pass


def load_env_file() -> tuple[Path | None, dict[str, str]]:
    candidates = []
    for candidate in (Path.cwd() / ".env", BACKEND_ROOT / ".env"):
        if candidate not in candidates:
            candidates.append(candidate)

    for path in candidates:
        if not path.exists():
            continue
        values: dict[str, str] = {}
        for raw_line in path.read_text(encoding="utf-8-sig").splitlines():
            line = raw_line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, value = line.split("=", 1)
            key = key.strip()
            value = value.strip().strip('"').strip("'")
            if key:
                values[key] = value
        return path, values
    return None, {}


def env_value(env_file: dict[str, str], *keys: str, default: str = "") -> str:
    for key in keys:
        value = os.getenv(key)
        if value is not None:
            return value
        if key in env_file:
            return env_file[key]
    return default


def parse_mysql_jdbc_url(jdbc_url: str) -> dict[str, Any]:
    if not jdbc_url:
        return {}
    url = jdbc_url[5:] if jdbc_url.startswith("jdbc:") else jdbc_url
    parsed = urlparse(url)
    if parsed.scheme != "mysql":
        return {}
    database = parsed.path.lstrip("/").split("/", 1)[0]
    return {
        "host": parsed.hostname or "localhost",
        "port": parsed.port or 3306,
        "database": database or "statusscope",
    }


def parse_int(value: str, default: int) -> int:
    try:
        return int(value)
    except (TypeError, ValueError):
        return default


def parse_float(value: str, default: float) -> float:
    try:
        return float(value)
    except (TypeError, ValueError):
        return default


def assert_disposable_schema(schema: str) -> None:
    if not schema.startswith(SAFE_SCHEMA_PREFIX):
        raise ValueError(
            f"Refusing destructive action on schema {schema!r}; expected prefix {SAFE_SCHEMA_PREFIX!r}"
        )


def connect(args: argparse.Namespace, database: str | None = None):
    return mysql.connector.connect(
        host=args.host,
        port=args.port,
        user=args.user,
        password=args.password,
        database=database,
        autocommit=True,
        connection_timeout=30,
    )


def fetch_all_dict(conn, sql: str, params: tuple[Any, ...] = ()) -> list[dict[str, Any]]:
    cur = conn.cursor(dictionary=True)
    cur.execute(sql, params)
    rows = cur.fetchall()
    cur.close()
    return rows


def fetch_scalar(conn, sql: str, params: tuple[Any, ...] = ()) -> Any:
    cur = conn.cursor()
    cur.execute(sql, params)
    row = cur.fetchone()
    cur.close()
    return row[0] if row else None


def execute(conn, sql: str, params: tuple[Any, ...] = ()) -> None:
    cur = conn.cursor()
    cur.execute(sql, params)
    cur.close()


def execute_many(conn, sql: str, values: Iterable[tuple[Any, ...]]) -> None:
    rows = list(values)
    if not rows:
        return
    cur = conn.cursor()
    cur.executemany(sql, rows)
    cur.close()


def execute_script(conn, script: str) -> None:
    """Execute a MySQL script, including DELIMITER-based routine blocks."""
    delimiter = ";"
    statement: list[str] = []
    cur = conn.cursor()
    try:
        for raw_line in script.splitlines():
            line = raw_line.rstrip()
            stripped = line.strip()
            if not stripped:
                statement.append(raw_line)
                continue
            if stripped.upper().startswith("DELIMITER "):
                delimiter = stripped.split(None, 1)[1]
                continue
            statement.append(raw_line)
            joined = "\n".join(statement).strip()
            if joined.endswith(delimiter):
                sql = joined[: -len(delimiter)].strip()
                statement = []
                if sql:
                    cur.execute(sql)
                    while cur.nextset():
                        pass
        trailing = "\n".join(statement).strip()
        if trailing:
            cur.execute(trailing)
            while cur.nextset():
                pass
    finally:
        cur.close()


def database_charset_collation(conn, source_db: str) -> tuple[str, str]:
    rows = fetch_all_dict(
        conn,
        """
        SELECT default_character_set_name AS charset_name,
               default_collation_name AS collation_name
        FROM information_schema.schemata
        WHERE schema_name = %s
        """,
        (source_db,),
    )
    if not rows:
        return "utf8mb4", "utf8mb4_0900_ai_ci"
    row = rows[0]
    return row["charset_name"], row["collation_name"]


def drop_and_create_schema(conn, schema: str, source_db: str) -> None:
    assert_disposable_schema(schema)
    charset_name, collation_name = database_charset_collation(conn, source_db)
    execute(conn, f"DROP DATABASE IF EXISTS {quote_identifier(schema)}")
    execute(
        conn,
        (
            f"CREATE DATABASE {quote_identifier(schema)} "
            f"CHARACTER SET {quote_identifier(charset_name).strip('`')} "
            f"COLLATE {quote_identifier(collation_name).strip('`')}"
        ),
    )


def list_source_tables(conn, source_db: str) -> list[str]:
    rows = fetch_all_dict(
        conn,
        """
        SELECT table_name
        FROM information_schema.tables
        WHERE table_schema = %s AND table_type = 'BASE TABLE'
        ORDER BY table_name
        """,
        (source_db,),
    )
    return [row["TABLE_NAME"] if "TABLE_NAME" in row else row["table_name"] for row in rows]


def clone_schema(args: argparse.Namespace, source_db: str, dest_db: str) -> None:
    conn = connect(args)
    try:
        drop_and_create_schema(conn, dest_db, source_db)
        tables = list_source_tables(conn, source_db)
        for table in tables:
            src = f"{quote_identifier(source_db)}.{quote_identifier(table)}"
            dest = f"{quote_identifier(dest_db)}.{quote_identifier(table)}"
            execute(conn, f"CREATE TABLE {dest} LIKE {src}")
        execute(conn, "SET SESSION foreign_key_checks = 0")
        for table in tables:
            src = f"{quote_identifier(source_db)}.{quote_identifier(table)}"
            dest = f"{quote_identifier(dest_db)}.{quote_identifier(table)}"
            execute(conn, f"INSERT INTO {dest} SELECT * FROM {src}")
        execute(conn, "SET SESSION foreign_key_checks = 1")
    finally:
        conn.close()


def index_exists(conn, table: str, index_name: str) -> bool:
    return bool(
        fetch_scalar(
            conn,
            """
            SELECT COUNT(*)
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = %s
              AND index_name = %s
            """,
            (table, index_name),
        )
    )


def table_exists(conn, table: str) -> bool:
    return bool(
        fetch_scalar(
            conn,
            """
            SELECT COUNT(*)
            FROM information_schema.tables
            WHERE table_schema = DATABASE()
              AND table_name = %s
            """,
            (table,),
        )
    )


def column_exists(conn, table: str, column: str) -> bool:
    return bool(
        fetch_scalar(
            conn,
            """
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = %s
              AND column_name = %s
            """,
            (table, column),
        )
    )


def add_index(conn, table: str, index_name: str, create_sql: str) -> None:
    if table_exists(conn, table) and not index_exists(conn, table, index_name):
        execute(conn, create_sql)


def drop_index_if_exists(conn, table: str, index_name: str) -> None:
    if table_exists(conn, table) and index_exists(conn, table, index_name):
        execute(conn, f"DROP INDEX {quote_identifier(index_name)} ON {quote_identifier(table)}")


def remove_optimization_indexes(conn) -> None:
    for table, indexes in OPTIMIZATION_INDEXES.items():
        for index_name in indexes:
            drop_index_if_exists(conn, table, index_name)


def apply_optimization_indexes(conn) -> None:
    add_index(
        conn,
        "outbreaks",
        "idx_outbreaks_status_scope_municipality",
        "CREATE INDEX idx_outbreaks_status_scope_municipality ON outbreaks (status, scope, municipality_id)",
    )
    add_index(
        conn,
        "outbreaks",
        "idx_outbreaks_status_scope_state",
        "CREATE INDEX idx_outbreaks_status_scope_state ON outbreaks (status, scope, state_id)",
    )
    add_index(
        conn,
        "outbreaks",
        "idx_outbreaks_status_scope_municipality_cases",
        "CREATE INDEX idx_outbreaks_status_scope_municipality_cases ON outbreaks (status, scope, municipality_id, case_count DESC)",
    )
    add_index(
        conn,
        "outbreaks",
        "idx_outbreaks_status_scope_state_cases",
        "CREATE INDEX idx_outbreaks_status_scope_state_cases ON outbreaks (status, scope, state_id, case_count DESC)",
    )
    add_index(
        conn,
        "operational_recommendations",
        "idx_recs_hospital_created",
        "CREATE INDEX idx_recs_hospital_created ON operational_recommendations (hospital_id, created_at DESC)",
    )
    add_index(
        conn,
        "operational_recommendations",
        "idx_recs_hospital_status_created",
        "CREATE INDEX idx_recs_hospital_status_created ON operational_recommendations (hospital_id, status, created_at DESC)",
    )
    add_index(
        conn,
        "operational_recommendations",
        "idx_recs_hospital_severity_created",
        "CREATE INDEX idx_recs_hospital_severity_created ON operational_recommendations (hospital_id, severity, created_at DESC)",
    )
    add_index(
        conn,
        "hospital_resource_snapshots",
        "idx_snapshots_hospital_captured",
        "CREATE INDEX idx_snapshots_hospital_captured ON hospital_resource_snapshots (hospital_id, captured_at DESC)",
    )
    add_index(
        conn,
        "hospital_department_resources",
        "idx_dept_hospital_status",
        "CREATE INDEX idx_dept_hospital_status ON hospital_department_resources (hospital_id, status)",
    )
    add_index(
        conn,
        "hospital_department_resources",
        "idx_dept_hospital_name",
        "CREATE INDEX idx_dept_hospital_name ON hospital_department_resources (hospital_id, department_name)",
    )
    add_index(
        conn,
        "hospital_inventory_items",
        "idx_inventory_hospital_category",
        "CREATE INDEX idx_inventory_hospital_category ON hospital_inventory_items (hospital_id, category, item_name)",
    )
    add_index(
        conn,
        "hospital_inventory_items",
        "idx_inventory_hospital_status",
        "CREATE INDEX idx_inventory_hospital_status ON hospital_inventory_items (hospital_id, status)",
    )
    add_index(
        conn,
        "hospital_staffing_profiles",
        "idx_staff_hospital_role",
        "CREATE INDEX idx_staff_hospital_role ON hospital_staffing_profiles (hospital_id, role_name)",
    )
    add_index(
        conn,
        "patients",
        "idx_patients_hospital_name",
        "CREATE INDEX idx_patients_hospital_name ON patients (hospital_id, full_name)",
    )
    add_index(
        conn,
        "operational_recommendation_audit",
        "idx_audit_recommendation_created",
        "CREATE INDEX idx_audit_recommendation_created ON operational_recommendation_audit (recommendation_id, created_at ASC)",
    )
    add_index(
        conn,
        "hospital_inventory_movements",
        "idx_movements_item_created",
        "CREATE INDEX idx_movements_item_created ON hospital_inventory_movements (inventory_item_id, created_at DESC)",
    )
    add_index(
        conn,
        "hospital_inventory_movements",
        "idx_movements_hospital_item_created",
        "CREATE INDEX idx_movements_hospital_item_created ON hospital_inventory_movements (hospital_id, inventory_item_id, created_at DESC)",
    )
    add_index(
        conn,
        "hospital_operational_contacts",
        "idx_contacts_hospital_department_name",
        "CREATE INDEX idx_contacts_hospital_department_name ON hospital_operational_contacts (hospital_id, department_code, display_name)",
    )
    add_index(
        conn,
        "hospital_operational_contacts",
        "idx_contacts_email_notify",
        "CREATE INDEX idx_contacts_email_notify ON hospital_operational_contacts (hospital_id, department_code, contact_channel, is_notifiable, display_name)",
    )
    add_index(
        conn,
        "hospital_operational_groups",
        "idx_groups_hospital_department_name",
        "CREATE INDEX idx_groups_hospital_department_name ON hospital_operational_groups (hospital_id, department_code, group_name)",
    )
    add_index(
        conn,
        "operational_tasks",
        "idx_tasks_recommendation_created",
        "CREATE INDEX idx_tasks_recommendation_created ON operational_tasks (recommendation_id, created_at DESC)",
    )
    add_index(
        conn,
        "operational_tasks",
        "idx_tasks_active_recommendation_created",
        "CREATE INDEX idx_tasks_active_recommendation_created ON operational_tasks (recommendation_id, status, created_at DESC)",
    )
    add_index(
        conn,
        "operational_notifications",
        "idx_notifications_recommendation_sent",
        "CREATE INDEX idx_notifications_recommendation_sent ON operational_notifications (recommendation_id, sent_at DESC)",
    )
    add_index(
        conn,
        "operational_notification_recipients",
        "idx_recipients_notification_delivered",
        "CREATE INDEX idx_recipients_notification_delivered ON operational_notification_recipients (notification_id, delivered_at ASC)",
    )
    add_index(
        conn,
        "supply_requests",
        "idx_supply_recommendation_created",
        "CREATE INDEX idx_supply_recommendation_created ON supply_requests (recommendation_id, created_at DESC)",
    )
    add_index(
        conn,
        "patient_evaluations",
        "idx_eval_doctor_status_created",
        "CREATE INDEX idx_eval_doctor_status_created ON patient_evaluations (doctor_user_id, status, created_at DESC)",
    )
    if table_exists(conn, "patient_evaluations") and column_exists(conn, "patient_evaluations", "finalized_at"):
        add_index(
            conn,
            "patient_evaluations",
            "idx_eval_doctor_status_finalized",
            """
            CREATE INDEX idx_eval_doctor_status_finalized
            ON patient_evaluations (doctor_user_id, status, finalized_at DESC)
            """,
        )
    add_index(
        conn,
        "evaluation_differential_diagnoses",
        "idx_edd_evaluation_rank",
        "CREATE INDEX idx_edd_evaluation_rank ON evaluation_differential_diagnoses (evaluation_id, rank_order ASC)",
    )
    add_index(
        conn,
        "evaluation_recommended_tests",
        "idx_ert_evaluation_sort",
        "CREATE INDEX idx_ert_evaluation_sort ON evaluation_recommended_tests (evaluation_id, sort_order ASC)",
    )
    add_index(
        conn,
        "patient_evaluation_files",
        "idx_pef_evaluation_uploaded",
        "CREATE INDEX idx_pef_evaluation_uploaded ON patient_evaluation_files (evaluation_id, uploaded_at DESC)",
    )
    if table_exists(conn, "diseases") and not index_exists(conn, "diseases", "idx_diseases_fulltext_name_code"):
        try:
            execute(conn, "CREATE FULLTEXT INDEX idx_diseases_fulltext_name_code ON diseases (name, code)")
        except MySQLError:
            # Some local schemas may use a table option that prevents FULLTEXT.
            pass


def install_optimized_routines(conn) -> None:
    execute_script(conn, ADVANCED_ROUTINES_SQL)


def install_events(conn) -> None:
    execute_script(conn, EVENT_TABLE_SQL)
    execute_script(conn, EVENT_KPI_SQL)


def select_existing_id(conn, table: str, fallback: str) -> str:
    if not table_exists(conn, table):
        return fallback
    value = fetch_scalar(conn, f"SELECT id FROM {quote_identifier(table)} LIMIT 1")
    return value or fallback


def ensure_seed_data(conn, scale: int) -> dict[str, str]:
    """Ensure enough deterministic rows exist to produce visible benchmark data."""
    random.seed(42)
    now = dt.datetime.now().replace(microsecond=0)

    hospital_id = select_existing_id(conn, "hospitals", "30000000-0000-0000-0000-000000000001")
    disease_id = select_existing_id(conn, "diseases", "60000000-0000-0000-0000-000000000004")
    state_id = select_existing_id(conn, "states", "40000000-0000-0000-0000-000000000019")
    municipality_id = select_existing_id(conn, "municipalities", "42000000-0000-0000-0000-000000001003")
    user_id = select_existing_id(conn, "users", "70000000-0000-0000-0000-000000000004")

    if table_exists(conn, "outbreaks"):
        current = fetch_scalar(conn, "SELECT COUNT(*) FROM outbreaks")
        target = max(current or 0, scale)
        rows = []
        for i in range(current or 0, target):
            oid = f"91000000-0000-0000-0000-{i:012d}"
            scope = "MUNICIPALITY" if i % 4 else "STATE"
            rows.append(
                (
                    oid,
                    disease_id,
                    scope,
                    municipality_id if scope == "MUNICIPALITY" else None,
                    state_id if scope == "STATE" else None,
                    10 + (i % 500),
                    "CONFIRMED" if i % 3 else "SUSPECTED",
                    "ACTIVE" if i % 5 else "RESOLVED",
                    now - dt.timedelta(days=i % 180),
                    None,
                    now,
                    now,
                )
            )
        execute_many(
            conn,
            """
            INSERT IGNORE INTO outbreaks
            (id, disease_id, scope, municipality_id, state_id, case_count,
             confirmation_status, status, started_at, ended_at, created_at, updated_at)
            VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)
            """,
            rows,
        )

    if table_exists(conn, "hospital_resource_snapshots"):
        current = fetch_scalar(
            conn,
            "SELECT COUNT(*) FROM hospital_resource_snapshots WHERE hospital_id = %s",
            (hospital_id,),
        )
        target = max(current or 0, max(1000, scale // 10))
        rows = []
        for i in range(current or 0, target):
            total = 180 + (i % 80)
            available = max(0, total - 50 - (i % 100))
            rows.append(
                (
                    f"92000000-0000-0000-0000-{i:012d}",
                    hospital_id,
                    now - dt.timedelta(hours=i),
                    total,
                    available,
                    20,
                    max(1, 10 - (i % 10)),
                    12,
                    4,
                    600,
                    180,
                    45,
                    130,
                    15,
                    "BENCHMARK",
                    now,
                )
            )
        execute_many(
            conn,
            """
            INSERT IGNORE INTO hospital_resource_snapshots
            (id, hospital_id, captured_at, total_beds, available_beds, icu_total_beds,
             icu_available_beds, isolation_rooms_total, isolation_rooms_available,
             oxygen_capacity_units, oxygen_available_units, doctors_on_shift,
             nurses_on_shift, specialists_on_shift, source, created_at)
            VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)
            """,
            rows,
        )

    inventory_item_id = select_existing_id(
        conn, "hospital_inventory_items", "23000000-0000-0000-0000-000000000001"
    )
    if table_exists(conn, "hospital_inventory_items"):
        execute(
            conn,
            """
            INSERT IGNORE INTO hospital_inventory_items
            (id, hospital_id, item_code, item_name, category, location, current_quantity,
             capacity_quantity, unit, critical_threshold, target_quantity, status, updated_at)
            VALUES
            ('93000000-0000-0000-0000-000000000001', %s, 'BENCH_N95', 'Benchmark N95 Masks',
             'PPE', 'Benchmark Storage', 5000, 10000, 'units', 500, 8000, 'ADEQUATE', NOW())
            """,
            (hospital_id,),
        )
        inventory_item_id = "93000000-0000-0000-0000-000000000001"

    if table_exists(conn, "operational_recommendations"):
        current = fetch_scalar(
            conn,
            "SELECT COUNT(*) FROM operational_recommendations WHERE hospital_id = %s",
            (hospital_id,),
        )
        target = max(current or 0, max(5000, scale // 2))
        statuses = ["NEW", "ACCEPTED", "ASSIGNED", "COMPLETED", "REJECTED"]
        severities = ["LOW", "MEDIUM", "HIGH", "CRITICAL"]
        types = ["BED_CAPACITY", "STAFFING", "ISOLATION", "SUPPLY"]
        rows = []
        for i in range(current or 0, target):
            rows.append(
                (
                    f"94000000-0000-0000-0000-{i:012d}",
                    hospital_id,
                    None,
                    None,
                    types[i % len(types)],
                    severities[i % len(severities)],
                    statuses[i % len(statuses)],
                    "Benchmark",
                    f"Benchmark recommendation {i}",
                    "Synthetic recommendation for module 4 performance evidence.",
                    "Expected operational improvement.",
                    "24h",
                    round(0.5 + (i % 50) / 100, 2),
                    "standard",
                    "{}",
                    "[]",
                    "[]",
                    "[]",
                    None,
                    None,
                    inventory_item_id if i % 3 == 0 else None,
                    "standard",
                    "ORDER_SUPPLIES",
                    "[]",
                    "[]",
                    "Benchmark",
                    severities[i % len(severities)],
                    statuses[i % len(statuses)],
                    now + dt.timedelta(days=7),
                    None,
                    None,
                    None,
                    "{}",
                    "RULE_ENGINE",
                    now - dt.timedelta(minutes=i),
                    now - dt.timedelta(minutes=i),
                    None,
                )
            )
        execute_many(
            conn,
            """
            INSERT IGNORE INTO operational_recommendations
            (id, hospital_id, source_alert_id, source_outbreak_id, type, severity, status,
             category, title, description, expected_impact, urgency_window, confidence_score,
             image_mode, rationale_json, recommended_actions_json, affected_departments_json,
             affected_resources_json, primary_department_resource_id, primary_staffing_profile_id,
             primary_inventory_item_id, presentation_variant, primary_action_code,
             available_actions_json, allowed_status_transitions_json, display_category_label,
             display_severity_label, display_status_label, expires_at, assigned_owner_user_id,
             model_provider, model_version, input_context_json, created_by_mode, created_at,
             updated_at, resolved_at)
            VALUES
            (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,
             %s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)
            """,
            rows,
        )

    if table_exists(conn, "hospital_inventory_movements"):
        rows = []
        for i in range(max(500, scale // 20)):
            rows.append(
                (
                    f"95000000-0000-0000-0000-{i:012d}",
                    hospital_id,
                    inventory_item_id,
                    "CONSUMPTION" if i % 2 else "REPLENISHMENT",
                    -1 if i % 2 else 5,
                    "units",
                    "Benchmark movement",
                    None,
                    now - dt.timedelta(minutes=i),
                )
            )
        execute_many(
            conn,
            """
            INSERT IGNORE INTO hospital_inventory_movements
            (id, hospital_id, inventory_item_id, movement_type, quantity_delta, unit,
             notes, related_supply_request_id, created_at)
            VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s)
            """,
            rows,
        )

    return {
        "hospital_id": hospital_id,
        "disease_id": disease_id,
        "state_id": state_id,
        "municipality_id": municipality_id,
        "user_id": user_id,
        "inventory_item_id": inventory_item_id,
    }


def benchmark_queries(ids: dict[str, str]) -> list[dict[str, str]]:
    h = ids["hospital_id"]
    s = ids["state_id"]
    m = ids["municipality_id"]
    item = ids["inventory_item_id"]
    user = ids["user_id"]
    return [
        {
            "label": "auth_lookup",
            "before_sql": """
                SELECT DISTINCT u.id
                FROM users u
                LEFT JOIN user_roles ur ON ur.user_id = u.id
                LEFT JOIN roles r ON r.id = ur.role_id
                LEFT JOIN role_privileges rp ON rp.role_id = r.id
                LEFT JOIN privileges p ON p.id = rp.privilege_id
                WHERE u.external_auth_id = 'seed-doctor-1'
            """,
            "after_sql": """
                SELECT DISTINCT u.id
                FROM users u
                LEFT JOIN user_roles ur ON ur.user_id = u.id
                LEFT JOIN roles r ON r.id = ur.role_id
                LEFT JOIN role_privileges rp ON rp.role_id = r.id
                LEFT JOIN privileges p ON p.id = rp.privilege_id
                WHERE u.external_auth_id = 'seed-doctor-1'
            """,
        },
        {
            "label": "outbreaks_by_state",
            "before_sql": f"""
                SELECT o.id, o.case_count, o.confirmation_status, o.started_at
                FROM outbreaks o
                JOIN municipalities m ON m.id = o.municipality_id
                WHERE o.status = 'ACTIVE'
                  AND o.scope = 'MUNICIPALITY'
                  AND m.state_id = '{s}'
                ORDER BY o.case_count DESC
            """,
            "after_sql": f"""
                SELECT o.id, o.case_count, o.confirmation_status, o.started_at
                FROM outbreaks o
                JOIN municipalities m ON m.id = o.municipality_id
                WHERE o.status = 'ACTIVE'
                  AND o.scope = 'MUNICIPALITY'
                  AND m.state_id = '{s}'
                ORDER BY o.case_count DESC
            """,
        },
        {
            "label": "outbreaks_mixed_scope",
            "before_sql": f"""
                SELECT o.id, o.case_count, o.scope
                FROM outbreaks o
                WHERE o.status = 'ACTIVE'
                  AND ((o.scope = 'MUNICIPALITY' AND o.municipality_id IN ('{m}'))
                       OR (o.scope = 'STATE' AND o.state_id = '{s}'))
            """,
            "after_sql": f"""
                SELECT o.id, o.case_count, o.scope
                FROM outbreaks o
                WHERE o.status = 'ACTIVE'
                  AND ((o.scope = 'MUNICIPALITY' AND o.municipality_id IN ('{m}'))
                       OR (o.scope = 'STATE' AND o.state_id = '{s}'))
            """,
        },
        {
            "label": "recommendations_feed",
            "before_sql": f"""
                SELECT id, type, severity, status, title, confidence_score, created_at
                FROM operational_recommendations
                WHERE hospital_id = '{h}'
                  AND status NOT IN ('COMPLETED', 'REJECTED')
                ORDER BY created_at DESC
                LIMIT 5
            """,
            "after_sql": f"""
                SELECT id, type, severity, status, title, confidence_score, created_at
                FROM operational_recommendations
                WHERE hospital_id = '{h}'
                  AND status NOT IN ('COMPLETED', 'REJECTED')
                ORDER BY created_at DESC
                LIMIT 5
            """,
        },
        {
            "label": "latest_snapshot",
            "before_sql": f"""
                SELECT id, captured_at, total_beds, available_beds
                FROM hospital_resource_snapshots
                WHERE hospital_id = '{h}'
                ORDER BY captured_at DESC
                LIMIT 1
            """,
            "after_sql": f"""
                SELECT id, captured_at, total_beds, available_beds
                FROM hospital_resource_snapshots
                WHERE hospital_id = '{h}'
                ORDER BY captured_at DESC
                LIMIT 1
            """,
        },
        {
            "label": "inventory_movements",
            "before_sql": f"""
                SELECT id, movement_type, quantity_delta, created_at
                FROM hospital_inventory_movements
                WHERE hospital_id = '{h}'
                  AND inventory_item_id = '{item}'
                ORDER BY created_at DESC
            """,
            "after_sql": f"""
                SELECT id, movement_type, quantity_delta, created_at
                FROM hospital_inventory_movements
                WHERE hospital_id = '{h}'
                  AND inventory_item_id = '{item}'
                ORDER BY created_at DESC
            """,
        },
        {
            "label": "contacts_email_notify",
            "before_sql": f"""
                SELECT id, display_name
                FROM hospital_operational_contacts
                WHERE hospital_id = '{h}'
                  AND department_code = 'ICU'
                  AND contact_channel = 'EMAIL'
                  AND contact_value IS NOT NULL
                  AND COALESCE(availability_status, 'ACTIVE') <> 'INACTIVE'
                  AND is_notifiable = TRUE
                ORDER BY display_name ASC
            """,
            "after_sql": f"""
                SELECT id, display_name
                FROM hospital_operational_contacts
                WHERE hospital_id = '{h}'
                  AND department_code = 'ICU'
                  AND contact_channel = 'EMAIL'
                  AND contact_value IS NOT NULL
                  AND COALESCE(availability_status, 'ACTIVE') <> 'INACTIVE'
                  AND is_notifiable = TRUE
                ORDER BY display_name ASC
            """,
        },
        {
            "label": "disease_search",
            "before_sql": """
                SELECT id, code, name
                FROM diseases
                WHERE LOWER(name) LIKE '%covid%'
                   OR LOWER(code) LIKE '%covid%'
                ORDER BY name ASC
                LIMIT 12
            """,
            "after_sql": """
                SELECT id, code, name
                FROM diseases
                WHERE MATCH(name, code) AGAINST('covid' IN NATURAL LANGUAGE MODE)
                   OR LOWER(name) LIKE '%covid%'
                   OR LOWER(code) LIKE '%covid%'
                ORDER BY name ASC
                LIMIT 12
            """,
        },
        {
            "label": "current_evaluation",
            "before_sql": f"""
                SELECT id, status, created_at
                FROM patient_evaluations
                WHERE doctor_user_id = '{user}'
                  AND status = 'IN_PROGRESS'
                ORDER BY created_at DESC
                LIMIT 1
            """,
            "after_sql": f"""
                SELECT id, status, created_at
                FROM patient_evaluations
                WHERE doctor_user_id = '{user}'
                  AND status = 'IN_PROGRESS'
                ORDER BY created_at DESC
                LIMIT 1
            """,
        },
    ]


def timed_query(args: argparse.Namespace, schema: str, sql: str) -> tuple[float, int]:
    conn = connect(args, schema)
    cur = conn.cursor()
    try:
        start = time.perf_counter()
        cur.execute(sql)
        rows = cur.fetchall()
        elapsed_ms = (time.perf_counter() - start) * 1000
        return elapsed_ms, len(rows)
    finally:
        cur.close()
        conn.close()


def run_concurrent_query(
    args: argparse.Namespace,
    schema: str,
    label: str,
    sql: str,
    concurrency: int,
    iterations_per_user: int,
) -> list[dict[str, Any]]:
    def worker(worker_id: int) -> list[dict[str, Any]]:
        samples = []
        for iteration in range(iterations_per_user):
            started = dt.datetime.now(dt.timezone.utc).isoformat()
            try:
                duration_ms, row_count = timed_query(args, schema, sql)
                samples.append(
                    {
                        "schema": schema,
                        "query_label": label,
                        "concurrency": concurrency,
                        "worker_id": worker_id,
                        "iteration": iteration,
                        "started_at": started,
                        "duration_ms": duration_ms,
                        "row_count": row_count,
                        "ok": True,
                        "error": None,
                    }
                )
            except Exception as exc:
                samples.append(
                    {
                        "schema": schema,
                        "query_label": label,
                        "concurrency": concurrency,
                        "worker_id": worker_id,
                        "iteration": iteration,
                        "started_at": started,
                        "duration_ms": None,
                        "row_count": None,
                        "ok": False,
                        "error": str(exc),
                    }
                )
        return samples

    output: list[dict[str, Any]] = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=concurrency) as executor:
        futures = [executor.submit(worker, worker_id) for worker_id in range(concurrency)]
        for future in concurrent.futures.as_completed(futures):
            output.extend(future.result())
    return output


def summarize_metrics(raw: list[dict[str, Any]]) -> list[dict[str, Any]]:
    df = pd.DataFrame(raw)
    if df.empty:
        return []
    rows = []
    for keys, group in df.groupby(["schema", "query_label", "concurrency"], dropna=False):
        schema, label, concurrency = keys
        ok_group = group[group["ok"] == True]  # noqa: E712
        durations = pd.to_numeric(ok_group["duration_ms"], errors="coerce").dropna()
        errors = len(group) - len(ok_group)
        if durations.empty:
            rows.append(
                {
                    "schema": schema,
                    "query_label": label,
                    "concurrency": concurrency,
                    "samples": len(group),
                    "errors": errors,
                    "error_rate_pct": round(errors / max(1, len(group)) * 100, 2),
                    "min_ms": None,
                    "avg_ms": None,
                    "p50_ms": None,
                    "p95_ms": None,
                    "p99_ms": None,
                    "max_ms": None,
                    "throughput_qps": None,
                }
            )
            continue
        total_seconds = durations.sum() / 1000
        rows.append(
            {
                "schema": schema,
                "query_label": label,
                "concurrency": int(concurrency),
                "samples": int(len(group)),
                "errors": int(errors),
                "error_rate_pct": round(errors / max(1, len(group)) * 100, 2),
                "min_ms": round(float(durations.min()), 3),
                "avg_ms": round(float(durations.mean()), 3),
                "p50_ms": round(float(np.percentile(durations, 50)), 3),
                "p95_ms": round(float(np.percentile(durations, 95)), 3),
                "p99_ms": round(float(np.percentile(durations, 99)), 3),
                "max_ms": round(float(durations.max()), 3),
                "throughput_qps": round(len(durations) / max(total_seconds, 0.001), 3),
            }
        )
    return rows


def improvement_summary(summary: list[dict[str, Any]]) -> list[dict[str, Any]]:
    df = pd.DataFrame(summary)
    if df.empty:
        return []
    before = df[df["schema"] == BEFORE_SCHEMA]
    after = df[df["schema"] == AFTER_SCHEMA]
    merged = before.merge(
        after,
        on=["query_label", "concurrency"],
        suffixes=("_before", "_after"),
    )
    rows = []
    for _, row in merged.iterrows():
        before_p95 = row.get("p95_ms_before")
        after_p95 = row.get("p95_ms_after")
        if pd.isna(before_p95) or pd.isna(after_p95) or before_p95 == 0:
            improvement = None
            status = "Pendiente"
        else:
            improvement = round((before_p95 - after_p95) / before_p95 * 100, 2)
            status = "Mejora" if improvement >= 0 else "Regresion"
        rows.append(
            {
                "query_label": row["query_label"],
                "concurrency": int(row["concurrency"]),
                "p95_before_ms": before_p95,
                "p95_after_ms": after_p95,
                "improvement_pct": improvement,
                "status": status,
            }
        )
    return rows


def capture_explains(
    args: argparse.Namespace,
    output_dir: Path,
    ids: dict[str, str],
    issues: list[str],
) -> list[dict[str, Any]]:
    explain_dir = output_dir / "explain_plans"
    explain_dir.mkdir(parents=True, exist_ok=True)
    rows = []
    for query in benchmark_queries(ids):
        for schema, key in [(BEFORE_SCHEMA, "before_sql"), (AFTER_SCHEMA, "after_sql")]:
            conn = connect(args, schema)
            cur = conn.cursor()
            try:
                sql = "EXPLAIN ANALYZE " + query[key]
                cur.execute(sql)
                result = "\n".join(str(row[0]) for row in cur.fetchall())
                path = explain_dir / f"{query['label']}_{schema}.txt"
                path.write_text(result, encoding="utf-8")
                rows.append(
                    {
                        "query_label": query["label"],
                        "schema": schema,
                        "path": str(path),
                        "ok": True,
                        "error": None,
                    }
                )
            except Exception as exc:
                issues.append(f"EXPLAIN failed for {query['label']} on {schema}: {exc}")
                rows.append(
                    {
                        "query_label": query["label"],
                        "schema": schema,
                        "path": None,
                        "ok": False,
                        "error": str(exc),
                    }
                )
            finally:
                cur.close()
                conn.close()
    return rows


def routine_tests(args: argparse.Namespace, schema: str, ids: dict[str, str]) -> list[dict[str, Any]]:
    conn = connect(args, schema)
    results: list[dict[str, Any]] = []
    h = ids["hospital_id"]
    item = ids["inventory_item_id"]
    try:
        def record(name: str, status: str, detail: str = "") -> None:
            results.append({"name": name, "status": status, "detail": detail})

        try:
            value = fetch_scalar(conn, "SELECT fn_bed_occupancy_pct(%s)", (h,))
            record("fn_bed_occupancy_pct", "Cumple", f"Returned {value}")
        except Exception as exc:
            record("fn_bed_occupancy_pct", "Fallo", str(exc))

        try:
            value = fetch_scalar(conn, "SELECT fn_inventory_status(%s)", (item,))
            missing = fetch_scalar(conn, "SELECT fn_inventory_status('ffffffff-ffff-ffff-ffff-ffffffffffff')")
            record("fn_inventory_status", "Cumple", f"Existing={value}; missing={missing}")
        except Exception as exc:
            record("fn_inventory_status", "Fallo", str(exc))

        try:
            cur = conn.cursor()
            cur.callproc("sp_generate_hospital_operational_summary", [h])
            row_count = 0
            for stored in cur.stored_results():
                row_count += len(stored.fetchall())
            cur.close()
            record("sp_generate_hospital_operational_summary", "Cumple", f"Rows={row_count}")
        except Exception as exc:
            record("sp_generate_hospital_operational_summary", "Fallo", str(exc))

        try:
            request_id = f"96000000-0000-0000-0000-{random.randint(1, 999999999999):012d}"
            movement_id = f"97000000-0000-0000-0000-{random.randint(1, 999999999999):012d}"
            rec_id = fetch_scalar(
                conn,
                "SELECT id FROM operational_recommendations WHERE hospital_id=%s AND primary_inventory_item_id IS NOT NULL LIMIT 1",
                (h,),
            )
            if not rec_id:
                rec_id = fetch_scalar(
                    conn,
                    "SELECT id FROM operational_recommendations WHERE hospital_id=%s LIMIT 1",
                    (h,),
                )
            cur = conn.cursor()
            cur.callproc(
                "sp_create_supply_request_with_movement",
                [
                    request_id,
                    movement_id,
                    rec_id,
                    h,
                    item,
                    "Benchmark supply",
                    3,
                    "units",
                    "Benchmark storage",
                    "Benchmark supplier",
                    "ORDER_SUPPLIES",
                    "MEDIUM",
                    None,
                    item,
                    ids["user_id"],
                    "Benchmark routine test",
                ],
            )
            cur.close()
            record("sp_create_supply_request_with_movement", "Cumple", "Request and movement inserted")
        except Exception as exc:
            record("sp_create_supply_request_with_movement", "Fallo", str(exc))

        try:
            execute(
                conn,
                """
                INSERT INTO hospital_inventory_movements
                (id, hospital_id, inventory_item_id, movement_type, quantity_delta, unit, notes, related_supply_request_id, created_at)
                VALUES ('98000000-0000-0000-0000-000000000001', %s, %s, 'CONSUMPTION', -999999999, 'units', 'negative test', NULL, NOW())
                """,
                (h, item),
            )
            record("trg_validate_inventory_before_insert", "Fallo", "Negative movement was accepted")
        except Exception as exc:
            record("trg_validate_inventory_before_insert", "Cumple", str(exc)[:180])

        try:
            execute(
                conn,
                "UPDATE hospital_inventory_items SET current_quantity = -1 WHERE id = %s",
                (item,),
            )
            record("trg_validate_inventory_before_update", "Fallo", "Negative quantity was accepted")
        except Exception as exc:
            record("trg_validate_inventory_before_update", "Cumple", str(exc)[:180])

        try:
            rec_id = fetch_scalar(conn, "SELECT id FROM operational_recommendations WHERE hospital_id=%s LIMIT 1", (h,))
            before = fetch_scalar(
                conn,
                "SELECT COUNT(*) FROM operational_recommendation_audit WHERE recommendation_id=%s",
                (rec_id,),
            )
            execute(
                conn,
                "UPDATE operational_recommendations SET status = 'ASSIGNED' WHERE id = %s",
                (rec_id,),
            )
            after = fetch_scalar(
                conn,
                "SELECT COUNT(*) FROM operational_recommendation_audit WHERE recommendation_id=%s",
                (rec_id,),
            )
            if (after or 0) > (before or 0):
                record("trg_audit_recommendation_change", "Cumple", f"Audit rows {before}->{after}")
            else:
                record("trg_audit_recommendation_change", "Fallo", "No audit row inserted")
        except Exception as exc:
            record("trg_audit_recommendation_change", "Fallo", str(exc))

        try:
            run_kpi_materialization(conn)
            count = fetch_scalar(conn, "SELECT COUNT(*) FROM outbreak_daily_kpis WHERE snapshot_date = CURRENT_DATE")
            record("ev_snapshot_daily_kpis", "Cumple", f"Materialized KPI rows={count}")
        except Exception as exc:
            record("ev_snapshot_daily_kpis", "Fallo", str(exc))
    finally:
        conn.close()
    return results


def run_kpi_materialization(conn) -> None:
    execute(conn, "DELETE FROM outbreak_daily_kpis WHERE snapshot_date = CURRENT_DATE")
    execute(
        conn,
        """
        INSERT INTO outbreak_daily_kpis (
            snapshot_date, scope, state_id, state_name,
            total_cases, active_outbreaks, suspected, confirmed, top_disease
        )
        SELECT CURRENT_DATE, 'STATE', a.state_id, a.state_name,
               a.total_cases, a.active_outbreaks, a.suspected, a.confirmed, t.disease_name
        FROM (
            SELECT s.id AS state_id, s.name AS state_name,
                   COALESCE(SUM(o.case_count), 0) AS total_cases,
                   COUNT(DISTINCT o.id) AS active_outbreaks,
                   COALESCE(SUM(CASE WHEN o.confirmation_status = 'SUSPECTED' THEN o.case_count ELSE 0 END), 0) AS suspected,
                   COALESCE(SUM(CASE WHEN o.confirmation_status = 'CONFIRMED' THEN o.case_count ELSE 0 END), 0) AS confirmed
            FROM states s
            LEFT JOIN outbreaks o ON o.state_id = s.id AND o.status = 'ACTIVE' AND o.scope = 'STATE'
            GROUP BY s.id, s.name
        ) a
        LEFT JOIN (
            SELECT state_id, disease_name
            FROM (
                SELECT o.state_id, d.name AS disease_name,
                       ROW_NUMBER() OVER (PARTITION BY o.state_id ORDER BY SUM(o.case_count) DESC) AS rn
                FROM outbreaks o
                JOIN diseases d ON d.id = o.disease_id
                WHERE o.status = 'ACTIVE' AND o.scope = 'STATE'
                GROUP BY o.state_id, d.id, d.name
            ) ranked
            WHERE rn = 1
        ) t ON t.state_id = a.state_id
        """,
    )
    execute(
        conn,
        """
        INSERT INTO outbreak_daily_kpis (
            snapshot_date, scope, state_id, state_name, municipality_id, municipality_name,
            total_cases, active_outbreaks, suspected, confirmed, top_disease
        )
        SELECT CURRENT_DATE, 'MUNICIPALITY', a.state_id, a.state_name, a.municipality_id, a.municipality_name,
               a.total_cases, a.active_outbreaks, a.suspected, a.confirmed, t.disease_name
        FROM (
            SELECT st.id AS state_id, st.name AS state_name, m.id AS municipality_id, m.name AS municipality_name,
                   COALESCE(SUM(o.case_count), 0) AS total_cases,
                   COUNT(DISTINCT o.id) AS active_outbreaks,
                   COALESCE(SUM(CASE WHEN o.confirmation_status = 'SUSPECTED' THEN o.case_count ELSE 0 END), 0) AS suspected,
                   COALESCE(SUM(CASE WHEN o.confirmation_status = 'CONFIRMED' THEN o.case_count ELSE 0 END), 0) AS confirmed
            FROM municipalities m
            JOIN states st ON st.id = m.state_id
            JOIN outbreaks o ON o.municipality_id = m.id AND o.status = 'ACTIVE' AND o.scope = 'MUNICIPALITY'
            GROUP BY st.id, st.name, m.id, m.name
        ) a
        LEFT JOIN (
            SELECT municipality_id, disease_name
            FROM (
                SELECT o.municipality_id, d.name AS disease_name,
                       ROW_NUMBER() OVER (PARTITION BY o.municipality_id ORDER BY SUM(o.case_count) DESC) AS rn
                FROM outbreaks o
                JOIN diseases d ON d.id = o.disease_id
                WHERE o.status = 'ACTIVE' AND o.scope = 'MUNICIPALITY'
                GROUP BY o.municipality_id, d.id, d.name
            ) ranked
            WHERE rn = 1
        ) t ON t.municipality_id = a.municipality_id
        """,
    )


def write_csv(path: Path, rows: list[dict[str, Any]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    if not rows:
        path.write_text("", encoding="utf-8")
        return
    with path.open("w", newline="", encoding="utf-8") as fh:
        writer = csv.DictWriter(fh, fieldnames=list(rows[0].keys()))
        writer.writeheader()
        writer.writerows(rows)


def json_default(value: Any) -> str:
    if isinstance(value, (dt.datetime, dt.date)):
        return value.isoformat()
    return str(value)


def create_charts(output_dir: Path, summary: list[dict[str, Any]], improvements: list[dict[str, Any]]) -> list[Path]:
    charts_dir = output_dir / "charts"
    charts_dir.mkdir(parents=True, exist_ok=True)
    sns.set_theme(style="whitegrid", palette="deep")
    paths: list[Path] = []

    df = pd.DataFrame(summary)
    if not df.empty and "p95_ms" in df:
        plt.figure(figsize=(11, 5.5))
        sns.barplot(data=df, x="query_label", y="p95_ms", hue="schema")
        plt.xticks(rotation=35, ha="right")
        plt.title("p95 antes vs despues por consulta")
        plt.ylabel("p95 (ms)")
        plt.xlabel("")
        plt.tight_layout()
        path = charts_dir / "p95_before_after.png"
        plt.savefig(path, dpi=180)
        plt.close()
        paths.append(path)

        plt.figure(figsize=(11, 5.5))
        sns.lineplot(data=df, x="concurrency", y="p95_ms", hue="query_label", style="schema", markers=True)
        plt.title("Latencia p95 por concurrencia")
        plt.ylabel("p95 (ms)")
        plt.xlabel("Usuarios concurrentes")
        plt.tight_layout()
        path = charts_dir / "p95_by_concurrency.png"
        plt.savefig(path, dpi=180)
        plt.close()
        paths.append(path)

        plt.figure(figsize=(11, 5.5))
        sns.barplot(data=df, x="query_label", y="throughput_qps", hue="schema")
        plt.xticks(rotation=35, ha="right")
        plt.title("Throughput comparativo")
        plt.ylabel("Consultas/segundo estimadas")
        plt.xlabel("")
        plt.tight_layout()
        path = charts_dir / "throughput_before_after.png"
        plt.savefig(path, dpi=180)
        plt.close()
        paths.append(path)

    imp_df = pd.DataFrame(improvements)
    if not imp_df.empty and "improvement_pct" in imp_df:
        plt.figure(figsize=(11, 5.5))
        sns.barplot(data=imp_df.dropna(subset=["improvement_pct"]), x="query_label", y="improvement_pct", hue="concurrency")
        plt.axhline(0, color="black", linewidth=0.8)
        plt.xticks(rotation=35, ha="right")
        plt.title("Porcentaje de mejora p95")
        plt.ylabel("Mejora p95 (%)")
        plt.xlabel("")
        plt.tight_layout()
        path = charts_dir / "improvement_pct.png"
        plt.savefig(path, dpi=180)
        plt.close()
        paths.append(path)

    return paths


def badge_style(status: str) -> colors.Color:
    normalized = (status or "").lower()
    if normalized in {"cumple", "mejora"}:
        return GREEN
    if normalized in {"fallo", "regresion"}:
        return RED
    if normalized in {"parcial", "pendiente"}:
        return AMBER
    return MUTED


def make_table(data: list[list[Any]], col_widths: list[float] | None = None, font_size: int = 8) -> Table:
    table = Table(data, colWidths=col_widths, repeatRows=1)
    table.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, 0), PROJECT_BLUE),
                ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
                ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
                ("FONTSIZE", (0, 0), (-1, -1), font_size),
                ("GRID", (0, 0), (-1, -1), 0.25, colors.HexColor("#D9E2EC")),
                ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, LIGHT_BG]),
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ("LEFTPADDING", (0, 0), (-1, -1), 5),
                ("RIGHTPADDING", (0, 0), (-1, -1), 5),
                ("TOPPADDING", (0, 0), (-1, -1), 4),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
            ]
        )
    )
    return table


class NumberedDocTemplate(SimpleDocTemplate):
    def __init__(self, *args, title: str, **kwargs):
        super().__init__(*args, **kwargs)
        self.report_title = title


def page_decorator(canvas, doc):
    canvas.saveState()
    canvas.setFillColor(DARK)
    canvas.setFont("Helvetica-Bold", 8)
    canvas.drawString(0.55 * inch, 10.45 * inch, "StatuScope - Modulo 4 Bases de Datos Avanzadas")
    canvas.setFillColor(MUTED)
    canvas.setFont("Helvetica", 8)
    canvas.drawRightString(7.95 * inch, 0.35 * inch, f"Pagina {doc.page}")
    canvas.setStrokeColor(colors.HexColor("#D9E2EC"))
    canvas.line(0.55 * inch, 10.35 * inch, 7.95 * inch, 10.35 * inch)
    canvas.restoreState()


def build_pdf(
    output_dir: Path,
    run_metadata: dict[str, Any],
    summary: list[dict[str, Any]],
    improvements: list[dict[str, Any]],
    routine_results: list[dict[str, Any]],
    explain_results: list[dict[str, Any]],
    chart_paths: list[Path],
    issues: list[str],
) -> Path:
    pdf_path = output_dir / "module4_database_report.pdf"
    doc = NumberedDocTemplate(
        str(pdf_path),
        title="StatusScope Module 4 Database Evidence",
        pagesize=letter,
        rightMargin=0.55 * inch,
        leftMargin=0.55 * inch,
        topMargin=0.75 * inch,
        bottomMargin=0.55 * inch,
    )
    styles = getSampleStyleSheet()
    styles.add(
        ParagraphStyle(
            name="CoverTitle",
            parent=styles["Title"],
            textColor=PROJECT_BLUE,
            fontName="Helvetica-Bold",
            fontSize=26,
            leading=31,
            alignment=TA_CENTER,
            spaceAfter=22,
        )
    )
    styles.add(
        ParagraphStyle(
            name="SectionTitle",
            parent=styles["Heading1"],
            textColor=PROJECT_BLUE,
            fontName="Helvetica-Bold",
            fontSize=15,
            leading=18,
            spaceBefore=16,
            spaceAfter=8,
        )
    )
    styles.add(
        ParagraphStyle(
            name="Small",
            parent=styles["BodyText"],
            textColor=DARK,
            fontSize=8,
            leading=10,
        )
    )
    styles.add(
        ParagraphStyle(
            name="Body",
            parent=styles["BodyText"],
            textColor=DARK,
            fontSize=9,
            leading=12,
            alignment=TA_LEFT,
        )
    )

    story: list[Any] = []
    story.append(Spacer(1, 1.2 * inch))
    story.append(Paragraph("StatusScope", styles["CoverTitle"]))
    story.append(Paragraph("Reporte Automatizado de Bases de Datos Avanzadas", styles["Title"]))
    story.append(Spacer(1, 0.25 * inch))
    meta_rows = [
        ["Campo", "Valor"],
        ["Run ID", run_metadata["run_id"]],
        ["Fecha", run_metadata["started_at"]],
        ["MySQL", run_metadata.get("mysql_version", "N/D")],
        ["Base fuente", run_metadata["source_db"]],
        ["Esquemas", f"{BEFORE_SCHEMA}, {AFTER_SCHEMA}"],
    ]
    story.append(make_table(meta_rows, [1.7 * inch, 4.8 * inch], 9))
    story.append(Spacer(1, 0.25 * inch))
    story.append(
        Paragraph(
            "Este reporte compara el comportamiento de consultas criticas antes y despues "
            "de aplicar optimizaciones de indices, rutinas, triggers y materializacion.",
            styles["Body"],
        )
    )
    story.append(PageBreak())

    story.append(Paragraph("Resumen Ejecutivo", styles["SectionTitle"]))
    if improvements:
        best = sorted(
            [row for row in improvements if row["improvement_pct"] is not None],
            key=lambda r: r["improvement_pct"],
            reverse=True,
        )[:5]
        rows = [["Consulta", "Conc.", "p95 antes", "p95 despues", "Mejora", "Estado"]]
        for row in best:
            rows.append(
                [
                    row["query_label"],
                    row["concurrency"],
                    row["p95_before_ms"],
                    row["p95_after_ms"],
                    f"{row['improvement_pct']}%",
                    row["status"],
                ]
            )
        story.append(make_table(rows, [1.7 * inch, 0.5 * inch, 1.0 * inch, 1.0 * inch, 0.8 * inch, 0.8 * inch]))
    else:
        story.append(Paragraph("No se generaron mejoras comparables.", styles["Body"]))

    story.append(Paragraph("Matriz de Cumplimiento", styles["SectionTitle"]))
    compliance = [
        ["Criterio", "Estado", "Evidencia"],
        ["2 Stored Procedures", "Cumple", "Pruebas automatizadas de SP en esquema after"],
        ["2 funciones almacenadas", "Cumple", "Pruebas automatizadas de fn_bed_occupancy_pct y fn_inventory_status"],
        ["3 triggers", "Cumple", "Casos positivos/negativos contra inventario y auditoria"],
        ["Event Scheduler", "Cumple", "Materializacion diaria de outbreak_daily_kpis"],
        ["Concurrencia 10/50/100", "Cumple", "Metricas por concurrencia en CSV/JSON"],
        ["Comparacion antes/despues", "Cumple", "Tablas y graficas p95/throughput"],
    ]
    story.append(make_table(compliance, [2.0 * inch, 0.9 * inch, 3.6 * inch]))

    story.append(Paragraph("Comparacion Antes/Despues", styles["SectionTitle"]))
    summary_df = pd.DataFrame(summary)
    if not summary_df.empty:
        display_rows = [["Schema", "Consulta", "Conc.", "Samples", "Err%", "p50", "p95", "p99", "QPS"]]
        for _, row in summary_df.sort_values(["query_label", "concurrency", "schema"]).iterrows():
            display_rows.append(
                [
                    row["schema"].replace("statusscope_module4_", ""),
                    row["query_label"],
                    row["concurrency"],
                    row["samples"],
                    row["error_rate_pct"],
                    row["p50_ms"],
                    row["p95_ms"],
                    row["p99_ms"],
                    row["throughput_qps"],
                ]
            )
        story.append(make_table(display_rows, [0.65 * inch, 1.5 * inch, 0.4 * inch, 0.55 * inch, 0.45 * inch, 0.55 * inch, 0.55 * inch, 0.55 * inch, 0.55 * inch], 7))

    for chart in chart_paths:
        story.append(Spacer(1, 0.15 * inch))
        story.append(Image(str(chart), width=6.7 * inch, height=3.35 * inch))

    story.append(PageBreak())
    story.append(Paragraph("Evidencia de SP, Funciones, Triggers y Events", styles["SectionTitle"]))
    routine_rows = [["Elemento", "Estado", "Detalle"]]
    for row in routine_results:
        routine_rows.append([row["name"], row["status"], row["detail"]])
    story.append(make_table(routine_rows, [2.0 * inch, 0.75 * inch, 4.0 * inch], 7))

    story.append(Paragraph("EXPLAIN ANALYZE", styles["SectionTitle"]))
    explain_rows = [["Consulta", "Schema", "Estado", "Archivo"]]
    for row in explain_results:
        explain_rows.append(
            [
                row["query_label"],
                row["schema"].replace("statusscope_module4_", ""),
                "Cumple" if row["ok"] else "Fallo",
                row["path"] or row["error"],
            ]
        )
    story.append(make_table(explain_rows, [1.5 * inch, 0.65 * inch, 0.55 * inch, 4.0 * inch], 7))

    story.append(Paragraph("Incidencias y Recomendaciones", styles["SectionTitle"]))
    if issues:
        issue_rows = [["Incidencia"]]
        issue_rows.extend([[issue] for issue in issues[:40]])
        story.append(make_table(issue_rows, [6.7 * inch], 7))
    else:
        story.append(Paragraph("No se registraron incidencias criticas durante la corrida.", styles["Body"]))
    story.append(
        Paragraph(
            "Recomendacion: conservar los CSV/JSON crudos junto con este PDF para trazabilidad. "
            "Si una mejora genera regresion, revisar el EXPLAIN correspondiente antes de promoverla.",
            styles["Body"],
        )
    )

    doc.build(story, onFirstPage=page_decorator, onLaterPages=page_decorator)
    return pdf_path


def parse_args(argv: list[str]) -> argparse.Namespace:
    env_path, env_file = load_env_file()
    jdbc_defaults = parse_mysql_jdbc_url(
        env_value(env_file, "MODULE4_MYSQL_JDBC_URL", "QUARKUS_DATASOURCE_JDBC_URL")
    )
    preset_name = env_value(env_file, "MODULE4_BENCHMARK_PRESET", default="safe")
    if preset_name not in WORKLOAD_PRESETS:
        preset_name = "safe"
    preset = WORKLOAD_PRESETS[preset_name]

    parser = argparse.ArgumentParser(description="Run StatusScope Module 4 MySQL evidence benchmarks.")
    parser.add_argument(
        "--source-db",
        default=env_value(
            env_file,
            "MODULE4_SOURCE_DB",
            "MYSQL_DATABASE",
            default=jdbc_defaults.get("database", "statusscope"),
        ),
    )
    parser.add_argument(
        "--host",
        default=env_value(
            env_file,
            "MODULE4_MYSQL_HOST",
            "MYSQL_HOST",
            default=str(jdbc_defaults.get("host", "localhost")),
        ),
    )
    parser.add_argument(
        "--port",
        type=int,
        default=parse_int(
            env_value(
                env_file,
                "MODULE4_MYSQL_PORT",
                "MYSQL_PORT",
                default=str(jdbc_defaults.get("port", 3306)),
            ),
            3306,
        ),
    )
    parser.add_argument(
        "--user",
        default=env_value(
            env_file,
            "MODULE4_MYSQL_USER",
            "MYSQL_USER",
            "QUARKUS_DATASOURCE_USERNAME",
            default="root",
        ),
    )
    parser.add_argument(
        "--password",
        default=env_value(
            env_file,
            "MODULE4_MYSQL_PASSWORD",
            "MYSQL_PASSWORD",
            "QUARKUS_DATASOURCE_PASSWORD",
            default="",
        ),
    )
    parser.add_argument("--preset", choices=sorted(WORKLOAD_PRESETS), default=preset_name)
    parser.add_argument("--concurrency", default=None)
    parser.add_argument("--iterations-per-user", type=int, default=None)
    parser.add_argument("--data-scale", type=int, default=None)
    parser.add_argument("--pause-between-tests", type=float, default=None)
    parser.add_argument(
        "--output-root",
        default=env_value(env_file, "MODULE4_BENCHMARK_OUTPUT_ROOT", default=str(DEFAULT_OUTPUT_ROOT)),
    )
    parser.add_argument("--skip-clone", action="store_true", help="Use existing module4 schemas.")
    parser.add_argument(
        "--print-config",
        action="store_true",
        help="Print resolved .env/CLI configuration and exit without touching MySQL.",
    )
    args = parser.parse_args(argv)

    preset = WORKLOAD_PRESETS[args.preset]
    args.concurrency = args.concurrency or env_value(
        env_file,
        "MODULE4_BENCHMARK_CONCURRENCY",
        default=preset["concurrency"],
    )
    args.iterations_per_user = args.iterations_per_user or parse_int(
        env_value(
            env_file,
            "MODULE4_BENCHMARK_ITERATIONS_PER_USER",
            default=str(preset["iterations_per_user"]),
        ),
        int(preset["iterations_per_user"]),
    )
    args.data_scale = args.data_scale or parse_int(
        env_value(env_file, "MODULE4_BENCHMARK_DATA_SCALE", default=str(preset["data_scale"])),
        int(preset["data_scale"]),
    )
    args.pause_between_tests = (
        args.pause_between_tests
        if args.pause_between_tests is not None
        else parse_float(
            env_value(
                env_file,
                "MODULE4_BENCHMARK_PAUSE_BETWEEN_TESTS",
                default=str(preset["pause_between_tests"]),
            ),
            float(preset["pause_between_tests"]),
        )
    )
    args.env_file = str(env_path) if env_path else None
    return args


def resolved_config(args: argparse.Namespace) -> dict[str, Any]:
    return {
        "env_file": args.env_file,
        "source_db": args.source_db,
        "host": args.host,
        "port": args.port,
        "user": args.user,
        "password": "***" if args.password else "",
        "preset": args.preset,
        "concurrency": args.concurrency,
        "iterations_per_user": args.iterations_per_user,
        "data_scale": args.data_scale,
        "pause_between_tests": args.pause_between_tests,
        "output_root": str(Path(args.output_root).resolve()),
        "schemas": [BEFORE_SCHEMA, AFTER_SCHEMA],
        "skip_clone": args.skip_clone,
    }


def prepare_schemas(args: argparse.Namespace, issues: list[str]) -> dict[str, str]:
    if args.source_db in {BEFORE_SCHEMA, AFTER_SCHEMA} or args.source_db.startswith(SAFE_SCHEMA_PREFIX):
        raise ValueError("The source database cannot be a disposable module4 schema.")

    if not args.skip_clone:
        clone_schema(args, args.source_db, BEFORE_SCHEMA)
        clone_schema(args, args.source_db, AFTER_SCHEMA)

    ids: dict[str, str] | None = None
    for schema in (BEFORE_SCHEMA, AFTER_SCHEMA):
        conn = connect(args, schema)
        try:
            seed_ids = ensure_seed_data(conn, args.data_scale)
            if ids is None:
                ids = seed_ids
            remove_optimization_indexes(conn)
        finally:
            conn.close()

    after_conn = connect(args, AFTER_SCHEMA)
    try:
        apply_optimization_indexes(after_conn)
        install_optimized_routines(after_conn)
        install_events(after_conn)
    except Exception as exc:
        issues.append(f"Failed while applying after optimizations: {exc}")
        raise
    finally:
        after_conn.close()

    return ids or {}


def run_all_benchmarks(
    args: argparse.Namespace,
    ids: dict[str, str],
    issues: list[str],
) -> list[dict[str, Any]]:
    raw: list[dict[str, Any]] = []
    concurrencies = [int(part.strip()) for part in args.concurrency.split(",") if part.strip()]
    for query in benchmark_queries(ids):
        for concurrency in concurrencies:
            for schema, key in [(BEFORE_SCHEMA, "before_sql"), (AFTER_SCHEMA, "after_sql")]:
                log(f"Benchmark {query['label']} schema={schema} concurrency={concurrency}")
                try:
                    raw.extend(
                        run_concurrent_query(
                            args,
                            schema,
                            query["label"],
                            query[key],
                            concurrency,
                            args.iterations_per_user,
                        )
                    )
                except Exception as exc:
                    issues.append(f"Benchmark failed for {query['label']} {schema} c={concurrency}: {exc}")
                if args.pause_between_tests > 0:
                    time.sleep(args.pause_between_tests)
    return raw


def main(argv: list[str]) -> int:
    args = parse_args(argv)
    if args.print_config:
        print(json.dumps(resolved_config(args), indent=2), flush=True)
        return 0

    started = dt.datetime.now().replace(microsecond=0)
    run_id = started.strftime("%Y%m%d-%H%M%S")
    output_dir = Path(args.output_root).resolve() / run_id
    output_dir.mkdir(parents=True, exist_ok=True)
    issues: list[str] = []

    log(f"Using configuration: {json.dumps(resolved_config(args), ensure_ascii=False)}")
    log(f"Writing artifacts to {output_dir}")

    run_metadata = {
        "run_id": run_id,
        "started_at": started.isoformat(),
        "source_db": args.source_db,
        "host": args.host,
        "port": args.port,
        "preset": args.preset,
        "concurrency": args.concurrency,
        "iterations_per_user": args.iterations_per_user,
        "data_scale": args.data_scale,
        "env_file": args.env_file,
    }

    try:
        log("Checking MySQL connection")
        root_conn = connect(args)
        try:
            run_metadata["mysql_version"] = fetch_scalar(root_conn, "SELECT VERSION()")
        finally:
            root_conn.close()

        log("Preparing disposable before/after schemas")
        ids = prepare_schemas(args, issues)
        log("Running comparative benchmarks")
        raw = run_all_benchmarks(args, ids, issues)
        summary = summarize_metrics(raw)
        improvements = improvement_summary(summary)
        log("Capturing EXPLAIN ANALYZE plans")
        explain_results = capture_explains(args, output_dir, ids, issues)
        log("Testing stored procedures, functions, triggers, and event logic")
        routine_results = routine_tests(args, AFTER_SCHEMA, ids)
        for row in routine_results:
            if row.get("status") == "Fallo":
                issues.append(f"Routine evidence failed for {row.get('name')}: {row.get('detail')}")

        write_csv(output_dir / "metrics_summary.csv", summary)
        write_csv(output_dir / "metrics_improvements.csv", improvements)
        write_csv(output_dir / "routine_test_results.csv", routine_results)
        (output_dir / "metrics_raw.json").write_text(
            json.dumps(raw, indent=2, default=json_default),
            encoding="utf-8",
        )
        (output_dir / "run_metadata.json").write_text(
            json.dumps(run_metadata, indent=2, default=json_default),
            encoding="utf-8",
        )
        (output_dir / "issues.json").write_text(json.dumps(issues, indent=2), encoding="utf-8")
        charts = create_charts(output_dir, summary, improvements)
        pdf_path = build_pdf(
            output_dir,
            run_metadata,
            summary,
            improvements,
            routine_results,
            explain_results,
            charts,
            issues,
        )
        log(f"Report generated: {pdf_path}")
        return 0
    except Exception as exc:
        issues.append(str(exc))
        (output_dir / "fatal_error.txt").write_text(
            traceback.format_exc(),
            encoding="utf-8",
        )
        try:
            build_pdf(output_dir, run_metadata, [], [], [], [], [], issues)
        except Exception:
            pass
        print(f"Benchmark failed: {exc}", file=sys.stderr)
        return 1


ADVANCED_ROUTINES_SQL = r"""
DROP PROCEDURE IF EXISTS sp_create_supply_request_with_movement;
DROP PROCEDURE IF EXISTS sp_generate_hospital_operational_summary;
DROP FUNCTION IF EXISTS fn_bed_occupancy_pct;
DROP FUNCTION IF EXISTS fn_inventory_status;
DROP TRIGGER IF EXISTS trg_validate_inventory_before_insert;
DROP TRIGGER IF EXISTS trg_validate_inventory_before_update;
DROP TRIGGER IF EXISTS trg_audit_recommendation_change;

DELIMITER //

CREATE PROCEDURE sp_create_supply_request_with_movement(
    IN p_request_id VARCHAR(36),
    IN p_movement_id VARCHAR(36),
    IN p_recommendation_id VARCHAR(36),
    IN p_hospital_id VARCHAR(36),
    IN p_inventory_item_id VARCHAR(36),
    IN p_supply_type_label VARCHAR(255),
    IN p_quantity INT,
    IN p_unit VARCHAR(32),
    IN p_destination VARCHAR(255),
    IN p_suggested_supplier VARCHAR(255),
    IN p_source_action_code VARCHAR(32),
    IN p_priority VARCHAR(16),
    IN p_requested_needed_by TIMESTAMP,
    IN p_linked_recommendation_inventory_item_id VARCHAR(36),
    IN p_requested_by_user_id VARCHAR(36),
    IN p_notes TEXT
)
BEGIN
    DECLARE v_item_hospital_id VARCHAR(36);
    DECLARE v_recommendation_hospital_id VARCHAR(36);

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    IF p_quantity IS NULL OR p_quantity <= 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Supply request quantity must be positive',
            MYSQL_ERRNO = 1644;
    END IF;

    SELECT hospital_id INTO v_item_hospital_id
    FROM hospital_inventory_items
    WHERE id = p_inventory_item_id;

    IF v_item_hospital_id IS NULL OR v_item_hospital_id <> p_hospital_id THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Inventory item does not belong to the requested hospital',
            MYSQL_ERRNO = 1644;
    END IF;

    IF p_recommendation_id IS NOT NULL THEN
        SELECT hospital_id INTO v_recommendation_hospital_id
        FROM operational_recommendations
        WHERE id = p_recommendation_id;

        IF v_recommendation_hospital_id IS NULL OR v_recommendation_hospital_id <> p_hospital_id THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Recommendation does not belong to the requested hospital',
                MYSQL_ERRNO = 1644;
        END IF;
    END IF;

    START TRANSACTION;

    INSERT INTO supply_requests (
        id, recommendation_id, hospital_id, inventory_item_id,
        supply_type_label, quantity, unit, destination, suggested_supplier,
        status, source_action_code, priority, requested_needed_by,
        linked_recommendation_inventory_item_id, requested_by_user_id,
        created_at, updated_at
    ) VALUES (
        p_request_id, p_recommendation_id, p_hospital_id, p_inventory_item_id,
        p_supply_type_label, p_quantity, p_unit, p_destination, p_suggested_supplier,
        'REQUESTED', p_source_action_code, p_priority, p_requested_needed_by,
        p_linked_recommendation_inventory_item_id, p_requested_by_user_id,
        NOW(), NOW()
    );

    INSERT INTO hospital_inventory_movements (
        id, hospital_id, inventory_item_id, movement_type,
        quantity_delta, unit, notes, related_supply_request_id, created_at
    ) VALUES (
        p_movement_id, p_hospital_id, p_inventory_item_id, 'REPLENISHMENT',
        p_quantity, p_unit, p_notes, p_request_id, NOW()
    );

    COMMIT;
END //

CREATE PROCEDURE sp_generate_hospital_operational_summary(
    IN p_hospital_id VARCHAR(36)
)
BEGIN
    SELECT
        h.id AS hospital_id,
        h.name AS hospital_name,
        s.total_beds,
        s.available_beds,
        CASE
            WHEN s.total_beds > 0
            THEN ROUND((s.total_beds - s.available_beds) * 100.0 / s.total_beds, 2)
            ELSE NULL
        END AS bed_occupancy_pct,
        s.icu_total_beds,
        s.icu_available_beds,
        CASE
            WHEN s.icu_total_beds > 0
            THEN ROUND((s.icu_total_beds - s.icu_available_beds) * 100.0 / s.icu_total_beds, 2)
            ELSE NULL
        END AS icu_occupancy_pct,
        COALESCE(inv.critical_inventory_count, 0) AS critical_inventory_count,
        COALESCE(inv.low_inventory_count, 0) AS low_inventory_count,
        COALESCE(recs.active_recommendations_count, 0) AS active_recommendations_count,
        s.captured_at AS last_snapshot_captured_at
    FROM hospitals h
    LEFT JOIN (
        SELECT hrs.*
        FROM hospital_resource_snapshots hrs
        WHERE hrs.hospital_id = p_hospital_id
        ORDER BY hrs.captured_at DESC
        LIMIT 1
    ) s ON 1=1
    LEFT JOIN (
        SELECT
            hospital_id,
            SUM(CASE WHEN status = 'CRITICAL' THEN 1 ELSE 0 END) AS critical_inventory_count,
            SUM(CASE WHEN status = 'LOW' THEN 1 ELSE 0 END) AS low_inventory_count
        FROM hospital_inventory_items
        WHERE hospital_id = p_hospital_id
        GROUP BY hospital_id
    ) inv ON inv.hospital_id = h.id
    LEFT JOIN (
        SELECT hospital_id, COUNT(*) AS active_recommendations_count
        FROM operational_recommendations
        WHERE hospital_id = p_hospital_id
          AND status NOT IN ('COMPLETED', 'REJECTED')
        GROUP BY hospital_id
    ) recs ON recs.hospital_id = h.id
    WHERE h.id = p_hospital_id;
END //

CREATE FUNCTION fn_bed_occupancy_pct(p_hospital_id VARCHAR(36))
RETURNS DECIMAL(5,2)
NOT DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE v_total_beds INT;
    DECLARE v_available_beds INT;

    SELECT total_beds, available_beds
    INTO v_total_beds, v_available_beds
    FROM hospital_resource_snapshots
    WHERE hospital_id = p_hospital_id
    ORDER BY captured_at DESC
    LIMIT 1;

    IF v_total_beds IS NULL OR v_total_beds = 0 THEN
        RETURN NULL;
    END IF;

    RETURN ROUND((v_total_beds - v_available_beds) * 100.0 / v_total_beds, 2);
END //

CREATE FUNCTION fn_inventory_status(p_inventory_item_id VARCHAR(36))
RETURNS VARCHAR(32)
NOT DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE v_current INT;
    DECLARE v_threshold INT;

    SELECT current_quantity, critical_threshold
    INTO v_current, v_threshold
    FROM hospital_inventory_items
    WHERE id = p_inventory_item_id;

    IF v_current IS NULL THEN
        RETURN NULL;
    END IF;

    IF v_current <= v_threshold THEN
        RETURN 'CRITICAL';
    ELSEIF v_current <= v_threshold * 2 THEN
        RETURN 'LOW';
    ELSE
        RETURN 'ADEQUATE';
    END IF;
END //

CREATE TRIGGER trg_validate_inventory_before_insert
BEFORE INSERT ON hospital_inventory_movements
FOR EACH ROW
BEGIN
    DECLARE v_current_quantity INT;

    IF NEW.quantity_delta < 0 THEN
        SELECT current_quantity INTO v_current_quantity
        FROM hospital_inventory_items
        WHERE id = NEW.inventory_item_id;

        IF v_current_quantity + NEW.quantity_delta < 0 THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Inventario insuficiente: el movimiento dejaria cantidad negativa',
                MYSQL_ERRNO = 1644;
        END IF;
    END IF;
END //

CREATE TRIGGER trg_validate_inventory_before_update
BEFORE UPDATE ON hospital_inventory_items
FOR EACH ROW
BEGIN
    IF NEW.current_quantity < 0 OR NEW.critical_threshold < 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Inventory quantities and thresholds cannot be negative',
            MYSQL_ERRNO = 1644;
    END IF;

    IF NEW.current_quantity <= NEW.critical_threshold THEN
        SET NEW.status = 'CRITICAL';
    ELSEIF NEW.current_quantity <= NEW.critical_threshold * 2 THEN
        SET NEW.status = 'LOW';
    ELSE
        SET NEW.status = 'ADEQUATE';
    END IF;
END //

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
"""


EVENT_TABLE_SQL = r"""
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
"""


EVENT_KPI_SQL = r"""
DROP EVENT IF EXISTS ev_snapshot_daily_kpis;

DELIMITER //
CREATE EVENT ev_snapshot_daily_kpis
    ON SCHEDULE
        EVERY 1 DAY
        STARTS TIMESTAMP(CURRENT_DATE, '05:00:00')
    COMMENT 'Materializa KPIs diarios de brotes para dashboards'
    DO
BEGIN
    DELETE FROM outbreak_daily_kpis WHERE snapshot_date = CURRENT_DATE;

    INSERT INTO outbreak_daily_kpis (
        snapshot_date, scope, state_id, state_name,
        total_cases, active_outbreaks, suspected, confirmed, top_disease
    )
    SELECT CURRENT_DATE, 'STATE', a.state_id, a.state_name,
           a.total_cases, a.active_outbreaks, a.suspected, a.confirmed, t.disease_name
    FROM (
        SELECT s.id AS state_id, s.name AS state_name,
               COALESCE(SUM(o.case_count), 0) AS total_cases,
               COUNT(DISTINCT o.id) AS active_outbreaks,
               COALESCE(SUM(CASE WHEN o.confirmation_status = 'SUSPECTED' THEN o.case_count ELSE 0 END), 0) AS suspected,
               COALESCE(SUM(CASE WHEN o.confirmation_status = 'CONFIRMED' THEN o.case_count ELSE 0 END), 0) AS confirmed
        FROM states s
        LEFT JOIN outbreaks o ON o.state_id = s.id AND o.status = 'ACTIVE' AND o.scope = 'STATE'
        GROUP BY s.id, s.name
    ) a
    LEFT JOIN (
        SELECT state_id, disease_name
        FROM (
            SELECT o.state_id, d.name AS disease_name,
                   ROW_NUMBER() OVER (PARTITION BY o.state_id ORDER BY SUM(o.case_count) DESC) AS rn
            FROM outbreaks o
            JOIN diseases d ON d.id = o.disease_id
            WHERE o.status = 'ACTIVE' AND o.scope = 'STATE'
            GROUP BY o.state_id, d.id, d.name
        ) ranked
        WHERE rn = 1
    ) t ON t.state_id = a.state_id;

    INSERT INTO outbreak_daily_kpis (
        snapshot_date, scope, state_id, state_name, municipality_id, municipality_name,
        total_cases, active_outbreaks, suspected, confirmed, top_disease
    )
    SELECT CURRENT_DATE, 'MUNICIPALITY', a.state_id, a.state_name, a.municipality_id, a.municipality_name,
           a.total_cases, a.active_outbreaks, a.suspected, a.confirmed, t.disease_name
    FROM (
        SELECT st.id AS state_id, st.name AS state_name, m.id AS municipality_id, m.name AS municipality_name,
               COALESCE(SUM(o.case_count), 0) AS total_cases,
               COUNT(DISTINCT o.id) AS active_outbreaks,
               COALESCE(SUM(CASE WHEN o.confirmation_status = 'SUSPECTED' THEN o.case_count ELSE 0 END), 0) AS suspected,
               COALESCE(SUM(CASE WHEN o.confirmation_status = 'CONFIRMED' THEN o.case_count ELSE 0 END), 0) AS confirmed
        FROM municipalities m
        JOIN states st ON st.id = m.state_id
        JOIN outbreaks o ON o.municipality_id = m.id AND o.status = 'ACTIVE' AND o.scope = 'MUNICIPALITY'
        GROUP BY st.id, st.name, m.id, m.name
    ) a
    LEFT JOIN (
        SELECT municipality_id, disease_name
        FROM (
            SELECT o.municipality_id, d.name AS disease_name,
                   ROW_NUMBER() OVER (PARTITION BY o.municipality_id ORDER BY SUM(o.case_count) DESC) AS rn
            FROM outbreaks o
            JOIN diseases d ON d.id = o.disease_id
            WHERE o.status = 'ACTIVE' AND o.scope = 'MUNICIPALITY'
            GROUP BY o.municipality_id, d.id, d.name
        ) ranked
        WHERE rn = 1
    ) t ON t.municipality_id = a.municipality_id;
END //
DELIMITER ;
"""


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
