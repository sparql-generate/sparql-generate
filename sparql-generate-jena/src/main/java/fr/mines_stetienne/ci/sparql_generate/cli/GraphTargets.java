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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;


/**
 * The named graphs that the command line writes to files
 * ({@code --graphs-to-files}): a named graph whose IRI is the folder of the
 * base IRI followed by a relative path that names an RDF file, by its
 * extension or in the description of the targets ({@code --targets}), is
 * written to that file, in the output folder; the other graphs stay in the
 * main output.
 * <ul>
 * <li>The output folder is the folder of the base IRI if it is a
 * {@code file:} IRI, and else the working directory, which the base IRI
 * identifies for the documents read by the query.</li>
 * <li>Format and compression come from the extension of the path
 * ({@code dump.nq.gz}), unless the description of the targets gives them, with
 * the character encoding.</li>
 * <li>{@code <dump.nq#g=IRI>} names the graph {@code IRI} inside the file
 * {@code dump.nq}: the fragment is {@code g=} followed by the IRI of the
 * graph, percent-encoded or not, as {@code ENCODE_FOR_URI} or the expressions
 * embedded in an IRI ({@code <dump.nq#g={STR(?g)}>}) give it.</li>
 * <li>A path that would leave the output folder ({@code ..} or {@code .}
 * segments, empty segments, encoded or not) is an error.</li>
 * </ul>
 */
final class GraphTargets {

	/**
	 * The description of a target in the file of {@code --targets}: a JSON
	 * object whose keys are the paths of the files, relative to the output
	 * folder.
	 */
	static final class Description {
		/** a format name of Jena (N-Quads, Turtle...), a media type or an extension */
		String format;
		/** none, gzip, zip, bzip2, xz, tar.gz, tar.xz, tar.bz2 */
		String compression;
		/** a character encoding (UTF-8, UTF-16...) */
		String encoding;
	}

	/**
	 * The compressions of a target file, by extension.
	 */
	enum Compression {
		NONE(""), TAR_GZ(".tar.gz"), TAR_XZ(".tar.xz"), TAR_BZIP2(".tar.bz2"), GZIP(".gz"), ZIP(".zip"),
		BZIP2(".bz2"), XZ(".xz");

		final String extension;

		Compression(String extension) {
			this.extension = extension;
		}

		boolean isTar() {
			return this == TAR_GZ || this == TAR_XZ || this == TAR_BZIP2;
		}

		static Compression ofName(String name) {
			switch (name.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "")) {
			case "":
			case "none":
				return NONE;
			case "gzip":
			case "gz":
				return GZIP;
			case "zip":
				return ZIP;
			case "bzip2":
			case "bz2":
				return BZIP2;
			case "xz":
				return XZ;
			case "tar.gz":
			case "targz":
			case "tgz":
				return TAR_GZ;
			case "tar.xz":
			case "tarxz":
				return TAR_XZ;
			case "tar.bz2":
			case "tarbz2":
				return TAR_BZIP2;
			default:
				throw new TargetException("Unknown compression in the description of the targets: " + name
						+ " (none, gzip, zip, bzip2, xz, tar.gz, tar.xz, tar.bz2)");
			}
		}

