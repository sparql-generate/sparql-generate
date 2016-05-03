/*
 * Copyright 2016 ITEA 12004 SEAS Project.
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
import com.github.thesmartenergy.sparql.generate.jena.function.FunctionBase6;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.List;
import java.util.Map;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.log4j.Logger;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.CsvMapReader;
import org.supercsv.prefs.CsvPreference;

/**
 * A SPARQL function that return a RDF literal. The function URI is
 * {@code <http://w3id.org/sparql-generate/fn/CustomCSV>}. This function partly
 * implements the CSVW dialect description
 *
 * @see
 * <a href="https://www.w3.org/TR/tabular-metadata/#dialect-descriptions">CSVW
 * Dialect Descriptions</a>
 * @author Noorani Bakerally <noorani.bakerally at emse.fr>
 */

public class FN_CustomCSV extends FunctionBase6 {
    //TODO write multiple unit tests for this class.

    /**
     * The logger.
     */
    private static final Logger LOG = Logger.getLogger(FN_CustomCSV.class);

    /**
     * The SPARQL function URI.
     */
    public static final String URI = SPARQLGenerate.FN + "CustomCSV";

    /**
     * The datatype URI of the first parameter and the return literals.
     */
    private static final String datatypeUri = "urn:iana:mime:text/csv";

    /**
     *
     * @param csv a RDF Literal with datatype URI
     * {@code <urn:iana:mime:text/csv>} or {@code xsd:string} representing the
     * source CSV document
     * @param column denotes the column to be selected for the CSV document. If
     * the value for the header is true, the path will be an RDF Literal of
     * datatype {@code xsd:string} to represent the column name. Else, it is
     * going to be an integer of datatype {@code xsd:int} to denote the index of
     * the column starting at 0 for the first column on the far left.
     * @param quoteChar a RDF Literal with datatype {@code xsd:string} for the
     * quote character
     * @param delimiterChar a RDF Literal with datatype {@code xsd:string} for
     * the delimiter character
     * @param endOfLineSymbols a RDF Literal with datatype {@code xsd:string}
     * for the end of line symbol
     * @param header a RDF Literal with datatype {@code xsd:boolean} where true
     * represents the presence of a header in the source CSV document
     * @return a RDF Literal with datatype URI {@code <urn:iana:mime:text/csv>}
     */
    @Override
    public NodeValue exec(NodeValue csv, NodeValue column, NodeValue quoteChar, NodeValue delimiterChar, NodeValue endOfLineSymbols, NodeValue header) {

        if (csv.getDatatypeURI() == null
                && datatypeUri == null
                || csv.getDatatypeURI() != null
                && !csv.getDatatypeURI().equals(datatypeUri)
                && !csv.getDatatypeURI().equals("http://www.w3.org/2001/XMLSchema#string")) {
            LOG.warn("The URI of NodeValue1 MUST be <" + datatypeUri + ">"
                    + "or <http://www.w3.org/2001/XMLSchema#string>."
                    + " Returning null.");
        }

        LOG.debug("===========> " + column);
        DocumentBuilderFactory builderFactory
                = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        try {

            String sourceCSV = String.valueOf(csv.asNode().getLiteralLexicalForm());

            InputStream is = new ByteArrayInputStream(sourceCSV.getBytes());
            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            //ICsvListReader listReader = null;
            //listReader = new CsvListReader(br, CsvPreference.STANDARD_PREFERENCE);
            //listReader.getHeader(true);
            String headerRow = "";
            System.out.println("test=================header" + header.getBoolean());
            if (header.getBoolean()) {
                headerRow = br.readLine().split(endOfLineSymbols.asString())[0];
            }
            System.out.println("test=================header" + headerRow);
            CsvPreference prefs = new CsvPreference.Builder(quoteChar.asString().charAt(0), delimiterChar.asString().charAt(0),
                    endOfLineSymbols.asString()).build();

            String nodeVal = "none";

            if (header.getBoolean()) {
                CsvMapReader mapReader = new CsvMapReader(br, prefs);
                String headers_str[] = mapReader.getHeader(true);
                Map<String, String> headers = mapReader.read(headers_str);
                String columnName = (String) column.asNode().getLiteralValue();
                nodeVal = headers.get(columnName);

            } else {
                List<String> values = new CsvListReader(br, prefs).read();

                nodeVal = values.get(0);
            }
            return new NodeValueString(nodeVal);

        } catch (Exception e) {
            LOG.debug("Error:XPATJ " + e.getMessage());
            throw new ExprEvalException("FunctionBase: no evaluation", e);
        }
    }
}
