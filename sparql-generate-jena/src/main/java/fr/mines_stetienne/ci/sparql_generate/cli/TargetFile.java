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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;
import org.apache.commons.io.output.WriterOutputStream;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.riot.RIOT;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFWriter;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.core.Quad;

import fr.mines_stetienne.ci.sparql_generate.cli.GraphTargets.Compression;

/**
 * A file that named graphs are written to ({@link GraphTargets}). It is
 * created at its first triple:
 * <ul>
 * <li>N-Triples and N-Quads are written as the triples are generated, each
 * triple once, or as they come with {@code --stream};</li>
 * <li>Turtle and TriG likewise with {@code --stream}, one block per triple,
 * and else at the end, from the graph built in memory;</li>
 * <li>the other formats (JSON-LD, RDF/XML, RDF/JSON, N3) at the end.</li>
 * </ul>
 * The bytes are transcoded from UTF-8 to the character encoding of the
 * target, then compressed; a tar archive is written from a temporary file
 * at the end, with one entry named after the file without its compression
 * extension, as is the entry of a zip archive.
 */
final class TargetFile {

	private final Path path;

	private final String name;

	private final Lang lang;

	private final Compression compression;

	private final Charset charset;

	private final boolean stream;

	private final Map<String, String> prefixes;

	/** the stream the RDF writer writes to (UTF-8), once opened */
	private OutputStream out;

	/** the file of a tar archive before it is archived */
	private Path tarContent;

	private StreamRDF writer;

	private DatasetGraph buffer;

	private boolean closed = false;

	TargetFile(final Path path, final String name, final Lang lang, final Compression compression,
			final Charset charset, final boolean stream, final Map<String, String> prefixes) {
		this.path = path;
		this.name = name;
		this.lang = lang;
		this.compression = compression;
		this.charset = charset;
		this.stream = stream;
		this.prefixes = prefixes;
	}

	Path getPath() {
		return path;
	}

	Lang getLang() {
		return lang;
	}

	Compression getCompression() {
		return compression;
	}

	Charset getCharset() {
		return charset;
	}

	private boolean isNTriplesOrNQuads() {
		return Lang.NTRIPLES.equals(lang) || Lang.NQUADS.equals(lang);
	}

	private boolean isStreamed() {
		return isNTriplesOrNQuads()
				|| (stream && (Lang.TURTLE.equals(lang) || Lang.TRIG.equals(lang)));
	}

	/**
	 * @param graph the graph in the file, null for its default graph
	 * @param triple the triple
	 */
	synchronized void add(final Node graph, final Triple triple) {
		if (closed) {
			throw new TargetException("The target file " + name + " is already closed");
		}
		if (!isStreamed()) {
			if (buffer == null) {
				buffer = DatasetGraphFactory.createTxnMem();
				prefixes.forEach((p, iri) -> buffer.prefixes().add(p, iri));
			}
			buffer.add(graph == null ? Quad.defaultGraphIRI : graph, triple.getSubject(), triple.getPredicate(),
					triple.getObject());
			return;
		}
		if (writer == null) {
			open();
		}
		if (graph == null) {
			writer.triple(triple);
		} else {
			writer.quad(Quad.create(graph, triple));
		}
	}

	private void open() {
		try {
			out = openStream();
		} catch (IOException ex) {
			throw new TargetException("Cannot open the target file " + path + ": " + ex.getMessage(), ex);
		}
		if (isNTriplesOrNQuads() && !stream) {
			// each triple once, as the main output without --stream
			writer = new DistinctNTriplesStreamRDF(out, Lang.NQUADS.equals(lang));
		} else if (Lang.NTRIPLES.equals(lang)) {
			writer = StreamRDFWriter.getWriterStream(out, RDFFormat.NTRIPLES);
		} else if (Lang.NQUADS.equals(lang)) {
			writer = StreamRDFWriter.getWriterStream(out, RDFFormat.NQUADS);
		} else if (Lang.TRIG.equals(lang)) {
			writer = StreamRDFWriter.getWriterStream(out, RDFFormat.TRIG_BLOCKS);
		} else {
			writer = StreamRDFWriter.getWriterStream(out, RDFFormat.TURTLE_BLOCKS);
		}
		writer.start();
		prefixes.forEach(writer::prefix);
	}

