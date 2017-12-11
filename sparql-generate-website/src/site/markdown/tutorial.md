# SPARQL-Generate Tutorial

---

## A beginner's example

Let us assume we have an XML file at URL `http://opendata.domain.com/locations.xml` containing the following:

```
<location>
   <l_id>234</l_id>
   <name>La Péniche</name>
   <latitude>45.1837569</latitude>
   <longitude>5.7035581</longitude>
</location>
```

This XML file is using an ad hoc schema that does not integrate well with other data. We would like to transform the data into RDF, resulting in the following RDF graph (in Turtle format):

```
PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

<http://example.com/locations/234>
   a  geo:SpatialThing;
   rdfs:label  "La Péniche";
   geo:lat  "45.1837569"^^xsd:decimal;
   geo:long "5.7035581"^^xsd:decimal .
```

This RDF graph makes explicit the fact that the entity described in a location (a spatial thing), that the numérical values correspond to latitude and longitude (because we reuse a well known web vocabulary), and provides an identifier that can be reused across data sets.

In general, if an XML file follows the same schema as the one used above, then the expected RDF graph will have the following form:

```
?location_uri
   a  geo:SpatialThing;
   rdfs:label  ?name;
   geo:lat  ?lat;
   geo:long ?long .
```

where the variables `?location_uri`, `?name`, `?lat`, and `?long` should be replaced by values constructed from selected content in the XML file. This is precisely how we specify the output in a SPARQL-Generate transformation, using the `GENERATE` clause:

```
GENERATE {
   ?location_uri
      a  geo:SpatialThing;
      rdfs:label  ?name;
      geo:lat  ?lat;
      geo:long ?long .
}
```

We now need to specify how the values of the variables will be constructed. First, we need to specify from which source file we will extract data. This is done with the `SOURCE` clause that appears after the `GENERATE` clause:

```
GENERATE {
   ?location_uri
      a  geo:SpatialThing;
      rdfs:label  ?name;
      geo:lat  ?lat;
      geo:long ?long .
}
SOURCE <http://opendata.domain.com/locations.xml> AS ?source
```

A source must be associated with a variable in order to refer to the content later. What follows is a clause where the values of the variables in the `GENERATE` clause will be defined. let us start with the value of variable `?name`. In general, it corresponds to the content of the element `name` inside the element `location`. This can be expressed as an XPath expression:

```
/location/name/text()
```

In order to bind the result of this XPath expression to the variable `?name`, we make use of the SPARQL clause `BIND` inside a `WHERE` claude:

```
GENERATE {
   ?location_uri
      a  geo:SpatialThing;
      rdfs:label  ?name;
      geo:lat  ?lat;
      geo:long ?long .
}
SOURCE <http://opendata.domain.com/locations.xml> AS ?source
WHERE {
   BIND(fun:XPath(?source,"/location/name/text()") AS ?name)
}
```

Note that we are using a function that takes as a first parameter a piece of XML and as a second parameter, an XPath expression. The other variables can be bound likewise but require more complicated expressions. `?location_uri` is a concatenation of a namespace with the content of the `l_id` element. However, a simple concatenation would yield a literal character string, instead of an IRI. An additional cast is required:

```
   BIND(IRI(CONCAT("http://example.com/locations/",fun:XPath(?source,"/location/l_id/text()"))) AS ?location_uri)
```

`?lat` and `?long` must be assigned a data type. This can be done with `STRDT`:

```
   BIND(STRDT(fun:XPath(?source,"/location/latitude/text()"),xsd:decimal) AS ?lat)
```

The first parameter of `STRDT` is a character string that corresponds to the encoding of the value for the datatype (the *lexical form* of the literal) and the second parameter is a URI (not a character string). The complete query is as follows:

```
GENERATE {
   ?location_uri
      a  geo:SpatialThing;
      rdfs:label  ?name;
      geo:lat  ?lat;
      geo:long ?long .
}
SOURCE <http://opendata.domain.com/locations.xml> AS ?source
WHERE {
   BIND(IRI(CONCAT("http://example.com/locations/",fun:XPath(?source,"/location/l_id/text()"))) AS ?location_uri)
   BIND(fun:XPath(?source,"/location/name/text()") AS ?name)
   BIND(STRDT(fun:XPath(?source,"/location/latitude/text()"),xsd:decimal) AS ?lat)
   BIND(STRDT(fun:XPath(?source,"/location/longitude/text()"),xsd:decimal) AS ?long)
}
```

The first binding is the result of nested functions that can be hard to read and understand. It is possible to make it more readable by adding intermediary variables, as follows:

```
...
WHERE {
   # The location id
   BIND(fun:XPath(?source,"/location/l_id/text()") AS ?l_id)
   # The complete URI
   BIND(IRI(CONCAT("http://example.com/locations/",?l_id) AS ?location_uri)
   ...
}
```