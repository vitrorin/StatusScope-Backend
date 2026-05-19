from __future__ import annotations

import argparse
import json
import re
import shutil
import ssl
import urllib.error
import urllib.parse
import urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from html import unescape
from pathlib import Path

from comun.rutas import DOWNLOAD_CACHE_DIR, RAW_STATE_DIR


BULLETIN_INDEX_URL = (
    "https://www.gob.mx/salud/documentos/"
    "boletinepidemiologico-sistema-nacional-de-vigilancia-epidemiologica-sistema-unico-de-informacion-417103"
)
DEFAULT_PDF_PATH = RAW_STATE_DIR / "boletin nacional.pdf"
METADATA_PATH = DOWNLOAD_CACHE_DIR / "weekly_bulletin_metadata.json"
DEFAULT_SEED_URL = "https://www.gob.mx/cms/uploads/attachment/file/1076715/sem16.pdf"
USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36"
)


@dataclass(frozen=True)
class BulletinCandidate:
    week: int
    url: str
    label: str


def main() -> None:
    parser = argparse.ArgumentParser(description="Download the latest weekly epidemiological bulletin PDF.")
    parser.add_argument("--index-url", default=BULLETIN_INDEX_URL)
    parser.add_argument("--pdf-url", help="Use this PDF URL directly instead of discovering it from the index page.")
    parser.add_argument("--output", type=Path, default=DEFAULT_PDF_PATH)
    parser.add_argument("--keep-download", action="store_true")
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--force-check", action="store_true", help="Ignore recent metadata and probe gob.mx again.")
    args = parser.parse_args()

    pdf_url = args.pdf_url or discover_latest_pdf_url_with_fallback(args.index_url, force_check=args.force_check)
    if args.dry_run:
        print(f"PDF URL: {pdf_url}")
        print(f"Output: {args.output}")
        return

    if can_reuse_existing_pdf(pdf_url, args.output):
        print(f"Reusing existing weekly bulletin PDF: {args.output}")
    else:
        download_pdf(pdf_url, args.output, keep_download=args.keep_download)
    save_metadata(pdf_url)
    print(f"Updated weekly bulletin PDF: {args.output}")


def discover_latest_pdf_url_with_fallback(index_url: str, *, force_check: bool = False) -> str:
    if not force_check:
        cached_url = recent_known_pdf_url()
        if cached_url:
            print(f"Using recently checked bulletin URL: {cached_url}")
            return cached_url

    try:
        return discover_latest_pdf_url(index_url)
    except RuntimeError as exc:
        print(f"Index discovery failed: {exc}")
        discovered = discover_by_direct_probe()
        print(f"Selected bulletin by direct probe: {discovered}")
        return discovered


def discover_latest_pdf_url(index_url: str) -> str:
    html = fetch_text(index_url)
    if "Challenge Validation" in html or "challenge" in html[:2000].lower():
        raise RuntimeError(
            "Gob.mx returned an anti-bot challenge instead of the bulletin index. "
            "Falling back to direct PDF probing."
        )
    candidates = extract_bulletin_candidates(html, index_url)
    if not candidates:
        raise RuntimeError(f"No weekly bulletin PDF links were found in {index_url}")

    selected = max(candidates, key=lambda candidate: candidate.week)
    print(f"Selected bulletin: week {selected.week} ({selected.url})")
    return selected.url


def discover_by_direct_probe() -> str:
    seed_url = last_known_pdf_url() or DEFAULT_SEED_URL
    seed = parse_bulletin_url(seed_url)
    if seed is None:
        raise RuntimeError("Could not parse last known bulletin URL for direct probing")

    # First, look for newer weeks near the previous attachment id.
    for week in range(seed.week + 1, 54):
        found = probe_week_near_attachment_id(week, seed.attachment_id, search_radius=1500)
        if found:
            seed = parse_bulletin_url(found)
            continue
        break

    # If the current saved week is still the newest available, return it.
    return build_pdf_url(seed.attachment_id, seed.week)


