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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.rdf.model.Model;
import org.junit.Test;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.SPARQLExtException;
import fr.mines_stetienne.ci.sparql_generate.stream.LocatorAcceptBase;
import fr.mines_stetienne.ci.sparql_generate.stream.LookUpRequest;
import fr.mines_stetienne.ci.sparql_generate.stream.SPARQLExtStreamManager;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;

/**
 * How the IRI of a function or an iterator is resolved.
 */
public class FunctionRegistryTest {

	static {
		SPARQLExt.init();
	}

	/**
	 * Records the requests, and finds nothing.
	 */
	private static class RecordingLocator extends LocatorAcceptBase {
		final List<String> requests = Collections.synchronizedList(new ArrayList<>());

		@Override
		public TypedInputStream open(LookUpRequest request) {
			requests.add(request.getFilenameOrURI());
			return null;
		}

		@Override
		public String getName() {
			return "RecordingLocator";
		}
	}

	/**
	 * The functions of Jena that it loads by class name, such as afn:bnode and
	 * afn:localname, are available.
	 */
	@Test
	public void jenaFunctionsLoadedByClassName() {
		assertGenerates("<http://ex/s> <http://ex/p> true . <http://ex/s> <http://ex/q> \"x\" .",
				"PREFIX afn: <http://jena.apache.org/ARQ/function#> "
						+ "GENERATE { <http://ex/s> <http://ex/p> ?ok . <http://ex/s> <http://ex/q> ?l } "
						+ "WHERE { BIND(BNODE() AS ?b) BIND(STRLEN(afn:bnode(?b)) > 0 AS ?ok) "
						+ "BIND(afn:localname(<http://ex/x>) AS ?l) }");
	}

	/**
	 * An unknown function in the namespace of SPARQL, XPath, Jena or
	 * SPARQL-Generate is not looked up as a FUNCTION query, which would be a
	 * request on the network.
	 */
	@Test
	public void unknownBuiltInFunctionsAreNotLookedUp() {
		RecordingLocator locator = new RecordingLocator();
		SPARQLExtStreamManager sm = SPARQLExtStreamManager.makeStreamManager(locator);
		Model model = PlanFactory
				.create("PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "
						+ "PREFIX fun: <http://w3id.org/sparql-generate/fn/> "
						+ "GENERATE { <http://ex/s> <http://ex/p> 1 . <http://ex/s> <http://ex/q> ?a, ?b } "
						+ "WHERE { BIND(xsd:noSuchType(1) AS ?a) BIND(fun:noSuchFunction(1) AS ?b) }")
				.execGenerate(ContextUtils.build().setStreamManager(sm).build());
		assertEquals(locator.requests.toString(), 0, locator.requests.size());
		assertEquals(EngineTestUtils.write(model), 1, model.size());
	}

	/**
	 * Same for an iterator; an unknown iterator is an error.
	 */
	@Test
	public void unknownBuiltInIteratorIsNotLookedUp() {
		RecordingLocator locator = new RecordingLocator();
		SPARQLExtStreamManager sm = SPARQLExtStreamManager.makeStreamManager(locator);
		try {
			PlanFactory
					.create("PREFIX iter: <http://w3id.org/sparql-generate/iter/> "
							+ "GENERATE { <http://ex/s> <http://ex/p> ?i } ITERATOR iter:noSuchIterator(1) AS ?i")
					.execGenerate(ContextUtils.build().setStreamManager(sm).build());
			fail("an unknown iterator is an error");
		} catch (SPARQLExtException ex) {
			assertTrue(ex.getMessage(), ex.getMessage().contains("noSuchIterator"));
		}
		assertEquals(locator.requests.toString(), 0, locator.requests.size());
	}

	/**
	 * A function IRI in another namespace is still looked up as a FUNCTION
	 * query.
	 */
	@Test
	public void otherIrisAreLookedUp() {
		RecordingLocator locator = new RecordingLocator();
		SPARQLExtStreamManager sm = SPARQLExtStreamManager.makeStreamManager(locator);
		PlanFactory.create("GENERATE { <http://ex/s> <http://ex/p> ?a } WHERE { BIND(<urn:x-test:f>(1) AS ?a) }")
				.execGenerate(ContextUtils.build().setStreamManager(sm).build());
		assertTrue(locator.requests.toString(), locator.requests.contains("urn:x-test:f"));
	}
}
