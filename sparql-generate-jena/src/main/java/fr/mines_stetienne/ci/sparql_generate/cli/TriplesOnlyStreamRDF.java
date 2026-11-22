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
package fr.mines_stetienne.ci.sparql_generate.cli;

import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFWrapper;
import org.apache.jena.sparql.core.Quad;

/**
 * The output of a format that holds triples only: a quad of the default graph
 * is a triple, a quad of a named graph is an error.
 */
final class TriplesOnlyStreamRDF extends StreamRDFWrapper {

	private final String format;

	TriplesOnlyStreamRDF(StreamRDF other, String format) {
		super(other);
		this.format = format;
	}

	@Override
	public void quad(Quad quad) {
		if (!quad.isDefaultGraph()) {
			throw new QuadsNotSupportedException(format);
		}
		triple(quad.asTriple());
	}
}
