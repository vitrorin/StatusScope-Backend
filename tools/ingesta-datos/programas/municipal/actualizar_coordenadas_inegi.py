from __future__ import annotations

import csv
import json
import math
import urllib.request
from pathlib import Path

from comun.rutas import DOWNLOAD_CACHE_DIR, MUNICIPALITIES_PATH


INEGI_MGEM_URL = "https://gaia.inegi.org.mx/wscatgeo/v2/geo/mgem/{state_code}"
INEGI_LOCALITIES_URL = "https://gaia.inegi.org.mx/wscatgeo/v2/localidades/{municipality_code}"
OUTPUT_PRECISION = 6


def polygon_area(ring: list[list[float]]) -> float:
    area = 0.0
    for index in range(len(ring) - 1):
        lon1, lat1 = ring[index]
        lon2, lat2 = ring[index + 1]
        area += lon1 * lat2 - lon2 * lat1
    return area / 2.0


def polygon_centroid(ring: list[list[float]]) -> tuple[float, float]:
    area_factor = 0.0
    centroid_lon = 0.0
    centroid_lat = 0.0

    for index in range(len(ring) - 1):
        lon1, lat1 = ring[index]
        lon2, lat2 = ring[index + 1]
        cross = lon1 * lat2 - lon2 * lat1
        area_factor += cross
        centroid_lon += (lon1 + lon2) * cross
        centroid_lat += (lat1 + lat2) * cross

    if abs(area_factor) < 1e-12:
        return ring[0][0], ring[0][1]

    return centroid_lon / (3 * area_factor), centroid_lat / (3 * area_factor)


def point_in_ring(lon: float, lat: float, ring: list[list[float]]) -> bool:
    inside = False
    previous = len(ring) - 1
    for current in range(len(ring)):
        current_lon, current_lat = ring[current]
        previous_lon, previous_lat = ring[previous]
        if ((current_lat > lat) != (previous_lat > lat)
                and lon < ((previous_lon - current_lon) * (lat - current_lat))
                / (previous_lat - current_lat) + current_lon):
            inside = not inside
        previous = current
    return inside


def distance_to_segment_squared(lon: float, lat: float, start: list[float], end: list[float]) -> float:
    start_lon, start_lat = start
    end_lon, end_lat = end
    dx = end_lon - start_lon
    dy = end_lat - start_lat
    length_squared = dx * dx + dy * dy
    if length_squared == 0:
        return (lon - start_lon) ** 2 + (lat - start_lat) ** 2

    ratio = max(0.0, min(1.0, ((lon - start_lon) * dx + (lat - start_lat) * dy) / length_squared))
    projected_lon = start_lon + ratio * dx
    projected_lat = start_lat + ratio * dy
    return (lon - projected_lon) ** 2 + (lat - projected_lat) ** 2


def min_distance_to_ring_squared(lon: float, lat: float, ring: list[list[float]]) -> float:
    return min(
        distance_to_segment_squared(lon, lat, ring[index], ring[index + 1])
        for index in range(len(ring) - 1)
    )


def largest_outer_ring(geometry: dict) -> list[list[float]]:
    polygons = geometry["coordinates"] if geometry["type"] == "MultiPolygon" else [geometry["coordinates"]]
    outer_rings = [polygon[0] for polygon in polygons if polygon]
    return max(outer_rings, key=lambda ring: abs(polygon_area(ring)))


def representative_point(geometry: dict) -> tuple[float, float]:
    ring = largest_outer_ring(geometry)
    centroid = polygon_centroid(ring)
    if point_in_ring(centroid[0], centroid[1], ring):
        return centroid

    longitudes = [point[0] for point in ring]
    latitudes = [point[1] for point in ring]
    bbox_center = ((min(longitudes) + max(longitudes)) / 2.0, (min(latitudes) + max(latitudes)) / 2.0)
    if point_in_ring(bbox_center[0], bbox_center[1], ring):
        return bbox_center

    best_point: tuple[float, float] | None = None
    best_distance = -1.0
    grid_steps = 28
    for x_index in range(grid_steps + 1):
        lon = min(longitudes) + (max(longitudes) - min(longitudes)) * x_index / grid_steps
        for y_index in range(grid_steps + 1):
            lat = min(latitudes) + (max(latitudes) - min(latitudes)) * y_index / grid_steps
            if not point_in_ring(lon, lat, ring):
                continue
            distance = min_distance_to_ring_squared(lon, lat, ring)
            if distance > best_distance:
                best_point = (lon, lat)
                best_distance = distance

    if best_point is not None:
        return best_point

    return ring[0][0], ring[0][1]


