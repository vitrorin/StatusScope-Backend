from __future__ import annotations

import argparse
import csv
from datetime import date
from pathlib import Path

from comun.outbreaks import (
    catalog_by_key,
    find_db_municipality,
    find_sheet,
    first_day_months_back,
    load_db_municipalities,
    lookup,
    municipality_catalog,
    normalize,
    parse_date,
)
from comun.xlsx import read_xlsx
from comun.rutas import MUNICIPAL_OUTPUT_DIR, MUNICIPALITIES_PATH, RAW_MUNICIPAL_DIR


DEFAULT_SOURCE_DIR = RAW_MUNICIPAL_DIR / "Enfermedades Respiratorias"
DEFAULT_CASES_PATH = DEFAULT_SOURCE_DIR / "COVID19MEXICO.csv"
DEFAULT_CATALOG_PATH = DEFAULT_SOURCE_DIR / "240708 Catalogos.xlsx"
DEFAULT_MUNICIPALITIES_PATH = MUNICIPALITIES_PATH
DEFAULT_OUTPUT_PATH = MUNICIPAL_OUTPUT_DIR / "respiratory_municipal_cases.csv"

OUTPUT_FIELDS = [
    "source_dataset",
    "fecha_actualizacion",
    "fecha_sintomas",
    "municipality_id",
    "municipio_nombre",
    "sexo",
    "edad",
    "resultado_pcr",
    "covid_signal_status",
    "flu_signal_status",
    "diseases_flagged",
]


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Extract municipal respiratory surveillance cases from COVID19MEXICO.csv."
    )
    parser.add_argument("--cases", type=Path, default=DEFAULT_CASES_PATH)
    parser.add_argument("--catalog", type=Path, default=DEFAULT_CATALOG_PATH)
    parser.add_argument("--municipalities", type=Path, default=DEFAULT_MUNICIPALITIES_PATH)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT_PATH)
    parser.add_argument(
        "--location-source",
        choices=("diagnosis", "residence"),
        default="residence",
        help="Use residence municipality as the municipal outbreak origin signal.",
    )
    parser.add_argument("--reference-date", type=parse_date, default=date.today())
    parser.add_argument("--months-back", type=int, default=2)
    parser.add_argument("--date-field", default="FECHA_SINTOMAS")
    args = parser.parse_args()

    catalogs = load_catalogs(args.catalog)
    db_municipalities = load_db_municipalities(args.municipalities)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    cutoff_date = first_day_months_back(args.reference_date, args.months_back)

    total_rows = 0
    kept_rows = 0
    unmatched_municipalities: set[tuple[str, str]] = set()
    with args.cases.open("r", encoding="utf-8-sig", newline="") as source:
        reader = csv.DictReader(source)
        try:
            validate_case_headers(reader.fieldnames or [], args.location_source, args.date_field)
        except ValueError as exc:
            raise SystemExit(f"Error: {exc}") from None

        with args.output.open("w", encoding="utf-8", newline="") as target:
            writer = csv.DictWriter(target, fieldnames=OUTPUT_FIELDS)
            writer.writeheader()

            for row in reader:
                total_rows += 1
                extracted = extract_row(
                    row,
                    catalogs,
                    db_municipalities,
                    args.location_source,
                    args.date_field,
                    cutoff_date,
                )
                if extracted is None:
                    continue
                if not extracted["municipality_id"]:
                    unmatched_municipalities.add((row["ENTIDAD_RES"].strip(), extracted["municipio_nombre"]))
                writer.writerow(extracted)
                kept_rows += 1

    print(f"Input rows: {total_rows}")
    print(f"Extracted rows: {kept_rows}")
    print(f"Date filter: {args.date_field} >= {cutoff_date.isoformat()}")
    print(f"Unmatched municipalities: {len(unmatched_municipalities)}")
    for state_code, municipality_name in sorted(unmatched_municipalities):
        print(f"  - state {state_code}: {municipality_name}")
    print(f"Output: {args.output}")


