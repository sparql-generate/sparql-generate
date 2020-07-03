/*
 * Copyright 2020 MINES Saint-Étienne
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
package fr.emse.ci.sparqlext.geojson;

import com.github.filosganga.geogson.gson.GeometryAdapterFactory;
import com.github.filosganga.geogson.model.Feature;
import com.github.filosganga.geogson.model.FeatureCollection;
import com.github.filosganga.geogson.model.Geometry;
import com.github.filosganga.geogson.model.GeometryCollection;
import fr.emse.ci.sparqlext.SPARQLExt;
import fr.emse.ci.sparqlext.utils.WktLiteral;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.apache.jena.sparql.function.FunctionBase1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;
import org.apache.jena.sparql.expr.ExprEvalException;

/**
 * Binding function
 * <a href="http://w3id.org/sparql-generate/fn/GeoJSONGeometry">fun:GeoJSONGeometry</a>
 * extracts the
 * <a href="https://tools.ietf.org/html/rfc7946#page-7">Geometry</a>
 * member from a <a href="https://tools.ietf.org/html/rfc7946#page-11">Feature
 * object</a> as a wktLiteral.
 *
 * <p>
 * See
 * <a href="https://w3id.org/sparql-generate/playground.html#ex=example/generate/Example-GeoJSON">Live
 * example</a></p>
 *
 * <ul>
 * <li>Param 1 is a GeoJSON
 * <a href="https://tools.ietf.org/html/rfc7946#page-11">Feature object</a>
 * in JSON;</li>
 * <li>Result is a GeoSPARQL wktLiteral.</li>
 * </ul>
 *
 * <p>
 * The following geometry types are supported: Point, LineString, Polygon,
 * MultiPoint, MultiLineString, MultiPolygon, and GeometryCollection.</p>
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
    public static final String URI = SPARQLExt.FUN + "GeoJSONGeometry";

    /**
     * The datatype URI of the first parameter and the return literals.
     */
    private static final String datatypeUri = "https://www.iana.org/assignments/media-types/application/geo+json";

    /**
     * Registering the GeometryAdapterFactory. Gson TypeAdapterFactory is
     * responsible fpr serializing/de-serializing all the
     * {@link Geometry}, {@link Feature} and {@link FeatureCollection}
     * instances.
     */
    private static Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(new GeometryAdapterFactory())
            .create();

    @Override
    public NodeValue exec(NodeValue json) {
        if(json == null) {
        	String msg = "No JSON provided";
            LOG.debug(msg);
        	throw new ExprEvalException(msg);
        }
        if (!json.isLiteral()) {
            LOG.debug("The argument should be a literal. Got" + json);
            throw new ExprEvalException("The argument should be a literal. Got" + json);
        }
        String s = json.getNode().getLiteralLexicalForm();
        Feature feature = gson.fromJson(s, Feature.class);
        return getNodeValue(feature.geometry());
    }

    public NodeValue getNodeValue(Geometry geom) {
        Node n = NodeFactory.createLiteral(getGeometry(geom), WktLiteral.wktLiteralType);
        return new NodeValueNode(n);
    }

    private String getGeometry(Geometry geom) {
        return getGeometry(geom, true);
    }

    private String getGeometry(Geometry geom, boolean initial) {
        //GeoGSON supports the following geometry types:
        // Point, LineString, Polygon, MultiPoint, MultiLineString, MultiPolygon, and GeometryCollection.
        if (geom instanceof GeometryCollection) {
            // GeometryCollection specification: https://tools.ietf.org/html/rfc7946#page-9
            GeometryCollection gCollection = (GeometryCollection) geom;
            StringBuilder sb = new StringBuilder();
            if (initial) {
                sb.append(geom.getClass().getSimpleName().toUpperCase());
            }
            sb.append("(");
            sb.append(gCollection.getGeometries().stream().map(g -> getGeometry(g, false)).collect(Collectors.joining(", ")));
            sb.append(")");
            return sb.toString();
        } else {
            return geom.getClass().getSimpleName().toUpperCase() + gson.toJson(geom.positions()).
                    replace(",", " ").
                    replace("] [", ", ").
                    replace("[", "(").
                    replace("]", ")");
        }
    }
}
