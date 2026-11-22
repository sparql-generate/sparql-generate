/*
 * Copyright 2026 MINES Saint-Étienne
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.util.NodeIsomorphismMap;

/**
 * A {@code GRAPH VarOrXIri { ... }} block of a {@code GENERATE} template: the
 * triples of its elements are generated in the named graph that the graph
 * node denotes, as the quad patterns of a SPARQL 1.1 Update template.
 *
 * <p>
 * Its elements are triple blocks ({@link ElementGenerateTriplesBlock}) and
 * sub-queries ({@link ElementSubExtQuery}), whose triples outside their own
 * {@code GRAPH} blocks are also generated in this graph. {@code GRAPH} blocks
 * cannot be nested.
 * </p>
 *
 * @author Maxime Lefrançois
 */
public class ElementGenerateGraph extends Element {

    private final Node graphNode;

    private final List<Element> elements;

    /**
     * @param graphNode a variable, an IRI, an IRI with embedded expressions,
     * or an embedded expression
     * @param elements the triple blocks and sub-queries of the block
     */
    public ElementGenerateGraph(Node graphNode, List<Element> elements) {
        this.graphNode = Objects.requireNonNull(graphNode, "graph node");
        this.elements = new ArrayList<>(Objects.requireNonNull(elements, "elements"));
    }

    public Node getGraphNode() {
        return graphNode;
    }

    /**
     * The elements of the block, which may be replaced in place.
     */
    public List<Element> getElements() {
        return elements;
    }

    @Override
    public boolean equalTo(Element other, NodeIsomorphismMap isoMap) {
        if (!(other instanceof ElementGenerateGraph)) {
            return false;
        }
        ElementGenerateGraph el = (ElementGenerateGraph) other;
        if (!graphNode.equals(el.graphNode) || elements.size() != el.elements.size()) {
            return false;
        }
        for (int i = 0; i < elements.size(); i++) {
            if (!elements.get(i).equalTo(el.elements.get(i), isoMap)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return 31 * graphNode.hashCode() + elements.hashCode();
    }

    @Override
    public void visit(org.apache.jena.sparql.syntax.ElementVisitor v) {
        if (v instanceof SPARQLExtElementVisitor) {
            ((SPARQLExtElementVisitor) v).visit(this);
        }
    }
}
