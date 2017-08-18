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
 *
 * @author maxime.lefrancois
 */
public class Node_XExpr extends Node_X {
    
    private final Expr expr;

    public Node_XExpr(Expr expr) {
        super(UUID.randomUUID().toString().substring(0,8));
        this.expr = expr;
    }

    public Expr getExpr() {
        return expr;
    }
    
    @Override
    public Object visitWith(NodeVisitor v) {
        if (v instanceof SPARQLGenerateNodeVisitor) {
            ((SPARQLGenerateNodeVisitor) v).visit(this);
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if(!(o instanceof Node_XExpr)) {
            return false;
        }
        Node_XExpr on = (Node_XExpr) o;
        return on.getExpr().equals(expr);
    }
    
}
