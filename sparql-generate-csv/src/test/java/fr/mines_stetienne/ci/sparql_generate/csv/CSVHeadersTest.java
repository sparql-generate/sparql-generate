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

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.query.ResultSet;
import org.junit.Test;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.engine.PlanFactory;
import fr.mines_stetienne.ci.sparql_generate.stream.LocatorStringMap;
import fr.mines_stetienne.ci.sparql_generate.stream.SPARQLExtStreamManager;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;

/**
 * iter:CSVHeaders on the text of a CSV document, or on its IRI, of which it
 * reads the header line only; with RAISE, the check that a column exists.
 */
public class CSVHeadersTest {

	static {
		SPARQLExt.init();
	}

	private static final String CSV = "id,\"full name\",city\n1,Ada,London\n2,Alan,\"Wilmslow\"\n";

	private static List<String> headers(String source) {
		LocatorStringMap locator = new LocatorStringMap();
		locator.put("http://ex/people.csv", CSV, "text/csv");
		SPARQLExtStreamManager sm = SPARQLExtStreamManager.makeStreamManager(locator);
		ResultSet rs = PlanFactory.create("PREFIX iter: <http://w3id.org/sparql-generate/iter/> "
				+ "SELECT ?h ITERATOR iter:CSVHeaders(" + source + ") AS ?h WHERE { }")
				.execSelect(ContextUtils.build().setStreamManager(sm).build());
		List<String> headers = new ArrayList<>();
		while (rs.hasNext()) {
			headers.add(rs.next().getLiteral("h").getString());
		}
		return headers;
	}

	@Test
	public void ofText() {
		assertEquals(List.of("id", "full name", "city"), headers("\"id,\\\"full name\\\",city\\n1,Ada,London\""));
	}

	@Test
	public void ofIri() {
		assertEquals(List.of("id", "full name", "city"), headers("<http://ex/people.csv>"));
	}
}
