# Reference

| Page | Content |
|:--|:--|
| [Functions](functions.md) | the functions of the library, by module, generated from the Java sources |
| [Iterators](iterators.md) | the iterator functions of the library, by module, generated from the Java sources |
| [Grammar](grammar.md) | where the grammar of the language is defined |
| [Media type](media-type.md) | the media type `application/vnd.sparql-generate` |

The IRIs of the functions and iterators are in the namespaces `http://w3id.org/sparql-generate/fn/` (prefix `fun:`) and `http://w3id.org/sparql-generate/iter/` (prefix `iter:`). Each module of the library is a Maven artifact (see [Getting started](../getting-started.md#maven)); new functions and iterators are Java classes registered by a `FunctionLoader` or an `IteratorFunctionLoader` (see [Java API](../java-api.md#adding-functions-and-iterators)).

The [Javadoc](https://ci.mines-stetienne.fr/sparql-generate/apidocs/) documents the Java API.
