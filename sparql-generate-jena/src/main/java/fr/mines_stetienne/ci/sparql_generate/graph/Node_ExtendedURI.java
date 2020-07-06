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

import fr.mines_stetienne.ci.sparql_generate.expr.E_URIParam;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.graph.NodeVisitor;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;

/**
 * The class of expression nodes of type {@code <text{<expr>}> }, or any other
 * IRI with embedded expressions.
 *
 * @author Maxime Lefrançois
 */
public class Node_ExtendedURI extends Node_ExprList {

    /**
     * 
     * @param components list of NodeValueString or Expressions
     */
    private Node_ExtendedURI(List<Expr> components) {
        super(components);
    }
    
    /**
     * Builder of immutable Node_ExtendedURI
     */
    public static class Builder {
        
        private final List<Expr> components = new ArrayList<>();
        
        public void add(String s){
            if(!s.isEmpty()) {
                components.add(new NodeValueString(s));
            }
        }
                
        public void add(Expr e){
            components.add(new E_URIParam(e));
        }
        
        public Node_ExtendedURI build() {
            return new Node_ExtendedURI(components);
        }
        
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Object visitWith(NodeVisitor v) {
        if (v instanceof SPARQLExtNodeVisitor) {
            ((SPARQLExtNodeVisitor) v).visit(this);
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
        if (!(o instanceof Node_ExtendedURI)) {
            return false;
        }
        Node_ExtendedURI on = (Node_ExtendedURI) o;
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
