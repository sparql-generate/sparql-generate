@title SPARQL-Generate | Java API for version 1.x

# SPARQL-Generate Java API for version 1.x

The reference implementation of SPARQL-Generate is based on Apache Jena. Its binaries, sources and documentation are available for download at [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Csparql-generate-jena%22). 

The [sources of `sparql-generate-jena`](https://github.com/sparql-generate/sparql-generate) contains project `sparql-generat-example` which can be used as a starting point to use the SPARQL-Generate API. 

The [javadoc](http://w3id.org/sparql-generate/apidocs/index.html) contains comprehensive documentations and examples, and the [sources](http://search.maven.org/#search%7Cga%7C1%7Csparql-generate) contains a set of unit tests to get more examples. 


### Maven dependency

To use it in your Maven project, add the following dependency declaration to your Maven project file ( `*.pom` file):

```xml
<dependency>
    <groupId>fr.emse.ci</groupId>
    <artifactId>sparql-generate</artifactId>
    <version>${sparql-generate.version}</version>
</dependency>
```

### Parsing a Query

```java
 SPARQLGenerateQuery query = (SPARQLGenerateQuery) QueryFactory.create(queryString, SPARQLGenerate.SYNTAX);
```

### Classes `PlanFactory` and `RootPlan`

First use class `PlanFactory` to instantiate a `RootPlan` for a SPARQL-Generate query. Then the `RootPlan` can be executed several times on different SPARQL datasets, with different initial bindings, and using different execution contexts (e.g., where URLs may be mapped to local files)

Call one of the `execGenerate`, `execSelect`, or `execTemplate` methods to trigger the execution. Here is the signature of some of these methods:

```java
RootPlan plan = PlanFactory.create(query);

// one of:

Model execGenerate(Dataset inputDataset, QuerySolution initialBindings, Context context);
String execTemplate(Dataset inputDataset, QuerySolution initialBindings, Context context);
ResultSet execSelect(Dataset inputDataset, QuerySolution initialBindings, Context context);
```


### The input Dataset

Part of the SPARQL-Generate query execution consists in evaluating a SPARQL 1.1 `SELECT *` query over a RDF Graph, or a SPARQL Dataset. Exactly like in SPARQL 1.1. The corresponding parameters are `inputModel` or `inputDataset`. To instantiate a `Model`, which is the Jena class for a RDF Graph, one may use for example:

```java
Model model = ModelFactory.createDefaultModel();
RDFDataMgr.read(model, inputStream, Lang.TURTLE);
```


### The context

The default behaviour of the implementation is to use the Jena StreamManager to **fetch the queries or sources from their URL**. One may map URIs to local files using [the Jena StreamManager and LocationMapper](http://jena.apache.org/documentation/io/rdf-input.html#streammanager-and-locationmapper).

The following snippet illustrates how this is achieved:


```java
// tell the engine to look into the `resources` directory 
LocatorFileAccept locator = new LocatorFileAccept(new File("resources").toURI().getPath());

// initialize the location mapper
LocationMapperAccept mapper = new LocationMapperAccept();

// tell the engine to read `document.csv` instead of looking up URI `https://example.com/document`
mapper.addAltEntry("https://example.com/document", "document.csv");

// tell the engine to read `query.rqg` instead of looking up URI `https://example.com/query`
mapper.addAltEntry("https://example.com/query", "query.rqg");

// initialize the StreamManager
SPARQLExtStreamManager sm = SPARQLExtStreamManager.makeStreamManager(locator, mapper);

// create the execution context from the StreamManager
Context context = SPARQLExt.createContext(sm);
```

The context contains other parameterizable execution configuration. For example by default the engine parallelizes the execution as much as possible using all processors to speed up the transformation. This behavior may be changed to one-processor synchronous execution using a different  Java 7 `ExecutorService`, to consume less memory. For example:

```java
// create an executor service with a single worker
ExecutorService executor = Executors.newSingleThreadExecutor();
Context context = SPARQLExt.createContext(sm, execuor);
```

### The initial binding

One may execute the query with initial bindings. This is useful when one wants to pass initial parameters to a query. 

The snippet below illustrates the creation of an initial binding with variable `?msg` bound to the string `"mymessage"`.

```java
// create the RDF literal
RDFNode jenaLiteral = initialModel.createLiteral("mymessage");

// create the binding
QuerySolutionMap initialBinding = new QuerySolutionMap();
initialBinding.add("msg", jenaLiteral);
```

### Using a SNAPSHOT version from the sources

Most recent [SNAPSHOT of SPARQL-Generate](https://github.com/sparql-generate/sparql-generate/releases/tag/2.0-SNAPSHOT) version can be downloaded, built (using Maven), and installed automatically using [JitPack](https://jitpack.io/docs/).

Add repository:

```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>
```

And dependency


```xml
<dependency>
    <groupId>fr.emse.ci</groupId>
    <artifactId>sparql-generate</artifactId>
    <version>2.0-SNAPSHOT</version>
</dependency>
```


### Is the documentation missing important information?

Don't hesitate to [create an issue on GitHub](https://github.com/sparql-generate/sparql-generate/issues/new) to ask for help or suggest improvement. 
