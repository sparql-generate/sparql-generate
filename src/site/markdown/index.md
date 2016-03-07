# What is SPARQL-Generate ?

SPARQL-Generate stands for SPARQL-Generate Protocol and RDF Generation Language. The aim of SPARQL-Generate `GENERATE` queries is to offer a simple template-based option to interpret any document as proper RDF, with a well defined semantics grounded on the SPARQL 1.1. 

## Specification

The complete specification (will be) available here: http://w3id.org/sparql-generate.

In the meantime, you can subscribe to the mailing list: https://groups.google.com/forum/#!forum/sparql-generate-jena.

## What use cases for SPARQL-Generate ?

SPARQL-Generate is already in use for the following use cases:

- Give ways to interpret JSON or even CBOR documents as RDF (ongoing work in the ITEA 2 SEAS project: serve the consumption data of the GECAD microgrid as CBOR, and point to the SPARQL-Generate `GENERATE` query that enables to interpret it as RDF. More generally, any kind of document, XML, JSON, CBOR, can be interpreted as RDF using the proper SPARQL-Generate `GENERATE` query with proper *iterator functions*)
- Generate a whole part of a vocabulary from a configuration file, and ensure that specific naming conventions are respected for resource URIs, labels and comments (ongoing work in the ITEA 2 SEAS project: the ontology of Multi-Dimensional Quantities, is parametrized and generated from JSON configuration files);
- generate RDF from several open data sources (ongoing work in the OpenSensingCity French ANR project)

# The RDF Generation Language

The RDF Generation Language part of SPARQL-Generate extends the SPARQL 1.1 Query language, and offers a simple template-based RDF Graph generation from RDF literals. 

Other languages enable to describe mapping from documents to RDF. By design, SPARQL-Generate extends SPARQL and hence offers an alternative that presents the following advantages:

- anyone familiar with SPARQL can easily learn SPARQL-Generate;
- implementing over existing SPARQL engines is simple;
- SPARQL-Generate leverages the expressivity of SPARQL 1.1: Aggregates, Solution Sequences and Modifiers, SPARQL functions and their extension mechanism.

SPARQL-Generate `GENERATE` extends the SPARQL 1.1 recommendation with the following keywords:

## General form of a SPARQL Generate Query

The general structure of a SPARQL-Generate query is as follows:

```
PREFIX declarations            -- same as SPARQL 1.1
GENERATE template              -- replaces and extends SPARQL 1.1 CONSTRUCT
FROM and FROM NAMED clauses    -- same as SPARQL 1.1
ITERATOR and SOURCE clauses    -- see below
WHERE clause                   -- same as SPARQL 1.1
Solution modifiers              -- group by, order by, limit, offset,... same as SPARQL 1.1
```

## `ITERATOR` clause

The `ITERATOR` clause uses the concept of SPARQL-Generate *iterator function*. A SPARQL-Generate *iterator* is similar to a SPARQL 1.1 Function, except it *returns a list of RDF Terms* instead of just one. The syntax of the `ITERATOR`clause is the following:

```ITERATOR <iterator>(args) AS ?var```

where `<iterator>` is the IRI of the SPARQL-Generate *iterator function*. As the name `ITERATOR` suggests, `?var`will be bound to every RDF Term in the list returned by the evaluation of the iterator function over the arguments `args`.

As a simple example, consider the JSON message:

```
{ "firstname" : "Maxime" ,
  "lastname" : "Lefrancois" ,
  "birthday" : "04-26" ,
  "country" : "FR" ,
}
```
If this message is bound to variable `?doc`, then the following SPARQL-Generate query:

```
BASE <http://example.org/>
GENERATE 
  { <> ?p ?o . }
ITERATOR <http://w3id.org/sparql-generate/ite/JSONListKeys>(?doc) AS ?key
WHERE
  { 
    BIND( uri( ?key ) AS ?p )
    BIND( <http://w3id.org/sparql-generate/fn/JSONPath> ( ?doc, CONCAT( "$." , ?key ) ) AS ?o )
  }
```
Then one retrieve the following RDF Graph (serialized here in Turtle):

```
@base <http://example.org/> .
<>      <firstname> "Maxime" ;
        <lastname>  "Lefrancois" ;
        <birthday>  "04/26" ;
        <country>   "FR" .
```


## `SOURCE` clause

