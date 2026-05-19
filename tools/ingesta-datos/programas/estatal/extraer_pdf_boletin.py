from __future__ import annotations

import argparse
import csv
import re
import site
import sys
from functools import lru_cache
from pathlib import Path

VENDOR_DIR = Path(__file__).resolve().parents[1] / "vendor"
if VENDOR_DIR.exists():
    sys.path.insert(0, str(VENDOR_DIR))

try:
    from pypdf import PdfReader
except ModuleNotFoundError:
    site.addsitedir(site.getusersitepackages())
    from pypdf import PdfReader

from comun.outbreaks import normalize
from comun.rutas import RAW_STATE_DIR, STATE_OUTPUT_DIR


DEFAULT_PDF_PATH = RAW_STATE_DIR / "boletin nacional.pdf"
DEFAULT_OUTPUT_CSV_PATH = STATE_OUTPUT_DIR / "state_disease_cases.csv"

STATE_NAMES = [
    "Aguascalientes",
    "Baja California",
    "Baja California Sur",
    "Campeche",
    "Coahuila",
    "Colima",
    "Chiapas",
    "Chihuahua",
    "Ciudad de México",
    "Durango",
    "Guanajuato",
    "Guerrero",
    "Hidalgo",
    "Jalisco",
    "México",
    "Michoacán",
    "Morelos",
    "Nayarit",
    "Nuevo León",
    "Oaxaca",
    "Puebla",
    "Querétaro",
    "Quintana Roo",
    "San Luis Potosí",
    "Sinaloa",
    "Sonora",
    "Tabasco",
    "Tamaulipas",
    "Tlaxcala",
    "Veracruz",
    "Yucatán",
    "Zacatecas",
]

FIELDNAMES = [
    "enfermedad",
    "estado",
    "casos_hombres_2026",
    "casos_mujeres_2026",
    "total_casos_2026",
]

SKIP_HEADER_TOKENS = {
    "VIGILANCIA",
    "EPIDEMIOLOGICA",
    "SEMANA",
    "CUADRO",
    "CASOS",
    "ENTIDAD",
    "FEDERATIVA",
    "CIE",
    "REV",
    "ACUM",
    "SEM",
    "FUENTE",
    "SINAVE",
    "SALUD",
    "INFORMACION",
    "PRELIMINAR",
    "HOMBRE",
    "MUJER",
}

NON_STANDARD_HM_KEYWORDS = [
    "HIPERPLASIA DE PROSTATA",
    "EMBARAZO",
    "TUMOR MALIGNO DE LA MAMA",
    "CUELLO DEL UTERO",
    "DISPLASIA CERVICAL",
    "TUMOR MALIGNO DE LA PROSTATA",
    "CUERPO DEL UTERO",
    "OVARIO",
]


def main() -> None:
    parser = argparse.ArgumentParser(description="Extract state-level disease cases from the national bulletin PDF.")
    parser.add_argument("--pdf", type=Path, default=DEFAULT_PDF_PATH)
    parser.add_argument("--output-csv", type=Path, default=DEFAULT_OUTPUT_CSV_PATH)
    parser.add_argument("--include-zero", action="store_true")
    args = parser.parse_args()

    rows, warnings = extract_pdf(args.pdf, include_zero=args.include_zero)
    args.output_csv.parent.mkdir(parents=True, exist_ok=True)
    write_csv(args.output_csv, rows)

    print(f"Extracted rows: {len(rows)}")
    print(f"Warnings: {len(warnings)}")
    for warning in warnings[:40]:
        print(f"  - {warning}")
    if len(warnings) > 40:
        print(f"  ... {len(warnings) - 40} more")
    print(f"CSV output: {args.output_csv}")


def extract_pdf(pdf_path: Path, include_zero: bool) -> tuple[list[dict[str, object]], list[str]]:
    reader = PdfReader(str(pdf_path))
    rows: list[dict[str, object]] = []
    warnings: list[str] = []

    for page_index, page in enumerate(reader.pages, start=1):
        text = page.extract_text(extraction_mode="layout") or ""
        state_lines = extract_state_lines(text)
        if len(state_lines) < 20:
            continue
        if "Estadios Clínicos" in text or "Estadios Clinicos" in text:
            rows.extend(extract_hiv_stage_rows(page_index, state_lines, include_zero, warnings))
            continue
        if not is_standard_hm_table(text):
            continue

        values_by_state = {}
        possible_group_counts = []
        for state, value_text in state_lines:
            tokens = tokenize_values(value_text)
            possible_group_counts.extend(
                group_count for group_count in range(1, 5) if parse_values_strict(tokens, group_count * 4)
            )

        if not possible_group_counts:
            warnings.append(f"page {page_index}: no se pudo determinar cantidad de enfermedades")
            continue
        group_count = max(possible_group_counts, key=possible_group_counts.count)

        diseases = extract_disease_names(page, group_count)
        if len(diseases) != group_count:
            warnings.append(f"page {page_index}: nombres detectados {diseases}, grupos {group_count}")
            diseases = [f"PDF page {page_index} disease {index + 1}" for index in range(group_count)]

        for state, value_text in state_lines:
            tokens = tokenize_values(value_text)
            values = parse_values(tokens, group_count * 4)
            if values is None:
                warnings.append(f"page {page_index}: no se pudo parsear fila de {state}: {value_text}")
                continue

            for group_index, disease in enumerate(diseases):
                group_values = values[group_index * 4 : (group_index + 1) * 4]
                male_cases = group_values[1] or 0
                female_cases = group_values[2] or 0
                total_cases = male_cases + female_cases
                if total_cases == 0 and not include_zero:
                    continue
                rows.append(
                    {
                        "enfermedad": disease,
                        "estado": state,
                        "casos_hombres_2026": male_cases,
                        "casos_mujeres_2026": female_cases,
                        "total_casos_2026": total_cases,
                    }
                )

    return rows, warnings


