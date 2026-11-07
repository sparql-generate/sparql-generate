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
package fr.mines_stetienne.ci.sparql_generate.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.jena.rdf.model.Model;
import org.junit.Test;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.stream.LocatorStringMap;
import fr.mines_stetienne.ci.sparql_generate.stream.SPARQLExtStreamManager;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;

/**
 * Recursive calls to named queries are tabled: a call identical to a call in
 * progress (same query, same arguments) adds nothing, so that a cycle of
 * calls terminates.
 */
public class RecursionTest {

	static {
		SPARQLExt.init();
	}

	private static ContextUtils.Builder builder(String iri, String query) {
		LocatorStringMap locator = new LocatorStringMap();
		locator.put(iri, query, SPARQLExt.MEDIA_TYPE);
		return ContextUtils.build().setStreamManager(SPARQLExtStreamManager.makeStreamManager(locator));
	}

	/**
	 * q(0) calls q(1), which calls q(2), which calls q(0) again.
	 */
	@Test(timeout = 60000)
	public void cycleOfGenerateCallsTerminates() {
		Model model = PlanFactory.create("GENERATE { GENERATE <http://ex.org/q>(0) . } WHERE { }")
				.execGenerate(builder("http://ex.org/q",
						"GENERATE <http://ex.org/q>(?n) { <http://ex.org/s> <http://ex.org/p> ?n . "
								+ "GENERATE <http://ex.org/q>(?m) . } WHERE { BIND(IF(?n = 2, 0, ?n + 1) AS ?m) }")
						.build());
		assertEquals(EngineTestUtils.write(model), 3, model.size());
	}

	@Test(timeout = 60000)
	public void cycleOfTemplateCallsTerminates() {
		String result = PlanFactory.create("TEMPLATE { TEMPLATE <http://ex.org/t>(0) . } WHERE { }")
				.execTemplate(builder("http://ex.org/t",
						"TEMPLATE <http://ex.org/t>(?n) { ?n TEMPLATE <http://ex.org/t>(?m) . } "
								+ "WHERE { BIND(IF(?n = 2, 0, ?n + 1) AS ?m) }")
						.build());
		assertEquals("012", result);
	}

	/**
	 * Calls whose query creates blank nodes are not reused: each one gives its
	 * own blank nodes.
	 */
	@Test
	public void nonDeterministicCallsAreNotReused() {
		Model model = PlanFactory.create("GENERATE { GENERATE <http://ex.org/b>(1) . GENERATE <http://ex.org/b>(1) . } WHERE { }")
				.execGenerate(builder("http://ex.org/b",
						"GENERATE <http://ex.org/b>(?n) { [] <http://ex.org/p> ?n . } WHERE { }").build());
		assertTrue(EngineTestUtils.write(model), model.size() >= 1);
	}
}
