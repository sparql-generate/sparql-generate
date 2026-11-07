# FUNCTION

A `FUNCTION` query defines a function in SPARQL-Generate. It has an IRI, a signature, and a body that is one expression:

```sparql-generate
FUNCTION <iri> ( ?param1, ?param2 ) { expression }
```

A query calls it like any SPARQL extension function, `<iri>(arguments)`. When the engine meets a function IRI that is not in its library, it looks up a `FUNCTION` query at that IRI (in the queryset, on the file system, or on the Web), binds the parameters to the arguments, and evaluates the body. An error in the body, such as an unbound argument, is an evaluation error of the call: in a `BIND`, the variable stays unbound.

The body is a primary expression: a function call, a built-in call such as `IF(...)` or `CONCAT(...)`, a literal, a variable, or a bracketed expression `( ... )`.

!example function
