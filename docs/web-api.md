# Web API

The server of the playground exposes a Web API that executes a query given in the request: `https://ci.mines-stetienne.fr/sparql-generate/api/transform`. It accepts `GET` and `POST` (form-encoded) requests with the parameters:

- `query`, the query as a string, or `queryurl`, the URL of the query;
- `param`, zero or more times: the values of the parameters of the signature of the query, in order, as `xsd:string` literals.

The answer is Turtle for a `GENERATE` query, SPARQL JSON results for a `SELECT` query, and text for a `TEMPLATE` query.

```sh
curl --get https://ci.mines-stetienne.fr/sparql-generate/api/transform \
  --data-urlencode 'query=GENERATE { <http://example.org/s> <http://example.org/p> 42 } WHERE {}'
```

The API runs on the server of MINES Saint-Étienne with a time limit of 10 seconds per query, and without the documents and named queries of a playground session: the query can only use documents and queries available on the Web. For other uses, run the [command line](cli.md) or the [Java API](java-api.md).
