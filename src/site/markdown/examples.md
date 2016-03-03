# Get Started

To use SPARQL-Generate-Jena in your project, the most simple is to add the following maven dependency in your Java maven project file ( `*.pom` file):
 
```xml
<dependency>
    <groupId>org.w3id.sparql.template</groupId>
    <artifactId>sparql-generate-jena</artifactId>
    <version>{latest version}</version>
</dependency>
```

Then a little call to `SPARQL-Generate.init()`, and you can go:
 
```java
SPARQL-Generate.init();

// this line tells the engine that this SPARQL-Generate files at this URL may be found locally in the resource folder.
SPARQL-Generate.MAP_LOCAL.put("http://example.org/query", "/query.rqg"); 

String jsonDatatypeIri = "urn:iana:mime:application:json";
String json = "{ 
 name: "simple doc",
 values: [1 2 3]
}";

String request = "PREFIX dc: <http://purl.org/dc/terms/> \n" +
"PREFIX rqg-sel: <http://w3id.org/sparql-generate/sel/> \n" +
"PREFIX rqg-fn: <http://w3id.org/sparql-generate/fn/> \n" +
"PREFIX base <http://example.org/> \n" +
"GENERATE { \n" +
" <> dc:title ?name ; \n" +
"    &lt;values> LIST( ?decvalue ) . \n" +
" } \n" +
"}" +
"WHERE { \n" +
" BIND( STRLANG( rqg-fn:JSONPath_jayway_string( ?json, '$.name' ) , 'en' ) AS ?name ) \n" +
" BIND( xsd:decimal( rqg-fn:JSONPath_jayway_string( ?value, '$' ) ) AS ?decvalue ) \n" +
"} \n" +
"SELECTOR rqg-sel:JSONPath_jayway( ?json, '$.values[*]' ) AS ?value";

SPARQL-GenerateQuery q = SPARQL-GenerateQueryFactory.create(request);
q.serialize(System.out);

GenerationPlan plan = GenerationPlanFactory.create(q);

Model m = plan.exec(json, iri); // this will change in the future.

m.write(System.out, "TTL");
```

This code generates a Jena model that contains the RDF graph:
```
@prefix dc: <http://purl.org/dc/terms/> .
@base <http://example.org/> .

<> dc:title "simple doc"@en ;
   &lt;values> ( 1.0 2.0 3.0 ) .
```