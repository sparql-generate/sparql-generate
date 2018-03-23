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



##### Directory structure (optional)

Optionally, the query may be run in a directory with a queryset and/or documentset and/or dataset specified as follows:

The queryset is specified in a subfolder `queryset` that contains named queries and a configuration file `configuration.ttl` whose typical content is as follows:

```
@prefix lm: <http://jena.hpl.hp.com/2004/08/location-mapping#> .
_:mapping lm:mapping [ lm:name "https://example.org/query" ; lm:altName "queryset/query.rqg" ; lm:media "application/vnd.sparql-generate" ] .
_:mapping lm:mapping [ lm:name "..." ; lm:altName "..." ; lm:media "..." ] .
...
```

Here, `https://example.org/query` is the name of the SPARQL-Generate query, `application/vnd.sparql-generate` is the SPARQL-Generate media type, and this query may be found at `queryset/query.rqg`.


The dataset is specified in a subfolder `dataset` that contains named graphs and a configuration file `configuration.ttl` whose typical content is as follows:

```
@prefix lm: <http://jena.hpl.hp.com/2004/08/location-mapping#> .
_:mapping lm:mapping [ lm:name "https://example.org/graph" ; lm:altName "dataset/graph.ttl" ; lm:media "text/turtle" ] .
_:mapping lm:mapping [ lm:name "..." ; lm:altName "..." ; lm:media "..." ] .
...
```

Here, `https://example.org/graph` is the name of the graph, `text/turtle` is the type of its serialization, and this graph may be loaded from `dataset/graph.ttl`.



The documentset is specified in a subfolder `documentset` that contains named documents and a configuration file `configuration.ttl` whose typical content is as follows:

```
@prefix lm: <http://jena.hpl.hp.com/2004/08/location-mapping#> .
_:mapping lm:mapping [ lm:name "https://example.org/doc.json" ; lm:altName "documentset/doc.json" ; lm:media "application/json" ] .
_:mapping lm:mapping [ lm:name "..." ; lm:altName "..." ; lm:media "..." ] .
...
```

Here, `https://example.org/doc.json` is the name of the document, `application/json` is its type, and this document may be loaded at `documentset/doc.json`.

