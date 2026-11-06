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
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.junit.Test;

/**
 * fun:bnode(label) returns the same blank node for the same label, across
 * the solutions of one execution, unlike BNODE(label) whose scope is one
 * solution.
 */
public class BNodeFunctionTest {

	private static final String QUERY = "PREFIX fun: <http://w3id.org/sparql-generate/fn/> "
			+ "GENERATE { ?b <http://ex/p> ?x } WHERE { VALUES (?x ?l) { (1 \"a\") (2 \"a\") (3 \"b\") } "
			+ "BIND(fun:bnode(?l) AS ?b) }";

	@Test
	public void sameLabelSameBlankNodeAcrossSolutions() {
		assertGenerates("[ <http://ex/p> 1, 2 ] . [ <http://ex/p> 3 ] .", QUERY);
	}

	@Test
	public void sameLabelAcrossIterationsOfSubQueries() {
		assertGenerates("[ <http://ex/p> 1, 2 ] .",
				"PREFIX fun: <http://w3id.org/sparql-generate/fn/> "
						+ "GENERATE { GENERATE { ?b <http://ex/p> ?x } WHERE { BIND(fun:bnode(\"a\") AS ?b) } . } "
						+ "WHERE { VALUES ?x { 1 2 } }");
	}

	@Test
	public void eachExecutionHasItsOwnBlankNodes() {
		Set<RDFNode> first = generate(QUERY).listSubjects().mapWith(r -> (RDFNode) r).toSet();
		Model second = generate(QUERY);
		for (RDFNode node : first) {
			assertTrue(!second.containsResource(node));
		}
	}
}
