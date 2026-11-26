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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.query.ResultSet;
import org.junit.Test;

import fr.mines_stetienne.ci.sparql_generate.RaiseException;
import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.engine.PlanFactory;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;

/**
 * iter:CSVStrict reads a CSV document as the logical sources of RML-IO: an
 * empty cell is the empty string, a row with another number of fields than
 * the header is a data error.
 */
public class CSVStrictTest {

	static {
		SPARQLExt.init();
	}

	private static List<String> rows(String csv, String args, String vars) {
		ResultSet rs = PlanFactory.create("PREFIX iter: <http://w3id.org/sparql-generate/iter/> "
				+ "SELECT * ITERATOR iter:CSVStrict(\"" + csv + "\"" + args + ") AS " + vars + " WHERE { }")
				.execSelect(ContextUtils.build().build());
		List<String> rows = new ArrayList<>();
		while (rs.hasNext()) {
			rows.add(rs.next().toString());
		}
		return rows;
	}

	private static RaiseException error(String csv, String args, String vars) {
		try {
			rows(csv, args, vars);
		} catch (RuntimeException ex) {
			RaiseException raised = RaiseException.findIn(ex);
			assertTrue("not a data error: " + ex, raised != null);
			return raised;
		}
		fail("no error raised");
		return null;
	}

	@Test
	public void emptyCellIsTheEmptyString() {
		assertEquals(List.of("( ?id = \"1\" ) ( ?name = \"a\" ) ( ?row = 0 ) -> ()", "( ?id = \"2\" ) ( ?name = \"\" ) ( ?row = 1 ) -> ()",
				"( ?id = \"3\" ) ( ?name = \"\" ) ( ?row = 2 ) -> ()"),
				rows("id,name\\n1,a\\n2,\\n3,\\\"\\\"", ", \"id\", \"name\"", "?id ?name ?row"));
	}

	@Test
	public void batches() {
		assertEquals(3, rows("id\\n1\\n2\\n3", ", 2, \"id\"", "?id").size());
	}

	@Test
	public void rowWithFewerFieldsIsAnError() {
		RaiseException ex = error("id,name,age\\n6,Phoebe 37", ", \"id\", \"name\"", "?id ?name");
		assertEquals("A row of the CSV document does not have as many fields as the header: \"(inline document)\", 0, 2, 3",
				ex.getMessage());
	}

	@Test
	public void rowWithMoreFieldsIsAnError() {
		error("id,name\\n6,Phoebe,37", ", \"id\"", "?id");
	}

	@Test
	public void unknownColumnIsAnError() {
		RaiseException ex = error("id,name\\n6,Phoebe", ", \"age\"", "?age");
		assertEquals("The CSV document has no column: \"(inline document)\", \"age\"", ex.getMessage());
	}

	@Test
	public void emptyLinesAreSkipped() {
		assertEquals(1, rows("id,name\\n6,Phoebe\\n\\n", ", \"id\"", "?id").size());
	}
}
