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
import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerate;
import com.github.thesmartenergy.sparql.generate.jena.iterator.IteratorFunctionBase1;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Iterator function
 * <a href="http://w3id.org/sparql-generate/iter/GeoJSONFeatures">iter:GeoJSONFeatures</a>
 * takes as input a <a href="https://tools.ietf.org/html/rfc7946">GeoJSON</a>
 * document, decodes it, and extracts a list of sub-JSON
 * <a href="https://tools.ietf.org/html/rfc7946#page-11">Features</a> contained
 * in the <a href="https://tools.ietf.org/html/rfc7946#page-12">FeatureCollection</a>
 * object.
 *
 * <ul>
 *   <li>Param 1: (json): a GeoJSON object with the type FeatureCollection.</li>
 * </ul>
 * 
 * <br/>
 * <strong>Example: </strong>
 *
 * <p>The iterator clause:</p>
 * <code>ITERATOR iter:GeoJSONFeatures(?source) AS ?earthquake</code>
 * <p>executed when <code>?source</code> is bound to the following GeoJSON 
 * document </p> 
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
 * <p>returns the following bindings:</p>
 * <pre>
 * ?earthquake => { "type":"Feature", "properties":{ "place":"5km SSW of Volcano, Hawaii", "time":1528974963260, "updated":1528975327080, "url":"https://earthquake.usgs.gov/earthquakes/eventpage/hv70265026", "detail":"https://earthquake.usgs.gov/earthquakes/feed/v1.0/detail/hv70265026.geojson", "type":"earthquake" }, "geometry":{ "type":"Polygon", "coordinates":[ [ [ 30, 10 ], [ 40, 40 ], [ 20, 40 ], [ 10, 20 ], [ 30, 10 ] ] ] }, "id":"hv70265026" }
 * ?earchquake => { "type":"Feature", "properties":{ "place":"1km SSE of Volcano, Hawaii", "time":1528975443520, "updated":1528975662950, "url":"https://earthquake.usgs.gov/earthquakes/eventpage/hv70265061", "detail":"https://earthquake.usgs.gov/earthquakes/feed/v1.0/detail/hv70265061.geojson", "type":"earthquake" }, "geometry":{ "type":"Point", "coordinates":[ -155.2333374, 19.4148331, -1.07 ] }, "id":"hv70265061" }
 * </pre>
 *
 * @author El Mehdi Khalfi <el-mehdi.khalfi at emse.fr>
 * @since   2018-09-04
 */
public class ITER_GeoJSONFeatures extends IteratorFunctionBase1 {

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ITER_GeoJSONFeatures.class);

    /**
     * The SPARQL function URI.
     */
    public static final String URI = SPARQLGenerate.ITER + "GeoJSONFeatures";

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
    public List<List<NodeValue>> exec(NodeValue json) {
        if (json.getDatatypeURI() != null
                && !json.getDatatypeURI().equals(datatypeUri)
                && !json.getDatatypeURI().equals("http://www.w3.org/2001/XMLSchema#string")) {
            LOG.debug("The URI of NodeValue1 MUST be"
                    + " <" + datatypeUri + "> or"
                    + " <http://www.w3.org/2001/XMLSchema#string>. Got "
                    + json.getDatatypeURI());
        }
        FeatureCollection featureCollection = gson.fromJson(json.asString(), FeatureCollection.class);
        List<NodeValue> nodeValues = new ArrayList<>();

        for (Feature feature : featureCollection.features()) {
            String featureJsonString = gson.toJson(feature);
            Node node = NodeFactory.createLiteral(featureJsonString, XSDDatatype.XSDstring);
            NodeValue nodeValue = new NodeValueNode(node);
            nodeValues.add(nodeValue);
        }
        return new ArrayList<>(Collections.singletonList(nodeValues));
    }
}
