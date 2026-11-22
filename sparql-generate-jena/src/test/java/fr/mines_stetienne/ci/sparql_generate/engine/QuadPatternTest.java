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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.util.IsoMatcher;
import org.junit.Test;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.query.SPARQLExtQuery;
import fr.mines_stetienne.ci.sparql_generate.stream.LocatorStringMap;
import fr.mines_stetienne.ci.sparql_generate.stream.SPARQLExtStreamManager;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;

/**
 * {@code GRAPH} blocks in a {@code GENERATE} template.
 */
public class QuadPatternTest {

	static {
		SPARQLExt.init();
	}

	private static final String PREFIX = "PREFIX ex: <http://ex/> ";

	private static Dataset generate(String query, String... iriAndQuery) {
		LocatorStringMap locator = new LocatorStringMap();
		for (int i = 0; i < iriAndQuery.length; i += 2) {
			locator.put(iriAndQuery[i], iriAndQuery[i + 1], SPARQLExt.MEDIA_TYPE);
		}
		SPARQLExtStreamManager sm = SPARQLExtStreamManager.makeStreamManager(locator);
		return PlanFactory.create(query).execGenerateDataset(ContextUtils.build().setStreamManager(sm).build());
	}

	private static Dataset trig(String trig) {
		Dataset dataset = DatasetFactory.create();
		RDFDataMgr.read(dataset, new StringReader("@prefix ex: <http://ex/> . " + trig), null, Lang.TRIG);
		return dataset;
	}

	private static String write(Dataset dataset) {
		StringWriter sw = new StringWriter();
		RDFDataMgr.write(sw, dataset, Lang.NQUADS);
		return sw.toString();
	}

	private static void assertGenerates(String expectedTrig, Dataset actual) {
		Dataset expected = trig(expectedTrig);
		assertTrue("expected:\n" + write(expected) + "\nbut got:\n" + write(actual),
				IsoMatcher.isomorphic(expected.asDatasetGraph(), actual.asDatasetGraph()));
	}

	private static void assertGenerates(String expectedTrig, String query, String... iriAndQuery) {
		assertGenerates(expectedTrig, generate(query, iriAndQuery));
	}

	@Test
	public void constantGraph() {
		assertGenerates("ex:g { ex:s ex:p 1, 2 }",
				PREFIX + "GENERATE { GRAPH ex:g { ex:s ex:p ?o } } WHERE { VALUES ?o { 1 2 } }");
	}

	@Test
	public void variableGraph() {
		assertGenerates("ex:g1 { ex:s ex:p 1 } ex:g2 { ex:s ex:p 2 }",
				PREFIX + "GENERATE { GRAPH ?g { ex:s ex:p ?o } } "
						+ "WHERE { VALUES (?g ?o) { (ex:g1 1) (ex:g2 2) } }");
	}

	/**
	 * An IRI with embedded expressions names the graph, as in the other
	 * positions of the template.
	 */
	@Test
	public void graphIriTemplate() {
		assertGenerates("ex:g1 { ex:s ex:p 1 } ex:g2 { ex:s ex:p 2 }",
				PREFIX + "GENERATE { GRAPH <http://ex/g{?o}> { ex:s ex:p ?o } } WHERE { VALUES ?o { 1 2 } }");
	}

	@Test
	public void graphExpression() {
		assertGenerates("ex:g1 { ex:s ex:p 1 } ex:g2 { ex:s ex:p 2 }",
				PREFIX + "GENERATE { GRAPH ?{ IRI(CONCAT(\"http://ex/g\", STR(?o))) } { ex:s ex:p ?o } } "
						+ "WHERE { VALUES ?o { 1 2 } }");
	}

	@Test
	public void defaultAndNamedGraphs() {
		assertGenerates("ex:s ex:p 1 . ex:a { ex:s ex:q 1 } ex:s ex:r 1 . ex:b { ex:s ex:t 1 . ex:s ex:u 1 }",
				PREFIX + "GENERATE { ex:s ex:p ?o . GRAPH ex:a { ex:s ex:q ?o } . ex:s ex:r ?o "
						+ "GRAPH ex:b { ex:s ex:t ?o . ex:s ex:u ?o . } } WHERE { VALUES ?o { 1 } }");
	}

