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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.rdf.model.Model;
import org.junit.Test;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.engine.PlanFactory;
import fr.mines_stetienne.ci.sparql_generate.stream.DocumentNotFoundException;
import fr.mines_stetienne.ci.sparql_generate.stream.LocatorAcceptBase;
import fr.mines_stetienne.ci.sparql_generate.stream.LookUpRequest;
import fr.mines_stetienne.ci.sparql_generate.stream.SPARQLExtStreamManager;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;

/**
 * An iterator over a JSON document that cannot be found.
 */
public class MissingJSONDocumentTest {

	static {
		SPARQLExt.init();
	}

	private static final String QUERY = "PREFIX iter: <http://w3id.org/sparql-generate/iter/> "
			+ "GENERATE { <http://ex/s> <http://ex/p> ?x } "
			+ "ITERATOR iter:JSONPath(<urn:x-test:missing.json>, \"$[*]\") AS ?x";

	private static Model generate(boolean failOnMissing) {
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
		return PlanFactory.create(QUERY).execGenerate(ContextUtils.build().setStreamManager(sm).build());
	}

	@Test
	public void noSolutionByDefault() {
		assertTrue(generate(false).isEmpty());
	}

	@Test
	public void failsIfRequested() {
		try {
			generate(true);
			fail("a missing document stops the execution");
		} catch (RuntimeException ex) {
			DocumentNotFoundException notFound = DocumentNotFoundException.findIn(ex);
			assertTrue(ex.toString(), notFound != null);
			assertEquals("urn:x-test:missing.json", notFound.getUri());
		}
	}
}
