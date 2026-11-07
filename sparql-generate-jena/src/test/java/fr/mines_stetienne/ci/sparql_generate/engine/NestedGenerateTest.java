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
import static org.junit.Assert.assertTrue;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResourceFactory;

import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;

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

	/**
	 * A binding clause BIND or SOURCE rebinds a variable already bound, as
	 * ITERATOR does: the last clause wins.
	 */
	@Test
	public void bindRebindsVariable() {
		assertGenerates("<http://ex/s> <http://ex/p> 5 .",
				"PREFIX ite: <http://w3id.org/sparql-generate/iter/> "
						+ "GENERATE { <http://ex/s> <http://ex/p> ?x } ITERATOR ite:for(1, 1, 2) AS ?x BIND(5 AS ?x)");
	}

	@Test
	public void bindCanUseThePreviousValue() {
		assertGenerates("<http://ex/s> <http://ex/p> 2 .",
				"PREFIX ite: <http://w3id.org/sparql-generate/iter/> "
						+ "GENERATE { <http://ex/s> <http://ex/p> ?x } ITERATOR ite:for(1, 1, 2) AS ?x BIND(?x + 1 AS ?x)");
	}

	@Test
	public void subQueryValuesWithSeveralVariables() {
		assertGenerates("<http://ex/1> <http://ex/a> <http://ex/b> .",
				"GENERATE { GENERATE { ?s ?p ?o } WHERE { VALUES (?p ?o) { (<http://ex/a> <http://ex/b>) } } . } "
						+ "WHERE { VALUES ?s { <http://ex/1> } }");
	}

	/**
	 * A blank node at the end of a property path, in the WHERE clause of a
	 * sub-query, survives the rewriting of a query with embedded expressions.
	 */
	@Test
	public void blankNodeInPropertyPathOfSubQuery() {
		Model input = EngineTestUtils.turtle("<http://ex/a> <http://ex/p> [] . <http://ex/b> <http://ex/r> 1 .");
		Model model = PlanFactory
				.create("GENERATE { <http://ex/{STR(?i)}> <http://ex/q> ?i . "
						+ "GENERATE { ?s <http://ex/q> ?i } WHERE { ?s <http://ex/p>|<http://ex/r> [] "
						+ "FILTER NOT EXISTS { ?s <http://ex/r> [] } } . } WHERE { VALUES ?i { 1 } }")
				.execGenerate(ContextUtils.build().setInputModel(input).build());
		Model expected = EngineTestUtils.turtle("<http://ex/1> <http://ex/q> 1 . <http://ex/a> <http://ex/q> 1 .");
		assertTrue(EngineTestUtils.write(model), model.isIsomorphicWith(expected));
	}

	/**
	 * An iterator of a sub-query runs once per solution of the parent, equal
	 * solutions included, and the rows of all the runs form one batch.
	 */
	@Test
	public void iteratorOfSubQueryRunsOncePerParentSolution() {
		String result = EngineTestUtils.template("PREFIX iter: <http://w3id.org/sparql-generate/iter/> "
				+ "TEMPLATE { TEMPLATE { ?x } ITERATOR iter:for(1, 1, 3) AS ?x . } WHERE { VALUES ?a { 1 1 2 } }");
		assertEquals("121212", result);
	}

	/**
	 * Consecutive BIND clauses read the values of the previous ones; an error
	 * leaves a variable unbound.
	 */
	@Test
	public void consecutiveBindClauses() {
		assertGenerates("<http://ex/s> <http://ex/p> 2, 3, 30, 4, 5, 50 .",
				"PREFIX iter: <http://w3id.org/sparql-generate/iter/> "
						+ "GENERATE { <http://ex/s> <http://ex/p> ?a, ?b, ?c, ?d } ITERATOR iter:for(1, 1, 3) AS ?i "
						+ "BIND(?i * 2 AS ?a) BIND(?a + 1 AS ?b) BIND(1/0 AS ?c) BIND(COALESCE(?c, ?b * 10) AS ?d)");
	}

	@Test
	public void consecutiveBindClausesRebindingAVariable() {
		assertGenerates("<http://ex/s> <http://ex/p> 2, 3 . <http://ex/t> <http://ex/p> 12, 13 .",
				"PREFIX iter: <http://w3id.org/sparql-generate/iter/> "
						+ "GENERATE { <http://ex/s> <http://ex/p> ?a . <http://ex/t> <http://ex/p> ?b } "
						+ "ITERATOR iter:for(1, 1, 3) AS ?i BIND(?i AS ?a) BIND(?a + 10 AS ?b) BIND(?a + 1 AS ?a) BIND(?b + 1 AS ?b)");
	}

}
