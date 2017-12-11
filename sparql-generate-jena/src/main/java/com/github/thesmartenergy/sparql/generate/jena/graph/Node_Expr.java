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

import java.util.UUID;
import org.apache.jena.graph.NodeVisitor;
import org.apache.jena.sparql.expr.Expr;

/**
 * The class of expression nodes of type  {@code ?{ <expr> }}, or 
 * {@code ${ <expr> }}. They can be used anywhere variables are
 * legal, and they bind a (implicit) variable to the given SPARQL expression.
 * 
 * @author maxime.lefrancois
 */
public class Node_Expr extends Node_Extended {
    
    /**
     * The SPARQL expression specified in this node.
     */
    private final Expr expr;

    /**
     * Constructor 
     *
     * @param expr The SPARQL expression specified in this node.
     */
    public Node_Expr(Expr expr) {
        super(UUID.randomUUID().toString().substring(0,8));
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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if(!(o instanceof Node_Expr)) {
            return false;
        }
        Node_Expr on = (Node_Expr) o;
        return on.getExpr().equals(expr);
    }
    
}
