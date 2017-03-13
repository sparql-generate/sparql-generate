# SPARQL RDF Generation Language Web API

This website exposes a Web API at URL [http://w3id.org/sparql-generate/api/transform](http://w3id.org/sparql-generate/api/transform).

This API accepts only `POST` operations with the following form parameters, which all must be URL-encoded:

 - `query`: the query to execute;
 - `queryurl`: the url of the query to execute. Mutually exclusive with parameter `query`;
 - `documentset`: a JSON message of the form `[{"uri":"http://uri.of/the/document","document":"the\ndocument"},{...}]`;
    
This API does not leverage the whole capabilities of `sparql-generate-jena`. Indeed, only one query and a documentset can be sent to the server. The engine will attempt to HTTP-dereference any other link to a SPARQL-Generate query (in a 'GENERATE' clause) or to a message (in a 'SOURCE' clause).
