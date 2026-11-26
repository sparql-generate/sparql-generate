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

import static fr.mines_stetienne.ci.sparql_generate.engine.EngineTestUtils.assertGenerates;

import org.junit.Test;


import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;

/**
 * iter:SPARQL iterates over the solutions of a SELECT query evaluated on an
 * RDF document (a document read by its IRI: the conformance case iter-sparql).
 */
public class SPARQLIteratorTest {

	static {
		SPARQLExt.init();
	}

	private static final String P = "PREFIX iter: <http://w3id.org/sparql-generate/iter/> PREFIX ex: <http://ex/> ";

	private static final String FRIENDS = "<http://ex/0> <http://xmlns.com/foaf/0.1/name> \"Monica\" ; <http://xmlns.com/foaf/0.1/age> 33 . "
			+ "<http://ex/1> <http://xmlns.com/foaf/0.1/name> \"Rachel\" .";

	/**
	 * The query, in a string of SPARQL-Generate: an opening brace is written
	 * \\{, or it would start an embedded expression.
	 */
	private static final String QUERY = "PREFIX foaf: <http://xmlns.com/foaf/0.1/> "
			+ "SELECT ?person ?name ?age WHERE \\{ ?person foaf:name ?name OPTIONAL \\{ ?person foaf:age ?age } } ORDER BY ?person";

	@Test
	public void inlineDocument() {
		assertGenerates("PREFIX ex: <http://ex/> <http://ex/0> ex:name \"Monica\" ; ex:age 33 ; ex:row 0 . <http://ex/1> ex:name \"Rachel\" ; ex:row 1 .",
				P + "GENERATE { ?person ex:name ?name ; ex:age ?age ; ex:row ?row } ITERATOR iter:SPARQL(\"\"\"" + FRIENDS
						+ "\"\"\", \"" + QUERY + "\", \"person\", \"name\", \"age\") AS ?person ?name ?age ?row");
	}

	@Test
	public void projectedVariablesByDefault() {
		assertGenerates("PREFIX ex: <http://ex/> <http://ex/0> ex:name \"Monica\" . <http://ex/1> ex:name \"Rachel\" .",
				P + "GENERATE { ?person ex:name ?name } ITERATOR iter:SPARQL(\"\"\"" + FRIENDS + "\"\"\", \"" + QUERY
						+ "\") AS ?person ?name");
	}
}
