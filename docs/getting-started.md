# Getting started

The engine runs on Java 17 or later. It is distributed as an executable JAR, as Maven artifacts, and as a web playground.

## Try it in the playground

The [playground](https://ci.mines-stetienne.fr/sparql-generate/playground.html) runs queries on the server of MINES Saint-Étienne: write a query, add documents and graphs, and run it. Each example of this documentation has a *Run in the playground* button. The playground limits the execution time to 10 seconds; use the executable JAR or the Java API for larger transformations.

## The executable JAR

Download `sparql-generate-2.2.0.jar` from the [releases page](https://github.com/sparql-generate/sparql-generate/releases) and check that Java 17 or later is installed:

```sh
java -version
```

Create a directory with a query and a document:

```text
my-project/
├── query.rqg
└── people.csv
```

`people.csv`:

```text
id,name,born,country
1,Ada Lovelace,1815-12-10,GB
2,Alan Turing,1912-06-23,GB
3,Grace Hopper,1906-12-09,US
```

`query.rqg`:

```sparql-generate
PREFIX iter: <http://w3id.org/sparql-generate/iter/>
PREFIX schema: <http://schema.org/>
BASE <http://example.org/>

GENERATE {
  <person/{?id}> a schema:Person ;
    schema:name ?name .
}
ITERATOR iter:CSV(<people.csv>, "id", "name") AS ?id ?name
```

Run it:

```sh
java -jar sparql-generate-2.2.0.jar --dir my-project --base http://example.org/
```

The engine reads `query.rqg` in the directory and resolves `<people.csv>` against the base of the query: `http://example.org/people.csv`. The option `--base` declares that each file of the directory is identified by its path resolved against `http://example.org/`, so the engine reads the file `people.csv` instead of looking up the IRI on the Web. The generated graph is written in Turtle on the standard output; warnings and errors go to the standard error. A file `sparql-generate-conf.json` in the directory can map IRIs to files, declare named queries and graphs, and set the output; see [the command line](cli.md).

## Docker

There is no official Docker image. The JAR runs in any Java 17 image:

```sh
docker run --rm -v "$PWD/my-project":/work -v "$PWD/sparql-generate-2.2.0.jar":/sparql-generate.jar \
  eclipse-temurin:17-jre java -jar /sparql-generate.jar --dir /work --base http://example.org/
```

## Maven

The artifacts are on Maven Central, group `fr.mines-stetienne.ci.sparql-generate`. `sparql-generate-jena` is the engine with the core functions and iterators; each format has its own module, and `sparql-generate-all` depends on all of them:

```xml
<dependency>
    <groupId>fr.mines-stetienne.ci.sparql-generate</groupId>
    <artifactId>sparql-generate-all</artifactId>
    <version>2.2.0</version>
</dependency>
```

| Artifact | Adds |
|:--|:--|
| `sparql-generate-jena` | the engine, the command line, the core functions and iterators |
| `sparql-generate-csv` | CSV documents |
| `sparql-generate-json` | JSON and CBOR documents |
| `sparql-generate-xml` | XML and HTML documents |
| `sparql-generate-geojson` | GeoJSON documents |
| `sparql-generate-sql` | SQL databases |
| `sparql-generate-markdown` | Markdown documents |
| `sparql-generate-webapis` | HTTP requests and RDF serialisations |
| `sparql-generate-mqtt` | MQTT streams |
| `sparql-generate-ws` | WebSocket streams |
| `sparql-generate-binary` | binary literals |
| `sparql-generate-all` | all of the above |

The [Java API](java-api.md) page shows how to parse and execute a query.

## Next steps

- The [tutorial](tutorial.md) introduces the language with the air quality examples of the playground.
- The [language guide](language/index.md) describes each construct with an example.
- The [reference](reference/index.md) lists the functions and iterators.
