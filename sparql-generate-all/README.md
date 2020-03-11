# SPARQL-Generate-CLI

Use SPARQL-Generate as a Command Line Interface.

```
usage: SPARQL-Generate processor
 -b,--base <arg>             Base URI of the working directory. If set,
                             each file in the working directory is
                             identified by a URI resolved against the
                             base.
 -d,--dir <arg>              Location of the directory with the queryset,
                             documentset, dataset, and configuration files
                             as explained in
                             https://w3id.org/sparql-generate/language-cli
                             .html. Default value is . (the current
                             folder)
 -dt,--debug-template        Debug the template output: insert warning
                             identifiers that refer to the log.
 -f,--log-file <arg>         Location where the log is to be stored. Log
                             also goes to the console.
 -h,--help                   Show help
 -hdt,--hdt                  Generate output as HDT.
 -l,--log-level <arg>        Set log level, acceptable values are TRACE <
                             DEBUG < INFO < WARN < ERROR < OFF. No value
                             or unrecognized value results in level DEBUG
 -o,--output <arg>           Location where the output is to be stored. By
                             default the output is stored in a file with
                             the same name as the query and the extension
                             '.out'.
 -oa,--output-append         Write from the end of the output file,
                             instead of replacing it.
 -of,--output-format <arg>   Format of the output file, e.g. TTL, NT, etc.
                             for GENERATE, or TEXT, XML, CSV, etc. for
                             SELECT.
 -q,--query-file <arg>       Name of the query file in the directory.
                             Default value is ./query.rqg
 -s,--stream                 Generate output as stream.
    --source <uri=uri>       Replaces <source> in a SOURCE clause with the
                             given value, e.g. urn:sg:source=source.json.
```


