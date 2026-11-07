# From RML

[RML](https://rml.io/) describes a mapping from heterogeneous sources to RDF as an RDF graph of triples maps. SPARQL-Generate describes it as a query. This page gives the SPARQL-Generate construct that corresponds to each RML construct, and the translator of RML mappings into SPARQL-Generate queries.

## A mapping and the corresponding query

The RML mapping below generates a `foaf:Person` for each row of `student.csv`.

```turtle
@prefix rr: <http://www.w3.org/ns/r2rml#> .
@prefix rml: <http://semweb.mmlab.be/ns/rml#> .
@prefix ql: <http://semweb.mmlab.be/ns/ql#> .
@prefix foaf: <http://xmlns.com/foaf/0.1/> .
@prefix ex: <http://example.com/> .

<#Student> a rr:TriplesMap ;
  rml:logicalSource [ rml:source "student.csv" ; rml:referenceFormulation ql:CSV ] ;
  rr:subjectMap [ rr:template "http://example.com/student/{ID}" ; rr:class foaf:Person ] ;
  rr:predicateObjectMap [ rr:predicate foaf:name ; rr:objectMap [ rml:reference "Name" ] ] ;
  rr:predicateObjectMap [ rr:predicate ex:practises ;
                          rr:objectMap [ rr:template "http://example.com/sport/{Sport}" ] ] .
```

The SPARQL-Generate query iterates over the rows of the same document; each part of the triples map becomes a part of the template.

!example rml-equivalent

## Correspondence of the constructs

| RML | SPARQL-Generate |
|:--|:--|
| triples map | a `GENERATE` query, or a nested `GENERATE` in a larger query |
| logical source, reference formulation `ql:CSV` | `ITERATOR iter:CSV(<source>, "column", ...) AS ?var ...` |
| reference formulation `ql:JSONPath`, iterator | `ITERATOR iter:JSONPath(<source>, "iterator", "reference", ...) AS ?item ?var ...` |
| reference formulation `ql:XPath`, iterator | `ITERATOR iter:XPath(<source>, "iterator", "reference", ...) AS ?item ?var ...` |
| `rml:reference` | a variable bound by the iterator, or a function such as `fun:JSONPath(?item, "reference")` |
| `rr:template` | an IRI or a literal with embedded expressions: `<student/{?id}>`, `"{?first} {?last}"` |
| `rr:constant` | a constant term of the template |
| `rr:class` | a triple `a <class>` |
| `rr:termType rr:BlankNode` | a blank node of the template, or `fun:bnode(string)` for a blank node identified by a value |
| `rr:language`, `rml:languageMap` | `"{?label}"@en`, or `STRLANG(?label, ?lang)` |
| `rr:datatype`, `rml:datatypeMap` | `"{?value}"^^xsd:date`, or `STRDT(?value, ?datatype)` |
| referencing object map, same logical source | the subject of the parent written in the same template |
| referencing object map with `rr:joinCondition` | two iterators, or a nested `GENERATE`, joined on a shared variable; or the parent generated with `FROM GENERATE` and joined in the `WHERE` clause |
| function maps (FnO) | SPARQL functions, functions of the [library](reference/functions.md), or [`FUNCTION`](language/function.md) queries |
| `rr:graphMap` | no construct: a `GENERATE` query outputs triples of one graph |
| RML-star, RDF 1.2 triple terms | no construct |

SPARQL-Generate has constructs with no RML counterpart: the `WHERE` clause over an RDF dataset, the [`TEMPLATE`](language/template.md) form for text output, named queries with parameters, the stream iterators (MQTT, WebSocket, HTTP polling), and content negotiation with `SOURCE ... ACCEPT`.

## The translator

[rml-to-sparql-generate](https://github.com/sparql-generate/rml-to-sparql-generate) translates an RML mapping into a SPARQL-Generate query. The translator is itself a SPARQL-Generate `TEMPLATE` query: it reads the mapping as its default graph and writes the text of the `GENERATE` query.

```sh
mkdir -p work/dataset && cp mapping.ttl work/dataset/default.ttl
cp -r rml2sparql-generate.rqg templates iterators work/
(cd work && java -jar sparql-generate.jar -d . -q rml2sparql-generate.rqg \
      -b http://example.com/base/ -o generated.rqg)
java -jar sparql-generate.jar -d data -q generated.rqg -b http://example.com/base/ -o out.nt -fo NT
```

The [README of the translator](https://github.com/sparql-generate/rml-to-sparql-generate#readme) lists the constructs it translates, RML 1.x and RML-Core, and its limits.
