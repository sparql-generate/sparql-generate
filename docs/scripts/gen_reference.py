#!/usr/bin/env python3
"""Generate the reference of the SPARQL-Generate functions and iterators.

The script reads the Java sources of the engine: the registrations in
``SPARQLExt.java`` and in the ``FunctionsLoader.java`` of each module
(``fnreg.put(FUN_X.URI, FUN_X.class)``, ``itereg.put(ITER_X.URI, ...)``), the
``URI`` constant of each registered class, and its class Javadoc. It writes one
Markdown page for the functions and one for the iterators.

The MkDocs hook ``docs/hooks/reference.py`` calls :func:`generate` at each
build, so the pages are never edited by hand. Run the script directly to
write the pages to a directory::

    python3 docs/scripts/gen_reference.py [output-directory]
"""

from __future__ import annotations

import html
import re
import sys
from dataclasses import dataclass, field
from html.parser import HTMLParser
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
GITHUB = "https://github.com/sparql-generate/sparql-generate/blob/master/"

NAMESPACES = {
    "SPARQLExt.FUN": "http://w3id.org/sparql-generate/fn/",
    "SPARQLExt.ITER": "http://w3id.org/sparql-generate/iter/",
    "ST.NS": "http://ns.inria.fr/sparql-template/",
}
PREFIXES = {
    "http://w3id.org/sparql-generate/fn/": "fun",
    "http://w3id.org/sparql-generate/iter/": "iter",
    "http://ns.inria.fr/sparql-template/": "st",
}

MODULES = [
    ("sparql-generate-jena", "Core (sparql-generate-jena)"),
    ("sparql-generate-csv", "CSV (sparql-generate-csv)"),
    ("sparql-generate-json", "JSON and CBOR (sparql-generate-json)"),
    ("sparql-generate-xml", "XML and HTML (sparql-generate-xml)"),
    ("sparql-generate-geojson", "GeoJSON (sparql-generate-geojson)"),
    ("sparql-generate-sql", "SQL (sparql-generate-sql)"),
    ("sparql-generate-markdown", "Markdown (sparql-generate-markdown)"),
    ("sparql-generate-webapis", "Web APIs (sparql-generate-webapis)"),
    ("sparql-generate-mqtt", "MQTT (sparql-generate-mqtt)"),
    ("sparql-generate-ws", "WebSocket (sparql-generate-ws)"),
    ("sparql-generate-binary", "Binary data (sparql-generate-binary)"),
]

PUT = re.compile(r"\b(\w+)\.put\(\s*(\w+)\.URI\s*,\s*(\w+)\.class\s*\)")
URI_DECL = re.compile(r"static\s+(?:final\s+)?String\s+URI\s*=\s*([^;]+);")
ST_CONST = re.compile(r"static\s+final\s+String\s+(\w+)\s*=\s*NS\s*\+\s*\"([^\"]*)\"\s*;")
CLASS_DOC = re.compile(r"/\*\*((?:(?!\*/).)*)\*/\s*(?:@\w+(?:\([^)]*\))?\s*)*public\s+(?:final\s+)?(?:abstract\s+)?class\s+(\w+)", re.S)


@dataclass
class Entry:
    kind: str  # "function" or "iterator"
    uri: str
    java_class: str
    module: str
    source: Path
    doc: str = ""

    @property
    def local_name(self) -> str:
        for ns in PREFIXES:
            if self.uri.startswith(ns):
                return self.uri[len(ns):]
        return self.uri.rsplit("/", 1)[-1]

    @property
    def qname(self) -> str:
        for ns, prefix in PREFIXES.items():
            if self.uri.startswith(ns):
                return f"{prefix}:{self.uri[len(ns):]}"
        return f"<{self.uri}>"


def st_constants() -> dict[str, str]:
    st = REPO / "sparql-generate-jena/src/main/java/fr/mines_stetienne/ci/sparql_generate/utils/ST.java"
    text = st.read_text(encoding="utf-8")
    return {name: NAMESPACES["ST.NS"] + local for name, local in ST_CONST.findall(text)}


def resolve_uri(expr: str, st: dict[str, str]) -> str | None:
    expr = expr.strip()
    m = re.fullmatch(r"([\w.]+)\s*\+\s*\"([^\"]*)\"", expr)
    if m and m.group(1) in NAMESPACES:
        return NAMESPACES[m.group(1)] + m.group(2)
    m = re.fullmatch(r"\"([^\"]*)\"", expr)
    if m:
        return m.group(1)
    m = re.fullmatch(r"ST\.(\w+)", expr)
    if m and m.group(1) in st:
        return st[m.group(1)]
    return None


