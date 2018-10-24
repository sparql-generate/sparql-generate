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
package com.github.thesmartenergy.sparql.generate.jena.function.library;

import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerate;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.QueryBuildException;
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

/**
 * Binding function <a href="http://w3id.org/sparql-generate/fn/dateTime">fun:dateTime</a>
 * converts a given datetime or a UNIX timestamp in milliseconds to an xsd:dateTime.
 * <ul>
 *  <li>args contains the supplied arguments such that
 *      <ul>
 *          <li>the first argument is either a datetime or a UNIX timestamp (in milliseconds);</li>
 *          <li>the second argument is optional, and (if provided) contains the parsing format string in the <a href="https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html#iso8601timezone">ISO 8601</a> format according to universal time;</li>
 *          <li>if there is no second argument, the first argument is considered as a UNIX timestamp (in milliseconds) in UTC time zone.</li>
 *          </ul>
 *  </li>
 *  <li>Result is a xsd:dateTime.</li>
 * </ul>
 *
 * <b>Examples:</b>
 * <pre>
 * {@code
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
    public static final String URI = SPARQLGenerate.FUN + "dateTime";

    @Override
    public NodeValue exec(List<NodeValue> args) {

        NodeValue dt = args.get(0);
        String dateTimeString = dt.asString();

        if (dt == null || dateTimeString.isEmpty()) {
            LOG.debug("The NodeValue is null or empty");
            throw new ExprEvalException("The NodeValue is null or empty");
        }

        ZonedDateTime date = null;
        if (args.size() == 2) {
            NodeValue df = args.get(1) == null? NodeValue.nvEmptyString: args.get(1);
            DateTimeFormatter parseFormat;

            switch (df.getString()) {
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
                    parseFormat = DateTimeFormatter.ofPattern(df.getString());
            }

            try {
                date = ZonedDateTime.parse(dateTimeString, parseFormat);
            } catch (DateTimeParseException e) {
                LOG.debug("The NodeValue {} MUST correspond to the format {}.", dt, df);
                throw new ExprEvalException("The NodeValue " + dt + " MUST correspond to the format " + df + " on in milliseconds.");
            }
        } else if (args.size() == 1) {
            //https://docs.oracle.com/javase/7/docs/api/java/util/Date.html#Date(long)
            try {
                long miliSeconds = Long.parseLong(dateTimeString);
                date = ZonedDateTime.ofInstant(Instant.ofEpochMilli(miliSeconds), ZoneOffset.UTC);
            } catch (NumberFormatException ex) {
                LOG.debug("The NodeValue {} MUST be an integer.", dt);
                throw new ExprEvalException("The NodeValue " + dt + " MUST be an integer.");
            }
        }

        Node node = NodeFactory.createLiteral(DateTimeFormatter.ISO_DATE_TIME.format(date), XSDDatatype.XSDdateTime);
        return NodeValue.makeNode(node);
    }

    @Override
    public void checkBuild(String uri, ExprList args) {
        if (args.size() == 0 || args.size() > 2) {
            throw new QueryBuildException("Function '"
                    + this.getClass().getName() + "' takes up to two argument");
        }
    }
}
