/*
 * Copyright 2020 MINES Saint-Étienne
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
package fr.mines_stetienne.ci.sparql_generate;

import static org.junit.Assert.assertEquals;

import org.apache.jena.rdf.model.Model;
import org.junit.Test;

import fr.mines_stetienne.ci.sparql_generate.engine.PlanFactory;
import fr.mines_stetienne.ci.sparql_generate.stream.LocatorStringMap;
import fr.mines_stetienne.ci.sparql_generate.stream.SPARQLExtStreamManager;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;

/**
 * The CSV and JSON workloads of the benchmark of pysparql-generate, with a
 * nested GENERATE, on 5,000 rows: no row is lost, whatever the batches.
 */
public class NestedScaleTest {

	private static final int ROWS = 5000;

	private static final String PREFIXES = "BASE <http://example.com/> "
			+ "PREFIX iter: <http://w3id.org/sparql-generate/iter/> PREFIX ex: <http://example.com/ns#> ";

	private static Model generate(String query, String uri, String document, String mediaType) {
		SPARQLExt.init();
		LocatorStringMap locator = new LocatorStringMap();
		locator.put(uri, document, mediaType);
		return PlanFactory.create(query).execGenerate(
				ContextUtils.build().setStreamManager(SPARQLExtStreamManager.makeStreamManager(locator)).build());
	}

	/**
	 * Each row has two distinct tags: 2 triples per row, plus 2 in the nested
	 * GENERATE.
	 */
	@Test
	public void csvNested() {
		StringBuilder csv = new StringBuilder("id,name,tags\n");
		for (int i = 0; i < ROWS; i++) {
			csv.append(i).append(",Stop ").append(i).append(",a").append(i).append(";b").append(i).append('\n');
		}
		Model model = generate(PREFIXES + "GENERATE { <stop/{ ?id }> a ex:Stop ; ex:name ?name . "
				+ "GENERATE { <stop/{ ?id }> ex:tag <tag/{ ?tag }> . } ITERATOR iter:Split(?tags, \";\") AS ?tag . } "
				+ "ITERATOR iter:CSV(<http://example.com/data.csv>, \"id\", \"name\", \"tags\") AS ?id ?name ?tags",
				"http://example.com/data.csv", csv.toString(), "text/csv");
		assertEquals(4 * ROWS, model.size());
	}

	@Test
	public void jsonNested() {
		StringBuilder json = new StringBuilder("[");
		for (int i = 0; i < ROWS; i++) {
			json.append(i == 0 ? "" : ",").append("{\"id\": ").append(i).append(", \"name\": \"Stop ").append(i)
					.append("\", \"tags\": [\"a").append(i).append("\", \"b").append(i).append("\"]}");
		}
		json.append("]");
		Model model = generate(PREFIXES + "GENERATE { <stop/{ ?id }> a ex:Stop ; ex:name ?name . "
				+ "GENERATE { <stop/{ ?id }> ex:tag <tag/{ ?tag }> . } ITERATOR iter:JSONPath(?e, \"$.tags[*]\") AS ?tag . } "
				+ "ITERATOR iter:JSONPath(<http://example.com/data.json>, \"$[*]\", \"$.id\", \"$.name\") AS ?e ?id ?name",
				"http://example.com/data.json", json.toString(), "application/json");
		assertEquals(4 * ROWS, model.size());
	}
}
