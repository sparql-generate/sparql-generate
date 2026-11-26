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

/**
 * A constant literal whose datatype is not a datatype of XSD keeps its
 * datatype, as STRDT gives it, in a template and in an expression. Each test
 * has its own datatype, which no parsing has registered before (Jena
 * registers a datatype when it parses a literal of that type).
 */
public class LiteralDatatypeTest {

	static {
		SPARQLExt.init();
	}

	private static final String P = "PREFIX ex: <http://ex.org/> ";

	/**
	 * The query is executed before the expected output is parsed.
	 */
	private static void assertGenerates(String expectedTurtle, String query) {
		Model actual = EngineTestUtils.generate(query);
		String nt = EngineTestUtils.write(actual);
		Model expected = EngineTestUtils.turtle(expectedTurtle);
		assertTrue("got:\n" + nt, actual.isIsomorphicWith(expected));
	}

	@Test
	public void inGenerateTemplate() {
		assertGenerates(P + "ex:s ex:p \"x\"^^ex:dt1 , \"w1\"^^ex:dt1 .",
				P + "GENERATE { ex:s ex:p \"x\"^^ex:dt1 , \"w{?n}\"^^ex:dt1 . } WHERE { BIND(1 AS ?n) }");
	}

	@Test
	public void inExpressions() {
		assertGenerates(P + "ex:s ex:p \"y\"^^ex:dt2 , \"z\"^^ex:dt2 ; ex:same true .",
				P + "GENERATE { ex:s ex:p ?b , ?{ \"z\"^^ex:dt2 } ; ex:same ?same . } "
						+ "WHERE { BIND(\"y\"^^ex:dt2 AS ?b) BIND(sameTerm(?b, STRDT(\"y\", ex:dt2)) AS ?same) }");
	}

	@Test
	public void inBindingClause() {
		assertGenerates(P + "ex:s ex:p \"y\"^^ex:dt3 .", P + "GENERATE { ex:s ex:p ?b . } BIND(\"y\"^^ex:dt3 AS ?b)");
	}

	@Test
	public void inTemplate() {
		assertEquals("http://ex.org/dt4", EngineTestUtils.template(P + "TEMPLATE { STR(DATATYPE(\"x\"^^ex:dt4)) } WHERE { }"));
	}
}
