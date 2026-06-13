from __future__ import annotations

import shutil
import subprocess
import sys
from pathlib import Path


PROGRAMAS_DIR = Path(__file__).resolve().parents[1]


def run_module(module_name: str, *args: str, capture_failure: bool = False) -> None:
    command = [sys.executable, "-m", module_name, *args]
    print(f"\n> {' '.join(command)}", flush=True)
    if not capture_failure:
        subprocess.run(command, cwd=PROGRAMAS_DIR, check=True)
        return

    result = subprocess.run(command, cwd=PROGRAMAS_DIR, text=True, capture_output=True)
    if result.stdout:
        print(result.stdout, end="")
    if result.returncode != 0:
        error_lines = [line for line in result.stderr.splitlines() if line.strip()]
        if error_lines:
            print(f"Download error: {error_lines[-1]}")
        raise subprocess.CalledProcessError(result.returncode, command, result.stdout, result.stderr)


def publish_csv(source: Path, target: Path) -> None:
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(source, target)
    print(f"Published backend CSV: {target}")
