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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.jena.rdf.model.Model;
import org.junit.Test;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.stream.LocatorStringMap;
import fr.mines_stetienne.ci.sparql_generate.stream.SPARQLExtStreamManager;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;

/**
 * The arguments of a call of a named query or function may be IRIs and
 * literals with embedded expressions, or embedded expressions: the called
 * query receives their values.
 */
public class CallArgumentTest {

	static {
		SPARQLExt.init();
	}

	private static final String P = "PREFIX ex: <http://ex.org/> ";

	/** A named GENERATE, TEMPLATE, SELECT and FUNCTION of two parameters. */
	private static final String[] NAMED = {
			"http://ex.org/q", P + "GENERATE ex:q(?a, ?b) { ?a ex:p ?b . }",
			"http://ex.org/t", P + "TEMPLATE ex:t(?a, ?b) { \"[\" STR(?a) \" \" STR(?b) \"]\" }",
			"http://ex.org/s", P + "SELECT ex:s(?a, ?b) ?c WHERE { BIND(CONCAT(STR(?a), \" \", STR(?b)) AS ?c) }",
			"http://ex.org/f", P + "FUNCTION ex:f(?a, ?b) { CONCAT(STR(?a), \" \", STR(?b)) }" };

	private static ContextUtils.Builder context() {
		LocatorStringMap locator = new LocatorStringMap();
		for (int i = 0; i < NAMED.length; i += 2) {
			locator.put(NAMED[i], NAMED[i + 1], SPARQLExt.MEDIA_TYPE);
		}
		return ContextUtils.build().setStreamManager(SPARQLExtStreamManager.makeStreamManager(locator));
	}

	private static Model generate(String query) {
		return PlanFactory.create(P + query).execGenerate(context().build());
	}

	private static String template(String query) {
		return PlanFactory.create(P + query).execTemplate(context().build());
	}

	private static void assertGenerates(String expectedTurtle, String query) {
		Model expected = EngineTestUtils.turtle(P + expectedTurtle);
		Model actual = generate(query);
		assertTrue("expected:\n" + EngineTestUtils.write(expected) + "\nbut got:\n" + EngineTestUtils.write(actual),
				actual.isIsomorphicWith(expected));
	}

	@Test
	public void nestedGenerateCall() {
		assertGenerates("<http://ex.org/line/1> ex:p \"x1\" . <http://ex.org/line/2> ex:p \"x2\" .",
				"GENERATE { GENERATE ex:q(<http://ex.org/line/{?v}>, \"x{?v}\") . } WHERE { VALUES ?v { 1 2 } }");
	}

	@Test
	public void nestedGenerateCallWithExpressionArgument() {
		assertGenerates("<http://ex.org/line/1> ex:p 2 . <http://ex.org/line/2> ex:p 3 .",
				"GENERATE { GENERATE ex:q(<http://ex.org/line/{?v}>, ?v + 1) . } WHERE { VALUES ?v { 1 2 } }");
	}

	@Test
	public void nestedGenerateCallInGraphBlock() {
		Model model = PlanFactory.create(P + "GENERATE { GRAPH ex:g { GENERATE ex:q(<http://ex.org/line/{?v}>, ?v) . } } "
				+ "WHERE { VALUES ?v { 1 } }").execGenerateDataset(context().build()).getNamedModel("http://ex.org/g");
		assertTrue(EngineTestUtils.write(model), model.isIsomorphicWith(
				EngineTestUtils.turtle(P + "<http://ex.org/line/1> ex:p 1 .")));
	}

	/**
	 * In a GENERATE nested in a GENERATE nested in the query.
	 */
	@Test
	public void callInNestedGenerate() {
		assertGenerates("<http://ex.org/line/1> ex:p \"x1\" . <http://ex.org/line/2> ex:p \"x2\" .",
				"GENERATE { GENERATE { GENERATE ex:q(<http://ex.org/line/{?v}>, \"x{?v}\") . } "
						+ "WHERE { VALUES ?v { 1 2 } } . } WHERE { }");
	}

	@Test
	public void iteratorCall() {
		assertGenerates("ex:s ex:c \"http://ex.org/line/1 x1\" .",
				"GENERATE { ex:s ex:c ?c } BIND(1 AS ?v) ITERATOR ex:s(<http://ex.org/line/{?v}>, \"x{?v}\") AS ?c");
	}

	@Test
	public void iteratorCallInNestedGenerate() {
		assertGenerates("ex:s ex:c \"http://ex.org/line/1 x1\" .",
				"GENERATE { GENERATE { ex:s ex:c ?c } ITERATOR ex:s(<http://ex.org/line/{?v}>, \"x{?v}\") AS ?c . } "
						+ "WHERE { BIND(1 AS ?v) }");
	}

	@Test
	public void functionCallInGenerate() {
		assertGenerates("ex:s ex:c \"http://ex.org/line/1 x1\" .",
				"GENERATE { ex:s ex:c ?{ ex:f(<http://ex.org/line/{?v}>, \"x{?v}\") } } WHERE { BIND(1 AS ?v) }");
	}

	@Test
	public void templateCall() {
		assertEquals("[http://ex.org/line/1 x1][http://ex.org/line/2 x2]",
				template("TEMPLATE { TEMPLATE ex:t(<http://ex.org/line/{?v}>, \"x{?v}\") . } WHERE { VALUES ?v { 1 2 } }"));
	}

	@Test
	public void functionCallInTemplate() {
		assertEquals("http://ex.org/line/1 x1",
				template("TEMPLATE { ex:f(<http://ex.org/line/{?v}>, \"x{?v}\") } WHERE { BIND(1 AS ?v) }"));
	}
}
