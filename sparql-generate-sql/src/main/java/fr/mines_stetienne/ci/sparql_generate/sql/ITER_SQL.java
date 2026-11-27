/*
 * Copyright 2020 Ecole des Mines de Saint-Etienne.
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

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.function.Consumer;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.riot.SysRIOT;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.util.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.mines_stetienne.ci.sparql_generate.RaiseException;
import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.iterator.DownstreamException;
import fr.mines_stetienne.ci.sparql_generate.iterator.IteratorStreamFunctionBase;
import fr.mines_stetienne.ci.sparql_generate.stream.DocumentNotFoundException;
import fr.mines_stetienne.ci.sparql_generate.stream.SPARQLExtStreamManager;

/**
 * Iterator function <a href="http://w3id.org/sparql-generate/iter/SQL">SQL</a>
 * issues a SQL query to a database.
 *
 * <ul>
 * <li>Param 1: (database): the JDBC URL of the database, an IRI or a string,
 * with its user and password if it needs them, such as
 * {@code <jdbc:mysql://localhost/db?user=me&password=secret>},
 * {@code "jdbc:postgresql://localhost/db?user=me&password=secret"},
 * {@code "jdbc:sqlserver://localhost;databaseName=db;user=me;password={secret}"}
 * or {@code <jdbc:sqlite:data.db>}. Any JDBC driver of the classpath is used;
 * the drivers of PostgreSQL, MySQL and SQL Server are included.</li>
 * <li>Param 2: (sql) the SQL query;</li>
 * <li>Params 3 .. N (optional): the names of the columns to bind, in the order
 * of the output variables. A name is the label of a column of the result,
 * compared exactly, else regardless of case (as JDBC does). A name that no
 * column has, or that several columns have, is an error that stops the
 * execution. Without names, the columns of the result are bound in their
 * order.</li>
 * </ul>
 * The rows of the result are one batch.
 *
 * The following variables may be bound:
 *
 * <ul>
 * <li>Output 1 .. N: the value of each column, as the natural RDF literal of
 * R2RML (https://www.w3.org/TR/r2rml/#natural-mapping), in the canonical
 * lexical form of its datatype: an integer type gives {@code xsd:integer},
 * {@code DECIMAL} and {@code NUMERIC} {@code xsd:decimal}, {@code REAL},
 * {@code FLOAT} and {@code DOUBLE} {@code xsd:double}, {@code BOOLEAN} (and a
 * {@code BIT} of one bit) {@code xsd:boolean}, {@code DATE} {@code xsd:date},
 * {@code TIME} {@code xsd:time}, {@code TIMESTAMP} {@code xsd:dateTime}, the
 * binary types {@code xsd:hexBinary}, and the other types a string. A
 * {@code CHAR(n)} value is padded with spaces to n characters. A SQL
 * {@code NULL} leaves the variable unbound.</li>
 * </ul>
 *
 * A query that the database rejects (a syntax error, an unknown table or
 * column) is an error that stops the execution. A database that cannot be
 * reached gives no row, with a warning, as a missing document; with
 * {@code --fail-on-missing}, it stops the execution.
 *
 * @author Omar Qawasmeh, Maxime Lefrançois
 *
 * @organization Ecole des Mines de Saint Etienne
 */
public class ITER_SQL extends IteratorStreamFunctionBase {

	/**
	 * The logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(ITER_SQL.class);

	/**
	 * The SPARQL function URI.
	 */
	public static final String URI = SPARQLExt.ITER + "SQL";

	/**
	 * The type of a SQL Server {@code DATETIMEOFFSET} column
	 * ({@code microsoft.sql.Types.DATETIMEOFFSET}).
	 */
	private static final int SQLSERVER_DATETIMEOFFSET = -155;

	/**
	 * The connection properties: none, the user and password are those of the
	 * JDBC URL; a MySQL connection is in UTC.
	 */
	public static Properties getProperties(String url) {
		final Properties properties = new Properties();
		if (url.startsWith("jdbc:mysql:") && !url.contains("serverTimezone=")) {
			properties.setProperty("serverTimezone", "UTC");
		}
		return properties;
	}

