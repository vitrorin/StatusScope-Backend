from __future__ import annotations

import argparse
import shutil
import tempfile
import urllib.error
import urllib.request
import zipfile
from dataclasses import dataclass
from pathlib import Path

from comun.outbreaks import normalize
from comun.rutas import DOWNLOAD_CACHE_DIR, RAW_MUNICIPAL_DIR


RESPIRATORY_DIR = RAW_MUNICIPAL_DIR / "Enfermedades Respiratorias"
EFE_DIR = RAW_MUNICIPAL_DIR / "Enfermedades Febriles Exantemáticas"
VECTOR_DIR = RAW_MUNICIPAL_DIR / "Enfermedades Transmitidas por Vectores"


@dataclass(frozen=True)
class SourceFile:
    label: str
    url: str
    target_path: Path
    exact_names: tuple[str, ...]
    required_keywords: tuple[str, ...] = ()
    rejected_keywords: tuple[str, ...] = ()


SOURCE_FILES = [
    SourceFile(
        label="respiratory_cases",
        url="https://datosabiertos.salud.gob.mx/gobmx/salud/datos_abiertos/datos_abiertos_influenza_covid19.zip",
        target_path=RESPIRATORY_DIR / "COVID19MEXICO.csv",
        exact_names=("COVID19MEXICO.csv",),
    ),
    SourceFile(
        label="respiratory_catalog",
        url="https://datosabiertos.salud.gob.mx/gobmx/salud/datos_abiertos/diccionario_datos_abiertos.zip",
        target_path=RESPIRATORY_DIR / "240708 Catalogos.xlsx",
        exact_names=("240708 Catalogos.xlsx",),
        required_keywords=("catalog",),
        rejected_keywords=("descriptor", "descriptores"),
    ),
    SourceFile(
        label="efe_cases",
        url="https://datosabiertos.salud.gob.mx/gobmx/salud/datos_abiertos/efe/datos_abiertos_efe.zip",
        target_path=EFE_DIR / "efes_abierto_16.csv",
        exact_names=("efes_abierto_16.csv",),
        required_keywords=("efes", "abierto"),
    ),
    SourceFile(
        label="efe_catalog",
        url="https://datosabiertos.salud.gob.mx/gobmx/salud/datos_abiertos/efe/diccionario_datos_efe.zip",
        target_path=EFE_DIR / "20201123_Catálogos_EFEs.xlsx",
        exact_names=("20201123_Catálogos_EFEs.xlsx",),
        required_keywords=("catalog", "efe"),
        rejected_keywords=("descriptor", "descriptores"),
    ),
    SourceFile(
        label="dengue_cases",
        url="https://datosabiertos.salud.gob.mx/gobmx/salud/datos_abiertos/etv/datos_abiertos_dengue.zip",
        target_path=VECTOR_DIR / "dengue_abierto.csv",
        exact_names=("dengue_abierto.csv",),
    ),
    SourceFile(
        label="dengue_catalog",
        url="https://datosabiertos.salud.gob.mx/gobmx/salud/datos_abiertos/etv/diccionario_datos_dengue.zip",
        target_path=VECTOR_DIR / "Catálogos_Dengue.xlsx",
        exact_names=("Catálogos_Dengue.xlsx",),
        required_keywords=("catalog", "dengue"),
        rejected_keywords=("descriptor", "descriptores"),
    ),
]


def main() -> None:
    parser = argparse.ArgumentParser(description="Download and place official Salud open-data files for outbreaks.")
    parser.add_argument(
        "--keep-downloads",
        action="store_true",
        help="Store downloaded ZIPs in the ingestion cache instead of only using them for this run.",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Print the files that would be downloaded and updated without touching the filesystem.",
    )
    args = parser.parse_args()

    if args.dry_run:
        print_plan()
        return

    DOWNLOAD_CACHE_DIR.mkdir(parents=True, exist_ok=True)
    if args.keep_downloads:
        for source in SOURCE_FILES:
            zip_path = DOWNLOAD_CACHE_DIR / zip_name_from_url(source.url)
            download_and_place(source, zip_path, cache_path=zip_path)
        return

    temp_root = DOWNLOAD_CACHE_DIR / "tmp"
    temp_root.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="statuscope_ingesta_", dir=temp_root) as temp_dir:
        temp_path = Path(temp_dir)
        for source in SOURCE_FILES:
            zip_name = zip_name_from_url(source.url)
            zip_path = temp_path / zip_name
            cache_path = DOWNLOAD_CACHE_DIR / zip_name
            download_and_place(source, zip_path, cache_path=cache_path)


