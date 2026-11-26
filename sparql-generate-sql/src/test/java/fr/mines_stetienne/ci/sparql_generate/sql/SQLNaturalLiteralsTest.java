/*
 * Copyright 2026 École des Mines de Saint-Étienne.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.graph.Node;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.mines_stetienne.ci.sparql_generate.RaiseException;
import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.engine.PlanFactory;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;

/**
 * iter:SQL gives the natural RDF literals of R2RML, binds named columns, and
 * stops on a query that the database rejects.
 */
public class SQLNaturalLiteralsTest {

	private static final String URL = "jdbc:h2:mem:sqlnatural;DB_CLOSE_DELAY=-1";

	static {
		SPARQLExt.init();
	}

	@BeforeClass
	public static void createTable() throws Exception {
		try (Connection conn = DriverManager.getConnection(URL, "me", "secret"); Statement st = conn.createStatement()) {
			st.execute("CREATE TABLE patient (id INTEGER, big BIGINT, name VARCHAR(20), code CHAR(6), "
					+ "weight REAL, height DOUBLE PRECISION, price DECIMAL(10,2), paid BOOLEAN, birth DATE, "
					+ "arrival TIMESTAMP, alarm TIME, photo VARBINARY(8), note VARCHAR(20))");
			st.execute("INSERT INTO patient VALUES (10, 12345678901234, 'Monica', 'ab', 80.25, 1.65, 30.00, FALSE, "
					+ "DATE '1981-10-10', TIMESTAMP '2009-10-10 12:12:22', TIME '08:30:00', X'89504E47', NULL)");
			st.execute("INSERT INTO patient VALUES (11, -1, 'Rachel', 'abcdef', 70.22, 30, 0.5, TRUE, "
					+ "DATE '1982-11-12', TIMESTAMP '2008-11-12 09:45:44.25', TIME '23:59:59', X'00FF', '')");
		}
	}

	private static String url() {
		return URL + ";USER=me;PASSWORD=secret";
	}

	/** The values of the row of patient id, column by column. */
	private static Map<String, Node> row(String columns, int id) {
		String[] names = columns.split(" ");
		StringBuilder args = new StringBuilder();
		StringBuilder vars = new StringBuilder();
		for (String n : names) {
			args.append(", \"").append(n).append("\"");
			vars.append(" ?").append(n);
		}
		ResultSet rs = PlanFactory
				.create("PREFIX iter: <http://w3id.org/sparql-generate/iter/> SELECT * ITERATOR iter:SQL(\"" + url()
						+ "\", \"SELECT * FROM patient WHERE id = " + id + "\"" + args + ") AS" + vars + " WHERE { }")
				.execSelect(ContextUtils.build().build());
		assertTrue(rs.hasNext());
		QuerySolution qs = rs.next();
		Map<String, Node> row = new HashMap<>();
		for (String n : names) {
			RDFNode v = qs.get(n);
			row.put(n, v == null ? null : v.asNode());
		}
		assertFalse(rs.hasNext());
		return row;
	}

	private static void assertLiteral(String lexicalForm, String datatype, Node node) {
		assertNotNull(node);
		assertEquals(lexicalForm, node.getLiteralLexicalForm());
		assertEquals("http://www.w3.org/2001/XMLSchema#" + datatype, node.getLiteralDatatypeURI());
	}

	@Test
	public void naturalLiteralsOfR2RML() {
		Map<String, Node> r = row("ID BIG NAME CODE WEIGHT HEIGHT PRICE PAID BIRTH ARRIVAL ALARM PHOTO NOTE", 10);
		assertLiteral("10", "integer", r.get("ID"));
		assertLiteral("12345678901234", "integer", r.get("BIG"));
		assertLiteral("Monica", "string", r.get("NAME"));
		// CHAR(6): padded with spaces
		assertLiteral("ab    ", "string", r.get("CODE"));
		assertLiteral("8.025E1", "double", r.get("WEIGHT"));
		assertLiteral("1.65E0", "double", r.get("HEIGHT"));
		assertLiteral("30.0", "decimal", r.get("PRICE"));
		assertLiteral("false", "boolean", r.get("PAID"));
		assertLiteral("1981-10-10", "date", r.get("BIRTH"));
		assertLiteral("2009-10-10T12:12:22", "dateTime", r.get("ARRIVAL"));
		assertLiteral("08:30:00", "time", r.get("ALARM"));
		assertLiteral("89504E47", "hexBinary", r.get("PHOTO"));
		// NULL: unbound
		assertEquals(null, r.get("NOTE"));
	}

