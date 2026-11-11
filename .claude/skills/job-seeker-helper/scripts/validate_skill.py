#!/usr/bin/env python3
"""Validate Claude Skill folder structure and basic frontmatter."""
from __future__ import annotations

import argparse
import re
from pathlib import Path
from typing import Dict, List, Sequence, Tuple

FRONTMATTER_RE = re.compile(r"\A---\s*\n(?P<body>.*?)\n---\s*\n", re.DOTALL)
REQUIRED_FIELDS = {"name", "description"}


def parse_frontmatter(text: str) -> Dict[str, str]:
    match = FRONTMATTER_RE.match(text)
    if not match:
        raise ValueError("missing YAML frontmatter between --- markers")
    data: Dict[str, str] = {}
    for line in match.group("body").splitlines():
        if not line.strip() or line.strip().startswith("#"):
            continue
        if ":" not in line:
            raise ValueError(f"invalid frontmatter line: {line!r}")
        key, value = line.split(":", 1)
        data[key.strip()] = value.strip().strip('"').strip("'")
    missing = REQUIRED_FIELDS - data.keys()
    if missing:
        raise ValueError(f"missing required frontmatter field(s): {', '.join(sorted(missing))}")
    return data


def validate_skill_dir(path: Path) -> List[str]:
    errors: List[str] = []
    skill_file = path / "SKILL.md"
    if not skill_file.exists():
        return [f"{path}: missing SKILL.md"]
    try:
        text = skill_file.read_text(encoding="utf-8")
        metadata = parse_frontmatter(text)
        if len(metadata["description"]) > 240:
            errors.append(f"{skill_file}: description is long; keep it concise for triggering")
        expected_name = path.name
        if metadata["name"] != expected_name:
            errors.append(f"{skill_file}: name '{metadata['name']}' should match folder '{expected_name}'")
    except Exception as exc:  # pragma: no cover - message path matters more than type
        errors.append(f"{skill_file}: {exc}")
    return errors


def find_skill_dirs(root: Path) -> List[Path]:
    if (root / "SKILL.md").exists():
        return [root]
    return sorted(path for path in root.iterdir() if path.is_dir() and (path / "SKILL.md").exists())


def validate(root: str | Path) -> Tuple[bool, List[str]]:
    root_path = Path(root)
    if not root_path.exists():
        return False, [f"{root_path}: path does not exist"]
    skill_dirs = find_skill_dirs(root_path)
    if not skill_dirs:
        return False, [f"{root_path}: no skill directories with SKILL.md found"]
    errors: List[str] = []
    for skill_dir in skill_dirs:
        errors.extend(validate_skill_dir(skill_dir))
    return not errors, errors


def main(argv: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Validate Claude Skills.")
    parser.add_argument("root", help="Skill directory or parent .claude/skills directory")
    args = parser.parse_args(argv)

    ok, errors = validate(args.root)
    if ok:
        print(f"OK: validated skills under {args.root}")
        return 0
    for error in errors:
        print(f"ERROR: {error}")
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