		/** the compression given by the end of a file name, NONE if none */
		static Compression ofFileName(String name) {
			String lower = name.toLowerCase(Locale.ROOT);
			for (Compression c : values()) {
				if (c != NONE && lower.endsWith(c.extension)) {
					return c;
				}
			}
			return NONE;
		}
	}

	/**
	 * The RDF formats of a target file, by extension.
	 */
	private static final Map<String, Lang> EXTENSIONS = new HashMap<>();
	static {
		EXTENSIONS.put("nq", Lang.NQUADS);
		EXTENSIONS.put("nt", Lang.NTRIPLES);
		EXTENSIONS.put("ttl", Lang.TURTLE);
		EXTENSIONS.put("trig", Lang.TRIG);
		EXTENSIONS.put("n3", Lang.N3);
		EXTENSIONS.put("jsonld", Lang.JSONLD);
		EXTENSIONS.put("rdf", Lang.RDFXML);
		EXTENSIONS.put("rdfxml", Lang.RDFXML);
		EXTENSIONS.put("rj", Lang.RDFJSON);
		EXTENSIONS.put("rdfjson", Lang.RDFJSON);
	}

	private static final Pattern ABSOLUTE_IRI = Pattern.compile("^[A-Za-z][A-Za-z0-9+.-]*:.*");

	/**
	 * A resolved graph name: the file, and the graph inside the file (null
	 * for its default graph).
	 */
	static final class Resolved {
		final TargetFile file;
		final Node graph;

		Resolved(TargetFile file, Node graph) {
			this.file = file;
			this.graph = graph;
		}
	}

	private static final Resolved NONE = new Resolved(null, null);

	private final String folderIri;

	private final Path folder;

	private final Map<String, Description> descriptions;

	private final boolean stream;

	private final Map<Path, TargetFile> files = new LinkedHashMap<>();

	private final Map<Node, Resolved> resolved = new HashMap<>();

	private final Map<String, String> prefixes = new LinkedHashMap<>();

	/**
	 * @param base the base IRI of the query; null for the working directory
	 * @param workingDir the working directory
	 * @param descriptions the descriptions of the targets, by relative path, or
	 *            null
	 * @param stream true to write the files as the triples are generated
	 *            (--stream), false to write each triple once
	 */
	GraphTargets(final String base, final File workingDir, final Map<String, Description> descriptions,
			final boolean stream) {
		final String b = base != null ? base : workingDir.getAbsoluteFile().toURI().toString();
		final int slash = b.lastIndexOf('/');
		if (slash < 0) {
			throw new TargetException("--graphs-to-files needs a base IRI with a path, got " + b);
		}
		this.folderIri = b.substring(0, slash + 1);
		if (folderIri.startsWith("file:")) {
			try {
				this.folder = Paths.get(URI.create(folderIri)).toAbsolutePath().normalize();
			} catch (IllegalArgumentException ex) {
				throw new TargetException("--graphs-to-files: the base IRI is not a folder: " + b, ex);
			}
		} else {
			this.folder = workingDir.getAbsoluteFile().toPath().normalize();
		}
		this.descriptions = new HashMap<>();
		if (descriptions != null) {
			for (Map.Entry<String, Description> e : descriptions.entrySet()) {
				String path = e.getKey().startsWith("./") ? e.getKey().substring(2) : e.getKey();
				checkSafe(path, e.getKey());
				this.descriptions.put(path, e.getValue() == null ? new Description() : e.getValue());
			}
		}
		this.stream = stream;
	}

	/**
	 * Reads the description of the targets: a JSON object whose keys are
	 * paths relative to the output folder, and whose values have the keys
	 * format, compression and encoding.
	 */
	static Map<String, Description> readDescriptions(final File file) {
		final Type type = new TypeToken<LinkedHashMap<String, Description>>() {
		}.getType();
		try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
			Map<String, Description> map = new Gson().fromJson(reader, type);
			return map == null ? new LinkedHashMap<>() : map;
		} catch (IOException ex) {
			throw new TargetException("Cannot read the description of the targets " + file, ex);
		} catch (JsonParseException ex) {
			throw new TargetException("Invalid description of the targets " + file + ": " + ex.getMessage(), ex);
		}
	}

	Path getFolder() {
		return folder;
	}

	String getFolderIri() {
		return folderIri;
	}

	/**
	 * @param graph the name of a graph
	 * @return its file and the graph inside the file, or null if it names no
	 *         file
	 */
	synchronized Resolved resolve(final Node graph) {
		Resolved r = resolved.get(graph);
		if (r == null) {
			r = doResolve(graph);
			resolved.put(graph, r);
		}
		return r == NONE ? null : r;
	}

	private Resolved doResolve(final Node graph) {
		if (!graph.isURI() || !graph.getURI().startsWith(folderIri)) {
			return NONE;
		}
		final String iri = graph.getURI();
		String rest = iri.substring(folderIri.length());
		String fragment = null;
		final int hash = rest.indexOf('#');
		if (hash >= 0) {
			fragment = rest.substring(hash + 1);
			rest = rest.substring(0, hash);
		}
		if (rest.isEmpty() || rest.indexOf('?') >= 0) {
			// the folder itself, or a query: not a file
			return NONE;
		}
		final String path = percentDecode(rest, iri);
		final Description description = descriptions.get(path);
		final Lang lang = lang(path, description);
		if (lang == null) {
			if (description != null) {
				throw new TargetException("No format for the target " + path
						+ ": give it in the description of the targets, or as an extension of the file name");
			}
			// not the name of an RDF file
			return NONE;
		}
		checkSafe(path, iri);
		final Path file = folder.resolve(path).normalize();
		if (!file.startsWith(folder) || file.equals(folder)) {
			throw new TargetException("The graph <" + iri + "> names a file outside of the output folder " + folder);
		}
		final Node inFile;
		if (fragment == null) {
			inFile = null;
		} else if (fragment.startsWith("g=") && fragment.length() > 2) {
			final String name = percentDecode(fragment.substring(2), iri);
			if (!ABSOLUTE_IRI.matcher(name).matches()) {
				throw new TargetException("The graph <" + iri + "> names a graph in the file " + path
						+ " whose IRI is not absolute: " + name);
			}
			inFile = NodeFactory.createURI(name);
		} else {
			throw new TargetException("The graph <" + iri + "> names the file " + path
					+ " with a fragment that is not g= followed by the IRI of a graph");
		}
		TargetFile target = files.get(file);
		if (target == null) {
			target = new TargetFile(file, path, lang, compression(path, description), charset(path, description),
					stream, prefixes);
			files.put(file, target);
		}
		if (inFile != null && !RDFLanguages.isQuads(lang)) {
			throw new TargetException("The graph <" + iri + "> names a graph in the file " + path + ", which " + lang.getLabel() + " cannot hold: use .nq, .trig or .jsonld");
		}
		return new Resolved(target, inFile);
	}

	/**
	 * Refuses a relative path that could leave the output folder.
	 */
	private static void checkSafe(final String path, final String name) {
		if (path.isEmpty() || path.startsWith("/") || path.indexOf('\\') >= 0 || path.indexOf('\0') >= 0) {
			throw new TargetException("Unsafe target path " + path + " (" + name + ")");
		}
		for (String segment : path.split("/", -1)) {
			if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
				throw new TargetException("Unsafe target path " + path + " (" + name
						+ "): a target is written in the output folder only");
			}
		}
	}

	private static Lang lang(final String path, final Description description) {
		if (description != null && description.format != null) {
			final String f = description.format.trim();
			Lang lang = RDFLanguages.nameToLang(f);
			if (lang == null) {
				lang = RDFLanguages.contentTypeToLang(f);
			}
			if (lang == null) {
				lang = EXTENSIONS.get(f.toLowerCase(Locale.ROOT).replaceFirst("^\\.", ""));
			}
			if (lang == null) {
				throw new TargetException("Unknown format in the description of the targets: " + f);
			}
			return lang;
		}
		final String name = path.substring(path.lastIndexOf('/') + 1);
		final Compression compression = Compression.ofFileName(name);
		final String base = name.substring(0, name.length() - compression.extension.length());
		final int dot = base.lastIndexOf('.');
		if (dot <= 0) {
			return null;
		}
		return EXTENSIONS.get(base.substring(dot + 1).toLowerCase(Locale.ROOT));
	}

	private static Compression compression(final String path, final Description description) {
		if (description != null && description.compression != null) {
			return Compression.ofName(description.compression.trim());
		}
		return Compression.ofFileName(path.substring(path.lastIndexOf('/') + 1));
	}

	private static Charset charset(final String path, final Description description) {
		if (description != null && description.encoding != null) {
			try {
				return Charset.forName(description.encoding.trim());
			} catch (IllegalArgumentException ex) {
				throw new TargetException(
						"Unknown character encoding for the target " + path + ": " + description.encoding, ex);
			}
		}
		return StandardCharsets.UTF_8;
	}

	/**
	 * Decodes the %XX sequences of a string as UTF-8.
	 */
	static String percentDecode(final String s, final String iri) {
		if (s.indexOf('%') < 0) {
			return s;
		}
		final StringBuilder sb = new StringBuilder(s.length());
		final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		int i = 0;
		while (i < s.length()) {
			final char c = s.charAt(i);
			if (c != '%') {
				sb.append(c);
				i++;
				continue;
			}
			bytes.reset();
			while (i < s.length() && s.charAt(i) == '%') {
				if (i + 2 >= s.length() || Character.digit(s.charAt(i + 1), 16) < 0
						|| Character.digit(s.charAt(i + 2), 16) < 0) {
					throw new TargetException("Invalid percent-encoding in the graph <" + iri + ">");
				}
				bytes.write(Integer.parseInt(s.substring(i + 1, i + 3), 16));
				i += 3;
			}
			sb.append(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
		}
		return sb.toString();
	}

	synchronized void prefix(final String prefix, final String iri) {
		prefixes.put(prefix, iri);
	}

	/**
	 * Closes every file: the buffered ones are written now.
	 */
	synchronized void close() {
		final List<Exception> errors = new ArrayList<>();
		for (TargetFile file : files.values()) {
			try {
				file.close();
			} catch (IOException | RuntimeException ex) {
				errors.add(ex);
			}
		}
		if (!errors.isEmpty()) {
			throw new TargetException("Error while writing the target files: " + errors.get(0).getMessage(),
					errors.get(0));
		}
	}

	/**
	 * @return the target files that a graph was routed to, by path
	 */
	synchronized Map<Path, TargetFile> getFiles() {
		return files;
	}
}