	/**
	 * The JDBC URL without its password, for the messages.
	 */
	static String redact(String url) {
		return url.replaceAll("(?i)(password=)(\\{(?:[^}]|\\}\\})*\\}|[^;&]*)", "$1***");
	}

	@Override
	public void checkBuild(ExprList args) {
	}

	@Override
	public void exec(List<NodeValue> args, Consumer<List<List<NodeValue>>> consumer) {
		if (args.size() < 2 || args.get(0) == null || args.get(1) == null) {
			LOG.debug("Must have two arguments, the URI to the data base and the SQL query");
			throw new ExprEvalException("Must have two arguments, the URI to the data base and the SQL query");
		}
		final NodeValue database = args.get(0);
		final String url;
		if (database.isIRI()) {
			url = database.asNode().getURI();
		} else if (database.isString()) {
			url = database.getString();
		} else {
			throw new ExprEvalException("First argument must be the JDBC URL of the data base, an IRI or a string");
		}
		if (!args.get(1).isString()) {
			throw new ExprEvalException("Second argument (the query) must be a String");
		}
		final String query = args.get(1).getString();
		final List<String> names = new ArrayList<>();
		for (int i = 2; i < args.size(); i++) {
			if (args.get(i) == null || !args.get(i).isString()) {
				throw new ExprEvalException("The names of the columns must be strings, got: " + args.get(i));
			}
			names.add(args.get(i).getString());
		}

		LOG.trace("Executing SQL on the data base " + redact(url) + " with query:\t" + query);
		final Connection connection;
		try {
			// JDBC 4 drivers of the classpath register themselves
			connection = DriverManager.getConnection(url, getProperties(url));
		} catch (SQLException ex) {
			// as a missing document
			if (failOnMissing()) {
				throw new DocumentNotFoundException(redact(url));
			}
			LOG.warn("Can not connect to the data base " + redact(url) + ": " + ex.getMessage());
			throw new ExprEvalException("Can not connect to the data base", ex);
		}
		try (Connection conn = connection) {
			final List<List<NodeValue>> rows;
			try (PreparedStatement ps = conn.prepareStatement(query); ResultSet rs = ps.executeQuery()) {
				final ResultSetMetaData rsmd = rs.getMetaData();
				final int[] columns = columns(rsmd, names, query);
				// one batch, as the other iterators without a batch size
				rows = new ArrayList<>();
				while (rs.next()) {
					final List<NodeValue> row = new ArrayList<>(columns.length);
					for (int column : columns) {
						row.add(naturalLiteral(rs, rsmd, column));
					}
					rows.add(row);
				}
			} catch (SQLException ex) {
				throw new RaiseException("SQL error: " + ex.getMessage(), List.of(NodeFactory.createLiteralString(query)));
			}
			consumer.accept(rows);
		} catch (SQLException ex) {
			// closing the connection
			LOG.debug("Can not close the connection to the data base", ex);
		} catch (RaiseException ex) {
			throw ex;
		} catch (Exception ex) {
			// RAISE, or an error of the rest of the query, is not an error of the data base
			DownstreamException.rethrowIfNotOwn(ex);
			throw ex;
		}
	}

	private boolean failOnMissing() {
		try {
			final Context context = getContext();
			final Object sm = context == null ? null : context.get(SysRIOT.sysStreamManager);
			return sm instanceof SPARQLExtStreamManager && ((SPARQLExtStreamManager) sm).isFailOnMissingDocument();
		} catch (RuntimeException ex) {
			return false;
		}
	}

