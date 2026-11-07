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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Runs the command-line interface in a separate JVM, as a user does.
 */
public class CliTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	static class Result {
		int exitCode;
		String out;
		String err;
	}

	private Result run(String query, String... args) throws Exception {
		return runIn(null, query, args);
	}

	/**
	 * @param cwd the working directory of the process, or null for the
	 *            current one
	 */
	private Result runIn(File cwd, String query, String... args) throws Exception {
		File dir = folder.getRoot();
		if (query != null) {
			Files.write(new File(dir, "query.rqg").toPath(), query.getBytes(StandardCharsets.UTF_8));
		}
		File out = new File(dir, "stdout.txt");
		File err = new File(dir, "stderr.txt");
		List<String> command = new ArrayList<>(Arrays.asList(
				new File(System.getProperty("java.home"), "bin/java").getAbsolutePath(), "-cp",
				System.getProperty("java.class.path"), SPARQLExtCli.class.getName(), "-d", dir.getAbsolutePath()));
		command.addAll(Arrays.asList(args));
		ProcessBuilder builder = new ProcessBuilder(command).redirectOutput(out).redirectError(err);
		if (cwd != null) {
			builder.directory(cwd);
		}
		Process process = builder.start();
		assertTrue("the CLI did not finish", process.waitFor(60, TimeUnit.SECONDS));
		Result result = new Result();
		result.exitCode = process.exitValue();
		result.out = read(out);
		result.err = read(err);
		return result;
	}

	private static String read(File file) throws IOException {
		return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
	}

	private static final String QUERY = "GENERATE { <http://ex/s> <http://ex/p> ?o } WHERE { VALUES ?o { 1 2 } }";

	/**
	 * The standard output carries the generated RDF only, so that it can be
	 * redirected to a file.
	 */
	@Test
	public void standardOutputIsValidTurtle() throws Exception {
		Result result = run(QUERY);
		assertEquals(result.err, 0, result.exitCode);
		Model model = ModelFactory.createDefaultModel();
		RDFDataMgr.read(model, new StringReader(result.out), null, Lang.TTL);
		assertEquals(2, model.size());
	}

	/**
	 * A GENERATE query without WHERE clause is valid: no warning.
	 */
	@Test
	public void noWarningWithoutWhereClause() throws Exception {
		Result result = run("GENERATE { <http://ex/s> <http://ex/p> 1 }");
		assertEquals(result.err, 0, result.exitCode);
		assertTrue(result.err, result.err.trim().isEmpty());
		assertTrue(result.out, result.out.contains("http://ex/p"));
	}

	@Test
	public void logsGoToStandardError() throws Exception {
		Result result = run(QUERY, "-l", "DEBUG");
		assertEquals(result.err, 0, result.exitCode);
		assertTrue(result.err.contains("DEBUG"));
		assertTrue(!result.out.contains("DEBUG"));
	}

	@Test
	public void logFileOption() throws Exception {
		Result result = run(QUERY, "--log-file", new File(folder.getRoot(), "log.txt").getAbsolutePath(), "-l", "INFO");
		assertEquals(result.err, 0, result.exitCode);
		assertTrue(new File(folder.getRoot(), "log.txt").exists());
		assertTrue(result.out.contains("http://ex/p"));
	}

	@Test
	public void syntaxOptionOnValidQuery() throws Exception {
		Result result = run(QUERY, "--syntax");
		assertEquals(result.err, 0, result.exitCode);
		assertTrue(!result.out.contains("http://ex/p"));
	}

	@Test
	public void syntaxOptionOnInvalidQueryFails() throws Exception {
		Result result = run("GENERATE { <http://ex/s> <http://ex/p> }", "-x");
		assertTrue(result.exitCode != 0);
	}

	@Test
	public void invalidQueryFails() throws Exception {
		Result result = run("GENERATE { <http://ex/s> <http://ex/p> }");
		assertTrue(result.exitCode != 0);
	}

	@Test
	public void missingQueryFails() throws Exception {
		Result result = run(null);
		assertTrue(result.exitCode != 0);
	}

	private static final String MISSING_SOURCE = "BASE <http://example.org/> "
			+ "GENERATE { <http://ex/s> <http://ex/p> 1 . <http://ex/s> <http://ex/doc> ?d } SOURCE <missing.txt> AS ?d";

	/**
	 * A missing document is a warning by default.
	 */
	@Test
	public void missingDocumentIsAWarning() throws Exception {
		Result result = run(MISSING_SOURCE, "-b", "http://example.org/");
		assertEquals(result.err, 0, result.exitCode);
		assertTrue(result.err, result.err.contains("missing.txt"));
		assertTrue(result.out, result.out.contains("http://ex/p"));
	}

	@Test
	public void missingDocumentFailsWithOption() throws Exception {
		Result result = run(MISSING_SOURCE, "-b", "http://example.org/", "--fail-on-missing");
		assertEquals(result.err, 1, result.exitCode);
		assertTrue(result.err, result.err.contains("Document not found: http://example.org/missing.txt"));
	}

	/**
	 * An error during the execution gives a non-zero exit status, so that
	 * scripts and continuous integration see it.
	 */
	@Test
	public void executionErrorFails() throws Exception {
		Result result = run("GENERATE { <http://ex/s> <http://ex/p> ?d } "
				+ "SOURCE <http://example.invalid/x> ACCEPT <http://example.org/not-a-media-type> AS ?d");
		assertTrue(result.err, result.exitCode != 0);
	}

	@Test
	public void invalidOptionCombinationFails() throws Exception {
		Result result = run("SELECT * WHERE { BIND(1 AS ?x) }", "--hdt", "-o",
				new File(folder.getRoot(), "out.hdt").getAbsolutePath());
		assertTrue(result.err, result.exitCode != 0);
	}

	@Test
	public void hdtOutput() throws Exception {
		File hdtFile = new File(folder.getRoot(), "out.hdt");
		Result result = run(QUERY, "--hdt", "-o", hdtFile.getAbsolutePath());
		assertEquals(result.err, 0, result.exitCode);
		try (org.rdfhdt.hdt.hdt.HDT hdt = org.rdfhdt.hdt.hdt.HDTManager.loadHDT(hdtFile.getAbsolutePath(), null)) {
			assertEquals(2, hdt.getTriples().getNumberOfElements());
		}
	}

	private static final String BIND_QUERY = "PREFIX ex: <http://ex/> GENERATE { ex:s ex:p ?x } WHERE { }";

	private Model bind(String value) throws Exception {
		Result result = run(BIND_QUERY, "-B", "x=" + value);
		assertEquals(result.err, 0, result.exitCode);
		Model model = ModelFactory.createDefaultModel();
		RDFDataMgr.read(model, new StringReader(result.out), null, Lang.TTL);
		assertEquals(result.out, 1, model.size());
		return model;
	}

	/**
	 * -B binds a variable to an RDF term: a number, a literal, an IRI or a
	 * prefixed name of the query.
	 */
	@Test
	public void bindInteger() throws Exception {
		assertEquals(42, bind("42").listObjects().next().asLiteral().getInt());
	}

	@Test
	public void bindLanguageTaggedString() throws Exception {
		assertEquals("en", bind("\"a string with spaces\"@en").listObjects().next().asLiteral().getLanguage());
	}

	@Test
	public void bindIri() throws Exception {
		assertEquals("http://ex/o", bind("<http://ex/o>").listObjects().next().asResource().getURI());
	}

	@Test
	public void bindPrefixedName() throws Exception {
		assertEquals("http://ex/o", bind("ex:o").listObjects().next().asResource().getURI());
	}

	/**
	 * -o is resolved against the current directory, not against -d; an absolute
	 * path stays absolute.
	 */
	@Test
	public void absoluteOutputPath() throws Exception {
		File out = new File(folder.newFolder("elsewhere"), "result.ttl");
		Result result = run(QUERY, "-o", out.getAbsolutePath());
		assertEquals(result.err, 0, result.exitCode);
		assertEquals(2, RDFDataMgr.loadModel(out.getAbsolutePath(), Lang.TTL).size());
	}

	@Test
	public void streamedOutputFile() throws Exception {
		File out = new File(folder.getRoot(), "result.ttl");
		Result result = run(QUERY, "-s", "-o", out.getAbsolutePath());
		assertEquals(result.err, 0, result.exitCode);
		assertEquals(2, RDFDataMgr.loadModel(out.getAbsolutePath(), Lang.TTL).size());
	}

	/**
	 * With -fo NT, the stream is N-Triples: full IRIs, no prefix. A stream
	 * may repeat a triple, here once per solution.
	 */
	@Test
	public void streamedNTriples() throws Exception {
		File out = new File(folder.getRoot(), "result.nt");
		Result result = run("PREFIX ex: <http://ex/> GENERATE { ex:s ex:p ?o, \"a\" } WHERE { VALUES ?o { 1 2 } }",
				"-s", "-fo", "NT", "-o", out.getAbsolutePath());
		assertEquals(result.err, 0, result.exitCode);
		List<String> lines = Files.readAllLines(out.toPath());
		assertEquals(lines.toString(), 4, lines.size());
		for (String line : lines) {
			assertTrue(line, line.startsWith("<http://ex/s> <http://ex/p> ") && line.endsWith(" ."));
		}
		assertEquals(3, RDFDataMgr.loadModel(out.getAbsolutePath(), Lang.NT).size());
	}

	@Test
	public void streamedNTriplesToStandardOutput() throws Exception {
		Result result = run(QUERY, "-s", "-fo", "NT");
		assertEquals(result.err, 0, result.exitCode);
		Model model = ModelFactory.createDefaultModel();
		RDFDataMgr.read(model, new StringReader(result.out), null, Lang.NT);
		assertEquals(2, model.size());
	}

	@Test
	public void selectStreamedOutputFile() throws Exception {
		File out = new File(folder.getRoot(), "result.csv");
		Result result = run("SELECT ?x WHERE { VALUES ?x { 1 2 } }", "-s", "-fo", "CSV", "-o", out.getAbsolutePath());
		assertEquals(result.err, 0, result.exitCode);
		assertEquals(3, Files.readAllLines(out.toPath()).size());
	}

	/**
	 * A limit reached gives exit status 1 and says that the output is
	 * truncated: no partial output without a word.
	 */
	@Test
	public void maxTriplesReached() throws Exception {
		Result result = run(QUERY, "--max-triples", "1", "-s");
		assertEquals(result.err, 1, result.exitCode);
		assertTrue(result.err, result.err.contains("Limit of 1 triples reached, output truncated."));
	}

	@Test
	public void timeLimitReached() throws Exception {
		File out = new File(folder.getRoot(), "big.nt");
		Result result = run("PREFIX iter: <http://w3id.org/sparql-generate/iter/> "
				+ "GENERATE { GENERATE { <http://ex/s> <http://ex/p> ?k } ITERATOR iter:for(0, 1, 5000) AS ?j "
				+ "WHERE { BIND(?i * ?j AS ?k) } . } ITERATOR iter:for(0, 1, 5000) AS ?i",
				"--time-limit", "1", "-s", "-fo", "NT", "-o", out.getAbsolutePath());
		assertEquals(result.err, 1, result.exitCode);
		assertTrue(result.err, result.err.contains("Time limit of 1 s reached, output truncated."));
	}

	@Test
	public void selectOutputFile() throws Exception {
		File out = new File(folder.getRoot(), "result.csv");
		Result result = run("SELECT ?x WHERE { VALUES ?x { 1 2 } }", "-fo", "CSV", "-o", out.getAbsolutePath());
		assertEquals(result.err, 0, result.exitCode);
		assertEquals(3, Files.readAllLines(out.toPath()).size());
	}

	@Test
	public void relativeOutputPathIsRelativeToCurrentDirectory() throws Exception {
		File cwd = folder.newFolder("cwd");
		Result result = runIn(cwd, QUERY, "-o", "result.ttl");
		assertEquals(result.err, 0, result.exitCode);
		assertEquals(2, RDFDataMgr.loadModel(new File(cwd, "result.ttl").getAbsolutePath(), Lang.TTL).size());
	}

	/**
	 * PERFORM queries are parsed but cannot be executed: say so.
	 */
	@Test
	public void performQueryIsNotSupported() throws Exception {
		Result result = run("PERFORM { } WHERE { }");
		assertTrue(result.exitCode != 0);
		assertTrue(result.err, result.err.contains("PERFORM queries are not supported"));
	}

	private Result runWithConf(String conf) throws Exception {
		Files.write(new File(folder.getRoot(), "sparql-generate-conf.json").toPath(),
				conf.getBytes(StandardCharsets.UTF_8));
		return run(QUERY);
	}

	/**
	 * Without -l nor configuration, the log level is WARN.
	 */
	@Test
	public void defaultLogLevelIsWarn() throws Exception {
		Result result = run(QUERY);
		assertTrue(result.err, !result.err.contains(" INFO ") && !result.err.contains(" DEBUG "));
	}

	/**
	 * The log level of sparql-generate-conf.json is "log", as documented, or
	 * "loglevel", as in former versions.
	 */
	@Test
	public void logLevelFromConfigurationLog() throws Exception {
		assertTrue(runWithConf("{ \"log\": 4 }").err.contains(" DEBUG "));
	}

	@Test
	public void logLevelFromConfigurationLogAsString() throws Exception {
		assertTrue(runWithConf("{ \"log\": \"4\" }").err.contains(" DEBUG "));
	}

	@Test
	public void logLevelFromConfigurationLoglevel() throws Exception {
		assertTrue(runWithConf("{ \"loglevel\": 4 }").err.contains(" DEBUG "));
	}
}
