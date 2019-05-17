# SPARQL-Generate and SPARQL-Template Java API

*This document is for versions 2.x.x onward. For previous versions, check out [the documentation for versions 1.x.x](http://w3id.org/sparql-generate/get-started-v1.html)*

`sparql-generate-jena` is an implementation of SPARQL-Generate and SPARQL-Template over Apache Jena. Its binaries, sources and documentation are available for download at [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Csparql-generate-jena%22). To use it in your Maven project, add the following dependency declaration to your Maven project file ( `*.pom` file):
 
```xml
<dependency>
    <groupId>fr.emse.ci</groupId>
    <artifactId>sparql-generate</artifactId>
    <version>${sparql-generate.version}</version>
</dependency>
```

Parse a query as follows:

```java
 String queryString = "........";
 SPARQLExtQuery query = (SPARQLExtQuery) QueryFactory.create(queryString, SPARQLExt.SYNTAX);
```

Then, use class `PlanFactory` to instantiate a `RootPlan` for that query.

```java
RootPlan plan = PlanFactory.create(query);
```

Then the `RootPlan` can be executed multiple times against different Datasets and Documentsets.

To instantiate a `Dataset`, one may use:

```java
Dataset dataset = DatasetFactory.create();
```

The engine fetches queries and documents on the Web if no mapping to local files is provided. Such mappings can be created using [Jena's `StreamManager` mechanism](http://jena.apache.org/documentation/io/rdf-input.html#streammanager-and-locationmapper). The following snippet illustrates how this is achieved:

```java
LocatorFileAccept locator = new LocatorFileAccept(new File("directory").toURI().getPath());
LocationMapperAccept mapper = new LocationMapperAccept();
mapper.addAltEntry("http://example.org/file1.json", "local/file/system/file1.json");
mapper.addAltEntry("http://example.org/file2.csv", "local/file/system/file2.csv");
SPARQLExtStreamManager sm = SPARQLExtStreamManager.makeStreamManager(locator, mapper);
```

The `StreamManager` is then used to instantiate the execution `Context`.

```java
// create the query execution context
Context context = SPARQLExt.createContext(sm);
```

Finally, call one of the `exec` methods to trigger the RDF generation, depending on the type of the query.

```java
Model     output = plan.execGenerate(dataset, context);
ResultSet output = plan.execSelect  (dataset, context);
String    output = plan.execTemplate(dataset, context);
```

**Remember:** The [javadoc](http://w3id.org/sparql-generate/apidocs/index.html) contains comprehensive documentations and examples, and the [sources](https://github.com/sparql-generate/sparql-generate) contain unit tests and other examples. 
