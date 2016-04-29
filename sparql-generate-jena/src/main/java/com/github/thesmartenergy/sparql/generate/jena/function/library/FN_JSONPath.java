/*
 * Copyright 2016 ITEA 12004 SEAS Project.
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
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import net.minidev.json.JSONArray;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueBoolean;
import org.apache.jena.sparql.expr.nodevalue.NodeValueDecimal;
import org.apache.jena.sparql.expr.nodevalue.NodeValueDouble;
import org.apache.jena.sparql.expr.nodevalue.NodeValueFloat;
import org.apache.jena.sparql.expr.nodevalue.NodeValueInteger;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.apache.jena.sparql.function.FunctionBase2;
import org.apache.log4j.Logger;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;

/**
 * A SPARQL Function that extracts a string from a JSON document, according to a
 * JSONPath expression. The Function URI is
 * {@code <http://w3id.org/sparql-generate/fn/JSON_Path_jayway>}.
 * It takes two parameters as input:
 * <ul>
 * <li>a RDF Literal with datatype URI
 * {@code <urn:iana:mime:application/json>}</li>
 * <li>a RDF Literal with datatype {@code xsd:string}</li>
 * </ul>
 * and returns a RDF Literal with datatype being the type of the object extracted from the JSON document
 * @author Noorani Bakerally
 */
public final class FN_JSONPath extends FunctionBase2 {
    //TODO write multiple unit tests for this class.

    /**
     * The logger.
     */
    private static final Logger LOG = Logger.getLogger(FN_JSONPath.class);

    /**
     * The SPARQL function URI.
     */
    public static final String URI = SPARQLGenerate.FN + "JSONPath";

    /**
     * The datatype URI of the first parameter and the return literals.
     */
    private static final String datatypeUri = "urn:iana:mime:application/json";

    /**
     * {@inheritDoc }
     */
    @Override
    public NodeValue exec(NodeValue json, NodeValue jsonpath) {
        if (json.getDatatypeURI() == null
                && datatypeUri == null
                || json.getDatatypeURI() != null
                && !json.getDatatypeURI().equals(datatypeUri)
                && !json.getDatatypeURI().equals("http://www.w3.org/2001/XMLSchema#string")) {
            LOG.warn("The URI of NodeValue1 MUST be <" + datatypeUri + ">"
                    + "or <http://www.w3.org/2001/XMLSchema#string>."
                    + " Returning null.");
        }

        try {
            Object value = JsonPath.parse(json.asNode().getLiteralLexicalForm())
                    .limit(1).read(jsonpath.getString());
            
            if (value instanceof String) {
                return new NodeValueString((String) value);
            } else if (value instanceof Float) {
                return new NodeValueFloat((Float) value);
            } else if (value instanceof Boolean) {
                return new NodeValueBoolean((Boolean) value);
            } else if (value instanceof Integer) {
                return new NodeValueInteger((Integer) value);
            } else if (value instanceof Double) {
                return new NodeValueDouble((Double) value);
            } else if (value instanceof BigDecimal) {
                return new NodeValueDecimal((BigDecimal) value);
            } else {
                String strValue = String.valueOf(value);
                
                
                JsonParser parser = new JsonParser();
                JsonElement valElement = parser.parse(strValue);
                JsonArray list = valElement.getAsJsonArray();
                
                if (list.size() == 1){
                    String jsonstring = list.get(0).getAsString();
                    Node node = NodeFactory.createLiteral(jsonstring);
                    NodeValue nodeValue = new NodeValueNode(node);
                    return nodeValue;
                    
                } else {
                  return new NodeValueString(String.valueOf(value)); 
                }
            }
        } catch (Exception e) {
            throw new ExprEvalException("FunctionBase: no evaluation", e);
        }
    }
}
