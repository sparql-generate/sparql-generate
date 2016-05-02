# Iterator and custom SPARQL functions

`sparql-generate-jena` provides a set of iterator functions and custom SPARQL functions that enable to generate RDF from JSON, XML, HTML, CSV, and plain text.

Custom SPARQL functions all take a set of RDF terms as input, and output zero or one RDF term. They all have namespace `http://w3id.org/sparql-generate/fn/` with preferred prefix `fn`.

Iterator functions are used in the `ITERATOR` clause. They all take a set of RDF terms as input, and output zero or more RDF terms. They all have namespace `http://w3id.org/sparql-generate/ite/` with preferred prefix `ite`.

```
PREFIX fn: <http://w3id.org/sparql-generate/fn/>
PREFIX ite: <http://w3id.org/sparql-generate/ite/>
```

This document overviews these functions, and gives an example for each. The javadoc also contains documentation for [iterator functions](apidocs/com/github/thesmartenergy/sparql/generate/jena/iterator/library/package-summary.html) and [custom SPARQL functions](apidocs/com/github/thesmartenergy/sparql/generate/jena/function/library/package-summary.html).

## Generating RDF from XML

### Iterator function http://w3id.org/sparql-generate/ite/XPath

```
set of literals http://w3id.org/sparql-generate/ite/XPath( xsd:string message, xsd:string xpath )
```

Lists XML entities that are result of the execution of XPath `xpath` over document `message`.

For example, let be the following partial solution binding:

```
?message => "<reading sensor='s12' unit='mmHg'><value time='12:00'>768</value><value time='13:00'>756</value></reading>"
```

Then iterator clause `ITERATOR ite:XPath( ?message, "/reading/value" ) AS ?value` leads to the following set of partial solution bindings:

```
?message => "<reading sensor='s12' unit='mmHg'><value time='12:00'>768</value><value time='13:00'>756</value></reading>" , ?value => "<value time='12:00'>768</value>"
?message => "<reading sensor='s12' unit='mmHg'><value time='12:00'>768</value><value time='13:00'>756</value></reading>" , ?value => "<value time='13:00'>756</value>"
```

This iterator function uses the standard Java library `javax.xml.xpath`.

### Custom SPARQL function http://w3id.org/sparql-generate/fn/XPath

```
xsd:string http://w3id.org/sparql-generate/fn/XPath( xsd:string message, xsd:string xpath )
```

This iterator uses the standard Java library 'javax.xml.xpath'.

## Generating RDF from JSON

### Iterator function http://w3id.org/sparql-generate/ite/JSONPath

```
set of literals http://w3id.org/sparql-generate/ite/JSONPath( xsd:string message, xsd:string json_path_jayway )
```

Lists JSON documents that are result of the execution of JSON Path `json_path_jayway` over document `message`.

For example, let be the following partial solution binding:

```
?message => "{ "x" : [ 1 , 2.0, "tt" , { } ] }"
```

Then iterator clause `ITERATOR ite:JSONPath( ?message, "$.x[1:4]" ) AS ?value` leads to the following set of partial solution bindings:

```
?message => "{ 'x' : [ 1 , 2.0, 'tt' , { } ] }" , ?value => "1"^^xsd:integer
?message => "{ 'x' : [ 1 , 2.0, 'tt' , { } ] }" , ?value => "2.0"^^xsd:decimal
?message => "{ 'x' : [ 1 , 2.0, 'tt' , { } ] }" , ?value => "tt"
?message => "{ 'x' : [ 1 , 2.0, 'tt' , { } ] }" , ?value => "{ }"
```

