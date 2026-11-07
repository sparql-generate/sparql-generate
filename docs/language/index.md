# Language guide

SPARQL-Generate is SPARQL 1.1 with new query forms, new clauses, and expressions embedded in templates. Any SPARQL 1.1 `SELECT` query is a SPARQL-Generate query. The grammar is in the [reference](../reference/grammar.md); this guide describes each construct with an example that the test suite of the engine executes.

The media type of a SPARQL-Generate query is `application/vnd.sparql-generate`, and its file extension is `.rqg` (see [Media type](../reference/media-type.md)).

## Query forms

| Form | Output | Page |
|:--|:--|:--|
| `GENERATE` | an RDF graph, or a stream of triples | [GENERATE](generate.md) |
| `SELECT` | a sequence of solutions, as in SPARQL 1.1 | [SELECT](select.md) |
| `TEMPLATE` | a text, or a stream of strings | [TEMPLATE](template.md) |
| `FUNCTION` | a function, called in expressions | [FUNCTION](function.md) |
| `PERFORM` | actions; parsed, not executed by the 2.2 engine | below |

## Structure of a query

A `GENERATE`, `SELECT` or `TEMPLATE` query has the following parts, in this order:

```sparql-generate
PREFIX ...                       # prologue, as in SPARQL
GENERATE <name> (?param1, ?param2) # query form, optional name and signature
{ ... }                          # template: triples, or text for TEMPLATE
FROM ... / FROM NAMED ...        # dataset clauses, as in SPARQL
SOURCE ... AS ?var               # binding clauses, any number, in any order
ITERATOR ... AS ?var1 ?var2
BIND( ... AS ?var )
WHERE { ... }                    # graph pattern, optional
GROUP BY ... ORDER BY ... LIMIT  # solution modifiers, as in SPARQL
EXPRESSIONS ( ... AS ?var )      # post-processing of the solutions
```

The engine evaluates a query in three steps:

1. The binding clauses are evaluated in order. A `SOURCE` clause binds a variable to the content of a document; an `ITERATOR` clause calls an iterator function, which returns rows, and binds its variables to the values of each row. The result is a sequence of solutions.
2. The `WHERE` clause is evaluated against the input dataset, and joined with the solutions of step 1, as a SPARQL `VALUES` clause would be. The solution modifiers apply.
3. The template is instantiated once per solution (or once per group with `GROUP BY` and aggregates).

The binding clauses can use the variables bound by the clauses before them, and the variables of the query that calls this query (see [nested and named queries](generate.md#nested-generate)).

## Expressions embedded in templates

In a template, a string between `{` and `}` inside an IRI or a literal is a SPARQL expression, evaluated for each solution and converted to a string:

| Written | Meaning |
|:--|:--|
| `<person/{?id}>` | `IRI(CONCAT("person/", STR(?id)))`, resolved against the base |
| `"{?first} {?last}"` | `CONCAT(STR(?first), " ", STR(?last))` |
| `"{?born}"^^xsd:date` | `STRDT(STR(?born), xsd:date)` |
| `"{?label}"@en` | `STRLANG(STR(?label), "en")` |
| `?{ expression }` or `${ expression }` | the value of the expression, anywhere a variable is allowed |

In an IRI or a literal of a template, `\{` and `\}` stand for the braces themselves. See [embedded expressions](generate.md#embedded-expressions).

## Named queries and their signature

A `GENERATE`, `SELECT` or `TEMPLATE` query can have an IRI as a name, followed by a signature: the list of its parameters. Another query calls it by this IRI with arguments:

- a nested `GENERATE <name>(arguments) .` in a `GENERATE` template;
- a nested `TEMPLATE <name>(arguments) .` in a `TEMPLATE` template;
- `ITERATOR <name>(arguments) AS ?var1 ?var2` for a named `SELECT` query;
- `<name>(arguments)` in an expression for a `FUNCTION` query.

The engine looks the named query up by its IRI with the media type `application/vnd.sparql-generate`: in the queryset of the [command line](../cli.md#configuration-file) or the playground, on the file system, or on the Web. Named queries make queries modular, and a named query can call itself.

## PERFORM

The grammar has a fourth form, `PERFORM`, for queries that perform actions, such as calls to Web APIs, instead of producing an output. The engine 2.2 parses `PERFORM` queries but does not execute them: the command line answers "unsupported query type".

## Differences with SPARQL 1.1

- `GENERATE` replaces `CONSTRUCT`; `CONSTRUCT`, `ASK` and `DESCRIBE` are not part of the language.
- The `WHERE` keyword and the `WHERE` clause are optional in `GENERATE` and `TEMPLATE` queries.
- The binding clauses `SOURCE`, `ITERATOR` and `BIND` come after the dataset clauses and before the `WHERE` clause.
- A `FROM` clause can generate the graph it adds to the dataset: `FROM GENERATE { ... } .`, or `FROM GENERATE { ... } NAMED <graph> .` for a named graph.
