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

import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetTime;
import java.time.ZoneOffset;

import org.apache.jena.graph.Node;
import org.junit.Test;

/**
 * The natural RDF literals of two PostgreSQL types that its JDBC driver
 * reports under another JDBC type: timetz as TIME, bit(n) with n &gt; 1 as BIT
 * (a PGobject, not a Boolean). The cell and its metadata are those that
 * PostgreSQL JDBC 42.7.14 gives on PostgreSQL 16.
 */
public class SQLPostgreSQLTypesTest {

	/** One cell of a result set, as the driver gives it. */
	private static Node cell(int jdbcType, String typeName, Object object, String string, Object typed)
			throws SQLException {
		final ResultSetMetaData rsmd = (ResultSetMetaData) Proxy.newProxyInstance(getClass0().getClassLoader(),
				new Class<?>[] { ResultSetMetaData.class }, (proxy, method, args) -> {
					switch (method.getName()) {
					case "getColumnType":
						return jdbcType;
					case "getColumnTypeName":
						return typeName;
					case "getPrecision":
						return 0;
					default:
						throw new UnsupportedOperationException(method.getName());
					}
				});
		final ResultSet rs = (ResultSet) Proxy.newProxyInstance(getClass0().getClassLoader(),
				new Class<?>[] { ResultSet.class }, (proxy, method, args) -> {
					switch (method.getName()) {
					case "getObject":
						if (args.length == 2) {
							if (typed == null || !((Class<?>) args[1]).isInstance(typed)) {
								throw new SQLException("Cannot convert the column of type " + typeName
										+ " to requested type " + ((Class<?>) args[1]).getName());
							}
							return typed;
						}
						return object;
					case "getString":
						return string;
					case "getBoolean":
						throw new SQLException("Cannot cast to boolean: \"" + string + "\"");
					default:
						throw new UnsupportedOperationException(method.getName());
					}
				});
		return ITER_SQL.naturalLiteral(rs, rsmd, 1).asNode();
	}

	private static Class<?> getClass0() {
		return SQLPostgreSQLTypesTest.class;
	}

	@Test
	public void timeWithTimeZoneReportedAsTime() throws SQLException {
		final OffsetTime t = OffsetTime.of(8, 30, 0, 0, ZoneOffset.ofHours(2));
		final Node n = cell(Types.TIME, "timetz", java.sql.Time.valueOf("07:30:00"), "08:30:00+02", t);
		assertEquals("08:30:00+02:00", n.getLiteralLexicalForm());
		assertEquals("http://www.w3.org/2001/XMLSchema#time", n.getLiteralDatatypeURI());
	}

	@Test
	public void timeWithTimeZoneUtc() throws SQLException {
		final OffsetTime t = OffsetTime.of(23, 59, 59, 500_000_000, ZoneOffset.UTC);
		final Node n = cell(Types.TIME, "timetz", java.sql.Time.valueOf("23:59:59"), "23:59:59.5+00", t);
		assertEquals("23:59:59.5Z", n.getLiteralLexicalForm());
	}

	@Test
	public void bitStringOfSeveralBits() throws SQLException {
		final Node n = cell(Types.BIT, "bit", new Object() {
			@Override
			public String toString() {
				return "101";
			}
		}, "101", null);
		assertEquals("101", n.getLiteralLexicalForm());
		assertEquals("http://www.w3.org/2001/XMLSchema#string", n.getLiteralDatatypeURI());
	}

	@Test
	public void bitOfOneBitIsABoolean() throws SQLException {
		final Node n = cell(Types.BIT, "bit", Boolean.TRUE, "1", null);
		assertEquals("true", n.getLiteralLexicalForm());
		assertEquals("http://www.w3.org/2001/XMLSchema#boolean", n.getLiteralDatatypeURI());
	}
}
