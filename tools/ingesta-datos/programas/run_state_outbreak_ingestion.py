from __future__ import annotations

import argparse
import shutil
import subprocess
import sys
from pathlib import Path

from ingesta_paths import BACKEND_OUTBREAKS_DIR, STATE_OUTPUT_DIR


SCRIPT_DIR = Path(__file__).resolve().parent
DOWNLOAD_SCRIPT = "download_weekly_bulletin.py"
EXTRACT_SCRIPT = "extract_state_pdf.py"
FILTER_SCRIPT = "filter_state_outbreak_relevant.py"
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
            run_script(DOWNLOAD_SCRIPT, *download_args, capture_failure=True)
        except subprocess.CalledProcessError:
            if BACKEND_STATE_CSV.exists():
                print(
                    "\nWARNING: Could not update the weekly bulletin PDF. "
                    f"Keeping existing backend state outbreak CSV: {BACKEND_STATE_CSV}"
                )
                return
            raise

    extract_args = ["--include-zero"] if args.include_zero else []
    run_script(EXTRACT_SCRIPT, *extract_args)
    run_script(FILTER_SCRIPT)

    if args.publish_backend:
        BACKEND_STATE_CSV.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(FINAL_CSV, BACKEND_STATE_CSV)
        print(f"Published backend CSV: {BACKEND_STATE_CSV}")


def run_script(script_name: str, *args: str, capture_failure: bool = False) -> None:
    command = [sys.executable, str(SCRIPT_DIR / script_name), *args]
    print(f"\n> {' '.join(command)}", flush=True)
    if not capture_failure:
        subprocess.run(command, check=True)
        return

    result = subprocess.run(command, text=True, capture_output=True)
    if result.stdout:
        print(result.stdout, end="")
    if result.returncode != 0:
        error_lines = [line for line in result.stderr.splitlines() if line.strip()]
        if error_lines:
            print(f"Download error: {error_lines[-1]}")
        raise subprocess.CalledProcessError(result.returncode, command, result.stdout, result.stderr)


if __name__ == "__main__":
    main()