	/**
	 * The indexes of the columns to bind: the named ones, or all of them.
	 */
	private static int[] columns(ResultSetMetaData rsmd, List<String> names, String query) throws SQLException {
		final int count = rsmd.getColumnCount();
		if (names.isEmpty()) {
			final int[] all = new int[count];
			for (int i = 0; i < count; i++) {
				all[i] = i + 1;
			}
			return all;
		}
		final int[] columns = new int[names.size()];
		for (int n = 0; n < names.size(); n++) {
			final String name = names.get(n);
			int exact = 0, exactCount = 0, folded = 0, foldedCount = 0;
			for (int i = 1; i <= count; i++) {
				final String label = rsmd.getColumnLabel(i);
				if (name.equals(label)) {
					exact = i;
					exactCount++;
				} else if (name.equalsIgnoreCase(label)) {
					folded = i;
					foldedCount++;
				}
			}
			if (exactCount > 1 || (exactCount == 0 && foldedCount > 1)) {
				throw new RaiseException("SQL error: several columns of the result of the query have this name",
						List.of(NodeFactory.createLiteralString(name), NodeFactory.createLiteralString(query)));
			}
			if (exactCount == 0 && foldedCount == 0) {
				throw new RaiseException("SQL error: no such column in the result of the query",
						List.of(NodeFactory.createLiteralString(name), NodeFactory.createLiteralString(query)));
			}
			columns[n] = exactCount == 1 ? exact : folded;
		}
		return columns;
	}

	private static NodeValue literal(String lexicalForm, RDFDatatype datatype) {
		final Node node = NodeFactory.createLiteralDT(lexicalForm, datatype);
		return NodeValue.makeNode(node);
	}

	/**
	 * The natural RDF literal of a cell (R2RML, section 10.2), or null for
	 * NULL.
	 */
	static NodeValue naturalLiteral(ResultSet rs, ResultSetMetaData rsmd, int i) throws SQLException {
		final Object value = rs.getObject(i);
		if (value == null) {
			return null;
		}
		final int type = rsmd.getColumnType(i);
		switch (type) {
		case Types.NULL:
			return null;
		case Types.BOOLEAN:
			return literal(rs.getBoolean(i) ? "true" : "false", XSDDatatype.XSDboolean);
		case Types.BIT:
			if (value instanceof Boolean) {
				return literal((Boolean) value ? "true" : "false", XSDDatatype.XSDboolean);
			}
			if (value instanceof byte[]) {
				return literal(hex((byte[]) value), XSDDatatype.XSDhexBinary);
			}
			// a bit string of more than one bit (PostgreSQL bit(n), n > 1): its digits, "101"
			return NodeValue.makeString(rs.getString(i));
		case Types.TINYINT:
		case Types.SMALLINT:
		case Types.INTEGER:
		case Types.BIGINT:
			return literal(rs.getBigDecimal(i).toBigInteger().toString(), XSDDatatype.XSDinteger);
		case Types.DECIMAL:
		case Types.NUMERIC:
			return literal(canonicalDecimal(rs.getBigDecimal(i)), XSDDatatype.XSDdecimal);
		case Types.REAL: {
			final float f = rs.getFloat(i);
			if (Float.isNaN(f) || Float.isInfinite(f)) {
				return literal(special(f), XSDDatatype.XSDdouble);
			}
			// the decimal value of the single precision number, 80.25 or 70.22
			return literal(canonicalDouble(new BigDecimal(Float.toString(f))), XSDDatatype.XSDdouble);
		}
		case Types.FLOAT:
		case Types.DOUBLE: {
			final double d = rs.getDouble(i);
			if (Double.isNaN(d) || Double.isInfinite(d)) {
				return literal(special(d), XSDDatatype.XSDdouble);
			}
			return literal(canonicalDouble(new BigDecimal(Double.toString(d))), XSDDatatype.XSDdouble);
		}
		case Types.DATE:
			return literal(date(rs, i), XSDDatatype.XSDdate);
		case Types.TIME:
			if (!withTimeZone(rsmd.getColumnTypeName(i))) {
				return literal(time(rs.getObject(i, LocalTime.class)), XSDDatatype.XSDtime);
			}
			// PostgreSQL reports timetz as TIME
		case Types.TIME_WITH_TIMEZONE: {
			final OffsetTime t = rs.getObject(i, OffsetTime.class);
			return literal(time(t.toLocalTime()) + offset(t.getOffset()), XSDDatatype.XSDtime);
		}
		case Types.TIMESTAMP:
			if (withTimeZone(rsmd.getColumnTypeName(i))) {
				return literal(dateTime(rs.getObject(i, OffsetDateTime.class)), XSDDatatype.XSDdateTime);
			}
			return literal(dateTime(rs.getObject(i, LocalDateTime.class)), XSDDatatype.XSDdateTime);
		case Types.TIMESTAMP_WITH_TIMEZONE:
		case SQLSERVER_DATETIMEOFFSET:
			return literal(dateTime(rs.getObject(i, OffsetDateTime.class)), XSDDatatype.XSDdateTime);
		case Types.BINARY:
		case Types.VARBINARY:
		case Types.LONGVARBINARY:
		case Types.BLOB:
			return literal(hex(rs.getBytes(i)), XSDDatatype.XSDhexBinary);
		case Types.CHAR:
		case Types.NCHAR: {
			// a fixed-length string is padded with spaces (MySQL removes them)
			final String s = rs.getString(i);
			final int length = rsmd.getPrecision(i);
			final StringBuilder sb = new StringBuilder(s);
			while (length > 0 && length < 100000 && sb.codePointCount(0, sb.length()) < length) {
				sb.append(' ');
			}
			return NodeValue.makeString(sb.toString());
		}
		default:
			return NodeValue.makeString(rs.getString(i));
		}
	}

