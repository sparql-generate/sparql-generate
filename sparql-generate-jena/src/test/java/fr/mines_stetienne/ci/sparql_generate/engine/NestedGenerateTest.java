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
import static fr.mines_stetienne.ci.sparql_generate.engine.EngineTestUtils.generate;
import static org.junit.Assert.assertEquals;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResourceFactory;

import org.junit.Test;

/**
 * Nested GENERATE queries: each solution of the outer query is passed to the
 * sub-query, whose own WHERE clause is joined with it.
 */
public class NestedGenerateTest {

	@Test
	public void subQueryValuesAreJoinedWithOuterSolutions() {
		assertGenerates("<http://ex/1> <http://ex/q> 1, 2 . <http://ex/2> <http://ex/q> 1, 2 .",
				"GENERATE { GENERATE { ?s <http://ex/q> ?j } WHERE { VALUES ?j { 1 2 } } . } "
						+ "WHERE { VALUES ?s { <http://ex/1> <http://ex/2> } }");
	}

	/**
	 * A blank node label used in a GENERATE and in its sub-GENERATE denotes the
	 * same blank node, one per solution of the outer query.
	 */
	@Test
	public void blankNodeIsSharedWithSubQuery() {
		assertGenerates("[ <http://ex/p> 1 ; <http://ex/q> 1, 2 ] . [ <http://ex/p> 2 ; <http://ex/q> 1, 2 ] .",
				"GENERATE { _:b <http://ex/p> ?i . GENERATE { _:b <http://ex/q> ?j } WHERE { VALUES ?j { 1 2 } } . } "
						+ "WHERE { VALUES ?i { 1 2 } }");
	}

	/**
	 * Same with embedded expressions, which make the engine rewrite the query.
	 */
	@Test
	public void blankNodeIsSharedWithSubQueryWithEmbeddedExpressions() {
		assertGenerates(
				"[ <http://ex/p1> 1 ; <http://ex/q11> 1 ; <http://ex/q12> 2 ] . "
						+ "[ <http://ex/p2> 2 ; <http://ex/q21> 1 ; <http://ex/q22> 2 ] .",
				"GENERATE { _:b <http://ex/p{STR(?i)}> ?i . "
						+ "GENERATE { _:b <http://ex/q{STR(?i)}{STR(?j)}> ?j } WHERE { VALUES ?j { 1 2 } } . } "
						+ "WHERE { VALUES ?i { 1 2 } }");
	}

	/**
	 * A blank node of the outer query used two levels below.
	 */
	@Test
	public void blankNodeIsSharedWithSubSubQuery() {
		assertGenerates("[ <http://ex/p> 1 ; <http://ex/r> 3 ] . [ <http://ex/p> 2 ; <http://ex/r> 3 ] .",
				"GENERATE { _:b <http://ex/p> ?i . "
						+ "GENERATE { GENERATE { _:b <http://ex/r> ?k } WHERE { VALUES ?k { 3 } } . } . } "
						+ "WHERE { VALUES ?i { 1 2 } }");
	}

	/**
	 * A blank node label that only appears in the sub-query is fresh for each
	 * solution of the sub-query.
	 */
	@Test
	public void blankNodeOfSubQueryIsFreshPerSolution() {
		Model model = generate("GENERATE { GENERATE { _:c <http://ex/q> ?j } WHERE { VALUES ?j { 1 2 } } . } "
				+ "WHERE { VALUES ?i { 1 2 } }");
		assertEquals(4, model.listSubjectsWithProperty(ResourceFactory.createProperty("http://ex/q")).toList().size());
	}

	/**
	 * A variable of an ITERATOR clause of a sub-query that is already bound by
	 * the outer query is bound anew by the iterator, as in nested loops.
	 */
	@Test
	public void iteratorOfSubQueryRebindsOuterVariable() {
		assertGenerates("<http://ex/a> <http://ex/p> 0, 1 . <http://ex/b> <http://ex/p> 0, 1 .",
				"PREFIX ite: <http://w3id.org/sparql-generate/iter/> "
						+ "GENERATE { GENERATE { ?s <http://ex/p> ?i } ITERATOR ite:for(0, 1, 2) AS ?i . } "
						+ "WHERE { VALUES (?s ?i) { (<http://ex/a> 7) (<http://ex/b> 8) } }");
	}

	@Test
	public void subQueryValuesWithSeveralVariables() {
		assertGenerates("<http://ex/1> <http://ex/a> <http://ex/b> .",
				"GENERATE { GENERATE { ?s ?p ?o } WHERE { VALUES (?p ?o) { (<http://ex/a> <http://ex/b>) } } . } "
						+ "WHERE { VALUES ?s { <http://ex/1> } }");
	}

}
