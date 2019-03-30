/*
 * Copyright 2016 Ecole des Mines de Saint-Etienne.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.thesmartenergy.sparql.generate.jena.iterator.library;

import com.github.filosganga.geogson.gson.GeometryAdapterFactory;
import com.github.filosganga.geogson.model.Feature;
import com.github.filosganga.geogson.model.FeatureCollection;
import com.github.filosganga.geogson.model.Geometry;
import com.github.filosganga.geogson.model.GeometryCollection;
import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerate;
import com.github.thesmartenergy.sparql.generate.jena.iterator.IteratorFunctionBase1;
import com.github.thesmartenergy.sparql.generate.jena.utils.WktLiteral;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Iterator function
 * <a href="http://w3id.org/sparql-generate/iter/GeoJSON">iter:GeoJSON</a>
 * takes as input a <a href="https://tools.ietf.org/html/rfc7946">GeoJSON</a> document, decodes it, and
 *
 * <ul>
 * <li>extracts a list of sub-JSON <a href="https://tools.ietf.org/html/rfc7946#page-11">Features</a> contained in the <a href="https://tools.ietf.org/html/rfc7946#page-12">FeatureCollection</a> object, then extracts the <a href="https://tools.ietf.org/html/rfc7946#page-7">Geometry</a> member from the <a href="https://tools.ietf.org/html/rfc7946#page-11">Feature object</a> as a wktLiteral.</li>
 * <li>extracts the corresponding properties for each <a href="https://tools.ietf.org/html/rfc7946#page-11">Feature object</a>.</li>
 * </ul>
 * <br>
 * <b>Parameters: </b>
 * <ul>
 * <li>Param 1: (json): a GeoJSON object with the type FeatureCollection.</li>
 * </ul>
 * <br>
 * <b>Example: </b>
 * <p>
 * Iterating over this GeoJSON document (as <tt>?source</tt>) with ITERATOR <tt>iter:GeoJSON(?source) AS ?geometricCoordinates ?properties</tt> <br>
 * <pre>
 * {
 *    "type":"FeatureCollection",
 *    "metadata":{
 *       "generated":1528963592000,
 *       "url":"https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/2.5_month.geojson",
 *       "title":"USGS Magnitude 2.5+ Earthquakes, Past Month"
 *    },
 *    "features":[
 *       {
 *          "type":"Feature",
 *          "properties":{
 *             "place":"1km SSE of Volcano, Hawaii",
 *             "time":1528975443520,
 *             "updated":1528975662950,
 *             "url":"https://earthquake.usgs.gov/earthquakes/eventpage/hv70265061",
 *             "detail":"https://earthquake.usgs.gov/earthquakes/feed/v1.0/detail/hv70265061.geojson",
 *             "type":"earthquake"
 *          },
 *          "geometry":{
 *             "type":"Point",
 *             "coordinates":[
 *                -155.2333374,
 *                19.4148331,
 *                -1.07
 *             ]
 *          },
 *          "id":"hv70265061"
 *       },
 *       {
 *          "type":"Feature",
 *          "properties":{
 *             "place":"5km SSW of Volcano, Hawaii",
 *             "time":1528974963260,
 *             "updated":1528975327080,
 *             "url":"https://earthquake.usgs.gov/earthquakes/eventpage/hv70265026",
 *             "detail":"https://earthquake.usgs.gov/earthquakes/feed/v1.0/detail/hv70265026.geojson",
 *             "type":"earthquake"
 *          },
 *          "geometry":{
 *             "type":"Polygon",
 *             "coordinates":[
 *                [
 *                   [
 *                      30,
 *                      10
 *                   ],
 *                   [
 *                      40,
 *                      40
 *                   ],
 *                   [
 *                      20,
 *                      40
 *                   ],
 *                   [
 *                      10,
 *                      20
 *                   ],
 *                   [
 *                      30,
 *                      10
 *                   ]
 *                ]
 *             ]
 *          },
 *          "id":"hv70265026"
 *       }
 *    ]
 * }
 * </pre>
 * returns (in each iteration):<br>
 * <pre>
 * ?geometricCoordinates => "POINT (-155.2333374 19.4148331 -1.07)"^^wkt:wktLiteral
 * ?properties => "properties":{ "place":"1km SSE of Volcano, Hawaii", "time":1528975443520, "updated":1528975662950, "url":"https://earthquake.usgs.gov/earthquakes/eventpage/hv70265061", "detail":"https://earthquake.usgs.gov/earthquakes/feed/v1.0/detail/hv70265061.geojson", "type":"earthquake" }
 *
 * ?geometricCoordinates => POLYGON (((30.0 10.0, 40.0 40.0, 20.0 40.0, 10.0 20.0, 30.0 10.0)))"^^wkt:wktLiteral
 * ?properties => "properties":{ "place":"5km SSW of Volcano, Hawaii", "time":1528974963260, "updated":1528975327080, "url":"https://earthquake.usgs.gov/earthquakes/eventpage/hv70265026", "detail":"https://earthquake.usgs.gov/earthquakes/feed/v1.0/detail/hv70265026.geojson", "type":"earthquake" }
 * </pre>
 *
 * @author El Mehdi Khalfi <el-mehdi.khalfi at emse.fr>
 * @since 2018-09-19
 */
