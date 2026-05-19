from __future__ import annotations

import argparse
from datetime import date

from comun.outbreaks import parse_date
from comun.procesos import publish_csv, run_module
from comun.rutas import BACKEND_OUTBREAKS_DIR, MUNICIPAL_OUTPUT_DIR


EXTRACTOR_MODULES = [
    "municipal.extraer_respiratorias",
    "municipal.extraer_febriles_exantematicas",
    "municipal.extraer_dengue",
]
DOWNLOAD_MODULE = "municipal.descargar_datos_abiertos"
COMBINE_MODULE = "municipal.combinar_outbreaks_municipales"
FINAL_CSV = MUNICIPAL_OUTPUT_DIR / "municipal_outbreak_signals.csv"
BACKEND_MUNICIPAL_CSV = BACKEND_OUTBREAKS_DIR / "municipal_outbreaks.csv"


def main() -> None:
    parser = argparse.ArgumentParser(description="Run the municipal outbreak ingestion pipeline.")
    parser.add_argument("--reference-date", type=parse_date, default=date.today())
    parser.add_argument("--months-back", type=int, default=2)
    parser.add_argument(
        "--download",
        action="store_true",
        help="Download official Salud ZIPs before extracting outbreak signals.",
    )
    parser.add_argument(
        "--keep-downloads",
        action="store_true",
        help="Keep downloaded ZIPs after the run. Only used with --download.",
    )
    parser.add_argument(
        "--publish-backend",
        action="store_true",
        help="Copy the final municipal outbreak CSV into the backend resources directory.",
    )
    args = parser.parse_args()

    if args.download:
        download_args = ["--keep-downloads"] if args.keep_downloads else []
        run_module(DOWNLOAD_MODULE, *download_args)

    for module_name in EXTRACTOR_MODULES:
        run_module(
            module_name,
            "--reference-date",
            args.reference_date.isoformat(),
            "--months-back",
            str(args.months_back),
        )
    run_module(COMBINE_MODULE)

    if args.publish_backend:
        publish_csv(FINAL_CSV, BACKEND_MUNICIPAL_CSV)


if __name__ == "__main__":
    main()
