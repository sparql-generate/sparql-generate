# Sources and iterators

The binding clauses come after the query form and the dataset clauses, and before the `WHERE` clause. They are evaluated in order, and each one can use the variables bound before it.

## SOURCE

```sparql-generate
SOURCE <iri> AS ?var
SOURCE <iri> ACCEPT <media-type-iri> AS ?var
```

`SOURCE` binds `?var` to a literal whose lexical form is the content of the document identified by the IRI, and whose datatype is the IRI of its media type (for example `https://www.iana.org/assignments/media-types/application/json`). The IRI can contain embedded expressions, such as `<https://example.org/station/{?id}>`, or be a variable bound before.

The engine looks the document up in the documentset of the [command line](../cli.md#configuration-file) or of the playground, then on the file system, then on the Web with an HTTP GET. If the document cannot be found, `?var` is unbound.

!example source

### Content negotiation with ACCEPT

`ACCEPT` gives the media type expected for the document, as an IRI. On the Web, the engine sends it in the HTTP `Accept` header; in a documentset, it selects the document declared with this media type. Here the IRI `http://example.org/people` has a JSON and a CSV representation, and the query asks for the CSV one.

!example source-accept

## ITERATOR

```sparql-generate
ITERATOR iter:function(arguments) AS ?var1 ?var2 ...
```

An iterator function returns a list of rows. `ITERATOR` binds the variables after `AS` to the values of each row: the clause multiplies each solution by the rows of the iterator. Most iterators take the document as their first argument, either a literal (the content) or an IRI (looked up as for `SOURCE`). The [reference](../reference/iterators.md) lists the iterators and their parameters.

### JSON

`iter:JSONPath(json, path, path1, path2, ...)` binds the first variable to each value matched by the JSONPath expression `path`, and the next variables to the values of `path1`, `path2`, ... evaluated on that value.

!example iterator-json

### CSV

`iter:CSV(csv, column1, column2, ...)` binds the variables to the cells of the named columns, row by row. Optional parameters set the dialect (header, quote, delimiter, end of line) and a batch size, so that large documents are read in batches.

!example iterator-csv

### XML

`iter:XPath(xml, xpath, xpath1, xpath2, ...)` binds the first variable to each node matched by `xpath`, and the next variables to the values of `xpath1`, `xpath2`, ... evaluated on that node.

!example iterator-xml

### Other iterators

| Source | Iterators |
|:--|:--|
| JSON | `iter:JSONPath`, `iter:JSONListKeys`, `iter:JSONSurfer` (streaming) |
| CSV | `iter:CSV`, `iter:CSVHeaders` |
| XML, HTML | `iter:XPath`, `iter:CSSPath` |
| GeoJSON | `iter:GeoJSON` |
| CBOR | `iter:CBOR` |
| SQL | `iter:SQL(<jdbc:...>, "SELECT ...")`: the JDBC URL carries the user and the password (`jdbc:postgresql://host/db?user=me&password=secret`); any JDBC driver of the classpath, MySQL included |
| text | `iter:regex`, `iter:Split` |
| numbers | `iter:for` |
| streams | `iter:HTTPGet`, `iter:MQTTSubscribe`, `iter:WebSocket` |
| queries | `iter:call-select`, named `SELECT` queries (see [SELECT](select.md#named-select-queries-as-iterators)) |

## BIND

`BIND( expression AS ?var )` in the binding clauses binds a variable before the next clauses, so that a later `SOURCE` or `ITERATOR` can use it. It has the same meaning as in a SPARQL graph pattern.

## Functions

Functions extract values from documents in expressions: `fun:JSONPath(?doc, "$.name")`, `fun:XPath(?doc, "/note/to")`, `fun:CSSPath(?html, "h1")`, `fun:dateTime(...)`, and others. A function returns one RDF term, where an iterator returns rows. The [reference](../reference/functions.md) lists them.
