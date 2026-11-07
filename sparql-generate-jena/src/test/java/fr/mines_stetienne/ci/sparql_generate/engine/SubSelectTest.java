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
 * Sub-SELECT queries in the WHERE clause.
 */
public class SubSelectTest {

	@Test
	public void subSelect() {
		assertGenerates("<http://ex.org/a> <http://ex.org/q> 1 .",
				"GENERATE { ?s <http://ex.org/q> ?o } WHERE { { SELECT ?s ?o WHERE { VALUES (?s ?o) { (<http://ex.org/a> 1) } } } }");
	}

	/**
	 * The engine copies the query by serializing it: the copy must not print
	 * the prologue inside the sub-SELECT, which is not valid syntax.
	 */
	@Test
	public void subSelectInQueryWithPrefix() {
		assertGenerates("<http://ex.org/a> <http://ex.org/q> 1 .",
				"PREFIX : <http://ex.org/> GENERATE { ?s :q ?o } WHERE { { SELECT ?s ?o WHERE { VALUES (?s ?o) { (:a 1) } } } }");
	}

	@Test
	public void subSelectInQueryWithPrefixAndEmbeddedExpression() {
		assertGenerates("<http://ex.org/a> <http://ex.org/q1> 1 .",
				"PREFIX : <http://ex.org/> GENERATE { ?s <http://ex.org/q{STR(?o)}> ?o } "
						+ "WHERE { { SELECT ?s ?o WHERE { VALUES (?s ?o) { (:a 1) } } } }");
	}
}
