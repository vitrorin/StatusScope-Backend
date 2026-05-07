from __future__ import annotations

import argparse
import shutil
import subprocess
import sys
from datetime import date
from pathlib import Path

from common_outbreaks import parse_date
from ingesta_paths import BACKEND_OUTBREAKS_DIR, MUNICIPAL_OUTPUT_DIR


SCRIPT_DIR = Path(__file__).resolve().parent
EXTRACTOR_SCRIPTS = [
    "extract_respiratory_municipal.py",
    "extract_febrile_exanthematous_municipal.py",
    "extract_dengue_municipal.py",
]
DOWNLOAD_SCRIPT = "download_open_data_sources.py"
COMBINE_SCRIPT = "combine_municipal_outbreaks.py"
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
        run_script(DOWNLOAD_SCRIPT, *download_args)

    for script_name in EXTRACTOR_SCRIPTS:
        run_script(
            script_name,
            "--reference-date",
            args.reference_date.isoformat(),
            "--months-back",
            str(args.months_back),
        )
    run_script(COMBINE_SCRIPT)

    if args.publish_backend:
        BACKEND_MUNICIPAL_CSV.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(FINAL_CSV, BACKEND_MUNICIPAL_CSV)
        print(f"Published backend CSV: {BACKEND_MUNICIPAL_CSV}")


def run_script(script_name: str, *args: str) -> None:
    command = [sys.executable, str(SCRIPT_DIR / script_name), *args]
    print(f"\n> {' '.join(command)}", flush=True)
    subprocess.run(command, check=True)


if __name__ == "__main__":
    main()
