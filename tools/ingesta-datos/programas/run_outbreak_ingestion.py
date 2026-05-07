from __future__ import annotations

import argparse
import subprocess
import sys
from datetime import date
from pathlib import Path

from common_outbreaks import parse_date


SCRIPT_DIR = Path(__file__).resolve().parent
MUNICIPAL_SCRIPT = "run_municipal_outbreak_ingestion.py"
STATE_SCRIPT = "run_state_outbreak_ingestion.py"


def main() -> None:
    parser = argparse.ArgumentParser(description="Run the full outbreak ingestion pipeline.")
    parser.add_argument("--reference-date", type=parse_date, default=date.today())
    parser.add_argument("--months-back", type=int, default=2)
    parser.add_argument("--skip-municipal", action="store_true")
    parser.add_argument("--skip-state", action="store_true")
    parser.add_argument("--keep-downloads", action="store_true")
    parser.add_argument("--force-state-check", action="store_true")
    parser.add_argument("--state-pdf-url", help="Use a direct weekly bulletin PDF URL for the state pipeline.")
    parser.add_argument(
        "--no-publish-backend",
        action="store_true",
        help="Generate CSV outputs without copying them into backend resources.",
    )
    args = parser.parse_args()

    if args.skip_municipal and args.skip_state:
        raise ValueError("At least one pipeline must run.")

    publish_args = [] if args.no_publish_backend else ["--publish-backend"]

    if not args.skip_municipal:
        municipal_args = [
            "--download",
            "--reference-date",
            args.reference_date.isoformat(),
            "--months-back",
            str(args.months_back),
            *publish_args,
        ]
        if args.keep_downloads:
            municipal_args.append("--keep-downloads")
        run_script(MUNICIPAL_SCRIPT, *municipal_args)

    if not args.skip_state:
        state_args = ["--download", *publish_args]
        if args.keep_downloads:
            state_args.append("--keep-download")
        if args.force_state_check:
            state_args.append("--force-check")
        if args.state_pdf_url:
            state_args.extend(["--pdf-url", args.state_pdf_url])
        run_script(STATE_SCRIPT, *state_args)

    print("\nFull outbreak ingestion finished successfully.")


def run_script(script_name: str, *args: str) -> None:
    command = [sys.executable, str(SCRIPT_DIR / script_name), *args]
    print(f"\n> {' '.join(command)}", flush=True)
    subprocess.run(command, check=True)


if __name__ == "__main__":
    main()