def find_class(module_dir: Path, name: str) -> Path | None:
    matches = sorted(module_dir.glob(f"src/main/java/**/{name}.java"))
    return matches[0] if matches else None


def class_javadoc(source: Path, name: str) -> str:
    text = source.read_text(encoding="utf-8")
    for doc, cls in CLASS_DOC.findall(text):
        if cls == name:
            lines = [re.sub(r"^\s*\* ?", "", line) for line in doc.splitlines()]
            body = []
            for line in lines:
                if re.match(r"\s*@(author|since|version|see)\b", line):
                    break
                body.append(line)
            return "\n".join(body).strip()
    return ""


def registrations(module_dir: Path) -> list[tuple[str, str]]:
    """The (registry, class) pairs registered by a module."""
    if module_dir.name == "sparql-generate-jena":
        loaders = [module_dir / "src/main/java/fr/mines_stetienne/ci/sparql_generate/SPARQLExt.java"]
    else:
        loaders = sorted(module_dir.glob("src/main/java/**/FunctionsLoader.java"))
    result = []
    for loader in loaders:
        for registry, uri_class, cls in PUT.findall(loader.read_text(encoding="utf-8")):
            if uri_class == cls:
                result.append((registry, cls))
    return result


def collect() -> list[Entry]:
    st = st_constants()
    entries = []
    for module, _title in MODULES:
        module_dir = REPO / module
        for registry, cls in registrations(module_dir):
            source = find_class(module_dir, cls)
            if source is None:
                raise SystemExit(f"{module}: class {cls} not found")
            m = URI_DECL.search(source.read_text(encoding="utf-8"))
            uri = resolve_uri(m.group(1), st) if m else None
            if uri is None:
                raise SystemExit(f"{source}: cannot resolve the URI constant")
            kind = "iterator" if "iter" in registry.lower() else "function"
            entries.append(Entry(kind, uri, cls, module, source, class_javadoc(source, cls)))
    return entries


class _JavadocToMarkdown(HTMLParser):
    """Converts the HTML of a Javadoc comment to Markdown."""

    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.out: list[str] = []
        self.lists: list[str] = []
        self.links: list[str | None] = []
        self.in_pre = False
        self.in_code = 0

    def emit(self, text: str):
        self.out.append(text)

    def newline(self, blank: bool = False):
        self.emit("\n\n" if blank else "\n")

    def handle_starttag(self, tag, attrs):
        tag = tag.lower()
        attrs = dict(attrs)
        if tag == "p":
            self.newline(blank=not self.lists)
        elif tag in ("ul", "ol"):
            self.lists.append(tag)
            self.newline(blank=len(self.lists) == 1)
        elif tag == "li":
            self.newline()
            indent = "    " * (len(self.lists) - 1)
            bullet = "1." if self.lists and self.lists[-1] == "ol" else "-"
            self.emit(f"{indent}{bullet} ")
        elif tag == "pre":
            self.in_pre = True
            self.emit("\n\n```\n")
        elif tag in ("code", "tt") and not self.in_pre:
            self.in_code += 1
            self.emit("`")
        elif tag in ("b", "strong"):
            self.emit("**")
        elif tag in ("i", "em"):
            self.emit("*")
        elif tag == "br":
            self.newline()
        elif tag == "a":
            href = attrs.get("href")
            self.links.append(href)
            if href:
                self.emit("[")

    def handle_endtag(self, tag):
        tag = tag.lower()
        if tag in ("ul", "ol"):
            if self.lists:
                self.lists.pop()
            self.newline(blank=not self.lists)
        elif tag == "p":
            self.newline(blank=not self.lists)
        elif tag == "pre":
            self.in_pre = False
            self.emit("\n```\n\n")
        elif tag in ("code", "tt") and not self.in_pre and self.in_code:
            self.in_code -= 1
            self.emit("`")
        elif tag in ("b", "strong"):
            self.emit("**")
        elif tag in ("i", "em"):
            self.emit("*")
        elif tag == "a":
            href = self.links.pop() if self.links else None
            if href:
                self.emit(f"]({href})")

    def handle_data(self, data):
        if self.in_pre:
            self.emit(data)
            return
        data = re.sub(r"\s+", " ", data)
        if not self.in_code:
            data = re.sub(r"([\\*_\[\]])", r"\\\1", data)
        self.emit(data)

    def markdown(self) -> str:
        text = "".join(self.out)
        lines = [line.rstrip() for line in text.splitlines()]
        text = "\n".join(lines)
        text = re.sub(r"\n{3,}", "\n\n", text)
        text = re.sub(r"(?m)^ (?=\S)", "", text)
        # Markdown emphasis does not allow spaces inside the markers
        text = re.sub(r"\*\*(\s*)([^*\n]*?)(\s*)\*\*", r"\1**\2**\3", text)
        return text.strip()


