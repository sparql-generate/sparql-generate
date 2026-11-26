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

import static fr.mines_stetienne.ci.sparql_generate.engine.EngineTestUtils.generate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.StringWriter;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.Test;

/**
 * fun:unsafeIRI(str) returns the IRI of a string as it is, without checking
 * it (the term type rml:UnsafeIRI of RML); IRI(str) still rejects an invalid
 * IRI.
 */
public class UnsafeIRIFunctionTest {

	private static final String PREFIX = "PREFIX fun: <http://w3id.org/sparql-generate/fn/> ";

	private static Set<String> subjects(Model model) {
		return model.listSubjects().mapWith(r -> r.getURI()).toSet().stream().collect(Collectors.toSet());
	}

	@Test
	public void keepsTheStringAsItIs() {
		Model model = generate(PREFIX + "GENERATE { ?s a <http://ex/C> } WHERE { "
				+ "VALUES ?v { \"http://ex/Emily Smith\" \"http://ex/Bob/Charles\" \"http://ex/Zoë Krüger\" } "
				+ "BIND(fun:unsafeIRI(?v) AS ?s) }");
		assertEquals(Set.of("http://ex/Emily Smith", "http://ex/Bob/Charles", "http://ex/Zoë Krüger"), subjects(model));
	}

	@Test
	public void iriStillRejectsASpace() {
		Model model = generate("GENERATE { ?s a <http://ex/C> } WHERE { "
				+ "BIND(IRI(\"http://ex/Emily Smith\") AS ?s) }");
		assertEquals(0, model.size());
	}

	@Test
	public void relativeStringIsAnError() {
		Model model = generate(PREFIX + "BASE <http://ex/> GENERATE { ?s a <http://ex/C> } WHERE { "
				+ "BIND(fun:unsafeIRI(\"Emily Smith\") AS ?s) }");
		assertEquals(0, model.size());
	}

	@Test
	public void iriUnchanged() {
		Model model = generate(PREFIX + "GENERATE { ?s a <http://ex/C> } WHERE { "
				+ "BIND(fun:unsafeIRI(<http://ex/a>) AS ?s) }");
		assertEquals(Set.of("http://ex/a"), subjects(model));
	}

	@Test
	public void nTriplesEscapesTheSpace() {
		Model model = generate(PREFIX + "GENERATE { ?s a <http://ex/C> } WHERE { "
				+ "BIND(fun:unsafeIRI(\"http://ex/Emily Smith\") AS ?s) }");
		StringWriter out = new StringWriter();
		RDFDataMgr.write(out, model, Lang.NTRIPLES);
		assertTrue(out.toString(), out.toString().startsWith("<http://ex/Emily\\u0020Smith> "));
	}
}
