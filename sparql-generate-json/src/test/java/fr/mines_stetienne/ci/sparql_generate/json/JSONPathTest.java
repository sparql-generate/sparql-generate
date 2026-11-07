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
package fr.mines_stetienne.ci.sparql_generate.json;

import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.io.StringWriter;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.Test;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.engine.PlanFactory;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;

public class JSONPathTest {

	static {
		SPARQLExt.init();
	}

	private static void assertJSONPath(String expectedObject, String json, String jsonPath) {
		Model expected = ModelFactory.createDefaultModel();
		RDFDataMgr.read(expected, new StringReader("<http://ex/s> <http://ex/p> " + expectedObject + " ."), null,
				Lang.TTL);
		// braces start embedded expressions in SPARQL-Generate literals
		json = json.replace("{", "\\{").replace("}", "\\}");
		String query = "PREFIX fun: <http://w3id.org/sparql-generate/fn/> GENERATE { <http://ex/s> <http://ex/p> ?o } "
				+ "WHERE { BIND(fun:JSONPath(\"\"\"" + json + "\"\"\", \"" + jsonPath + "\") AS ?o) }";
		Model actual = PlanFactory.create(query).execGenerate(ContextUtils.build().build());
		StringWriter sw = new StringWriter();
		RDFDataMgr.write(sw, actual, Lang.NT);
		assertTrue("got:\n" + sw, actual.isIsomorphicWith(expected));
	}

	@Test
	public void definitePath() {
		assertJSONPath("12", "{\"a\": {\"b\": 12}}", "$.a.b");
	}

	/**
	 * A path with a filter or a wildcard gives a list: when it has one element,
	 * the result is that element (issue #110).
	 */
	@Test
	public void filterWithOneMatchGivesTheValue() {
		assertJSONPath("1377422166", "[{\"country\": \"China\", \"population\": 1377422166}, {\"country\": \"India\"}]",
				"$[?(@.country=='China')].population");
	}

	@Test
	public void wildcardWithOneMatchGivesTheValue() {
		assertJSONPath("\"x\"", "{\"a\": [{\"b\": \"x\"}]}", "$.a[*].b");
	}

	@Test
	public void arrayValueIsSerialized() {
		assertJSONPath("\"[1,2]\"", "{\"a\": [1, 2]}", "$.a");
	}

	/**
	 * A definite path that selects a one-element array gives the array.
	 */
	@Test
	public void definitePathToOneElementArray() {
		assertJSONPath("\"[\\\"a\\\"]\"", "{\"tags\": [\"a\"]}", "$.tags");
	}

	@Test
	public void definitePathToOneElementArrayOfNumbers() {
		assertJSONPath("\"[5]\"", "{\"one\": [5], \"two\": [1, 2]}", "$.one");
	}

	/**
	 * iter:JSONPath gives each element as it is, even a one-element array.
	 */
	@Test
	public void iteratorKeepsOneElementArrays() {
		Model expected = ModelFactory.createDefaultModel();
		RDFDataMgr.read(expected, new StringReader("<http://ex/s> <http://ex/p> \"[\\\"x\\\"]\", \"y\" ."), null, Lang.TTL);
		String query = "PREFIX ite: <http://w3id.org/sparql-generate/iter/> GENERATE { <http://ex/s> <http://ex/p> ?o } "
				+ "ITERATOR ite:JSONPath(\"\\{\\\"items\\\": [[\\\"x\\\"], \\\"y\\\"]\\}\", \"$.items[*]\") AS ?o";
		Model actual = PlanFactory.create(query).execGenerate(ContextUtils.build().build());
		StringWriter sw = new StringWriter();
		RDFDataMgr.write(sw, actual, Lang.NT);
		assertTrue("got:\n" + sw, actual.isIsomorphicWith(expected));
	}

	/**
	 * A JSON null element gives an unbound value; the other elements are kept.
	 */
	@Test
	public void iteratorWithNullElement() {
		Model expected = ModelFactory.createDefaultModel();
		RDFDataMgr.read(expected, new StringReader("<http://ex/s> <http://ex/p> 1, 2 ."), null, Lang.TTL);
		String query = "PREFIX ite: <http://w3id.org/sparql-generate/iter/> GENERATE { <http://ex/s> <http://ex/p> ?o } "
				+ "ITERATOR ite:JSONPath(\"[1, null, 2]\", \"$[*]\") AS ?o";
		Model actual = PlanFactory.create(query).execGenerate(ContextUtils.build().build());
		StringWriter sw = new StringWriter();
		RDFDataMgr.write(sw, actual, Lang.NT);
		assertTrue("got:\n" + sw, actual.isIsomorphicWith(expected));
	}

	/**
	 * A JSON null leaves the variable unbound, and the query goes on.
	 */
	@Test
	public void nullValueIsUnbound() {
		Model expected = ModelFactory.createDefaultModel();
		RDFDataMgr.read(expected, new StringReader("<http://ex/s> <http://ex/p> 1 ."), null, Lang.TTL);
		String query = "PREFIX fun: <http://w3id.org/sparql-generate/fn/> "
				+ "GENERATE { <http://ex/s> <http://ex/p> 1 . <http://ex/s> <http://ex/q> ?o } "
				+ "WHERE { BIND(fun:JSONPath(\"\\{\\\"n\\\": null\\}\", \"$['n']\") AS ?o) }";
		Model actual = PlanFactory.create(query).execGenerate(ContextUtils.build().build());
		StringWriter sw = new StringWriter();
		RDFDataMgr.write(sw, actual, Lang.NT);
		assertTrue("got:\n" + sw, actual.isIsomorphicWith(expected));
	}

	/**
	 * iter:CBOR and fun:CBOR read the base64 encoding of a CBOR document, here
	 * {"name":"kgc","n":3}.
	 */
	@Test
	public void cbor() {
		Model expected = ModelFactory.createDefaultModel();
		RDFDataMgr.read(expected, new StringReader("<http://ex/s> <http://ex/p> \"\\\"kgc\\\"\"^^<https://www.iana.org/assignments/media-types/application/cbor> ; <http://ex/n> 3 ."), null, Lang.TTL);
		String query = "PREFIX fun: <http://w3id.org/sparql-generate/fn/> PREFIX ite: <http://w3id.org/sparql-generate/iter/> "
				+ "GENERATE { <http://ex/s> <http://ex/p> ?name ; <http://ex/n> ?n } "
				+ "ITERATOR ite:CBOR(\"omRuYW1lY2tnY2FuAw==\", \"$.name\") AS ?name "
				+ "WHERE { BIND(fun:CBOR(\"omRuYW1lY2tnY2FuAw==\", \"$.n\") AS ?n) }";
		Model actual = PlanFactory.create(query).execGenerate(ContextUtils.build().build());
		StringWriter sw = new StringWriter();
		RDFDataMgr.write(sw, actual, Lang.NT);
		assertTrue("got:\n" + sw, actual.isIsomorphicWith(expected));
	}
}