def probe_week_near_attachment_id(week: int, center_id: int, search_radius: int) -> str | None:
    # Gob.mx attachment ids generally increase over time; scan forward first, then a small backward range.
    forward_ids = list(range(center_id + 1, center_id + search_radius + 1))
    found = probe_ids_for_week(week, forward_ids)
    if found:
        return found

    backward_ids = list(range(center_id, max(center_id - 500, 0), -1))
    found = probe_ids_for_week(week, backward_ids)
    if found:
        return found
    return None


def probe_ids_for_week(week: int, attachment_ids: list[int]) -> str | None:
    executor = ThreadPoolExecutor(max_workers=64)
    try:
        futures = {
            executor.submit(pdf_exists, build_pdf_url(attachment_id, week)): attachment_id
            for attachment_id in attachment_ids
        }
        for future in as_completed(futures):
            attachment_id = futures[future]
            if future.result():
                executor.shutdown(wait=False, cancel_futures=True)
                return build_pdf_url(attachment_id, week)
        return None
    finally:
        executor.shutdown(wait=False, cancel_futures=True)


def pdf_exists(url: str) -> bool:
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT, "Range": "bytes=0-4"})
    try:
        with urllib.request.urlopen(request, timeout=0.7, context=ssl.create_default_context()) as response:
            return response.read(5) == b"%PDF-"
    except (urllib.error.URLError, TimeoutError, ValueError):
        return False


def extract_bulletin_candidates(html: str, base_url: str) -> list[BulletinCandidate]:
    candidates: list[BulletinCandidate] = []
    for match in re.finditer(r"<a\b[^>]*href=[\"']([^\"']+)[\"'][^>]*>(.*?)</a>", html, re.IGNORECASE | re.DOTALL):
        href = unescape(match.group(1))
        label = clean_html(match.group(2))
        candidate = candidate_from_link(href, label, base_url)
        if candidate:
            candidates.append(candidate)

    if candidates:
        return dedupe_candidates(candidates)

    # Fallback for pages where PDF URLs are present outside anchors.
    for match in re.finditer(r"https?://[^\"'\s>]+sem(\d+)\.pdf", html, re.IGNORECASE):
        week = int(match.group(1))
        candidates.append(BulletinCandidate(week=week, url=match.group(0), label=f"sem{week}.pdf"))
    return dedupe_candidates(candidates)


def candidate_from_link(href: str, label: str, base_url: str) -> BulletinCandidate | None:
    absolute_url = urllib.parse.urljoin(base_url, href)
    combined = f"{label} {absolute_url}"
    week = extract_week(combined)
    if week is None:
        return None
    if ".pdf" not in absolute_url.lower():
        return None
    return BulletinCandidate(week=week, url=absolute_url, label=label)


def extract_week(value: str) -> int | None:
    patterns = [
        r"Semana\s+Epidemiol[oó]gica\s+(\d{1,2})",
        r"semana\s+(\d{1,2})",
        r"sem(\d{1,2})\.pdf",
    ]
    for pattern in patterns:
        match = re.search(pattern, value, re.IGNORECASE)
        if match:
            week = int(match.group(1))
            if 1 <= week <= 53:
                return week
    return None


def dedupe_candidates(candidates: list[BulletinCandidate]) -> list[BulletinCandidate]:
    by_url = {}
    for candidate in candidates:
        by_url[candidate.url] = candidate
    return list(by_url.values())


def download_pdf(pdf_url: str, output_path: Path, *, keep_download: bool) -> None:
    output_path.parent.mkdir(parents=True, exist_ok=True)
    temp_output = output_path.with_suffix(output_path.suffix + ".tmp")
    download_to_path(pdf_url, temp_output)
    if temp_output.stat().st_size == 0:
        temp_output.unlink(missing_ok=True)
        raise ValueError(f"Downloaded empty PDF from {pdf_url}")
    if not looks_like_pdf(temp_output):
        temp_output.unlink(missing_ok=True)
        raise ValueError(f"Downloaded file does not look like a PDF: {pdf_url}")
    temp_output.replace(output_path)

    if keep_download:
        DOWNLOAD_CACHE_DIR.mkdir(parents=True, exist_ok=True)
        cache_path = DOWNLOAD_CACHE_DIR / Path(urllib.parse.urlparse(pdf_url).path).name
        shutil.copyfile(output_path, cache_path)
        print(f"Cached weekly bulletin PDF: {cache_path}")


