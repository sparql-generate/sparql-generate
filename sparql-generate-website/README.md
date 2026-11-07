# SPARQL-Generate website

The sources of the demonstration website of SPARQL-Generate,
<https://ci.mines-stetienne.fr/sparql-generate/>: the overview page, the
playground, and the grammar. The documentation is in `../docs/` (MkDocs,
published on ReadTheDocs); the former pages of this site redirect to it.

```sh
npm ci          # Node 20 or later
npm test        # unit tests of the playground state (links, validation, tutorial order)
npm run build   # writes public/
```

| Path | Content |
|:--|:--|
| `layout.html` | the page layout: header, navigation, footer |
| `pages/*.html` | the pages; a first-line comment gives their title and options |
| `src/playground.js` | the playground: CodeMirror 6 editors, examples, WebSocket client |
| `src/state.js` | the state of the playground and its links (`#ex=...`, `#msg=...`) |
| `src/vendor/codemirror-sparql-generate.mjs` | the CodeMirror 6 mode of [highlight-sparql-generate](https://github.com/sparql-generate/highlight-sparql-generate), copied |
| `resources/` | files copied as they are: CSS, images, BibTeX, the 2017 evaluation |
| `grammar-diagram/` | the EBNF of the grammar |
| `build.mjs` | the build: pages, resources, redirects, and the esbuild bundle |

The playground needs the API of `../sparql-generate-server` at the same origin,
under `/sparql-generate/`: `api/example/...` for the examples and the WebSocket
`transformStream` to run the queries. Each example of the server is tested by
`PlaygroundExamplesTest`. To run the playground locally, build the WAR of the
server, add the content of `public/` to it, and deploy it in a servlet
container that supports JSR-356 WebSockets (for example Jetty 9.4 with its
websocket module), under the context path `/sparql-generate`.

Links: `playground.html#ex=example/generate/00-Source` opens an example of the
server; `playground.html#msg=...` (lz-string, as in the former playground)
opens a query with its documents and graphs, as made by the *Copy link* button
and by the documentation.
