from __future__ import annotations

import argparse
from datetime import date

from comun.outbreaks import parse_date
from comun.procesos import run_module


MUNICIPAL_MODULE = "municipal.ejecutar_ingesta_municipal"
STATE_MODULE = "estatal.ejecutar_ingesta_estatal"


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
        run_module(MUNICIPAL_MODULE, *municipal_args)

    if not args.skip_state:
        state_args = ["--download", *publish_args]
        if args.keep_downloads:
            state_args.append("--keep-download")
        if args.force_state_check:
            state_args.append("--force-check")
        if args.state_pdf_url:
            state_args.extend(["--pdf-url", args.state_pdf_url])
        run_module(STATE_MODULE, *state_args)

    print("\nFull outbreak ingestion finished successfully.")


if __name__ == "__main__":
    main()
