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
import java.util.Iterator;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
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
 * {@code <http://w3id.org/sparql-generate/fn/XPath>}. It takes two parameters
 * as input:
 * <ul>
 * <li>{@param xml}a RDF Literal with datatype URI
 * {@code <urn:iana:mime:application/xml>} representing the source XML document</li>
 * <li>{@param xpath} a RDF Literal with datatype {@code xsd:string} representing the XPath expression to
 * be evaluated on the XML document
 * </li>
 * </ul>
 * and returns a RDF Literal with datatype being the type of the object extracted from the XML document
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
     * {@inheritDoc }
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
        LOG.debug("===========> " + xpath);
        DocumentBuilderFactory builderFactory
                = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        DocumentBuilder builder = null;
        try {
            // THIS IS A HACK !! FIND A BETTER WAY TO MANAGE NAMESPACES
            String xmlstring = xml.asNode().getLiteralLexicalForm().replaceAll("xmlns=\"[^\"]*\"", "");
            
            
            builder = builderFactory.newDocumentBuilder();
            InputStream in = new ByteArrayInputStream(xmlstring.getBytes());
            Document document = builder.parse(in);

            XPath xPath = XPathFactory.newInstance().newXPath();
            xPath.setNamespaceContext(new UniversalNamespaceResolver(document));
            //Node node = (Node) xPath.compile(xpath.getString()).evaluate(document, XPathConstants.NODE);
            Object value = xPath.compile(xpath.getString()).evaluate(document);
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
            LOG.debug("Error:XPATH " + e.getMessage());
            throw new ExprEvalException("FunctionBase: no evaluation", e);
        }
    }

    public class UniversalNamespaceResolver implements NamespaceContext {
        // the delegate

        private final Document sourceDocument;

        /**
         * This constructor stores the source document to search the namespaces
         * in it.
         *
         * @param document source document
         */
        public UniversalNamespaceResolver(Document document) {
            sourceDocument = document;
        }

        /**
         * The lookup for the namespace uris is delegated to the stored
         * document.
         *
         * @param prefix to search for
         * @return uri
         */
        @Override
        public String getNamespaceURI(String prefix) {
            if (prefix.equals(XMLConstants.DEFAULT_NS_PREFIX)) {
                return sourceDocument.lookupNamespaceURI(null);
            } else {
                return sourceDocument.lookupNamespaceURI(prefix);
            }
        }

        /**
         * This method is not needed in this context, but can be implemented in
         * a similar way.
         */
        @Override
        public String getPrefix(String namespaceURI) {
            return sourceDocument.lookupPrefix(namespaceURI);
        }

        @Override
        public Iterator getPrefixes(String namespaceURI) {
            // not implemented yet
            return null;
        }

    }
}
