# Changelog

This page summarises the changes of the engine since version 2.0. The full history is in the [commits](https://github.com/sparql-generate/sparql-generate/commits/master) and the [releases](https://github.com/sparql-generate/sparql-generate/releases).

## 2.3.0

Not yet released.

**Language and engine**

- **Changed**: a JSON number written with a decimal point and without exponent (`0.1`, `-65.814436`) is an `xsd:decimal`, with its exact value; a number with an exponent (`1.5e3`) is an `xsd:double`, an integer an `xsd:integer`. It was an `xsd:double`. This holds for `fun:JSONPath`, `iter:JSONPath`, `iter:JSONSurfer`, `fun:CBOR` and `iter:CBOR`, whatever the default configuration of JSONPath.
- An `OutOfMemoryError` in an iterator stops the execution with status 1 and a message; the CSV parser reported it as a parsing error, and the execution went on with part of the rows.

**Performance**

- Consecutive `BIND` clauses after an `ITERATOR` run in one pass, each row extended by one flat binding: as fast as the same `BIND` in the `WHERE` clause.

**Command line**

- The N-Triples (and N-Quads) output without `--stream` is written as the triples are generated, each one once, without building the graph in memory: the flat CSV workload of 1 M rows (5 M triples) now runs with a heap of 2 GB. The other formats still build the graph.

## 2.2.4

Released on 2026-11-07. Faster evaluation of the queries translated from RML and of nested queries, and fixes; two changes of behaviour, marked **Changed**.

**Performance**

- A `WHERE` clause made of `BIND` and `FILTER`, without solution modifier, is evaluated without Jena's query engine; a nested `GENERATE` receives the variables of its parent instead of computing them again; a `BIND` clause no longer builds a log message for each binding. The bookkeeping of the batches of an iterator in a sub-query, run once per solution of the parent, is linear; it was quadratic. The query translated from RML for the KGCW 2024 task records-1M (20 M triples): 219 s to 30 s; the flat CSV mapping translated from RML at 8 M triples: 61 s to 16 s; a CSV of 1 M rows with a nested `GENERATE` over `iter:Split`: 54 s to 11 s; the same in JSON: 58 s to 14 s.

**Language and engine**

- `fun:JSONPath` on a JSON `null` leaves the variable unbound; the whole query failed with an internal error.
- `fun:CBOR` and `iter:CBOR` read CBOR; they parsed the decoded bytes as JSON text, which is still accepted.
- **Changed**: `iter:JSONSurfer` gives the integers of a JSON document as `xsd:integer`, as `iter:JSONPath` does; it gave them as `xsd:double`. An integer beyond 64 bits is an `xsd:integer` for `fun:JSONPath` and `iter:JSONPath` (it left the variable unbound).
- `iter:CSV` with a batch size no longer sends an empty last batch, on which an aggregate gave one more solution.
- `fun:HTTPExtractBody`, `fun:HTTPExtractHeader` and `fun:HTTPExtractResponseCode` accept the response of a server of any version of HTTP, not only HTTP/1.1.
- `iter:HTTPGet` types each response with its media type, `https://www.iana.org/assignments/media-types/application/json` (the prefix was doubled), and no longer waits after its last call.
- **Changed**: `iter:SQL` connects with the JDBC URL alone, with the user and password it carries, and any JDBC driver of the classpath; it used the user `root` and the password `password`, and only MySQL. The rows of the result are one batch; each row was a batch of its own.

## 2.2.3

Released on 2026-11-07. Fixes of the defects found by the benchmark and by the formal semantics, a few changes of behaviour (marked **Changed**), and a faster evaluation.

**Language and engine**

- `SELECT DISTINCT` (and `REDUCED`, and expressions in the projection) without `WHERE` clause apply to the solutions of the binding clauses; they were ignored.

- An unbound argument in a call to a named `SELECT` query, `ITERATOR <q.rqg>(?u)`, leaves the parameter unbound instead of failing.
- `"a\\}b"` is read as `a\}b`: the escape `\\` is read before the brace.
- A blank node at the end of a property path in the `WHERE` clause of a sub-query, `?s <p>|<q> []`, no longer breaks the query.
- The functions that Jena loads by class name are available: `afn:bnode`, `afn:localname` and the other `afn:` functions, and `java:` IRIs.
- An unknown function or iterator in the namespace of XSD, XPath, SPARQL, Jena or SPARQL-Generate is reported at once; it is no longer looked up as a query on the Web. Connections to a URL time out after 15 s (`-Dsun.net.client.defaultConnectTimeout` overrides it).
- A `SOURCE` clause whose document cannot be found logs a warning, as the iterators do.
- A named query called on several rows, whose `FROM`, `FROM NAMED` or `FROM GENERATE` clause depends on its parameters (`FROM <http://ex/g{?n}>`), is evaluated on the dataset of each row; it used the dataset of the first row for all of them. The rows that determine the same dataset are evaluated together.
- **Changed**: an error in the rest of a query, after an `ITERATOR` clause (a `SOURCE` with an invalid `ACCEPT`, a failed `SELECT`...), stops the execution, as it does in a query without `ITERATOR`; it was logged as "Unanticipated exception for ITERATOR" and the execution went on.
- The streams of the MQTT and WebSocket iterators are closed when the execution fails too, and their thread no longer keeps the JVM alive.
- **Changed**: the last variable of `iter:CSSPath`, "has next", is true when another element follows, as for `iter:XPath` and `iter:JSONPath`; it was the opposite.
- **Changed**: `fun:GeoJSONGeometry` writes a `MultiPolygon` as `MULTIPOLYGON(((a)), ((b)))`, one list of rings per polygon; it wrote all the rings in one polygon.
- No more warning "No query pattern in non-DESCRIBE query" for a query without `WHERE` clause.

**Performance**

- The results of the `WHERE` clause are passed to `GENERATE` and `TEMPLATE` as they are computed, without copies; an IRI template such as `<stop/{?id}>` resolves its constant part once instead of parsing each IRI. On synthetic workloads (flat and nested CSV and JSON), 100,000 rows: CSV with a nested `GENERATE` 10.0 s to 7.2 s and half the memory, nested JSON 10.0 s to 7.2 s; flat CSV at 1,000,000 rows with `-s -fo NT`, 21 s to 9 s (with the buffered output below).

**Command line**

- `--fail-on-missing` (key `failOnMissing` of `sparql-generate-conf.json`): a document, a graph or a named query that cannot be found stops the execution with status 1. In the Java API: `SPARQLExtStreamManager.setFailOnMissingDocument(true)`.
- With `--stream`, `-fo NT` writes N-Triples (full IRIs, no prefix); the default is still Turtle, one triple per line. The streamed output is buffered: it was written with one system call per piece of a line.
- The output files are closed at the end of the execution.

## 2.2.2

Released on 2026-11-07. This documentation on Read the Docs, the rewritten playground (CodeMirror 6, tutorial order, shareable links) and its refreshed examples, each run by a test against the engine.

## 2.2.1

Released on 2026-11-07. The playground server starts on Java 17 again (Jersey 2.48); Javadoc of the `st:` functions.

## 2.2.0

Released on 2026-11-07. Group `fr.mines-stetienne.ci.sparql-generate`, published on Maven Central through the Central Portal.

**Platform**

- Apache Jena 5.6.0; Java 17 or later is required.
- log4j 1.2 is replaced by reload4j 1.2.26; dependencies updated (Guava 33.7.1, jsoup 1.23.2, htmlcleaner 2.29, Jackson CBOR 2.21.8, Gson 2.13.2, Jetty 9.4.58, `mysql-connector-j` 8.4.0, HDT 3.0.10, commons-io 2.22.0, SLF4J 2.0.18).

**Language and engine**

- `TEMPLATE` queries with `before`, `separator` or `after` parse again on Jena 5.
- `GROUP` in `TEMPLATE` queries aggregates the solutions of a group again; aggregates inside `BOX` and `FORMAT` are evaluated.
- Calls to a named `TEMPLATE` with a `WHERE` clause at the call site work.
- Calls to `FUNCTION` queries work, and are memoised for one execution.
- `LIST` with `GROUP BY` gives one list per group.
- Calls to named queries are tabled, so that recursive calls terminate.
- `fun:bnode(string)` gives the same blank node for the same string during one execution, sub-queries included.
- Blank nodes of a `GENERATE` template are shared with its sub-queries; the `VALUES` of a sub-query are kept when it is joined with the outer solutions; the variables of an `ITERATOR` already bound by an outer query are rebound.
- Values are converted with `STR` in IRI templates, as in literal templates.
- Triples that are not valid RDF are not generated.
- The variable of a `SOURCE` clause stays unbound when its IRI or its `ACCEPT` is unbound.
- A `SELECT` query without `WHERE` clause projects its result.
- `iter:XPath` parses well-formed XML as XML (#109); `fun:XPath` returns string results of XPath expressions (#140); a one-element JSONPath result gives its value (#110).
- The UTF-8 byte order mark of local files is removed.

**Command line**

- The log goes to the standard error, at level `WARN` by default; the standard output carries only the result.
- The exit status is 1 when the query is invalid or its execution fails.
- `-x` is `--syntax` and `-f` is `--log-file` (both had the long name `log-file`).
- `-B` binds parameters to any RDF term, IRIs and prefixed names included; `-o` and `-f` are relative to the current directory, or absolute.
- The field `log` of `sparql-generate-conf.json` sets the log level as documented; `loglevel` is still read.
- `PERFORM` queries are reported as not supported, with exit status 1.

**Playground and documentation**

- Every example of the playground runs off-line and is tested against its expected output; the examples that depended on closed services were rewritten. Examples of `FUNCTION` queries and of named `SELECT` queries were added.
- This documentation, on ReadTheDocs.

## 2.1.1

Released 2025-09-29.

- Apache Jena 4.9; CSV cells of any size; serialisation of `SOURCE ... ACCEPT` fixed; JSONPath 2.9.0.

## 2.1.0

Released 2023-07-28.

- Apache Jena 4.9.0; the JSONSurfer iterator unwraps value objects.

## 2.0.x

Versions 2.0.0 (2020-07-08) to 2.0.12 (2022-10-04).

- 2.0.0 is the first release of version 2 of the language, with the group `fr.mines-stetienne.ci.sparql-generate`: `TEMPLATE`, `SELECT` with binding clauses, `FUNCTION`, named queries with a signature, `LIST`, the SQL module and the streaming JSON iterator `iter:JSONSurfer`.
- Later 2.0.x versions added the module for binary literals, `fun:HTTPHead` and the Web API functions, the option `-B` of the command line, streaming in the SQL iterator, an option to fail on exceptions, and moved to Java 11 and Apache Jena 4.5.

Versions 1.x used the groups `com.github.thesmartenergy`, then `fr.emse.ci` (see the [archive](archive/get-started-1-x.md)).
