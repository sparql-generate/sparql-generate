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
package fr.mines_stetienne.ci.sparql_generate.geojson;

import com.github.filosganga.geogson.gson.GeometryAdapterFactory;
import com.github.filosganga.geogson.model.Feature;
import com.github.filosganga.geogson.model.FeatureCollection;
import com.github.filosganga.geogson.model.Geometry;
import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.iterator.IteratorFunctionBase1;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.mines_stetienne.ci.sparql_generate.stream.LookUpRequest;
import fr.mines_stetienne.ci.sparql_generate.stream.SPARQLExtStreamManager;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import java.util.List;
import java.util.Objects;
import org.apache.commons.io.IOUtils;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.riot.SysRIOT;
import org.apache.jena.sparql.expr.ExprEvalException;

/**
 * <p>
 * Iterator function
 * <a href="http://w3id.org/sparql-generate/iter/GeoJSON">iter:GeoJSON</a>
 * iterates over the features collection of a
 * <a href="https://tools.ietf.org/html/rfc7946">GeoJSON</a>
 * document, and outputs (1) the
 * <a href="https://tools.ietf.org/html/rfc7946#page-7">Geometry</a> as a
 * wktLiteral, and (2) the
 * <a href="https://tools.ietf.org/html/rfc7946#page-11">Features</a> as a JSON
 * Literal</p>
 *
 * <p>
 * See
 * <a href="https://w3id.org/sparql-generate/playground.html#ex=example/generate/Example-GeoJSON">Live
 * example</a></p>
 *
 * <ul>
 * <li>Param 1: (geojson): the URI of the GeoJSON document (a URI), or the
 * GeoJSON object itself (a String);</li>
 * </ul>
 *
 * The following variables may be bound:
 *
 * <ul>
 * <li>Output 1: (wktLiteral) geometry of the feature;</li>
 * <li>Output 2: (json literal) properties of the feature</li>
 * </ul>
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
    public static final String URI = SPARQLExt.ITER + "GeoJSON";

    private static final String GEOJSON_URI = "http://www.iana.org/assignments/media-types/application/geo+json";

    /**
     * Registering the GeometryAdapterFactory. Gson TypeAdapterFactory is
     * responsible fpr serializing/de-serializing all the
     * {@link Geometry}, {@link Feature} and {@link FeatureCollection}
     * instances.
     */
    private final static Gson GSON = new GsonBuilder()
            .registerTypeAdapterFactory(new GeometryAdapterFactory())
            .create();

    private final static FUN_GeoJSONGeometry geoJSONGeom = new FUN_GeoJSONGeometry();

    @Override
    public List<List<NodeValue>> exec(NodeValue json) {
        if(json == null) {
        	String msg = "No JSON provided";
            LOG.debug(msg);
        	throw new ExprEvalException(msg);
        }
        String s = getString(json);
        List<List<NodeValue>> nodeValues = new ArrayList<>();
        FeatureCollection featureCollection = GSON.fromJson(s, FeatureCollection.class);

        for (Feature feature : featureCollection.features()) {
            List<NodeValue> values = new ArrayList<>();
            NodeValue geometry = geoJSONGeom.getNodeValue(feature.geometry());
            values.add(geometry);
            Node properties = NodeFactory.createLiteral(
                    GSON.toJson(feature.properties()),
                    TypeMapper.getInstance().getSafeTypeByName("http://www.iana.org/assignments/media-types/application/json"));
            values.add(new NodeValueNode(properties));
            nodeValues.add(values);
        }
        return nodeValues;
    }

    private String getString(NodeValue geojson) throws ExprEvalException {
        if (geojson.isString()) {
            return geojson.getString();
        } else if (geojson.isLiteral()) {
            return geojson.asNode().getLiteralLexicalForm();
        } else if (!geojson.isIRI()) {
            String message = String.format("First argument must be a URI or a String");
            LOG.warn(message);
            throw new ExprEvalException(message);
        }
        String jsonPath = geojson.asNode().getURI();
        LookUpRequest req = new LookUpRequest(jsonPath, "application/geo+json");
        final SPARQLExtStreamManager sm = (SPARQLExtStreamManager) getContext().get(SysRIOT.sysStreamManager);
        Objects.requireNonNull(sm);
        TypedInputStream tin = sm.open(req);
        if (tin == null) {
            String message = String.format("Could not look up geoJSON document %s", jsonPath);
            LOG.warn(message);
            throw new ExprEvalException(message);
        }
        try {
            return IOUtils.toString(tin.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new ExprEvalException("IOException while looking up geoJSON document " + jsonPath, ex);
        }
    }

}
