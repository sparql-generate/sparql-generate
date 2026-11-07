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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import fr.mines_stetienne.ci.sparql_generate.ExecutionLimitException;
import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;

/**
 * The limits of an execution, which the playground server sets.
 */
public class ExecutionLimitTest {

	static {
		SPARQLExt.init();
	}

	/**
	 * An execution of tens of millions of triples stops after its time limit.
	 */
	@Test(timeout = 30000)
	public void timeLimit() {
		long start = System.nanoTime();
		try {
			PlanFactory.create("PREFIX iter: <http://w3id.org/sparql-generate/iter/> "
					+ "GENERATE { GENERATE { <http://ex/s> <http://ex/p> ?k } ITERATOR iter:for(0, 1, 5000) AS ?j "
					+ "WHERE { BIND(?i * ?j AS ?k) } . } ITERATOR iter:for(0, 1, 5000) AS ?i")
					.execGenerateStream(ContextUtils.build().setTimeLimit(1)
							.setGenerateOutput(new org.apache.jena.riot.system.StreamRDFBase()).build());
			fail("the time limit stops the execution");
		} catch (ExecutionLimitException ex) {
			assertTrue(ex.getMessage(), ex.getMessage().contains("time limit"));
		}
		long millis = (System.nanoTime() - start) / 1_000_000;
		assertTrue("stopped after " + millis + " ms", millis < 10000);
	}

	@Test
	public void maxTriples() {
		try {
			PlanFactory.create("PREFIX iter: <http://w3id.org/sparql-generate/iter/> "
					+ "GENERATE { <http://ex/s> <http://ex/p> ?i } ITERATOR iter:for(0, 1, 100) AS ?i")
					.execGenerate(ContextUtils.build().setMaxTriples(10).build());
			fail("the limit of triples stops the execution");
		} catch (ExecutionLimitException ex) {
			assertTrue(ex.getMessage(), ex.getMessage().contains("10 triples"));
		}
	}
}
