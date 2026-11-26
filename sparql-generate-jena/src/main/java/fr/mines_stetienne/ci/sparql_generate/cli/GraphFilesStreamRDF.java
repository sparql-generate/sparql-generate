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

import java.util.Objects;

import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.Quad;

/**
 * Sends the quads of the named graphs that name files to these files
 * ({@link GraphTargets}), and everything else to the main output.
 */
final class GraphFilesStreamRDF implements StreamRDF {

	private final StreamRDF main;

	private final GraphTargets targets;

	GraphFilesStreamRDF(final StreamRDF main, final GraphTargets targets) {
		this.main = Objects.requireNonNull(main);
		this.targets = Objects.requireNonNull(targets);
	}

	@Override
	public void start() {
		main.start();
	}

	@Override
	public synchronized void triple(final Triple triple) {
		main.triple(triple);
	}

	@Override
	public synchronized void quad(final Quad quad) {
		if (!quad.isDefaultGraph()) {
			final GraphTargets.Resolved target = targets.resolve(quad.getGraph());
			if (target != null) {
				target.file.add(target.graph, quad.asTriple());
				return;
			}
		}
		main.quad(quad);
	}

	@Override
	public void base(final String base) {
		main.base(base);
	}

	@Override
	public synchronized void prefix(final String prefix, final String iri) {
		main.prefix(prefix, iri);
		targets.prefix(prefix, iri);
	}

	@Override
	public void finish() {
		main.finish();
	}

	/**
	 * The main output kept in memory: the dataset whose default graph and
	 * named graphs the command line writes at the end.
	 */
	static final class Collector implements StreamRDF {

		private final Dataset dataset;

		private final DatasetGraph dsg;

		Collector(final Dataset dataset) {
			this.dataset = dataset;
			this.dsg = dataset.asDatasetGraph();
		}

		@Override
		public void start() {
		}

		@Override
		public synchronized void triple(final Triple triple) {
			dataset.getDefaultModel().getGraph().add(triple);
		}

		@Override
		public synchronized void quad(final Quad quad) {
			if (quad.isDefaultGraph()) {
				triple(quad.asTriple());
			} else {
				dsg.add(quad);
			}
		}

		@Override
		public void base(final String base) {
		}

		@Override
		public synchronized void prefix(final String prefix, final String iri) {
			dataset.getDefaultModel().setNsPrefix(prefix, iri);
			dsg.prefixes().add(prefix, iri);
		}

		@Override
		public void finish() {
		}
	}
}
