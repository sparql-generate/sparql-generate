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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.junit.Test;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.stream.LocatorStringMap;
import fr.mines_stetienne.ci.sparql_generate.stream.SPARQLExtStreamManager;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;

/**
 * Calls to named queries.
 */
public class NamedQueryCallTest {

	static {
		SPARQLExt.init();
	}

	private static Model generate(String query, String... iriAndQuery) {
		LocatorStringMap locator = new LocatorStringMap();
		for (int i = 0; i < iriAndQuery.length; i += 2) {
			locator.put(iriAndQuery[i], iriAndQuery[i + 1], SPARQLExt.MEDIA_TYPE);
		}
		SPARQLExtStreamManager sm = SPARQLExtStreamManager.makeStreamManager(locator);
		return PlanFactory.create(query).execGenerate(ContextUtils.build().setStreamManager(sm).build());
	}

	private static void assertIsomorphic(String expectedTurtle, Model actual) {
		Model expected = EngineTestUtils.turtle(expectedTurtle);
		assertTrue("expected:\n" + EngineTestUtils.write(expected) + "\nbut got:\n" + EngineTestUtils.write(actual),
				actual.isIsomorphicWith(expected));
	}

	/**
	 * An argument that is unbound leaves the parameter of the called SELECT
	 * query unbound.
	 */
	@Test
	public void unboundArgumentOfIteratorCall() {
		Model model = generate(
				"GENERATE { <http://ex.org/s> <http://ex.org/p> ?o } ITERATOR <http://ex.org/q>(?u) AS ?o",
				"http://ex.org/q", "SELECT <http://ex.org/q>(?x) ?o WHERE { BIND(COALESCE(?x, \"unbound\") AS ?o) }");
		assertIsomorphic("<http://ex.org/s> <http://ex.org/p> \"unbound\" .", model);
	}

	/**
	 * The input dataset has the named graphs g1 and g2.
	 */
	private static Model generateWithGraphs(String query, String... iriAndQuery) {
		LocatorStringMap locator = new LocatorStringMap();
		for (int i = 0; i < iriAndQuery.length; i += 2) {
			locator.put(iriAndQuery[i], iriAndQuery[i + 1], SPARQLExt.MEDIA_TYPE);
		}
		SPARQLExtStreamManager sm = SPARQLExtStreamManager.makeStreamManager(locator);
		Dataset dataset = DatasetFactory.create();
		dataset.addNamedModel("http://ex.org/g1", EngineTestUtils.turtle("<http://ex.org/s1> <http://ex.org/p> 1 ."));
		dataset.addNamedModel("http://ex.org/g2", EngineTestUtils.turtle("<http://ex.org/s2> <http://ex.org/p> 2 ."));
		return PlanFactory.create(query)
				.execGenerate(ContextUtils.build().setStreamManager(sm).setInputDataset(dataset).build());
	}

	/**
	 * A query called on several rows, whose FROM clause depends on its
	 * parameter, is evaluated on the dataset of each row.
	 */
	@Test
	public void fromClauseOfEachCall() {
		Model model = generateWithGraphs(
				"GENERATE { GENERATE <http://ex.org/q>(?n) . } WHERE { VALUES ?n { 1 2 } }",
				"http://ex.org/q", "GENERATE <http://ex.org/q>(?n) { ?s <http://ex.org/q> ?n } "
						+ "FROM <http://ex.org/g{?n}> WHERE { ?s ?p ?o }");
		assertIsomorphic("<http://ex.org/s1> <http://ex.org/q> 1 . <http://ex.org/s2> <http://ex.org/q> 2 .", model);
	}

	@Test
	public void fromNamedClauseOfEachCall() {
		Model model = generateWithGraphs(
				"GENERATE { GENERATE <http://ex.org/q>(?n) . } WHERE { VALUES ?n { 1 2 } }",
				"http://ex.org/q", "GENERATE <http://ex.org/q>(?n) { ?s <http://ex.org/in> ?g } "
						+ "FROM NAMED <http://ex.org/g{?n}> WHERE { GRAPH ?g { ?s ?p ?o } }");
		assertIsomorphic("<http://ex.org/s1> <http://ex.org/in> <http://ex.org/g1> . "
				+ "<http://ex.org/s2> <http://ex.org/in> <http://ex.org/g2> .", model);
	}

	@Test
	public void fromGenerateClauseOfEachCall() {
		Model model = generateWithGraphs(
				"GENERATE { GENERATE <http://ex.org/q>(?n) . } WHERE { VALUES ?n { 1 2 } }",
				"http://ex.org/q", "GENERATE <http://ex.org/q>(?n) { ?s <http://ex.org/q> ?n } "
						+ "FROM GENERATE { <http://ex.org/s{?n}> <http://ex.org/p> 0 } . WHERE { ?s ?p ?o }");
		assertIsomorphic("<http://ex.org/s1> <http://ex.org/q> 1 . <http://ex.org/s2> <http://ex.org/q> 2 .", model);
	}

