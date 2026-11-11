#!/usr/bin/env python3
"""Combine Markdown job application outputs into one packet."""
from __future__ import annotations

import argparse
from datetime import date
from pathlib import Path
from typing import Iterable, List, Sequence


def read_file(path: str | Path) -> str:
    source = Path(path)
    return source.read_text(encoding="utf-8").strip()


def section_title(path: str | Path) -> str:
    name = Path(path).stem.replace("_", " ").replace("-", " ").title()
    return name


def build_packet(files: Iterable[str | Path], title: str = "Application Packet") -> str:
    lines: List[str] = [f"# {title}", "", f"Generated: {date.today().isoformat()}", ""]
    for file_path in files:
        source = Path(file_path)
        content = read_file(source)
        lines.extend(["---", "", f"## {section_title(source)}", "", content, ""])
    lines.extend([
        "---",
        "",
        "## Verification reminder",
        "",
        "Verify all dates, metrics, company names, tools, and personal information before submitting.",
        "",
    ])
    return "\n".join(lines)


def main(argv: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Build a combined Markdown application packet.")
    parser.add_argument("files", nargs="+", help="Markdown files to combine")
    parser.add_argument("--title", default="Application Packet")
    parser.add_argument("--output", default="application_packet.md")
    args = parser.parse_args(argv)

    packet = build_packet(args.files, title=args.title)
    Path(args.output).write_text(packet, encoding="utf-8")
    print(f"Wrote {args.output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
