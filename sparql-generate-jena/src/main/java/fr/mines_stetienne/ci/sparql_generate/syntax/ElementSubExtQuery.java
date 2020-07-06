/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.mines_stetienne.ci.sparql_generate.syntax;


import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.util.NodeIsomorphismMap;
import fr.mines_stetienne.ci.sparql_generate.query.SPARQLExtQuery;

/**
 * A SPARQL-Generate sub-query.
 *
 * @author Maxime Lefrançois
 */
public class ElementSubExtQuery extends Element {

    private SPARQLExtQuery query;

    public ElementSubExtQuery(SPARQLExtQuery query) {
        this.query = query;
    }

    public SPARQLExtQuery getQuery() {
        return query;
    }

    @Override
    public boolean equalTo(Element other, NodeIsomorphismMap isoMap) {
        if (!(other instanceof ElementSubExtQuery)) {
            return false;
        }
        ElementSubExtQuery el = (ElementSubExtQuery) other;
        return query.equals(el.query);
    }

    @Override
    public int hashCode() {
        return query.hashCode();
    }

    @Override
    public void visit(org.apache.jena.sparql.syntax.ElementVisitor v) {
        if (v instanceof SPARQLExtElementVisitor) {
            ((SPARQLExtElementVisitor) v).visit(this);
        }
    }
}
