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
package fr.mines_stetienne.ci.sparql_generate.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import fr.mines_stetienne.ci.sparql_generate.RaiseException;
import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.engine.PlanFactory;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;

/**
 * A RAISE evaluated for a value of iter:JSONSurfer, in the rest of the
 * query, stops the execution: the iterator neither logs it as an error of the
 * document nor wraps it.
 */
public class JSONSurferRaiseTest {

	static {
		SPARQLExt.init();
	}

	private final List<LoggingEvent> events = new ArrayList<>();

	private final AppenderSkeleton appender = new AppenderSkeleton() {
		@Override
		protected void append(LoggingEvent event) {
			events.add(event);
		}

		@Override
		public void close() {
		}

		@Override
		public boolean requiresLayout() {
			return false;
		}
	};

	@Before
	public void listen() {
		Logger.getRootLogger().addAppender(appender);
	}

	@After
	public void stopListening() {
		Logger.getRootLogger().removeAppender(appender);
	}

	private void assertRaisedSilently(String query, String message) {
		try {
			PlanFactory.create("PREFIX iter: <http://w3id.org/sparql-generate/iter/> PREFIX ex: <http://ex.org/> " + query)
					.execGenerate(ContextUtils.build().build());
			fail("no error raised");
		} catch (RuntimeException ex) {
			RaiseException raised = RaiseException.findIn(ex);
			assertTrue("not a RaiseException: " + ex, raised != null);
			assertSame("wrapped: " + ex, raised, ex);
			assertEquals(message, raised.getMessage());
		}
		for (LoggingEvent event : events) {
			assertTrue("logged: " + event.getLevel() + " " + event.getRenderedMessage(),
					event.getLevel().toInt() < Level.WARN_INT);
		}
	}

	@Test
	public void raiseInBindingClause() {
		assertRaisedSilently("GENERATE { ex:s ex:p ?x } ITERATOR iter:JSONSurfer('[\\{\"id\": 1\\}, \\{\"id\": 2\\}]', \"$[*]\", \"$.id\") AS ?o ?id "
				+ "BIND(IF(?id = 2, RAISE(\"stop\", ?id), 1) AS ?x)", "stop: 2");
	}

	@Test
	public void raiseInBatch() {
		assertRaisedSilently("GENERATE { ex:s ex:p ?x } ITERATOR iter:JSONSurfer('[\\{\"id\": 1\\}, \\{\"id\": 2\\}]', \"$[*]\", 1, \"$.id\") AS ?o ?id "
				+ "WHERE { BIND(IF(?id = 2, RAISE(\"stop\", ?id), 1) AS ?x) }", "stop: 2");
	}
}