def load_catalogs(catalog_path: Path) -> dict[str, dict]:
    workbook = read_xlsx(catalog_path)
    return {
        "entities": catalog_by_key(find_sheet(workbook, "ENTIDADES"), "CLAVE_ENTIDAD", "ENTIDAD_FEDERATIVA"),
        "municipalities": municipality_catalog(find_sheet(workbook, "MUNICIPIOS"), zfill_codes=False),
        "sex": catalog_by_key(find_sheet(workbook, "SEXO"), "CLAVE", "DESCRIPCION"),
        "resultado_pcr": catalog_by_key(find_sheet(workbook, "RESULTADO_PCR"), "CLAVE", "DESCRIPCION"),
        "covid_classification": catalog_by_key(
            find_sheet(workbook, "CLASIFICACION_FINAL_COVID"),
            "CLAVE",
            "CLASIFICACION",
        ),
        "flu_classification": catalog_by_key(
            find_sheet(workbook, "CLASIFICACION_FINAL_FLU"),
            "CLAVE",
            "CLASIFICACION",
        ),
    }


def extract_row(
    row: dict[str, str],
    catalogs: dict[str, dict],
    db_municipalities: dict[str, dict],
    location_source: str,
    date_field: str,
    cutoff_date: date,
) -> dict[str, str] | None:
    case_date = parse_date(row[date_field])
    if case_date < cutoff_date:
        return None

    covid_signal_status = signal_status(lookup(catalogs["covid_classification"], row["CLASIFICACION_FINAL_COVID"]))
    flu_signal_status = signal_status(lookup(catalogs["flu_classification"], row["CLASIFICACION_FINAL_FLU"]))
    if not covid_signal_status and not flu_signal_status:
        return None

    diseases_flagged = []
    if covid_signal_status:
        diseases_flagged.append(f"COVID-19:{covid_signal_status}")
    if flu_signal_status:
        diseases_flagged.append(f"INFLUENZA:{flu_signal_status}")

    state_field = "ENTIDAD_UM" if location_source == "diagnosis" else "ENTIDAD_RES"
    municipality_field = "MUNICIPIO_UM" if location_source == "diagnosis" else "MUNICIPIO_RES"
    state_code = row[state_field].strip()
    municipality_code = row[municipality_field].strip()
    source_municipality_name = catalogs["municipalities"].get((state_code, municipality_code), "")
    municipality = find_db_municipality(db_municipalities, state_code, source_municipality_name)

    return {
        "source_dataset": "COVID19MEXICO",
        "fecha_actualizacion": row["FECHA_ACTUALIZACION"].strip(),
        "fecha_sintomas": row["FECHA_SINTOMAS"].strip(),
        "municipality_id": municipality["id"] if municipality else "",
        "municipio_nombre": municipality["name"] if municipality else source_municipality_name,
        "sexo": lookup(catalogs["sex"], row["SEXO"]),
        "edad": row["EDAD"].strip(),
        "resultado_pcr": lookup(catalogs["resultado_pcr"], row["RESULTADO_PCR"]),
        "covid_signal_status": covid_signal_status,
        "flu_signal_status": flu_signal_status,
        "diseases_flagged": "|".join(diseases_flagged),
    }


def signal_status(classification: str) -> str:
    normalized = normalize(classification)
    if "CONFIRMADO" in normalized:
        return "CONFIRMED"
    if "SOSPECHOSO" in normalized:
        return "SUSPECTED"
    return ""


def validate_case_headers(fieldnames: list[str], location_source: str, date_field: str) -> None:
    required = {
        "FECHA_ACTUALIZACION",
        "SEXO",
        "EDAD",
        "RESULTADO_PCR",
        "CLASIFICACION_FINAL_COVID",
        "CLASIFICACION_FINAL_FLU",
        "FECHA_SINTOMAS",
        date_field,
    }
    if location_source == "diagnosis":
        required.update({"MUNICIPIO_UM"})
    else:
        required.update({"ENTIDAD_RES", "MUNICIPIO_RES"})

    missing = sorted(required - set(fieldnames))
    if missing:
        if location_source == "diagnosis" and "MUNICIPIO_UM" in missing:
            raise ValueError(
                "COVID19MEXICO.csv does not include MUNICIPIO_UM, so it cannot produce "
                "municipal diagnosis-location data without using residence municipality."
            )
        raise ValueError(f"Missing required columns in COVID19MEXICO.csv: {', '.join(missing)}")


if __name__ == "__main__":
    main()