	/**
	 * Without dot, the dot after a GRAPH block is optional, as in SPARQL
	 * Update.
	 */
	@Test
	public void consecutiveGraphBlocks() {
		assertGenerates("ex:a { ex:s ex:p 1 } ex:b { ex:s ex:p 1 }",
				PREFIX + "GENERATE { GRAPH ex:a { ex:s ex:p 1 } GRAPH ex:b { ex:s ex:p 1 } }");
	}

	@Test
	public void emptyGraphBlock() {
		Dataset dataset = generate(PREFIX + "GENERATE { GRAPH ex:a { } ex:s ex:p 1 }");
		assertGenerates("ex:s ex:p 1 .", dataset);
		assertFalse(dataset.listNames().hasNext());
	}

	/**
	 * A quad whose graph variable is unbound is not generated, as a triple
	 * whose term is unbound; nor is a quad whose graph is a literal.
	 */
	@Test
	public void unboundOrLiteralGraphIsNotGenerated() {
		assertGenerates("ex:s ex:p 1, 2, 3 . ex:g { ex:s ex:q 1 }",
				PREFIX + "GENERATE { ex:s ex:p ?o . GRAPH ?g { ex:s ex:q ?o } } "
						+ "WHERE { VALUES (?g ?o) { (ex:g 1) (UNDEF 2) (\"g\" 3) } }");
	}

	/**
	 * A graph variable bound to a blank node names a graph: RDF 1.1 datasets
	 * and Jena accept blank nodes as graph names.
	 */
	@Test
	public void blankNodeGraph() {
		Dataset dataset = generate(PREFIX + "GENERATE { GRAPH ?g { ex:s ex:p 1 } } WHERE { BIND(BNODE() AS ?g) }");
		List<Node> names = new ArrayList<>();
		dataset.asDatasetGraph().listGraphNodes().forEachRemaining(names::add);
		assertEquals(1, names.size());
		assertTrue(names.get(0).isBlank());
		assertEquals(1, dataset.asDatasetGraph().getGraph(names.get(0)).size());
	}

	/**
	 * A blank node label denotes the same blank node in the default graph and
	 * in the GRAPH blocks, for each solution.
	 */
	@Test
	public void blankNodeIsSharedAcrossGraphs() {
		assertGenerates("_:b1 ex:p 1 . ex:g { _:b1 ex:q 1 } _:b2 ex:p 2 . ex:g { _:b2 ex:q 2 }",
				PREFIX + "GENERATE { _:b ex:p ?o . GRAPH ex:g { _:b ex:q ?o } } WHERE { VALUES ?o { 1 2 } }");
	}

	@Test
	public void listInGraph() {
		assertGenerates("ex:g { ex:s ex:p ( 1 2 ) }",
				PREFIX + "GENERATE { GRAPH ex:g { ex:s ex:p LIST(?o) } } WHERE { VALUES ?o { 1 2 } } ORDER BY ?o");
	}

	@Test
	public void subQueryInGraphGeneratesInThatGraph() {
		assertGenerates("ex:g { ex:s ex:p 1 . ex:s ex:q 2 }",
				PREFIX + "GENERATE { GRAPH ex:g { ex:s ex:p ?o . GENERATE { ex:s ex:q ?j } WHERE { BIND(?o + 1 AS ?j) } . } } "
						+ "WHERE { VALUES ?o { 1 } }");
	}

	@Test
	public void subQueryInVariableGraph() {
		assertGenerates("ex:g1 { ex:s ex:q 1 } ex:g2 { ex:s ex:q 2 }",
				PREFIX + "GENERATE { GRAPH ?g { GENERATE { ex:s ex:q ?o } . } } "
						+ "WHERE { VALUES (?g ?o) { (ex:g1 1) (ex:g2 2) (UNDEF 3) } }");
	}

	/**
	 * A GRAPH block of the sub-query overrides the GRAPH block that contains
	 * it.
	 */
	@Test
	public void graphOfSubQueryOverridesOuterGraph() {
		assertGenerates("ex:g { ex:s ex:p 1 } ex:h { ex:s ex:q 1 }",
				PREFIX + "GENERATE { GRAPH ex:g { GENERATE { ex:s ex:p ?o . GRAPH ex:h { ex:s ex:q ?o } } . } } "
						+ "WHERE { VALUES ?o { 1 } }");
	}

