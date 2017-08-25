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

import java.util.List;
import java.util.UUID;
import org.apache.jena.graph.NodeVisitor;
import org.apache.jena.sparql.expr.Expr;

/**
 * The class of expression nodes of type {@code <text{<expr>}> }, or any other
 * IRI with embedded expressions.
 *
 * @author maxime.lefrancois
 */
public class Node_XURI extends Node_XExprList {

    /**
     * 
     * @param components list of NodeValueString or Expressions
     */
    private Node_XURI(List<Expr> components) {
        super(UUID.randomUUID().toString().substring(0,8), components);
    }
    
    /**
     * Builder of immutable Node_XURI
     */
    public static class Builder extends Node_XExprList.Builder {
        @Override
        public Node_XURI build() {
            return new Node_XURI(components);
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Object visitWith(NodeVisitor v) {
        if (v instanceof SPARQLGenerateNodeVisitor) {
            ((SPARQLGenerateNodeVisitor) v).visit(this);
        }
        return null;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Node_XURI)) {
            return false;
        }
        Node_XURI on = (Node_XURI) o;
        if (components.size() != on.components.size()) {
            return false;
        }
        for (int i = 0; i < components.size(); i++) {
            if (!components.get(i).equals(on.components.get(i))) {
                return false;
            }
        }
        return true;
    }

}
