/*
 * Copyright 2016 Ecole des Mines de Saint-Etienne.
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
package fr.emse.ci.sparqlext.function.library;

import fr.emse.ci.sparqlext.SPARQLExt;
import java.math.BigInteger;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.apache.jena.query.QueryBuildException;

/**
 * Binding function
 * <a href="http://w3id.org/sparql-generate/fn/dateTime">fun:dateTime</a>
 * converts a given datetime or a UNIX timestamp in milliseconds to an
 * xsd:dateTime.
 * 
 * <p>
 * See
 * <a href="https://w3id.org/sparql-generate/playground.html#ex=example/generate/Example-dateTime">Live
 * example</a></p>
 *
 * <ul>
 * <li>args contains the supplied arguments such that
 * <ul>
 * <li>the first argument is either a datetime (String) or a UNIX timestamp in
 * milliseconds (String or integer);</li>
 * <li>the second argument is optional. If provided contains the parsing format
 * string in the
 * <a href="https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html#iso8601timezone">ISO
 * 8601</a> format according to universal time;</li>
 * <li>if there is no second argument, the first argument must be a UNIX
 * timestamp in milliseconds (String or integer);</li>
 * </ul>
 * </li>
 * <li>Result is a xsd:dateTime.</li>
 * </ul>
 *
 * <b>Examples:</b>
 * <pre>
 * {@code
 * fun:dateTime(1453508109000) => "2016-01-23T01:15:09Z"^^http://www.w3.org/2001/XMLSchema#dateTime
 * fun:dateTime("1453508109000") => "2016-01-23T01:15:09Z"^^http://www.w3.org/2001/XMLSchema#dateTime
 * fun:dateTime("04/09/2018","dd/MM/yyyy") => "2018-09-04T00:00:00Z"^^http://www.w3.org/2001/XMLSchema#dateTime
 * fun:dateTime("2018-09-01T20:13:42Z", "ISO_DATE_TIME") => "2018-09-01T20:13:42Z"^^http://www.w3.org/2001/XMLSchema#dateTime
 * fun:dateTime("2018-09-01", "ISO_DATE") => "2018-09-01T00:00:00Z"^^http://www.w3.org/2001/XMLSchema#dateTime
 * }
 * </pre>
 *
 * @author El Mehdi Khalfi <el-mehdi.khalfi at emse.fr>
 * @since 2018-09-05
 */
public final class FUN_dateTime extends FunctionBase {

    private static final String ISO_DATE_TIME = "ISO_DATE_TIME";
    private static final String ISO_DATE = "ISO_DATE";
    private static final String EMPTY_STRING = "";

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(FUN_dateTime.class);

    /**
     * The SPARQL function URI.
     */
    public static final String URI = SPARQLExt.FUN + "dateTime";

    @Override
    public NodeValue exec(List<NodeValue> args) {
        if (args.size() > 2) {
            final String message = "Accepts maximum two arguments.";
            LOG.warn(message);
            throw new ExprEvalException(message);
        }
        final ZonedDateTime date;
        final NodeValue dt = args.get(0);
        if (dt.isNumber()) {
            if (args.size() > 1) {
                final String message = "If the first argument is a number, accepts no other argument.";
                LOG.warn(message);
                throw new ExprEvalException(message);
            }
            final BigInteger dateTimeInteger;
            if (dt.isInteger()) {
                dateTimeInteger = dt.getInteger();
            } else if (dt.isDecimal()) {
                dateTimeInteger = dt.getDecimal().toBigInteger();
            } else if (dt.isDouble()) {
                dateTimeInteger = BigInteger.valueOf(Math.round(dt.getDouble()));
            } else if (dt.isFloat()) {
                dateTimeInteger = BigInteger.valueOf(Math.round(dt.getFloat()));
            } else {
                final String message = "The first argument is a number, but it is not an integer nor a decimal nor a double nor a float.";
                LOG.warn(message);
                throw new ExprEvalException(message);
            }
            date = exec(dateTimeInteger);
        } else if (dt.isString()) {
            final String dateTimeString = dt.asString();
            if (args.size() == 1) {
                date = exec(dateTimeString);
            } else {
                final NodeValue df = args.get(1);
                if (!df.isString()) {
                    LOG.debug("The second argument must be a String that encodes the format. Got {}", df);
                }
                date = exec(dateTimeString, df.getString());
            }
        } else {
            final String message = "First argument must be an integer or a string";
            LOG.warn(message);
            throw new ExprEvalException(message);
        }
        Node node = NodeFactory.createLiteral(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(date), XSDDatatype.XSDdateTime);
        return NodeValue.makeNode(node);
    }

    @Override
    public void checkBuild(String uri, ExprList args) {
        if (args.size() == 0 || args.size() > 2) {
            throw new QueryBuildException("Function '"
                    + this.getClass().getName() + "' takes up to two argument");
        }
    }

    private ZonedDateTime exec(BigInteger dateTimeInteger) throws ExprEvalException {
        long miliSeconds = dateTimeInteger.longValue();
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(miliSeconds), ZoneOffset.UTC);
    }

    private ZonedDateTime exec(String dateTimeString) throws ExprEvalException {
        //https://docs.oracle.com/javase/7/docs/api/java/util/Date.html#Date(long)
        try {
            long miliSeconds = Long.parseLong(dateTimeString);
            return ZonedDateTime.ofInstant(Instant.ofEpochMilli(miliSeconds), ZoneOffset.UTC);
        } catch (NumberFormatException ex) {
            final String message = String.format("The first argument %s MUST be an integer.", dateTimeString);
            LOG.debug(message);
            throw new ExprEvalException(message);
        }
    }

    private ZonedDateTime exec(String dateTimeString, String format) {
        final DateTimeFormatter parseFormat;
        try {
            switch (format) {
                case ISO_DATE_TIME:
                    parseFormat = DateTimeFormatter.ISO_DATE_TIME;
                    break;
                case EMPTY_STRING:
                    parseFormat = DateTimeFormatter.ISO_DATE_TIME;
                    break;
                case ISO_DATE:
                    parseFormat = DateTimeFormatter.ISO_DATE;
                    break;
                default:
                    parseFormat = DateTimeFormatter.ofPattern(format);
            }
        } catch (IllegalArgumentException ex) {
            final String message = String.format("The second argument %s is not valid DateTimeFormatter format.", format);
            LOG.debug(message);
            throw new ExprEvalException(message);
        }
        try {
            return ZonedDateTime.parse(dateTimeString, parseFormat);
        } catch (DateTimeParseException ex1) {
            final String message1 = String.format("Error parsing with ZonedDateTime: %s", ex1.getMessage());
            LOG.debug(message1);
            try {
                LocalDateTime date = LocalDateTime.parse(dateTimeString, parseFormat);
                return date.atZone(ZoneId.systemDefault());
            } catch (DateTimeParseException ex2) {
                final String message2 = String.format("Error parsing with LocalDateTime: %s", ex1.getMessage());
                LOG.debug(message2);
                try {
                    LocalDate date = LocalDate.parse(dateTimeString, parseFormat);
                    return date.atStartOfDay(ZoneId.systemDefault());
                } catch (DateTimeParseException ex3) {
                    final String message3 = String.format("Error parsing with LocalDate: %s", ex1.getMessage());
                    LOG.debug(message3);
                    throw new ExprEvalException(message3);
                }
            }
        }
    }
}
