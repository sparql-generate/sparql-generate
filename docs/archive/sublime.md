# Sublime Text package

!!! note "Archived page"
    The Sublime Text package is no longer maintained. The [VS Code extension](../vscode.md) replaces it.

The [`LinkedData` package](https://github.com/sparql-generate/sublime-editor) edits and runs SPARQL-Generate projects in Sublime Text. Its installation instructions are in its repository. As for the command line, all input and output files are assumed to be in UTF-8.

## Running

A SPARQL-Generate project is a folder that contains a query file `query.rqg`, and optionally a configuration file `sparql-generate-conf.json`, which may name another query file (see [the command line](../cli.md#configuration-file)).

- Tools › Build, or `Ctrl+B` (`Super+B` on macOS), runs the project.
- The bottom panel shows the log at level `INFO`. A more verbose log is written to `<queryname>.rqgout`; `Ctrl+Shift+L` (`Super+Shift+L`) opens it.
- `Ctrl+Shift+O` (`Super+Shift+O`) opens the configuration file.