	/**
	 * Creates the file and its parent folders; the stream, UTF-8, transcoded
	 * and compressed.
	 */
	private OutputStream openStream() throws IOException {
		if (path.getParent() != null) {
			Files.createDirectories(path.getParent());
		}
		OutputStream file;
		if (compression.isTar()) {
			tarContent = Files.createTempFile(path.getParent(), ".sparql-generate-", ".tmp");
			file = new BufferedOutputStream(Files.newOutputStream(tarContent), 1 << 16);
		} else {
			file = new BufferedOutputStream(Files.newOutputStream(path), 1 << 16);
			switch (compression) {
			case GZIP:
				file = new GZIPOutputStream(file, 1 << 16);
				break;
			case ZIP:
				ZipOutputStream zip = new ZipOutputStream(file);
				zip.putNextEntry(new ZipEntry(entryName()));
				file = zip;
				break;
			case BZIP2:
				file = new BZip2CompressorOutputStream(file);
				break;
			case XZ:
				file = new XZCompressorOutputStream(file);
				break;
			default:
				break;
			}
		}
		if (StandardCharsets.UTF_8.equals(charset)) {
			return file;
		}
		// the RDF writers write UTF-8
		return WriterOutputStream.builder().setWriter(new OutputStreamWriter(file, charset))
				.setCharset(StandardCharsets.UTF_8).get();
	}

	/**
	 * The name of the entry of an archive: the name of the file without its
	 * compression extension.
	 */
	private String entryName() {
		String fileName = path.getFileName().toString();
		if (fileName.toLowerCase(java.util.Locale.ROOT).endsWith(compression.extension)) {
			fileName = fileName.substring(0, fileName.length() - compression.extension.length());
		}
		return fileName;
	}

	/**
	 * Writes a buffered file, closes the file, and writes a tar archive.
	 */
	synchronized void close() throws IOException {
		if (closed) {
			return;
		}
		closed = true;
		if (buffer != null) {
			out = openStream();
			if (RDFLanguages.isQuads(lang)) {
				RDFDataMgr.write(out, buffer, lang);
			} else if (Lang.N3.equals(lang)) {
				// N3 has @prefix, not the PREFIX of SPARQL that Turtle also allows
				RDFWriter.source(buffer.getDefaultGraph()).format(RDFFormat.TURTLE_PRETTY)
						.set(RIOT.symTurtleDirectiveStyle, "at").output(out);
			} else {
				RDFDataMgr.write(out, buffer.getDefaultGraph(), lang);
			}
		} else if (writer != null) {
			writer.finish();
		}
		if (out == null) {
			return;
		}
		out.close();
		if (tarContent != null) {
			writeTar();
		}
	}

	private void writeTar() throws IOException {
		try {
			OutputStream file = new BufferedOutputStream(Files.newOutputStream(path), 1 << 16);
			switch (compression) {
			case TAR_GZ:
				file = new GzipCompressorOutputStream(file);
				break;
			case TAR_XZ:
				file = new XZCompressorOutputStream(file);
				break;
			default:
				file = new BZip2CompressorOutputStream(file);
				break;
			}
			try (TarArchiveOutputStream tar = new TarArchiveOutputStream(file)) {
				tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
				TarArchiveEntry entry = new TarArchiveEntry(entryName());
				entry.setSize(Files.size(tarContent));
				entry.setModTime(System.currentTimeMillis());
				tar.putArchiveEntry(entry);
				try (InputStream in = Files.newInputStream(tarContent)) {
					in.transferTo(tar);
				}
				tar.closeArchiveEntry();
				tar.finish();
			}
		} finally {
			Files.deleteIfExists(tarContent);
		}
	}
}
