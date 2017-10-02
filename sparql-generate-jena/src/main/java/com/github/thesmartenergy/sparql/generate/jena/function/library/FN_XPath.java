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
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionBase2;
import org.apache.jena.sparql.expr.nodevalue.NodeValueBoolean;
import org.apache.jena.sparql.expr.nodevalue.NodeValueDecimal;
import org.apache.jena.sparql.expr.nodevalue.NodeValueDouble;
import org.apache.jena.sparql.expr.nodevalue.NodeValueFloat;
import org.apache.jena.sparql.expr.nodevalue.NodeValueInteger;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * A SPARQL Function that extracts a string from a XML document, according to a
 * XPath expression. The Function URI is
 * {@code <http://w3id.org/sparql-generate/fn/XPath>}.
 *
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class FN_XPath extends FunctionBase2 {
    //TODO write multiple unit tests for this class.

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(FN_XPath.class);

    /**
     * The SPARQL function URI.
     */
    public static final String URI = SPARQLGenerate.FN + "XPath";

    /**
     * The datatype URI of the first parameter and the return literals.
     */
    private static final String datatypeUri = "http://www.iana.org/assignments/media-types/application/xml";

    /**
     *
     * @param xml a RDF Literal with datatype URI
     * {@code <http://www.iana.org/assignments/media-types/application/xml>} pr
     * {@code xsd:string} representing the source XML document
     * @param xpath a RDF Literal with datatype {@code xsd:string} representing
     * the XPath expression to be evaluated on the XML document
     * @return a RDF Literal with datatype being the type of the object
     * extracted from the XML document
     */
    @Override
    public NodeValue exec(NodeValue xml, NodeValue xpath) {
        if (xml.getDatatypeURI() != null
                && !xml.getDatatypeURI().equals(datatypeUri)
                && !xml.getDatatypeURI().equals("http://www.w3.org/2001/XMLSchema#string")) {
            LOG.warn("The URI of NodeValue1 MUST be <" + datatypeUri + ">"
                    + " or <http://www.w3.org/2001/XMLSchema#string>. Got "
                    + xml.getDatatypeURI());
        }
        DocumentBuilderFactory builderFactory
                = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        DocumentBuilder builder = null;
        try {
            // THIS IS A HACK !! FIND A BETTER WAY TO MANAGE NAMESPACES
            String xmlstring = xml.asNode().getLiteralLexicalForm().replaceAll("xmlns=\"[^\"]*\"", "");

            builder = builderFactory.newDocumentBuilder();
            InputStream in = new ByteArrayInputStream(xmlstring.getBytes("UTF-8"));
            Document document = builder.parse(in);

            XPath xPath = XPathFactory.newInstance().newXPath();
            xPath.setNamespaceContext(new UniversalNamespaceResolver(document));
            //Node node = (Node) xPath.compile(xpath.getString()).evaluate(document, XPathConstants.NODE);

            org.w3c.dom.Node xmlNode = (org.w3c.dom.Node) xPath
                    .compile(xpath.getString())
                    .evaluate(document, XPathConstants.NODE);
            if(xmlNode == null) {
                LOG.debug("No evaluation of " + xpath);
                throw new ExprEvalException("No evaluation of " + xpath);
            }

            /*
                Node node = NodeFactory.createLiteral(xmlNode.getNodeValue(), dt);
                NodeValue nodeValue = new NodeValueNode(node);
                nodeValues.add(nodeValue);
             */
            NodeValue nodeValue = null;
            Object value = xmlNode.getNodeValue();
            if (value instanceof Float) {
                nodeValue = new NodeValueFloat((Float) value);
            } else if (value instanceof Boolean) {
                nodeValue = new NodeValueBoolean((Boolean) value);
            } else if (value instanceof Integer) {
                nodeValue = new NodeValueInteger((Integer) value);
            } else if (value instanceof Double) {
                nodeValue = new NodeValueDouble((Double) value);
            } else if (value instanceof BigDecimal) {
                nodeValue = new NodeValueDecimal((BigDecimal) value);
            } else if (value instanceof String) {
                nodeValue = new NodeValueString((String) value);
            } else {

                RDFDatatype dt = TypeMapper.getInstance()
                        .getSafeTypeByName(datatypeUri);

                TransformerFactory tFactory = TransformerFactory.newInstance();
                Transformer transformer = tFactory.newTransformer();
                DOMSource source = new DOMSource(xmlNode);
                StringWriter writer = new StringWriter();
                transformer.transform(source, new StreamResult(writer));
                Node node = NodeFactory.createLiteral(writer.getBuffer().toString(), dt);
                nodeValue = new NodeValueNode(node);
            }
            LOG.debug("Evaluation of " + xpath + ":  " + nodeValue);
            return nodeValue;
        } catch (Exception ex) {
            LOG.debug("No evaluation of " + xml + ", " + xpath, ex);
            throw new ExprEvalException("No evaluation of " + xml + ", " + xpath, ex);
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
