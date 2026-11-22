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
package fr.mines_stetienne.ci.sparql_generate.cli;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.serializer.SerializationContext;
import org.apache.jena.sparql.util.FmtUtils;

/**
 * Outputs Stream RDF as Turtle, one triple per line, to a PrintStream with a
 * PrefixMapping. The stream is flushed at the end, not closed.
 * 
 * @author Maxime Lefrançois
 */
public class ConsoleStreamRDF implements StreamRDF {

	private final PrefixMapping pm;
	private final SerializationContext context;

	private PrintStream out;

	private int i = 0;
	private final int MAX = 10000;

	/**
	 * The name of the format of the output, if it holds triples only, or null
	 * if quads are written as TriG.
	 */
	private final String triplesOnlyFormat;

	public ConsoleStreamRDF(PrintStream out, PrefixMapping pm) {
		this(out, pm, null);
	}

	/**
	 * @param triplesOnlyFormat the name of the format asked for, if it holds
	 *            triples only: a quad of a named graph is then an error; or
	 *            null: such a quad is written as a line of TriG.
	 */
	public ConsoleStreamRDF(PrintStream out, PrefixMapping pm, String triplesOnlyFormat) {
		this.out = out;
		this.pm = pm;
		this.triplesOnlyFormat = triplesOnlyFormat;
		context = new SerializationContext(pm);
	}

	/**
	 * Writes to an output stream through a buffer, in UTF-8.
	 */
	public ConsoleStreamRDF(OutputStream out, PrefixMapping pm) {
		this(out, pm, null);
	}

	/**
	 * Writes to an output stream through a buffer, in UTF-8.
	 *
	 * @see #ConsoleStreamRDF(PrintStream, PrefixMapping, String)
	 */
	public ConsoleStreamRDF(OutputStream out, PrefixMapping pm, String triplesOnlyFormat) {
		this(new PrintStream(new BufferedOutputStream(out, 1 << 16), false, StandardCharsets.UTF_8), pm,
				triplesOnlyFormat);
	}

	@Override
	public void start() {
	}

	@Override
	public void base(String string) {
		out.append("@base <").append(string).append("> .\n");
	}

	@Override
	public void prefix(String prefix, String uri) {
		out.append("@prefix ").append(prefix).append(": <").append(uri).append("> .\n");
	}

	@Override
	public void triple(Triple triple) {
		out.append(FmtUtils.stringForTriple(triple, context)).append(" .\n");
		i++;
		if (i >= MAX) {
			i = 0;
			out.flush();
		}
	}

	/**
	 * A quad of a named graph is written as a TriG block of one triple,
	 * {@code <g> { <s> <p> <o> . }}: the output of a query with quads is TriG,
	 * that of a query without quads stays Turtle.
	 */
	@Override
	public void quad(Quad quad) {
		if (quad.isDefaultGraph()) {
			triple(quad.asTriple());
			return;
		}
		if (triplesOnlyFormat != null) {
			throw new QuadsNotSupportedException(triplesOnlyFormat);
		}
		out.append(FmtUtils.stringForNode(quad.getGraph(), context)).append(" { ")
				.append(FmtUtils.stringForTriple(quad.asTriple(), context)).append(" . }\n");
		i++;
		if (i >= MAX) {
			i = 0;
			out.flush();
		}
	}

	@Override
	public void finish() {
		out.flush();
	}
}
