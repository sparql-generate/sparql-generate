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
import java.util.Map;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionBase2;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.supercsv.io.CsvMapReader;
import org.supercsv.prefs.CsvPreference;

/**
 * Binding function
 * <a href="http://w3id.org/sparql-generate/fn/CSV">fun:CSV</a> selects the
 * value for a given column in a given CSV row.
 *
 * <ul>
 * <li>Param 1 is a CSV document with two rows: a row of column names and a row
 * of values;</li>
 * <li>Param 2 is the column name;</li>
 * <li>Result is a string.</li>
 * </ul>
 *
 * @author Noorani Bakerally <noorani.bakerally at emse.fr>
 */
public class FUN_CSV extends FunctionBase2 {
    //TODO write multiple unit tests for this class.

    private static final Logger LOG = LoggerFactory.getLogger(FUN_CSV.class);

    public static final String URI = SPARQLGenerate.FUN + "CSV";

    private static final String datatypeUri = "http://www.iana.org/assignments/media-types/text/csv";

    @Override
    public NodeValue exec(NodeValue csv, NodeValue column) {
        if (csv.getDatatypeURI() != null
                && !csv.getDatatypeURI().equals(datatypeUri)
                && !csv.getDatatypeURI().equals("http://www.w3.org/2001/XMLSchema#string")) {
            LOG.debug("The URI of NodeValue1 MUST be <" + datatypeUri + ">"
                    + "or <http://www.w3.org/2001/XMLSchema#string>."
            );
        }

        try {

            String sourceCSV = String.valueOf(csv.asNode().getLiteralLexicalForm());
            InputStream is = new ByteArrayInputStream(sourceCSV.getBytes("UTF-8"));
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            CsvMapReader mapReader = new CsvMapReader(br, CsvPreference.STANDARD_PREFERENCE);
            String header[] = mapReader.getHeader(true);
            Map<String, String> row = mapReader.read(header);

            String columnName = (String) column.asNode().getLiteralValue();
            String value = row.get(columnName);
            NodeValue node;
            if (value == null) {
                node = new NodeValueString("");
            } else {
                node = new NodeValueString(value);
            }
            return node;
        } catch (Exception ex) {
            LOG.debug("No evaluation for " + csv + ", " + column, ex);
            throw new ExprEvalException("No evaluation for " + csv + ", " + column, ex);
        }
    }
}
