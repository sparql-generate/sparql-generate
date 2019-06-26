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
package fr.emse.ci.sparqlext.graph;

import fr.emse.ci.sparqlext.normalizer.TemplateUtils;
import fr.emse.ci.sparqlext.query.SPARQLExtQuery;
import org.apache.jena.ext.com.google.common.base.Objects;
import org.apache.jena.graph.NodeVisitor;

/**
 * The class of expression nodes of type {@code ?{ TEMPLATE ... . }}, or
 * {@code ${ TEMPLATE ... .}} . They can be used anywhere variables are legal,
 * and they bind a (implicit) variable to a fun:select-call-template function.
 *
 * @author maxime.lefrancois
 */
public class Node_Template extends Node_Extended {

    /**
     * The TEMPLATE query.
     */
    private final SPARQLExtQuery query;

    /**
     * Constructor
     *
     * @param query The TEMPLATE query.
     */
    public Node_Template(SPARQLExtQuery query) {
        this.query = query;
    }

    /**
     * The TEMPLATE query.
     *
     * @return
     */
    public SPARQLExtQuery getQuery() {
        return query;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object visitWith(NodeVisitor v) {
        if (v instanceof SPARQLExtNodeVisitor) {
            ((SPARQLExtNodeVisitor) v).visit(this);
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
        if (!(o instanceof Node_Template)) {
            return false;
        }
        Node_Template on = (Node_Template) o;
        return Objects.equal(on.getQuery(), query);
    }

}
