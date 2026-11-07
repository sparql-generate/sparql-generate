/*
 * Copyright 2026 École des Mines de Saint-Étienne.
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
package fr.mines_stetienne.ci.sparql_generate.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.IOUtils;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.resultset.ResultSetLang;
import org.apache.jena.riot.ResultSetMgr;
import org.apache.jena.riot.system.StreamRDFBase;
import org.apache.jena.sparql.resultset.ResultSetCompare;
import org.apache.jena.sparql.util.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.gson.Gson;

import fr.mines_stetienne.ci.sparql_generate.FileConfigurations;
import fr.mines_stetienne.ci.sparql_generate.api.entities.Request;
import fr.mines_stetienne.ci.sparql_generate.cli.CliRequest;
import fr.mines_stetienne.ci.sparql_generate.engine.PlanFactory;
import fr.mines_stetienne.ci.sparql_generate.engine.RootPlan;
import fr.mines_stetienne.ci.sparql_generate.query.SPARQLExtQuery;
import fr.mines_stetienne.ci.sparql_generate.stream.LocatorURLAccept;
import fr.mines_stetienne.ci.sparql_generate.stream.SPARQLExtStreamManager;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;

/**
 * Runs every example of the playground ({@code src/main/resources/examples}) the
 * way the playground runs it: the example is loaded as {@link ListExercises}
 * loads it, and set up by {@link RequestSetup} as {@link TransformStream} sets it
 * up.
 *
 * <p>
 * Each example must parse, execute within the time limit of the playground, and
 * produce a non-empty output. When the example directory holds an expected
 * output, the output must match it: {@code expected_output.ttl} (isomorphic
 * graph) for GENERATE, {@code expected_output.srj} (same solutions) for SELECT,
 * {@code expected_output.txt} (same text) for TEMPLATE.
 * </p>
 *
 * <p>
 * Examples run off-line: the stream manager does not look URIs up on the Web,
 * so every document an example needs is in its documentset. The examples listed
 * in {@link #ONLINE} demonstrate a network source (HTTP polling, MQTT,
 * WebSocket); they run only with {@code -Dexamples.online=true}, and an endless
 * stream passes when it produces some output before the time limit.
 * </p>
 *
 * <p>
 * {@code -Dexamples.record=true} writes the expected output of the examples
 * that do not have one yet, to be reviewed before it is committed.
 * </p>
 */
@RunWith(Parameterized.class)
public class PlaygroundExamplesTest {

	/**
	 * The time limit, in seconds. The playground stops a query after
	 * {@link fr.mines_stetienne.ci.sparql_generate.JerseyApp#MAX_TIME} seconds
	 * (10 by default); the margin absorbs slow build machines.
	 */
	private static final long TIMEOUT_SECONDS = 30;

	/**
	 * Examples whose source is on the network.
	 */
	static final Set<String> ONLINE = new HashSet<>(Arrays.asList("generate/Example-HTTPGet",
			"generate/Example-MQTTSubscribe", "generate/Example-WebSocket"));

	/**
	 * Examples whose source is an endless stream.
	 */
	static final Set<String> ENDLESS = new HashSet<>(
			Arrays.asList("generate/Example-MQTTSubscribe", "generate/Example-WebSocket"));

	private static final String[] TYPES = { "generate", "select", "template" };

	private static final Gson GSON = new Gson();

	private final String name;

	private final File dir;

	public PlaygroundExamplesTest(String name, File dir) {
		this.name = name;
		this.dir = dir;
	}

	@Parameters(name = "{0}")
	public static Collection<Object[]> data() throws URISyntaxException {
		List<Object[]> data = new ArrayList<>();
		for (String type : TYPES) {
			File root = new File(PlaygroundExamplesTest.class.getClassLoader().getResource("examples/" + type).toURI());
			File[] dirs = root.listFiles(File::isDirectory);
			Arrays.sort(dirs);
			for (File dir : dirs) {
				data.add(new Object[] { type + "/" + dir.getName(), dir });
			}
		}
		return data;
	}

	@Test
	public void runsAsInThePlayground() throws Exception {
		boolean online = ONLINE.contains(name);
		assumeTrue("example " + name + " needs the network, run with -Dexamples.online=true",
				!online || Boolean.getBoolean("examples.online"));
		RequestSetup.init();

		assertTrue("readme.html is missing", new File(dir, "readme.html").isFile());
		Request request = load(dir);
		assertFalse("the query is empty", request.defaultquery.trim().isEmpty());

		SPARQLExtQuery query = RequestSetup.query(request);
		RootPlan plan = PlanFactory.create(query);
		Dataset dataset = RequestSetup.dataset(request, (msg, ex) -> {
			throw new AssertionError(msg, ex);
		});
		SPARQLExtStreamManager sm = RequestSetup.streamManager(request);
		if (!online) {
			new ArrayList<>(sm.locators()).stream().filter((loc) -> loc instanceof LocatorURLAccept)
					.forEach(sm::remove);
		}

		ExecutorService executor = Executors.newSingleThreadExecutor();
		ExecutorService engineExecutor = Executors.newCachedThreadPool();
		try {
			ContextUtils.Builder builder = ContextUtils.build().setStreamManager(sm)
					.setDebugTemplate(request.debugTemplate).setPrefixMapping(query.getPrefixMapping())
					.setInputDataset(dataset).setExecutor(engineExecutor);
			if (ENDLESS.contains(name)) {
				runEndless(plan, query, builder, executor);
			} else if (query.isGenerateType()) {
				checkGenerate(execute(executor, () -> plan.execGenerate(builder.build())));
			} else if (query.isSelectType()) {
				checkSelect(execute(executor, () -> ResultSetFactory.makeRewindable(plan.execSelect(builder.build()))));
			} else if (query.isTemplateType()) {
				checkTemplate(execute(executor, () -> plan.execTemplate(builder.build())));
			} else {
				fail("The query is neither a GENERATE, a SELECT, nor a TEMPLATE query");
			}
		} finally {
			executor.shutdownNow();
			engineExecutor.shutdownNow();
		}
	}

