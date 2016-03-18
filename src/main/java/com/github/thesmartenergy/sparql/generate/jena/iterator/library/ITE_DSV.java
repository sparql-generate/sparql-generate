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
import com.github.thesmartenergy.sparql.generate.jena.iterator.IteratorFunctionBase2;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.apache.log4j.Logger;

/**
 *
 * @author bakerally
 */
public class ITE_DSV extends IteratorFunctionBase2 {
     /**
     * The logger.
     */
    private static final Logger LOG = Logger.getLogger(ITE_DSV.class);

    /**
     * The SPARQL function URI.
     */
    public static final String URI = SPARQLGenerate.ITE + "DSV";

    /**
     * The datatype URI of the first parameter and the return literals.
     */
    private static final String datatypeUri = "urn:iana:mime:application/xml";
    
    @Override
    public List<NodeValue> exec(NodeValue v1, NodeValue v2) {
        
        List<NodeValue> nodeValues = new ArrayList<>(2);
        nodeValues.add(new NodeValueString("1"));
        nodeValues.add(new NodeValueString("2"));
        
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
