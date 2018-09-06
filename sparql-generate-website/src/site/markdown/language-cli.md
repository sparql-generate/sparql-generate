# SPARQL-Generate Executable JAR

[![Download button](download.png)](sparql-generate-jena.jar)

This executable JAR requires Java JRE 1.8 or above. The default template for calling SPARQL-Generate is as follows:

```
java -jar sparql-generate-jena.jar [arguments]
```

#### SPARQL-Generate Executable JAR Arguments

* `-h,--help` Show help
* `-d,--dir <arg>` Location of the directory with the queryset, documentset, dataset, and configuration files as explained below. Default value is '.' (the current folder)
* `-q,--query-file <arg>` Name of the query file in the directory. Default value is ./query.rqg
* `-o,--output <arg>` Location where the output is to be stored. No value means output goes to the console.
* `-l,--log-level <arg>` Set log level, acceptable values are TRACE < DEBUG < INFO < WARN < ERROR < FATAL < OFF. No value or unrecognized value results in level DEBUG
* `-f,--log-file <arg>` Location where the log is to be stored. No value means output goes to the console.
* `-s,--stream` Generate output as stream.



##### Configuration file (optional)

Optionally, the query may be run in a directory with a configuration file `sparql-generate-conf.json` that describes the queryset and/or documentset and/or dataset as follows:

```
{
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
  "log": "5"
}
```

- `query` specifies the path to the file that contains the default query in UTF-8 encoding (`query.rqg` is the default value).
- `namedqueries` is a table specifying named queries. For each of the named queries, `uri` is its URI and `path` is the path to the file that contains the query in UTF-8 encoding.
- `graph` specifies the path to the file that contains the default graph in UTF-8 encoding (`dataset/default.ttl` is the default value).
- `namedgraphs` is a table specifying named graphs. For each of the named graphs, `uri` is its URI and `path` is the path to the file that contains the graph in UTF-8 encoding.
- `documentset` is a table specifying named documents. For each of the named documents, `uri` is its URI, `path` is the path to the file that contains the document in UTF-8 encoding, and `mediatype` is the media type of the document.
- `log` is the default log level (`{ "0": "OFF", "1": "ERROR", "2": "WARN", "3": "INFO", "4": "DEBUG", "5": "TRACE"} `)


