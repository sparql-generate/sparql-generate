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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.ResultSet;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFBase;
import org.apache.jena.graph.Triple;
import org.junit.Test;

import fr.mines_stetienne.ci.sparql_generate.RaiseException;
import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.expr.E_Raise;
import fr.mines_stetienne.ci.sparql_generate.query.SPARQLExtQuery;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;

/**
 * {@code RAISE(message, value...)}: an expression whose evaluation stops the
 * execution with a {@link RaiseException}, wherever an expression is
 * evaluated, and only when it is reached.
 */
public class RaiseTest {

	static {
		SPARQLExt.init();
	}

	private static final String PREFIX = "PREFIX ex: <http://ex/> PREFIX iter: <http://w3id.org/sparql-generate/iter/> ";

	private static RaiseException raised(Runnable execution) {
		try {
			execution.run();
		} catch (RuntimeException ex) {
			RaiseException raised = RaiseException.findIn(ex);
			assertNotNull("not a RaiseException: " + ex, raised);
			// not wrapped: the plan rethrows it as is
			assertTrue("wrapped: " + ex, ex == raised);
			return raised;
		}
		fail("no error raised");
		return null;
	}

	private static RaiseException raisedByGenerate(String query) {
		return raised(() -> EngineTestUtils.generate(PREFIX + query));
	}

	@Test
	public void messageAndValues() {
		RaiseException ex = raisedByGenerate("GENERATE { ex:s ex:p ?x } "
				+ "WHERE { BIND(RAISE(\"Column not found\", \"IDs\", 3, ex:a, ?unbound, 1/0) AS ?x) }");
		assertEquals("Column not found", ex.getRaisedMessage());
		assertEquals("Column not found: \"IDs\", 3, <http://ex/a>, UNDEF, UNDEF", ex.getMessage());
		assertEquals(5, ex.getValues().size());
		assertEquals(NodeFactory.createLiteralString("IDs"), ex.getValues().get(0));
		assertEquals(NodeFactory.createURI("http://ex/a"), ex.getValues().get(2));
		assertNull(ex.getValues().get(3));
		assertNull(ex.getValues().get(4));
	}

	@Test
	public void messageOnly() {
		RaiseException ex = raisedByGenerate("GENERATE { ex:s ex:p ?x } WHERE { BIND(RAISE(\"stop\") AS ?x) }");
		assertEquals("stop", ex.getMessage());
		assertTrue(ex.getValues().isEmpty());
	}

	@Test
	public void messageWithEmbeddedExpression() {
		RaiseException ex = raisedByGenerate(
				"GENERATE { ex:s ex:p ?x } WHERE { VALUES ?n { 2 } BIND(RAISE(\"row {?n} has no name\") AS ?x) }");
		assertEquals("row 2 has no name", ex.getMessage());
	}

	@Test
	public void needsAMessage() {
		try {
			QueryFactory.create(PREFIX + "GENERATE { ex:s ex:p ?x } WHERE { BIND(RAISE() AS ?x) }", SPARQLExt.SYNTAX);
			fail("RAISE() parsed");
		} catch (QueryParseException ex) {
			assertTrue(ex.getMessage(), ex.getMessage().contains("RAISE needs a message"));
		}
	}

	@Test
	public void keywordIsCaseInsensitive() {
		RaiseException ex = raisedByGenerate("GENERATE { ex:s ex:p ?x } WHERE { BIND(raise(\"lower\") AS ?x) }");
		assertEquals("lower", ex.getMessage());
	}

