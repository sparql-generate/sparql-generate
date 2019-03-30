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

import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerate;
import com.google.gson.Gson;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueBoolean;
import org.apache.jena.sparql.expr.nodevalue.NodeValueDecimal;
import org.apache.jena.sparql.expr.nodevalue.NodeValueDouble;
import org.apache.jena.sparql.expr.nodevalue.NodeValueFloat;
import org.apache.jena.sparql.expr.nodevalue.NodeValueInteger;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.apache.jena.sparql.function.FunctionBase2;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Map;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Binding function
 * <a href="http://w3id.org/sparql-generate/fn/JSONPath">fun:JSONPath</a>
 * extracts a string from a JSON document, according to a JSONPath expression.
 *
 * <ul>
 * <li>Param 1 is a JSON document;</li>
 * <li>Param 2 is the JSONPath query. See https://github.com/json-path/JsonPath
 * for the syntax specification;</li>
 * <li>Result is a boolean, float, double, integer, string, as it best
 * fits.</li>
 * </ul>
 *
 * @author Noorani Bakerally <noorani.bakerally at emse.fr>
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public final class FUN_JSONPath extends FunctionBase2 {

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(FUN_JSONPath.class);

    /**
     * The SPARQL function URI.
     */
    public static final String URI = SPARQLGenerate.FUN + "JSONPath";

    /**
     * The datatype URI of the first parameter and the return literals.
     */
    private static final String datatypeUri = "http://www.iana.org/assignments/media-types/application/json";

    private static RDFDatatype dt = TypeMapper.getInstance().getSafeTypeByName(datatypeUri);

    private static Gson GSON = new Gson();

    @Override
    public NodeValue exec(NodeValue json, NodeValue jsonpath) {
        if (json.getDatatypeURI() != null
                && !json.getDatatypeURI().equals(datatypeUri)
                && !json.getDatatypeURI().equals("http://www.w3.org/2001/XMLSchema#string")) {
            LOG.debug("The datatype of the first argument should be <" + datatypeUri + ">"
                    + " or <http://www.w3.org/2001/XMLSchema#string>. Got "
                    + json.getDatatypeURI());
        }
        if (!jsonpath.isString()) {
            LOG.debug("The second argument should be a string. Got " + json);
        }

        try {
            Object value = JsonPath.parse(json.asNode().getLiteralLexicalForm())
                    .limit(1).read(jsonpath.getString());
            return nodeForObject(value);
        } catch (Exception ex) {
            LOG.debug("No evaluation of " + json + ", " + jsonpath, ex);
            throw new ExprEvalException("No evaluation of " + json + ", " + jsonpath, ex);
        }
    }

    public NodeValue nodeForObject(Object value) {
        if (value instanceof String) {
            return new NodeValueString((String) value);
        } else if (value instanceof Float) {
            return new NodeValueFloat((Float) value);
        } else if (value instanceof Boolean) {
            return new NodeValueBoolean((Boolean) value);
        } else if (value instanceof Integer) {
            return new NodeValueInteger((Integer) value);
        } else if (value instanceof Long) {
            return new NodeValueInteger((Long) value);
        } else if (value instanceof Double) {
            return new NodeValueDouble((Double) value);
        } else if (value instanceof BigDecimal) {
            return new NodeValueDecimal((BigDecimal) value);
        } else if (value instanceof ArrayList) {
            String jsonString = new Gson().toJson(value);
            return new NodeValueString(jsonString);
        } else if (value instanceof Map) {
            String jsonString = GSON.toJson(value, Map.class);
            return new NodeValueNode(NodeFactory.createLiteral(jsonString, dt));
        } else {
            String strValue = String.valueOf(value);

            JsonParser parser = new JsonParser();
            JsonElement valElement = parser.parse(strValue);
            JsonArray list = valElement.getAsJsonArray();

            if (list.size() == 1) {
                String jsonstring = list.get(0).getAsString();
                Node node = NodeFactory.createLiteral(jsonstring);
                NodeValue nodeValue = new NodeValueNode(node);
                return nodeValue;

            } else {
                return new NodeValueString(String.valueOf(value));
            }

        }
    }
}
