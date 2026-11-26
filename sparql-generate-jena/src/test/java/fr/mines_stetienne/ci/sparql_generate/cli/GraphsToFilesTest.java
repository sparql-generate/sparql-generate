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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.util.IsoMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * {@code --graphs-to-files} and {@code --targets}: named graphs written to
 * the files they name, in a separate JVM as {@link CliTest}; and the
 * resolution of graph names by {@link GraphTargets}.
 */
public class GraphsToFilesTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	static class Result {
		int exitCode;
		String out;
		String err;
	}

	private Result run(String query, String... args) throws Exception {
		File dir = folder.getRoot();
		Files.write(new File(dir, "query.rqg").toPath(), query.getBytes(StandardCharsets.UTF_8));
		File out = folder.newFile();
		File err = folder.newFile();
		List<String> command = new ArrayList<>(Arrays.asList(
				new File(System.getProperty("java.home"), "bin/java").getAbsolutePath(), "-cp",
				System.getProperty("java.class.path"), SPARQLExtCli.class.getName(), "-d", dir.getAbsolutePath()));
		command.addAll(Arrays.asList(args));
		// the current directory: the working directory
		Process process = new ProcessBuilder(command).directory(dir).redirectOutput(out).redirectError(err).start();
		assertTrue("the CLI did not finish", process.waitFor(60, TimeUnit.SECONDS));
		Result result = new Result();
		result.exitCode = process.exitValue();
		result.out = new String(Files.readAllBytes(out.toPath()), StandardCharsets.UTF_8);
		result.err = new String(Files.readAllBytes(err.toPath()), StandardCharsets.UTF_8);
		out.delete();
		err.delete();
		return result;
	}

	private static final String BASE = "http://ex/base/";

	/**
	 * Triples in the default graph, in a graph that names a file, in a graph
	 * that does not, and in a graph of a file.
	 */
	private static String query(String file) {
		return "BASE <" + BASE + "> PREFIX ex: <http://ex/> PREFIX ite: <http://w3id.org/sparql-generate/iter/> "
				+ "GENERATE { ex:s ex:p ?i . GRAPH <" + file + "> { ex:s ex:q ?i } GRAPH ex:g { ex:s ex:r ?i } } "
				+ "ITERATOR ite:for(1, 1, 3) AS ?i";
	}

	private static final String MAIN = "<http://ex/s> <http://ex/p> 1, 2 . <http://ex/g> { <http://ex/s> <http://ex/r> 1, 2 }";

	private static final String FILE = "<http://ex/s> <http://ex/q> 1, 2 .";

	private static void assertDataset(String expectedTrig, InputStream actual, Lang lang) {
		Dataset expected = DatasetFactory.create();
		RDFDataMgr.read(expected, new StringReader(expectedTrig), null, Lang.TRIG);
		Dataset dataset = DatasetFactory.create();
		RDFDataMgr.read(dataset, actual, lang);
		StringBuilder sb = new StringBuilder();
		dataset.asDatasetGraph().find().forEachRemaining(q -> sb.append(q).append('\n'));
		assertTrue(sb.toString(), IsoMatcher.isomorphic(expected.asDatasetGraph(), dataset.asDatasetGraph()));
	}

	private static void assertDataset(String expectedTrig, String actual, Lang lang) {
		assertDataset(expectedTrig, new ByteArrayInputStream(actual.getBytes(StandardCharsets.UTF_8)), lang);
	}

	private File file(String path) {
		return new File(folder.getRoot(), path);
	}

	private void assertFile(String expectedTrig, String path, Lang lang) throws IOException {
		File f = file(path);
		assertTrue(path + " not written", f.exists());
		try (InputStream in = Files.newInputStream(f.toPath())) {
			assertDataset(expectedTrig, in, lang);
		}
	}

	@Test
	public void namedGraphToFile() throws Exception {
		for (String[] args : new String[][] { {}, { "-s" }, { "-fo", "NQUADS" }, { "-fo", "NQUADS", "-s" } }) {
			file("out/people.nq").delete();
			List<String> list = new ArrayList<>(Arrays.asList("-b", BASE, "--graphs-to-files"));
			list.addAll(Arrays.asList(args));
			Result result = run(query("out/people.nq"), list.toArray(new String[0]));
			assertEquals(list + result.err, 0, result.exitCode);
			assertDataset(MAIN, result.out, list.contains("NQUADS") ? Lang.NQUADS : Lang.TRIG);
			assertFile(FILE, "out/people.nq", Lang.NQUADS);
		}
	}

	/**
	 * A graph name relative to the base without RDF extension stays a graph;
	 * an absolute graph name too, and one resolved outside of the folder.
	 */
	@Test
	public void otherGraphsStayInTheOutput() throws Exception {
		for (String graph : new String[] { "people", "http://ex/elsewhere.nq", "../up.nq" }) {
			Result result = run(query(graph), "-b", BASE, "--graphs-to-files", "-fo", "NQUADS");
			assertEquals(graph + result.err, 0, result.exitCode);
			assertTrue(graph + result.out, result.out.contains("<http://ex/q>"));
		}
		assertFalse(new File(folder.getRoot().getParentFile(), "up.nq").exists());
		assertFalse(file("people").exists());
	}

	@Test
	public void withoutTheFlagNothingChanges() throws Exception {
		Result result = run(query("people.nq"), "-b", BASE, "-fo", "NQUADS");
		assertEquals(result.err, 0, result.exitCode);
		assertTrue(result.out, result.out.contains("<http://ex/base/people.nq>"));
		assertFalse(file("people.nq").exists());
	}

	@Test
	public void formatsByExtension() throws Exception {
		Map<String, Lang> formats = new HashMap<>();
		formats.put("f.nt", Lang.NTRIPLES);
		formats.put("f.ttl", Lang.TURTLE);
		formats.put("f.trig", Lang.TRIG);
		formats.put("f.n3", Lang.TURTLE);
		formats.put("f.jsonld", Lang.JSONLD);
		formats.put("f.rdf", Lang.RDFXML);
		formats.put("f.rdfxml", Lang.RDFXML);
		formats.put("f.rj", Lang.RDFJSON);
		formats.put("f.rdfjson", Lang.RDFJSON);
		for (Map.Entry<String, Lang> e : formats.entrySet()) {
			for (String stream : new String[] { "", "-s" }) {
				file(e.getKey()).delete();
				Result result = stream.isEmpty() ? run(query(e.getKey()), "-b", BASE, "--graphs-to-files")
						: run(query(e.getKey()), "-b", BASE, "--graphs-to-files", stream);
				assertEquals(e.getKey() + stream + result.err, 0, result.exitCode);
				assertFile(FILE, e.getKey(), e.getValue());
			}
		}
	}

	/**
	 * N3 with @prefix directives, not PREFIX.
	 */
	@Test
	public void n3UsesAtPrefix() throws Exception {
		Result result = run(query("f.n3").replace("PREFIX ex: <http://ex/>", "PREFIX ex: <http://ex/> PREFIX q: <http://ex/q>"),
				"-b", BASE, "--graphs-to-files");
		assertEquals(result.err, 0, result.exitCode);
		String n3 = new String(Files.readAllBytes(file("f.n3").toPath()), StandardCharsets.UTF_8);
		assertFalse(n3, n3.contains("PREFIX"));
	}

	@Test
	public void compressionsByExtension() throws Exception {
		for (String name : new String[] { "c.nq.gz", "c.nq.zip", "c.nq.tar.gz", "c.nq.tar.xz", "c.nq.bz2",
				"c.nq.xz", "c.ttl.tar.bz2" }) {
			Result result = run(query(name), "-b", BASE, "--graphs-to-files");
			assertEquals(name + result.err, 0, result.exitCode);
			File f = file(name);
			assertTrue(name, f.exists());
			byte[] bytes = Files.readAllBytes(f.toPath());
			String inner = name.endsWith(".ttl.tar.bz2") ? "c.ttl" : "c.nq";
			byte[] content = decompress(name, bytes, inner);
			assertDataset(FILE, new ByteArrayInputStream(content), inner.endsWith(".ttl") ? Lang.TURTLE : Lang.NQUADS);
			// no temporary file left
			for (String other : folder.getRoot().list()) {
				assertFalse(other, other.endsWith(".tmp"));
			}
		}
	}

	private static byte[] decompress(String name, byte[] bytes, String inner) throws IOException {
		InputStream in = new ByteArrayInputStream(bytes);
		if (name.endsWith(".zip")) {
			try (ZipInputStream zip = new ZipInputStream(in)) {
				ZipEntry entry = zip.getNextEntry();
				assertEquals(inner, entry.getName());
				return IOUtils.toByteArray(zip);
			}
		}
		if (name.contains(".tar.")) {
			InputStream c = name.endsWith(".gz") ? new GzipCompressorInputStream(in)
					: name.endsWith(".xz") ? new XZCompressorInputStream(in) : new BZip2CompressorInputStream(in);
			try (TarArchiveInputStream tar = new TarArchiveInputStream(c)) {
				TarArchiveEntry entry = tar.getNextEntry();
				assertEquals(inner, entry.getName());
				byte[] content = IOUtils.toByteArray(tar);
				assertNull(tar.getNextEntry());
				return content;
			}
		}
		if (name.endsWith(".gz")) {
			return IOUtils.toByteArray(new GZIPInputStream(in));
		}
		if (name.endsWith(".bz2")) {
			return IOUtils.toByteArray(new BZip2CompressorInputStream(in));
		}
		return IOUtils.toByteArray(new XZCompressorInputStream(in));
	}

	/**
	 * GRAPH <dump.nq#g=IRI>: the graph IRI in the file dump.nq; the IRI can be
	 * percent-encoded, as the expressions embedded in an IRI give it.
	 */
	@Test
	public void namedGraphsInsideAFile() throws Exception {
		String query = "BASE <" + BASE + "> PREFIX ex: <http://ex/> PREFIX ite: <http://w3id.org/sparql-generate/iter/> "
				+ "GENERATE { GRAPH <dump.nq> { ex:s ex:p ?i } GRAPH <dump.nq#g=http://ex/g1> { ex:s ex:q ?i } "
				+ "GRAPH <dump.nq#g={STR(?g)}> { ex:s ex:r ?i } GRAPH <dump.trig#g=http://ex/g%23a%2525b> { ex:s ex:p ?i } } "
				+ "ITERATOR ite:for(1, 1, 3) AS ?i BIND(IRI(CONCAT(\"http://ex/h?x=\", STR(?i), \"#f\")) AS ?g)";
		for (String stream : new String[] { "", "-s" }) {
			Result result = stream.isEmpty() ? run(query, "-b", BASE, "--graphs-to-files", "-fo", "NQUADS")
					: run(query, "-b", BASE, "--graphs-to-files", "-fo", "NQUADS", stream);
			assertEquals(result.err, 0, result.exitCode);
			assertTrue(result.out, result.out.trim().isEmpty());
			assertFile("<http://ex/s> <http://ex/p> 1, 2 . <http://ex/g1> { <http://ex/s> <http://ex/q> 1, 2 } "
					+ "<http://ex/h?x=1#f> { <http://ex/s> <http://ex/r> 1 } <http://ex/h?x=2#f> { <http://ex/s> <http://ex/r> 2 }",
					"dump.nq", Lang.NQUADS);
			assertFile("<http://ex/g#a%25b> { <http://ex/s> <http://ex/p> 1, 2 }", "dump.trig", Lang.TRIG);
		}
	}

	@Test
	public void namedGraphInATriplesOnlyFileFails() throws Exception {
		Result result = run(query("dump.ttl#g=http://ex/g1"), "-b", BASE, "--graphs-to-files");
		assertEquals(result.err, 1, result.exitCode);
		assertTrue(result.err, result.err.contains("which Turtle cannot hold"));
	}

	@Test
	public void invalidFragmentFails() throws Exception {
		for (String graph : new String[] { "dump.nq#other", "dump.nq#g=relative", "dump.nq#g=" }) {
			Result result = run(query(graph), "-b", BASE, "--graphs-to-files");
			assertEquals(graph + result.err, 1, result.exitCode);
			assertTrue(graph + result.err, result.err.contains("ERROR"));
		}
	}

	/**
	 * A graph name can come from the data: a path that would leave the
	 * folder is refused, encoded or not.
	 */
	@Test
	public void unsafePathsFail() throws Exception {
		for (String graph : new String[] { "%2E%2E/evil.nq", "a/%2e%2e/%2E%2E/evil.nq", "a//evil.nq", "%2Fevil.nq",
				"a%5C..%5Cevil.nq", "a/%2E/evil.nq" }) {
			Result result = run(query(graph), "-b", BASE, "--graphs-to-files");
			assertEquals(graph + result.err, 1, result.exitCode);
			assertTrue(graph + result.err, result.err.contains("Unsafe target path") || result.err.contains("outside"));
		}
		assertFalse(new File(folder.getRoot().getParentFile(), "evil.nq").exists());
		assertFalse(new File("/evil.nq").exists());
	}

	/**
	 * A graph name computed from the data, with an unsafe path.
	 */
	@Test
	public void unsafePathFromTheDataFails() throws Exception {
		String query = "BASE <" + BASE + "> PREFIX ex: <http://ex/> "
				+ "GENERATE { GRAPH ?g { ex:s ex:p ?x } } WHERE { VALUES ?x { \"ok\" \"%2E%2E/evil\" } "
				+ "BIND(IRI(CONCAT(?x, \".nq\")) AS ?g) }";
		Result result = run(query, "-b", BASE, "--graphs-to-files", "-fo", "NQUADS");
		assertEquals(result.err, 1, result.exitCode);
		assertTrue(result.err, result.err.contains("Unsafe target path"));
		assertFalse(new File(folder.getRoot().getParentFile(), "evil.nq").exists());
	}

	/**
	 * With a file: base, the files are written in its folder.
	 */
	@Test
	public void fileBase() throws Exception {
		File out = folder.newFolder("elsewhere");
		String base = out.toURI().toString();
		// the base of the command line: the query has no BASE
		Result result = run(query("people.ttl").replace("BASE <" + BASE + "> ", ""), "-b", base, "--graphs-to-files",
				"-fo", "NQUADS");
		assertEquals(result.err, 0, result.exitCode);
		assertFile(FILE, "elsewhere/people.ttl", Lang.TURTLE);
		assertFalse(file("people.ttl").exists());
	}

	/**
	 * Without base, the relative IRIs of the query are resolved against the
	 * current directory: the files are written there.
	 */
	@Test
	public void noBase() throws Exception {
		Result result = run(query("people.ttl").replace("BASE <" + BASE + "> ", ""), "--graphs-to-files", "-fo",
				"NQUADS");
		assertEquals(result.err, 0, result.exitCode);
		assertFile(FILE, "people.ttl", Lang.TURTLE);
	}

	/**
	 * A main output format that holds triples only is fine when the quads go
	 * to files.
	 */
	@Test
	public void triplesOnlyOutputWhenEveryGraphGoesToAFile() throws Exception {
		String query = "BASE <" + BASE + "> PREFIX ex: <http://ex/> "
				+ "GENERATE { ex:s ex:p 1 . GRAPH <people.nq> { ex:s ex:q 2 } }";
		for (String[] args : new String[][] { { "-fo", "NT" }, { "-fo", "TTL" }, { "-fo", "NT", "-s" } }) {
			List<String> list = new ArrayList<>(Arrays.asList("-b", BASE, "--graphs-to-files"));
			list.addAll(Arrays.asList(args));
			Result result = run(query, list.toArray(new String[0]));
			assertEquals(list + result.err, 0, result.exitCode);
			assertDataset("<http://ex/s> <http://ex/p> 1 .", result.out, Lang.TURTLE);
			assertFile("<http://ex/s> <http://ex/q> 2 .", "people.nq", Lang.NQUADS);
		}
	}

	/**
	 * The description of the targets: encoding, format of a file without
	 * extension, compression.
	 */
	@Test
	public void targetsDescription() throws Exception {
		File targets = folder.newFile("targets.json");
		Files.write(targets.toPath(), ("{ \"./dump1.nq\": { \"encoding\": \"UTF-16\" },"
				+ " \"data/dump2\": { \"format\": \"Turtle\", \"compression\": \"gzip\" },"
				+ " \"dump3.out\": { \"format\": \"application/n-triples\" } }").getBytes(StandardCharsets.UTF_8));
		String query = "BASE <" + BASE + "> PREFIX ex: <http://ex/> "
				+ "GENERATE { GRAPH <dump1.nq> { ex:s ex:p \"é\" } GRAPH <data/dump2> { ex:s ex:q 2 } "
				+ "GRAPH <dump3.out> { ex:s ex:r 3 } }";
		Result result = run(query, "-b", BASE, "--targets", targets.getAbsolutePath(), "-fo", "NQUADS");
		assertEquals(result.err, 0, result.exitCode);
		byte[] utf16 = Files.readAllBytes(file("dump1.nq").toPath());
		assertArrayEquals(new byte[] { (byte) 0xFE, (byte) 0xFF }, Arrays.copyOf(utf16, 2));
		assertDataset("<http://ex/s> <http://ex/p> \"é\" .", new String(utf16, StandardCharsets.UTF_16), Lang.NQUADS);
		assertDataset("<http://ex/s> <http://ex/q> 2 .",
				new ByteArrayInputStream(IOUtils.toByteArray(new GZIPInputStream(Files.newInputStream(file("data/dump2").toPath())))),
				Lang.TURTLE);
		assertFile("<http://ex/s> <http://ex/r> 3 .", "dump3.out", Lang.NTRIPLES);
		// the same with the configuration file, relative to the working directory
		Files.write(file("sparql-generate-conf.json").toPath(), "{ \"targets\": \"targets.json\" }".getBytes(StandardCharsets.UTF_8));
		file("dump1.nq").delete();
		result = run(query, "-b", BASE, "-fo", "NQUADS");
		assertEquals(result.err, 0, result.exitCode);
		assertTrue(file("dump1.nq").exists());
	}

	@Test
	public void invalidTargetsDescriptionFails() throws Exception {
		File targets = folder.newFile("targets.json");
		Files.write(targets.toPath(), "{ \"../x.nq\": {} }".getBytes(StandardCharsets.UTF_8));
		Result result = run(query("x.nq"), "-b", BASE, "--targets", targets.getAbsolutePath());
		assertEquals(result.err, 1, result.exitCode);
		Files.write(targets.toPath(), "{ \"x.nq\": { \"compression\": \"rar\" } }".getBytes(StandardCharsets.UTF_8));
		result = run(query("x.nq"), "-b", BASE, "--targets", targets.getAbsolutePath());
		assertEquals(result.err, 1, result.exitCode);
		assertTrue(result.err, result.err.contains("Unknown compression"));
	}

	/**
	 * A file is created at its first triple: a graph without triples gives
	 * no file.
	 */
	@Test
	public void noTripleNoFile() throws Exception {
		String query = "BASE <" + BASE + "> PREFIX ex: <http://ex/> "
				+ "GENERATE { GRAPH <empty.nq> { ex:s ex:p ?x } } WHERE { VALUES ?x { UNDEF } }";
		Result result = run(query, "-b", BASE, "--graphs-to-files");
		assertEquals(result.err, 0, result.exitCode);
		assertFalse(file("empty.nq").exists());
	}

	/**
	 * The files written before RAISE stops the execution are complete files.
	 */
	@Test
	public void filesAreClosedWhenTheExecutionFails() throws Exception {
		String query = "BASE <" + BASE + "> PREFIX ex: <http://ex/> "
				+ "GENERATE { GENERATE { GRAPH <part.nq.gz> { ex:s ex:p 1 } } . "
				+ "GENERATE { ex:s ex:p ?y } WHERE { BIND(RAISE(\"stop\") AS ?y) } . }";
		Result result = run(query, "-b", BASE, "--graphs-to-files", "-s", "-fo", "NQUADS");
		assertEquals(result.err, 1, result.exitCode);
		assertTrue(result.err, result.err.contains("stop"));
		// a complete gzip file
		assertDataset("<http://ex/s> <http://ex/p> 1 .", new ByteArrayInputStream(
				IOUtils.toByteArray(new GZIPInputStream(Files.newInputStream(file("part.nq.gz").toPath())))), Lang.NQUADS);
	}

	// GraphTargets alone

	private GraphTargets targets() {
		return new GraphTargets(BASE + "query.rqg", folder.getRoot(), null, false);
	}

	@Test
	public void resolution() {
		GraphTargets t = targets();
		assertEquals(BASE, t.getFolderIri());
		assertNull(t.resolve(NodeFactory.createURI("http://ex/other/a.nq")));
		assertNull(t.resolve(NodeFactory.createURI(BASE + "a")));
		assertNull(t.resolve(NodeFactory.createURI(BASE + "a.nq?x=1")));
		assertNull(t.resolve(NodeFactory.createURI(BASE)));
		assertNull(t.resolve(NodeFactory.createBlankNode()));
		GraphTargets.Resolved r = t.resolve(NodeFactory.createURI(BASE + "sub/a%20b.nq.gz"));
		assertEquals(new File(folder.getRoot(), "sub/a b.nq.gz").toPath().toAbsolutePath(), r.file.getPath());
		assertEquals(Lang.NQUADS, r.file.getLang());
		assertEquals(GraphTargets.Compression.GZIP, r.file.getCompression());
		assertNull(r.graph);
		r = t.resolve(NodeFactory.createURI(BASE + "a.trig#g=http%3A%2F%2Fex%2Fg%C3%A9"));
		assertEquals(NodeFactory.createURI("http://ex/gé"), r.graph);
		assertEquals(Lang.TRIG, r.file.getLang());
		// the same file for the same path
		assertTrue(r.file == t.resolve(NodeFactory.createURI(BASE + "a.trig")).file);
	}

	@Test
	public void percentDecoding() {
		assertEquals("a b/é#%", GraphTargets.percentDecode("a%20b/%C3%A9%23%25", "x"));
		try {
			GraphTargets.percentDecode("a%2", "x");
			fail();
		} catch (TargetException ex) {
			// expected
		}
	}
}
