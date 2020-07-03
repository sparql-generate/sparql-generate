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

import java.util.List;
import java.util.Objects;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementVisitor;
import org.apache.jena.sparql.util.NodeIsomorphismMap;

/**
 * A BOX element in SPARQL-Template queries.
 * 
 * @author Maxime Lefrançois
 */
public class ElementBox extends Element {

    final List<Element> exprs;

    public ElementBox(List<Element> exprs) {
        Objects.nonNull(exprs);
        this.exprs = exprs;
    }

    boolean isEmpty() {
        return exprs.isEmpty();
    }
    
    public int size() {
        return exprs.size();
    }
    
    public List<Element> getTExpressions() {
        return exprs;
    }

    @Override
    public boolean equalTo(Element el, NodeIsomorphismMap isoMap) {
        if (!(el instanceof ElementBox)) {
            return false;
        }
        ElementBox el2 = (ElementBox) el;
        if (size() != el2.size()) {
            return false;
        }
        List<Element> exprs2 = el2.getTExpressions();
        for (int i = 0; i < size(); i++) {
            if (!exprs.get(i).equals(exprs2.get(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return 473 * exprs.hashCode();
    }

    @Override
    public void visit(ElementVisitor v) {
        if (v instanceof SPARQLExtElementVisitor) {
            ((SPARQLExtElementVisitor) v).visit(this);
        }
    }
}
