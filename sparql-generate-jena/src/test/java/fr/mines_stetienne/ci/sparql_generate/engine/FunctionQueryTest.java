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
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.Lang;
import org.junit.Test;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.stream.LocatorStringMap;
import fr.mines_stetienne.ci.sparql_generate.stream.SPARQLExtStreamManager;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;

/**
 * Functions defined by a FUNCTION query, looked up by their IRI.
 */
public class FunctionQueryTest {

	static {
		SPARQLExt.init();
	}

	private static Model generate(String query, String functionIri, String function) {
		LocatorStringMap locator = new LocatorStringMap();
		locator.put(functionIri, function, SPARQLExt.MEDIA_TYPE);
		SPARQLExtStreamManager sm = SPARQLExtStreamManager.makeStreamManager(locator);
		return PlanFactory.create(query).execGenerate(ContextUtils.build().setStreamManager(sm).build());
	}

	@Test
	public void functionIsCalled() {
		Model model = generate(
				"GENERATE { <http://ex.org/s> <http://ex.org/p> ?o } WHERE { BIND(<http://ex.org/twice>(\"a\") AS ?o) }",
				"http://ex.org/twice", "FUNCTION <http://ex.org/twice>(?x) { CONCAT(?x, ?x) }");
		assertTrue(EngineTestUtils.write(model),
				model.contains(ResourceFactory.createResource("http://ex.org/s"),
						ResourceFactory.createProperty("http://ex.org/p"), "aa"));
	}

	/**
	 * A function whose expression is not deterministic is evaluated at each
	 * call, not memoised.
	 */
	@Test
	public void nonDeterministicFunctionIsNotMemoised() {
		Model model = generate(
				"GENERATE { <http://ex.org/s> <http://ex.org/p> ?a, ?b } "
						+ "WHERE { BIND(<http://ex.org/fresh>(\"a\") AS ?a) BIND(<http://ex.org/fresh>(\"a\") AS ?b) }",
				"http://ex.org/fresh", "FUNCTION <http://ex.org/fresh>(?x) { STRUUID() }");
		assertEquals(EngineTestUtils.write(model), 2, model.size());
	}
}