	@Test
	public void subQueryOutsideGraphBlocksWithGraphBlock() {
		assertGenerates("ex:s ex:p 1 . ex:h { ex:s ex:q 1 }",
				PREFIX + "GENERATE { GENERATE { ex:s ex:p ?o . GRAPH ex:h { ex:s ex:q ?o } } . } "
						+ "WHERE { VALUES ?o { 1 } }");
	}

	private static final String ITE = PREFIX + "PREFIX ite: <http://w3id.org/sparql-generate/iter/> ";

	/**
	 * The graph variable is bound by a BIND clause after an ITERATOR clause,
	 * as in the queries translated from RML.
	 */
	@Test
	public void graphBoundByIteratorAndBind() {
		assertGenerates("ex:s ex:p 1, 2 . ex:g1 { ex:s ex:p 1 } ex:g2 { ex:s ex:p 2 }",
				ITE + "GENERATE { ex:s ex:p ?i . GRAPH ?g { ex:s ex:p ?i } } ITERATOR ite:for(1, 1, 3) AS ?i "
						+ "BIND(IRI(CONCAT(\"http://ex/g\", STR(?i))) AS ?g)");
	}

	@Test
	public void graphIriTemplateOfIteratorVariable() {
		assertGenerates("ex:g-a { ex:s ex:p \"a\" } ex:g-b { ex:s ex:p \"b\" }",
				ITE + "GENERATE { GRAPH <http://ex/g-{?x}> { ex:s ex:p ?x } } ITERATOR ite:Split(\"a,b\", \",\") AS ?x");
	}

	@Test
	public void graphBoundInWhereClause() {
		assertGenerates("ex:g1 { ex:s ex:p 1 } ex:g2 { ex:s ex:p 2 }",
				ITE + "GENERATE { GRAPH ?g { ex:s ex:p ?i } } ITERATOR ite:for(1, 1, 3) AS ?i "
						+ "WHERE { BIND(IRI(CONCAT(\"http://ex/g\", STR(?i))) AS ?g) }");
	}

	/**
	 * In a sub-query, the graph variable is bound by the iterator of the
	 * sub-query, from a variable of the parent.
	 */
	@Test
	public void graphOfSubQueryBoundByItsIterator() {
		assertGenerates("ex:g1-a { ex:s ex:p \"a\" } ex:g1-b { ex:s ex:p \"b\" } ex:g2-a { ex:s ex:p \"a\" } ex:g2-b { ex:s ex:p \"b\" }",
				ITE + "GENERATE { GENERATE { GRAPH ?g { ex:s ex:p ?x } } ITERATOR ite:Split(\"a,b\", \",\") AS ?x "
						+ "BIND(IRI(CONCAT(\"http://ex/g\", STR(?i), \"-\", ?x)) AS ?g) . } "
						+ "ITERATOR ite:for(1, 1, 3) AS ?i");
	}

	/**
	 * A sub-query in a GRAPH block whose graph the parent computes from its
	 * iterator, two levels deep.
	 */
	@Test
	public void subSubQueryInGraphOfIterator() {
		assertGenerates("ex:g1 { ex:s ex:p 1 . ex:s ex:q \"a\", \"b\" } ex:g2 { ex:s ex:p 2 . ex:s ex:q \"a\", \"b\" }",
				ITE + "GENERATE { GRAPH <http://ex/g{?i}> { ex:s ex:p ?i . "
						+ "GENERATE { GENERATE { ex:s ex:q ?x } ITERATOR ite:Split(\"a,b\", \",\") AS ?x . } . } } "
						+ "ITERATOR ite:for(1, 1, 3) AS ?i");
	}

	/**
	 * A named GENERATE query called from a GRAPH block generates its default
	 * graph in that graph; a call with the same arguments from another graph
	 * is executed again.
	 */
	@Test
	public void namedQueryCalledInGraph() {
		assertGenerates("ex:s ex:q 1 . ex:g { ex:s ex:q 1 } ex:h { ex:s ex:q 1 . ex:s ex:r 1 }",
				PREFIX + "GENERATE { GENERATE ex:q(?o) . GRAPH ex:g { GENERATE ex:q(?o) . } "
						+ "GRAPH ?h { GENERATE ex:q(?o) . } } WHERE { VALUES (?o ?h) { (1 ex:h) } }",
				"http://ex/q", "PREFIX ex: <http://ex/> GENERATE ex:q(?x) { ex:s ex:q ?x . GRAPH ex:h { ex:s ex:r ?x } }");
	}

