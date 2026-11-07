"""MkDocs hook: add the generated reference pages of the functions and iterators.

The pages are built from the Java sources at each build by
``docs/scripts/gen_reference.py``; they are not stored in the repository.
"""

from __future__ import annotations

import importlib.util
import sys
from pathlib import Path

from mkdocs.structure.files import File

_SCRIPT = Path(__file__).resolve().parents[1] / "scripts" / "gen_reference.py"


def _generator():
    spec = importlib.util.spec_from_file_location("gen_reference", _SCRIPT)
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module  # dataclasses look their module up
    spec.loader.exec_module(module)
    return module


def on_files(files, config, **kwargs):
    for path, content in _generator().generate().items():
        files.append(File.generated(config, path, content=content))
    return files