def is_standard_hm_table(text: str) -> bool:
    normalized = normalize(text)
    if any(keyword in normalized for keyword in NON_STANDARD_HM_KEYWORDS):
        return False

    header_lines = [line.strip() for line in text.splitlines()[:25] if line.strip()]
    header_text = " ".join(header_lines)
    return bool(re.search(r"\bH\s*M\b|\bH\s+H\b|\bM\s+M\b|HM", header_text))


def extract_state_lines(text: str) -> list[tuple[str, str]]:
    lines = []
    ordered_states = sorted(STATE_NAMES, key=len, reverse=True)
    for raw_line in text.splitlines():
        line = raw_line.strip()
        if not line or line.startswith("TOTAL") or line.startswith("FUENTE") or line.startswith("§"):
            continue
        for state in ordered_states:
            if line.startswith(state):
                value_text = line[len(state) :].strip()
                if value_text:
                    lines.append((state, value_text))
                break
    return lines


def tokenize_values(value_text: str) -> tuple[str, ...]:
    return tuple(re.findall(r"\d+|-", value_text))


@lru_cache(maxsize=None)
def parse_values(tokens: tuple[str, ...], expected_count: int) -> tuple[int | None, ...] | None:
    candidates = parse_value_candidates(tokens, expected_count, strict=False)
    if not candidates:
        return None
    return min(candidates, key=score_standard_hm_values)


@lru_cache(maxsize=None)
def parse_value_candidates(
    tokens: tuple[str, ...],
    expected_count: int,
    *,
    strict: bool,
) -> tuple[tuple[int | None, ...], ...]:
    if expected_count < 0:
        return tuple()
    if not tokens:
        return (tuple(),) if expected_count == 0 else tuple()
    if expected_count == 0:
        return tuple()

    first = tokens[0]
    candidates: list[tuple[int | None, ...]] = []
    if first == "-":
        for rest in parse_value_candidates(tokens[1:], expected_count - 1, strict=strict):
            candidates.append((None,) + rest)
        if not strict:
            candidates.extend(parse_value_candidates(tokens[1:], expected_count, strict=strict))
        return tuple(candidates)

    if len(tokens) >= 2 and tokens[1].isdigit() and len(tokens[1]) == 3:
        combined = int(first + tokens[1])
        for rest in parse_value_candidates(tokens[2:], expected_count - 1, strict=strict):
            candidates.append((combined,) + rest)

    for rest in parse_value_candidates(tokens[1:], expected_count - 1, strict=strict):
        candidates.append((int(first),) + rest)
    return tuple(candidates)


def score_standard_hm_values(values: tuple[int | None, ...]) -> tuple[int, int, int]:
    score = 0
    large_values = 0
    total_gap = 0

    for group_start in range(0, len(values), 4):
        group = values[group_start : group_start + 4]
        if len(group) != 4:
            break

        weekly, male_cases, female_cases, prior_year_cases = (value or 0 for value in group)
        current_total = male_cases + female_cases
        if prior_year_cases:
            gap = abs(prior_year_cases - current_total)
            total_gap += gap
            if current_total and gap > max(5, int(current_total * 0.2)):
                score += 25
            if male_cases > prior_year_cases:
                score += 1_000
            if female_cases > prior_year_cases:
                score += 1_000
        elif current_total:
            score += 10

        if weekly > current_total and current_total:
            score += 250
        smaller_sex_count = min(male_cases, female_cases)
        larger_sex_count = max(male_cases, female_cases)
        if smaller_sex_count and larger_sex_count >= 10_000 and larger_sex_count > smaller_sex_count * 20:
            score += 1_500
        large_values += sum(1 for value in group if value and value >= 100_000)

    return (score, large_values, total_gap)


