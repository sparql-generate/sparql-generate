# Tutorial

The tutorial of the playground introduces the language step by step, on the reports of air quality stations of the [Air Quality Open Data Platform](https://aqicn.org/api/), dumped in 2018. Each step opens in the playground with its query, its documents and an explanation; edit the query and run it again to see the effect of a change.

The examples run in the [playground](https://ci.mines-stetienne.fr/sparql-generate/playground.html) of the server of MINES Saint-Étienne, which runs the engine 2.2. The test suite of the engine executes every step and compares its output with the expected one.

## Generating RDF

| Step | Introduces |
|:--|:--|
| [00 Source](https://ci.mines-stetienne.fr/sparql-generate/playground.html#ex=example/generate/00-Source) | the `SOURCE` clause, which binds a variable to a document |
| [01 Functions](https://ci.mines-stetienne.fr/sparql-generate/playground.html#ex=example/generate/01-Functions) | functions that extract values from documents, here `fun:JSONPath` |
| [02 Sweetener](https://ci.mines-stetienne.fr/sparql-generate/playground.html#ex=example/generate/02-Sweetener) | expressions embedded in IRIs and literals |
| [03 Iterators](https://ci.mines-stetienne.fr/sparql-generate/playground.html#ex=example/generate/03-Iterators) | the `ITERATOR` clause, and sources whose IRI depends on an iterator |
| [04 Aggregators](https://ci.mines-stetienne.fr/sparql-generate/playground.html#ex=example/generate/04-Aggregators) | aggregates in the template |
| [05 Nested GENERATE](https://ci.mines-stetienne.fr/sparql-generate/playground.html#ex=example/generate/05-NestedGenerate) | `GENERATE` queries nested in a template |
| [06 Different sources](https://ci.mines-stetienne.fr/sparql-generate/playground.html#ex=example/generate/06-DifferentSources) | a nested query that reads other documents, in XML |
| [07 Calling external queries](https://ci.mines-stetienne.fr/sparql-generate/playground.html#ex=example/generate/07-CallingExternalQueries) | named queries with a signature, called from a template |
| [08 Lists](https://ci.mines-stetienne.fr/sparql-generate/playground.html#ex=example/generate/08-Lists) | RDF lists with `LIST` |
| [09 Using the dataset](https://ci.mines-stetienne.fr/sparql-generate/playground.html#ex=example/generate/09-UsingDataset) | the `WHERE` clause over an RDF graph, here a mapping from JSON keys to classes |
| [10 User functions](https://ci.mines-stetienne.fr/sparql-generate/playground.html#ex=example/generate/10-UserFunctions) | `FUNCTION` queries |
| [11 Named SELECT](https://ci.mines-stetienne.fr/sparql-generate/playground.html#ex=example/generate/11-NamedSelect) | named `SELECT` queries used as iterators |

## Selecting

| Step | Introduces |
|:--|:--|
| [00 Select](https://ci.mines-stetienne.fr/sparql-generate/playground.html#ex=example/select/00-Select) | a `SELECT` query with binding clauses |
| [01 Aggregators](https://ci.mines-stetienne.fr/sparql-generate/playground.html#ex=example/select/01-Aggregators) | aggregates over documents |
| [02 Iterators](https://ci.mines-stetienne.fr/sparql-generate/playground.html#ex=example/select/02-Iterators) | a CSV document read in batches |
| [03 Iterators and streams](https://ci.mines-stetienne.fr/sparql-generate/playground.html#ex=example/select/03-Iterators-Streams) | the same query, with the results streamed |

## Generating text

| Step | Introduces |
|:--|:--|
| [00 Template](https://ci.mines-stetienne.fr/sparql-generate/playground.html#ex=example/template/00-Template) | a `TEMPLATE` query and its separator |
| [01 Sweetener](https://ci.mines-stetienne.fr/sparql-generate/playground.html#ex=example/template/01-Sweetener) | expressions embedded in strings |
| [02 Format](https://ci.mines-stetienne.fr/sparql-generate/playground.html#ex=example/template/02-Format) | `FORMAT` |
| [03 Format with a template](https://ci.mines-stetienne.fr/sparql-generate/playground.html#ex=example/template/03-FormatWithTemplate) | a format string read from a document |
| [04 Group](https://ci.mines-stetienne.fr/sparql-generate/playground.html#ex=example/template/04-Group) | `GROUP` and `GROUP BY` |
| [05 Box](https://ci.mines-stetienne.fr/sparql-generate/playground.html#ex=example/template/05-Box) | `BOX` and indentation |
| [06 Calling external queries](https://ci.mines-stetienne.fr/sparql-generate/playground.html#ex=example/template/06-CallingExternalQueries) | named templates called with `st:call-template` |
| [07 Filtering calls](https://ci.mines-stetienne.fr/sparql-generate/playground.html#ex=example/template/07-FilteringCallsToExernalQueries) | sub-templates with their own `WHERE` clause |

## More examples

The playground also has one example per format and stream: CSV, JSON, XML, HTML (`CSSPath`), GeoJSON, regular expressions, dates, HTTP polling, MQTT and WebSocket. The examples whose source is on the network depend on the availability of these services: HTTP polling reads a report on the server of MINES Saint-Étienne, and WebSocket a public market data feed; the MQTT example does not run against the public broker `test.mosquitto.org`, which closes the connection.
