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
package fr.mines_stetienne.ci.sparql_generate.graph;

import fr.mines_stetienne.ci.sparql_generate.serializer.SPARQLExtFmtUtils;
import java.util.UUID;
import org.apache.jena.atlas.io.IndentedLineBuffer;
import org.apache.jena.graph.Node_Fluid;
import org.apache.jena.sparql.serializer.SerializationContext;

/**
 * The class of expression nodes: a node that can be used anywhere variables are
 * legal, and that bind a (implicit) variable to a SPARQL expression.
 *
 * @author Maxime Lefrançois
 */
public abstract class Node_Extended extends Node_Fluid {

    /**
     * Constructor
     *
     * @param label identifier of the node
     */
    Node_Extended() {
        super(UUID.randomUUID().toString().substring(0, 8));
    }

    /**
     * Identifier for the node, should be a unique random string. Should be
     * legal for SPARQL variable naming.
     *
     * @return
     */
    public String getLabel() {
        return (String) label;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        IndentedLineBuffer buff = new IndentedLineBuffer();
        buff.print("?" + label + " := ");
        SerializationContext context = new SerializationContext();
        SPARQLExtFmtUtils.printNode(buff, this, context);
        return buff.toString();
    }

}
