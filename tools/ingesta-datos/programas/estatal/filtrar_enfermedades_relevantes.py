from __future__ import annotations

import argparse
import csv
from difflib import get_close_matches
from pathlib import Path

from comun.outbreaks import normalize
from comun.rutas import BACKEND_DATA_DIR, STATE_OUTPUT_DIR


DEFAULT_STATE_CASES_PATH = STATE_OUTPUT_DIR / "state_disease_cases.csv"
DEFAULT_DB_DISEASES_PATH = BACKEND_DATA_DIR / "diseases" / "diseases_and_specialties.csv"
DEFAULT_FILTERED_CSV_PATH = STATE_OUTPUT_DIR / "state_outbreak_relevant_cases.csv"
DEFAULT_COMPARISON_CSV_PATH = STATE_OUTPUT_DIR / "state_disease_catalog_comparison.csv"

CASE_FIELDS = [
    "scope",
    "estado",
    "disease_name",
    "case_count",
    "confirmation_status",
    "status",
]

COMPARISON_FIELDS = [
    "boletin_enfermedad",
    "clasificacion",
    "en_base_de_datos",
    "db_match",
    "match_tipo",
    "sugerencia",
]

OUTBREAK_RELEVANT_DISEASES = {
    "ABSCESO HEPATICO AMEBIANO",
    "AMEBIASIS INTESTINAL",
    "ASCARIASIS",
    "BRUCELOSIS",
    "CANDIDIASIS UROGENITAL",
    "CHANCRO BLANDO",
    "CISTICERCOSIS",
    "CONJUNTIVITIS",
    "CONJUNTIVITIS EPIDEMICA AGUDA HEMORRAGICA",
    "COVID 19",
    "DENGUE CON SIGNOS DE ALARMA",
    "DENGUE GRAVE",
    "DENGUE NO GRAVE",
    "ENFERMEDAD FEBRIL EXANTEMATICA",
    "ENFERMEDAD INVASIVA POR NEUMOCOCO",
    "ENFERMEDAD POR VIRUS CHIKUNGUNYA",
    "ENFERMEDADES INFECCIOSAS INTESTINALES",
    "ENTERITIS DEBIDA A ROTAVIRUS",
    "ENTEROBIASIS",
    "ERISIPELA",
    "ESCABIOSIS",
    "ESCARLATINA",
    "FARINGITIS Y AMIGDALITIS ESTREPTOCOCICAS",
    "FIEBRE MANCHADA",
    "FIEBRE PARATIFOIDEA",
    "FIEBRE TIFOIDEA",
    "GIARDIASIS",
    "HEPATITIS VIRICA A",
    "HEPATITIS VIRICA B",
    "HEPATITIS VIRICA C",
    "HERPES GENITAL",
    "INFECCIONES INTESTINALES POR OTROS ORGANISMOS Y LAS MAL DEFINIDAS",
    "INFECCIONES INVASIVAS POR HAEMOPHILUS INFLUENZAE",
    "INFECCIONES RESPIRATORIAS AGUDAS",
    "INFECCION ASOCIADA A LA S/C",
    "INFECCION DE VIAS URINARIAS",
    "INFECCION GONOCOCICA DEL TRACTO GENITOURINARIO",
    "INFECCION POR EL VIRUS DE LA INMUNODEFICIENCIA HUMANA",
    "INFECCION POR VIRUS DE PAPILOMA HUMANO",
    "INFLUENZA",
    "INTOXICACION ALIMENTARIA BACTERIANA",
    "LEISHMANIASIS CUTANEA",
    "LEPRA°",
    "LEPTOSPIROSIS",
    "LINFOGRANULOMA VENEREO POR CLAMIDIAS",
    "MAL DEL PINTO",
    "MENINGITIS MENINGOCOCICA",
    "MENINGITIS TUBERCULOSA",
    "NEUMONIAS Y BRONCONEUMONIAS",
    "OTITIS MEDIA AGUDA",
    "OTRAS RICKETTSIOSIS",
    "OTRAS HELMINTIASIS EXCEPTO B73 Y B75",
    "OTRAS HEPATITIS VIRICAS",
    "OTRAS INFECCIONES INTESTINALES DEBIDAS A PROTOZOARIOS",
    "OTRAS SALMONELOSIS",
    "PALUDISMO POR PLASMODIUM VIVAX",
    "PAROTIDITIS INFECCIOSA",
    "PARALISIS FLACIDA AGUDA (MENORES DE 15 ANOS)",
    "SHIGELOSIS",
    "SIFILIS ADQUIRIDA",
    "SIFILIS CONGENITA",
    "SINDROME COQUELUCHOIDE",
    "SINDROME MENINGEO G00-G03 EXCEPTO G00.0 Y G00.1",
    "TENIASIS",
    "TOS FERINA",
    "TOXOPLASMOSIS",
    "TRACOMA",
    "TRICOMONIASIS UROGENITAL",
    "TRIPANOSOMIASIS AMERICANA (ENFERMEDAD DE CHAGAS) AGUDA",
    "TRIPANOSOMIASIS AMERICANA (ENFERMEDAD DE CHAGAS) CRONICA",
    "TRIQUINOSIS",
    "TUBERCULOSIS OTRAS FORMAS",
    "TUBERCULOSIS RESPIRATORIA",
    "TETANOS",
    "VARICELA",
    "VIRUELA SIMICA",
    "VULVOVAGINITIS",
    "^MIASIS",
}

