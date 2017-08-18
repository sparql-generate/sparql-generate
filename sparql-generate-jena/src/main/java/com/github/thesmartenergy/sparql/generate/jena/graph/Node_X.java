/*
 * Copyright 2017 Ecole des Mines de Saint-Etienne.
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
package com.github.thesmartenergy.sparql.generate.jena.graph;

import com.github.thesmartenergy.sparql.generate.jena.serializer.SPARQLGenerateFmtUtils;
import org.apache.jena.atlas.io.IndentedLineBuffer;
import org.apache.jena.graph.Node_Fluid;
import org.apache.jena.sparql.serializer.SerializationContext;

/**
 *
 * @author maxime.lefrancois
 */
public abstract class Node_X extends Node_Fluid {
    
    public Node_X(String label) {
        super(label);
    }
    
    public String getLabel() {
        return (String) label;
    }
    
    @Override
    public String toString() {
        IndentedLineBuffer buff = new IndentedLineBuffer() ;
        buff.print("?" + label + " := ");
        SerializationContext context = new SerializationContext();
        SPARQLGenerateFmtUtils.printNode(buff, this, context);
        return buff.toString();
    }

}
