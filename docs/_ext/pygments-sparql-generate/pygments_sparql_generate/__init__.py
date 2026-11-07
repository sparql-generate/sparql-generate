"""Pygments lexer for SPARQL-Generate (``.rqg``).

Installed, it registers the alias ``sparql-generate`` with Pygments (entry
point ``pygments.lexers``), so ``pygmentize -l sparql-generate``, LaTeX
minted, MkDocs and Sphinx find it by name.
"""

from .lexer import SparqlGenerateLexer

__all__ = ["SparqlGenerateLexer"]
__version__ = "0.1.0"