	/**
	 * GRAPH blocks cannot be nested, as in SPARQL Update; a sub-query in a
	 * GRAPH block can have its own.
	 */
	@Test
	public void nestedGraphBlocksAreRefused() {
		try {
			QueryFactory.create(PREFIX + "GENERATE { GRAPH ex:g { GRAPH ex:h { ex:s ex:p 1 } } }", SPARQLExt.SYNTAX);
			fail("nested GRAPH blocks were accepted");
		} catch (QueryParseException ex) {
			assertTrue(ex.getMessage(), ex.getMessage().contains("cannot be nested"));
		}
		// a GRAPH block after a sub-query inside a GRAPH block is still refused
		try {
			QueryFactory.create(PREFIX + "GENERATE { GRAPH ex:g { GENERATE { ex:s ex:p 1 } . GRAPH ex:h { ex:s ex:p 1 } } }",
					SPARQLExt.SYNTAX);
			fail("nested GRAPH blocks were accepted");
		} catch (QueryParseException ex) {
			assertTrue(ex.getMessage(), ex.getMessage().contains("cannot be nested"));
		}
	}

	/**
	 * The serialization of a query with GRAPH blocks parses to the same
	 * query.
	 */
	@Test
	public void serializationRoundTrip() {
		String[] queries = {
				PREFIX + "GENERATE { ex:s ex:p ?o . GRAPH ex:g { ex:s ex:p ?o } GRAPH ?g { ex:s ex:q ?o . "
						+ "GENERATE { ex:s ex:r ?o . GRAPH ex:h { ex:s ex:t ?o } } WHERE { } . ex:s ex:u ?o } "
						+ "ex:s ex:v ?o } WHERE { VALUES (?g ?o) { (ex:g 1) } }",
				PREFIX + "GENERATE { GRAPH <http://ex/g{?o}> { ex:s ex:p ?o } GRAPH ?{ IRI(\"http://ex/x\") } { ex:s ex:p 1 } } "
						+ "WHERE { VALUES ?o { 1 } }",
				PREFIX + "GENERATE { GRAPH ex:g { GENERATE ex:q(?o) . } }" };
		for (String q : queries) {
			SPARQLExtQuery query = (SPARQLExtQuery) QueryFactory.create(q, SPARQLExt.SYNTAX);
			String serialized = query.toString();
			assertTrue(serialized, serialized.contains("GRAPH"));
			SPARQLExtQuery query2 = (SPARQLExtQuery) QueryFactory.create(serialized, SPARQLExt.SYNTAX);
			assertEquals(serialized, query, query2);
			assertEquals(serialized, query2.toString());
		}
	}

	@Test
	public void hasQuadPatterns() {
		assertTrue(((SPARQLExtQuery) QueryFactory.create(PREFIX + "GENERATE { GRAPH ex:g { ex:s ex:p 1 } }",
				SPARQLExt.SYNTAX)).hasQuadPatterns());
		assertTrue(((SPARQLExtQuery) QueryFactory.create(
				PREFIX + "GENERATE { GENERATE { GRAPH ex:g { ex:s ex:p 1 } } . }", SPARQLExt.SYNTAX)).hasQuadPatterns());
		assertFalse(((SPARQLExtQuery) QueryFactory.create(
				PREFIX + "GENERATE { ex:s ex:p 1 . GENERATE { ex:s ex:p 2 } . } WHERE { GRAPH ?g { ?s ?p ?o } }",
				SPARQLExt.SYNTAX)).hasQuadPatterns());
	}

	/**
	 * The methods that return a model return the default graph of a query
	 * with GRAPH blocks; the named graphs are ignored, with a warning.
	 */
	@Test
	public void modelIsTheDefaultGraph() {
		Model model = EngineTestUtils.generate(
				PREFIX + "GENERATE { ex:s ex:p 1 . GRAPH ex:g { ex:s ex:q 1 } . GENERATE { GRAPH ex:h { ex:s ex:r 1 } } . }");
		assertTrue(model.isIsomorphicWith(EngineTestUtils.turtle("<http://ex/s> <http://ex/p> 1 .")));
	}

