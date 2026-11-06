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
 * Expressions embedded in IRIs and literals of a GENERATE template.
 */
public class EmbeddedExpressionTest {

	@Test
	public void stringInIriIsPercentEncoded() {
		assertGenerates("<http://ex/a%20b> <http://ex/p> 1 .",
				"GENERATE { <http://ex/{?x}> <http://ex/p> 1 } WHERE { BIND(\"a b\" AS ?x) }");
	}

	/**
	 * As in literals, a value that is not a string is first converted with STR.
	 */
	@Test
	public void numberInIri() {
		assertGenerates("<http://ex/item/12> <http://ex/p> 12 .",
				"GENERATE { <http://ex/item/{?i}> <http://ex/p> ?i } WHERE { VALUES ?i { 12 } }");
	}

	@Test
	public void numberInLiteral() {
		assertGenerates("<http://ex/s> <http://ex/p> \"n12\" .",
				"GENERATE { <http://ex/s> <http://ex/p> \"n{?i}\" } WHERE { VALUES ?i { 12 } }");
	}

	@Test
	public void languageTaggedStringInIri() {
		assertGenerates("<http://ex/chat> <http://ex/p> 1 .",
				"GENERATE { <http://ex/{?x}> <http://ex/p> 1 } WHERE { BIND(\"chat\"@fr AS ?x) }");
	}

	@Test
	public void numberInIriFunction() {
		assertGenerates("<http://ex/item/12> <http://ex/p> 1 .",
				"GENERATE { ?s <http://ex/p> 1 } WHERE { VALUES ?i { 12 } BIND(<http://ex/item/{?i}> AS ?s) }");
	}
}
