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
package fr.mines_stetienne.ci.sparql_generate.engine;

import static org.junit.Assert.assertTrue;

import org.apache.jena.rdf.model.Model;
import org.junit.Test;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.stream.LocatorStringMap;
import fr.mines_stetienne.ci.sparql_generate.stream.SPARQLExtStreamManager;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;

/**
 * Calls to named queries.
 */
public class NamedQueryCallTest {

	static {
		SPARQLExt.init();
	}

	private static Model generate(String query, String... iriAndQuery) {
		LocatorStringMap locator = new LocatorStringMap();
		for (int i = 0; i < iriAndQuery.length; i += 2) {
			locator.put(iriAndQuery[i], iriAndQuery[i + 1], SPARQLExt.MEDIA_TYPE);
		}
		SPARQLExtStreamManager sm = SPARQLExtStreamManager.makeStreamManager(locator);
		return PlanFactory.create(query).execGenerate(ContextUtils.build().setStreamManager(sm).build());
	}

	private static void assertIsomorphic(String expectedTurtle, Model actual) {
		Model expected = EngineTestUtils.turtle(expectedTurtle);
		assertTrue("expected:\n" + EngineTestUtils.write(expected) + "\nbut got:\n" + EngineTestUtils.write(actual),
				actual.isIsomorphicWith(expected));
	}

	/**
	 * An argument that is unbound leaves the parameter of the called SELECT
	 * query unbound.
	 */
	@Test
	public void unboundArgumentOfIteratorCall() {
		Model model = generate(
				"GENERATE { <http://ex.org/s> <http://ex.org/p> ?o } ITERATOR <http://ex.org/q>(?u) AS ?o",
				"http://ex.org/q", "SELECT <http://ex.org/q>(?x) ?o WHERE { BIND(COALESCE(?x, \"unbound\") AS ?o) }");
		assertIsomorphic("<http://ex.org/s> <http://ex.org/p> \"unbound\" .", model);
	}

	@Test
	public void unboundArgumentOfGenerateCall() {
		Model model = generate("GENERATE { GENERATE <http://ex.org/g>(?u) . } WHERE { }",
				"http://ex.org/g", "GENERATE <http://ex.org/g>(?x) { <http://ex.org/s> <http://ex.org/p> ?o } "
						+ "WHERE { BIND(COALESCE(?x, \"unbound\") AS ?o) }");
		assertIsomorphic("<http://ex.org/s> <http://ex.org/p> \"unbound\" .", model);
	}
}
