# TEMPLATE

A `TEMPLATE` query outputs text. Its syntax adapts STTL, the SPARQL Template Transformation Language of Corby and Faron-Zucker[^sttl], to the binding clauses of SPARQL-Generate. The template is a sequence of expressions, evaluated for each solution and concatenated:

```sparql-generate
TEMPLATE {
  before = expression ;      # optional, output once before the first solution
  expression expression ...  # output for each solution
  ; separator = expression   # optional, output between two solutions
  ; after = expression       # optional, output once after the last solution
}
```

The expressions are literals, variables, function calls and built-in calls; a bracketed expression `( ... )` is not allowed directly in a template, but can be written `STR( ... )`. Unbound values output nothing.

!example template

## GROUP, BOX and FORMAT

- `GROUP { expressions ; separator = "..." }` concatenates the expressions over the solutions of a group, like `GROUP_CONCAT`. With `GROUP BY`, the template is output once per group. `GROUP DISTINCT` removes duplicates. The default separator is a space.
- `BOX { expressions }` increases the indentation of the lines output inside it.
- `FORMAT { format expressions }` formats the expressions with a format string, as Java `String.format` does (`%s`). The format can also be the IRI of a document that contains it.

!example template-group-box-format

!!! warning "Known limitation in 2.2"
    An aggregate inside `BOX`, such as `BOX { str(avg(?x)) }`, outputs nothing in the engine 2.2; move the aggregate out of the `BOX`.

## Named templates and sub-templates

A named `TEMPLATE` query has an IRI and a signature. A template calls it with a sub-template `TEMPLATE <name>(arguments) .`, which can have its own binding clauses and `WHERE` clause after the arguments. The function `st:call-template(<name>, arguments)` calls it from an expression.

!example named-template

[^sttl]: Olivier Corby, Catherine Faron-Zucker. STTL: A SPARQL-based Transformation Language for RDF. WEBIST 2015. <https://ns.inria.fr/sparql-template/>
