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

import java.util.Objects;

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

    public StreamRDFModel(final Model model) {
        Objects.requireNonNull(model);
        this.model = model;
    }

    @Override
    public void start() {
    }

    @Override
    public synchronized void triple(Triple triple) {
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
