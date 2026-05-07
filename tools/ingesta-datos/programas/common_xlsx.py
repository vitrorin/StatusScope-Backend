from __future__ import annotations

import zipfile
import xml.etree.ElementTree as ET
from pathlib import Path


SPREADSHEET_NS = {
    "a": "http://schemas.openxmlformats.org/spreadsheetml/2006/main",
    "r": "http://schemas.openxmlformats.org/officeDocument/2006/relationships",
}

RELATIONSHIP_ID = "{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id"


def read_xlsx(path: Path) -> dict[str, list[list[str]]]:
    with zipfile.ZipFile(path) as archive:
        archive_names = set(archive.namelist())
        shared_strings = _load_shared_strings(archive, archive_names)
        workbook = ET.fromstring(archive.read("xl/workbook.xml"))
        relationships = ET.fromstring(archive.read("xl/_rels/workbook.xml.rels"))
        targets_by_id = {
            relationship.attrib["Id"]: relationship.attrib["Target"]
            for relationship in relationships
        }

        sheets: dict[str, list[list[str]]] = {}
        for sheet in workbook.find("a:sheets", SPREADSHEET_NS):
            name = sheet.attrib["name"]
            target = targets_by_id[sheet.attrib[RELATIONSHIP_ID]]
            sheets[name] = _read_sheet(archive, target, shared_strings)
        return sheets


def _load_shared_strings(archive: zipfile.ZipFile, archive_names: set[str]) -> list[str]:
    if "xl/sharedStrings.xml" not in archive_names:
        return []

    root = ET.fromstring(archive.read("xl/sharedStrings.xml"))
    values = []
    for item in root.findall("a:si", SPREADSHEET_NS):
        values.append("".join(text.text or "" for text in item.findall(".//a:t", SPREADSHEET_NS)))
    return values


def _read_sheet(
    archive: zipfile.ZipFile,
    target: str,
    shared_strings: list[str],
) -> list[list[str]]:
    root = ET.fromstring(archive.read("xl/" + target))
    rows = []
    for row in root.findall(".//a:sheetData/a:row", SPREADSHEET_NS):
        values: list[str] = []
        for cell in row.findall("a:c", SPREADSHEET_NS):
            index = _column_index(cell.attrib["r"])
            while len(values) <= index:
                values.append("")
            values[index] = _cell_value(cell, shared_strings)
        rows.append(values)
    return rows


def _cell_value(cell: ET.Element, shared_strings: list[str]) -> str:
    value = cell.find("a:v", SPREADSHEET_NS)
    if value is None:
        inline = cell.find("a:is/a:t", SPREADSHEET_NS)
        return inline.text if inline is not None and inline.text is not None else ""

    raw = value.text or ""
    if cell.attrib.get("t") == "s":
        return shared_strings[int(raw)]
    return raw


def _column_index(cell_reference: str) -> int:
    letters = "".join(character for character in cell_reference if character.isalpha())
    index = 0
    for letter in letters:
        index = index * 26 + ord(letter.upper()) - 64
    return index - 1