EXACT_EQUIVALENT_ALIASES = {
    "CANDIDIASIS UROGENITAL": "Vaginal yeast infection",
    "CHANCRO BLANDO": "Chancroid",
    "CISTICERCOSIS": "Cysticercosis",
    "CONJUNTIVITIS": "Conjunctivitis",
    "COVID 19": "COVID-19",
    "DENGUE GRAVE": "Dengue Severe",
    "ENFERMEDAD POR VIRUS CHIKUNGUNYA": "Chikungunya",
    "ENTEROBIASIS": "Pinworm infection",
    "BRUCELOSIS": "Brucellosis",
    "ESCABIOSIS": "Scabies",
    "ESCARLATINA": "Scarlet fever",
    "ERISIPELA": "Erysipelas",
    "FARINGITIS Y AMIGDALITIS ESTREPTOCOCICAS": "Strep throat",
    "FIEBRE MANCHADA": "Rocky Mountain spotted fever",
    "FIEBRE PARATIFOIDEA": "Paratyphoid fever",
    "FIEBRE TIFOIDEA": "Typhoid fever",
    "HEPATITIS VIRICA A": "Hepatitis A",
    "HEPATITIS VIRICA B": "Hepatitis B",
    "HEPATITIS VIRICA C": "Hepatitis C",
    "HERPES GENITAL": "Genital herpes",
    "INFECCION ASOCIADA A LA S/C": "Healthcare-associated infection",
    "INFECCION DE VIAS URINARIAS": "Urethritis",
    "INFECCION GONOCOCICA DEL TRACTO GENITOURINARIO": "Gonorrhea",
    "INFECCION POR EL VIRUS DE LA INMUNODEFICIENCIA HUMANA": "HIV/AIDS",
    "INFECCION POR VIRUS DE PAPILOMA HUMANO": "HPV",
    "INFLUENZA": "Influenza",
    "LEISHMANIASIS CUTANEA": "Leishmaniasis",
    "LEPRA°": "Leprosy",
    "LEPTOSPIROSIS": "Leptospirosis",
    "LINFOGRANULOMA VENEREO POR CLAMIDIAS": "Lymphogranuloma venereum",
    "MAL DEL PINTO": "Pinto disease",
    "OTITIS MEDIA AGUDA": "Acute otitis media",
    "OTRAS RICKETTSIOSIS": "Rocky Mountain spotted fever",
    "OTRAS HEPATITIS VIRICAS": "Other viral hepatitis",
    "PALUDISMO POR PLASMODIUM VIVAX": "Malaria",
    "PAROTIDITIS INFECCIOSA": "Mumps",
    "SIFILIS ADQUIRIDA": "Syphilis",
    "SIFILIS CONGENITA": "Syphilis",
    "TENIASIS": "Taeniasis",
    "TETANOS": "Tetanus",
    "TOS FERINA": "Whooping cough",
    "TOXOPLASMOSIS": "Toxoplasmosis",
    "TRACOMA": "Trachoma",
    "TRICOMONIASIS UROGENITAL": "Trichomonas infection",
    "TRIQUINOSIS": "Trichinosis",
    "VARICELA": "Chickenpox",
    "VIRUELA SIMICA": "Mpox",
    "VULVOVAGINITIS": "Vaginitis",
    "^MIASIS": "Myiasis",
}