def download_state_geojson(state_code: str) -> Path:
    DOWNLOAD_CACHE_DIR.mkdir(parents=True, exist_ok=True)
    target = DOWNLOAD_CACHE_DIR / f"inegi_mgem_{state_code}.geojson"
    if target.exists() and target.stat().st_size > 1024:
        return target

    request = urllib.request.Request(
        INEGI_MGEM_URL.format(state_code=state_code),
        headers={"User-Agent": "Mozilla/5.0"},
    )
    with urllib.request.urlopen(request, timeout=90) as response:
        target.write_bytes(response.read())
    return target


def download_localities(municipality_code: str) -> Path:
    DOWNLOAD_CACHE_DIR.mkdir(parents=True, exist_ok=True)
    target = DOWNLOAD_CACHE_DIR / f"inegi_localidades_{municipality_code}.json"
    if target.exists() and target.stat().st_size > 64:
        return target

    request = urllib.request.Request(
        INEGI_LOCALITIES_URL.format(municipality_code=municipality_code),
        headers={"User-Agent": "Mozilla/5.0"},
    )
    with urllib.request.urlopen(request, timeout=60) as response:
        target.write_bytes(response.read())
    return target


def load_cabecera_point(municipality_code: str, cabecera_code: str) -> tuple[float, float] | None:
    if not cabecera_code or cabecera_code == "0000":
        return None

    data = json.loads(download_localities(municipality_code).read_text(encoding="utf-8"))
    target_code = f"{municipality_code}{cabecera_code.zfill(4)}"
    for locality in data.get("datos", []):
        if str(locality.get("cvegeo")) == target_code:
            return float(locality["latitud"]), float(locality["longitud"])
    return None


def load_inegi_points() -> dict[str, tuple[float, float]]:
    points: dict[str, tuple[float, float]] = {}
    fallback_count = 0
    for state_number in range(1, 33):
        state_code = f"{state_number:02d}"
        path = download_state_geojson(state_code)
        data = json.loads(path.read_text(encoding="utf-8"))
        for feature in data.get("features", []):
            cvegeo = str(feature["properties"]["cvegeo"]).zfill(5)
            cabecera = str(feature["properties"].get("cve_cab", "")).zfill(4)
            point = load_cabecera_point(cvegeo, cabecera)
            if point is None:
                lon, lat = representative_point(feature["geometry"])
                point = (lat, lon)
                fallback_count += 1
            points[cvegeo] = point
        print(f"{state_code}: {len(data.get('features', []))} municipios")
    print(f"Cabecera fallback to polygon interior: {fallback_count}")
    return points


def format_coordinate(value: float) -> str:
    return f"{value:.{OUTPUT_PRECISION}f}".rstrip("0").rstrip(".")


def main() -> None:
    points = load_inegi_points()
    rows: list[dict[str, str]] = []
    with MUNICIPALITIES_PATH.open("r", encoding="utf-8-sig", newline="") as source:
        reader = csv.DictReader(source)
        fieldnames = reader.fieldnames or []
        for row in reader:
            cvegeo = row["code"].zfill(5)
            if cvegeo in points:
                latitude, longitude = points[cvegeo]
                row["latitud"] = format_coordinate(latitude)
                row["longitud"] = format_coordinate(longitude)
            rows.append(row)

    missing = [row["code"] for row in rows if row["code"].zfill(5) not in points]
    if missing:
        raise RuntimeError(f"No INEGI geometry found for {len(missing)} municipalities: {missing[:10]}")

    with MUNICIPALITIES_PATH.open("w", encoding="utf-8", newline="") as target:
        writer = csv.DictWriter(target, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows)

    print(f"Updated {len(rows)} municipalities in {MUNICIPALITIES_PATH}")


if __name__ == "__main__":
    main()