This iterator function uses library [JsonPath from GitHub user jayway](https://github.com/jayway/JsonPath).

### Iterator function  http://w3id.org/sparql-generate/ite/JSONListKeys

```
set of xsd:string http://w3id.org/sparql-generate/ite/JSONListKeys( xsd:string message )
```

Lists the keys of JSON object encoded in `message`.

For example, let be the following partial solution binding:

```
?message => "{ 'a' : 1 , 'b' : 2 , 'c' : 3 }"
```

Then iterator clause `ITERATOR ite:JSONPath( ?message ) AS ?value` leads to the following set of partial solution bindings:

```
?message => "{ 'a' : 1 , 'b' : 2 , 'c' : 3 }" , ?value => "a"
?message => "{ 'a' : 1 , 'b' : 2 , 'c' : 3 }" , ?value => "b"
?message => "{ 'a' : 1 , 'b' : 2 , 'c' : 3 }" , ?value => "c"
```


### Iterator function  http://w3id.org/sparql-generate/ite/JSONListElement

```
set of xsd:string http://w3id.org/sparql-generate/ite/JSONListElement( xsd:string message , xsd:string json_path_jayway )
```

Like `rqg:ite:JSONPath`, but embeds each solution in a JSON object to ease the generation of RDF lists from the result set.

For example, let be the following partial solution binding:

```
?message => "{ 'a' : 'aaa' , 'b' : 'bbb' , 'c' : 'ccc' }"
```

Then iterator clause `ITERATOR ite:JSONPath( ?message , '$.[*]' ) AS ?value` leads to the following set of partial solution bindings:

```
?message => "{ 'a' : 1 , 'b' : 2 , 'c' : 3 }" , ?value => "{ 'element' : 'aaa' , 'position' : 1 , 'hasNext' : true }"
?message => "{ 'a' : 1 , 'b' : 2 , 'c' : 3 }" , ?value => "{ 'element' : 'bbb' , 'position' : 2 , 'hasNext' : true }"
?message => "{ 'a' : 1 , 'b' : 2 , 'c' : 3 }" , ?value => "{ 'element' : 'ccc' , 'position' : 3 , 'hasNext' : false }"
```

This iterator uses library [JsonPath from GitHub user jayway](https://github.com/jayway/JsonPath).

### Custom SPARQL function http://w3id.org/sparql-generate/fn/JSONPath

```
xsd:string http://w3id.org/sparql-generate/fn/JSONPath( xsd:string message, xsd:string json_path_jayway )
```

## Generating RDF from CSV

### Iterator function http://w3id.org/sparql-generate/ite/CSV

```
set of xsd:string http://w3id.org/sparql-generate/ite/CSV( xsd:string message, xsd:string column )
```

Queries CSV conformant to [RFC 4180](https://tools.ietf.org/html/rfc4180).

This iterator function generates a set of CSV documents from a CSV document.
See also [the javadoc](apidocs/com/github/thesmartenergy/sparql/generate/jena/iterator/library/ITE_CSV.html).

Use is `ite:CSV( literal message, literal colum)`, where `message` is the CSV document, and `columns` is the name of a colum.

For example, let be the following partial solution binding (`?message` is bound to a multi-line literal in Turtle):

```
?message => """x,y
1,2
3,4"""
```

Then iterator clause 'ITERATOR ite:CSV( ?message, "x" ) AS ?x' leads to the following set of partial solution bindings:

```
?x => 2 , ?message => """x,y
1,2
3,4"""

?x => 4 , ?message => """x,y
1,2
3,4"""
```

This iterator function uses library [SuperCSV](http://super-csv.github.io/super-csv/) 

### Custom SPARQL function http://w3id.org/sparql-generate/fn/CSV

Queries CSV conformant to [RFC 4180](https://tools.ietf.org/html/rfc4180).

```
xsd:string http://w3id.org/sparql-generate/fn/CSV( xsd:string message, xsd:string column )
```

### Iterator function http://w3id.org/sparql-generate/ite/CustomCSV


```
set of xsd:string http://w3id.org/sparql-generate/ite/CustomCSV( xsd:string message, xsd:string query )
```

Queries other forms of CSV.

This iterator function uses library [SuperCSV](http://super-csv.github.io/super-csv/) 

### Custom SPARQL function http://w3id.org/sparql-generate/fn/CustomCSV

```
xsd:string http://w3id.org/sparql-generate/fn/CustomCSV( xsd:string message, xsd:string column )
```

## Generating RDF from HTML

### Iterator function http://w3id.org/sparql-generate/ite/CSSPath


```
set of xsd:string http://w3id.org/sparql-generate/ite/CSSPath( xsd:string message, xsd:string css3selector )
```

Queries a HTML document with CSS3 selectors.

This iterator function uses the [JSOUP](https://jsoup.org/) Java HTML parser.

### Custom SPARQL function http://w3id.org/sparql-generate/fn/HTMLTag

```
xsd:string http://w3id.org/sparql-generate/fn/HTMLTag( xsd:string message, xsd:string tag_name )
```

### Custom SPARQL function http://w3id.org/sparql-generate/fn/HTMLAttribute

```
xsd:string http://w3id.org/sparql-generate/fn/HTMLAttribute( xsd:string message, xsd:string attribute_name )
```


## Generating RDF from plain text

### Iterator function http://w3id.org/sparql-generate/ite/Split


```
set of xsd:string http://w3id.org/sparql-generate/ite/Split( xsd:string message, xsd:string separator )
```

Splits string 'message' with respect to 'separator'


For example, let be the following partial solution binding:

```
?message => "a,b,c"
```

Then iterator clause 'ITERATOR ite:Split( ?message, "," ) AS ?x' leads to the following set of partial solution bindings:

```
?message => "a,b,c" , ?x => "a"
?message => "a,b,c" , ?x => "b"
?message => "a,b,c" , ?x => "c"
```

### Custom SPARQL function http://w3id.org/sparql-generate/fn/SplitAtPosition

```
xsd:string http://w3id.org/sparql-generate/fn/XPath( xsd:string message, xsd:string regex , xsd:integer position )
```