	@Test
	public void onlyWhenReached() {
		// IF, COALESCE, || and && evaluate RAISE only when they need its value
		assertGenerates("<http://ex/s> <http://ex/p> 1, 2, 3, 4 .",
				PREFIX + "GENERATE { ex:s ex:p ?a, ?b, ?c, ?d } "
						+ "WHERE { BIND(1 AS ?x) "
						+ "BIND(IF(BOUND(?x), ?x, RAISE(\"no x\")) AS ?a) "
						+ "BIND(COALESCE(?x + 1, RAISE(\"no x\")) AS ?b) "
						+ "BIND(IF(?x = 1 || RAISE(\"not 1\"), 3, 0) AS ?c) "
						+ "BIND(IF(?x = 2 && RAISE(\"not reached\"), 0, 4) AS ?d) }");
	}

	@Test
	public void forEachSolutionThatReachesIt() {
		RaiseException ex = raisedByGenerate("GENERATE { ex:s ex:p ?y } "
				+ "WHERE { VALUES ?x { 1 2 3 } BIND(IF(?x < 3, ?x, RAISE(\"too large\", ?x)) AS ?y) }");
		assertEquals("too large: 3", ex.getMessage());
	}

	@Test
	public void notCaughtByCoalesce() {
		// COALESCE catches the errors of evaluation, not RAISE
		RaiseException ex = raisedByGenerate(
				"GENERATE { ex:s ex:p ?y } WHERE { BIND(COALESCE(RAISE(\"first\"), 1) AS ?y) }");
		assertEquals("first", ex.getMessage());
	}

	@Test
	public void inFilter() {
		RaiseException ex = raisedByGenerate("GENERATE { ex:s ex:p ?x } "
				+ "WHERE { VALUES ?x { 1 \"a\" } FILTER(isNumeric(?x) || RAISE(\"not a number\", ?x)) }");
		assertEquals("not a number: \"a\"", ex.getMessage());
		assertGenerates("<http://ex/s> <http://ex/p> 1, 2 .", PREFIX + "GENERATE { ex:s ex:p ?x } "
				+ "WHERE { VALUES ?x { 1 2 } FILTER(isNumeric(?x) || RAISE(\"not a number\", ?x)) }");
	}

	@Test
	public void inBindingClause() {
		RaiseException ex = raisedByGenerate("GENERATE { ex:s ex:p ?y } "
				+ "ITERATOR iter:for(1, 1, 3) AS ?i BIND(IF(?i != 2, ?i, RAISE(\"two\", ?i)) AS ?y)");
		assertEquals("two: 2", ex.getMessage());
	}

	@Test
	public void inIteratorArgument() {
		// an error of an iterator is a warning, RAISE in its arguments is not
		RaiseException ex = raisedByGenerate(
				"GENERATE { ex:s ex:p ?i } ITERATOR iter:for(1, 1, RAISE(\"no end\")) AS ?i");
		assertEquals("no end", ex.getMessage());
	}

	@Test
	public void afterAnIterator() {
		// the rest of the query, evaluated for each batch of the iterator
		RaiseException ex = raisedByGenerate("GENERATE { ex:s ex:p ?y } "
				+ "ITERATOR iter:for(1, 1, 5) AS ?i WHERE { BIND(IF(?i < 3, ?i, RAISE(\"three\")) AS ?y) }");
		assertEquals("three", ex.getMessage());
	}

	@Test
	public void inTemplateOfGenerate() {
		RaiseException ex = raisedByGenerate(
				"GENERATE { ex:s ex:p ?{ IF(?x > 1, RAISE(\"large\", ?x), ?x) } } WHERE { VALUES ?x { 1 2 } }");
		assertEquals("large: 2", ex.getMessage());
	}

	@Test
	public void inNestedGenerate() {
		RaiseException ex = raisedByGenerate("GENERATE { GENERATE { ex:s ex:p ?y } "
				+ "WHERE { BIND(IF(?x = 1, RAISE(\"nested\", ?x), ?x) AS ?y) } . } WHERE { VALUES ?x { 1 } }");
		assertEquals("nested: 1", ex.getMessage());
	}

