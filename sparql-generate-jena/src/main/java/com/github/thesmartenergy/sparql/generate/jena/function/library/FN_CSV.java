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
import java.util.Map;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionBase2;
import org.apache.log4j.Logger;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.supercsv.io.CsvMapReader;
import org.supercsv.prefs.CsvPreference;

/**
 * A SPARQL function that return a RDF literal. The function URI is
 * {@code <http://w3id.org/sparql-generate/fn/CSV>}.
 *
 * @author Noorani Bakerally <noorani.bakerally at emse.fr>
 */
public class FN_CSV extends FunctionBase2 {
    //TODO write multiple unit tests for this class.

    /**
     * The logger.
     */
    private static final Logger LOG = Logger.getLogger(FN_CSV.class);

    /**
     * The SPARQL function URI.
     */
    public static final String URI = SPARQLGenerate.FN + "CSV";

    /**
     * The datatype URI of the first parameter and the return literals.
     */
    private static final String datatypeUri = "http://www.iana.org/assignments/media-types/text/csv";

    /**
     *
     * @param csv the source CSV document(basically a single row) which is a RDF
     * Literal with datatype URI {@code <http://www.iana.org/assignments/media-types/text/csv>} or
     * {@code xsd:string}
     * @param column the column to be extracted from {
     * @param csv}
     * @return a RDF Literal with datatype URI {@code <http://www.iana.org/assignments/media-types/text/csv>}
     * for the {
     * @param column}
     */
    @Override
    public NodeValue exec(NodeValue csv, NodeValue column) {
        if (csv.getDatatypeURI() != null
                && !csv.getDatatypeURI().equals(datatypeUri)
                && !csv.getDatatypeURI().equals("http://www.w3.org/2001/XMLSchema#string")) {
            LOG.warn("The URI of NodeValue1 MUST be <" + datatypeUri + ">"
                    + "or <http://www.w3.org/2001/XMLSchema#string>."
                    + " Returning null.");
        }

        
        DocumentBuilderFactory builderFactory
                = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        try {

            String sourceCSV = String.valueOf(csv.asNode().getLiteralLexicalForm());


            InputStream is = new ByteArrayInputStream(sourceCSV.getBytes("UTF-8"));
            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            //ICsvListReader listReader = null;
            //listReader = new CsvListReader(br, CsvPreference.STANDARD_PREFERENCE);
            //listReader.getHeader(true);
            CsvMapReader mapReader = new CsvMapReader(br, CsvPreference.STANDARD_PREFERENCE);
            String headers_str[] = mapReader.getHeader(true);
            Map<String, String> headers = mapReader.read(headers_str);

            //return new NodeValueString(headers.get(path.asNode().getLiteralValue()));
            String columnName = (String) column.asNode().getLiteralValue();

            return new NodeValueString(headers.get(columnName));

        } catch (Exception e) {
            LOG.debug("Error:XPATH " + e.getMessage());
            throw new ExprEvalException("FunctionBase: no evaluation", e);
        }
    }
}
