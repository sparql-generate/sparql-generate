# SPARQL-Generate Executable JAR

[Download it here](sparql-generate-jena.jar): [![Download button](download.gif)](sparql-generate-jena.jar)

The default template for using the executable JAR is as follows:

```
java -jar sparql-generate-jena.jar [arguments]
```

#### SPARQL-Generate Executable JAR Arguments

* `-d,--dir <arg>` Location of the directory with the queries, documentset, dataset, and configuration files as explained in https://w3id.org/sparql-generate/language-cli.html. Default value is .
*  `-f,--log-file <arg>` Location where the log is to be stored. No value means output goes to the console.
* `-h,--help` Show help
* `-l,--log-level <arg>` Set log level, acceptable values are TRACE < DEBUG < INFO < WARN < ERROR < FATAL < OFF. No value or unrecognized value results in level DEBUG
* `-o,--output <arg>` Location where the output is to be stored. No value means output goes to the console.
* `-q,--query-file <arg>` Name of the query file in the directory. Default value is query.rqg