PARTIAL_CATEGORY_SUGGESTIONS = {
    "ABSCESO HEPATICO AMEBIANO": "Infectious gastroenteritis",
    "AMEBIASIS INTESTINAL": "Infectious gastroenteritis",
    "ASCARIASIS": "Parasitic disease",
    "CONJUNTIVITIS EPIDEMICA AGUDA HEMORRAGICA": "Conjunctivitis due to virus",
    "DENGUE CON SIGNOS DE ALARMA": "Dengue fever",
    "DENGUE NO GRAVE": "Dengue fever",
    "ENFERMEDAD FEBRIL EXANTEMATICA": "Viral exanthem",
    "ENFERMEDAD INVASIVA POR NEUMOCOCO": "Pneumonia",
    "ENFERMEDADES INFECCIOSAS INTESTINALES": "Infectious gastroenteritis",
    "ENTERITIS DEBIDA A ROTAVIRUS": "Infectious gastroenteritis",
    "GIARDIASIS": "Infectious gastroenteritis",
    "INFECCIONES INTESTINALES POR OTROS ORGANISMOS Y LAS MAL DEFINIDAS": "Infectious gastroenteritis",
    "INFECCIONES INVASIVAS POR HAEMOPHILUS INFLUENZAE": "Meningitis",
    "INFECCIONES RESPIRATORIAS AGUDAS": "Common cold",
    "INTOXICACION ALIMENTARIA BACTERIANA": "Infectious gastroenteritis",
    "MENINGITIS MENINGOCOCICA": "Meningitis",
    "MENINGITIS TUBERCULOSA": "Tuberculosis",
    "NEUMONIAS Y BRONCONEUMONIAS": "Pneumonia",
    "OTRAS HELMINTIASIS EXCEPTO B73 Y B75": "Parasitic disease",
    "OTRAS INFECCIONES INTESTINALES DEBIDAS A PROTOZOARIOS": "Infectious gastroenteritis",
    "OTRAS SALMONELOSIS": "Infectious gastroenteritis",
    "PARALISIS FLACIDA AGUDA (MENORES DE 15 ANOS)": "Viral exanthem",
    "SHIGELOSIS": "Infectious gastroenteritis",
    "SINDROME COQUELUCHOIDE": "Whooping cough",
    "SINDROME MENINGEO G00-G03 EXCEPTO G00.0 Y G00.1": "Meningitis",
    "TRIPANOSOMIASIS AMERICANA (ENFERMEDAD DE CHAGAS) AGUDA": "Chagas disease",
    "TRIPANOSOMIASIS AMERICANA (ENFERMEDAD DE CHAGAS) CRONICA": "Chagas disease",
    "TUBERCULOSIS OTRAS FORMAS": "Tuberculosis",
    "TUBERCULOSIS RESPIRATORIA": "Tuberculosis",
}


def main() -> None:
    parser = argparse.ArgumentParser(description="Filter state bulletin diseases relevant to infectious outbreaks.")
    parser.add_argument("--state-cases", type=Path, default=DEFAULT_STATE_CASES_PATH)
    parser.add_argument("--db-diseases", type=Path, default=DEFAULT_DB_DISEASES_PATH)
    parser.add_argument("--filtered-csv", type=Path, default=DEFAULT_FILTERED_CSV_PATH)
    parser.add_argument("--comparison-csv", type=Path, default=DEFAULT_COMPARISON_CSV_PATH)
    args = parser.parse_args()

    state_rows = read_csv(args.state_cases)
    db_diseases = read_db_diseases(args.db_diseases)

    filtered_rows = [row for row in state_rows if normalize(row["enfermedad"]) in OUTBREAK_RELEVANT_DISEASES]
    comparison_rows = compare_diseases(filtered_rows, db_diseases)
    grouped_rows = group_state_outbreak_rows(filtered_rows, db_diseases)

    write_csv(args.filtered_csv, CASE_FIELDS, grouped_rows)
    write_csv(args.comparison_csv, COMPARISON_FIELDS, comparison_rows)

    matched = sum(1 for row in comparison_rows if row["en_base_de_datos"] == "SI")
    partial = sum(1 for row in comparison_rows if row["en_base_de_datos"] == "PARCIAL")
    missing = sum(1 for row in comparison_rows if row["en_base_de_datos"] == "NO")
    print(f"Original rows: {len(state_rows)}")
    print(f"Filtered rows: {len(filtered_rows)}")
    print(f"Grouped state outbreak signals: {len(grouped_rows)}")
    print(f"Filtered diseases: {len(comparison_rows)}")
    print(f"Matched in DB: {matched}")
    print(f"Partial/category matches: {partial}")
    print(f"Missing in DB: {missing}")
    print(f"Filtered CSV: {args.filtered_csv}")
    print(f"Comparison CSV: {args.comparison_csv}")


