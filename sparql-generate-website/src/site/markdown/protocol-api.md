# SPARQL-Generate Protocol API

This website exposes a Web API at URL [http://w3id.org/sparql-generate/api/protocol](http://w3id.org/sparql-generate/api/protocol).

This API accepts the following query parameters, which all must be URL-encoded:

 - `uri`: the URL to the resource to transform to RDF;
 - `accept`: the media type to serialize the generated RDF. It can be either `text/turtle` or `application/rdf+xml`. The default value is `text/turtle`. If set, this parameter takes precedence over the HTTP Accept header field;
