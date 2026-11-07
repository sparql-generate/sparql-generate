# Compared to SPARQL Anything

[SPARQL Anything](https://sparql-anything.cc/) and SPARQL-Generate both query non-RDF documents with SPARQL. They differ in how a document enters the query.

## Two approaches

**SPARQL-Generate** (2016) adds clauses to SPARQL. `SOURCE` binds a variable to the content of a document, `ITERATOR` binds variables to the parts of a document extracted by an iterator function (JSONPath, XPath, CSV columns, regular expressions...), and functions extract values in expressions. The document is never turned into RDF: its values are bound to variables, and the template builds the output. The query forms `GENERATE`, `TEMPLATE` and `FUNCTION` are new syntax, defined by the grammar of the language and its semantics.

**SPARQL Anything** (2021) keeps the syntax of SPARQL 1.1. A `SERVICE` clause with an IRI of the form `<x-sparql-anything:location=...>` designates a document; the engine views it as an RDF graph built according to the Façade-X model (containers for arrays and rows, properties for keys and columns), and the graph pattern inside the `SERVICE` clause queries that view. The output is produced by standard `CONSTRUCT` or `SELECT` queries.

## Side by side

| | SPARQL-Generate | SPARQL Anything |
|:--|:--|:--|
| Syntax | SPARQL 1.1 plus `GENERATE`, `TEMPLATE`, `FUNCTION`, `SOURCE`, `ITERATOR`, embedded expressions | SPARQL 1.1; options in the `SERVICE` IRI or in triples |
| Access to a document | values bound by iterators and functions | graph patterns over the Façade-X view of the document |
| Text output | `TEMPLATE` queries | none; RDF and result tables |
| Modularity | named queries with parameters, called from templates, iterators and expressions | none in the language |
| Streams | iterators over MQTT, WebSocket and repeated HTTP requests; batched CSV and JSON iterators; streamed output | streamed `CONSTRUCT` output and slicing options (since 1.2) |
| Formats | JSON, CSV, XML, HTML, GeoJSON, CBOR, SQL, Markdown, text, binary | JSON, CSV, XML, HTML, Markdown, YAML, spreadsheets, Word documents, file systems, archives, image metadata, and others |
| Engine | Java, Apache Jena | Java, Apache Jena |

## Choosing

SPARQL Anything suits queries written by users who know SPARQL and want to explore a document as a graph without learning new clauses, and the formats that SPARQL-Generate does not read (spreadsheets, office documents). SPARQL-Generate suits transformations that control the iteration (which parts of a document, in which order, joined with which other source), that produce text, that are split into reusable parametrised queries, or that consume streams.

## Sources

- E. Daga, L. Asprino, P. Mulholland, A. Gangemi. *Facade-X: An Opinionated Approach to SPARQL Anything*. SEMANTiCS 2021.
- L. Asprino, E. Daga, A. Gangemi, P. Mulholland. *Knowledge Graph Construction with a Façade: A Unified Method to Access Heterogeneous Data Sources on the Web*. ACM Transactions on Internet Technology 23(1), 2023.
- The [SPARQL Anything documentation](https://sparql-anything.cc/), and the [publications](publications.md) of SPARQL-Generate.
