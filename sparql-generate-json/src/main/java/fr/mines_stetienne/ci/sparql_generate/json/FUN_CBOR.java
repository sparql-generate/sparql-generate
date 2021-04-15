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
package fr.mines_stetienne.ci.sparql_generate.json;

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
import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.utils.LogUtils;
import java.util.Base64;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Binding function
 * <a href="http://w3id.org/sparql-generate/fn/CBOR">fun:CBOR</a> takes as input
 * a CBOR document, decodes it, and return a sub-JSON document according to a
 * JSONPath expression.
 *
 *
 * <ul>
 * <li>Param 1 (cbor) is a base64 encoding of the CBOR document in a RDF Literal
 * with datatype URI
 * {@code <https://www.iana.org/assignments/media-types/application/cbor>} or
 * {@code xsd:string}</li>
 * <li>Param 2 is the JSONPath query. See https://github.com/json-path/JsonPath
 * for the syntax specification;</li>
 * </ul>
 *
 * @author Noorani Bakerally <noorani.bakerally at emse.fr>
 */
public final class FUN_CBOR extends FunctionBase2 {
    //TODO write multiple unit tests for this class.

    private static final Logger LOG = LoggerFactory.getLogger(FUN_CBOR.class);

    public static final String URI = SPARQLExt.FUN + "CBOR";

    private static final String datatypeUri = "https://www.iana.org/assignments/media-types/application/cbor";

    @Override
    public NodeValue exec(NodeValue cbor, NodeValue jsonpath) {
        if(cbor == null) {
        	String msg = "No JSON provided";
            LOG.debug(msg);
        	throw new ExprEvalException(msg);
        }
        if(jsonpath == null) {
        	String msg = "No JSONPath provided";
            LOG.debug(msg);
        	throw new ExprEvalException(msg);
        }
        if (cbor.getDatatypeURI() != null
                && !cbor.getDatatypeURI().equals(datatypeUri)
                && !cbor.getDatatypeURI().equals("http://www.w3.org/2001/XMLSchema#string")) {
            LOG.debug("The URI of NodeValue1 MUST be <" + datatypeUri + ">"
                    + "or <http://www.w3.org/2001/XMLSchema#string>."
            );
        }

        String json = new String(Base64.getDecoder().decode(cbor.asNode().getLiteralLexicalForm().getBytes()));
        try {
            Object value = JsonPath.parse(json)
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

                if (list.size() == 1) {
                    String jsonstring = list.get(0).getAsString();
                    Node node = NodeFactory.createLiteral(jsonstring);
                    NodeValue nodeValue = new NodeValueNode(node);
                    return nodeValue;

                } else {
                    return new NodeValueString(String.valueOf(value));
                }
            }
        } catch (Exception ex) {
            if(LOG.isDebugEnabled()) {
                LOG.debug("No evaluation of " + jsonpath + "  on " + LogUtils.compress(cbor.asNode()), ex);
            }
            throw new ExprEvalException("FunctionBase: no evaluation", ex);
        }
    }
}
