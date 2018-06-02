# SPARQL-Generate Overview

---

## Syntax

The general structure of a SPARQL-Generate query is as follows:

```
PREFIX declarations             -- same as SPARQL 1.1
FROM and FROM NAMED clauses     -- same as SPARQL 1.1
GENERATE template               -- replaces and extends SPARQL 1.1 CONSTRUCT
ITERATOR, SOURCE, BIND clauses  -- see below
WHERE clause                    -- same as SPARQL 1.1
Solution modifiers              -- group by, order by, limit, offset,... same as SPARQL 1.1
```

The syntax of the **`ITERATOR` clause** is the following:  

```
ITERATOR <iterator>(args) AS ?var
```

Where `<iterator>` is the IRI of the SPARQL-Generate *iterator function*, which is similar to a SPARQL 1.1 Function, except it *returns a list of RDF Terms* instead of just one. `?var` will be bound to every RDF Term in the list returned by the evaluation of the iterator function over the arguments `args`.


The **`SOURCE` clause** enables to bind a named document to a variable. Its syntax is as follows:

```
SOURCE <source_node> ACCEPT <type_uri> AS ?var
``` 

If `<source>` is a HTTP IRI, then the engine will operate a HTTP GET to that IRI (or use a local cache), then bind variable `?var` to the RDF Literal representation of the retrieved document. 
`ACCEPT <type_uri>` is optional. It gives a hint to the engine about how to negotiate a representation of the resource identified by `<source>` with the server.

The **`GENERATE` template** replaces and extends the SPARQL 1.1 `CONSTRUCT` clause: one may embed other GENERATE queries in the `GENERATE` template. The `GENERATE` part of the embedded query may be a template as above, or just a URI that indicates the engine that it must fetch and execute an existing query on the web, with the current binding solutions.

One may **embed SPARQL expressions in nodes** (since version 2.0.0-beta) wherever variables would be legal:

- IRIs and literals can contain embedded expressions: `<foo{ <expr> }bar>`, `"foo{ <expr> }bar"@en`,  `'''foo{ <expr> }'''^^<bar{ <expr> }>`;
- variables can be replaced by any other expression node, such as in `?{ ?value1 >= 2.5 }`.


## Binding and iterator functions

`sparql-generate-jena` provides a set of SPARQL binding functions and SPARQL-Generate iterator functions that enable to generate RDF from JSON, XML, HTML, CSV, and plain text.

Custom SPARQL binding functions all take a set of RDF terms as input, and output zero or one RDF term. They all have namespace `http://w3id.org/sparql-generate/fn/` with preferred prefix `fun`.

Iterator functions are used in the `ITERATOR` clause. They all take a set of RDF terms as input, and output zero or more RDF terms. They all have namespace `http://w3id.org/sparql-generate/iter/` with preferred prefix `iter`.

```
PREFIX fun: <http://w3id.org/sparql-generate/fn/>
PREFIX iter: <http://w3id.org/sparql-generate/iter/>
```

In this document, we solely describe one iterator function, and one custom binding function. All the other functions are described in the javadoc:

* documentation for the [iterator functions](apidocs/com/github/thesmartenergy/sparql/generate/jena/iterator/library/package-summary.html)
* documentation for the [custom SPARQL functions](apidocs/com/github/thesmartenergy/sparql/generate/jena/function/library/package-summary.html).

**Example 1: iterator function iter:JSONPath**

A SPARQL Iterator function that extracts a list of sub-JSON documents of a JSON document, according to a JSONPath expression.

```
set of literals iter:JSONPath( xsd:string message, xsd:string json_path_jayway )
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


**Example 2: custom binding function fun:HTMLTagElement**

A SPARQL function that extracts the text from an HTML element. It takes two parameters as input:

* a RDF Literal with datatype URI http://www.iana.org/assignments/media-types/text/html or `xsd:string` representing the source HTML document;
* a RDF Literal with datatype `xsd:string` representing name of the HTML element from which text is to be extracted.

It returns a RDF Literal with datatype URI `xsd:string` for the text of the element .

```
xsd:string fun:JSONPath( xsd:string message, xsd:string tagname )
```

---

## How it works

To put it simply, the execution of a SPARQL-Generate is defined as follows:

1. clauses `ITERATOR` and `SOURCE` are processed in order, and one constructs a [SPARQL 1.1 VALUES](https://www.w3.org/TR/sparql11-query/#inline-data) data block.
1. one constructs a SPARQL 1.1 `SELECT *` query from the SPARQL-Generate `WHERE` clause, and add the data block at the beginning of the `WHERE` clause.
1. this SPARQL 1.1 SELECT query is evaluated on the SPARQL dataset, and produces a set of solution bindings.
1. for each of these solution bindings, and for each element in the GENERATE template, one either produce triples, or execute the embedded query.  

---

## IANA considerations.

```
Type name:
   application

Subtype name:
   vnd.sparql-generate

Required parameters:
   None

Optional parameters:
   None

Encoding considerations:
   The syntax of the SPARQL-Generate Language is expressed over code points in Unicode [UNICODE]. The encoding is always UTF-8 [RFC3629].
   Unicode code points may also be expressed using an \uXXXX (U+0 to U+FFFF) or \UXXXXXXXX syntax (for U+10000 onwards) where X is a hexadecimal digit [0-9A-F]

Security considerations:
   See SPARQL Query appendix C, Security Considerations as well as RFC 3629 [RFC3629] section 7, Security Considerations.

Interoperability considerations:
   There are no known interoperability issues.

Published specification:
   https://w3id.org/sparql-generate/language

Fragment identifier considerations:
   None

Additional information:

Magic number(s):
   A SPARQL-Generate query may have the string 'PREFIX' (case independent) near the beginning of the document.

File extension(s): 
   ".rqg"

Macintosh file type code(s): 
   TEXT

Person & email address to contact for further information:
   Maxime Lefrançois <maxime.lefrancois.86@gmail.com>

Intended usage:
   COMMON

Restrictions on usage:
   None

Author/Change controller:
   Maxime Lefrançois <maxime.lefrancois@emse.com>

The Internet Media type of a SPARQL-Generate query is 'application/vnd.sparql-generate', with file extension '*.rqg'.
```