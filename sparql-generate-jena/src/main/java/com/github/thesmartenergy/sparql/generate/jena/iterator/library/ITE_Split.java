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
package com.github.thesmartenergy.sparql.generate.jena.iterator.library;

import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerate;
import java.util.ArrayList;
import com.github.thesmartenergy.sparql.generate.jena.iterator.IteratorFunctionBase2;
import java.util.List;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.log4j.Logger;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import java.util.Arrays;

/**
 * A SPARQL Iterator function that returns a list of strings
 * splitted based on delimter.  The Iterator function URI is
 * {@code <http://w3id.org/sparql-generate/ite/Split>}.
 * It takes two parameters as input:
 * <ul>
 * <li>a RDF Literal with datatype {@code xsd:string} 
 * representing the source string</li>
 * <li>a RDF Literal with datatype {@code xsd:string} representing the delimeter which can be regular expression</li>
 * </ul>
 * and returns a list of RDF Literal with datatype {@code xsd:string} 
 *
 * @author Noorani Bakerally
 */
public class ITE_Split extends IteratorFunctionBase2 {

    /**
     * The logger.
     */
    private static final Logger LOG = Logger.getLogger(ITE_Split.class);
    public static final String URI = SPARQLGenerate.ITE + "Split";
    
    @Override
    public List<NodeValue> exec(NodeValue stringValue, NodeValue delimeterValue) {
            String string = stringValue.getString();
            String delimeter = delimeterValue.getString();
            List <String> splits = new ArrayList<String>(Arrays.asList(string.split(delimeter)));
     
            //will contain the final results
            List<NodeValue> nodeValues = new ArrayList<>(splits.size());
            
           for (String split:splits){
               NodeValue nodeValue = new NodeValueString(split);
               nodeValues.add(nodeValue);
           }
           return nodeValues;
  
    }
}