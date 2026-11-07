# Python API

A Python implementation of SPARQL-Generate, with a Python API, is in preparation in the project `pysparql-generate`. It is not published yet; this page will document it when it is.

Until then, Python programs can run the [executable JAR](cli.md) as a subprocess:

```python
import subprocess

result = subprocess.run(
    ["java", "-jar", "sparql-generate-2.2.0.jar", "--dir", "my-project", "--base", "http://example.org/"],
    capture_output=True, text=True, check=True,
)
turtle = result.stdout   # the generated graph, in Turtle
# with rdflib: rdflib.Graph().parse(data=turtle, format="turtle")
```

The command line exits with status 1 when the query is invalid or fails, so `check=True` raises an exception in these cases; the log is in `result.stderr`.
