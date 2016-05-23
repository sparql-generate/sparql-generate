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
import org.apache.jena.sparql.function.FunctionBase3;
import org.apache.log4j.Logger;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;

/**
 * A SPARQL function that takes as an input a CBOR document, decodes it and return a sub-JSON document 
 * according to a JSONPath expression. The Iterator function URI is
 * {@code <http://w3id.org/sparql-generate/fn/CBOR>}.
 * It takes two parameters as input:
 * <ul>
 * <li> {@param  cbor} a RDF Literal with datatype URI
 * {@code <urn:iana:mime:application/cbor>}</li>
 * <li>{@param jsonpath} a RDF Literal with datatype {@code xsd:string}</li>
 * </ul>
 * and returns a RDF Literal with datatype URI
 * {@code <urn:iana:mime:application/json>}.
 *
 * @author Noorani Bakerally
 */
public final class FN_Regex extends FunctionBase3 {
    //TODO write multiple unit tests for this class.

    /**
     * The logger.
     */
    private static final Logger LOG = Logger.getLogger(FN_Regex.class);

    /**
     * The SPARQL function URI.
     */
    public static final String URI = SPARQLGenerate.FN + "regex";

    /**
     * The datatype URI of the first parameter and the return literals.
     */
    private static final String datatypeUri = "urn:iana:mime:application/cbor";

    /**
    * A SPARQL function that takes as an input a CBOR document, decodes it and return a sub-JSON document 
    * according to a JSONPath expression. The Iterator function URI is
    * {@code <http://w3id.org/sparql-generate/fn/CBOR>}.
    * It takes two parameters as input:
    * <ul>
    * <li> {@param  cbor} a RDF Literal with datatype URI
    * {@code <urn:iana:mime:application/cbor>}</li>
    * <li>{@param jsonpath} a RDF Literal with datatype {@code xsd:string}</li>
    * </ul>
    * and returns a RDF Literal with datatype URI
    * {@code <urn:iana:mime:application/json>}.
    *
    * @author Noorani Bakerally
    */
    @Override
    public NodeValue exec(NodeValue stringValue, NodeValue regex, NodeValue locationV) {
        
        String string = stringValue.getString();
        String regexString = regex.getString();

        int location = locationV.getInteger().intValue();
        
        Pattern pattern = Pattern.compile(regexString);
        
        Matcher matcher = pattern.matcher(string);
        
       NodeValue nodeValue = null;
       //nodeValue = new NodeValueString(String.valueOf(matcher.find()));
       
       if (matcher.find()){
           nodeValue = new NodeValueString(matcher.group(location));
       }
       return nodeValue;
    }
}