	private static boolean withTimeZone(String typeName) {
		if (typeName == null) {
			return false;
		}
		final String t = typeName.toLowerCase(Locale.ROOT);
		return t.equals("timestamptz") || t.equals("timetz") || t.contains("with time zone") || t.equals("datetimeoffset");
	}

	private static String date(ResultSet rs, int i) throws SQLException {
		try {
			return rs.getObject(i, LocalDate.class).toString();
		} catch (SQLException | RuntimeException ex) {
			return rs.getDate(i).toLocalDate().toString();
		}
	}

	private static String time(LocalTime t) {
		final String s = String.format(Locale.ROOT, "%02d:%02d:%02d", t.getHour(), t.getMinute(), t.getSecond());
		return s + fraction(t.getNano());
	}

	private static String dateTime(LocalDateTime t) {
		return t.toLocalDate().toString() + "T" + time(t.toLocalTime());
	}

	private static String dateTime(OffsetDateTime t) {
		return dateTime(t.toLocalDateTime()) + offset(t.getOffset());
	}

	private static String offset(ZoneOffset offset) {
		return offset.getTotalSeconds() == 0 ? "Z" : offset.getId();
	}

	/**
	 * The fractional seconds, without trailing zeros, or nothing.
	 */
	private static String fraction(int nanos) {
		if (nanos == 0) {
			return "";
		}
		String f = String.format(Locale.ROOT, "%09d", nanos);
		f = f.replaceFirst("0+$", "");
		return "." + f;
	}

	private static String special(double d) {
		return Double.isNaN(d) ? "NaN" : d > 0 ? "INF" : "-INF";
	}

	/**
	 * The canonical lexical form of xsd:decimal (XML Schema 1.0): no exponent,
	 * no trailing zero, a decimal point and at least one digit on each side.
	 */
	static String canonicalDecimal(BigDecimal d) {
		String s = d.stripTrailingZeros().toPlainString();
		if (s.equals("-0")) {
			s = "0";
		}
		return s.contains(".") ? s : s + ".0";
	}

	/**
	 * The canonical lexical form of xsd:double: a mantissa with one digit
	 * before the decimal point and at least one after, "E", the exponent
	 * ("8.025E1", "1.0E0", "0.0E0").
	 */
	static String canonicalDouble(BigDecimal d) {
		if (d.signum() == 0) {
			return "0.0E0";
		}
		final BigDecimal s = d.stripTrailingZeros();
		final String digits = s.unscaledValue().abs().toString();
		final int exponent = digits.length() - 1 - s.scale();
		final String mantissa = digits.charAt(0) + "." + (digits.length() > 1 ? digits.substring(1) : "0");
		return (s.signum() < 0 ? "-" : "") + mantissa + "E" + exponent;
	}

	private static String hex(byte[] bytes) {
		final StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			sb.append(Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, 16)));
			sb.append(Character.toUpperCase(Character.forDigit(b & 0xF, 16)));
		}
		return sb.toString();
	}

}