	@Test
	public void canonicalFormsAndEmptyString() {
		Map<String, Node> r = row("BIG WEIGHT HEIGHT PRICE PAID ARRIVAL PHOTO NOTE", 11);
		assertLiteral("-1", "integer", r.get("BIG"));
		// single precision: the decimal value of the float, not of its double
		assertLiteral("7.022E1", "double", r.get("WEIGHT"));
		assertLiteral("3.0E1", "double", r.get("HEIGHT"));
		assertLiteral("0.5", "decimal", r.get("PRICE"));
		assertLiteral("true", "boolean", r.get("PAID"));
		assertLiteral("2008-11-12T09:45:44.25", "dateTime", r.get("ARRIVAL"));
		assertLiteral("00FF", "hexBinary", r.get("PHOTO"));
		// the empty string is a value, not NULL
		assertLiteral("", "string", r.get("NOTE"));
	}

	@Test
	public void namedColumnsInTheirOrderRegardlessOfCase() {
		Map<String, Node> r = row("name id", 10);
		assertLiteral("Monica", "string", r.get("name"));
		assertLiteral("10", "integer", r.get("id"));
	}

	@Test
	public void canonicalDouble() {
		assertEquals("8.025E1", ITER_SQL.canonicalDouble(new BigDecimal("80.25")));
		assertEquals("1.0E0", ITER_SQL.canonicalDouble(new BigDecimal("1")));
		assertEquals("3.0E1", ITER_SQL.canonicalDouble(new BigDecimal("30.000")));
		assertEquals("-1.5E-3", ITER_SQL.canonicalDouble(new BigDecimal("-0.0015")));
		assertEquals("0.0E0", ITER_SQL.canonicalDouble(BigDecimal.ZERO));
		assertEquals("30.0", ITER_SQL.canonicalDecimal(new BigDecimal("30.00")));
		assertEquals("-0.25", ITER_SQL.canonicalDecimal(new BigDecimal("-0.250")));
		assertEquals("100.0", ITER_SQL.canonicalDecimal(new BigDecimal("1E+2")));
	}

	@Test
	public void passwordRedacted() {
		assertEquals("jdbc:postgresql://h/db?user=u&password=***",
				ITER_SQL.redact("jdbc:postgresql://h/db?user=u&password=s3cret"));
		assertEquals("jdbc:sqlserver://h;user=sa;password=***;encrypt=false",
				ITER_SQL.redact("jdbc:sqlserver://h;user=sa;password={a;b}}c};encrypt=false"));
	}

	private static void assertRaised(String query, String message) {
		try {
			PlanFactory.create("PREFIX iter: <http://w3id.org/sparql-generate/iter/> PREFIX ex: <http://ex.org/> " + query)
					.execGenerate(ContextUtils.build().build());
			fail("no error raised");
		} catch (RuntimeException ex) {
			RaiseException raised = RaiseException.findIn(ex);
			assertNotNull("not a RaiseException: " + ex, raised);
			assertTrue(raised.getMessage(), raised.getMessage().startsWith(message));
		}
	}

	@Test
	public void unknownColumnIsAnError() {
		assertRaised("GENERATE { ex:s ex:p ?x } ITERATOR iter:SQL(\"" + url()
				+ "\", \"SELECT id FROM patient\", \"nocolumn\") AS ?x",
				"SQL error: no such column in the result of the query");
	}

	@Test
	public void duplicateColumnIsAnError() {
		assertRaised("GENERATE { ex:s ex:p ?x } ITERATOR iter:SQL(\"" + url()
				+ "\", \"SELECT name, name FROM patient\", \"NAME\") AS ?x",
				"SQL error: several columns of the result of the query have this name");
	}

	@Test
	public void rejectedQueryIsAnError() {
		assertRaised("GENERATE { ex:s ex:p ?x } ITERATOR iter:SQL(\"" + url()
				+ "\", \"SELECT id FROM nosuchtable\") AS ?x", "SQL error: ");
		assertRaised("GENERATE { ex:s ex:p ?x } ITERATOR iter:SQL(\"" + url()
				+ "\", \"SELEC id FROM patient\") AS ?x", "SQL error: ");
	}
}
