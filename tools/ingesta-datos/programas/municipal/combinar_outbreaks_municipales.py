import argparse
import csv
from collections import defaultdict
from datetime import datetime
from pathlib import Path

from comun.rutas import MUNICIPAL_OUTPUT_DIR


DEFAULT_INPUT_DIR = MUNICIPAL_OUTPUT_DIR
DEFAULT_OUTPUT_PATH = DEFAULT_INPUT_DIR / "municipal_outbreak_signals.csv"

INPUT_FILES = [
    "respiratory_municipal_cases.csv",
    "febrile_exanthematous_municipal_cases.csv",
    "dengue_municipal_cases.csv",
]

OUTPUT_FIELDS = [
    "scope",
    "municipality_id",
    "municipio_nombre",
    "disease_key",
    "disease_name",
    "confirmation_status",
    "case_count",
    "started_at",
    "ended_at",
    "status",
    "source_datasets",
    "latest_update",
]

DISEASE_NAME_BY_SIGNAL_KEY = {
    "COVID-19": "COVID-19",
    "INFLUENZA": "Influenza",
    "MEASLES": "Measles",
    "RUBELLA": "Rubella",
    "DENGUE": "Dengue fever",
    "CHIKUNGUNYA": "Chikungunya",
}

DATE_FIELDS_BY_PRIORITY = [
    "fecha_sintomas",
    "fecha_diagnostico",
    "fecha_actualizacion",
]


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Combine extracted municipal case CSVs into grouped outbreak signals."
    )
    parser.add_argument("--input-dir", type=Path, default=DEFAULT_INPUT_DIR)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT_PATH)
    args = parser.parse_args()

    grouped = {}
    source_rows = defaultdict(int)
    expanded_signals = 0
    skipped_rows = 0

    for file_name in INPUT_FILES:
        path = args.input_dir / file_name
        if not path.exists():
            raise FileNotFoundError(f"Missing input CSV: {path}")

        with path.open("r", encoding="utf-8-sig", newline="") as source:
            reader = csv.DictReader(source)
            for row in reader:
                source_rows[file_name] += 1
                municipality_id = row.get("municipality_id", "").strip()
                if not municipality_id:
                    skipped_rows += 1
                    continue

                for disease_key, confirmation_status in parse_diseases_flagged(row.get("diseases_flagged", "")):
                    expanded_signals += 1
                    disease_name = DISEASE_NAME_BY_SIGNAL_KEY.get(disease_key, disease_key.title())
                    group_key = (municipality_id, disease_key, confirmation_status)
                    case_date = case_signal_date(row)
                    latest_update = parse_date(row.get("fecha_actualizacion", ""))

                    if group_key not in grouped:
                        grouped[group_key] = {
                            "scope": "MUNICIPALITY",
                            "municipality_id": municipality_id,
                            "municipio_nombre": row.get("municipio_nombre", "").strip(),
                            "disease_key": disease_key,
                            "disease_name": disease_name,
                            "confirmation_status": confirmation_status,
                            "case_count": 0,
                            "started_at": case_date,
                            "ended_at": None,
                            "status": "ACTIVE",
                            "source_datasets": set(),
                            "latest_update": latest_update,
                        }

                    aggregate = grouped[group_key]
                    aggregate["case_count"] += 1
                    aggregate["source_datasets"].add(row.get("source_dataset", file_name).strip() or file_name)
                    aggregate["started_at"] = min_optional_date(aggregate["started_at"], case_date)
                    aggregate["latest_update"] = max_optional_date(aggregate["latest_update"], latest_update)

    rows = [serialize_row(row) for row in sorted(grouped.values(), key=sort_key)]
    write_csv(args.output, rows)

    print(f"Input rows: {sum(source_rows.values())}")
    for file_name, count in sorted(source_rows.items()):
        print(f"  - {file_name}: {count}")
    print(f"Expanded disease signals: {expanded_signals}")
    print(f"Skipped rows without municipality_id: {skipped_rows}")
    print(f"Grouped outbreak signals: {len(rows)}")
    print(f"Output CSV: {args.output}")


def parse_diseases_flagged(value: str) -> list[tuple[str, str]]:
    signals = []
    for raw_signal in value.split("|"):
        raw_signal = raw_signal.strip()
        if not raw_signal:
            continue
        parts = raw_signal.split(":", 1)
        if len(parts) != 2:
            raise ValueError(f"Invalid diseases_flagged signal: {raw_signal}")
        disease_key = parts[0].strip().upper()
        confirmation_status = parts[1].strip().upper()
        if confirmation_status not in {"CONFIRMED", "SUSPECTED"}:
            raise ValueError(f"Invalid confirmation status in signal: {raw_signal}")
        signals.append((disease_key, confirmation_status))
    return signals


def case_signal_date(row: dict[str, str]):
    for field in DATE_FIELDS_BY_PRIORITY:
        parsed = parse_date(row.get(field, ""))
        if parsed is not None:
            return parsed
    return None


def parse_date(value: str):
    value = value.strip()
    if not value:
        return None
    for fmt in ("%Y-%m-%d", "%d/%m/%Y"):
        try:
            return datetime.strptime(value, fmt).date()
        except ValueError:
            pass
    raise ValueError(f"Unsupported date format: {value}")


def min_optional_date(left, right):
    if left is None:
        return right
    if right is None:
        return left
    return min(left, right)


def max_optional_date(left, right):
    if left is None:
        return right
    if right is None:
        return left
    return max(left, right)


def serialize_row(row: dict) -> dict[str, str]:
    serialized = row.copy()
    serialized["case_count"] = str(serialized["case_count"])
    serialized["source_datasets"] = "|".join(sorted(serialized["source_datasets"]))
    serialized["started_at"] = format_datetime(serialized["started_at"])
    serialized["ended_at"] = ""
    serialized["latest_update"] = format_date(serialized["latest_update"])
    return serialized


def format_datetime(value) -> str:
    if value is None:
        return ""
    return f"{value.isoformat()} 00:00:00"


def format_date(value) -> str:
    return "" if value is None else value.isoformat()


def sort_key(row: dict):
    return (
        row["municipio_nombre"],
        row["disease_name"],
        row["confirmation_status"],
        row["municipality_id"],
    )


def write_csv(path: Path, rows: list[dict[str, str]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as target:
        writer = csv.DictWriter(target, fieldnames=OUTPUT_FIELDS)
        writer.writeheader()
        writer.writerows(rows)


if __name__ == "__main__":
    main()
