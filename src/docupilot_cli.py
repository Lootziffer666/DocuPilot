#!/usr/bin/env python3
"""DocuPilot intake prototype (dependency-light).

- Builds an inventory matrix for uploaded documents.
- Performs lightweight topic clustering and contradiction hints.
- Produces a modular execution schedule for downstream artifacts.
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import json
import re
from collections import Counter, defaultdict
from dataclasses import dataclass, asdict
from pathlib import Path
from typing import Iterable


TOPIC_HINTS = {
    "algorithms": ["algorithm", "modell", "pipeline", "heuristik", "regel"],
    "ui_logic": ["ui", "frontend", "button", "screen", "workflow", "ux"],
    "theoretical_hypotheses": ["hypothese", "theorie", "annahme", "methodik", "korpus"],
    "business_rules": ["anforderung", "regelmatrix", "compliance", "policy", "soll", "muss"],
    "paper_prep": ["abstract", "related work", "method", "results", "diskussion"],
}

CONTRADICTION_PATTERNS = [
    (re.compile(r"\bmust\b|\bmuss\b", re.IGNORECASE), re.compile(r"\bmust not\b|\bdarf nicht\b", re.IGNORECASE)),
    (re.compile(r"\benabled\b|\baktiv\b", re.IGNORECASE), re.compile(r"\bdisabled\b|\bdeaktiv\b", re.IGNORECASE)),
]


@dataclass
class FileFact:
    path: str
    sha1: str
    cluster: str
    signals: list[str]
    contradictions: list[str]


def read_text(path: Path) -> str:
    try:
        return path.read_text(encoding="utf-8", errors="ignore")
    except Exception:
        return ""


def classify(text: str) -> tuple[str, list[str]]:
    lowered = text.lower()
    scores = Counter()
    signals = []
    for cluster, hints in TOPIC_HINTS.items():
        for hint in hints:
            if hint in lowered:
                scores[cluster] += 1
                signals.append(hint)
    if not scores:
        return "unclassified", []
    return scores.most_common(1)[0][0], sorted(set(signals))


def contradiction_hints(text: str) -> list[str]:
    hints = []
    for left, right in CONTRADICTION_PATTERNS:
        if left.search(text) and right.search(text):
            hints.append(f"Potential internal contradiction: '{left.pattern}' vs '{right.pattern}'")
    return hints


def build_inventory(files: Iterable[Path]) -> list[FileFact]:
    facts: list[FileFact] = []
    for path in files:
        text = read_text(path)
        cluster, signals = classify(text)
        contradictions = contradiction_hints(text)
        sha1 = hashlib.sha1(text.encode("utf-8", errors="ignore")).hexdigest() if text else ""
        facts.append(FileFact(str(path), sha1, cluster, signals, contradictions))
    return facts


def write_outputs(facts: list[FileFact], out_dir: Path) -> None:
    out_dir.mkdir(parents=True, exist_ok=True)
    json_path = out_dir / "inventory_matrix.json"
    csv_path = out_dir / "inventory_matrix.csv"
    schedule_path = out_dir / "modular_schedule.json"

    with json_path.open("w", encoding="utf-8") as f:
        json.dump([asdict(x) for x in facts], f, ensure_ascii=False, indent=2)

    with csv_path.open("w", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=["path", "sha1", "cluster", "signals", "contradictions"])
        writer.writeheader()
        for fact in facts:
            writer.writerow(
                {
                    "path": fact.path,
                    "sha1": fact.sha1,
                    "cluster": fact.cluster,
                    "signals": "; ".join(fact.signals),
                    "contradictions": " | ".join(fact.contradictions),
                }
            )

    schedule = {
        "phase_1_scan_inventory": [
            "ingest_files",
            "cluster_topics",
            "detect_contradictions",
            "build_inventory_matrix",
        ],
        "phase_2_modular_schedule": {
            "master_overview": ["status extraction", "project dashboard synthesis"],
            "paper_prep": ["method/data extraction", "citation map"],
            "technical_prd": ["functional requirements", "rule matrix consolidation"],
            "change_log": ["content hash diff", "human-readable delta summary"],
        },
        "phase_3_context_isolation": {
            "master_overview": ["all clusters except drafts older than active milestone"],
            "paper_prep": ["theoretical_hypotheses", "paper_prep", "selected data files"],
            "technical_prd": ["algorithms", "business_rules", "ui_logic"],
            "change_log": ["old/new file versions only"],
        },
    }
    with schedule_path.open("w", encoding="utf-8") as f:
        json.dump(schedule, f, ensure_ascii=False, indent=2)


def collect_files(root: Path, extensions: set[str]) -> list[Path]:
    files = []
    for path in root.rglob("*"):
        if path.is_file() and (not extensions or path.suffix.lower() in extensions):
            files.append(path)
    return sorted(files)


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description="DocuPilot inventory prototype")
    p.add_argument("root", nargs="?", default=".", help="Folder with uploaded files")
    p.add_argument("--ext", nargs="*", default=[".md", ".txt", ".rst", ".csv"], help="Extensions to scan")
    p.add_argument("--out", default=".docupilot", help="Output folder")
    return p.parse_args()


def main() -> int:
    args = parse_args()
    root = Path(args.root)
    out = Path(args.out)
    exts = {x if x.startswith(".") else f".{x}" for x in args.ext}

    files = collect_files(root, exts)
    facts = build_inventory(files)
    write_outputs(facts, out)

    clusters = defaultdict(int)
    for fact in facts:
        clusters[fact.cluster] += 1

    print(f"Scanned {len(facts)} files. Output written to {out}")
    print("Cluster distribution:")
    for cluster, count in sorted(clusters.items(), key=lambda x: (-x[1], x[0])):
        print(f"- {cluster}: {count}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
