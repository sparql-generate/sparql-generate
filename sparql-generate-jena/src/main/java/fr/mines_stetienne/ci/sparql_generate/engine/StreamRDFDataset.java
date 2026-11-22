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

import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.Quad;

/**
 * Collects the output of a GENERATE query in a dataset: the triples in its
 * default graph, the quads in its named graphs. The default graph is the model
 * given, which holds the prefixes of the query, as {@link StreamRDFModel}
 * does.
 */
class StreamRDFDataset implements StreamRDF {

    private final Model defaultModel;

    private final DatasetGraph dsg;

    StreamRDFDataset(final Dataset dataset) {
        Objects.requireNonNull(dataset);
        this.defaultModel = dataset.getDefaultModel();
        this.dsg = dataset.asDatasetGraph();
    }

    @Override
    public void start() {
    }

    @Override
    public synchronized void triple(Triple triple) {
        defaultModel.getGraph().add(triple);
    }

    @Override
    public synchronized void quad(Quad quad) {
        if (quad.isDefaultGraph()) {
            defaultModel.getGraph().add(quad.asTriple());
        } else {
            dsg.add(quad);
        }
    }

    @Override
    public void base(String base) {
    }

    @Override
    public synchronized void prefix(String prefix, String iri) {
        defaultModel.setNsPrefix(prefix, iri);
        dsg.prefixes().add(prefix, iri);
    }

    @Override
    public void finish() {
    }

}
