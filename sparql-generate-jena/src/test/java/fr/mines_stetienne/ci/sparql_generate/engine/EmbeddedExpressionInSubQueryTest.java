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
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;

/**
 * An embedded expression in the WHERE clause of a nested GENERATE, on a
 * variable of the parent query, constrains the pattern as the variable does.
 */
public class EmbeddedExpressionInSubQueryTest {

	static {
		SPARQLExt.init();
	}

	private static final Model INPUT = EngineTestUtils.turtle("<http://ex/a> <http://ex/friend> <http://ex/b> . "
			+ "<http://ex/b> <http://ex/friend> <http://ex/c> .");

	private static void assertGenerates(String expectedTurtle, String query) {
		Model expected = EngineTestUtils.turtle(expectedTurtle);
		Model actual = PlanFactory.create(query).execGenerate(ContextUtils.build().setInputModel(INPUT).build());
		assertTrue("expected:\n" + EngineTestUtils.write(expected) + "\nbut got:\n" + EngineTestUtils.write(actual),
				actual.isIsomorphicWith(expected));
	}

	private static final String EXPECTED = "<http://ex/a> <http://ex/knows> <http://ex/b> . "
			+ "<http://ex/b> <http://ex/knows> <http://ex/c> .";

	@Test
	public void parentVariable() {
		assertGenerates(EXPECTED, "GENERATE { GENERATE { ?p <http://ex/knows> ?o } WHERE { ?p <http://ex/friend> ?o } . } "
				+ "WHERE { VALUES ?p { <http://ex/a> <http://ex/b> } }");
	}

	@Test
	public void iriTemplateOnParentVariable() {
		assertGenerates(EXPECTED, "GENERATE { GENERATE { <http://ex/{?n}> <http://ex/knows> ?o } "
				+ "WHERE { <http://ex/{?n}> <http://ex/friend> ?o } . } WHERE { VALUES ?n { \"a\" \"b\" } }");
	}

	@Test
	public void embeddedExpressionOnParentVariable() {
		assertGenerates(EXPECTED, "GENERATE { GENERATE { ?{ IRI(CONCAT(\"http://ex/\", ?n)) } <http://ex/knows> ?o } "
				+ "WHERE { ?{ IRI(CONCAT(\"http://ex/\", ?n)) } <http://ex/friend> ?o } . } WHERE { VALUES ?n { \"a\" \"b\" } }");
	}

	/**
	 * In the WHERE clause of the main query, the template is on a variable
	 * bound before, by an ITERATOR clause.
	 */
	@Test
	public void iriTemplateOnIteratorVariable() {
		assertGenerates(EXPECTED, "PREFIX ite: <http://w3id.org/sparql-generate/iter/> "
				+ "GENERATE { <http://ex/{?n}> <http://ex/knows> ?o } ITERATOR ite:Split(\"a,b\", \",\") AS ?n "
				+ "WHERE { <http://ex/{?n}> <http://ex/friend> ?o }");
	}
}
