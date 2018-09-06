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
package com.github.thesmartenergy.sparql.generate.jena.function.library;

import com.github.filosganga.geogson.gson.GeometryAdapterFactory;
import com.github.filosganga.geogson.model.Feature;
import com.github.filosganga.geogson.model.FeatureCollection;
import com.github.filosganga.geogson.model.Geometry;
import com.github.filosganga.geogson.model.GeometryCollection;
import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerate;
import com.github.thesmartenergy.sparql.generate.jena.utils.WktLiteral;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.apache.jena.sparql.function.FunctionBase1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Binding function
 * <a href="http://w3id.org/sparql-generate/fn/GeoJSONGeometry">fun:GeoJSONGeometry</a>
 * extracts the <a href="https://tools.ietf.org/html/rfc7946#page-7">Geometry</a> member from a <a href="https://tools.ietf.org/html/rfc7946#page-11">Feature object</a> as a wktLiteral.
 *
 * <ul>
 * <li>Param 1 is a GeoJSON <a href="https://tools.ietf.org/html/rfc7946#page-11">Feature object</a> in JSON;</li>
 * <li>Result is a GeoSPARQL wktLiteral.</li>
 * </ul>
 *
 * <b>Example: </b>
 * If we pass this GeoJSON document:
 * <pre>
 * {@code
 * {
 *    "type":"Feature",
 *    "properties":{
 *       "place":"5km SSW of Volcano, Hawaii",
 *       "time":1528974963260,
 *       "updated":1528975327080,
 *       "url":"https://earthquake.usgs.gov/earthquakes/eventpage/hv70265026",
 *       "detail":"https://earthquake.usgs.gov/earthquakes/feed/v1.0/detail/hv70265026.geojson",
 *       "type":"earthquake"
 *    },
 *    "geometry":{
 *       "type":"Polygon",
 *       "coordinates":[
 *          [
 *             [
 *                30,
 *                10
 *             ],
 *             [
 *                40,
 *                40
 *             ],
 *             [
 *                20,
 *                40
 *             ],
 *             [
 *                10,
 *                20
 *             ],
 *             [
 *                30,
 *                10
 *             ]
 *          ]
 *       ]
 *    },
 *    "id":"hv70265026"
 * }
 * }
 * </pre>
 * to <a href="http://w3id.org/sparql-generate/fn/GeoJSONGeometry">fun:GeoJSONGeometry</a> as an input, the return value will be
 * <pre>
 * "POLYGON (((30.0 10.0, 40.0 40.0, 20.0 40.0, 10.0 20.0, 30.0 10.0)))"^^wkt:wktLiteral
 * </pre>
 *
 * @author El Mehdi Khalfi <el-mehdi.khalfi at emse.fr>
 * @since 2018-09-05
 */
public final class FUN_GeoJSONGeometry extends FunctionBase1 {

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(FUN_GeoJSONGeometry.class);

    /**
     * The SPARQL function URI.
     */
    public static final String URI = SPARQLGenerate.FUN + "GeoJSONGeometry";

    /**
     * The datatype URI of the first parameter and the return literals.
     */
    private static final String datatypeUri = "https://www.iana.org/assignments/media-types/application/geo+json";

    /**
     * Registering the GeometryAdapterFactory.
     * Gson TypeAdapterFactory is responsible fpr serializing/de-serializing all the {@link Geometry}, {@link Feature}
     * and {@link FeatureCollection} instances.
     */
    private static Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(new GeometryAdapterFactory())
            .create();

    @Override
    public NodeValue exec(NodeValue json) {
        if (json.getDatatypeURI() != null
                && !json.getDatatypeURI().equals(datatypeUri)
                && !json.getDatatypeURI().equals("http://www.w3.org/2001/XMLSchema#string")) {
            LOG.debug("The URI of NodeValue1 MUST be"
                    + " <" + datatypeUri + "> or"
                    + " <http://www.w3.org/2001/XMLSchema#string>. Got "
                    + json.getDatatypeURI());
        }

        Feature feature = gson.fromJson(json.getString(), Feature.class);

        Geometry g = feature.geometry();

        //GeoGSON supports the following geometry types:
        // Point, LineString, Polygon, MultiPoint, MultiLineString, MultiPolygon, and GeometryCollection.

        if (g instanceof GeometryCollection) {
            // GeometryCollection specification: https://tools.ietf.org/html/rfc7946#page-9
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
            Node n = NodeFactory.createLiteral(g.getClass().getSimpleName().toUpperCase() + " (" + String.join(", ", geomJsons) + ")", WktLiteral.wktLiteralType);
            return new NodeValueNode(n);
        }

        String gString = gson.toJson(g.positions()).
                replace(",", " ").
                replace("] [", ", ").
                replace("[", "(").
                replace("]", ")");
        Node n = NodeFactory.createLiteral(g.getClass().getSimpleName().toUpperCase() + " " + gString, WktLiteral.wktLiteralType);

        return new NodeValueNode(n);
    }
}
