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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.jena.rdf.model.Model;
import org.junit.Test;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;

/**
 * The SELECT of a GENERATE query whose WHERE clause has only BIND and FILTER
 * is evaluated without Jena's query engine: the output is the same as with
 * it.
 */
public class DirectSelectTest {

	static {
		SPARQLExt.init();
	}

	private static final String PREFIXES = "BASE <http://ex/a/> PREFIX iter: <http://w3id.org/sparql-generate/iter/> ";

	private static final String[] QUERIES = {
			// BIND, FILTER, an error in a BIND, an IRI resolved against the base
			"GENERATE { <s{?i}> <p> ?j, ?s, ?z, ?o } ITERATOR iter:for(1, 1, 5) AS ?i "
					+ "WHERE { BIND(?i * 2 AS ?j) FILTER(?j > 4) BIND(STR(?j) AS ?s) BIND(1/0 AS ?z) BIND(IRI(\"b\") AS ?o) }",
			// a FILTER in error removes the solution
			"GENERATE { <s> <p> ?i } ITERATOR iter:for(1, 1, 3) AS ?i WHERE { FILTER(?u > 1 || ?i = 2) }",
			// nested GENERATE, whose WHERE reads the variables of the parent
			"GENERATE { <s{?i}> <p> ?i . GENERATE { ?s <q> ?k , \"{?k}-{?i}\" } WHERE { BIND(?i + 10 AS ?k) FILTER(?k != 12) } . } "
					+ "ITERATOR iter:for(1, 1, 3) AS ?i WHERE { BIND(<x{?i}> AS ?s) }",
			// a BIND of a variable of the parent: left to Jena
			"GENERATE { <s> <p> ?i . GENERATE { <t> <p> ?i } WHERE { BIND(?i + 1 AS ?j) BIND(?j AS ?i) } . } "
					+ "ITERATOR iter:for(1, 1, 2) AS ?i",
			// no WHERE clause: expressions of the template only
			"GENERATE { <s{?i}> <p> \"{?i * 3}\" } ITERATOR iter:for(1, 1, 4) AS ?i",
			// blank nodes, one per solution
			"GENERATE { [] <p> ?j } ITERATOR iter:for(1, 1, 3) AS ?i WHERE { BIND(?i AS ?j) }" };

	private static Model generate(String query, boolean direct) {
		boolean before = DirectSelect.enabled;
		DirectSelect.enabled = direct;
		try {
			return EngineTestUtils.generate(PREFIXES + query);
		} finally {
			DirectSelect.enabled = before;
		}
	}

	@Test
	public void sameOutputAsJena() {
		final long before = DirectSelect.EXECUTIONS.get();
		for (String query : QUERIES) {
			Model jena = generate(query, false);
			Model direct = generate(query, true);
			assertFalse(query, jena.isEmpty());
			assertTrue(query + "\nJena:\n" + EngineTestUtils.write(jena) + "\ndirect:\n" + EngineTestUtils.write(direct),
					direct.isIsomorphicWith(jena));
		}
		assertTrue("the direct evaluation was used", DirectSelect.EXECUTIONS.get() > before + QUERIES.length);
	}
}
