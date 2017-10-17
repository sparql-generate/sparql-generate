# SPARQL-Generate Executable JAR

 
[Download it here](sparql-generate-jena.jar)

The default template for using the executable JAR is as follows:

```
java -jar sparql-generate-jena.jar [arguments]
```

#### SPARQL-Generate Executable JAR Arguments

* `-h` or `--help` to view options available
* `-qf` or `--queryfile` for the local path of the file containing a query
* `-qs` or `--querystring` for passing a SPARQL Query string directly 
* `-f` or `--outputformat` to set the RDF output format, possible options are TTL for Turtle, NTRIPLES for NTRIPLES, RDFXML for RDF/XML, N3 for N3, JSONLD for JSON-LD, TRIG for TRIG
* `-c` for setting up a mapping from remote IRI to locate files of the form `http://ex.org/file1=path/to/file1;http://ex.org/file2=path/to/file2;`

#### SPARQL-Generate Executable JAR Examples

* Executing a query found in file query.rqg

`java -jar sparql-generate-jena.jar -qf path/to/query.rqg `

* Executing a query found in file query.rqg where query contains the URI http://example.org/countries.json of a remote file countries.json which can also be found locally.

`java -jar sparql-generate-jena.jar -qf path/to/query.rqg -c http://example.org/countries.json=path/to/countries.json`

* Executing a query found in file query.rqg where query contains two URIs http://example.org/countries.json of a remote file countries.json and http://example.org/continents.json of a remote file continents.json and both files can be found locally.

`java -jar sparql-generate-jena.jar -qf path/to/query.rqg -c http://example.org/countries.json=path/to/countries.json;http://example.org/continents.json=path/to/continents.json`

* Executing a query found in file query.rqg and returns the output in RDF/XML

`java -jar sparql-generate-jena.jar -qf path/to/query.rqg -f RDFXML`