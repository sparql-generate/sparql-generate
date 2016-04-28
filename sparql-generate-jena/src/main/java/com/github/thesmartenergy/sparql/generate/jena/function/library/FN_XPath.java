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
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.math.BigDecimal;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionBase2;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.apache.jena.sparql.expr.nodevalue.NodeValueBoolean;
import org.apache.jena.sparql.expr.nodevalue.NodeValueDecimal;
import org.apache.jena.sparql.expr.nodevalue.NodeValueDouble;
import org.apache.jena.sparql.expr.nodevalue.NodeValueFloat;
import org.apache.jena.sparql.expr.nodevalue.NodeValueInteger;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
/**
 * A SPARQL Function that extracts a string from a XML document, according to a
 * XPath expression. The Function URI is
 * {@code <http://w3id.org/sparql-generate/fn/XPath>}.
 * It takes two parameters as input:
 * <ul>
 * <li>a RDF Literal with datatype URI
 * {@code <urn:iana:mime:application/json>}</li>
 * <li>a RDF Literal with datatype {@code xsd:string}</li>
 * </ul>
 * and returns a RDF Literal with datatype URI
 * {@code <urn:iana:mime:application/json>}.
 *
 * @author maxime.lefrancois
 */
public class FN_XPath extends FunctionBase2 {
    //TODO write multiple unit tests for this class.

    /**
     * The logger.
     */
    private static final Logger LOG = Logger.getLogger(FN_XPath.class);

    /**
     * The SPARQL function URI.
     */
    public static final String URI = SPARQLGenerate.FN + "XPath";

    /**
     * The datatype URI of the first parameter and the return literals.
     */
    private static final String datatypeUri = "urn:iana:mime:application/xml";

    /**
     * Returns the evaluation of XPath {@code xpath} over the XML
     * document {@code xml}.
     * @param xml the RDF Literal that represents a XML document
     * @param xpath the xsd:string that represents the XPath
     * @return -
     */
    @Override
    public NodeValue exec(NodeValue xml, NodeValue xpath) {
        if (xml.getDatatypeURI() == null
                && datatypeUri == null
                || xml.getDatatypeURI() != null
                && !xml.getDatatypeURI().equals(datatypeUri)
                && !xml.getDatatypeURI().equals("http://www.w3.org/2001/XMLSchema#string")) {
            LOG.warn("The URI of NodeValue1 MUST be <" + datatypeUri + ">"
                    + "or <http://www.w3.org/2001/XMLSchema#string>."
                    + " Returning null.");
        }
        LOG.debug("===========> "+xpath);
        DocumentBuilderFactory builderFactory
                = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        DocumentBuilder builder = null;
        try {
            builder = builderFactory.newDocumentBuilder();
            InputStream in = new ByteArrayInputStream(
                    xml.asNode().getLiteralLexicalForm().getBytes());
            Document document = builder.parse(in);

            XPath xPath =  XPathFactory.newInstance().newXPath();
            //Node node = (Node) xPath.compile(xpath.getString()).evaluate(document, XPathConstants.NODE);
           Object value =  xPath.compile(xpath.getString()).evaluate(document);
            if (value instanceof String) {
                   return new NodeValueString((String) value);
               } else if (value instanceof Float) {
                   return new NodeValueFloat((Float) value);
               } else if (value instanceof Boolean) {
                   return new NodeValueBoolean((Boolean) value);
               } else if (value instanceof Integer) {
                   return new NodeValueInteger((Integer) value);
               } else if (value instanceof Double) {
                   return new NodeValueDouble((Double) value);
               } else if (value instanceof BigDecimal) {
                   return new NodeValueDecimal((BigDecimal) value);
               }
            return new NodeValueString(String.valueOf(value));
        } catch (Exception e) {
            LOG.debug("Error:XPATJ "+e.getMessage());
            throw new ExprEvalException("FunctionBase: no evaluation", e);
        }
    }
}