def read_csv(path: Path) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8", newline="") as source:
        return list(csv.DictReader(source))


def read_db_diseases(path: Path) -> dict[str, str]:
    with path.open("r", encoding="utf-8-sig", newline="") as source:
        reader = csv.DictReader(source)
        return {normalize(row["prognosis"]): row["prognosis"] for row in reader}


def compare_diseases(filtered_rows: list[dict[str, str]], db_diseases: dict[str, str]) -> list[dict[str, str]]:
    disease_names = sorted({row["enfermedad"] for row in filtered_rows}, key=normalize)
    comparison_rows = []
    normalized_db_names = list(db_diseases.keys())

    for disease_name in disease_names:
        normalized_name = normalize(disease_name)
        match, status, match_kind = find_match(normalized_name, db_diseases)
        close = ""
        if match is None and status == "NO":
            close_matches = get_close_matches(normalized_name, normalized_db_names, n=1, cutoff=0.72)
            if close_matches:
                close = db_diseases[close_matches[0]]
        suggestion = ""
        if status == "PARCIAL" and match:
            suggestion = f"Categoria relacionada existente: {match}. Requiere enfermedad especifica."
        elif status == "NO":
            suggestion = close

        comparison_rows.append(
            {
                "boletin_enfermedad": disease_name,
                "clasificacion": "infecciosa_o_brote",
                "en_base_de_datos": status,
                "db_match": match or "",
                "match_tipo": match_kind,
                "sugerencia": suggestion,
            }
        )
    return comparison_rows


def find_match(normalized_name: str, db_diseases: dict[str, str]) -> tuple[str | None, str, str]:
    if normalized_name in db_diseases:
        return db_diseases[normalized_name], "SI", "exacto"

    alias = EXACT_EQUIVALENT_ALIASES.get(normalized_name)
    if alias and normalize(alias) in db_diseases:
        return db_diseases[normalize(alias)], "SI", "equivalente"

    category = PARTIAL_CATEGORY_SUGGESTIONS.get(normalized_name)
    if category and normalize(category) in db_diseases:
        return db_diseases[normalize(category)], "PARCIAL", "categoria_amplia"

    return None, "NO", ""


def group_state_outbreak_rows(
    filtered_rows: list[dict[str, str]],
    db_diseases: dict[str, str],
) -> list[dict[str, str]]:
    grouped: dict[tuple[str, str, str], dict[str, object]] = {}

    for row in filtered_rows:
        source_disease = row["enfermedad"].strip()
        match, status, match_kind = find_match(normalize(source_disease), db_diseases)
        disease_name = match or source_disease
        grouping_type = match_kind or "sin_match"
        key = (row["estado"].strip(), disease_name)

        if key not in grouped:
            grouped[key] = {
                "scope": "STATE",
                "estado": row["estado"].strip(),
                "disease_name": disease_name,
                "grouping_type": set(),
                "source_diseases": set(),
                "case_count": 0,
                "confirmation_status": "CONFIRMED",
                "status": "ACTIVE",
            }

        aggregate = grouped[key]
        aggregate["grouping_type"].add(grouping_type)
        aggregate["source_diseases"].add(source_disease)
        aggregate["case_count"] += parse_int(row["total_casos_2026"])

    return [serialize_grouped_state_row(row) for row in sorted(grouped.values(), key=state_group_sort_key)]


def parse_int(value: str) -> int:
    value = value.strip()
    return int(value) if value else 0


def serialize_grouped_state_row(row: dict[str, object]) -> dict[str, str]:
    return {
        "scope": str(row["scope"]),
        "estado": str(row["estado"]),
        "disease_name": str(row["disease_name"]),
        "case_count": str(row["case_count"]),
        "confirmation_status": str(row["confirmation_status"]),
        "status": str(row["status"]),
    }


def state_group_sort_key(row: dict[str, object]) -> tuple[str, str, str]:
    return (str(row["estado"]), str(row["disease_name"]), str(row["grouping_type"]))


def write_csv(path: Path, fieldnames: list[str], rows: list[dict[str, str]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as target:
        writer = csv.DictWriter(target, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows)


if __name__ == "__main__":
    main()
