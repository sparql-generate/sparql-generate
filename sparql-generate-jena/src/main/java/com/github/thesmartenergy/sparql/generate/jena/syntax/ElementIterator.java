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
package com.github.thesmartenergy.sparql.generate.jena.syntax;

import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementVisitor;
import org.apache.jena.sparql.util.NodeIsomorphismMap;

/**
 * A SPARQL Generate {@code ITERATOR} clause.
 * 
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class ElementIterator extends ElementIteratorOrSource {
    
    private Expr expr;

    public ElementIterator(Expr expr, Var var) {
        super(var);
        this.expr = expr;
    }

    public Expr getExpr() {
        return expr;
    }
    
    @Override
    public void visit(ElementVisitor v) {
        if (v instanceof SPARQLGenerateElementVisitor) {
            ((SPARQLGenerateElementVisitor) v).visit(this);
        }
    }

    @Override
    public int hashCode() {
        return getVar().hashCode() ^ getExpr().hashCode();
    }

    @Override
    public boolean equalTo(Element el2, NodeIsomorphismMap isoMap) {
        if (el2 == null) {
            return false;
        }
        if (!(el2 instanceof ElementIterator)) {
            return false;
        }
        ElementIterator iter2 = (ElementIterator) el2;
        if (!this.getVar().equals(iter2.getVar())) {
            return false;
        }
        if (!this.getExpr().equals(iter2.getExpr())) {
            return false;
        }
        return true;
    }
    
}
