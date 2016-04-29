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
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.apache.jena.graph.Node;
import java.math.BigDecimal;
import java.util.Map;
import static org.apache.jena.query.ResultSetFactory.result;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.apache.jena.sparql.function.FunctionBase2;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.apache.jena.sparql.expr.nodevalue.NodeValueBoolean;
import org.apache.jena.sparql.expr.nodevalue.NodeValueDecimal;
import org.apache.jena.sparql.expr.nodevalue.NodeValueDouble;
import org.apache.jena.sparql.expr.nodevalue.NodeValueFloat;
import org.apache.jena.sparql.expr.nodevalue.NodeValueInteger;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.CsvMapReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;

/**
 * A SPARQL function that return a RDF literal. The function URI is
 * {@code <http://w3id.org/sparql-generate/fn/CSV>}.
 * It takes the following two parameters:
 * <ul>
 *      <li>{@param csv} the source CSV document(basically a single row) which is a RDF Literal with datatype URI
 * {@code <urn:iana:mime:text/csv>} </li>
 *      <li>{@param column} the column to be extracted from {@param csv} </li>
 * </ul>
 * and returns a RDF Literal with datatype URI 
 * {@code <urn:iana:mime:text/csv>} for the {@param column}.
 * @author Noorani Bakerally
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
    private static final String datatypeUri = "urn:iana:mime:text/csv";

    /**
     * {@inheritDoc }
     */
    @Override
    public NodeValue exec(NodeValue csv, NodeValue column) {
        
        /*
        if (xml.getDatatypeURI() == null
                && datatypeUri == null
                || xml.getDatatypeURI() != null
                && !xml.getDatatypeURI().equals(datatypeUri)
                && !xml.getDatatypeURI().equals("http://www.w3.org/2001/XMLSchema#string")) {
            LOG.warn("The URI of NodeValue1 MUST be <" + datatypeUri + ">"
                    + "or <http://www.w3.org/2001/XMLSchema#string>."
                    + " Returning null.");
        }
        */
        
        LOG.debug("===========> "+column);
        DocumentBuilderFactory builderFactory
                = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        try {
            
            String sourceCSV = String.valueOf(csv.asNode().getLiteralValue());
            
            InputStream is = new ByteArrayInputStream(sourceCSV.getBytes());
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            
            
            //ICsvListReader listReader = null;
            //listReader = new CsvListReader(br, CsvPreference.STANDARD_PREFERENCE);
            //listReader.getHeader(true);
            CsvMapReader mapReader = new CsvMapReader(br, CsvPreference.STANDARD_PREFERENCE);
            String headers_str [] = mapReader.getHeader(true);
            Map  <String,String> headers= mapReader.read(headers_str);
           
            //return new NodeValueString(headers.get(path.asNode().getLiteralValue()));
            
            String columnName = (String) column.asNode().getLiteralValue();
            
            return new NodeValueString(headers.get(columnName));
            
            
        } catch (Exception e) {
            LOG.debug("Error:XPATJ "+e.getMessage());
            throw new ExprEvalException("FunctionBase: no evaluation", e);
        }
    }
}