	@Test
	public void inSelect() {
		RootPlan plan = PlanFactory.create(PREFIX + "SELECT ?y WHERE { VALUES ?x { 1 } "
				+ "OPTIONAL { ?x ex:p ?z } BIND(COALESCE(?z, RAISE(\"no z for\", ?x)) AS ?y) }");
		RaiseException ex = raised(() -> {
			ResultSet rs = plan.execSelect(ContextUtils.build().build());
			while (rs.hasNext()) {
				rs.next();
			}
		});
		assertEquals("no z for: 1", ex.getMessage());
	}

	@Test
	public void inTemplate() {
		RaiseException ex = raised(() -> EngineTestUtils.template(
				PREFIX + "TEMPLATE { \"x: \" IF(?x = 1, RAISE(\"template\"), ?x) } WHERE { VALUES ?x { 1 } }"));
		assertEquals("template", ex.getMessage());
	}

	@Test
	public void inStream() {
		// the triples generated before the error are emitted, then the error
		List<Triple> triples = new ArrayList<>();
		StreamRDF stream = new StreamRDFBase() {
			@Override
			public void triple(Triple triple) {
				triples.add(triple);
			}
		};
		RootPlan plan = PlanFactory.create(PREFIX + "GENERATE { ex:s ex:p ?y } "
				+ "ITERATOR iter:for(1, 1, 5) AS ?i WHERE { BIND(IF(?i < 3, ?i, RAISE(\"three\")) AS ?y) }");
		RaiseException ex = raised(
				() -> plan.execGenerateStream(ContextUtils.build().setGenerateOutput(stream).build()));
		assertEquals("three", ex.getMessage());
	}

	@Test
	public void serializationRoundTrip() {
		String[] queries = { "GENERATE { ex:s ex:p ?y } WHERE { BIND(COALESCE(IRI(?x), RAISE(\"not an IRI\", ?x)) AS ?y) }",
				"GENERATE { ex:s ex:p ?y } ITERATOR iter:for(1, 1, 3) AS ?i BIND(IF(?i < 3, ?i, RAISE(\"three\", ?i, ex:a)) AS ?y)",
				"GENERATE { ex:s ex:p ?{ RAISE(\"in a template\") } } WHERE { FILTER(BOUND(?x) || RAISE(\"no x\")) }",
				"TEMPLATE { IF(?x, RAISE(\"t\"), \"f\") } WHERE { BIND(true AS ?x) }" };
		for (String q : queries) {
			SPARQLExtQuery query = (SPARQLExtQuery) QueryFactory.create(PREFIX + q, SPARQLExt.SYNTAX);
			String serialized = query.toString();
			assertTrue(serialized, serialized.contains("RAISE("));
			SPARQLExtQuery query2 = (SPARQLExtQuery) QueryFactory.create(serialized, SPARQLExt.SYNTAX);
			assertEquals(serialized, query, query2);
			assertEquals(serialized, query2.toString());
		}
	}

	@Test
	public void copiedByNormalizers() {
		// the plan normalizes a copy of the query: expressions embedded in IRIs
		// and literals around RAISE
		RaiseException ex = raisedByGenerate("GENERATE { <s/{?x}> ex:p ?y } "
				+ "WHERE { VALUES ?x { \"a\" } BIND(RAISE(\"iri <{?x}>\", <v/{?x}>) AS ?y) }");
		assertTrue(ex.getMessage(), ex.getMessage().startsWith("iri <a>: <"));
		assertTrue(ex.getMessage(), ex.getMessage().endsWith("v/a>"));
	}

	@Test
	public void expressionClass() {
		SPARQLExtQuery query = (SPARQLExtQuery) QueryFactory.create(
				PREFIX + "SELECT ?y WHERE { BIND(RAISE(\"m\", 1) AS ?y) }", SPARQLExt.SYNTAX);
		String text = query.toString();
		assertTrue(text, text.contains("RAISE(\"m\", 1)"));
		assertTrue(new E_Raise(new org.apache.jena.sparql.expr.ExprList()).getFunctionPrintName(null).equals("RAISE"));
	}
}
