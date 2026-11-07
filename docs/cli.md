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
| `-fo`, `--format-output` | format | the output format: `TTL` (default), `NT`, `JSON-LD`, `RDF/XML`... for `GENERATE`; `TEXT`, `CSV`, `TSV`, `JSON`, `XML` for `SELECT` |
| `-s`, `--stream` | | write the output as it is generated (see [streaming](language/generate.md#streaming)) |
| `-hdt`, `--hdt` | | write a `GENERATE` output as HDT; requires `--output` |
| `-dt`, `--debug-template` | | in `TEMPLATE` output, insert identifiers that refer to the warnings of the log |
| `-x`, `--syntax` | | check the syntax of the query and exit |
| `-l`, `--log-level` | level | `TRACE`, `DEBUG`, `INFO`, `WARN` (default), `ERROR` or `OFF`; an unrecognised value gives `WARN` |
| `-f`, `--log-file` | file | also write the log to this file |
| `-B` | `name=term` | bind the parameter `?name` to an RDF term, written as in the query, with its prefixes: `-Bn=12`, `-Bs='"a string"@en'`, `-Biri='<http://example.org/>'`, `-Bp=ex:o`; a value that is not an RDF term is an error |
| `--source` | `iri=iri` | deprecated: replace the IRI of a `SOURCE` clause |

The log goes to the standard error. The exit status is 0 when the query was executed, and 1 when the query is invalid, when its execution or the writing of the output fails, or when the options do not fit the query (such as `--hdt` with a `SELECT` query). A document that cannot be found or parsed is a warning, not a failure: the variables it would bind stay unbound.

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
| `log` | integer | `2` | 0 `OFF`, 1 `ERROR`, 2 `WARN`, 3 `INFO`, 4 `DEBUG`, 5 `TRACE`; the former key `loglevel` is also read |
| `logFile` | path | none | as `--log-file`, relative to the working directory |

A document of the `documentset` can be declared several times with different media types: a `SOURCE ... ACCEPT` clause selects one of them (see [content negotiation](language/sources-and-iterators.md#content-negotiation-with-accept)). The examples of this documentation and of the playground use this layout: each one is a working directory that the command line can run.

## Query parameters

A query with a name and a signature, such as `GENERATE <http://example.org/q> (?country) { ... }`, can be run with its parameters bound by `-B`:

```sh
java -jar sparql-generate-2.2.0.jar --dir my-project -Bcountry='"GB"'
```
