@title SPARQL-Generate | Sublime IDE

# Integration with the Sublime Text editor

Edit and run SPARQL-Generate projects directly in Sublime text with the [pre-release of the `LinkedData` package](https://github.com/sparql-generate/sublime-editor).

Note: as for SPARQL-Generate CLI, all input and output files are assumed to be in UTF-8.

## Installation

Installation instructions are available [on the GitHub of the `LinkedData` package](https://github.com/sparql-generate/sublime-editor).

## Running

The SPARQL-Generate Sublime package are **folders** that contain a query file `query.rqg`.

Optionally, a SPARQL-Generate project configuration file `sparql-generate-conf.json`, which may override the name of the main query file.

Run the SPARQL-Generate project in (Tools -> Build), or CTRL+B. (SUPER+B on Mac).

The bottom part of the text editor contains the live log with level INFO.

A more verbose Log file is outputted in `<queryname>.rqgout`. Shortcut to open it is CTRL+SHIFT+L (SUPER+SHIFT+L on Mac).

## Configuration 

You can configure your SPARQL-Generate project inside a `sparql-generate-conf.json` file in the same folder as the main query. Key shortcut CTRL+SHIFT+O should open this file (SUPER+SHIFT+O on Mac).

Configuration file `sparql-generate-conf.json` is a JSON Object. 

The following keys specify the query execution context:

* `base` (optional, no default value). Base URI of the working directory. If set, each file in the working directory is identified by a URI resolved against the base. Example: `"http://example.org/"`. 
* `query` (default value `query.rqg`). Location of the main query file.
* `namedqueries` is an array of objects. Each named query object contains:
    * `uri` (string) the URI that identifies the named query
    * `path` (string) the location of the named query file.
* `graph` (default value `dataset/default.ttl`). Location of the default graph in Turtle.
* `namedgraphs` is an array of objects. Each named graph object contains:
    * `uri` (string) the URI that identifies the named graph
    * `path` (string) the location of the named graph in Turtle.
* `documentset` is an array of objects. Each named document object contains:
    * `uri` (string) the URI that identifies the named document
    * `path` (string) the location of the named document
    * `mediatype` (string, optional) the media type of the document. Example: `text/csv`, `application/json`

The following keys specify the output:

* `output` (optional, default value `query.out`). Location where the output is to be stored.
* `outputAppend` (boolean, default value `false`). `true` if the output is to be appended to the output file
* `outputFormat` (optional). Format of the output file, e.g. `"TTL"`, `"NT"`, etc. for GENERATE, or `"TEXT"`, `"XML"`, `"CSV"`, etc. for SELECT. 
* `stream` (boolean, default value `false`). If `true`, generate output as stream.
* `hdt` (boolean, default value `false`). If `true`, generate output as HDT.

The following keys determine the amount of logging:

* `log` (integer, default value 5). The log level (`{ "0": "OFF", "1": "ERROR", "2": "WARN", "3": "INFO", "4": "DEBUG", "5": "TRACE"} `)
* `debugTemplate` (boolean, default value `false`). Debug the template output: insert warning identifiers that refer to the log.

Notes:

* key `logFile` is ignored. Log will always be outputted in `<queryname>.rqgout`.
* `log`>3 is very verbose and **should be used only to debug. Not to evaluate performances.**  


#### Example of a configuration document

```
{
  "base": "http://example.org/"
  "query": "query.rqg",
  "namedqueries": [
    {
      "uri": "http://example.org/query#0",
      "path": "queryset/query0.rqg"
    }
  ],
  "graph": "dataset/default.ttl",
  "namedgraphs": [
    {
      "uri": "http://example.org/graph#0",
      "path": "dataset/graph0.ttl"
    }
  ],
  "documentset": [
    {
      "uri": "http://example.org/document#0",
      "mediatype": "text/plain",
      "path": "documentset/document0.txt"
    }
  ],
  ""
  "log": "5"
}
```
