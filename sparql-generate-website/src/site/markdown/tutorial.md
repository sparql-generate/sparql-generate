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

This RDF graph makes explicit the fact that the entity described is a location (a spatial thing), that the numerical values correspond to latitude and longitude (because we reuse a well known web vocabulary), and provides an identifier that can be reused across data sets.

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

In order to bind the result of this XPath expression to the variable `?name`, we make use of the SPARQL clause `BIND` inside a `WHERE` clause:

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

## Beautifying the code

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

Web developers are often dealing with URLs that have to follow a certain pattern, such as `http://api.myservice.com/things/{id}`. In SPARQL-Generate, it is possible to use a similar scheme in URIs as well as in literals. The transformation above can be rewritten into:

```
GENERATE {
   <http://example.com/locations/{?l_id}>
      a  geo:SpatialThing;
      rdfs:label  ?name;
      geo:lat  "{?lat}"^^xsd:decimal;
      geo:long "{?long}"^^xsd:decimal .
}
SOURCE <http://opendata.domain.com/locations.xml> AS ?source
WHERE {
   BIND(fun:XPath(?source,"/location/l_id/text()") AS ?l_id)
   BIND(fun:XPath(?source,"/location/name/text()") AS ?name)
   BIND(fun:XPath(?source,"/location/latitude/text()") AS ?lat)
   BIND(fun:XPath(?source,"/location/longitude/text()") AS ?long)
}
```

## Iterating through a set of elements

In our example so far, the XML file only had one `location` element, which made it easy to describe what parts of the file had to be bound to the variables. However, it may happen that we have something like this:

```
<locations>
   <location>
      <l_id>234</l_id>
      <name>La Péniche</name>
      <latitude>45.1837569</latitude>
      <longitude>5.7035581</longitude>
   </location>
   <location>
      <l_id>706</l_id>
      <name>École des Mines de Saint-Étienne</name>
      <latitude>45.4227587</latitude>
      <longitude>4.40666</longitude>
   </location>
</locations>
```

In this case, the XPath expressions return multiple values, which is a problem since we must assign the correct identifier to the correct name and geocoordinates. SPARQL-Generate has a special clause to split the document into subparts that are process iteratively: the `ITERATOR` clause that makes use of special iteration functions:

```
GENERATE {
   <http://example.com/locations/{?l_id}>
      a  geo:SpatialThing;
      rdfs:label  ?name;
      geo:lat  "{?lat}"^^xsd:decimal;
      geo:long "{?long}"^^xsd:decimal .
}
SOURCE <http://opendata.domain.com/locations.xml> AS ?source
ITERATOR iter:XPath(?source,"/locations/location") AS ?location
WHERE {
   BIND(fun:XPath(?location,"/location/l_id/text()") AS ?l_id)
   BIND(fun:XPath(?location,"/location/name/text()") AS ?name)
   BIND(fun:XPath(?location,"/location/latitude/text()") AS ?lat)
   BIND(fun:XPath(?location,"/location/longitude/text()") AS ?long)
}
```

In this case, the iterator function `iter:XPath` creates two blocks of XML data:

```
<location>
   <l_id>234</l_id>
   <name>La Péniche</name>
   <latitude>45.1837569</latitude>
   <longitude>5.7035581</longitude>
</location>
```

and:

```
<location>
   <l_id>706</l_id>
   <name>École des Mines de Saint-Étienne</name>
   <latitude>45.4227587</latitude>
   <longitude>4.40666</longitude>
</location>
```

on which the variable bindings can be set unambiguously. The `GENERATE` clause in this case is process as any times as there are results in the iteration, leading to an RDF graph as follows:

```
PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

<http://example.com/locations/234>
   a  geo:SpatialThing;
   rdfs:label  "La Péniche";
   geo:lat  "45.1837569"^^xsd:decimal;
   geo:long "5.7035581"^^xsd:decimal .
<http://example.com/locations/706>
   a  geo:SpatialThing;
   rdfs:label  "École des Mines de Saint-Étienne";
   geo:lat  "45.4227587"^^xsd:decimal;
   geo:long "4.40666"^^xsd:decimal .
```

## Towards more complex expressions

The expressions used in a `BIND` clause can combine arithmetics, boolean comparators, string operations, etc. as defined in [SPARQL 1.1](https://www.w3.org/TR/sparql11-query/#SparqlOps). Additionally, we provide a few extra functions that can 