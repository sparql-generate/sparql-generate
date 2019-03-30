/*
 * Copyright 2019 École des Mines de Saint-Étienne.
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

import java.util.UUID;
import org.apache.jena.graph.NodeVisitor;
import org.apache.jena.graph.Node_Concrete;
import org.apache.jena.sparql.expr.Expr;

/**
 * The class of list nodes of type {@code LIST( <expr> )}. They can be used in
 * the place of objects in the GENERATE pattern.
 *
 * @author maxime.lefrancois
 */
public class Node_List extends Node_Concrete {

    /**
     * The SPARQL expression specified in this node.
     */
    private final Expr expr;

    /**
     * Constructor
     *
     * @param expr The SPARQL expression specified in this node.
     */
    public Node_List(Expr expr) {
        super(UUID.randomUUID().toString().substring(0, 8));
        this.expr = expr;
    }

    /**
     * The SPARQL expression specified in this node.
     *
     * @return
     */
    public Expr getExpr() {
        return expr;
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
    public Object visitWith(NodeVisitor v) {
        if (v instanceof SPARQLGenerateNodeVisitor) {
            ((SPARQLGenerateNodeVisitor) v).visit(this);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("NodeList ")
                .append(label)
                .append(" -> ")
                .append(expr.toString());
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Node_List)) {
            return false;
        }
        Node_List on = (Node_List) o;
        return on.getExpr().equals(expr);
    }

}
