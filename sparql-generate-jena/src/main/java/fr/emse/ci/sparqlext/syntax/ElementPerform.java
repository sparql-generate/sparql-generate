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
package fr.emse.ci.sparqlext.syntax;

import java.util.Objects;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementVisitor;
import org.apache.jena.sparql.util.NodeIsomorphismMap;

/**
 * A PERFORM clause (future work)
 *
 * @author Maxime Lefrançois
 */
public class ElementPerform extends Element {

    final Node name;
    final ExprList params;

    public ElementPerform(Node name, ExprList params) {
        Objects.nonNull(name);
        this.name = name;
        this.params = params;    }

    public Node getName() {
        return name;
    }

    public ExprList getParams() {
        return params;
    }

    
    @Override
    public boolean equalTo(Element el, NodeIsomorphismMap isoMap) {
        if (!(el instanceof ElementPerform)) {
            return false;
        }
        ElementPerform el2 = (ElementPerform) el;
        if (!name.equals(el2.name)) {
            return false;
        }
        if (params == null && el2.params != null
                || params != null && el2.params == null) {
            return false;
        }
        if (params == null && el2.params == null) {
            return true;
        }
        if(params.size() != el2.params.size()) {
            return false;
        }
        for (int i = 0; i < params.size(); i++) {
            if (!params.get(i).equals(el2.params.get(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return 7 * name.hashCode() ^ params.hashCode();
    }

    @Override
    public void visit(ElementVisitor v) {
        if (v instanceof SPARQLExtElementVisitor) {
            ((SPARQLExtElementVisitor) v).visit(this);
        }
    }

}
