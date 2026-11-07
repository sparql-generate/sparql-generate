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
package fr.mines_stetienne.ci.sparql_generate.sql;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import org.apache.jena.query.ResultSet;
import org.junit.Test;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.engine.PlanFactory;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;

/**
 * iter:SQL on an embedded H2 database, whose user and password are in the
 * JDBC URL.
 */
public class SQLTest {

	static {
		SPARQLExt.init();
	}

	@Test
	public void rowsOfAnyJdbcDriverInOneBatch() throws Exception {
		String url = "jdbc:h2:mem:sqltest;DB_CLOSE_DELAY=-1;USER=me;PASSWORD=secret";
		try (Connection conn = DriverManager.getConnection(url); Statement st = conn.createStatement()) {
			st.execute("CREATE TABLE people (id INT, name VARCHAR(20))");
			st.execute("INSERT INTO people VALUES (1, 'a'), (2, 'b'), (3, 'c')");
			// one batch: the aggregate counts every row
			ResultSet rs = PlanFactory
					.create("PREFIX iter: <http://w3id.org/sparql-generate/iter/> SELECT (COUNT(?id) AS ?n) "
							+ "ITERATOR iter:SQL(<" + url + ">, \"SELECT id, name FROM people\") AS ?id ?name WHERE { }")
					.execSelect(ContextUtils.build().build());
			assertEquals(3, rs.next().getLiteral("n").getInt());
		}
	}
}
