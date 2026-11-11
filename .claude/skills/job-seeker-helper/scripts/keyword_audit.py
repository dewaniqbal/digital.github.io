#!/usr/bin/env python3
"""Deterministic keyword audit for resume/CV vs job description.

This script is intentionally simple and dependency-free so Claude can run it
inside a local repo without network access.
"""
from __future__ import annotations

import argparse
import json
import re
from collections import Counter
from pathlib import Path
from typing import Dict, Iterable, List, Sequence

STOPWORDS = {
    "a", "an", "and", "are", "as", "at", "be", "by", "for", "from", "has",
    "have", "in", "into", "is", "it", "of", "on", "or", "our", "that", "the",
    "their", "this", "to", "we", "with", "you", "your", "will", "work", "role",
    "team", "teams", "candidate", "candidates", "experience", "skills", "ability",
    "strong", "using", "use", "used", "responsibilities", "requirements", "preferred",
    "must", "nice", "plus", "including", "across", "within", "about", "help", "support",
}

KNOWN_TERMS = {
    # Analysis / data
    "a/b testing", "ab testing", "analytics", "business intelligence", "dashboard",
    "dashboards", "data analysis", "data visualization", "etl", "excel", "looker",
    "power bi", "python", "r", "sql", "tableau", "statistics", "experimentation",
    "stakeholder communication", "forecasting", "kpi", "metrics", "reporting",
    # Product / business
    "roadmap", "go-to-market", "market research", "user research", "product strategy",
    "requirements gathering", "prioritization", "agile", "scrum", "jira", "figma",
    # Engineering / technical
    "api", "aws", "azure", "docker", "git", "github", "java", "javascript", "kubernetes",
    "linux", "node", "react", "rest", "typescript", "ci/cd", "testing",
    # Soft skills
    "communication", "collaboration", "cross-functional", "leadership", "problem solving",
    "project management", "stakeholders", "presentation", "documentation",
}


def normalize(text: str) -> str:
    """Normalize text for matching."""
    text = text.lower().replace("&", " and ")
    text = re.sub(r"[^a-z0-9+#./\-\s]", " ", text)
    text = re.sub(r"\s+", " ", text).strip()
    return text


def tokenize(text: str) -> List[str]:
    """Tokenize normalized text into meaningful words."""
    tokens = []
    for tok in normalize(text).split():
        clean = tok.strip(".,;:()[]{}")
        if len(clean) >= 3 and clean not in STOPWORDS:
            tokens.append(clean)
    return tokens


def contains_term(text: str, term: str) -> bool:
    """Return True when text contains a term with reasonable word boundaries."""
    text_norm = normalize(text)
    term_norm = normalize(term)
    if not term_norm:
        return False
    # Allow direct substring for terms containing symbols like c++ or ci/cd.
    if any(ch in term_norm for ch in "+#/.-"):
        return term_norm in text_norm
    return re.search(rf"(?<![a-z0-9]){re.escape(term_norm)}(?![a-z0-9])", text_norm) is not None


def extract_terms(job_text: str, max_terms: int = 80) -> List[Dict[str, object]]:
    """Extract candidate keywords from a job description."""
    found = Counter()
    normalized_job = normalize(job_text)

    for term in KNOWN_TERMS:
        if contains_term(normalized_job, term):
            canonical = "a/b testing" if term == "ab testing" else term
            found[canonical] += 3

    tokens = tokenize(job_text)
    token_counts = Counter(tokens)
    for token, count in token_counts.items():
        if count > 1 or token in {"sql", "python", "tableau", "excel", "aws", "react"}:
            found[token] += count

    # Add common two-word noun-ish phrases from the JD.
    for left, right in zip(tokens, tokens[1:]):
        phrase = f"{left} {right}"
        if left not in STOPWORDS and right not in STOPWORDS and len(left) >= 3 and len(right) >= 3:
            if phrase in KNOWN_TERMS:
                found[phrase] += 1

    terms = [
        {"term": term, "weight": weight}
        for term, weight in sorted(found.items(), key=lambda item: (-item[1], item[0]))
    ]
    return terms[:max_terms]


def audit(resume_text: str, job_text: str, max_terms: int = 80) -> Dict[str, object]:
    """Compare resume text against JD terms."""
    terms = extract_terms(job_text, max_terms=max_terms)
    matched = []
    missing = []

    for item in terms:
        term = str(item["term"])
        output_item = {"term": term, "weight": int(item["weight"])}
        if contains_term(resume_text, term):
            matched.append(output_item)
        else:
            missing.append(output_item)

    total_weight = sum(int(item["weight"]) for item in terms) or 1
    matched_weight = sum(int(item["weight"]) for item in matched)
    score = round((matched_weight / total_weight) * 100, 1)

    return {
        "coverage_score": score,
        "term_count": len(terms),
        "matched_terms": matched,
        "missing_terms": missing,
        "notes": [
            "Add missing keywords only when they truthfully reflect the candidate's experience.",
            "Treat this as a drafting aid, not an ATS guarantee.",
        ],
    }


def markdown_report(result: Dict[str, object]) -> str:
    """Render audit results as Markdown."""
    lines = [
        "# ATS Keyword Audit",
        "",
        f"## Coverage score",
        "",
        f"{result['coverage_score']}%",
        "",
        "## Matched terms",
        "",
    ]
    matched_terms: Sequence[Dict[str, object]] = result.get("matched_terms", [])  # type: ignore[assignment]
    missing_terms: Sequence[Dict[str, object]] = result.get("missing_terms", [])  # type: ignore[assignment]
    if matched_terms:
        lines.extend(f"- {item['term']}" for item in matched_terms)
    else:
        lines.append("- None detected")

    lines.extend(["", "## Missing terms to review", ""])
    if missing_terms:
        lines.extend(f"- {item['term']} — add only if true" for item in missing_terms)
    else:
        lines.append("- None detected")

    lines.extend(["", "## Notes", ""])
    for note in result.get("notes", []):
        lines.append(f"- {note}")
    lines.append("")
    return "\n".join(lines)


def read_text(path: str | Path) -> str:
    return Path(path).read_text(encoding="utf-8")


def main(argv: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Audit resume keywords against a job description.")
    parser.add_argument("--resume", required=True, help="Path to resume/CV text or Markdown file")
    parser.add_argument("--job", required=True, help="Path to job description text or Markdown file")
    parser.add_argument("--output", help="Optional path for Markdown report")
    parser.add_argument("--json", action="store_true", help="Print JSON instead of Markdown")
    args = parser.parse_args(argv)

    result = audit(read_text(args.resume), read_text(args.job))
    if args.json:
        rendered = json.dumps(result, indent=2)
    else:
        rendered = markdown_report(result)

    if args.output:
        Path(args.output).write_text(rendered, encoding="utf-8")
    else:
        print(rendered)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
