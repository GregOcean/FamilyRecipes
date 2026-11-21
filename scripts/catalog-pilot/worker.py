#!/usr/bin/env python3
"""CLI compatibility entrypoint; production implementation is bundled with the backend."""
from pathlib import Path
_source = Path(__file__).resolve().parents[2] / 'backend/src/main/resources/catalog/worker.py'
exec(compile(_source.read_text(), str(_source), 'exec'), globals())