The `SOURCE` clause enables to fetch a web document, and to bind it as a RDF Literal to a variable. Its syntax is as follows:

```
SOURCE <source> ACCEPT <iana urn> AS ?var
``` 

If `<source>` is a HTTP IRI, then the engine will operate a HTTP GET to that IRI (or use a local cache), then bind variable `?var` to the RDF Literal representation of the retrieved document. 

`ACCEPT <iana urn>` is optional. If set, it must be a IANA MIME URN. For instance: `<urn:iana:mime:application/json>`. It tells the engine which Accept Header field to use when operating the GET.

The RDF Literal representation of the retrieved document is as follows:

- its lexical form is a Unicode representation of the payload. 
- its datatype IRI is a IANA MIME URN, based on the internet media type of the retrieved document, or `xsd:string` if the Content-Type is not defined in the response. For instance, if the mime type is "application/vcard+json". Then the datatype IRI will be `<urn:iana:mime:application:vcard+json>`.

For example, the following SPARQL-Generate query retrieves the document at URL `<http://country.io/capital.json>`, then generates a little RDF description of the country code and the capital name of the two first countries whose code starts with an 'F'.

```
BASE <http://example.org/> 
GENERATE { 
  [] a <Country> ;
    <key> ?key ;
    <capital> ?capital.
}
SOURCE <http://country.io/capital.json> AS ?source
ITERATOR <http://w3id.org/sparql-generate/ite/JSONListKeys>(?source) AS ?key
WHERE {
   FILTER( STRSTARTS(?key,"F") )
   BIND( CONCAT('$.', ?key) AS ?query )
   BIND( <http://w3id.org/sparql-generate/fn/JSONPath>(?source, ?query ) AS ?capital )
}
ORDER BY ?key
LIMIT 2
```

It generates the following RDF Graph:

``` 
@base <http://example.org/> .
[] a          <Country> ;
   <capital>  "Suva" ;
   <code>     "FJ" .
[] a  <Country> ;
   <capital>  "Stanley" ;
   <code>     "FK" .
[] a          <Country> ;
   <capital>  "Helsinki" ;
   <code>     "FI" .
```

## sub-`GENERATE` queries

One may embed other GENERATE queries in the `GENERATE` template. The `GENERATE` part of the embedded query may be a template as above, or just a URI that indicates the engine that it must fetch and execute an existing query on the web, with the current binding solutions. For instance, the following example   

## How it works

The execution of a SPARQL Generate is roughly defined as follows:

1. clauses `ITERATOR` and `SOURCE` are processed in order, and one constructs a [https://www.w3.org/TR/sparql11-query/#inline-data](SPARQL 1.1 VALUES) data block.
1. one constructs a SPARQL 1.1 `SELECT *` query from the SPARQL Generate query, and add the data block at the beginning of the WHERE clause.
1. this SPARQL 1.1 SELECT query is evaluated on the SPARQL dataset, and produces a set of solution bindings.
1. for each of these solution bindings, and for each element in the GENERATE template, one either produce triples, or execute the embedded query.  


# The Protocol

The protocol part of SPARQL-Generate enables a web client or a web server to point to the URL of a SPARQL-Generate document that may be used to interpret the request (or the response) payload as RDF.

## "How you, server, should interpret my lightweight HTTP request payload as RDF"

For HTTP requests, SPARQL-Generate defines Header Field X-SPARQL-Generate-Companion, If the server is RDF-enabled, it can use the value of this header field (a URL), to retrieve a SPARQL-Generate document, and interpret the input as RDF. An example of such a HTTP Request Header field:

```
SPARQL-Generate-Companion: <http://manufacturer.com/deviceX/sparql-gen>
```

## "How you, client, should interpret my lightweight HTTP resonse payload as RDF"

For HTTP responses, the server may use the web-link relation type defined by SPARQL-Generate, [http://w3id.org/sparql-generate/companion](http://w3id.org/sparql-generate/companion), with the document as anchor, and the URL where one should be able to retrieve a SPARQL-Generate document as source. If the client is RDF-enabled, it can use this SPARQL Generate query to interpret the output as RDF. An example of such a web-link in a HTTP Response Header field:

```
Link: <http://manufacturer.com/deviceX/sparql-gen>; rel="http://w3id.org/sparql-generate/companion"
```

## IANA considerations.

The Internet Media type of a SPARQL-Generate query is `application/sparql-generate`, with file extension `*.rqg`.

