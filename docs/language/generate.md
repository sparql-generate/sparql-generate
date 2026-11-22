# GENERATE

A `GENERATE` query outputs an RDF graph. Its template is a set of triple patterns, as in a SPARQL `CONSTRUCT` query, instantiated once per solution. Triples whose instantiation is not valid RDF (an unbound variable, a literal as subject) are not generated.

## From inline data

Without any document, a `GENERATE` query works as a SPARQL `CONSTRUCT` query. This one takes its solutions from a `VALUES` block.

!example generate-values

## Embedded expressions

Expressions between braces inside IRIs and literals are evaluated for each solution. `?{ expression }` (or `${ expression }`) stands for the value of an expression wherever a variable is allowed; here, an integer computed from the birth date.

!example embedded-expressions

## Nested GENERATE

A template can contain other `GENERATE` queries, each terminated by a dot. A nested query is evaluated on the solutions of the query that contains it: its binding clauses and its `WHERE` clause extend each of these solutions, so it sees their variables. Here the outer query iterates over the people, and the nested query over the skills of each person. Nesting avoids repeating the outer clauses in several queries, and reduces the number of solutions.

!example nested-generate

## Named GENERATE queries

A named query has an IRI and a signature. A template calls it with `GENERATE <name>(arguments) .`, which binds the parameters of the signature to the arguments. The named query below is in the queryset of the example.

!example named-generate

## Named graphs

A `GRAPH` block of the template generates its triples in a named graph, as the quad patterns of a SPARQL 1.1 Update or of a Jena `CONSTRUCT` query: the output is then an RDF dataset. The triples outside `GRAPH` blocks are in the default graph. The graph is a constant IRI, a variable, an IRI with embedded expressions, or an embedded expression `?{ ... }`; a dot after the block is optional.

Here the names of the people are in the graph of their country, and their birth dates in a graph computed by a `BIND` clause from the variables of the iterator.

!example named-graphs

For each solution:

- a graph variable that is unbound, or bound to a literal, generates nothing, as a triple whose term is unbound or not valid RDF;
- a graph variable bound to a blank node names a graph: RDF 1.1 datasets, TriG, N-Quads and Jena accept blank nodes as graph names. A `GRAPH` block cannot name a blank node itself (`GRAPH _:g` is a syntax error, as in SPARQL);
- a blank node label denotes the same blank node inside and outside the `GRAPH` blocks of the template.

`GRAPH` blocks cannot be nested. A nested `GENERATE` placed in a `GRAPH` block generates its default graph in that graph; its own `GRAPH` blocks keep their graph. A call to a named `GENERATE` query from a `GRAPH` block follows the same rule: the triples of its default graph go to the graph of the block. A nested query in a `GRAPH ?g` block is evaluated separately for the solutions of each value of `?g`.

A query without `GRAPH` block gives exactly the same graph as before. The output of a query with `GRAPH` blocks is written in TriG by default on the [command line](../cli.md), or in N-Quads with `--format-output NQUADS`; a format that holds triples only (Turtle, N-Triples, RDF/XML, HDT) is refused. In Java, `RootPlan.execGenerateDataset` returns the dataset; `execGenerate`, which returns a model, returns the default graph and logs a warning that the named graphs were ignored. A `FROM GENERATE` clause builds one graph: the named graphs of its query are ignored, with the same warning. The playground and the web API write the dataset in TriG (the web API in N-Quads if the request accepts `application/n-quads`).

## Lists

`LIST( ?var )` in the object of a triple pattern generates an RDF list of the values of `?var` in the solutions, in the order of the solutions. Here the list orders people by birth date.

!example lists

With `GROUP BY`, `LIST` is an aggregate: the template is instantiated once per group, with the list of the values of the group.

!example lists-group

Without `GROUP BY`, `LIST` and the other aggregates span all the solutions of their query. A nested `GENERATE` is evaluated on all the solutions of its parent at once, so a `LIST` in a nested query collects the values for all the parent solutions. To get one list per parent solution, group the nested query by the variables of the parent:

!example lists-nested

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

With the option `--stream` of the [command line](../cli.md), or *return stream* in the playground, the engine writes each triple (or quad) as soon as it is generated, instead of building the graph first. Combined with the batched iterators (`iter:CSV` with a batch size, `iter:JSONSurfer`) and the stream iterators (`iter:HTTPGet`, `iter:MQTTSubscribe`, `iter:WebSocket`), it transforms documents that do not fit in memory and endless streams.

A batched or stream iterator delivers its solutions batch by batch, and the rest of the query is evaluated once per batch: `DISTINCT`, `GROUP BY` and the aggregates, `ORDER BY`, `LIMIT` and `OFFSET`, and `LIST(...)`, apply to each batch, not to the whole document. Their result then depends on the batch size; a query without them gives the same triples whatever the batch size.
