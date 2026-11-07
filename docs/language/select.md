# SELECT

A SPARQL-Generate `SELECT` query is a SPARQL 1.1 `SELECT` query that can also have binding clauses (`SOURCE`, `ITERATOR`, `BIND`) before its `WHERE` clause. It outputs a sequence of solutions.

!example select

## Named SELECT queries as iterators

A `SELECT` query can have an IRI as a name and a signature:

```sparql-generate
SELECT <name> ( ?param1, ?param2 ) ?var1 ?var2 ...
```

Another query uses it as an iterator: `ITERATOR <name>(arguments) AS ?x1 ?x2 ...` binds the parameters to the arguments, executes the query, and binds `?x1`, `?x2`, ... to the projected variables of each solution, in order. A query can thus reuse the extraction logic of another one.

!example named-select

The iterator `iter:call-select` does the same for a `SELECT` query given by its IRI as first argument.
