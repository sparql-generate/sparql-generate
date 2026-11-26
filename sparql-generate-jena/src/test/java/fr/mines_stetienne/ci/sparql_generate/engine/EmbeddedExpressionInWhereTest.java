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
 * An IRI or a literal with embedded expressions in a triple pattern of a
 * WHERE clause constrains the pattern, wherever the variables of its
 * expressions are bound: by another triple of the same basic graph pattern,
 * by a later element of the group, or, under OPTIONAL, by the left side.
 */
public class EmbeddedExpressionInWhereTest {

	static {
		SPARQLExt.init();
	}

	private static final String P = "BASE <http://ex/> PREFIX ex: <http://ex/ns#> ";

	private static final Model INPUT = EngineTestUtils.turtle(P + "<p/1> ex:id \"1\" ; ex:name \"one\" . "
			+ "<p/2> ex:id \"2\" ; ex:name \"two\" . <p/3> ex:id \"3\" . "
			+ "<q/1> ex:label \"label 1\" . <q/2> ex:label \"label 2\" .");

	private static void assertGenerates(String expectedTurtle, String query) {
		Model expected = EngineTestUtils.turtle(P + expectedTurtle);
		Model actual = PlanFactory.create(P + query).execGenerate(ContextUtils.build().setInputModel(INPUT).build());
		assertTrue("expected:\n" + EngineTestUtils.write(expected) + "\nbut got:\n" + EngineTestUtils.write(actual),
				actual.isIsomorphicWith(expected));
	}

	private static final String NAMES = "<out/1> ex:name \"one\" . <out/2> ex:name \"two\" .";

	@Test
	public void variableOfAnotherTripleOfThePattern() {
		assertGenerates(NAMES, "GENERATE { <out/{?i}> ex:name ?n } WHERE { ?x ex:id ?i . <p/{?i}> ex:name ?n }");
	}

	@Test
	public void variableOfALaterTripleOfThePattern() {
		assertGenerates(NAMES, "GENERATE { <out/{?i}> ex:name ?n } WHERE { <p/{?i}> ex:name ?n . ?x ex:id ?i }");
	}

	@Test
	public void literalOnAVariableOfAnotherTriple() {
		assertGenerates("<out/1> ex:label \"label 1\" . <out/2> ex:label \"label 2\" .",
				"GENERATE { <out/{?i}> ex:label ?l } WHERE { ?x ex:id ?i . ?y ex:label \"label {?i}\" . ?y ex:label ?l }");
	}

	@Test
	public void variableOfALaterElementOfTheGroup() {
		assertGenerates("<out/2> ex:name \"two\" .",
				"GENERATE { <out/{?i}> ex:name ?n } WHERE { <p/{?i}> ex:name ?n . BIND(\"2\" AS ?i) }");
	}

	@Test
	public void variableOfAnEarlierElementOfTheGroup() {
		assertGenerates(NAMES, "GENERATE { <out/{?i}> ex:name ?n } WHERE { ?x ex:id ?i . FILTER(true) <p/{?i}> ex:name ?n }");
	}

	/**
	 * Two triples on the same embedded expression, one of which binds its
	 * variable: the pattern is constrained.
	 */
	@Test
	public void sameNodeTwice() {
		assertGenerates("<out/1> ex:label \"label 1\" . <out/2> ex:label \"label 2\" .",
				"GENERATE { <out/{?i}> ex:label ?l } WHERE { ?x ex:id ?i . <q/{?i}> ex:label ?l . <q/{?i}> ex:label ?l2 }");
	}

	/**
	 * The variable of the expression is bound by the pattern itself: the node
	 * is constrained by a FILTER.
	 */
	@Test
	public void variableOfTheSameTriple() {
		assertGenerates("<p/1> ex:self \"1\" . <p/2> ex:self \"2\" . <p/3> ex:self \"3\" .",
				"GENERATE { ?x ex:self ?i } WHERE { ?x ex:id ?i . FILTER(?x = <p/{?i}>) }");
		assertGenerates("<p/1> ex:self \"1\" . <p/2> ex:self \"2\" . <p/3> ex:self \"3\" .",
				"GENERATE { <p/{?i}> ex:self ?i } WHERE { <p/{?i}> ex:id ?i }");
	}

	@Test
	public void optionalOnAVariableOfTheLeftSide() {
		assertGenerates("<out/1> ex:name \"one\" . <out/2> ex:name \"two\" . <out/3> ex:id \"3\" .",
				"GENERATE { <out/{?i}> ex:name ?n ; ex:id ?i3 } "
						+ "WHERE { ?x ex:id ?i OPTIONAL { <p/{?i}> ex:name ?n } BIND(IF(BOUND(?n), ?unbound, ?i) AS ?i3) }");
	}

	@Test
	public void optionalOnAVariableOfTheParentQuery() {
		assertGenerates("<out/1> ex:name \"one\" . <out/2> ex:name \"two\" . <out/3> ex:none \"none\" .",
				"GENERATE { GENERATE { <out/{?i}> ex:name ?n ; ex:none ?none } "
						+ "WHERE { OPTIONAL { <p/{?i}> ex:name ?n } BIND(IF(BOUND(?n), ?unbound, \"none\") AS ?none) } . } "
						+ "WHERE { ?x ex:id ?i }");
	}

	@Test
	public void optionalOnAVariableOfTheOptional() {
		assertGenerates("<out/1> ex:label \"label 1\" . <out/2> ex:label \"label 2\" .",
				"GENERATE { <out/{?i}> ex:label ?l } WHERE { ?x ex:id ?i OPTIONAL { ?x ex:id ?j . <q/{?j}> ex:label ?l } }");
	}

	/**
	 * A node whose expression is in error, or reads an unbound variable,
	 * matches nothing.
	 */
	@Test
	public void expressionInErrorMatchesNothing() {
		assertGenerates("", "GENERATE { ex:s ex:name ?n } WHERE { <p/{?unbound}> ex:name ?n }");
		assertGenerates("<out/3> ex:id \"3\" .", "GENERATE { <out/{?i}> ex:id ?i } "
				+ "WHERE { ?x ex:id ?i OPTIONAL { <p/{?i}> ex:name ?n } FILTER(!BOUND(?n)) }");
	}
}
