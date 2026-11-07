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
		Process process = new ProcessBuilder(command).redirectOutput(out).redirectError(err).start();
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
}
