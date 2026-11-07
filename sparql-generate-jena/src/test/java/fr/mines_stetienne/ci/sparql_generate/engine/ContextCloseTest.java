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
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.util.Context;
import org.junit.Test;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.iterator.IteratorFunctionBase0;
import fr.mines_stetienne.ci.sparql_generate.iterator.IteratorFunctionRegistry;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;

/**
 * The closing tasks of an execution, such as closing an MQTT or WebSocket
 * client, run at the end of the execution, also when it fails.
 */
public class ContextCloseTest {

	static final AtomicInteger CLOSED = new AtomicInteger();

	static final String OPEN = "http://ex/test-open";

	/**
	 * Gives one solution, and registers a closing task, as a stream iterator
	 * does.
	 */
	public static class ITER_Open extends IteratorFunctionBase0 {
		@Override
		public List<List<NodeValue>> exec() {
			ContextUtils.addTaskOnClose(getContext(), CLOSED::incrementAndGet);
			return Collections.singletonList(Collections.singletonList(NodeValue.makeInteger(1)));
		}
	}

	static {
		SPARQLExt.init();
		IteratorFunctionRegistry.get().put(OPEN, ITER_Open.class);
	}

	@Test
	public void closingTasksRunAfterTheExecution() {
		CLOSED.set(0);
		PlanFactory.create("GENERATE { <http://ex/s> <http://ex/p> ?x } ITERATOR <" + OPEN + ">() AS ?x")
				.execGenerate(ContextUtils.build().build());
		assertEquals(1, CLOSED.get());
	}

	@Test
	public void closingTasksRunWhenTheExecutionFails() {
		CLOSED.set(0);
		try {
			PlanFactory
					.create("GENERATE { <http://ex/s> <http://ex/p> ?x } ITERATOR <" + OPEN + ">() AS ?x "
							+ "SOURCE <http://example.invalid/x> ACCEPT <http://example.org/not-a-media-type> AS ?d")
					.execGenerate(ContextUtils.build().build());
			fail("an ACCEPT that is not a media type is an error");
		} catch (RuntimeException ex) {
			// expected
		}
		assertEquals(1, CLOSED.get());
	}

	/**
	 * The default executor of the stream iterators has daemon threads: an
	 * open stream does not keep the JVM alive.
	 */
	@Test
	public void defaultExecutorHasDaemonThreads() throws Exception {
		Context context = ContextUtils.build().build();
		boolean daemon = ContextUtils.getExecutor(context).submit(() -> Thread.currentThread().isDaemon())
				.get(10, TimeUnit.SECONDS);
		assertTrue(daemon);
		ContextUtils.close(context);
		assertTrue(ContextUtils.getExecutor(context).submit(() -> true).get(10, TimeUnit.SECONDS));
	}
}