public class ITER_GeoJSON extends IteratorFunctionBase1 {

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ITER_GeoJSON.class);

    /**
     * The SPARQL function URI.
     */
    public static final String URI = SPARQLGenerate.ITER + "GeoJSON";

    private static final String datatypeUri = "http://www.iana.org/assignments/media-types/application/geo+json";

    /**
     * Registering the GeometryAdapterFactory.
     * Gson TypeAdapterFactory is responsible fpr serializing/de-serializing all the {@link Geometry}, {@link Feature}
     * and {@link FeatureCollection} instances.
     */
    private static Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(new GeometryAdapterFactory())
            .create();

    @Override
    public Collection<List<NodeValue>> exec(NodeValue json) {
        if (json.getDatatypeURI() != null
                && !json.getDatatypeURI().equals(datatypeUri)
                && !json.getDatatypeURI().equals("http://www.w3.org/2001/XMLSchema#string")) {
            LOG.debug("The URI of NodeValue1 MUST be"
                    + " <" + datatypeUri + "> or"
                    + " <http://www.w3.org/2001/XMLSchema#string>. Got "
                    + json.getDatatypeURI());
        }

        // Generate a Collection of (two) Lists
        // first list is for geometries
        // second list is for properties
        Collection<List<NodeValue>> nodeValues = new HashSet<>();
        
        
        FeatureCollection featureCollection = gson.fromJson(json.asString(), FeatureCollection.class);

        for (Feature feature : featureCollection.features()) {
            Geometry g = feature.geometry();
            // features

            //GeoGSON supports the following geometry types:
            // Point, LineString, Polygon, MultiPoint, MultiLineString, MultiPolygon, and GeometryCollection.
            Node n;
            if (g instanceof GeometryCollection) {
                GeometryCollection geometryCollection = (GeometryCollection) g;
                List<String> geomJsons = new ArrayList<>();
                for (Geometry geom : geometryCollection.getGeometries()) {
                    String gString = gson.toJson(geom.positions()).
                            replace(",", " ").
                            replace("] [", ", ").
                            replace("[", "(").
                            replace("]", ")");
                    geomJsons.add(geom.getClass().getSimpleName().toUpperCase() + " " + gString);
                }
                n = NodeFactory.createLiteral(g.getClass().getSimpleName().toUpperCase() + " (" + String.join(", ", geomJsons) + ")", WktLiteral.wktLiteralType);
            } else {
                String gString = gson.toJson(g.positions()).
                        replace(",", " ").
                        replace("] [", ", ").
                        replace("[", "(").
                        replace("]", ")");
                n = NodeFactory.createLiteral(g.getClass().getSimpleName().toUpperCase() + " " + gString, WktLiteral.wktLiteralType);
            }
            NodeValue nodeValue = new NodeValueNode(n);
            
            List<NodeValue> values = new ArrayList<>();
            
            // Adding features iterator
            values.add(nodeValue);

            // properties
            Map<String, String> properties = feature.properties().entrySet().stream().collect(Collectors.toMap(
                    p -> p.getKey(),
                    p -> p.getValue().isJsonNull() ? "" : p.getValue().isJsonPrimitive() ? p.getValue().getAsString() : gson.toJson(p.getValue())
            ));
            // Adding properties iterator
            values.add(new NodeValueNode(
                    NodeFactory.createLiteral(
                            gson.toJson(properties),
                            TypeMapper.getInstance().getSafeTypeByName("http://www.iana.org/assignments/media-types/application/json")))
            );
        }
        return nodeValues;
    }
}
