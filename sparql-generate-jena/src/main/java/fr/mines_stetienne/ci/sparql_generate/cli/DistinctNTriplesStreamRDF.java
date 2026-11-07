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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.jena.atlas.io.AWriter;
import org.apache.jena.atlas.io.IO;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.out.NodeFormatter;
import org.apache.jena.riot.out.NodeFormatterNT;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.Quad;

/**
 * Writes the triples as N-Triples, each one once, in the order in which they
 * are first generated: the set of triples of the graph, without building the
 * graph in memory. A triple is recognised by the first 128 bits of the
 * SHA-256 digest of its N-Triples line: 16 bytes per distinct triple.
 */
final class DistinctNTriplesStreamRDF implements StreamRDF {

	private final Writer out;
	private final NodeFormatter formatter = new NodeFormatterNT();
	private final StringWriter line = new StringWriter(256);
	private final AWriter lineWriter = IO.wrap(line);
	private final MessageDigest sha256;
	private final DigestSet seen = new DigestSet();

	DistinctNTriplesStreamRDF(OutputStream out) {
		this.out = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8), 1 << 16);
		try {
			sha256 = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}

	@Override
	public void start() {
	}

	@Override
	public void triple(Triple triple) {
		line.getBuffer().setLength(0);
		formatter.format(lineWriter, triple.getSubject());
		lineWriter.print(' ');
		formatter.format(lineWriter, triple.getPredicate());
		lineWriter.print(' ');
		formatter.format(lineWriter, triple.getObject());
		lineWriter.print(" .\n");
		lineWriter.flush();
		final String text = line.toString();
		final byte[] digest = sha256.digest(text.getBytes(StandardCharsets.UTF_8));
		if (seen.add(toLong(digest, 0), toLong(digest, 8))) {
			try {
				out.write(text);
			} catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		}
	}

	private static long toLong(byte[] b, int offset) {
		long v = 0;
		for (int i = 0; i < 8; i++) {
			v = (v << 8) | (b[offset + i] & 0xFF);
		}
		return v;
	}

	@Override
	public void quad(Quad quad) {
	}

	@Override
	public void base(String base) {
	}

	@Override
	public void prefix(String prefix, String iri) {
	}

	@Override
	public void finish() {
		try {
			out.flush();
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	/**
	 * A set of 128-bit keys, by open addressing.
	 */
	static final class DigestSet {
		private long[] high = new long[1 << 16];
		private long[] low = new long[1 << 16];
		private boolean[] used = new boolean[1 << 16];
		private int size = 0;

		/**
		 * @return true if the key was not in the set.
		 */
		boolean add(long h, long l) {
			if (size * 10L >= high.length * 6L) {
				grow();
			}
			return insert(h, l);
		}

		int size() {
			return size;
		}

		private boolean insert(long h, long l) {
			final int mask = high.length - 1;
			// the keys are digests, but spread them anyway
			final long mixed = (h ^ (l * 0x9E3779B97F4A7C15L)) * 0xBF58476D1CE4E5B9L;
			int i = (int) (mixed ^ (mixed >>> 31)) & mask;
			while (used[i]) {
				if (high[i] == h && low[i] == l) {
					return false;
				}
				i = (i + 1) & mask;
			}
			used[i] = true;
			high[i] = h;
			low[i] = l;
			size++;
			return true;
		}

		private void grow() {
			final long[] oldHigh = high;
			final long[] oldLow = low;
			final boolean[] oldUsed = used;
			high = new long[oldHigh.length * 2];
			low = new long[oldLow.length * 2];
			used = new boolean[oldUsed.length * 2];
			size = 0;
			for (int i = 0; i < oldHigh.length; i++) {
				if (oldUsed[i]) {
					insert(oldHigh[i], oldLow[i]);
				}
			}
		}
	}
}