def can_reuse_existing_pdf(pdf_url: str, output_path: Path) -> bool:
    if not output_path.exists() or not looks_like_pdf(output_path):
        return False

    metadata_url = last_known_pdf_url()
    if metadata_url:
        return metadata_url == pdf_url

    # Bootstrap case: the sem16 PDF may already exist from the previous manual flow,
    # before this downloader had written metadata.
    return pdf_url == DEFAULT_SEED_URL


def fetch_text(url: str) -> str:
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    try:
        with urllib.request.urlopen(request, timeout=30, context=ssl.create_default_context()) as response:
            return response.read().decode("utf-8", errors="replace")
    except urllib.error.URLError as exc:
        raise RuntimeError(f"Could not fetch bulletin index page: {url}") from exc


def download_to_path(url: str, path: Path) -> None:
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=60, context=ssl.create_default_context()) as response:
        with path.open("wb") as target:
            shutil.copyfileobj(response, target)


def looks_like_pdf(path: Path) -> bool:
    with path.open("rb") as source:
        return source.read(5) == b"%PDF-"


def clean_html(value: str) -> str:
    without_tags = re.sub(r"<[^>]+>", " ", value)
    return re.sub(r"\s+", " ", unescape(without_tags)).strip()


def last_known_pdf_url() -> str | None:
    metadata = read_metadata()
    url = metadata.get("url")
    return url if isinstance(url, str) and url else None


def recent_known_pdf_url() -> str | None:
    metadata = read_metadata()
    checked_at = metadata.get("checked_at")
    url = metadata.get("url")
    if not isinstance(checked_at, str) or not isinstance(url, str) or not url:
        return None
    try:
        checked_time = datetime.fromisoformat(checked_at)
    except ValueError:
        return None
    if checked_time.tzinfo is None:
        checked_time = checked_time.replace(tzinfo=timezone.utc)
    if datetime.now(timezone.utc) - checked_time <= timedelta(hours=12):
        return url
    return None


def read_metadata() -> dict[str, object]:
    if not METADATA_PATH.exists():
        return {}
    try:
        with METADATA_PATH.open("r", encoding="utf-8") as source:
            metadata = json.load(source)
        return metadata if isinstance(metadata, dict) else {}
    except (OSError, json.JSONDecodeError):
        return {}


def save_metadata(pdf_url: str) -> None:
    parsed = parse_bulletin_url(pdf_url)
    DOWNLOAD_CACHE_DIR.mkdir(parents=True, exist_ok=True)
    metadata = {
        "url": pdf_url,
        "attachment_id": parsed.attachment_id if parsed else None,
        "week": parsed.week if parsed else None,
        "checked_at": datetime.now(timezone.utc).isoformat(timespec="seconds"),
    }
    with METADATA_PATH.open("w", encoding="utf-8") as target:
        json.dump(metadata, target, indent=2, ensure_ascii=False)
        target.write("\n")


def parse_bulletin_url(url: str) -> ParsedBulletinUrl | None:
    match = re.search(r"/attachment/file/(\d+)/sem(\d{1,2})\.pdf", url, re.IGNORECASE)
    if not match:
        return None
    return ParsedBulletinUrl(attachment_id=int(match.group(1)), week=int(match.group(2)))


def build_pdf_url(attachment_id: int, week: int) -> str:
    return f"https://www.gob.mx/cms/uploads/attachment/file/{attachment_id}/sem{week}.pdf"


@dataclass(frozen=True)
class ParsedBulletinUrl:
    attachment_id: int
    week: int


if __name__ == "__main__":
    main()
