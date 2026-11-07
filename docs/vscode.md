# VS Code extension

The extension [vscode-sparql-generate](https://github.com/sparql-generate/vscode-sparql-generate) edits and runs SPARQL-Generate queries (`.rqg`) in Visual Studio Code. It replaces the [Sublime Text package](archive/sublime.md). Version 0.1.0 is a preview, distributed as a `.vsix` file: install it with *Extensions › … › Install from VSIX*.

## Features

- Highlighting of the language, from the same lexicon as this documentation and the playground ([highlight-sparql-generate](https://github.com/sparql-generate/highlight-sparql-generate)), expressions embedded in IRIs and strings included.
- **Run Query** (`Ctrl+Alt+Enter`, `Cmd+Alt+Enter` on macOS, the run button of the editor title, or the *Run Query* link above the query): runs the active query in its directory, as the [command line](cli.md) does, with its `sparql-generate-conf.json`. The result opens in a new editor (Turtle for `GENERATE`, text for `TEMPLATE`, a table for `SELECT`), and the log goes to the *SPARQL-Generate* output channel.
- Diagnostics: a syntax error is shown at its line and column. **Check Syntax** parses the query without running it, on demand or at each save.
- The engine is downloaded on first use, from the [releases](https://github.com/sparql-generate/sparql-generate/releases), and the extension finds a Java 17 runtime (`JAVA_HOME`, the path, the usual installation directories).
- Snippets for the query forms and clauses: `generate-json`, `generate-csv`, `template`, `iterator`, `source`, `function`, `select-named`, `box`, `format`...
- **Open the Project Configuration** opens or creates `sparql-generate-conf.json` next to the query.

## Settings

| Setting | Default | Meaning |
|:--|:--|:--|
| `sparqlGenerate.backend` | `java` | the engine that runs the queries |
| `sparqlGenerate.java.jar` | empty | the path of an engine JAR; empty means the downloaded one |
| `sparqlGenerate.java.engineVersion` | `latest` | the release to download: `latest` or `X.Y.Z` |
| `sparqlGenerate.java.path` | empty | a `java` executable or a JDK home; empty means found automatically |
| `sparqlGenerate.logLevel` | `WARN` | the log level of the engine (`-l`) |
| `sparqlGenerate.extraArguments` | `[]` | other options of the command line, such as `["-Bname=\"Alice\""]` |
| `sparqlGenerate.saveBeforeRun` | `true` | save the query before running it |
| `sparqlGenerate.checkSyntaxOnSave` | `false` | check the syntax at each save |
| `sparqlGenerate.codeLens` | `true` | show *Run Query* and *Check Syntax* above the query |

## Coming from Sublime Text

| Sublime Text package | VS Code extension |
|:--|:--|
| Tools › Build, `Ctrl+B` | Run Query, `Ctrl+Alt+Enter` |
| the `.rqglog` log, `Ctrl+Shift+L` | the *SPARQL-Generate* output channel (Show Log) |
| `Ctrl+Shift+O`, the configuration | Open the Project Configuration |
| the output in a `.out` file | a new editor, or the `output` file of the configuration |
| `sparql-generate.jar` copied into the package by hand | downloaded on first use |
