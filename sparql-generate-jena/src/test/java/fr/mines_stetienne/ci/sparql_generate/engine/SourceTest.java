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

import static fr.mines_stetienne.ci.sparql_generate.engine.EngineTestUtils.assertGenerates;
import static org.junit.Assert.assertTrue;

import org.apache.jena.rdf.model.Model;
import org.junit.Test;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.stream.LocatorStringMap;
import fr.mines_stetienne.ci.sparql_generate.stream.SPARQLExtStreamManager;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;

/**
 * SOURCE clauses whose IRI or ACCEPT is a variable.
 */
public class SourceTest {

	/**
	 * An unbound source variable leaves the variable of the SOURCE clause
	 * unbound.
	 */
	@Test
	public void unboundSourceVariable() {
		assertGenerates("<http://ex.org/s> <http://ex.org/p> \"ok\" .",
				"GENERATE { <http://ex.org/s> <http://ex.org/p> \"ok\" . <http://ex.org/s> <http://ex.org/doc> ?d } "
						+ "SOURCE ?u AS ?d");
	}

	@Test
	public void unboundAcceptVariable() {
		assertGenerates("<http://ex.org/s> <http://ex.org/p> \"ok\" .",
				"GENERATE { <http://ex.org/s> <http://ex.org/p> \"ok\" . <http://ex.org/s> <http://ex.org/doc> ?d } "
						+ "SOURCE <http://example.invalid/doc> ACCEPT ?a AS ?d");
	}

	/**
	 * With an embedded expression, the engine copies the query by serializing
	 * it: ACCEPT must survive the copy.
	 */
	@Test
	public void acceptIriWithEmbeddedExpression() {
		LocatorStringMap locator = new LocatorStringMap();
		locator.put("http://ex.org/doc", "hello", "text/plain");
		SPARQLExtStreamManager sm = SPARQLExtStreamManager.makeStreamManager(locator);
		Model model = PlanFactory.create("GENERATE { <http://ex.org/{STR(1)}> <http://ex.org/doc> ?d } "
				+ "SOURCE <http://ex.org/doc> ACCEPT <https://www.iana.org/assignments/media-types/text/plain> AS ?d")
				.execGenerate(ContextUtils.build().setStreamManager(sm).build());
		assertTrue(EngineTestUtils.write(model), EngineTestUtils.write(model).contains("\"hello\""));
	}
}