	/**
	 * A stream receives the triples of the default graph, then quads for the
	 * named graphs.
	 */
	@Test
	public void streamReceivesQuads() {
		final List<Triple> triples = new ArrayList<>();
		final List<Quad> quads = new ArrayList<>();
		StreamRDF stream = new StreamRDF() {
			@Override
			public void start() {
			}

			@Override
			public void triple(Triple triple) {
				triples.add(triple);
			}

			@Override
			public void quad(Quad quad) {
				quads.add(quad);
			}

			@Override
			public void base(String base) {
			}

			@Override
			public void prefix(String prefix, String iri) {
			}

			@Override
			public void finish() {
			}
		};
		PlanFactory.create(PREFIX + "GENERATE { ex:s ex:p 1 . GRAPH ex:g { ex:s ex:q 1 } }")
				.execGenerateStream(ContextUtils.build().setGenerateOutput(stream).build());
		assertEquals(1, triples.size());
		assertEquals(1, quads.size());
		assertEquals("http://ex/g", quads.get(0).getGraph().getURI());
	}

	/**
	 * A query without GRAPH block gives a dataset whose default graph is the
	 * model of execGenerate, and no named graph.
	 */
	@Test
	public void queryWithoutGraphBlock() {
		String q = PREFIX + "GENERATE { _:b ex:p ?o . GENERATE { _:b ex:q ?j } WHERE { BIND(?o * 2 AS ?j) } . "
				+ "ex:s ex:l LIST(?o) } WHERE { VALUES ?o { 1 2 } } ORDER BY ?o";
		Dataset dataset = generate(q);
		assertFalse(dataset.listNames().hasNext());
		assertTrue(dataset.getDefaultModel().isIsomorphicWith(EngineTestUtils.generate(q)));
		assertEquals("http://ex/", dataset.getDefaultModel().getNsPrefixURI("ex"));
		assertFalse(((SPARQLExtQuery) QueryFactory.create(q, SPARQLExt.SYNTAX)).hasQuadPatterns());
	}

	/**
	 * The constructor of the plan without graphs still works.
	 */
	@Test
	public void planOfTriples() {
		BasicPattern bgp = new BasicPattern();
		Node s = NodeFactory.createURI("http://ex/s");
		bgp.add(Triple.create(s, s, s));
		GenerateFormPlan plan = new GenerateFormPlan(bgp, new ArrayList<>());
		Model model = org.apache.jena.rdf.model.ModelFactory.createDefaultModel();
		List<org.apache.jena.sparql.engine.binding.Binding> values = new ArrayList<>();
		values.add(org.apache.jena.sparql.engine.binding.BindingFactory.binding());
		plan.exec(new ArrayList<>(), values,
				ContextUtils.build().setGenerateOutput(new StreamRDFModel(model)).build());
		assertEquals(1, model.size());
	}

	/**
	 * A FROM GENERATE clause gives one graph: the quads of its GRAPH blocks
	 * are ignored.
	 */
	@Test
	public void fromGenerateIgnoresNamedGraphs() {
		assertGenerates("ex:s ex:found ex:p .",
				PREFIX + "GENERATE { ?s ex:found ?p } FROM GENERATE { ex:s ex:p 1 . GRAPH ex:g { ex:s ex:q 1 } } . "
						+ "WHERE { ?s ?p ?o }");
	}

	@Test
	public void namesOfGraphs() {
		Dataset dataset = generate(PREFIX + "GENERATE { GRAPH ?g { ex:s ex:p ?o } } WHERE { VALUES (?g ?o) { (ex:a 1) (ex:b 2) (ex:a 3) } }");
		List<String> names = new ArrayList<>();
		for (Iterator<String> it = dataset.listNames(); it.hasNext();) {
			names.add(it.next());
		}
		names.sort(null);
		assertEquals("[http://ex/a, http://ex/b]", names.toString());
		assertEquals(2, dataset.getNamedModel("http://ex/a").size());
	}
}