def print_plan() -> None:
    for source in SOURCE_FILES:
        print(f"{source.label}")
        print(f"  URL: {source.url}")
        print(f"  target: {source.target_path}")


def download_and_place(source: SourceFile, zip_path: Path, *, cache_path: Path) -> None:
    zip_path.parent.mkdir(parents=True, exist_ok=True)
    print(f"\nDownloading {source.label}: {source.url}")
    try:
        urllib.request.urlretrieve(source.url, zip_path)
        print(f"Downloaded: {zip_path}")
        if zip_path.resolve() != cache_path.resolve():
            shutil.copyfile(zip_path, cache_path)
            print(f"Cached: {cache_path}")
    except (urllib.error.URLError, TimeoutError, OSError) as exc:
        if not cache_path.exists():
            raise RuntimeError(f"Could not download {source.label} and no cached ZIP exists: {source.url}") from exc
        zip_path = cache_path
        print(f"Download failed; using cached ZIP: {cache_path}")

    member = find_zip_member(zip_path, source)
    source.target_path.parent.mkdir(parents=True, exist_ok=True)
    temp_target_path = source.target_path.with_suffix(source.target_path.suffix + ".tmp")
    with zipfile.ZipFile(zip_path) as archive, archive.open(member) as source_file:
        with temp_target_path.open("wb") as target_file:
            shutil.copyfileobj(source_file, target_file)
    if temp_target_path.stat().st_size == 0:
        temp_target_path.unlink(missing_ok=True)
        raise ValueError(f"Extracted empty file for {source.label}: {member}")
    temp_target_path.replace(source.target_path)
    print(f"Updated: {source.target_path}")


def find_zip_member(zip_path: Path, source: SourceFile) -> str:
    with zipfile.ZipFile(zip_path) as archive:
        file_members = [
            member
            for member in archive.namelist()
            if not member.endswith("/") and not Path(member).name.startswith("._")
        ]
        exact_match = find_exact_member(file_members, source.exact_names)
        if exact_match:
            return exact_match

        keyword_match = find_keyword_member(file_members, source)
        if keyword_match:
            return keyword_match

    expected = ", ".join(source.exact_names)
    raise FileNotFoundError(f"Could not find {source.label} in {zip_path}. Expected one of: {expected}")


def find_exact_member(members: list[str], exact_names: tuple[str, ...]) -> str | None:
    expected = {normalized_file_name(name) for name in exact_names}
    for member in members:
        if normalized_file_name(Path(member).name) in expected:
            return member
    return None


def find_keyword_member(members: list[str], source: SourceFile) -> str | None:
    if not source.required_keywords:
        return None

    candidates = []
    for member in members:
        normalized_name = normalized_file_name(Path(member).name)
        if all(keyword in normalized_name for keyword in source.required_keywords) and not any(
            keyword in normalized_name for keyword in source.rejected_keywords
        ):
            candidates.append(member)

    if len(candidates) == 1:
        return candidates[0]
    if len(candidates) > 1:
        candidate_list = ", ".join(candidates)
        raise ValueError(f"Multiple candidates found for {source.label} in ZIP: {candidate_list}")
    return None


def normalized_file_name(value: str) -> str:
    normalized = normalize(value).lower()
    return "".join(character for character in normalized if character.isalnum())


def zip_name_from_url(url: str) -> str:
    return url.rstrip("/").rsplit("/", 1)[-1]


if __name__ == "__main__":
    main()
