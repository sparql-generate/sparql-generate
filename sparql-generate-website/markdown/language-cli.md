@title SPARQL-Generate | Executable JAR

# SPARQL-Generate Executable JAR

The SPARQL-Generate 2.0 executable JAR can be obtained from [the Releases page of the project on GitHub](https://github.com/sparql-generate/sparql-generate/releases). 

Executing this JAR requires Java JRE 1.8 or above. 

### Note on versions

The documentation below applies to versions >2 


## Command line

The default template for calling SPARQL Extensions is as follows:

```
java -jar sparql-generate.jar [arguments]
```


#### SPARQL Extensions Executable JAR Arguments

* `-h,--help` Show help
* `-b,--base <arg>`  Base URI of the working directory. If set, each file in the working directory is identified by a URI resolved against the base.
* `-d,--dir <arg>` Location of the directory with the queryset, documentset, dataset, and configuration files as explained below. Default value is '.' (the current folder)
* `-q,--query-file <arg>` Name of the query file in the directory. Default value is `./query.rqg`
* `-o,--output <arg>` Location where the output is to be stored. By default the output is stored in a file with the same name as the query and the extension '.out'.
* `-oa,--output-append` Write from the end of the output file, instead of replacing it.
* `-of,--output-format` Format of the output file, e.g. TTL, NT, etc. for GENERATE, or TEXT, XML, CSV, etc. for SELECT. 
* `-l,--log-level <arg>` Set log level, acceptable values are TRACE < DEBUG < INFO < WARN < ERROR < OFF. No value or unrecognized value results in level TRACE
* `-f,--log-file <arg>` Location where the log is to be stored. Output also goes to the console.
* `-s,--stream` Generate output as stream.
* `-hdt,--hdt` Generate output as HDT.
* `-dt,--debug-template` Debug the template output: insert warning identifiers that refer to the log.
* `--source` Replaces `<source>` in a `SOURCE` clause with the given value, e.g. `urn:sg:source=source.json`.


##### Configuration file (optional)

Optionally, the query may be run in a directory with a configuration file `sparql-generate-conf.json` that describes default execution options, and execution context configuration such as queryset and/or documentset and/or dataset.

**Note:** Command-line options always override the specification in the Configuration file `sparql-generate-conf.json`.

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

