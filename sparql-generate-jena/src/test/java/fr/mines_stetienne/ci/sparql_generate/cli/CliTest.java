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
		assertTrue(out.exists());
	}

	@Test
	public void relativeOutputPathIsRelativeToCurrentDirectory() throws Exception {
		File cwd = folder.newFolder("cwd");
		Result result = runIn(cwd, QUERY, "-o", "result.ttl");
		assertEquals(result.err, 0, result.exitCode);
		assertTrue(new File(cwd, "result.ttl").exists());
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
}
