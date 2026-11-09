#!/usr/bin/env python3
"""Offline quality gate for licensed JSONL recipe candidates; never imports to production."""
import argparse
import collections
import json
from pathlib import Path

def validate(rows, minimum=1000):
    errors = []
    seen = set()
    chinese = 0
    cuisines = collections.Counter()
    for number, row in enumerate(rows, 1):
        key = (row.get("source"), row.get("source_id"))
        if not all(key) or key in seen:
            errors.append(f"{number}: missing or duplicate source identity")
        seen.add(key)
        for field in ("title", "cuisine", "source_url", "author", "license", "license_url", "instructions"):
            if not row.get(field):
                errors.append(f"{number}: missing {field}")
        if row.get("rights_verified") is not True:
            errors.append(f"{number}: reuse rights have not been reviewed")
        images = row.get("images", [])
        covers = [i for i in images if i.get("role") == "cover"]
        steps = [i for i in images if i.get("role") == "step" and i.get("step_number")]
        if not covers or not steps:
            errors.append(f"{number}: requires a cover and a real step image")
        if covers and steps and set(i.get("sha256") for i in covers) & set(i.get("sha256") for i in steps):
            errors.append(f"{number}: cover cannot double as a step image")
        for image in images:
            if not all(image.get(k) for k in ("source_url", "author", "license", "license_url", "sha256")):
                errors.append(f"{number}: incomplete image provenance")
        popularity = row.get("popularity", {})
        if not all(popularity.get(k) for k in ("metric", "source_url", "observed_at")) or not isinstance(popularity.get("value"), (int, float)):
            errors.append(f"{number}: missing verifiable popularity evidence")
        chinese += row.get("cuisine_family") == "Chinese"
        cuisines[row.get("cuisine", "unknown")] += 1
    if len(rows) < minimum:
        errors.append(f"Only {len(rows)} candidates; need at least {minimum}")
    if chinese * 2 < len(rows):
        errors.append("Chinese cuisines account for less than half")
    return {"total": len(rows), "chinese": chinese, "cuisines": dict(cuisines), "errors": errors}

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("manifest", type=Path)
    parser.add_argument("--minimum", type=int, default=1000)
    args = parser.parse_args()
    rows = [json.loads(line) for line in args.manifest.read_text().splitlines() if line.strip()]
    report = validate(rows, args.minimum)
    print(json.dumps(report, ensure_ascii=False, indent=2))
    raise SystemExit(bool(report["errors"]))
