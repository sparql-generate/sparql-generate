"""Pygments lexer for SPARQL-Generate.

SPARQL-Generate is SPARQL 1.1 plus a closed set of additions. This lexer is
Pygments' own ``SparqlLexer`` with only those additions layered on top, so a
plain SPARQL 1.1 query gets exactly the tokens ``SparqlLexer`` gives it (the
parity test of ``tests/test_lexer.py``), except for the one known defect of
the base lexer listed there.

The additions come from ``_lexicon.py``, generated from ``src/lexicon.js`` of
the highlight-sparql-generate repository: the lexicon is written once, for
every engine.

- the keywords of SPARQL-Generate (``GENERATE``, ``TEMPLATE``, ``ITERATOR``,
  ``SOURCE`` ... ``ACCEPT``, ``BOX``, ``FORMAT`` ...), and ``GROUP`` alone (a
  template group), which SPARQL only knows in ``GROUP BY``;
- expression nodes ``?{ expr }`` and ``${ expr }``;
- expressions embedded in IRIs (``<http://ex/{?id}>``) and in string
  literals (``"Hello {?name}"``), in all four quote styles. ``\\{`` and
  ``\\}`` keep a literal brace;
- the library functions and iterators, by their conventional prefixes
  ``fun:`` and ``iter:``.

Usage::

    pygmentize -l sparql-generate query.rqg
    \\begin{minted}{sparql-generate} ... \\end{minted}     % LaTeX
"""

import copy
import re

from pygments.lexer import default, include
from pygments.lexers.rdf import SparqlLexer
from pygments.token import Keyword, Name, Punctuation, String

from . import _lexicon as L

__all__ = ["SparqlGenerateLexer"]

# The interpolation delimiters: `{` `}` inside an IRI or a string, and the
# `?{` / `${` ... `}` of an expression node.
INTERPOL = String.Interpol


def _ci(source):
    """A case-insensitive pattern, the Pygments way (a leading flag)."""
    return "(?i)" + source


def _string_states(base):
    """The four string states of SparqlLexer, taught to stop at ``{``.

    Short strings: the content rule of the base, with ``{`` added to its
    negated class, and an interpolation rule in front. Long strings: the base
    content rule ``[^\\\\]+`` runs past the closing quotes to the end of the
    input (a defect of SparqlLexer, see the parity test), so the content rule
    is rewritten to stop at the closing quotes as well as at ``{``.
    """
    long_content = {
        "triple-double-quoted-string": r'(?:[^\\"{]|"(?!""))+',
        "triple-single-quoted-string": r"(?:[^\\'{]|'(?!''))+",
    }
    states = {}
    for name in ("single-double-quoted-string", "single-single-quoted-string",
                 "triple-double-quoted-string", "triple-single-quoted-string"):
        rules = []
        for rule in base[name]:
            regex = rule[0]
            if name in long_content and regex == r"[^\\]+":
                rule = (long_content[name],) + tuple(rule[1:])
            elif isinstance(regex, str) and regex.startswith("[^") and regex.endswith("]+"):
                rule = ("[^{" + regex[2:],) + tuple(rule[1:])
            rules.append(rule)
        states[name] = [(r"\{", INTERPOL, "sg-expression")] + rules
    return states


def _tokens():
    base = copy.deepcopy(SparqlLexer.tokens)
    root = base["root"]
    # Insert the additions just after the base keyword rule (index 1, after
    # whitespace): the base keeps the words it knows (`group\s+by` stays one
    # SPARQL token), ours only ever match where the base has no rule.
    head, tail = root[:2], root[2:]
    additions = [
        (_ci(L.P["generateKeywords"]), Keyword),
        (_ci(L.P["reusedKeywords"]), Keyword),
        (L.P["libraryName"], Name.Function),
        (L.P["xexprOpen"], INTERPOL, "sg-expression"),
        (L.P["xiriStart"], Name.Label, "sg-iri"),
    ]
    base["root"] = head + additions + tail
    base.update(_string_states(base))
    base["sg-expression"] = [
        (r"\}", INTERPOL, "#pop"),
        (r"\{", Punctuation, "sg-group"),
        include("root"),
    ]
    # A group inside an expression: `{ IF(EXISTS { ?s ?p ?o }, 1, 0) }`.
    base["sg-group"] = [
        (r"\}", Punctuation, "#pop"),
        (r"\{", Punctuation, "#push"),
        include("root"),
    ]
    base["sg-iri"] = [
        (L.P["iriChar"] + "+", Name.Label),
        (r"\{", INTERPOL, "sg-expression"),
        (r">", Name.Label, "#pop"),
        default("#pop"),
    ]
    return base


class SparqlGenerateLexer(SparqlLexer):
    """Lexer for SPARQL-Generate queries (``.rqg``)."""

    name = "SPARQL-Generate"
    aliases = ["sparql-generate", "sparqlgenerate", "rqg"]
    filenames = ["*.rqg"]
    mimetypes = ["application/vnd.sparql-generate"]
    url = "https://ci.mines-stetienne.fr/sparql-generate/"
    version_added = ""

    tokens = _tokens()

    def analyse_text(text):
        """A query that uses a SPARQL-Generate form is ours, not SPARQL's."""
        if re.search(r"(?im)^\s*(GENERATE|TEMPLATE|PERFORM|FUNCTION)\b", text):
            return 0.6
        if re.search(r"(?i)\b(ITERATOR|SOURCE)\s+[?$<\w]", text):
            return 0.4
        return 0.0
