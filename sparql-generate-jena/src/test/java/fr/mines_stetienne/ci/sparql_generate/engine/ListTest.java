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

import org.junit.Test;

/**
 * LIST(?x) in a GENERATE template.
 */
public class ListTest {

	/**
	 * Without GROUP BY, the list has the values of all the solutions.
	 */
	@Test
	public void listOfAllSolutions() {
		assertGenerates("<http://ex/s/1> <http://ex/p> _:l . <http://ex/s/2> <http://ex/p> _:l . _:l <http://www.w3.org/1999/02/22-rdf-syntax-ns#first> \"a\" ; <http://www.w3.org/1999/02/22-rdf-syntax-ns#rest> ( \"b\" ) .",
				"GENERATE { <http://ex/s/{?g}> <http://ex/p> LIST(?x) } WHERE { VALUES (?g ?x) { (1 \"a\") (2 \"b\") } }");
	}

	/**
	 * With GROUP BY, one list per group, as an aggregate.
	 */
	@Test
	public void listPerGroup() {
		assertGenerates("<http://ex/s/1> <http://ex/p> ( \"a\" \"b\" ) . <http://ex/s/2> <http://ex/p> ( \"c\" ) .",
				"GENERATE { <http://ex/s/{?g}> <http://ex/p> LIST(?x) } "
						+ "WHERE { VALUES (?g ?x) { (1 \"a\") (1 \"b\") (2 \"c\") } } GROUP BY ?g");
	}

	/**
	 * A nested GENERATE is evaluated on the batch of the solutions of its
	 * parent: GROUP BY gives one list per solution of the parent.
	 */
	@Test
	public void listPerParentSolutionWithGroupBy() {
		assertGenerates("<http://ex/s/1> <http://ex/p> ( \"a\" \"b\" ) . <http://ex/s/2> <http://ex/p> ( \"c\" ) .",
				"GENERATE { GENERATE { <http://ex/s/{?g}> <http://ex/p> LIST(?x) } "
						+ "WHERE { VALUES (?h ?x) { (1 \"a\") (1 \"b\") (2 \"c\") } FILTER(?h = ?g) } GROUP BY ?g . } "
						+ "WHERE { VALUES ?g { 1 2 } }");
	}
}
