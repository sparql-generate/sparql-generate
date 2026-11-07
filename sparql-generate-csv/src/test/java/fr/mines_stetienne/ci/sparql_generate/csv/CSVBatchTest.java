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
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;

/**
 * iter:CSV with a batch size: the rest of the query is evaluated once per
 * batch.
 */
public class CSVBatchTest {

	static {
		SPARQLExt.init();
	}

	private static List<Integer> counts(String csv, int batch) {
		ResultSet rs = PlanFactory.create("PREFIX iter: <http://w3id.org/sparql-generate/iter/> "
				+ "SELECT (COUNT(?a) AS ?n) ITERATOR iter:CSV(\"" + csv + "\", " + batch + ", \"a\") AS ?a WHERE { }")
				.execSelect(ContextUtils.build().build());
		List<Integer> counts = new ArrayList<>();
		while (rs.hasNext()) {
			counts.add(rs.next().getLiteral("n").getInt());
		}
		return counts;
	}

	/**
	 * No empty last batch when the number of rows is a multiple of the size of
	 * a batch: an aggregate would give one more solution, with COUNT 0.
	 */
	@Test
	public void noEmptyLastBatch() {
		assertEquals(List.of(2, 2), counts("a\\n1\\n2\\n3\\n4", 2));
		assertEquals(List.of(2, 2, 1), counts("a\\n1\\n2\\n3\\n4\\n5", 2));
	}

	@Test
	public void emptyDocumentGivesOneEmptyBatch() {
		assertEquals(List.of(0), counts("a\\n", 2));
	}
}
