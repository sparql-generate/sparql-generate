# Queries calling queries

The query forms combine: a `GENERATE` query nests other `GENERATE` queries and calls named ones, builds a graph to query with `FROM GENERATE`, and iterates over the solutions of a named `SELECT` query; a `TEMPLATE` query calls named `TEMPLATE` queries and `FUNCTION` queries. The three examples of this page share their documents and their named queries: a JSON document lists the lines of a transit network and the stops they serve, a CSV document describes each stop.

## A GENERATE query that calls other queries

The query below generates one resource per line, and:

- for each stop of the line, a nested `GENERATE` query whose template is a `GRAPH` block: the description of the stop goes to the named graph of the line;
- in that block, a call of the named `GENERATE` query `<query/stop>` with two arguments, the identifier of the stop and the IRI of the line, written as an IRI with an embedded expression, `<line/{?id}>`;
- the named query `<query/stop>` iterates over the solutions of the named `SELECT` query `<query/stop-info>`, called as an iterator: the name, zone and latitude of a stop, read in the CSV document;
- a `FROM GENERATE` clause builds a graph of the stops each line serves, and a second nested `GENERATE` query reads it in its `WHERE` clause, to link the lines that share a stop.

!example calls-generate

## A TEMPLATE query that calls other queries

The same documents give a text: one section per line, written by the named `TEMPLATE` query `<query/stops>`. It iterates over the stops of the line with the same named `SELECT` query as above, and labels each stop with the `FUNCTION` query `<fn/label>`.

!example calls-template

## An error in the data

The named `SELECT` query `<query/stop-info>` reads the latitude of a stop as a decimal number, `xsd:decimal(?latText)`. When the CSV document has a latitude that is not a number, `COALESCE` gives the value of `RAISE("No valid latitude for stop", ?id)`, which [raises an error](index.md#raising-errors): the execution stops, in the named query called by the query called by the main query, and the [command line](../cli.md) writes the message on the standard error and exits with status 1. Here the `GENERATE` query of the first example runs on a document where one latitude is `unknown`; the `TEMPLATE` query of the second example stops with the same error.

!example calls-invalid

## What the calls see

- A nested query sees the variables of the solutions of its parent; a named query sees only its parameters, bound to the arguments of the call.
- The arguments of a call are expressions: variables, IRIs and literals with embedded expressions (`<line/{?id}>`, `"zone {?zone}"`), function calls. They are evaluated in the calling query, and a relative IRI is resolved against the base of the calling query. The `BASE` of a named query applies to its own IRIs, and to the body of a `FUNCTION` query.
- A named `SELECT` query called as an iterator binds the variables of the `AS` clause to its projected variables, in order, one row per solution.
- An error raised by `RAISE` in a called query stops the whole execution, as in the main query.
