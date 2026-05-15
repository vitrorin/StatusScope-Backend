from __future__ import annotations

import csv
import unicodedata
from datetime import date, datetime
from pathlib import Path
from typing import Iterable


DATE_FORMATS = ("%Y-%m-%d", "%d/%m/%Y")
NULL_DATE_VALUES = {"", "9999-99-99", "99/99/9999"}

SOURCE_MUNICIPALITY_ALIASES = {
    ("07", "CINTALAPA"): "CINTALAPA DE FIGUEROA",
    ("20", "JUCHITAN DE ZARAGOZA"): "HEROICA CIUDAD DE JUCHITAN DE ZARAGOZA",
    ("20", "MAGDALENA APASCO"): "MAGDALENA APAZCO",
    ("23", "SOLIDARIDAD"): "PLAYA DEL CARMEN",
}


def normalize(value: str) -> str:
    without_accents = unicodedata.normalize("NFD", value or "")
    without_marks = "".join(character for character in without_accents if unicodedata.category(character) != "Mn")
    return without_marks.upper().strip()


def parse_date(value: str) -> date:
    parsed = parse_optional_date(value)
    if parsed is None:
        raise ValueError(f"Invalid date value: {value!r}")
    return parsed


def parse_optional_date(value: str) -> date | None:
    cleaned = value.strip()
    if cleaned in NULL_DATE_VALUES:
        return None
    for date_format in DATE_FORMATS:
        try:
            return datetime.strptime(cleaned, date_format).date()
        except ValueError:
            continue
    return None


def first_day_months_back(reference_date: date, months_back: int) -> date:
    month_index = reference_date.year * 12 + reference_date.month - months_back
    year = (month_index - 1) // 12
    month = (month_index - 1) % 12 + 1
    return date(year, month, 1)


def find_sheet(workbook: dict[str, list[list[str]]], token: str) -> list[list[str]]:
    normalized_token = normalize(token)
    for name, rows in workbook.items():
        if normalized_token in normalize(name):
            return rows
    raise KeyError(f"Catalog sheet containing {token!r} was not found")


def catalog_by_key(rows: list[list[str]], key_column: str, value_column: str) -> dict[str, str]:
    header = [normalize(value) for value in rows[0]]
    key_index = header.index(normalize(key_column))
    value_index = header.index(normalize(value_column))

    catalog = {}
    for row in rows[1:]:
        if len(row) <= max(key_index, value_index):
            continue
        key = row[key_index].strip()
        if key:
            catalog[key] = row[value_index].strip()
    return catalog


def municipality_catalog(rows: list[list[str]], *, zfill_codes: bool = True) -> dict[tuple[str, str], str]:
    header = [normalize(value) for value in rows[0]]
    state_index = header.index(normalize("CLAVE_ENTIDAD"))
    municipality_index = header.index(normalize("CLAVE_MUNICIPIO"))
    name_index = header.index(normalize("MUNICIPIO"))

    catalog = {}
    for row in rows[1:]:
        if len(row) <= max(state_index, municipality_index, name_index):
            continue
        state_code = row[state_index].strip()
        municipality_code = row[municipality_index].strip()
        if zfill_codes:
            state_code = state_code.zfill(2)
            municipality_code = municipality_code.zfill(3)
        if state_code and municipality_code:
            catalog[(state_code, municipality_code)] = row[name_index].strip()
    return catalog


def load_db_municipalities(path: Path) -> dict[str, dict]:
    by_state_and_name = {}
    by_name = {}
    name_counts = {}

    with path.open("r", encoding="utf-8-sig", newline="") as source:
        reader = csv.DictReader(source)
        require_headers(reader.fieldnames or [], {"id", "state_id", "name"}, path.name)
        rows = list(reader)

    for row in rows:
        normalized_name = normalize(row["name"])
        name_counts[normalized_name] = name_counts.get(normalized_name, 0) + 1

    for row in rows:
        state_id = row["state_id"].strip()
        normalized_name = normalize(row["name"])
        municipality = {
            "id": row["id"].strip(),
            "name": row["name"].strip(),
        }
        by_state_and_name[(state_id, normalized_name)] = municipality
        if name_counts[normalized_name] == 1:
            by_name[normalized_name] = municipality

    return {
        "by_state_and_name": by_state_and_name,
        "by_name": by_name,
    }


def find_db_municipality(
    db_municipalities: dict[str, dict],
    source_state_code: str,
    source_municipality_name: str,
) -> dict[str, str] | None:
    if not source_municipality_name:
        return None

    normalized_name = normalize(source_municipality_name)
    normalized_name = SOURCE_MUNICIPALITY_ALIASES.get((source_state_code.zfill(2), normalized_name), normalized_name)
    state_id = state_id_from_entity_code(source_state_code)
    return db_municipalities["by_state_and_name"].get(
        (state_id, normalized_name)
    ) or db_municipalities["by_name"].get(normalized_name)


def state_id_from_entity_code(entity_code: str) -> str:
    return f"40000000-0000-0000-0000-{int(entity_code):012d}"


def require_headers(fieldnames: Iterable[str], required: set[str], source_name: str) -> None:
    missing = sorted(required - set(fieldnames))
    if missing:
        raise ValueError(f"Missing required columns in {source_name}: {', '.join(missing)}")


def lookup(catalog: dict[str, str], key: str) -> str:
    return catalog.get(key.strip(), "")


def age_years_or_one(row: dict[str, str], field: str = "EDAD_ANOS") -> str:
    years = row[field].strip()
    if years and years != "0":
        return years
    return "1"


def write_csv(path: Path, fieldnames: list[str], rows: Iterable[dict[str, object]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as target:
        writer = csv.DictWriter(target, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows)
