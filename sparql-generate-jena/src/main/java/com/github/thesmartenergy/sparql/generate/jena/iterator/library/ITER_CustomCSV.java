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
package com.github.thesmartenergy.sparql.generate.jena.iterator.library;

import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerate;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import com.github.thesmartenergy.sparql.generate.jena.iterator.IteratorFunctionBase5;
import java.io.BufferedReader;
import java.io.StringWriter;
import java.util.List;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.supercsv.io.ICsvListReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Iterator function
 * <a href="http://w3id.org/sparql-generate/iter/CustomCSV">iter:CustomCSV</a>
 * iterates over the lines of CSV documents having a certain dialect
 * specification.
 *
 * <ul>
 * <li>Param 1: (csv) is the CSV document;</li>
 * <li>Param 2: (quoteChar) is the quote character;</li>
 * <li>Param 3: (delimiterChar) is the delimiter character;</li>
 * <li>Param 4: (endOfLineSymbols) is the end of line symbol;</li>
 * <li>Param 5: (header) is true if the CSV has a header row;</li>
 * </ul>
 *
 * @author Noorani Bakerally <noorani.bakerally at emse.fr>
 */
public class ITER_CustomCSV extends IteratorFunctionBase5 {

    private static final Logger LOG = LoggerFactory.getLogger(ITER_CustomCSV.class);

    public static final String URI = SPARQLGenerate.ITER + "CustomCSV";

    private static final String datatypeUri = "http://www.iana.org/assignments/media-types/text/csv";

    @Override
    public List<NodeValue> exec(NodeValue csv, NodeValue quoteChar, NodeValue delimiterChar, NodeValue endOfLineSymbols, NodeValue header) {
        if (csv.getDatatypeURI() != null
                && !csv.getDatatypeURI().equals(datatypeUri)
                && !csv.getDatatypeURI().equals("http://www.w3.org/2001/XMLSchema#string")) {
            LOG.warn("The URI of NodeValue1 MUST be"
                    + " <" + datatypeUri + "> or"
                    + " <http://www.w3.org/2001/XMLSchema#string>. Got <"
                    + csv.getDatatypeURI() + ">.");
        }

        try {
            String sourceCSV = String.valueOf(csv.asNode().getLiteralLexicalForm());

            ICsvListReader listReader = null;
            InputStream is = new ByteArrayInputStream(sourceCSV.getBytes("UTF-8"));
            InputStreamReader reader = new InputStreamReader(is, "UTF-8");
            BufferedReader br = new BufferedReader(reader);

            String headerRow = "";
            if (header.getBoolean()) {
                headerRow = br.readLine().split(endOfLineSymbols.asString())[0];
            }

            CsvPreference prefs = new CsvPreference.Builder(quoteChar.asString().charAt(0), delimiterChar.asString().charAt(0),
                    endOfLineSymbols.asString()).build();

            listReader = new CsvListReader(br, prefs);

            List<NodeValue> nodeValues = new ArrayList<>(listReader.length());

            while (listReader.read() != null) {
                StringWriter sw = new StringWriter();

                if (header.getBoolean()) {
                    sw.write(headerRow);
                }
                sw.write("\n");
                String row = listReader.getUntokenizedRow();
                sw.write(row);

                NodeValue nodeValue = new NodeValueString(sw.toString());
                nodeValues.add(nodeValue);
            }

            return nodeValues;
        } catch (Exception ex) {
            LOG.debug("No evaluation for " + csv, ex);
            throw new ExprEvalException("No evaluation for " + csv, ex);
        }
    }
}
