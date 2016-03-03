# SPARQL-Generate-Jena

SPARQL-Generate implementation over Apache Jena.


## Available in version 0.1:

Don't try the example above in preliminary version 0.1.

Preliminary version 0.1 implements:
- keywords `GENERATE` and `SELECTOR`.
- there may be only one `SELECTOR` clause, used as follows: `SELECTOR <selector>(args)` (there is no `AS`).
- selector for JSON: <http://w3id.org/sparql-generate/sel/JSONPath_jayway>
- SPARQL function for JSON: <http://w3id.org/sparql-generate/sel/JSONPath_jayway_string>


# What is SPARQL-Generate ?

SPARQL-Generate stands for SPARQL-Generate Protocol and RDF Generation Language. The aim of SPARQL-Generate `GENERATE` queries is to offer a simple template-based option to interpret any document as proper RDF, with a well defined semantics grounded on the SPARQL 1.1. 

SPARQL-Generate is already in use for the following use cases:
- Give ways to interpret JSON or even CBOR documents as RDF (ongoing work in the ITEA 2 SEAS project: serve the consumption data of the GECAD microgrid as CBOR, and point to the SPARQL-Generate `GENERATE` query that enables to interpret it as RDF. More generally, any kind of document, XML, JSON, CBOR, can be interpreted as RDF using the proper SPARQL-Generate `GENERATE` query with proper *selectors*)

- Generate a whole part of a vocabulary from a configuration file, and ensure that specific naming conventions are respected for resource URIs, labels and comments (ongoing work in the ITEA 2 SEAS project: the ontology of Faceted Spaces, that is used to describe multi-dimensional quantities, is parametrized and generated from a JSON configuration file);
- generate RDF from several open data sources (ongoing work in the OpenSensingCity French ANR project)


## A RDF Generation Language

The RDF Generation Language part of SPARQL-Generate extends the SPARQL 1.1 Query language, and offers a simple template-based RDF Graph generation from RDF literals. 

Other languages enable to describe mapping from documents to RDF. By design, SPARQL-Generate extends SPARQL and hence offers an alternative that presents the following advantages:
- anyone familiar with SPARQL can easily learn SPARQL-Generate;
- naive implementation over existing SPARQL engines is simple;
- SPARQL-Generate leverages the expressivity of SPARQL 1.1: Aggregates, Solution Sequences and Modifiers, SPARQL functions and their extension mechanism.

## A Protocol

The protocol part of SPARQL-Generate enables a web client or a web server to point to the URL of a SPARQL-Generate document that may be used to interpret the request (or the response) payload as RDF.

For HTTP requests, SPARQL-Generate defines Header Field `X-SPARQL-Generate-Companion`, that is an URL where one should be able to retrieve a SPARQL-Generate document

