"""MkDocs hook: expand the runnable examples of the documentation.

A line ``!example NAME`` in a page is replaced by the files of the example
``docs/examples/NAME``: the query, its named queries, documents and default
graph, and its expected output, in content tabs, followed by a link that opens
the example in the playground.

The same directories are executed by the test ``PlaygroundExamplesTest`` of
``sparql-generate-server``, which compares the output of the engine with the
expected output shown here.
"""

from __future__ import annotations

import json
import re
import textwrap
from pathlib import Path

import lzstring

EXAMPLES = Path(__file__).resolve().parents[1] / "examples"
PLAYGROUND = "https://ci.mines-stetienne.fr/sparql-generate/playground.html"
GITHUB = "https://github.com/sparql-generate/sparql-generate/tree/master/docs/examples/"
DIRECTIVE = re.compile(r"^!example\s+([\w-]+)\s*$", re.M)

LEXERS = {
    ".rqg": "sparql-generate",
    ".json": "json",
    ".geojson": "json",
    ".xml": "xml",
    ".html": "html",
    ".ttl": "turtle",
    ".csv": "text",
    ".txt": "text",
}


def _fence(text: str, lang: str) -> str:
    fence = "```"
    while fence in text:
        fence += "`"
    return f"{fence}{lang}\n{text.rstrip()}\n{fence}"


def _tab(title: str, body: str) -> str:
    return f'=== "{title}"\n\n' + textwrap.indent(body, "    ") + "\n"


def _term(binding: dict | None) -> str:
    if binding is None:
        return ""
    value = binding["value"]
    if binding["type"] == "uri":
        text = f"<{value}>"
    elif binding["type"] == "bnode":
        text = f"_:{value}"
    else:
        text = json.dumps(value, ensure_ascii=False)
        if "xml:lang" in binding:
            text += "@" + binding["xml:lang"]
        elif "datatype" in binding and not binding["datatype"].endswith("#string"):
            text += f"^^<{binding['datatype']}>"
    return "`" + text.replace("|", "\\|") + "`"


def _results_table(srj: dict) -> str:
    variables = srj["head"]["vars"]
    rows = ["| " + " | ".join(f"?{v}" for v in variables) + " |",
            "|" + "|".join(":--" for _ in variables) + "|"]
    for b in srj["results"]["bindings"]:
        rows.append("| " + " | ".join(_term(b.get(v)) for v in variables) + " |")
    return "\n".join(rows)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def load(name: str) -> dict:
    """The files of an example, as the playground request they make."""
    root = EXAMPLES / name
    if not root.is_dir():
        raise ValueError(f"no example {name} in {EXAMPLES}")
    conf_path = root / "sparql-generate-conf.json"
    conf = json.loads(_read(conf_path)) if conf_path.exists() else {}
    graph = root / conf.get("graph", "dataset/default.ttl")
    return {
        "root": root,
        "query": _read(root / conf.get("query", "query.rqg")),
        "namedqueries": [dict(q, string=_read(root / q["path"])) for q in conf.get("namedqueries", [])],
        "documentset": [dict(d, string=_read(root / d["path"])) for d in conf.get("documentset", [])],
        "defaultgraph": _read(graph) if graph.exists() else "",
    }


def playground_url(name: str, example: dict) -> str:
    message = {
        "readme": f"<p>Example <code>{name}</code> of the SPARQL-Generate documentation.</p>",
        "defaultquery": example["query"],
        "namedqueries": [{"uri": q["uri"], "mediatype": q.get("mediatype", "application/vnd.sparql-generate"),
                          "string": q["string"]} for q in example["namedqueries"]],
        "defaultgraph": example["defaultgraph"],
        "namedgraphs": [],
        "documentset": [{"uri": d["uri"], "mediatype": d.get("mediatype", "text/plain"), "string": d["string"]}
                        for d in example["documentset"]],
        "stream": False,
        "loglevel": 3,
    }
    compressed = lzstring.LZString().compressToEncodedURIComponent(json.dumps(message, ensure_ascii=False))
    return f"{PLAYGROUND}#msg={compressed}"


def render(name: str) -> str:
    example = load(name)
    root = example["root"]
    tabs = [_tab("Query", _fence(example["query"], "sparql-generate"))]
    for q in example["namedqueries"]:
        title = Path(q["path"]).name
        body = f"Named query `<{q['uri']}>`\n\n" + _fence(q["string"], "sparql-generate")
        tabs.append(_tab(title, body))
    for d in example["documentset"]:
        title = Path(d["path"]).name
        lexer = LEXERS.get(Path(d["path"]).suffix, "text")
        media = f" (`{d['mediatype']}`)" if d.get("mediatype") else ""
        body = f"Document `<{d['uri']}>`{media}\n\n" + _fence(d["string"], lexer)
        tabs.append(_tab(title, body))
    if example["defaultgraph"].strip():
        tabs.append(_tab("Default graph", _fence(example["defaultgraph"], "turtle")))
    if (root / "expected_output.ttl").exists():
        tabs.append(_tab("Output", _fence(_read(root / "expected_output.ttl"), "turtle")))
    elif (root / "expected_output.txt").exists():
        tabs.append(_tab("Output", _fence(_read(root / "expected_output.txt"), "text")))
    elif (root / "expected_output.srj").exists():
        tabs.append(_tab("Output", _results_table(json.loads(_read(root / "expected_output.srj")))))
    else:
        raise ValueError(f"example {name} has no expected output")
    links = (f"[Run in the playground]({playground_url(name, example)}){{ .md-button }} "
             f"[Example files]({GITHUB}{name}){{ .md-button }}")
    return "\n".join(tabs) + "\n" + links + "\n"


def on_page_markdown(markdown, page, config, files, **kwargs):
    return DIRECTIVE.sub(lambda m: render(m.group(1)), markdown)