	@Test
	public void fromClauseOfEachTemplateCall() {
		LocatorStringMap locator = new LocatorStringMap();
		locator.put("http://ex.org/t", "TEMPLATE <http://ex.org/t>(?n) { ?o } FROM <http://ex.org/g{?n}> "
				+ "WHERE { ?s ?p ?o }", SPARQLExt.MEDIA_TYPE);
		Dataset dataset = DatasetFactory.create();
		dataset.addNamedModel("http://ex.org/g1", EngineTestUtils.turtle("<http://ex.org/s1> <http://ex.org/p> 1 ."));
		dataset.addNamedModel("http://ex.org/g2", EngineTestUtils.turtle("<http://ex.org/s2> <http://ex.org/p> 2 ."));
		String result = PlanFactory.create("TEMPLATE { TEMPLATE <http://ex.org/t>(?n) . } WHERE { VALUES ?n { 1 2 } }")
				.execTemplate(ContextUtils.build().setStreamManager(SPARQLExtStreamManager.makeStreamManager(locator))
						.setInputDataset(dataset).build());
		assertEquals("12", result);
	}

	/**
	 * The rows of a call that determine the same dataset are evaluated
	 * together, as a batch: LIMIT 1 keeps one solution for the rows on g1,
	 * and one for the row on g2.
	 */
	@Test
	public void rowsWithTheSameDatasetAreEvaluatedTogether() {
		Model model = generateWithGraphs(
				"GENERATE { GENERATE <http://ex.org/q>(?n, ?m) . } WHERE { VALUES (?n ?m) { (1 1) (1 2) (2 3) } }",
				"http://ex.org/q", "GENERATE <http://ex.org/q>(?n, ?m) { ?s <http://ex.org/m> ?m } "
						+ "FROM <http://ex.org/g{?n}> WHERE { ?s ?p ?o } LIMIT 1");
		assertEquals(EngineTestUtils.write(model), 2, model.size());
		assertEquals(EngineTestUtils.write(model), 1,
				model.listStatements(model.createResource("http://ex.org/s1"), null, (RDFNode) null).toList().size());
		assertTrue(EngineTestUtils.write(model), model.contains(model.createResource("http://ex.org/s2"),
				model.createProperty("http://ex.org/m"), model.createTypedLiteral(3)));
	}

	@Test
	public void unboundArgumentOfGenerateCall() {
		Model model = generate("GENERATE { GENERATE <http://ex.org/g>(?u) . } WHERE { }",
				"http://ex.org/g", "GENERATE <http://ex.org/g>(?x) { <http://ex.org/s> <http://ex.org/p> ?o } "
						+ "WHERE { BIND(COALESCE(?x, \"unbound\") AS ?o) }");
		assertIsomorphic("<http://ex.org/s> <http://ex.org/p> \"unbound\" .", model);
	}

	/**
	 * Two FROM GENERATE clauses with the same text, in two sibling
	 * sub-queries evaluated on the same binding, each build their own graph:
	 * the second one is not taken for a call already made.
	 */
	@Test
	public void identicalFromGenerateInSiblingSubQueries() {
		Model model = generate("GENERATE { "
				+ "GENERATE { ?s <http://ex.org/a> ?o } FROM GENERATE { <http://ex.org/s> <http://ex.org/p> 1 } . WHERE { ?s ?p ?o } . "
				+ "GENERATE { ?s <http://ex.org/b> ?o } FROM GENERATE { <http://ex.org/s> <http://ex.org/p> 1 } . WHERE { ?s ?p ?o } . "
				+ "}");
		assertIsomorphic("<http://ex.org/s> <http://ex.org/a> 1 ; <http://ex.org/b> 1 .", model);
	}

	/**
	 * The same named query called twice on the same binding, to the same
	 * output, is still executed once.
	 */
	@Test
	public void sameCallToTheSameOutputIsExecutedOnce() {
		java.util.List<org.apache.jena.graph.Triple> triples = new java.util.ArrayList<>();
		LocatorStringMap locator = new LocatorStringMap();
		locator.put("http://ex.org/q", "GENERATE <http://ex.org/q>(?n) { <http://ex.org/s> <http://ex.org/p> ?n }",
				SPARQLExt.MEDIA_TYPE);
		SPARQLExtStreamManager sm = SPARQLExtStreamManager.makeStreamManager(locator);
		PlanFactory.create("GENERATE { GENERATE <http://ex.org/q>(1) . GENERATE <http://ex.org/q>(1) . }")
				.execGenerateStream(ContextUtils.build().setStreamManager(sm)
						.setGenerateOutput(new org.apache.jena.riot.system.StreamRDFBase() {
							@Override
							public void triple(org.apache.jena.graph.Triple triple) {
								triples.add(triple);
							}
						}).build());
		assertEquals(1, triples.size());
	}

	/**
	 * A named query that calls itself with the same argument stops, also
	 * inside a FROM GENERATE.
	 */
	@Test
	public void recursionInFromGenerateStops() {
		Model model = generate("GENERATE { ?s <http://ex.org/in> ?o } "
				+ "FROM GENERATE <http://ex.org/q>(1) . WHERE { ?s ?p ?o }",
				"http://ex.org/q", "GENERATE <http://ex.org/q>(?n) { <http://ex.org/s> <http://ex.org/p> ?n . "
						+ "GENERATE <http://ex.org/q>(?n) . }");
		assertIsomorphic("<http://ex.org/s> <http://ex.org/in> 1 .", model);
	}
}