For HTTP responses, the server may use the web-link relation type defined by SPARQL-Generate, [http://w3id.org/sparql-generate/companion](http://w3id.org/sparql-generate/companion), with the document as anchor, and the URL where one should be able to retrieve a SPARQL-Generate document as source.

The Internet Media type of a SPARQL-Generate query is `application/sparql-generate`, with file extension `*.rqg`.


## Description of the SPARQL-Generate Language

SPARQL-Generate `GENERATE` extends the SPARQL 1.1 recommendation with the following capabilities:
- `SELECTOR` and `SOURCE` clauses, at the end of the query just before SPARQL 1.1 `VALUES` clause;
- use `GENERATE` template in place of `CONSTRUCT` template in a triple pattern. The `GENERATE` template adds new capabilities to the `CONSTRUCT` template:
    - `LIST( ?x )` is used in place of a RDF Term or variable. It generates a blank node common to all solutions of the query, and that blank node is also a RDF list that contains the RDF Terms bound to variable `?x` accross all of the solutions.
    - sub-`GENERATE` use the current binding to execute a new `GENERATE` query.
    - `EXPR( expr )` is used in place of a RDF Term or variable in a triple pattern. It is equivalent to a unused variable `?x`, that would be bound by a `BIND` clause at the end of the `WHERE` template.

### `SELECTOR` and `SOURCE`

`SELECTOR` or `SOURCE` clauses can be used in any order and any number just before the SPARQL 1.1 `VALUES` clause. At execution time, they are resolved just after the `VALUES` clause, in reverse order:

#### `SELECTOR` 

The `SELECTOR` clause is used as follows: `SELECTOR <selector>(args) AS ?var`. 

`<selector>` is the IRI of a SPARQL-Generate *Selector*. A SPARQL-Generate *Selector* is similar to a SPARQL 1.1 Function, except it *returns a list of RDF Terms*.

Then for every RDF Term in the list, the rest of the query is executed with `?var` bound to the RDF Term ;

`?var` may have already been bound. In such cases, the binding is *overriden*.

#### `SOURCE`

The `SOURCE` clause is used as follows: `SOURCE <source> ACCEPT "mime" AS ?var`. 

If `<source>` is bound to a IRI at that time, then the engine must operate a GET to that IRI, bound variable `?var` to the RDF Literal representation of the retrieved document in the `?var`, and continue the execution of the query. 

The variable `?var` must not have been bound before. For now, only source IRIs with `http` scheme are considered. In the future, `https`, `coap`, and `coaps` schemes may be considered.

`ACCEPT "mime"` is optional, and tells the engine which HTTP Accept Header Field it should use. mime MUST be a string literal, or IANA schemed URN (yet to be defined by IETF in a RFC), such that "application/vcard+json".

The lexical form of the RDF Literal representation of the retrieved document is a Unicode representation of the payload.
If this payload is encoded as text, then the lexical form is the Unicode representation of that text. Else, if the payload is encoded in a binary format, then the lexical form is the base64 representation of that binary document.

The datatype IRI of the RDF Literal representation of the retrieved document is a IANA schemed URN (yet to be defined by IETF in a RFC), defined based on the internet media type  of the retrieved document. For instance, if the mime type is "application/vcard+json". Then the datatype IRI will be `<urn:iana:mime:application:vcard+json>`

### `GENERATE`

The `GENERATE` clause replaces the `CONSTRUCT` clause, and augments it with sub-`GENERATE` queries, and `LIST` and `EXPR` constructors.

#### sub-`GENERATE`

A sub-`GENERATE` query generates more RDF based on the current binding solution.

It has the same syntax as a GENERATE query, except it MUST end with character `.`, and:

##### URI instead of the Generate pattern.

Instead of `GENERATE {...}`, one may use `GENERATE <iri>`. In such cases, the engine must operate a GET at the given IRI, (only http scheme is considered for now) with Accept Header Field set to "application/sparql-generate". If it retrieves a SPARQL-Generate query, then this query is executed with the current binding solution.

##### `USE BINDINGS` clause

By default, the sub-`GENERATE` query is evaluated with the current binding solution as initial binding. In most use cases, it is sufficient to pass only part of the binding solution, and it is also necessary to rename the variables. For this purpose, the sub-`GENERATE` query may end with a `WITH BINDINGS` clause. 

This clause is used as follows `WITH BINDINGS ( ?currentvar1 AS ?newvar1 ) ... ( ?currentvarn AS ?newvarn )`. 

only variables `?newvari` are passed to the sub-`GENERATE` query, and they are bound to what variables `?currentvari` is bound. 


### Grammar

The EBNF extends the [SPARQL 1.1 Grammar](https://www.w3.org/TR/sparql11-query/#sparqlGrammar).

```
[174]	GenerateUnit	::=	Generate
[175]   Generate        ::= SPARQL 1.1 Prologue GenerateQuery
[175]	GenerateQuery   ::=	'GENERATE' GenerateTemplate GenerateWhereClause SPARQL 1.1 SolutionModifier SelectorClause
[177]	GenerateTemplate	::=	'{' SPARQL 1.1 ConstructTriples? ( SubGenerateQuery SPARQL 1.1 ConstructTriples? )* '}'
[178]	GenerateWhereClause	::=	'WHERE'? '{' GenerateGroupPattern '}'
[178]	SelectorClause	::=	( 'SELECTOR' SPARQL 1.1 FunctionCall )?
[179]   GenerateGroupPattern ::= '{' ( GenerateSubSelect | GeneratePattern ) '}'
[179]   GeneratePattern ::= GenerateGroupOrUnionPattern | GenerateOptionalPattern | GenerateMinusPattern | SPARQL 1.1 ServiceGraphPattern | SPARQL 1.1 Filter | SPARQL 1.1 Bind | SPARQL 1.1 InlineData
[ ]     GenerateGroupOrUnionPattern ::= GenerateGroupPattern ( 'UNION' GenerateGroupPattern )*
[ ]     GenerateOptionalPattern ::= 'OPTIONAL' GenerateGroupPattern
[ ]     GenerateMinusPattern ::= 'MINUS' GenerateGroupPattern
[181]	GenerateSubSelect	::=	SelectClause GenerateWhereClause SPARQL 1.1 SolutionModifier SelectorClause
[181]	SubGenerateQuery	::=	'GENERATE' ( SPARQL 1.1 SourceSelector | GenerateTemplate GenerateWhereClause ) SPARQL 1.1 SolutionModifier SelectorClause 
qsdfqsdf
```

