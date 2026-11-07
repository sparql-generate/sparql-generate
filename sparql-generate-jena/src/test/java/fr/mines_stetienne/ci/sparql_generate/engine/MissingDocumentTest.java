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

import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.rdf.model.Model;
import org.junit.Test;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.stream.DocumentNotFoundException;
import fr.mines_stetienne.ci.sparql_generate.stream.LocatorAcceptBase;
import fr.mines_stetienne.ci.sparql_generate.stream.LookUpRequest;
import fr.mines_stetienne.ci.sparql_generate.stream.SPARQLExtStreamManager;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;

/**
 * A document that cannot be found: by default the variable of the SOURCE
 * clause is unbound and the graph of the FROM clause is empty; with
 * {@link SPARQLExtStreamManager#setFailOnMissingDocument(boolean)}, the
 * execution fails.
 */
public class MissingDocumentTest {

	static {
		SPARQLExt.init();
	}

	private static final String MISSING = "urn:x-test:missing";

	/**
	 * Finds nothing for MISSING, and does not go further (no network).
	 */
	private static SPARQLExtStreamManager streamManager(boolean failOnMissing) {
		SPARQLExtStreamManager sm = SPARQLExtStreamManager.makeStreamManager(new LocatorAcceptBase() {
			@Override
			public TypedInputStream open(LookUpRequest request) {
				return null;
			}

			@Override
			public String getName() {
				return "Nothing";
			}
		});
		sm.setFailOnMissingDocument(failOnMissing);
		return sm;
	}

	private static Model generate(String query, boolean failOnMissing) {
		return PlanFactory.create(query)
				.execGenerate(ContextUtils.build().setStreamManager(streamManager(failOnMissing)).build());
	}

	private static final String SOURCE = "GENERATE { <http://ex/s> <http://ex/p> 1 . <http://ex/s> <http://ex/doc> ?d } "
			+ "SOURCE <" + MISSING + "> AS ?d";

	private static final String FROM = "GENERATE { <http://ex/s> <http://ex/p> ?o } FROM <" + MISSING + "> "
			+ "WHERE { OPTIONAL { ?s ?p ?x } BIND(1 AS ?o) }";

	@Test
	public void missingSourceLeavesTheVariableUnbound() {
		Model model = generate(SOURCE, false);
		assertEquals(EngineTestUtils.write(model), 1, model.size());
	}

	@Test
	public void missingSourceFails() {
		assertFails(SOURCE);
	}

	@Test
	public void missingGraphIsEmpty() {
		Model model = generate(FROM, false);
		assertEquals(EngineTestUtils.write(model), 1, model.size());
	}

	@Test
	public void missingGraphFails() {
		assertFails(FROM);
	}

	private static void assertFails(String query) {
		try {
			generate(query, true);
			fail("a missing document stops the execution");
		} catch (RuntimeException ex) {
			DocumentNotFoundException notFound = DocumentNotFoundException.findIn(ex);
			assertTrue(ex.toString(), notFound != null);
			assertEquals(MISSING, notFound.getUri());
		}
	}
}
