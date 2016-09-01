# SPARQL binding functions and SPARQL-Generate iterator functions

`sparql-generate-jena` provides a set of SPARQL binding functions and SPARQL-Generate iterator functions that enable to generate RDF from JSON, XML, HTML, CSV, and plain text.

Custom SPARQL binding functions all take a set of RDF terms as input, and output zero or one RDF term. They all have namespace `http://w3id.org/sparql-generate/fn/` with preferred prefix `sgfn`.

Iterator functions are used in the `ITERATOR` clause. They all take a set of RDF terms as input, and output zero or more RDF terms. They all have namespace `http://w3id.org/sparql-generate/iter/` with preferred prefix `sgiter`.

```
PREFIX sgfn: <http://w3id.org/sparql-generate/fn/>
PREFIX sgiter: <http://w3id.org/sparql-generate/iter/>
```

In this document, we solely describe one iterator function, and one custom binding function. All the other functions are described in the javadoc:

* documentation for the [iterator functions](apidocs/com/github/thesmartenergy/sparql/generate/jena/iterator/library/package-summary.html)
* documentation for the [custom SPARQL functions](apidocs/com/github/thesmartenergy/sparql/generate/jena/function/library/package-summary.html).

## Example 1: iterator function http://w3id.org/sparql-generate/iter/JSONPath

A SPARQL Iterator function that extracts a list of sub-JSON documents of a JSON document, according to a JSONPath expression.

```
set of literals http://w3id.org/sparql-generate/iter/JSONPath( xsd:string message, xsd:string json_path_jayway )
```

For example, let be the following partial solution binding:

```
?message => "{ "x" : [ 1 , 2.0, "tt" , { } ] }"
```

Then iterator clause `ITERATOR iter:JSONPath( ?message, "$.x[1:4]" ) AS ?value` leads to the following set of partial solution bindings:

```
?message => "{ 'x' : [ 1 , 2.0, 'tt' , { } ] }" , ?value => "1"^^xsd:integer
?message => "{ 'x' : [ 1 , 2.0, 'tt' , { } ] }" , ?value => "2.0"^^xsd:decimal
?message => "{ 'x' : [ 1 , 2.0, 'tt' , { } ] }" , ?value => "tt"
?message => "{ 'x' : [ 1 , 2.0, 'tt' , { } ] }" , ?value => "{ }"
```

This iterator function uses library [JsonPath from GitHub user jayway](https://github.com/jayway/JsonPath).


## Example 2: custom binding function http://w3id.org/sparql-generate/fn/HTMLTagElement

A SPARQL function that extracts the text from an HTML element. It takes two parameters as input:

* a RDF Literal with datatype URI http://www.iana.org/assignments/media-types/text/html or `xsd:string` representing the source HTML document;
* a RDF Literal with datatype `xsd:string` representing name of the HTML element from which text is to be extracted.

It returns a RDF Literal with datatype URI `xsd:string` for the text of the element .

```
xsd:string http://w3id.org/sparql-generate/fn/JSONPath( xsd:string message, xsd:string tagname )
```
