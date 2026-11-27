# Changelog

This page summarises the changes of the engine since version 2.0. The full history is in the [commits](https://github.com/sparql-generate/sparql-generate/commits/master) and the [releases](https://github.com/sparql-generate/sparql-generate/releases).

## 2.6.3

Released on 2026-11-27. Memory: the documents read under an iteration.

**Iterators**

- `iter:CSV`, `iter:CSVStrict` and `iter:JSONSurfer` no longer keep every document they have read until the end of the execution. Each call registered a task that stops its parser if the execution is closed, and the task stayed registered once the document was read: the parser (a buffer of 2 MB for CSV) and the rest of the query it called back were never freed, so the memory grew with the number of documents read (an iteration over K GTFS feeds of kgc-bench, about 100 MB per feed: out of a 9 GB heap at K = 90). The task is now removed when the document is read, also when its parse fails.

**Java API**

- `ContextUtils.removeTaskOnClose(Context, Runnable)` removes a task registered by `addTaskOnClose` once the resource it stops has ended; `ContextUtils.countTasksOnClose(Context)` counts the registered tasks.

## 2.6.2

Released on 2026-11-27. Memory: large documents and joins over `FROM GENERATE` datasets.

**Engine**

- The solutions of an iterator are split into batches of 10,000 when the output of the query does not depend on the batches: a `GENERATE` query without `DISTINCT`, aggregate, `GROUP BY`, `ORDER BY`, `LIMIT`, `OFFSET` or `LIST(...)`, whose sub-queries are such queries without dataset clauses. `iter:CSV` and `iter:JSONSurfer` without a batch size then read the document in batches, and the memory no longer grows with its size (GTFS-Madrid-Bench at scale 100, a hand-written query: 17 GB to 1 GB of resident memory, the same output). The other queries are evaluated on all the solutions at once, as before. The system property `sparql-generate.batch-size` sets the size, `0` disables the splitting.
- The results of the `WHERE` clause are generated as they are read, in batches of 10,000, when the output of the query does not depend on the batches: a join over a `FROM GENERATE` dataset no longer keeps all its solutions in memory.
- A graph built by `FROM GENERATE` shares the equal literals of its triples (the keys of a join, one per row of the source): one object instead of one per triple, for the latest 65,536 distinct literals.
- The graphs of a `FROM` dataset are Jena's `GraphMem2Legacy`: the default `GraphMem2Fast` degenerates on many IRIs or literals that differ only in their last characters (the 1.26 million join keys of GTFS-Madrid-Bench at scale 1000: more than 200 s instead of 3 s).

**Iterators**

- `iter:SQL`: a PostgreSQL `timetz` column is an `xsd:time` with its offset (`08:30:00+02:00`); it stopped the execution ("Cannot convert the column of type TIMETZ to requested type java.time.LocalTime"). A `bit(n)` column with `n > 1` is the string of its bits (`"101"`); it stopped the execution ("Cannot cast to boolean"). A `bit(1)` is still an `xsd:boolean`.

**Java API**

- `ContextUtils.Builder.setBatchSize(int)`: the size of the batches of the automatic splitting, `0` to disable it.

## 2.6.1

Released on 2026-11-27.

**Language and engine**

- An argument with embedded expressions of a call in a `GENERATE` template reaches the called query: `GENERATE <stop>(<line/{?id}>, "zone {?z}") .` called the query with unbound parameters. The calls of `TEMPLATE`, `SELECT` (as an iterator) and `FUNCTION` queries already received them.
- An IRI or a literal with embedded expressions in a triple pattern of a `WHERE` clause constrains the pattern wherever the variables of its expressions are bound: by another triple of the same basic graph pattern (`?x ex:id ?i . <person/{?i}> ex:name ?n`), by a later element of the group, or, under `OPTIONAL`, by the left side (`?x ex:id ?i OPTIONAL { <person/{?i}> ex:name ?n }`). The node was a wildcard there, and the solutions a cross product. A node whose expression is in error (an unbound variable) matches nothing; it was a wildcard too.
- `IRI("rel")`, `URI(...)` and the IRIs with embedded expressions resolve a relative IRI against the base of the query (`BASE`, or `--base`), as in SPARQL 1.1, wherever they are evaluated: in the binding clauses (`BIND`, arguments of `ITERATOR` and `SOURCE`), in `TEMPLATE` clauses and in the arguments of calls. They resolved against the base of the system there (the working directory, a `file:` IRI), and against the base of the query only in the `WHERE` clause and in `GENERATE` templates. A named query resolves against its own base; the body of a `FUNCTION` query, against the base of the function.
- `fun:unsafeIRI(string)`: the IRI of a string that starts with a scheme, as it is, without checking it against the syntax of IRIs nor resolving it, for the term type `rml:UnsafeIRI` of RML (`<http://example.com/Emily Smith>`). `IRI(string)` still rejects such a string. The N-Triples and N-Quads writers write the characters that an IRI cannot hold as `\u` escape sequences (`<http://example.com/Emily\u0020Smith>`).
- A constant literal whose datatype is not a datatype of XSD keeps it: `"x"^^ex:dt`, in a `GENERATE` template or in an expression, gave `"x"` (since 2.0) when no literal of that datatype had been read before; `STRDT("x", ex:dt)` kept it.

**Iterators**

- A `RAISE` evaluated for a row of `iter:CSV` or of `iter:SQL` stops the execution with its message only: the iterator logged it first as a warning, with a stack trace, as an error of the document ("Exception while fetching or parsing CSV document", "Can not connect to the data base"). The same for any error of the rest of the query; `iter:JSONSurfer` no longer wraps them. The other iterators compute their rows before the rest of the query is evaluated.
- `iter:CSVStrict(<file>, "col", ...)`: `iter:CSV` with named columns, read as the logical sources of RML-IO: an empty cell is the empty string (`iter:CSV` leaves its variable unbound), a row whose number of fields is not that of the header, and a column that the header does not have, are data errors that stop the execution.
- `iter:SPARQL(<file>, "SELECT ...", "var", ...)`: the solutions of a SPARQL 1.1 `SELECT` query on an RDF document, one row per solution, then its position (RML-IO SPARQL results sources).
- `iter:SQL(database, query, "column", ...)`: the names of the columns to bind may follow the query, as for `iter:CSV`; the database may be given as a string. A cell is the natural RDF literal of R2RML in its canonical form: `xsd:integer` for every integer type (a `BIGINT` was read as a 32-bit `int`), `xsd:decimal`, `xsd:double` for `REAL`, `FLOAT` and `DOUBLE` (`REAL` was an `xsd:decimal`, `FLOAT` an `xsd:float`), `xsd:boolean` (also a `BIT` of one bit: SQL Server, MySQL `BOOLEAN`), `xsd:date`, `xsd:time`, `xsd:dateTime` (a `TIMESTAMP` was an `xsd:dateTimeStamp` without time zone, an invalid literal; a date was a string), `xsd:hexBinary` for the binary types (a string), a `CHAR(n)` padded to `n` characters (MySQL removes the spaces). A column name that the result does not have, or has twice, and a query that the database rejects, stop the execution with an error ("SQL error: ..."): they gave no row and a warning. A database that cannot be reached gives no row and a warning, as a missing document, and stops the execution with `--fail-on-missing`; the messages no longer show the password of the JDBC URL. See [SQL](language/sources-and-iterators.md#sql).
- The JDBC drivers of PostgreSQL (42.7.14) and SQL Server (13.6.2) are included with the one of MySQL, which goes from 8.4.0 to 9.7.0 (8.4.0 pulled `protobuf-java` 3.25.1, GHSA-735f-pc8j-v9w8).
- `iter:XPath`: a sub-query that does not start with `/` is evaluated from the matched node in its document, so that the parent, ancestor and sibling axes reach outside it (`"../@id"`); a sub-query that starts with `/` is still evaluated on a document made of the matched node.

**Documentation**

- [Queries calling queries](language/calls.md): runnable examples of a `GENERATE` query with nested queries, a `GRAPH` block, a call of a named `GENERATE` query with an IRI argument and a `FROM GENERATE` clause; a named `SELECT` query reused as an iterator by a `GENERATE` and by a `TEMPLATE` query; a `TEMPLATE` query that calls a named `TEMPLATE` query, which calls a `FUNCTION` query; and an input that makes them `RAISE` an error. An example of the documentation may expect an error (`expected_error.txt`), and the conformance cases of the engine may be `TEMPLATE` queries (`expected_output.txt`).

**Java API**

- `fr.mines_stetienne.ci.sparql_generate.iterator.DownstreamException`: an error of the rest of the query, raised in the callback of an iterator; an iterator that catches every exception lets it through with `DownstreamException.rethrowIfNotOwn(ex)`.

## 2.6.0

Released on 2026-11-26. `RAISE`, and named graphs written to files.

**Language and engine**

- `RAISE(message, value, ...)`: an expression that stops the execution with an error carrying a message and values, for the errors that only the data reveal: `BIND(COALESCE(IRI(?v), RAISE("Not an IRI", ?v)) AS ?s)`. It is evaluated for each solution that reaches it, and only when its value is needed (`IF`, `COALESCE`, `||`, `&&`); `BIND`, `FILTER`, `COALESCE` and the iterators do not catch its error. See [Raising errors](language/index.md#raising-errors). `raise` becomes a keyword of the language.

**Java API**

- `RaiseException`, a `QueryCancelledException`, with the message (`getRaisedMessage`) and the values (`getValues`) of the `RAISE` expression.

**Command line**

- A `RAISE` expression that is evaluated stops the execution with status 1, its message and values on the standard error.
- `--graphs-to-files`: a named graph whose IRI is a relative path to an RDF file, resolved against the base, is written to that file, in the folder of a `file:` base or else in the working directory: `GRAPH <out/people.ttl> { ... }`. Format and compression by the extension (`.nq`, `.nt`, `.ttl`, `.trig`, `.n3`, `.jsonld`, `.rdf`, `.rj`; `.gz`, `.zip`, `.bz2`, `.xz`, `.tar.gz`, `.tar.xz`, `.tar.bz2`); `GRAPH <dump.nq#g=IRI>` names a graph inside a file; a path that leaves the folder is an error; compatible with `--stream`. `--targets` (configuration key `targets`) describes the format, compression and character encoding of each file in a JSON file. Without the option, nothing changes. See [Named graphs written to files](cli.md#named-graphs-written-to-files).

**Dependencies**

- Guava 33.8.0 (Dependabot alert, GHSA-xxph-c9ww-hj94: the deserialization of some Guava collections could allocate unbounded memory; fixed in 33.7.2).

## 2.5.0

Released on 2026-11-22. For the translation of the RML modules (rml-to-sparql-generate 0.2.0).

**Documents**

- A local file is read decompressed according to its extension: `.gz`, `.bz2`, `.xz`, and the first file of `.zip`, `.tar`, `.tar.gz`, `.tar.bz2`, `.tar.xz` archives (only `.gz` and `.bz2` were, and only for some file locations). A UTF-16 or UTF-32 file with a byte order mark is read as UTF-8 text.

**Iterators**

- `iter:CSV` with named columns gives the position of each row (from 0, the header excluded, across batches) after the last column: `ITERATOR iter:CSV(<people.csv>, "id", "name") AS ?id ?name ?row`. A query that bound one variable more than the columns got no solution, with a warning.

## 2.4.1

Released on 2026-11-22.

**Language and engine**

- Two `FROM GENERATE` clauses with the same text, in two sibling nested queries evaluated on the same solution, each build their graph; the second one was empty, taken for a call already made. A query translated from an RML mapping whose two referencing object maps share their sources and join conditions lost the triples of the second one.
- An IRI with embedded expressions (`<person/{?name}>`, or `?{ ... }`) in a triple pattern of a `WHERE` clause, on a variable of the binding clauses or of the parent query, constrains the pattern as the variable does. The expression was evaluated in a group of its own, where the variable was unbound: the pattern matched every subject, and the solutions were a cross product.

## 2.4.0

Released on 2026-11-22. Named graphs: `GRAPH` blocks in `GENERATE` templates.

**Language and engine**

- **Named graphs**: a `GRAPH` block in a `GENERATE` template generates its triples in a named graph, as in SPARQL Update and in the `CONSTRUCT` queries of Jena: `GENERATE { ?s ?p ?o . GRAPH ?g { ?s ?q ?v } }`. The graph is an IRI, a variable, an IRI with embedded expressions (`GRAPH <graph/{?id}>`) or an embedded expression; a graph variable unbound or bound to a literal generates nothing, a blank node names a graph. A nested `GENERATE` in a `GRAPH` block, or a call to a named `GENERATE` query, generates its default graph in that graph. See [Named graphs](language/generate.md#named-graphs). A query without `GRAPH` block gives the same output as before.

**Java API**

- `RootPlan.execGenerateDataset` returns the generated dataset; `execGenerate` returns its default graph, with a warning when named graphs are ignored. A `StreamRDF` given to `execGenerateStream` receives the triples of named graphs through `quad`. `SPARQLExtQuery.hasQuadPatterns` tells whether a query has `GRAPH` blocks; `SPARQLExtElementVisitor` has a default method for the new element `ElementGenerateGraph`.

**Command line**

- The output of a query with named graphs is TriG by default, N-Quads with `--format-output NQUADS` (with or without `--stream`); a format that holds triples only (Turtle, N-Triples, RDF/XML, HDT) is refused with status 1. With `--stream` and without format, each quad is written as a line of TriG.

**Playground and web API**

- The playground writes the named graphs in TriG; the web API answers in TriG, or in N-Quads if the request accepts `application/n-quads`. A query without named graphs still gets Turtle.

## 2.3.1

Released on 2026-11-07.

- **Limits of an execution.** An execution can be given a time limit and a limit of triples: `ContextUtils.Builder.setTimeLimit` and `setMaxTriples` in Java, `--time-limit` and `--max-triples` on the command line. A limit reached stops the execution with the message `Time limit of 10 s reached, output truncated.` (or `Limit of n triples reached, output truncated.`), and exit status 1 on the command line. The stream iterators (`iter:HTTPGet`, `iter:MQTTSubscribe`, `iter:WebSocket`) stop at the time limit.
- **The playground server** is configured by the environment variables `SPARQL_GENERATE_MAX_TIME` (10 s by default) and `SPARQL_GENERATE_MAX_TRIPLES` (1,000,000 by default), see [Web API](web-api.md#limits-of-a-server). An execution that reaches a limit stops, and the playground says that the output is truncated; an execution went on running after its request had timed out.
- The message about an unknown function or iterator suggests the closest known one: `Unknown iterator <.../iter/JSONpath>. Did you mean <.../iter/JSONPath>?`.

## 2.3.0

Released on 2026-11-07. A change of the typing of JSON numbers, marked **Changed**; a fix that is a reason to upgrade from 2.2.4: an out-of-memory error could give a partial output with exit status 0; and a faster engine.

**Language and engine**

- **Changed**: a JSON number written with a decimal point and without exponent (`0.1`, `-65.814436`) is an `xsd:decimal`, with its exact value; a number with an exponent (`1.5e3`) is an `xsd:double`, an integer an `xsd:integer`. It was an `xsd:double`. This holds for `fun:JSONPath`, `iter:JSONPath`, `iter:JSONSurfer`, `fun:CBOR` and `iter:CBOR`, whatever the default configuration of JSONPath.
- An `OutOfMemoryError` in an iterator stops the execution with status 1 and a message; the CSV parser reported it as a parsing error, and the execution went on with part of the rows.

**Performance**

- Consecutive `BIND` clauses after an `ITERATOR` run in one pass, each row extended by one flat binding: as fast as the same `BIND` in the `WHERE` clause.
- `iter:XPath` evaluates a query or a sub-query that is a path of element names (or `*`), possibly ending with an attribute or `text()`, by walking the document, without the XPath engine of the JDK: a nested XML mapping of 1.3 M triples, 71 s to 13 s; its translation from RML, 111 s to 52 s.
- `iter:JSONPath` is linear in the number of matches (it was quadratic): the Facade-X graph of a nested JSON document of 1.3 M triples, 70 s to 28 s.
- `IRI()` takes a plain absolute `http` or `https` IRI as it is, without parsing it again; the shortcut of IRI templates also applies after a `#` or a `?` in their constant part.

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

Released on 2026-11-07. Fixes of the defects found by the benchmark and by minimal test queries against the documented behaviour, a few changes of behaviour (marked **Changed**), and a faster evaluation.

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