def javadoc_to_markdown(doc: str) -> str:
    doc = re.sub(r"\{@(?:link|linkplain)\s+([^}\s]+)(?:\s+([^}]*))?\}",
                 lambda m: "<code>" + html.escape(m.group(2) or m.group(1).lstrip("#")) + "</code>", doc)
    doc = re.sub(r"\{@code\s+([^}]*)\}", lambda m: "<code>" + html.escape(m.group(1)) + "</code>", doc)
    doc = re.sub(r"\{@literal\s+([^}]*)\}", lambda m: html.escape(m.group(1)), doc)
    # the old documentation links to the Javadoc pages and to the 2.0 playground
    doc = doc.replace("https://w3id.org/sparql-generate/playground.html",
                      "https://ci.mines-stetienne.fr/sparql-generate/playground.html")
    parser = _JavadocToMarkdown()
    parser.feed(doc)
    parser.close()
    return parser.markdown()


def summary(markdown: str) -> str:
    """The first sentence of a description, for the index table."""
    first = markdown.split("\n\n", 1)[0].replace("\n", " ")
    first = re.sub(r"^(Iterator function|Binding function|Function)\s+\[[^\]]+\]\([^)]+\)\s*", "", first)
    first = re.sub(r"^https?://\S+\s+sec\.\s+[\d.]+\s*", "", first)
    first = re.sub(r"\]\([^)]*\)", "]", first).replace("[", "").replace("]", "")
    m = re.match(r"(.+?\.)(\s|$)", first)
    sentence = (m.group(1) if m else first).strip()
    sentence = sentence[:1].upper() + sentence[1:]
    return sentence.replace("|", "\\|")


def page(kind: str, entries: list[Entry]) -> str:
    title = "Functions" if kind == "function" else "Iterators"
    intro = {
        "function": (
            "A function returns zero or one RDF term. It is called in any SPARQL expression: "
            "`BIND`, `FILTER`, the `SELECT` clause, or an expression embedded in a template. "
            "The namespace of the SPARQL-Generate functions is `http://w3id.org/sparql-generate/fn/` "
            "(prefix `fun:`); the STTL functions `st:` come from "
            "[SPARQL Template](http://ns.inria.fr/sparql-template/)."
        ),
        "iterator": (
            "An iterator function is called in an `ITERATOR` clause. It returns a list of rows, and "
            "binds the variables after `AS` to the values of each row. The namespace of the iterators "
            "is `http://w3id.org/sparql-generate/iter/` (prefix `iter:`)."
        ),
    }[kind]
    out = [
        f"# {title}",
        "",
        "<!-- Generated by docs/scripts/gen_reference.py from the Java sources. Do not edit. -->",
        "",
        intro,
        "",
        "Each module is a Maven artifact of group `fr.mines-stetienne.ci.sparql-generate`; "
        "`sparql-generate-all` and the executable JAR contain them all. "
        "This page is generated from the Javadoc of the classes that implement them.",
        "",
    ]
    for module, module_title in MODULES:
        group = sorted((e for e in entries if e.module == module and e.kind == kind),
                       key=lambda e: e.local_name.lower())
        if not group:
            continue
        out += [f"## {module_title}", "", "| Name | Summary |", "|:--|:--|"]
        for e in group:
            out.append(f"| [`{e.qname}`](#{anchor(e)}) | {summary(javadoc_to_markdown(e.doc))} |")
        out.append("")
        for e in group:
            source = GITHUB + e.source.relative_to(REPO).as_posix()
            out += [
                f"### {e.qname} {{ #{anchor(e)} }}",
                "",
                f"IRI `{e.uri}` · module `{e.module}` · class [`{e.java_class}`]({source})",
                "",
                javadoc_to_markdown(e.doc) or "*No description in the sources.*",
                "",
            ]
    return "\n".join(out).rstrip() + "\n"


def anchor(entry: Entry) -> str:
    return re.sub(r"[^A-Za-z0-9_-]", "-", entry.qname.replace(":", "-"))


def generate() -> dict[str, str]:
    """The generated pages, by path relative to the documentation directory."""
    entries = collect()
    return {
        "reference/functions.md": page("function", entries),
        "reference/iterators.md": page("iterator", entries),
    }


def main(argv: list[str]) -> int:
    out_dir = Path(argv[1]) if len(argv) > 1 else Path(".")
    for path, content in generate().items():
        target = out_dir / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(content, encoding="utf-8")
        print(target)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
