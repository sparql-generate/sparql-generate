/*
 * Copyright 2019 École des Mines de Saint-Étienne.
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
package fr.emse.ci.sparqlext.generate.engine;

import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.Quad;

/**
 * Utility class to generate a Model from a stream of triples.
 *
 * @author maxime.lefrancois
 */
class StreamRDFModel implements StreamRDF {

    private final Model model;

    public StreamRDFModel(final Model model) {
        this.model = model;
    }

    @Override
    public void start() {
    }

    @Override
    public synchronized void triple(Triple triple) {
        model.add(model.asStatement(triple));
    }

    @Override
    public void quad(Quad quad) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void base(String base) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void prefix(String prefix, String iri) {
        model.setNsPrefix(prefix, iri);
    }

    @Override
    public void finish() {
    }

}
