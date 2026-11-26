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
 * A relative IRI computed by {@code IRI()} or by an IRI with embedded
 * expressions is resolved against the base of the query, as in SPARQL 1.1,
 * wherever the expression is: binding clauses, WHERE clause, GENERATE and
 * TEMPLATE clauses, arguments of iterators and of calls. A named query
 * resolves against its own base.
 */
public class QueryBaseTest {

	static {
		SPARQLExt.init();
	}

	private static final String P = "BASE <http://ex.org/> PREFIX ex: <http://ex.org/ns#> ";

	private static ContextUtils.Builder context() {
		LocatorStringMap locator = new LocatorStringMap();
		locator.put("http://ex.org/q", P + "GENERATE <q>(?a) { ?a ex:p <in-q> . }", SPARQLExt.MEDIA_TYPE);
		locator.put("http://ex.org/t", P + "TEMPLATE <t>(?a) { STR(?a) \" \" STR(IRI(\"in-t\")) }", SPARQLExt.MEDIA_TYPE);
		locator.put("http://ex.org/s", P + "SELECT <s>(?a) ?c WHERE { BIND(STR(?a) AS ?c) }", SPARQLExt.MEDIA_TYPE);
		locator.put("http://other.org/f", "BASE <http://other.org/> FUNCTION <f>(?a) { CONCAT(STR(?a), \" \", STR(IRI(\"in-f\"))) }",
				SPARQLExt.MEDIA_TYPE);
		return ContextUtils.build().setStreamManager(SPARQLExtStreamManager.makeStreamManager(locator));
	}

	private static void assertGenerates(String expectedTurtle, String query) {
		Model expected = EngineTestUtils.turtle(P + expectedTurtle);
		Model actual = PlanFactory.create(P + query).execGenerate(context().build());
		assertTrue("expected:\n" + EngineTestUtils.write(expected) + "\nbut got:\n" + EngineTestUtils.write(actual),
				actual.isIsomorphicWith(expected));
	}

	private static String template(String query) {
		return PlanFactory.create(P + query).execTemplate(context().build());
	}

	@Test
	public void iriInBindingClause() {
		assertGenerates("<s> ex:p <rel> .", "GENERATE { <s> ex:p ?o } BIND(IRI(\"rel\") AS ?o)");
	}

	@Test
	public void iriWithEmbeddedExpressionInBindingClause() {
		assertGenerates("<s> ex:p <rel/1> .", "GENERATE { <s> ex:p ?o } BIND(1 AS ?v) BIND(<rel/{?v}> AS ?o)");
	}

	@Test
	public void iriInWhereClause() {
		assertGenerates("<s> ex:p <rel> .", "GENERATE { <s> ex:p ?o } WHERE { BIND(IRI(\"rel\") AS ?o) }");
	}

	@Test
	public void iriInIteratorArgument() {
		assertGenerates("<s> ex:p \"http://ex.org/rel/1\" .",
				"GENERATE { <s> ex:p ?o } BIND(1 AS ?v) ITERATOR <http://w3id.org/sparql-generate/iter/Split>(STR(<rel/{?v}>), \",\") AS ?o");
	}

	@Test
	public void iriInNestedGenerateBindingClause() {
		assertGenerates("<s> ex:p <rel/1> .",
				"GENERATE { GENERATE { <s> ex:p ?o } BIND(IRI(CONCAT(\"rel/\", STR(?v))) AS ?o) . } WHERE { BIND(1 AS ?v) }");
	}

	@Test
	public void iriInTemplateClause() {
		assertEquals("http://ex.org/rel/1", template("TEMPLATE { STR(<rel/{?v}>) } WHERE { BIND(1 AS ?v) }"));
	}

	@Test
	public void iriInArgumentsOfCalls() {
		assertGenerates("<line/1> ex:p <in-q> . <s> ex:c \"http://ex.org/line/1\" .",
				"GENERATE { GENERATE <q>(<line/{?v}>) . <s> ex:c ?c } BIND(1 AS ?v) ITERATOR <s>(<line/{?v}>) AS ?c");
		assertEquals("http://ex.org/line/1 http://ex.org/in-t",
				template("TEMPLATE { TEMPLATE <t>(<line/{?v}>) . } WHERE { BIND(1 AS ?v) }"));
	}

	/**
	 * The arguments are resolved against the base of the calling query, the
	 * body of the function against the base of the function.
	 */
	@Test
	public void functionHasItsOwnBase() {
		assertEquals("http://ex.org/line/1 http://other.org/in-f",
				template("TEMPLATE { <http://other.org/f>(<line/{?v}>) } WHERE { BIND(1 AS ?v) }"));
		assertGenerates("<s> ex:c \"http://ex.org/line/1 http://other.org/in-f\" .",
				"GENERATE { <s> ex:c ?{ <http://other.org/f>(<line/{?v}>) } } WHERE { BIND(1 AS ?v) }");
	}
}
