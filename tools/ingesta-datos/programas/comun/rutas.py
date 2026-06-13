from __future__ import annotations

from pathlib import Path


PROGRAMAS_DIR = Path(__file__).resolve().parents[1]
INGESTA_DIR = PROGRAMAS_DIR.parent
BACKEND_DIR = INGESTA_DIR.parents[1]
WORKSPACE_DIR = BACKEND_DIR
BACKEND_DATA_DIR = BACKEND_DIR / "src" / "main" / "resources" / "data"

DATA_DIR = INGESTA_DIR / ".data"
RAW_MUNICIPAL_DIR = DATA_DIR / "datos_nivel_municipio"
RAW_STATE_DIR = DATA_DIR / "datos_nivel_estado"
MUNICIPALITIES_PATH = BACKEND_DATA_DIR / "municipalities" / "mexico_municipalities.csv"
DOWNLOAD_CACHE_DIR = DATA_DIR / "descargas_ingesta"
EXTRACTED_DIR = DATA_DIR / "datos_extraidos"
MUNICIPAL_OUTPUT_DIR = EXTRACTED_DIR / "nivel_municipio"
STATE_OUTPUT_DIR = EXTRACTED_DIR / "nivel_estado"

BACKEND_OUTBREAKS_DIR = BACKEND_DATA_DIR / "outbreaks"
