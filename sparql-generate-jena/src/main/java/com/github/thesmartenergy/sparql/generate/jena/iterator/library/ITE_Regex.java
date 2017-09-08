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

import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerate;
import java.util.ArrayList;
import com.github.thesmartenergy.sparql.generate.jena.iterator.IteratorFunctionBase3;
import java.util.List;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.log4j.Logger;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A SPARQL Iterator function that returns a list of strings
 * splitted based on delimter.  The Iterator function URI is
 * {@code <http://w3id.org/sparql-generate/ite/Split>}.
 * It takes two parameters as input:
 * <ul>
 * <li>{@param  stringValue} a RDF Literal with datatype {@code xsd:string} 
 * representing the source string</li>
 * <li>{@param  delimeterValue} a RDF Literal with datatype {@code xsd:string} representing the delimeter which can be regular expression</li>
 * </ul>
 * and returns a list of RDF Literal with datatype {@code xsd:string} 
 *
 * @author Noorani Bakerally
 */
public class ITE_Regex extends IteratorFunctionBase3 {

    /**
     * The logger.
     */
    private static final Logger LOG = Logger.getLogger(ITE_Regex.class);
    public static final String URI = SPARQLGenerate.ITER + "regex";
    
    public ITE_Regex(){
    }
    
    @Override
    public List<NodeValue> exec(NodeValue stringValue, NodeValue regex, NodeValue locationV) {
        
            String string = stringValue.getString();
            String regexString = regex.getString();
            
            int location = locationV.getInteger().intValue();
            Pattern pattern = Pattern.compile(regexString,Pattern.MULTILINE);
           
            Matcher matcher = pattern.matcher(string);
            
            
           List<NodeValue> nodeValues = new ArrayList<>();
           while (matcher.find()){
               NodeValue nodeValue = new NodeValueString(matcher.group(location));
               nodeValues.add(nodeValue);
           }
           return nodeValues;
    }

    
}