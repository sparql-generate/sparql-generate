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
package fr.mines_stetienne.ci.sparql_generate.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.jena.graph.Node;

import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.Quad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to generate a Model from a stream of triples.
 *
 * @author Maxime Lefrançois
 */
class StreamRDFModel implements StreamRDF {

    private static final Logger LOG = LoggerFactory.getLogger(StreamRDFModel.class);

    private final Model model;

    private boolean warned = false;

    /**
     * The largest number of literals kept to be shared.
     */
    static final int MAX_SHARED_LITERALS = 1 << 16;

    /**
     * The literals already in the model, to be shared by the triples that
     * have an equal literal (the keys of a join, generated once per row), or
     * null.
     */
    private final Map<Node, Node> literals;

    public StreamRDFModel(final Model model) {
        this(model, false);
    }

    /**
     * @param model the model
     * @param shareLiterals true if equal literals of the triples are to be
     * one object in the model (a graph of FROM GENERATE, which lives for the
     * execution): the latest {@value #MAX_SHARED_LITERALS} literals at most.
     */
    StreamRDFModel(final Model model, final boolean shareLiterals) {
        Objects.requireNonNull(model);
        this.model = model;
        this.literals = shareLiterals ? new HashMap<>() : null;
    }

    private Node share(final Node node) {
        if (!node.isLiteral()) {
            return node;
        }
        final Node shared = literals.get(node);
        if (shared != null) {
            return shared;
        }
        if (literals.size() >= MAX_SHARED_LITERALS) {
            literals.clear();
        }
        literals.put(node, node);
        return node;
    }

    @Override
    public void start() {
    }

    @Override
    public synchronized void triple(Triple triple) {
        if (literals != null && (triple.getSubject().isLiteral() || triple.getObject().isLiteral())) {
            triple = Triple.create(share(triple.getSubject()), triple.getPredicate(), share(triple.getObject()));
        }
        model.add(model.asStatement(triple));
    }

    /**
     * A quad of the default graph is added to the model; a quad of a named
     * graph is ignored, with a warning for the first one.
     */
    @Override
    public synchronized void quad(Quad quad) {
        if (quad.isDefaultGraph()) {
            triple(quad.asTriple());
            return;
        }
        if (!warned) {
            warned = true;
            LOG.warn("The query generates quads in named graphs, such as graph " + quad.getGraph()
                    + ": they are ignored, only the default graph is kept. Execute it with execGenerateDataset to keep them.");
        }
    }

    @Override
    public void base(String base) {
    }

    @Override
    public void prefix(String prefix, String iri) {
        model.setNsPrefix(prefix, iri);
    }

    @Override
    public void finish() {
    }

}
