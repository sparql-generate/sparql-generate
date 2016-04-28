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
package com.github.thesmartenergy.sparql.generate.jena.iterator.library;

import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerate;
import com.github.thesmartenergy.sparql.generate.jena.iterator.IteratorFunctionBase1;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import com.github.thesmartenergy.sparql.generate.jena.iterator.IteratorFunctionBase2;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.StringWriter;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.apache.jena.sparql.expr.nodevalue.NodeValueBoolean;
import org.apache.jena.sparql.expr.nodevalue.NodeValueDecimal;
import org.apache.jena.sparql.expr.nodevalue.NodeValueDouble;
import org.apache.jena.sparql.expr.nodevalue.NodeValueFloat;
import org.apache.jena.sparql.expr.nodevalue.NodeValueInteger;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import java.math.BigDecimal;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.dom.DOMSource; 
import javax.xml.transform.stream.StreamResult;
import org.supercsv.io.ICsvListReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.CsvListWriter;
import org.supercsv.prefs.CsvPreference;

/**
 * A SPARQL Iterator function that return a row of a CSV document, together with the header. The Iterator function URI is
 * {@code <http://w3id.org/sparql-generate/ite/CSV>}.
 * It takes one parameter as input which is the source CSV document which is a RDF Literal with datatype URI
 * {@code <urn:iana:mime:text/csv>} 
 * and returns a list of RDF Literal with datatype URI
 * {@code <urn:iana:mime:text/csv>} for each row of the CSV document.
 * @see com.github.thesmartenergy.sparql.generate.jena.function.library.FN_CustomCSV for CSV document with different dialects
 * @author Noorani Bakerally
 */
public class ITE_CSV extends IteratorFunctionBase1 {

    /**
     * The logger.
     */
    private static final Logger LOG = Logger.getLogger(ITE_CSV.class);

    /**
     * The SPARQL function URI.
     */
    public static final String URI = SPARQLGenerate.ITE + "CSV";

    /**
     * The datatype URI of the first parameter and the return literals.
     */
    private static final String datatypeUri = "urn:iana:mime:text/csv";

    @Override
    public List<NodeValue> exec(NodeValue v) {
        
        if (v.getDatatypeURI() == null
                && datatypeUri == null
                || v.getDatatypeURI() != null
                && !v.getDatatypeURI().equals(datatypeUri)
                && !v.getDatatypeURI().equals("http://www.w3.org/2001/XMLSchema#string")) {
            LOG.warn("The URI of NodeValue1 MUST be"
                    + " <" + datatypeUri + "> or"
                    + " <http://www.w3.org/2001/XMLSchema#string>. Got <"
                    + v.getDatatypeURI() + ">. Returning null.");
        }
        RDFDatatype dt = TypeMapper.getInstance()
                        .getSafeTypeByName(datatypeUri);
        try {
           
            String sourceCSV = String.valueOf(v.asNode().getLiteralValue());
           
            ICsvListReader listReader = null;
            InputStream is = new ByteArrayInputStream(sourceCSV.getBytes());
            InputStreamReader reader = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(reader);
            
            String header = br.readLine();
            listReader = new CsvListReader(br, CsvPreference.STANDARD_PREFERENCE);
           
            List<NodeValue> nodeValues = new ArrayList<>(listReader.length());
          
           
            while (listReader.read() != null){
                StringWriter sw = new StringWriter();
                
                CsvListWriter listWriter = new CsvListWriter(sw, CsvPreference.TAB_PREFERENCE);
                listWriter.writeHeader(header);
                listWriter.write(listReader.getUntokenizedRow());
                listWriter.close();
                
            
                Node node = NodeFactory.createLiteral(sw.toString(),dt);
                NodeValueNode nodeValue = new NodeValueNode(node);
                nodeValues.add(nodeValue);
            }
      
            return nodeValues;  
        } catch (Exception e) {
            throw new ExprEvalException("FunctionBase: no evaluation", e);
        }
    }

    
}