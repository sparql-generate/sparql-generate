# The SPARQL-Generate Language

The SPARQL-Generate Language extends the SPARQL 1.1 Query language, and offers a simple template-based RDF Graph generation from RDF literals. 

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

```
ITERATOR <iterator>(args) AS ?var
```

where `<iterator>` is the IRI of the SPARQL-Generate *iterator function*. As the name `ITERATOR` suggests, `?var` will be bound to every RDF Term in the list returned by the evaluation of the iterator function over the arguments `args`.

As a simple example, consider the JSON message:

```
{ "firstname" : "Maxime" ,
  "lastname" : "Lefrancois" ,
  "birthday" : "04-26" ,
  "country" : "FR" 
}
```
The execution of the following SPARQL-Generate query,

```
BASE <http://example.org/>
GENERATE 
  { <> ?p ?o . }
ITERATOR <http://w3id.org/sparql-generate/iter/JSONListKeys>(?doc) AS ?key
WHERE
  { 
    BIND( uri( ?key ) AS ?p )
    BIND( <http://w3id.org/sparql-generate/fn/JSONPath> ( ?doc, CONCAT( "$." , ?key ) ) AS ?o )
  }
```
with initial binding of variable ?doc to the following RDF literal,

```
?doc = """{ "firstname" : "Maxime" ,
            "lastname" : "Lefrancois" ,
            "birthday" : "04-26" ,
            "country" : "FR"
          }"""^^<http://www.iana.org/assignments/media-types/application/vnd.sparql-generate>
```
will generate the following RDF Graph:

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

`ACCEPT <iana urn>` is optional. If set, it must be a IANA MIME URN. For instance: `<http://www.iana.org/assignments/media-types/application/json>`. It tells the engine which Accept Header field to use when operating the GET.

The RDF Literal representation of the retrieved document is as follows:

- its lexical form is a Unicode representation of the payload;
- its datatype IRI is a IANA MIME URN, based on the internet media type of the retrieved document. For instance, if the mime type is "application/vcard+json". Then the datatype IRI will be `<http://www.iana.org/assignments/media-types/application:vcard+json>`. If the Content-Type is not defined in the response, then the datatype IRI is `xsd:string`.

For example, the following SPARQL-Generate query retrieves the document at IRI <http://country.io/capital.json>, then generates a little RDF description of the country code and the capital name of the two first countries whose code starts with an 'F'.

```
BASE <http://example.org/> 
GENERATE { 
  [] a <Country> ;
    <key> ?key ;
    <capital> ?capital.
}
SOURCE <http://country.io/capital.json> AS ?source
ITERATOR <http://w3id.org/sparql-generate/iter/JSONListKeys>(?source) AS ?key
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

One may embed other GENERATE queries in the `GENERATE` template. The `GENERATE` part of the embedded query may be a template as above, or just a URI that indicates the engine that it must fetch and execute an existing query on the web, with the current binding solutions.

## How it works

To put it simply, the execution of a SPARQL Generate is defined as follows:

1. clauses `ITERATOR` and `SOURCE` are processed in order, and one constructs a [SPARQL 1.1 VALUES](https://www.w3.org/TR/sparql11-query/#inline-data) data block.
1. one constructs a SPARQL 1.1 `SELECT *` query from the SPARQL Generate query, and add the data block at the beginning of the WHERE clause.
1. this SPARQL 1.1 SELECT query is evaluated on the SPARQL dataset, and produces a set of solution bindings.
1. for each of these solution bindings, and for each element in the GENERATE template, one either produce triples, or execute the embedded query.  


## The SPARQL-Generate EBNF     

The EBNF extends the [SPARQL 1.1 EBNF](https://www.w3.org/TR/sparql11-query/#sparqlGrammar) with the following production rules:

```
[173] GenerateUnit ::= Generate
[174] Generate ::= Prologue GenerateQuery
[175] GenerateQuery ::= 'GENERATE' GenerateTemplate DatasetClause* IteratorOrSourceClause* WhereClause? SolutionModifier
[176] GenerateTemplate ::= '{' GenerateTemplateSub '}'
[177] GenerateTemplateSub ::= ConstructTriples? ( SubGenerateQuery ConstructTriples? )*
[178] IteratorOrSourceClause ::= IteratorClause | SourceClause
[179] IteratorClause ::= 'ITERATOR' FunctionCall 'AS' Var
[180] SourceClause ::= 'SOURCE' VarOrIri ( 'ACCEPT' VarOrIri )? 'AS' Var()
[181] SubGenerateQuery ::= 'GENERATE' ( SourceSelector | GenerateTemplate ) ( IteratorOrSourceClause* SolutionModifier '.' )?
```

## IANA considerations.

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
   Maxime Lefrançois <maxime.lefrancois.86@gmail.com>

The Internet Media type of a SPARQL-Generate query is `application/vnd.sparql-generate`, with file extension `*.rqg`.
