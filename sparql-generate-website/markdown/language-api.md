@title SPARQL-Generate | Web API

# SPARQL-Generate Web API

This website exposes a Web API at URL [http://w3id.org/sparql-generate/api/transform](http://ci.mines-stetienne.fr/sparql-generate/api/transform).

This API accepts `GET` and `POST` operations with the following parameters:

- a `queryurl` parameter or a `query` parameter: the url of the query to execute, or the query as a string,
- zero or more `param` parameters: the parameters for the query arguments. They are interpreted as `xsd:string` literals
