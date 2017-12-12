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
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.List;
import java.util.Map;
import org.apache.jena.atlas.lib.Lib;
import org.apache.jena.query.QueryBuildException;
import org.apache.jena.sparql.ARQInternalErrorException;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.apache.jena.sparql.function.FunctionBase;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.CsvMapReader;
import org.supercsv.prefs.CsvPreference;

/**
 * Binding function
 * <a href="http://w3id.org/sparql-generate/fn/CustomCSV">fun:CustomCSV</a>
 * selects the value for a given column in a given CSV row.
 *
 * <ul>
 * <li>Param 1 (csv) is a CSV document with an optional first row containing the
 * column names, and a row of values;</li>
 * <li>Param 2 (column) is the column name (if Param header is true), or the
 * column number starting from 0 (if Param header is false);</li>
 * <li>Param 3 (quoteChar) is the quote character (usually '"');</li>
 * <li>Param 4 (delimiterChar) is the delimiter character (usually ',');</li>
 * <li>Param 5 (endOfLineSymbols) is the end of line symbol (usually
 * '&bsol;n');</li>
 * <li>Param 6 (header) is true if the CSV document contains a first row with
 * the column names.</li>
 * <li>Result is a string.</li>
 * </ul>
 *
 * This function partly implements the CSVW dialect description.
 *
 * @see
 * <a href="https://www.w3.org/TR/tabular-metadata/#dialect-descriptions">CSVW
 * Dialect Descriptions</a>
 * @author Noorani Bakerally <noorani.bakerally at emse.fr>
 */
public class FUN_CustomCSV extends FunctionBase {
    //TODO write multiple unit tests for this class.

    private static final Logger LOG = LoggerFactory.getLogger(FUN_CustomCSV.class);

    public static final String URI = SPARQLGenerate.FUN + "CustomCSV";

    private static final String datatypeUri = "http://www.iana.org/assignments/media-types/text/csv";

    @Override
    public NodeValue exec(List<NodeValue> args) {
        if (args == null) {
            // The contract on the function interface is that this should not happen.
            throw new ARQInternalErrorException(Lib.className(this) + ": Null args list");
        }

        if (args.size() != 6) {
            throw new ExprEvalException(Lib.className(this) + ": Wrong number of arguments: Wanted 2, got " + args.size());
        }

        NodeValue v1 = args.get(0);
        NodeValue v2 = args.get(1);
        NodeValue v3 = args.get(2);
        NodeValue v4 = args.get(3);
        NodeValue v5 = args.get(4);
        NodeValue v6 = args.get(5);

        return exec(v1, v2, v3, v4, v5, v6);
    }

    @Override
    public void checkBuild(String uri, ExprList args) {
        if (args.size() != 6) {
            throw new QueryBuildException("Function '" + Lib.className(this) + "' takes two arguments");
        }
    }

    private NodeValue exec(NodeValue csv, NodeValue column, NodeValue quoteChar, NodeValue delimiterChar, NodeValue endOfLineSymbols, NodeValue header) {
        if (csv.getDatatypeURI() != null
                && !csv.getDatatypeURI().equals(datatypeUri)
                && !csv.getDatatypeURI().equals("http://www.w3.org/2001/XMLSchema#string")) {
            LOG.warn("The URI of NodeValue1 MUST be <" + datatypeUri + ">"
                    + "or <http://www.w3.org/2001/XMLSchema#string>."
            );
        }

        DocumentBuilderFactory builderFactory
                = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        try {

            String sourceCSV = String.valueOf(csv.asNode().getLiteralLexicalForm());

            InputStream is = new ByteArrayInputStream(sourceCSV.getBytes("UTF-8"));
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));

            String headers_str = "";
            if (header.getBoolean()) {
                headers_str = br.readLine().split(endOfLineSymbols.asString())[0];
            }

            CsvPreference prefs = new CsvPreference.Builder(quoteChar.asString().charAt(0), delimiterChar.asString().charAt(0), endOfLineSymbols.asString()).build();

            String nodeVal = "none";

            if (header.getBoolean()) {
                LOG.trace("header is true ");
                CsvMapReader mapReader = new CsvMapReader(br, prefs);

                Map<String, String> row = mapReader.read(headers_str.split(delimiterChar.asString()));
                String columnName = (String) column.asNode().getLiteralValue();
                nodeVal = row.get(columnName);

            } else {
                LOG.trace("header is false");
                List<String> values = new CsvListReader(br, prefs).read();
                nodeVal = values.get(Integer.valueOf(column.asString()));
            }
            if (nodeVal != null) {
                LOG.trace("return ", nodeVal);
                return new NodeValueString(nodeVal);
            } else {
                LOG.debug("node is null. return nothing");
                return new NodeValueString("");
            }

        } catch (Exception ex) {
            LOG.debug("No evaluation for " + csv + ", " + column, ex);
            throw new ExprEvalException("No evaluation for " + csv + ", " + column, ex);
        }
    }
}
