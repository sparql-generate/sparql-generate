# GENERATE

A `GENERATE` query outputs an RDF graph. Its template is a set of triple patterns, as in a SPARQL `CONSTRUCT` query, instantiated once per solution. Triples whose instantiation is not valid RDF (an unbound variable, a literal as subject) are not generated.

## From inline data

Without any document, a `GENERATE` query works as a SPARQL `CONSTRUCT` query. This one takes its solutions from a `VALUES` block.

!example generate-values

## Embedded expressions

Expressions between braces inside IRIs and literals are evaluated for each solution. `?{ expression }` (or `${ expression }`) stands for the value of an expression wherever a variable is allowed; here, an integer computed from the birth date.

!example embedded-expressions

## Nested GENERATE

A template can contain other `GENERATE` queries, each terminated by a dot. A nested query is evaluated once per solution of the query that contains it, and sees its variables: here the outer query iterates over the people, and the nested query over the skills of each person. Nesting avoids repeating the outer clauses in several queries, and reduces the number of solutions.

!example nested-generate

## Named GENERATE queries

A named query has an IRI and a signature. A template calls it with `GENERATE <name>(arguments) .`, which binds the parameters of the signature to the arguments. The named query below is in the queryset of the example.

!example named-generate

## Lists

`LIST( ?var )` in the object of a triple pattern generates an RDF list of the values of `?var` in the solutions, in the order of the solutions. Here the list orders people by birth date.

!example lists

With `GROUP BY`, `LIST` is an aggregate: the template is instantiated once per group, with the list of the values of the group.

!example lists-group

!!! warning "Known limitation"
    Inside a nested `GENERATE` without `GROUP BY`, the engine 2.2 builds one list with the values of all the executions of the nested query, instead of one list per execution. Use `GROUP BY` in the outer query, as above.

## Aggregates

With `GROUP BY`, the template is instantiated once per group, and `?{ aggregate }` expressions compute aggregates over the group.

!example aggregates

## The EXPRESSIONS clause

The `EXPRESSIONS` clause, after the solution modifiers, binds variables to expressions, aggregates included, evaluated on the solutions after grouping. The template can then use these variables several times.

!example expressions-clause

## Using the input dataset

The `WHERE` clause is evaluated on the input dataset, as in SPARQL, and joined with the solutions of the binding clauses. This example links the people of a CSV document to the countries of an RDF graph by their ISO code.

!example dataset

## Generating the input dataset

A `FROM` clause can contain a `GENERATE` query, whose output is added to the dataset of the query: `FROM GENERATE { ... } ... .` for the default graph, or `FROM GENERATE { ... } ... NAMED <graph> .` for a named graph. The generating query can also be a call to a named query: `FROM GENERATE <name>(arguments) .`. The `WHERE` clause can then query graphs generated from documents. Here the generated graph gives the nationality of each person, and the `WHERE` clause joins people of the same nationality.

!example from-generate

## Streaming

With the option `--stream` of the [command line](../cli.md), or *return stream* in the playground, the engine writes each triple as soon as it is generated, instead of building the graph first. Combined with the batched iterators (`iter:CSV` with a batch size, `iter:JSONSurfer`) and the stream iterators (`iter:HTTPGet`, `iter:MQTTSubscribe`, `iter:WebSocket`), it transforms documents that do not fit in memory and endless streams.
