# SPARQL-Generate Protocol API

This website exposes two Web APIs to demonstrate the SPARQL-Generate Protocol:

## This server acts as a SPARQL-Generate Client

The Web API at URL [http://w3id.org/sparql-generate/api/fetch](http://w3id.org/sparql-generate/api/fetch).

This server acts as a SPARQL-Generate Client and attempts to retrieve a message at the URI provided by the origin client.
It then attempts to generate RDF from it using the Link header parameter as described in the [protocol for clients](protocol.html), and to return the generated RDF to the origin client.

This API accepts only `GET` operations with the following query parameters, which all must be URL-encoded:

 - `uri`: the URL to the resource to transform to RDF;
 - `useaccept`: the media type that this server will use to access the message;
 - `accept`: the media type to serialize the generated RDF. It can be either `text/turtle` or `application/rdf+xml`. The default value is `text/turtle`. If set, this parameter takes precedence over the HTTP Accept header field;


## This server acts as a SPARQL-Generate Server

The Web API at URL [http://w3id.org/sparql-generate/api/fetch](http://w3id.org/sparql-generate/api/take).

This server acts as a SPARQL-Generate Server and attempts to generate RDF from the origin client request.
It uses the header parameters as described in the [protocol for servers](protocol.html), and returns the generated RDF to the origin client.

This API accepts only `GET` operations with the following query parameters, which all must be URL-encoded:

 - `accept`: the media type to serialize the generated RDF. It can be either `text/turtle` or `application/rdf+xml`. The default value is `text/turtle`. If set, this parameter takes precedence over the HTTP Accept header field;


