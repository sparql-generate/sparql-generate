# SPARQL-Generate

SPARQL-Generate is an extension of SPARQL 1.1 to generate RDF or text from RDF datasets and from documents in heterogeneous formats: JSON, CSV, XML, HTML, GeoJSON, CBOR, plain text, SQL databases, Markdown, and streams from MQTT, WebSocket or repeated HTTP requests.

A SPARQL-Generate query is a SPARQL query with three additions:

- the query forms `GENERATE` (RDF output), `TEMPLATE` (text output) and `FUNCTION` (a function defined by a query), next to `SELECT`;
- the binding clauses `SOURCE` and `ITERATOR`, which bind variables to documents and to the parts of documents, before the `WHERE` clause;
- expressions embedded in IRIs and literals of the templates, such as `<person/{?id}>` or `"{?first} {?last}"`.

The query below reads a CSV document and generates one resource per row.

!example iterator-csv

The reference implementation is a Java engine on Apache Jena, available as an executable JAR, as Maven artifacts, and in a web playground.

## Where to go next

| To | Read |
|:--|:--|
| run a first query | [Getting started](getting-started.md) |
| learn the language step by step | the [tutorial](tutorial.md), in the playground |
| look up a construct | the [language guide](language/index.md) |
| look up a function or an iterator | the [reference](reference/index.md) |
| use the engine from a program | the [command line](cli.md) and the [Java API](java-api.md) |
| translate RML mappings | [From RML](rml.md) |

## Project

SPARQL-Generate is developed at [MINES Saint-Étienne](https://www.mines-stetienne.fr/) since 2016. The source code is on [GitHub](https://github.com/sparql-generate/sparql-generate) under the Apache License 2.0. The language and its semantics are described in the [publications](publications.md). Questions and bug reports go to the [issue tracker](https://github.com/sparql-generate/sparql-generate/issues).

Contributors: Maxime Lefrançois (lead), Antoine Zimmermann, Mohammad-Noorani Bakerally, El Mehdi Khalfi, Omar Qawasmeh.

This work has been partly funded by the ITEA2 12004 SEAS project, the ANR 14-CE24-0029 OpenSensingCity project, ETSI STF 578, bilateral research contracts with ENGIE R&D CRIGEN, the ANR 19-CE23-0012 CoSWoT project, and the ANR HyperAgents project.