@lru_cache(maxsize=None)
def parse_values_strict(tokens: tuple[str, ...], expected_count: int) -> tuple[int | None, ...] | None:
    candidates = parse_value_candidates(tokens, expected_count, strict=True)
    if not candidates:
        return None
    return min(candidates, key=score_standard_hm_values)


def extract_hiv_stage_rows(
    page_index: int,
    state_lines: list[tuple[str, str]],
    include_zero: bool,
    warnings: list[str],
) -> list[dict[str, object]]:
    rows = []
    disease = "Infección por el virus de la inmunodeficiencia humana"
    for state, value_text in state_lines:
        tokens = tokenize_values(value_text)
        values = parse_values(tokens, 11)
        if values is None:
            warnings.append(f"page {page_index}: no se pudo parsear fila VIH de {state}: {value_text}")
            continue
        male_cases = sum(value or 0 for value in values[1:5])
        female_cases = sum(value or 0 for value in values[5:9])
        total_cases = male_cases + female_cases
        if total_cases == 0 and not include_zero:
            continue
        rows.append(
            {
                "enfermedad": disease,
                "estado": state,
                "casos_hombres_2026": male_cases,
                "casos_mujeres_2026": female_cases,
                "total_casos_2026": total_cases,
            }
        )
    return rows


def extract_disease_names(page, group_count: int) -> list[str]:
    items = collect_page_items(page)
    candidates = [
        item
        for item in items
        if 2300 <= item["y"] <= 4300
        and 2500 <= item["x"] <= 12500
        and is_disease_name_candidate(item["text"])
    ]
    if len(candidates) < group_count:
        return []

    clusters = cluster_items_by_x(candidates, group_count)
    names = []
    for cluster in clusters:
        parts = [item["text"].replace("§", "").replace("&", "").strip() for item in sorted(cluster, key=lambda it: (it["y"], it["x"]))]
        name = clean_disease_name(" ".join(part for part in parts if part))
        if name:
            names.append(name)
    return names


def collect_page_items(page) -> list[dict[str, object]]:
    items = []

    def visitor(text, cm, tm, font_dict, font_size):
        value = (text or "").strip()
        if value:
            items.append({"x": float(tm[4]), "y": float(tm[5]), "text": value.replace("\n", " ")})

    page.extract_text(visitor_text=visitor)
    return items


def is_disease_name_candidate(text: str) -> bool:
    cleaned = text.strip()
    normalized = normalize(cleaned)
    if len(cleaned) <= 1:
        return False
    if is_code_like(cleaned):
        return False
    if "CIE" in normalized or "REV" in normalized:
        return False
    if normalized in {"H", "M", "HM", "H M", "§", "&"}:
        return False
    if any(token in normalized.split() for token in SKIP_HEADER_TOKENS):
        return False
    if "CASOS POR" in normalized or "HASTA LA" in normalized:
        return False
    return bool(re.search(r"[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]", cleaned))


def is_code_like(value: str) -> bool:
    cleaned = value.strip()
    if re.fullmatch(r"[A-Z]\d[\dA-Z.,\- ]*", cleaned):
        return True
    if re.fullmatch(r"[A-Z]\d{2}(?:\.\d+)?(?:\s*-\s*[A-Z]?\d+(?:\.\d+)?)?", cleaned):
        return True
    if re.fullmatch(r"[A-Z]\d{2}(?:-[A-Z]?\d{2})?(?:,\s*[A-Z]?\d{2}(?:\.\d+)?)*", cleaned):
        return True
    return False


def cluster_items_by_x(items: list[dict[str, object]], cluster_count: int) -> list[list[dict[str, object]]]:
    ordered = sorted(items, key=lambda item: item["x"])
    centers = [ordered[round(index * (len(ordered) - 1) / max(cluster_count - 1, 1))]["x"] for index in range(cluster_count)]

    for _ in range(20):
        clusters = [[] for _ in centers]
        for item in ordered:
            nearest = min(range(len(centers)), key=lambda index: abs(item["x"] - centers[index]))
            clusters[nearest].append(item)
        new_centers = [
            sum(item["x"] for item in cluster) / len(cluster) if cluster else centers[index]
            for index, cluster in enumerate(clusters)
        ]
        if all(abs(new_centers[index] - centers[index]) < 1 for index in range(len(centers))):
            break
        centers = new_centers

    return [cluster for _, cluster in sorted(zip(centers, clusters), key=lambda pair: pair[0])]


def clean_disease_name(value: str) -> str:
    cleaned = re.sub(r"\s+", " ", value).strip(" -")
    return cleaned


def write_csv(path: Path, rows: list[dict[str, object]]) -> None:
    with path.open("w", encoding="utf-8", newline="") as target:
        writer = csv.DictWriter(target, fieldnames=FIELDNAMES)
        writer.writeheader()
        writer.writerows(rows)


if __name__ == "__main__":
    main()
