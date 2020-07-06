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
package fr.mines_stetienne.ci.sparql_generate.syntax;

import java.util.Objects;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementVisitor;
import org.apache.jena.sparql.util.NodeIsomorphismMap;

/**
 * A TExpression in SPARQL-Template queries.
 *
 * @author Maxime Lefrançois
 */
public class ElementExpr extends Element {

    private final Expr expr;

    public ElementExpr(Expr expr) {
        Objects.nonNull(expr);
        this.expr = expr;
    }

    public Expr getExpr() {
        return expr;
    }

    @Override
    public boolean equalTo(Element el2, NodeIsomorphismMap isoMap) {
        if (!(el2 instanceof ElementExpr)) {
            return false;
        }
        ElementExpr f2 = (ElementExpr) el2;
        if (!this.getExpr().equals(f2.getExpr())) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return 5 * expr.hashCode();
    }

    @Override
    public void visit(ElementVisitor v) {
        if (v instanceof SPARQLExtElementVisitor) {
            ((SPARQLExtElementVisitor) v).visit(this);
        }
    }
}