	/**
	 * Loads an example as {@link ListExercises} does.
	 */
	static Request load(File dir) throws Exception {
		FileConfigurations config;
		File confFile = new File(dir, "sparql-generate-conf.json");
		if (confFile.exists()) {
			try (InputStream in = new FileInputStream(confFile)) {
				config = GSON.fromJson(IOUtils.toString(in, StandardCharsets.UTF_8), CliRequest.class);
			}
		} else {
			config = FileConfigurations.DEFAULT;
		}
		return new Request(config, dir);
	}

	private <T> T execute(ExecutorService executor, java.util.concurrent.Callable<T> task) throws Exception {
		Future<T> future = executor.submit(task);
		try {
			return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		} catch (TimeoutException ex) {
			future.cancel(true);
			throw new AssertionError("Execution did not finish within " + TIMEOUT_SECONDS + " seconds");
		} catch (ExecutionException ex) {
			if (ex.getCause() instanceof Exception) {
				throw (Exception) ex.getCause();
			}
			throw ex;
		}
	}

	private void runEndless(RootPlan plan, SPARQLExtQuery query, ContextUtils.Builder builder,
			ExecutorService executor) throws Exception {
		assertTrue("an endless example must be a GENERATE query", query.isGenerateType());
		AtomicInteger triples = new AtomicInteger();
		Context context = builder.setGenerateOutput(new StreamRDFBase() {
			@Override
			public void triple(Triple triple) {
				triples.incrementAndGet();
			}
		}).build();
		Future<?> future = executor.submit(() -> plan.execGenerateStream(context));
		long deadline = System.currentTimeMillis() + TIMEOUT_SECONDS * 1000;
		while (triples.get() == 0 && !future.isDone() && System.currentTimeMillis() < deadline) {
			Thread.sleep(100);
		}
		future.cancel(true);
		assertTrue("the stream produced no triple within " + TIMEOUT_SECONDS + " seconds", triples.get() > 0);
	}

	private void checkGenerate(Model output) throws Exception {
		assertFalse("the generated graph is empty", output.isEmpty());
		File expectedFile = new File(dir, "expected_output.ttl");
		if (!expectedFile.exists()) {
			if (Boolean.getBoolean("examples.record")) {
				try (OutputStream out = new FileOutputStream(sourceFile(expectedFile))) {
					RDFDataMgr.write(out, output, Lang.TTL);
				}
			}
			return;
		}
		Model expected = RDFDataMgr.loadModel(expectedFile.toURI().toString(), Lang.TTL);
		assertTrue(describeDifference(output, expected), output.isIsomorphicWith(expected));
	}

	private void checkSelect(ResultSetRewindable output) throws Exception {
		assertTrue("the result set is empty", output.size() > 0);
		File expectedFile = new File(dir, "expected_output.srj");
		if (!expectedFile.exists()) {
			if (Boolean.getBoolean("examples.record")) {
				try (OutputStream out = new FileOutputStream(sourceFile(expectedFile))) {
					ResultSetFormatter.outputAsJSON(out, output);
				}
			}
			return;
		}
		ResultSetRewindable expected;
		try (InputStream in = new FileInputStream(expectedFile)) {
			expected = ResultSetFactory.makeRewindable(ResultSetMgr.read(in, ResultSetLang.RS_JSON));
		}
		output.reset();
		ByteArrayOutputStream actualText = new ByteArrayOutputStream();
		ResultSetFormatter.out(actualText, output);
		output.reset();
		assertTrue("the solutions differ from expected_output.srj, got:\n" + actualText.toString(StandardCharsets.UTF_8),
				ResultSetCompare.equalsByTerm(expected, output));
	}

	private void checkTemplate(String output) throws Exception {
		assertFalse("the generated text is empty", output.trim().isEmpty());
		File expectedFile = new File(dir, "expected_output.txt");
		if (!expectedFile.exists()) {
			if (Boolean.getBoolean("examples.record")) {
				Files.write(sourceFile(expectedFile).toPath(), output.getBytes(StandardCharsets.UTF_8));
			}
			return;
		}
		String expected = new String(Files.readAllBytes(expectedFile.toPath()), StandardCharsets.UTF_8);
		assertEquals(expected, output);
	}

	/**
	 * The file under {@code src/main/resources} that corresponds to a file of
	 * {@code target/classes}, where the examples are read from.
	 */
	private static File sourceFile(File file) {
		String path = file.getAbsolutePath().replace(File.separator + "target" + File.separator + "classes"
				+ File.separator, File.separator + "src" + File.separator + "main" + File.separator + "resources"
				+ File.separator);
		return new File(path);
	}

	private static String describeDifference(Model output, Model expected) {
		StringBuilder sb = new StringBuilder("Output is not isomorphic to expected_output.ttl");
		sb.append(" (").append(output.size()).append(" triples generated, ").append(expected.size())
				.append(" expected)");
		int max = 10;
		for (Statement s : output.difference(expected).listStatements().toList()) {
			if (max-- == 0) {
				break;
			}
			sb.append("\n  unexpected: ").append(s);
		}
		max = 10;
		for (Statement s : expected.difference(output).listStatements().toList()) {
			if (max-- == 0) {
				break;
			}
			sb.append("\n  missing:    ").append(s);
		}
		StringWriter sw = new StringWriter();
		output.write(sw, "TTL");
		sb.append("\nGenerated:\n").append(sw);
		return sb.toString();
	}
}
