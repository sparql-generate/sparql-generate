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
package fr.mines_stetienne.ci.sparql_generate.engine;

import java.util.Objects;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.Quad;

/**
 * The output of a sub-query of a {@code GRAPH} block: the triples of its
 * default graph go to the graph of the block, the quads of its own
 * {@code GRAPH} blocks go unchanged to the output of the parent query.
 *
 * <p>
 * Only the root query starts and finishes the output, and declares its
 * prefixes: these calls are not passed on.
 * </p>
 */
final class GraphRoutingStreamRDF implements StreamRDF {

    private final StreamRDF parent;

    private final Node graph;

    GraphRoutingStreamRDF(final StreamRDF parent, final Node graph) {
        this.parent = Objects.requireNonNull(parent);
        this.graph = Objects.requireNonNull(graph);
    }

    /**
     * The output of the parent query.
     */
    StreamRDF getParent() {
        return parent;
    }

    /**
     * The graph in which the default graph of the sub-query is generated.
     */
    Node getGraph() {
        return graph;
    }

    @Override
    public void start() {
    }

    @Override
    public void triple(final Triple triple) {
        parent.quad(Quad.create(graph, triple));
    }

    @Override
    public void quad(final Quad quad) {
        if (quad.isDefaultGraph()) {
            parent.quad(Quad.create(graph, quad.asTriple()));
        } else {
            parent.quad(quad);
        }
    }

    @Override
    public void base(final String base) {
    }

    @Override
    public void prefix(final String prefix, final String iri) {
    }

    @Override
    public void finish() {
    }
}
