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
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.apache.jena.sparql.function.FunctionBase3;
import org.apache.log4j.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A SPARQL function that takes as an input a CBOR document, decodes it and return a sub-JSON document 
 * according to a JSONPath expression. The Iterator function URI is
 * {@code <http://w3id.org/sparql-generate/fn/CBOR>}.
 * It takes two parameters as input:
 * <ul>
 * <li> {@param  cbor} a RDF Literal with datatype URI
 * {@code <http://www.iana.org/assignments/media-types/application/cbor>}</li>
 * <li>{@param jsonpath} a RDF Literal with datatype {@code xsd:string}</li>
 * </ul>
 * and returns a RDF Literal with datatype URI
 * {@code <http://www.iana.org/assignments/media-types/application/json>}.
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
    private static final String datatypeUri = "http://www.iana.org/assignments/media-types/application/cbor";

    /**
    * A SPARQL function that takes as an input a CBOR document, decodes it and return a sub-JSON document 
    * according to a JSONPath expression. The Iterator function URI is
    * {@code <http://w3id.org/sparql-generate/fn/CBOR>}.
    * It takes two parameters as input:
    * <ul>
    * <li> {@param  cbor} a RDF Literal with datatype URI
    * {@code <http://www.iana.org/assignments/media-types/application/cbor>}</li>
    * <li>{@param jsonpath} a RDF Literal with datatype {@code xsd:string}</li>
    * </ul>
    * and returns a RDF Literal with datatype URI
    * {@code <http://www.iana.org/assignments/media-types/application/json>}.
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
