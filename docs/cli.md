# Command line

The executable JAR `sparql-generate-2.2.0.jar` ([releases](https://github.com/sparql-generate/sparql-generate/releases)) executes one query on a working directory. It runs on Java 17 or later.

```sh
java -jar sparql-generate-2.2.0.jar [options]
```

## Working directory

The command line works on a directory (option `--dir`, by default the current directory) that holds the query, its documents, and an optional configuration file `sparql-generate-conf.json`. The query file (`--query-file`) and the paths of the configuration file are relative to this directory; the paths given to `--output` and `--log-file` are relative to the current directory, or absolute.

The engine looks a document, a named query or a graph up in this order:

1. the IRIs mapped by the configuration file (`documentset`, `namedqueries`, `namedgraphs`);
2. if `--base` is set, the files of the directory, each identified by its path resolved against the base;
3. the file system;
4. the Web, with an HTTP GET.

## Options

| Option | Argument | Meaning |
|:--|:--|:--|
| `-h`, `--help` | | show the help |
| `-d`, `--dir` | directory | the working directory; default `.` |
| `-q`, `--query-file` | file | the query file in the working directory; default `query.rqg` |
| `-b`, `--base` | IRI | identify each file of the working directory by its path resolved against this IRI |
| `-o`, `--output` | file | write the output to this file; by default the output goes to the standard output |
| `-ao`, `--append-output` | | append to the output file instead of replacing it |
| `-fo`, `--format-output` | format | the output format: `TTL` (default), `NT`, `JSON-LD`, `RDF/XML`... for `GENERATE`, and `TRIG` (default with [named graphs](language/generate.md#named-graphs)) or `NQUADS` for a query that generates quads; `TEXT`, `CSV`, `TSV`, `JSON`, `XML` for `SELECT` |
| `-s`, `--stream` | | write the output as it is generated (see [streaming](language/generate.md#streaming)): Turtle, one triple per line, or N-Triples with `-fo NT`, N-Quads with `-fo NQUADS`; the quads of named graphs make the Turtle output TriG, one `<g> { s p o . }` block per line; a triple may be written more than once |
| `-hdt`, `--hdt` | | write a `GENERATE` output as HDT; requires `--output` |
| `-dt`, `--debug-template` | | in `TEMPLATE` output, insert identifiers that refer to the warnings of the log |
| `-x`, `--syntax` | | check the syntax of the query and exit |
| `--fail-on-missing` | | fail when a document, a graph or a named query cannot be found (see below) |
| `--time-limit` | seconds | stop the execution after this time |
| `--max-triples` | n | stop a `GENERATE` execution beyond this number of triples |
| `--graphs-to-files` | | write the named graphs that name RDF files to these files (see [below](#named-graphs-written-to-files)) |
| `--targets` | file | describe the target files (format, compression, encoding) in a JSON file; implies `--graphs-to-files` |
| `-l`, `--log-level` | level | `TRACE`, `DEBUG`, `INFO`, `WARN` (default), `ERROR` or `OFF`; an unrecognised value gives `WARN` |
| `-f`, `--log-file` | file | also write the log to this file |
| `-B` | `name=term` | bind the parameter `?name` to an RDF term, written as in the query, with its prefixes: `-Bn=12`, `-Bs='"a string"@en'`, `-Biri='<http://example.org/>'`, `-Bp=ex:o`; a value that is not an RDF term is an error |
| `--source` | `iri=iri` | deprecated: replace the IRI of a `SOURCE` clause |

The log goes to the standard error. The exit status is 0 when the query was executed, and 1 when the query is invalid, when its execution or the writing of the output fails, or when the options do not fit the query (such as `--hdt` with a `SELECT` query, or a format that holds triples only, such as `TTL`, `NT` or `--hdt`, for a query that generates quads in named graphs: `The query generates quads in named graphs, which Turtle cannot hold: use --format-output TRIG or NQUADS.`; a query with `GRAPH` blocks is refused before its execution, the quads of a called named query stop it when they are generated). A document that cannot be found or parsed is a warning, not a failure: an iterator over it gives no solution, the variable of a `SOURCE` clause stays unbound, and the graph of a `FROM` clause is empty. With `--fail-on-missing`, a document, a graph or a named query that cannot be found stops the execution with status 1 and the message `Document not found: <iri>`. A limit set by `--time-limit` or `--max-triples` that is reached stops the execution with status 1 and the message `Time limit of 600 s reached, output truncated.` (or `Limit of n triples reached, output truncated.`); with `--stream`, the output written so far stays in the output file. So does an `OutOfMemoryError`, with status 1. A [`RAISE`](language/index.md#raising-errors) expression that is evaluated stops the execution with status 1, its message and values on the standard error: `No email address for: "Bob", 1`.

Levels `DEBUG` and `TRACE` log each solution and each triple: use them to understand a query, not to measure performance.

`PERFORM` queries are parsed but not executed: the command line answers that they are not supported and exits with status 1.

## Configuration file

`sparql-generate-conf.json`, in the working directory, sets the defaults of the execution. The options of the command line override it.

```json
{
  "base": "http://example.org/",
  "query": "query.rqg",
  "namedqueries": [
    { "uri": "http://example.org/query/person", "path": "queryset/person.rqg" }
  ],
  "graph": "dataset/default.ttl",
  "namedgraphs": [
    { "uri": "http://example.org/graph/countries", "path": "dataset/countries.ttl" }
  ],
  "documentset": [
    { "uri": "http://example.org/people", "mediatype": "text/csv", "path": "documentset/people.csv" },
    { "uri": "http://example.org/people", "mediatype": "application/json", "path": "documentset/people.json" }
  ],
  "output": "output.ttl",
  "outputFormat": "TTL",
  "stream": false,
  "log": 2
}
```

| Key | Type | Default | Meaning |
|:--|:--|:--|:--|
| `base` | IRI | none | as `--base` |
| `query` | path | `query.rqg` | the query file |
| `namedqueries` | array | `[]` | named queries: `uri`, `path`, optional `mediatype` |
| `graph` | path | `dataset/default.ttl` | the default graph of the input dataset, in Turtle, if the file exists |
| `namedgraphs` | array | `[]` | named graphs of the input dataset, in Turtle: `uri`, `path` |
| `documentset` | array | `[]` | documents: `uri`, `path`, optional `mediatype` |
| `output` | path | standard output | as `--output`, relative to the working directory |
| `outputAppend` | boolean | `false` | as `--append-output` |
| `outputFormat` | string | `TTL` or `TEXT` | as `--format-output` |
| `stream` | boolean | `false` | as `--stream` |
| `hdt` | boolean | `false` | as `--hdt` |
| `debugTemplate` | boolean | `false` | as `--debug-template` |
| `failOnMissing` | boolean | `false` | as `--fail-on-missing` |
| `timeLimit` | integer | `0` (none) | as `--time-limit` |
| `maxTriples` | integer | `0` (none) | as `--max-triples` |
| `graphsToFiles` | boolean | `false` | as `--graphs-to-files` |
| `targets` | path | none | as `--targets`, relative to the working directory |
| `log` | integer | `2` | 0 `OFF`, 1 `ERROR`, 2 `WARN`, 3 `INFO`, 4 `DEBUG`, 5 `TRACE`; the former key `loglevel` is also read |
| `logFile` | path | none | as `--log-file`, relative to the working directory |

A document of the `documentset` can be declared several times with different media types: a `SOURCE ... ACCEPT` clause selects one of them (see [content negotiation](language/sources-and-iterators.md#content-negotiation-with-accept)). The examples of this documentation and of the playground use this layout: each one is a working directory that the command line can run.

## Named graphs written to files

With `--graphs-to-files`, a `GENERATE` query writes some of its [named graphs](language/generate.md#named-graphs) to files instead of its output: a named graph whose IRI is a relative path, resolved against the base IRI, that ends with the extension of an RDF format. It has no syntax of its own: `GRAPH <people.ttl> { ... }` writes the file `people.ttl`, and a graph computed from the data, `GRAPH <out/{?country}.nq> { ... }`, one file per country. The other named graphs, and the default graph, stay in the output.

```sh
java -jar sparql-generate.jar -b http://example.org/ --graphs-to-files -fo NQUADS
```

- **Folder.** The files are written in the folder of the base IRI if it is a `file:` IRI (`-b file:///data/out/`), and else in the working directory (`--dir`), which the base identifies for the documents that the query reads. Without base, relative IRIs are resolved against the current directory, and the files are written there. A path of subfolders creates them.
- **Format and compression** come from the extension: `.nq`, `.nt`, `.ttl`, `.trig`, `.n3`, `.jsonld`, `.rdf` or `.rdfxml`, `.rj` or `.rdfjson`; then, optionally, `.gz`, `.zip`, `.bz2`, `.xz`, `.tar.gz`, `.tar.xz` or `.tar.bz2` (`dump.nq.gz`). An archive holds one entry, named after the file without its compression extension (`dump.nq`).
- **Named graphs inside a file.** `GRAPH <dump.nq#g=http://example.org/people> { ... }` writes the graph `http://example.org/people` in `dump.nq`, whose format must hold named graphs (`.nq`, `.trig`, `.jsonld`); `GRAPH <dump.nq> { ... }` writes its default graph. The fragment is `g=` followed by the absolute IRI of the graph, percent-encoded or not: `%XX` sequences are decoded, so a graph IRI that contains `%` or `#` is written with `%25` and `%23`. An IRI with embedded expressions encodes its expressions, which suits a computed graph: `GRAPH <dump.nq#g={STR(?g)}> { ... }`. Another fragment is an error.
- **Safety.** A graph name can come from the data: a path that would leave the folder (`..` or `.` segments, an empty segment, a backslash, encoded as `%2E%2E` or not) is an error, status 1. A graph name that resolves outside of the folder of the base, such as `<../people.nq>` or `<http://example.org/other/people.nq>`, does not name a file: it stays a named graph of the output.
- **Writing.** A file is created at its first triple, and closed at the end of the execution, also when the execution fails. Without `--stream`, each triple is written once: N-Triples and N-Quads as the triples are generated, the other formats at the end from the graph built in memory. With `--stream`, N-Triples, N-Quads, Turtle and TriG are written as the triples are generated, a triple may be written more than once, and JSON-LD, RDF/XML, RDF/JSON and N3 are still written at the end.
- **Output format.** The main output follows the usual rules; a format that holds triples only, such as `-fo NT`, is accepted for a query with `GRAPH` blocks, and refused only if a named graph reaches the output.

`--targets` gives a JSON file that describes the target files, for a format, a compression or a character encoding that the extension does not give. Its keys are paths relative to the folder, as in the graph names; each value has the optional keys `format` (a format name such as `N-Quads`, `Turtle`, `JSON-LD`, a media type such as `application/n-triples`, or an extension), `compression` (`none`, `gzip`, `zip`, `bzip2`, `xz`, `tar.gz`, `tar.xz`, `tar.bz2`) and `encoding` (a character encoding, `UTF-8` by default; `UTF-16` writes a byte order mark, big-endian). A path of the description names a file even without extension.

```json
{
  "dump1.nq": { "encoding": "UTF-16" },
  "out/people": { "format": "Turtle", "compression": "gzip" }
}
```

## Query parameters

A query with a name and a signature, such as `GENERATE <http://example.org/q> (?country) { ... }`, can be run with its parameters bound by `-B`:

```sh
java -jar sparql-generate-2.2.0.jar --dir my-project -Bcountry='"GB"'
```
