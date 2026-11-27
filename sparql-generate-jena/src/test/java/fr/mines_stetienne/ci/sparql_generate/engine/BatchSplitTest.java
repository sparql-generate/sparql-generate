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

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFBase;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.junit.Test;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;

/**
 * The solutions of an iterator are split into batches when the output of the
 * query does not depend on the batches, so that the memory does not grow with
 * the size of the documents; the output is the same as with one batch.
 */
public class BatchSplitTest {

	static {
		SPARQLExt.init();
	}

	private static final String PREFIXES = "PREFIX iter: <http://w3id.org/sparql-generate/iter/> "
			+ "BASE <http://ex/> ";

	/** The triples generated, in the order of the output stream. */
	private static List<String> stream(String query, int batchSize) {
		final List<String> triples = new ArrayList<>();
		final StreamRDF output = new StreamRDFBase() {
			@Override
			public void triple(Triple triple) {
				triples.add(local(triple.getSubject()) + " " + local(triple.getPredicate()) + " "
						+ (triple.getObject().isLiteral() ? triple.getObject().getLiteralLexicalForm()
								: local(triple.getObject())));
			}
		};
		PlanFactory.create(PREFIXES + query)
				.execGenerateStream(ContextUtils.build(output).setBatchSize(batchSize).build());
		return triples;
	}

	private static String local(Node node) {
		return node.isURI() ? node.getURI().replace("http://ex/", "") : node.toString();
	}

	private static Model model(String query, int batchSize) {
		return PlanFactory.create(PREFIXES + query).execGenerate(ContextUtils.build().setBatchSize(batchSize).build());
	}

	private static void assertSameOutput(String query) {
		final Model oneBatch = model(query, 0);
		for (int batchSize : new int[] { 1, 2, 3 }) {
			final Model split = model(query, batchSize);
			assertTrue("batch size " + batchSize + ":\n" + EngineTestUtils.write(split) + "\ninstead of\n"
					+ EngineTestUtils.write(oneBatch), split.isIsomorphicWith(oneBatch));
		}
	}

	private static final String NESTED = "GENERATE { <s/{?i}> <p> ?i . GENERATE { <t/{?i}> <q> ?i . } . } "
			+ "ITERATOR iter:for(1, 1, 4) AS ?i WHERE { }";

	/**
	 * Split into batches of one solution: the sub-query is evaluated after
	 * each batch; the output is the same.
	 */
	@Test
	public void splitWhenTheOutputDoesNotDependOnTheBatches() {
		assertEquals(List.of("s/1 p 1", "s/2 p 2", "s/3 p 3", "t/1 q 1", "t/2 q 2", "t/3 q 3"), stream(NESTED, 0));
		assertEquals(List.of("s/1 p 1", "t/1 q 1", "s/2 p 2", "t/2 q 2", "s/3 p 3", "t/3 q 3"), stream(NESTED, 1));
		assertEquals(List.of("s/1 p 1", "s/2 p 2", "t/1 q 1", "t/2 q 2", "s/3 p 3", "t/3 q 3"), stream(NESTED, 2));
		assertSameOutput(NESTED);
	}

	@Test
	public void splitWithBindAndWhere() {
		assertSameOutput("GENERATE { <s/{?j}> <p> ?o . } ITERATOR iter:for(1, 1, 5) AS ?i BIND(?i * 2 AS ?j) "
				+ "WHERE { VALUES ?o { \"a\" \"b\" } FILTER(?i != 3) OPTIONAL { ?x ?y ?z } }");
	}

	@Test
	public void splitNestedIterators() {
		assertSameOutput("GENERATE { GENERATE { <s/{?i}/{?k}> <p> ?k . } ITERATOR iter:for(1, 1, ?i) AS ?k . } "
				+ "ITERATOR iter:for(1, 1, 4) AS ?i WHERE { }");
	}

	/** LIMIT, aggregates, LIST: one batch, as before. */
	@Test
	public void notSplitWhenTheOutputDependsOnTheBatches() {
		final String limit = "GENERATE { <s/{?i}> <p> ?i . } ITERATOR iter:for(1, 1, 4) AS ?i WHERE { } LIMIT 2";
		assertEquals(2, stream(limit, 1).size());
		assertSameOutput(limit);
		final String offset = "GENERATE { <s/{?i}> <p> ?i . } ITERATOR iter:for(1, 1, 4) AS ?i WHERE { } OFFSET 1";
		assertEquals(2, stream(offset, 1).size());
		final String groupBy = "GENERATE { <s> <p> LIST(?i) . } ITERATOR iter:for(1, 1, 4) AS ?i WHERE { } "
				+ "GROUP BY ?x";
		assertSameOutput(groupBy);
		// one list of the three values
		assertEquals(1 + 6, stream(groupBy, 1).size());
		final String list = "GENERATE { <s/{?i}> <p> LIST(?i) . } ITERATOR iter:for(1, 1, 4) AS ?i WHERE { }";
		assertSameOutput(list);
		// one list of the three values, shared by the three subjects
		assertEquals(3 + 6, stream(list, 1).size());
	}

	/**
	 * A sub-query whose output depends on the batches of its parent: the
	 * solutions of the parent are not split.
	 */
	@Test
	public void notSplitWhenASubQueryDependsOnTheBatches() {
		final String subLimit = "GENERATE { GENERATE { <s/{?i}> <p> ?i . } WHERE { } LIMIT 2 . } "
				+ "ITERATOR iter:for(1, 1, 4) AS ?i WHERE { }";
		assertEquals(2, stream(subLimit, 1).size());
		assertSameOutput(subLimit);
	}

	@Test
	public void unionOfBatches() {
		assertTrue(PlanFactory.create(PREFIXES + NESTED).isUnionOfBatches());
		assertFalse(PlanFactory.create(PREFIXES + "GENERATE { <s> <p> ?i . } ITERATOR iter:for(1, 1, 4) AS ?i "
				+ "WHERE { } ORDER BY ?i").isUnionOfBatches());
		assertFalse(PlanFactory.create(PREFIXES + "GENERATE { GENERATE { <s> <p> ?i . } "
				+ "WHERE { } GROUP BY ?i . } ITERATOR iter:for(1, 1, 4) AS ?i WHERE { }").isUnionOfBatches());
		// a sub-query with a dataset would compute it again for each batch
		assertFalse(PlanFactory.create(PREFIXES + "GENERATE { GENERATE { <s> <p> ?i . } "
				+ "FROM GENERATE { <a> <b> <c> . } . WHERE { } . } ITERATOR iter:for(1, 1, 4) AS ?i WHERE { }")
				.isUnionOfBatches());
		assertFalse(PlanFactory.create(PREFIXES + "TEMPLATE { ?i } ITERATOR iter:for(1, 1, 4) AS ?i WHERE { }")
				.isUnionOfBatches());
	}

	@Test
	public void splitGivesBatchesOfAtMostTheSize() {
		final List<Binding> bindings = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			bindings.add(BindingFactory.empty());
		}
		final List<Integer> sizes = new ArrayList<>();
		IteratorPlan.split(new ArrayList<>(bindings), 2, (batch) -> sizes.add(batch.size()));
		assertEquals(List.of(2, 2, 1), sizes);
		sizes.clear();
		IteratorPlan.split(new ArrayList<>(), 2, (batch) -> sizes.add(batch.size()));
		assertEquals(List.of(0), sizes);
	}
}
