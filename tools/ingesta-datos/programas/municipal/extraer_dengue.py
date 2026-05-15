from __future__ import annotations

import argparse
import csv
from datetime import date
from pathlib import Path

from comun.outbreaks import (
    age_years_or_one,
    catalog_by_key,
    find_db_municipality,
    find_sheet,
    first_day_months_back,
    load_db_municipalities,
    municipality_catalog,
    parse_date,
)
from comun.xlsx import read_xlsx
from comun.rutas import MUNICIPAL_OUTPUT_DIR, MUNICIPALITIES_PATH, RAW_MUNICIPAL_DIR


DEFAULT_SOURCE_DIR = RAW_MUNICIPAL_DIR / "Enfermedades Transmitidas por Vectores"
DEFAULT_CASES_PATH = DEFAULT_SOURCE_DIR / "dengue_abierto.csv"
DEFAULT_CATALOG_PATH = DEFAULT_SOURCE_DIR / "Catálogos_Dengue.xlsx"
DEFAULT_MUNICIPALITIES_PATH = MUNICIPALITIES_PATH
DEFAULT_OUTPUT_PATH = MUNICIPAL_OUTPUT_DIR / "dengue_municipal_cases.csv"

OUTPUT_FIELDS = [
    "source_dataset",
    "fecha_actualizacion",
    "fecha_sintomas",
    "municipality_id",
    "municipio_nombre",
    "sexo",
    "edad",
    "diagnostico",
    "diseases_flagged",
]

DICTAMEN_SIGNALS = {
    "1": ("Confirmado a dengue", "DENGUE:CONFIRMED"),
    "2": ("Confirmado a chikungunya", "CHIKUNGUNYA:CONFIRMED"),
    "4": ("En estudio", "DENGUE:SUSPECTED"),
}


def main() -> None:
    parser = argparse.ArgumentParser(description="Extract municipal dengue surveillance cases.")
    parser.add_argument("--cases", type=Path, default=DEFAULT_CASES_PATH)
    parser.add_argument("--catalog", type=Path, default=DEFAULT_CATALOG_PATH)
    parser.add_argument("--municipalities", type=Path, default=DEFAULT_MUNICIPALITIES_PATH)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT_PATH)
    parser.add_argument("--reference-date", type=parse_date, default=date.today())
    parser.add_argument("--months-back", type=int, default=2)
    args = parser.parse_args()

    catalogs = load_catalogs(args.catalog)
    db_municipalities = load_db_municipalities(args.municipalities)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    cutoff_date = first_day_months_back(args.reference_date, args.months_back)

    total_rows = 0
    kept_rows = 0
    skipped_unmatched_rows = 0
    unmatched_municipalities: set[tuple[str, str]] = set()
    with args.cases.open("r", encoding="utf-8-sig", newline="") as source:
        reader = csv.DictReader(source)
        try:
            validate_case_headers(reader.fieldnames or [])
        except ValueError as exc:
            raise SystemExit(f"Error: {exc}") from None

        with args.output.open("w", encoding="utf-8", newline="") as target:
            writer = csv.DictWriter(target, fieldnames=OUTPUT_FIELDS)
            writer.writeheader()

            for row in reader:
                total_rows += 1
                extracted = extract_row(row, catalogs, db_municipalities, cutoff_date)
                if extracted is None:
                    continue
                if not extracted["municipality_id"]:
                    unmatched_municipalities.add((row["ENTIDAD_RES"].strip(), extracted["municipio_nombre"]))
                    skipped_unmatched_rows += 1
                    continue
                writer.writerow(extracted)
                kept_rows += 1

    print(f"Input rows: {total_rows}")
    print(f"Extracted rows: {kept_rows}")
    print(f"Date filter: FECHA_SIGN_SINTOMAS >= {cutoff_date.isoformat()}")
    print(f"Unmatched municipalities: {len(unmatched_municipalities)}")
    print(f"Skipped unmatched rows: {skipped_unmatched_rows}")
    for state_code, municipality_name in sorted(unmatched_municipalities):
        print(f"  - state {state_code}: {municipality_name}")
    print(f"Output: {args.output}")


def load_catalogs(catalog_path: Path) -> dict[str, dict]:
    workbook = read_xlsx(catalog_path)
    return {
        "municipalities": municipality_catalog(find_sheet(workbook, "MUNICIPIO")),
        "sex": catalog_by_key(find_sheet(workbook, "SEXO"), "CLAVE", "DESCRIPCION"),
    }


def extract_row(
    row: dict[str, str],
    catalogs: dict[str, dict],
    db_municipalities: dict[str, dict],
    cutoff_date: date,
) -> dict[str, str] | None:
    dictamen_code = row["DICTAMEN"].strip()
    signal = DICTAMEN_SIGNALS.get(dictamen_code)
    if signal is None:
        return None

    symptom_date = parse_date(row["FECHA_SIGN_SINTOMAS"])
    if symptom_date < cutoff_date:
        return None

    state_code = row["ENTIDAD_RES"].strip().zfill(2)
    municipality_code = row["MUNICIPIO_RES"].strip().zfill(3)
    source_municipality_name = catalogs["municipalities"].get((state_code, municipality_code), "")
    municipality = find_db_municipality(db_municipalities, state_code, source_municipality_name)
    diagnosis_name, diseases_flagged = signal

    return {
        "source_dataset": "DENGUE_ABIERTO",
        "fecha_actualizacion": parse_date(row["FECHA_ACTUALIZACION"]).isoformat(),
        "fecha_sintomas": symptom_date.isoformat(),
        "municipality_id": municipality["id"] if municipality else "",
        "municipio_nombre": municipality["name"] if municipality else source_municipality_name,
        "sexo": catalogs["sex"].get(row["SEXO"].strip(), ""),
        "edad": age_years_or_one(row),
        "diagnostico": diagnosis_name,
        "diseases_flagged": diseases_flagged,
    }


def validate_case_headers(fieldnames: list[str]) -> None:
    required = {
        "FECHA_ACTUALIZACION",
        "SEXO",
        "EDAD_ANOS",
        "ENTIDAD_RES",
        "MUNICIPIO_RES",
        "FECHA_SIGN_SINTOMAS",
        "DICTAMEN",
    }
    missing = sorted(required - set(fieldnames))
    if missing:
        raise ValueError(f"Missing required columns in dengue_abierto.csv: {', '.join(missing)}")


if __name__ == "__main__":
    main()
