# What is SPARQL-Generate ?

RDF aims at being the universal abstract data model for structured data on the Web. While there is effort to convert data in RDF, the vast majority of data available on the Web does not conform to RDF. Indeed, exposing data in RDF, either natively or through wrappers, can be very costly. Furthermore, in the emerging Web of Things, resource constraints of devices prevent from processing RDF graphs. Hence one cannot expect that all the data on the Web be available as RDF anytime soon. 

SPARQL-Generate is an extension of SPARQL for querying not only RDF datasets but also documents in arbitrary formats.

SPARQL-Generate is a protocol to automatically determine what SPARQL-Generate query to use with a given data format or even per payload in HTTP or CoAP messages.

SPARQL-Generate has a first reference implementation on top of Apache Jena, which currently enables to query and transform web documents in XML, JSON, CSV, HTML, CBOR, and plain text with regular expressions.


## A language to generate RDF from RDF datasets and documents in arbitrary formats

SPARQL-Generate is an extension of SPARQL 1.1 for querying not only RDF datasets but also documents in arbitrary formats. It offers a simple template-based option to generate RDF Graphs from documents, and presents the following advantages:

- Anyone familiar with SPARQL can easily learn SPARQL-Generate;
- SPARQL-Generate leverages the expressivity of SPARQL 1.1: Aggregates, Solution Sequences and Modifiers, SPARQL functions and their extension mechanism.
- It integrates seamlessly with existing standards for consuming Semantic Web data, such as SPARQL or Semantic Web programming frameworks.

As an example, the following SPARQL-Generate query,

```
BASE <http://example.org/>
PREFIX iter: <http://w3id.org/sparql-generate/iter/>
PREFIX fn: <http://w3id.org/sparql-generate/fn/>
GENERATE 
  { <> ?p ?o . }
SOURCE <http://example.org/profile> AS ?doc
ITERATOR iter:JSONListKeys(?doc) AS ?key
WHERE
  { 
    BIND( uri( ?key ) AS ?p )
    BIND( fn:JSONPath ( ?doc, CONCAT( "$." , ?key ) ) AS ?o )
  }
```
evaluated on a literals set where `<http://example.org/profile>` is the name for the following literal:

```
"""{ "firstname" : "Maxime" ,
     "lastname" : "Lefrancois" ,
     "birthday" : "04-26" ,
     "country" : "FR"
   }"""^^<urn:iana:mime:sparql-generate>
```
evaluates to the following RDF Graph:

```
@base <http://example.org/> .
<>      <firstname> "Maxime" ;
        <lastname>  "Lefrancois" ;
        <birthday>  "04/26" ;
        <country>   "FR" .
```

See also:
* the [language specification](language.html);
* a [form to test the SPARQL-Generate Language online](language-form.html);
* the [description of an online API](language-api.html) to start using the SPARQL-Generate Language.


## A protocol to interpret HTTP payloads as RDF

The protocol part of SPARQL-Generate enables a web client or a web server to point to the IRI of a SPARQL-Generate document that may be used to interpret the request (or the response) payload as RDF. It enables the following scenarios:

* A HTTP client can send a HTTP request in a non-RDF format, and tell the server how it may generate a RDF Graph from it;
* A HTTP server can send a HTTP response in a non-RDF format, and tell the client how it may generate a RDF Graph from it.

See also:
* the [specification of the protocol](protocol.html) (ongoing work);
* a [form to test the SPARQL-Generate Protocol online](protocol-form.html);
* the [description of an online API](protocol-api.html) to start using the SPARQL-Generate Protocol;

## A first implementation on top of Apache Jena

Since we leverage the expressiveness of SPARQL and its function extension mechanism, its implementation on top of a SPARQL engine is straightforward. This website describes a first implementation over Apache Jena, which currently enables to query and transform web documents in XML, JSON, CSV, HTML, CBOR, and plain text with regular expressions.

See also:
* a description of how to use SPARQL-Generate as [an executable JAR](language-cli.html);
* a guide to get started with [the Java library](get-started.html);
* the [guide for SPARQL binding functions and SPARQL-Generate iterator functions](functions.html) that enable to generate RDF from documents in XML, JSON, CSV, HTML, and plain text;
* the [reference Java documentation](apidocs/index.html);
* a [tests report](tests-reports.html) with test from the related work and more;


## Applications

SPARQL-Generate is already in use in the following projects:

* In the ITEA 3 SEAS project:
    * with the [CNR](www.cnr.tm.fr), their [Electric Vehicle Smart Charging Provider API](http://cnr-seas.cloudapp.net/scp) exposes optimized charge plan in XML, but also sends a link to a SPARQL-Generate query, so that the client can interpret the response as RDF.
    * with [TELECOM Saint-Etienne](https://www.telecom-st-etienne.fr/), an [implementation of Green-Button optimized for lightweight computers](http://w3id.org/seas/green-button/) also sends links to SPARQL-Generate queries, so that clients can interpret the response as RDF.
    * ongoing work with [GECAD ](http://gecad.isep.ipp.pt): serve the consumption data of the Instituto Politecnico de Porto microgrid as CBOR, and point to the SPARQL-Generate `GENERATE` query that enables to interpret it as RDF. 
    * the [ontology of Multi-Dimensional Quantities](https://w3id.org/multidimensional-quantity/), is parametrized and generated from JSON configuration files. SPARQL-Generate is used to generate a whole part of a vocabulary from a configuration file, and ensure that specific naming conventions are respected for resource URIs, labels and comments;
* In the [OpenSensingCity French ANR project](http://opensensingcity.emse.fr/):
    * ongoing work: generate RDF from several open data sources for the [Grand Lyon](www.grandlyon.com/).

## What you can find on this website

* The description of the reference implementation over [Apache Jena](https://jena.apache.org/):
    
* Other information [about this project](project-info.html), the [mailing list](mail-lists.html), and how to get involved.

