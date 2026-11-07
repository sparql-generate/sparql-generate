# Java API

The engine is a Java library on Apache Jena 5. Add `sparql-generate-all`, or `sparql-generate-jena` and the format modules you need, to your project (see [Getting started](getting-started.md#maven)). The Javadoc is at <https://ci.mines-stetienne.fr/sparql-generate/apidocs/>, and the module `sparql-generate-example` of the repository is a complete program.

## Parsing a query and making a plan

```java
import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.engine.PlanFactory;
import fr.mines_stetienne.ci.sparql_generate.engine.RootPlan;
import fr.mines_stetienne.ci.sparql_generate.query.SPARQLExtQuery;
import org.apache.jena.query.QueryFactory;

SPARQLExt.init();
SPARQLExtQuery query = (SPARQLExtQuery) QueryFactory.create(queryString, SPARQLExt.SYNTAX);
RootPlan plan = PlanFactory.create(query);
```

`PlanFactory.create(String)` parses and plans in one call. A `RootPlan` can be executed several times, with different contexts and initial bindings.

## Executing a plan

The execution context gives the input dataset, the stream manager that resolves the IRIs of documents and named queries, and the output when the result is streamed. `ContextUtils.build()` creates it:

```java
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;
import org.apache.jena.sparql.util.Context;

Context context = ContextUtils.build()
        .setInputDataset(dataset)       // or setInputModel(model); optional
        .setStreamManager(streamManager) // optional, see below
        .setPrefixMapping(query)        // prefixes of the output
        .build();
```

Then call the method that matches the query form:

```java
Model graph = plan.execGenerate(context);    // GENERATE
ResultSet results = plan.execSelect(context); // SELECT
String text = plan.execTemplate(context);    // TEMPLATE
```

Each method also takes a `List<Binding>` as first argument: the initial bindings of the parameters of the query, one execution per binding. For example, to bind `?country` to `"GB"`:

```java
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;

Binding binding = Binding.builder()
        .add(Var.alloc("country"), NodeFactory.createLiteralString("GB"))
        .build();
Model graph = plan.execGenerate(List.of(binding), context);
```

## Streaming

The `...Stream` methods write the output to the context as it is produced, and return nothing. Set the output in the context:

```java
// GENERATE: any Jena StreamRDF, here N-Triples on the standard output
Context context = ContextUtils.build()
        .setGenerateOutput(StreamRDFWriter.getWriterStream(System.out, Lang.NTRIPLES))
        .build();
plan.execGenerateStream(context);

// SELECT: a consumer of result sets, one per batch of solutions
plan.execSelectStream(ContextUtils.build().setSelectOutput(rs -> ResultSetFormatter.out(rs)).build());

// TEMPLATE: an IndentedWriter
plan.execTemplateStream(ContextUtils.build().setTemplateOutput(IndentedWriter.stdout).build());
```

## Resolving documents and named queries

By default the engine looks documents and named queries up on the file system, on the class path, and on the Web. A `SPARQLExtStreamManager` maps IRIs to other locations.

```java
import fr.mines_stetienne.ci.sparql_generate.stream.*;

// look relative paths up in the directory "resources"
LocatorFileAccept locator = new LocatorFileAccept(new File("resources").toURI().getPath());

// read documentset/people.csv instead of looking up http://example.org/people.csv
LocationMapperAccept mapper = new LocationMapperAccept();
mapper.addAltEntry("http://example.org/people.csv", "documentset/people.csv");

SPARQLExtStreamManager streamManager = SPARQLExtStreamManager.makeStreamManager(locator, mapper);
```

`LocatorStringMap` serves documents held in memory, with their media type; the playground uses it:

```java
LocatorStringMap strings = new LocatorStringMap();
strings.put("http://example.org/people.csv", csvContent, "text/csv");
strings.put("http://example.org/query/person", namedQuery, SPARQLExt.MEDIA_TYPE);
SPARQLExtStreamManager streamManager = SPARQLExtStreamManager.makeStreamManager(strings);
```

## Adding functions and iterators

A function is a Jena `Function`, registered with its IRI in the Jena `FunctionRegistry`. An iterator extends `IteratorFunctionBase` (all rows at once) or `IteratorStreamFunctionBase` (rows in batches), and is registered in the `IteratorFunctionRegistry`:

```java
public class ITER_Repeat extends IteratorFunctionBase {
    public static final String URI = "http://example.org/iter/repeat";

    @Override
    public List<List<NodeValue>> exec(List<NodeValue> args) {
        int n = args.get(1).getInteger().intValue();
        List<List<NodeValue>> rows = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            rows.add(List.of(args.get(0)));
        }
        return rows;
    }

    @Override
    public void checkBuild(ExprList args) {
        if (args.size() != 2) {
            throw new QueryBuildException("iter:repeat takes two arguments");
        }
    }
}

IteratorFunctionRegistry.get().put(ITER_Repeat.URI, ITER_Repeat.class);
```

The modules of the library register theirs at start-up through the Java `ServiceLoader`: a class that implements `FunctionLoader` and `IteratorFunctionLoader`, named in `META-INF/services/fr.mines_stetienne.ci.sparql_generate.function.FunctionLoader` and `META-INF/services/fr.mines_stetienne.ci.sparql_generate.iterator.IteratorFunctionLoader`.

## Migrating from 2.1

Version 2.2 requires Java 17 and Apache Jena 5.6. Applications that use Jena directly must move to Jena 5 (see the [Jena release notes](https://github.com/apache/jena/releases)). The API of SPARQL-Generate itself is unchanged; see the [changelog](changelog.md).
