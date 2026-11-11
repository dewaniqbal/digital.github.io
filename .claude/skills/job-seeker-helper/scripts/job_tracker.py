#!/usr/bin/env python3
"""Create and update a CSV job application tracker."""
from __future__ import annotations

import argparse
import csv
from datetime import date
from pathlib import Path
from typing import Dict, Iterable, List, Sequence

FIELDS = [
    "company",
    "role",
    "status",
    "source",
    "url",
    "applied_date",
    "next_action",
    "next_action_date",
    "contact",
    "notes",
    "last_updated",
]

STATUS_ALIASES = {
    "save": "Saved",
    "saved": "Saved",
    "apply": "Applied",
    "applied": "Applied",
    "recruiter": "Recruiter screen",
    "recruiter screen": "Recruiter screen",
    "interview": "Interviewing",
    "interviewing": "Interviewing",
    "takehome": "Take-home",
    "take-home": "Take-home",
    "final": "Final round",
    "final round": "Final round",
    "offer": "Offer",
    "rejected": "Rejected",
    "reject": "Rejected",
    "withdrawn": "Withdrawn",
    "closed": "Closed",
}


def normalize_status(status: str) -> str:
    value = (status or "Saved").strip()
    return STATUS_ALIASES.get(value.lower(), value[:1].upper() + value[1:])


def empty_row() -> Dict[str, str]:
    return {field: "" for field in FIELDS}


def load_tracker(path: str | Path) -> List[Dict[str, str]]:
    tracker_path = Path(path)
    if not tracker_path.exists():
        return []
    with tracker_path.open(newline="", encoding="utf-8") as handle:
        reader = csv.DictReader(handle)
        rows = []
        for row in reader:
            clean = empty_row()
            for key, value in row.items():
                if key in clean:
                    clean[key] = value or ""
            rows.append(clean)
        return rows


def find_row(rows: List[Dict[str, str]], company: str, role: str) -> Dict[str, str] | None:
    company_norm = company.strip().lower()
    role_norm = role.strip().lower()
    for row in rows:
        if row.get("company", "").strip().lower() == company_norm and row.get("role", "").strip().lower() == role_norm:
            return row
    return None


def add_or_update_job(rows: List[Dict[str, str]], **kwargs: str) -> Dict[str, str]:
    company = kwargs.get("company", "").strip()
    role = kwargs.get("role", "").strip()
    if not company or not role:
        raise ValueError("company and role are required")

    row = find_row(rows, company, role)
    if row is None:
        row = empty_row()
        rows.append(row)

    for field in FIELDS:
        value = kwargs.get(field)
        if value is not None and value != "":
            row[field] = value.strip()

    row["company"] = company
    row["role"] = role
    row["status"] = normalize_status(row.get("status", "Saved"))
    row["last_updated"] = kwargs.get("last_updated") or date.today().isoformat()
    return row


def write_tracker(path: str | Path, rows: Iterable[Dict[str, str]]) -> None:
    tracker_path = Path(path)
    tracker_path.parent.mkdir(parents=True, exist_ok=True)
    with tracker_path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=FIELDS)
        writer.writeheader()
        for row in rows:
            writer.writerow({field: row.get(field, "") for field in FIELDS})


def main(argv: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Create or update a job application tracker CSV.")
    parser.add_argument("--file", default="job_tracker.csv", help="Tracker CSV path")
    parser.add_argument("--company", required=True)
    parser.add_argument("--role", required=True)
    parser.add_argument("--status", default="Saved")
    parser.add_argument("--source", default="")
    parser.add_argument("--url", default="")
    parser.add_argument("--applied-date", default="")
    parser.add_argument("--next-action", default="")
    parser.add_argument("--next-action-date", default="")
    parser.add_argument("--contact", default="")
    parser.add_argument("--notes", default="")
    args = parser.parse_args(argv)

    rows = load_tracker(args.file)
    row = add_or_update_job(
        rows,
        company=args.company,
        role=args.role,
        status=args.status,
        source=args.source,
        url=args.url,
        applied_date=args.applied_date,
        next_action=args.next_action,
        next_action_date=args.next_action_date,
        contact=args.contact,
        notes=args.notes,
    )
    write_tracker(args.file, rows)
    print(f"Updated tracker: {row['company']} — {row['role']} ({row['status']})")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
