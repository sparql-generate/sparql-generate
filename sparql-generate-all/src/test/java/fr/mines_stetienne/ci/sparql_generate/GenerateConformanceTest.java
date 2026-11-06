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
package fr.mines_stetienne.ci.sparql_generate;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.IOUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.util.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.gson.Gson;

import fr.mines_stetienne.ci.sparql_generate.engine.PlanFactory;
import fr.mines_stetienne.ci.sparql_generate.engine.RootPlan;
import fr.mines_stetienne.ci.sparql_generate.query.SPARQLExtQuery;
import fr.mines_stetienne.ci.sparql_generate.stream.LocationMapperAccept;
import fr.mines_stetienne.ci.sparql_generate.stream.LocatorFileAccept;
import fr.mines_stetienne.ci.sparql_generate.stream.LookUpRequest;
import fr.mines_stetienne.ci.sparql_generate.stream.SPARQLExtStreamManager;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;

/**
 * Runs every test case of {@code sparql-generate-jena/src/test/resources/generate}:
 * each sub-directory holds a {@code sparql-generate-conf.json}, a query, its input
 * documents, and an {@code expected_output.ttl} that the generated graph must be
 * isomorphic to.
 *
 * The test cases live in sparql-generate-jena, but most of them need the
 * iterators and functions of the format modules, so they run here, where all
 * the modules are on the class path.
 */
@RunWith(Parameterized.class)
public class GenerateConformanceTest {

	private static final long TIMEOUT_SECONDS = 60;

	/**
	 * Test cases that do not pass yet. Each one is either an engine defect or a
	 * stale expected output; an entry is removed with the commit that resolves it.
	 * Run them anyway with {@code -Dconformance.all=true}.
	 */
	private static final Set<String> KNOWN_FAILURES = new HashSet<>(Arrays.asList(
			"cordis", "geojsontest1", "geojsontest2", "htmltest1", "jose", "list"));

	private final File dir;

	private final String name;

	public GenerateConformanceTest(String name, File dir) {
		this.name = name;
		this.dir = dir;
	}

	@Parameters(name = "{0}")
	public static Collection<Object[]> data() throws URISyntaxException {
		File root = new File(GenerateConformanceTest.class.getClassLoader().getResource("generate").toURI());
		File[] dirs = root.listFiles(File::isDirectory);
		Arrays.sort(dirs);
		List<Object[]> data = new ArrayList<>();
		for (File dir : dirs) {
			data.add(new Object[] { dir.getName(), dir });
		}
		return data;
	}

	@Test
	public void generatesExpectedOutput() throws Exception {
		assumeFalse("known failure: " + name,
				KNOWN_FAILURES.contains(name) && !Boolean.getBoolean("conformance.all"));
		SPARQLExt.init();
		FileConfigurations conf = readConfiguration(dir);
		SPARQLExtStreamManager sm = prepareStreamManager(dir, conf);

		String queryString;
		try (InputStream in = sm.open(new LookUpRequest(conf.query, SPARQLExt.MEDIA_TYPE))) {
			queryString = IOUtils.toString(in, StandardCharsets.UTF_8);
		}
		SPARQLExtQuery query = (SPARQLExtQuery) QueryFactory.create(queryString, SPARQLExt.SYNTAX);
		RootPlan plan = PlanFactory.create(query);
		Dataset dataset = conf.loadDataset(dir);

		ExecutorService executor = Executors.newSingleThreadExecutor();
		ExecutorService engineExecutor = Executors.newCachedThreadPool();
		Model output;
		try {
			Context context = ContextUtils.build().setPrefixMapping(query).setStreamManager(sm)
					.setExecutor(engineExecutor).setInputDataset(dataset).build();
			Future<Model> future = executor.submit(() -> plan.execGenerate(context));
			try {
				output = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
			} catch (TimeoutException ex) {
				future.cancel(true);
				fail("Execution did not finish within " + TIMEOUT_SECONDS + " seconds");
				return;
			}
		} finally {
			executor.shutdownNow();
			engineExecutor.shutdownNow();
		}

		Model expected = RDFDataMgr.loadModel(new File(dir, "expected_output.ttl").toURI().toString(), Lang.TTL);
		assertTrue(describeDifference(output, expected), output.isIsomorphicWith(expected));
	}

	private static FileConfigurations readConfiguration(File dir) throws Exception {
		File confFile = new File(dir, "sparql-generate-conf.json");
		FileConfigurations conf = null;
		if (confFile.exists()) {
			try (Reader reader = new InputStreamReader(confFile.toURI().toURL().openStream(),
					StandardCharsets.UTF_8)) {
				conf = new Gson().fromJson(reader, FileConfigurations.class);
			}
		}
		if (conf == null) {
			conf = new FileConfigurations();
		}
		if (conf.query == null) {
			conf.query = "query.rqg";
		}
		return conf;
	}

	/**
	 * Same mapping as the command-line interface.
	 */
	private static SPARQLExtStreamManager prepareStreamManager(File dir, FileConfigurations conf) {
		LocatorFileAccept locator = new LocatorFileAccept(dir.toURI().getPath());
		LocationMapperAccept mapper = new LocationMapperAccept();
		SPARQLExtStreamManager sm = SPARQLExtStreamManager.makeStreamManager(locator);
		sm.setLocationMapper(mapper);
		if (conf.namedqueries != null) {
			conf.namedqueries.forEach((doc) -> mapper.addAltEntry(new LookUpRequest(doc.uri, doc.mediatype),
					new LookUpRequest(doc.path)));
		}
		if (conf.documentset != null) {
			conf.documentset.forEach((doc) -> mapper.addAltEntry(new LookUpRequest(doc.uri, doc.mediatype),
					new LookUpRequest(doc.path)));
		}
		return sm;
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
		return sb.toString();
	}
}
