from __future__ import annotations

import argparse
import subprocess

from comun.procesos import publish_csv, run_module
from comun.rutas import BACKEND_OUTBREAKS_DIR, STATE_OUTPUT_DIR


DOWNLOAD_MODULE = "estatal.descargar_boletin_semanal"
EXTRACT_MODULE = "estatal.extraer_pdf_boletin"
FILTER_MODULE = "estatal.filtrar_enfermedades_relevantes"
FINAL_CSV = STATE_OUTPUT_DIR / "state_outbreak_relevant_cases.csv"
BACKEND_STATE_CSV = BACKEND_OUTBREAKS_DIR / "state_outbreaks.csv"


def main() -> None:
    parser = argparse.ArgumentParser(description="Run the state outbreak ingestion pipeline.")
    parser.add_argument("--download", action="store_true", help="Download the latest weekly bulletin PDF first.")
    parser.add_argument("--pdf-url", help="Use this PDF URL directly when downloading.")
    parser.add_argument("--keep-download", action="store_true")
    parser.add_argument("--force-check", action="store_true", help="Force gob.mx bulletin discovery even if metadata is recent.")
    parser.add_argument("--include-zero", action="store_true")
    parser.add_argument(
        "--publish-backend",
        action="store_true",
        help="Copy the final state outbreak CSV into the backend resources directory.",
    )
    args = parser.parse_args()

    if args.download:
        download_args = []
        if args.pdf_url:
            download_args.extend(["--pdf-url", args.pdf_url])
        if args.keep_download:
            download_args.append("--keep-download")
        if args.force_check:
            download_args.append("--force-check")
        try:
            run_module(DOWNLOAD_MODULE, *download_args, capture_failure=True)
        except subprocess.CalledProcessError:
            if BACKEND_STATE_CSV.exists():
                print(
                    "\nWARNING: Could not update the weekly bulletin PDF. "
                    f"Keeping existing backend state outbreak CSV: {BACKEND_STATE_CSV}"
                )
                return
            raise

    extract_args = ["--include-zero"] if args.include_zero else []
    run_module(EXTRACT_MODULE, *extract_args)
    run_module(FILTER_MODULE)

    if args.publish_backend:
        publish_csv(FINAL_CSV, BACKEND_STATE_CSV)


if __name__ == "__main__":
    main()
