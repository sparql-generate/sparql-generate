# What is SPARQL-Generate ?

SPARQL-Generate stands for SPARQL-Generate Protocol and RDF Generation Language. The aim of SPARQL-Generate `GENERATE` queries is to offer a simple template-based option to interpret documents as proper RDF, with a formal abstract syntax and semantics grounded on SPARQL 1.1, making it easy to implement on top of any SPARQL 1.1 implementation. 

## What you can find on this website


On this site you will find:

* An overview of SPARQL-Generate (this document);
* For the SPARQL RDF Generation Language:
    * The [specification](language.html);
    * A [form to test the SPARQL RDF Generation Language online](language-form.html);
    * The [description of an online API](language-api.html) to start using the SPARQL RDF Generation Language;
* For the SPARQL-Generate Protocol:
    * The [specification](protocol.html);
    * A [form to test the SPARQL-Generate Protocol online](protocol-form.html);
    * The [description of an online API](protocol-api.html) to start using the SPARQL-Generate Protocol;
* The description of the reference implementation over [Apache Jena](https://jena.apache.org/):
    * The [reference guide for iterator functions and SPARQL custom functions](functions.html) that enable to generate RDF from XML, JSON, CSV, HTML, and plain text;
    * A guide to get started with the [library](getStarted.html);
    * The [reference Java documentation](apidocs/index.html);
* Other information [about this project](project-info.html), the [mailing list](mail-lists.html), and how to get involved.


## What use cases for SPARQL-Generate ?

SPARQL-Generate is already in use for the following use cases:

- Give ways to interpret JSON or even CBOR documents as RDF (ongoing work in the ITEA 2 SEAS project: serve the consumption data of the GECAD microgrid as CBOR, and point to the SPARQL-Generate `GENERATE` query that enables to interpret it as RDF. More generally, any kind of document, XML, JSON, CBOR, can be interpreted as RDF using the proper SPARQL-Generate `GENERATE` query with proper *iterator functions*)
- Generate a whole part of a vocabulary from a configuration file, and ensure that specific naming conventions are respected for resource URIs, labels and comments (in the ITEA 2 SEAS project: the [ontology of Multi-Dimensional Quantities](http://w3id.org/multidimensional-quantity/), is parametrized and generated from JSON configuration files);
- generate RDF from several open data sources (ongoing work in the OpenSensingCity French ANR project)
- generate RDF from the green-button API


# The Protocol

The protocol part of SPARQL-Generate enables a web client or a web server to point to the IRI of a SPARQL-Generate document that may be used to interpret the request (or the response) payload as RDF. It enables the following scenarios:

* A HTTP client can send a HTTP request in a non-RDF format, and tell the server how it may generate a RDF Graph from it;
* A HTTP server can send a HTTP response in a non-RDF format, and tell the client how it may generate a RDF Graph from it.

See the [protocol.html](full description of the protocol).

# The RDF Generation Language

The RDF Generation Language part of SPARQL-Generate extends the SPARQL 1.1 Query language, and offers a simple template-based RDF Graph generation from RDF literals. 

Other languages enable to describe mapping from documents to RDF. By design, SPARQL-Generate extends SPARQL and hence offers an alternative that presents the following advantages:

- anyone familiar with SPARQL can easily learn SPARQL-Generate;
- implementing over existing SPARQL engines is simple;
- SPARQL-Generate leverages the expressivity of SPARQL 1.1: Aggregates, Solution Sequences and Modifiers, SPARQL functions and their extension mechanism.

SPARQL-Generate `GENERATE` extends the SPARQL 1.1 recommendation with the following keywords:

The general structure of a SPARQL-Generate query is as follows:

```
PREFIX declarations          --- same as SPARQL 1.1
GENERATE template            --- replaces and extends SPARQL 1.1 CONSTRUCT
FROM and FROM NAMED clauses  --- same as SPARQL 1.1
ITERATOR and SOURCE clauses  --- see below
WHERE clause                 --- same as SPARQL 1.1
Solution modifiers           --- group by, order by, limit, offset,... same as SPARQL 1.1
```

As a simple example, the following SPARQL-Generate query,

```
BASE <http://example.org/>
PREFIX rqg:ite: <http://w3id.org/sparql-generate/ite/>
PREFIX rqg:ite: <http://w3id.org/sparql-generate/ite/>
GENERATE 
  { <> ?p ?o . }
ITERATOR ite:JSONListKeys(?doc) AS ?key
WHERE
  { 
    BIND( uri( ?key ) AS ?p )
    BIND( <http://w3id.org/sparql-generate/fn/JSONPath> ( ?doc, CONCAT( "$." , ?key ) ) AS ?o )
  }
```
executed with an initial binding of variable '?doc' to the following RDF literal,

```
?doc = """{ "firstname" : "Maxime" ,
            "lastname" : "Lefrancois" ,
            "birthday" : "04-26" ,
            "country" : "FR"
          }"""^^<urn:iana:mime:sparql-generate>
```
will generate the following RDF Graph:

```
@base <http://example.org/> .
<>      <firstname> "Maxime" ;
        <lastname>  "Lefrancois" ;
        <birthday>  "04/26" ;
        <country>   "FR" .
```

