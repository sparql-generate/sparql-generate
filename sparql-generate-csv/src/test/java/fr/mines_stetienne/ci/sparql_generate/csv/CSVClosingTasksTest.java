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
package fr.mines_stetienne.ci.sparql_generate.csv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.expr.NodeValue;
import org.junit.Test;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.engine.PlanFactory;
import fr.mines_stetienne.ci.sparql_generate.iterator.IteratorFunctionBase0;
import fr.mines_stetienne.ci.sparql_generate.iterator.IteratorFunctionRegistry;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;

/**
 * A CSV document read by an iterator registers a task that stops its parser
 * if the execution is closed, and removes it when the document is read: the
 * task kept the parser, its buffer of 2 MB and the rest of the query until
 * the end of the execution, so that the memory grew with the number of
 * documents read (about 100 MB per GTFS feed in kgc-bench's gtfs-feeds).
 */
public class CSVClosingTasksTest {

	static final AtomicInteger MAX_TASKS = new AtomicInteger();

	static final String PROBE = "http://ex/test-closing-tasks";

	/**
	 * Gives one solution, and records the number of closing tasks registered.
	 */
	public static class ITER_Probe extends IteratorFunctionBase0 {
		@Override
		public List<List<NodeValue>> exec() {
			MAX_TASKS.accumulateAndGet(ContextUtils.countTasksOnClose(getContext()), Math::max);
			return Collections.singletonList(Collections.singletonList(NodeValue.makeInteger(1)));
		}
	}

	static {
		SPARQLExt.init();
		IteratorFunctionRegistry.get().put(PROBE, ITER_Probe.class);
	}

	private static final int DOCUMENTS = 200;

	private static String rows(int n) {
		StringBuilder sb = new StringBuilder("a");
		for (int i = 0; i < n; i++) {
			sb.append("\\n").append(i);
		}
		return sb.toString();
	}

	/**
	 * One document per solution of the outer iterator: when the probe is
	 * evaluated, only the documents being read have a closing task.
	 */
	private static void assertBounded(String inner) {
		MAX_TASKS.set(0);
		Model model = PlanFactory.create("PREFIX iter: <http://w3id.org/sparql-generate/iter/> "
				+ "GENERATE { <http://ex/s> <http://ex/p> ?b } "
				+ "ITERATOR iter:CSV(\"" + rows(DOCUMENTS) + "\", \"a\") AS ?a ?i "
				+ "ITERATOR " + inner + " "
				+ "ITERATOR <" + PROBE + ">() AS ?c")
				.execGenerate(ContextUtils.build().build());
		assertEquals(DOCUMENTS, model.size());
		assertTrue("closing tasks of the documents already read: " + MAX_TASKS.get(), MAX_TASKS.get() <= 2);
	}

	@Test
	public void csvRemovesItsClosingTask() {
		assertBounded("iter:CSV(CONCAT(\"b\\n\", ?a), \"b\") AS ?b ?j");
	}

	@Test
	public void csvWithoutColumnsRemovesItsClosingTask() {
		assertBounded("iter:CSV(CONCAT(\"b\\n\", ?a)) AS ?b");
	}

	@Test
	public void csvStrictRemovesItsClosingTask() {
		assertBounded("iter:CSVStrict(CONCAT(\"b\\n\", ?a), \"b\") AS ?b ?j");
	}

	/**
	 * A document that cannot be parsed (a row of more than the 512 columns of
	 * the default settings) removes its closing task too: with one row per
	 * batch, the probe of a row is evaluated after the documents of the
	 * previous rows.
	 */
	@Test
	public void failedParseRemovesItsClosingTask() {
		MAX_TASKS.set(0);
		String cell = ",".repeat(600);
		PlanFactory.create("PREFIX iter: <http://w3id.org/sparql-generate/iter/> "
				+ "GENERATE { <http://ex/s> <http://ex/p> ?b } "
				+ "ITERATOR iter:CSV(\"" + rows(5) + "\", 1, \"a\") AS ?a ?i "
				+ "ITERATOR <" + PROBE + ">() AS ?c "
				+ "ITERATOR iter:CSV(CONCAT(\"b\\n\", ?a, \"" + cell + "\")) AS ?b")
				.execGenerate(ContextUtils.build().build());
		assertTrue("closing tasks of the documents already read: " + MAX_TASKS.get(), MAX_TASKS.get() <= 1);
	}
}
