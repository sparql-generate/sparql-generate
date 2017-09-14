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
import com.github.thesmartenergy.sparql.generate.jena.iterator.IteratorFunctionBase2;
import java.util.List;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import java.util.Arrays;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A SPARQL Iterator function that returns a list of strings
 * splitted based on delimter.  The Iterator function URI is
 * {@code <http://w3id.org/sparql-generate/iter/Split>}.
 *
 * @author Noorani Bakerally <noorani.bakerally at emse.fr>
 */
public class ITE_Split extends IteratorFunctionBase2 {

    /**
     * The logger.
     */
    private static final Logger LOG = LogManager.getLogger(ITE_Split.class);
    public static final String URI = SPARQLGenerate.ITER + "Split";
    
    /**
     * 
     * @param stringValue  a RDF Literal with datatype {@code xsd:string}  representing the source string
     * @param delimeterValue  a RDF Literal with datatype {@code xsd:string} representing the delimeter which can be regular expression
     * @return  a list of RDF Literal with datatype {@code xsd:string} 
     */
    @Override
    public List<NodeValue> exec(NodeValue stringValue, NodeValue delimeterValue) {
        try {
            String string = stringValue.asNode().getLiteralLexicalForm();
            String delimeter = delimeterValue.asNode().getLiteralLexicalForm();
            List <String> splits = new ArrayList<String>(Arrays.asList(string.split(delimeter)));
     
            //will contain the final results
            List<NodeValue> nodeValues = new ArrayList<>(splits.size());
            
           for (String split:splits){
               NodeValue nodeValue = new NodeValueString(split);
               nodeValues.add(nodeValue);
           }
           return nodeValues;
        } catch(Exception e) {
            throw new ExprEvalException("Split: no evaluation", e);
        }
  
    }
}