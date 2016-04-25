# SPARQL RDF Generation Language Web API

This website exposes a Web API at URL [http://w3id.org/sparql-generate/api/transform](http://w3id.org/sparql-generate/api/transform).

This API accepts the following query parameters, which all must be URL-encoded:

 - `queryuri`: the URL to the query file to execute;
 - `query`: the query to execute (takes precedence over `queryuri`);
 - `message`: the message from which RDF must be generated;
 - `var`: the variable to which the message will be initially bound;
 - `accept`: the media type to serialize the generated RDF. It can be either `text/turtle` or `application/rdf+xml`. The default value is `text/turtle`. If set, this parameter takes precedence over the HTTP Accept header field;
    
This API does not leverage the whole capabilities of `sparql-generate-jena`. Indeed, only one query and one message can be sent to the server. The engine will attempt to HTTP-dereference any other link to a SPARQL-Generate query (in a 'GENERATE' clause) or to a message (in a 'SOURCE' clause).
