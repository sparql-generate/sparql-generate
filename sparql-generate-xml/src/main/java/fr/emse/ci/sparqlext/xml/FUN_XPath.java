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
package fr.emse.ci.sparqlext.xml;

import fr.emse.ci.sparqlext.utils.LogUtils;
import fr.emse.ci.sparqlext.SPARQLExt;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.util.Iterator;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
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
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.w3c.dom.Document;

/**
 * Binding function
 * <a href="http://w3id.org/sparql-generate/fn/XPath">fun:XPath</a>
 * extracts a string from a XML document, according to a XPath expression.
 *
 * <ul>
 * <li>Param 1 is the input string;</li>
 * <li>Param 2 is a XPath expression;</li>
 * <li>Result is a boolean, float, double, integer, string, as it best
 * fits.</li>
 * </ul>
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class FUN_XPath extends FunctionBase2 {

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(FUN_XPath.class);

    /**
     * The SPARQL function URI.
     */
    public static final String URI = SPARQLExt.FUN + "XPath";

    /**
     * The datatype URI of the first parameter and the return literals.
     */
    private static final String XML_URI = "http://www.iana.org/assignments/media-types/application/xml";

    private static final RDFDatatype DT = TypeMapper.getInstance().getSafeTypeByName(XML_URI);

    private static final TransformerFactory TRANSFORMER_FACTORY = TransformerFactory.newInstance();

    @Override
    public NodeValue exec(NodeValue xml, NodeValue xpath) {
        if (xml.getDatatypeURI() != null
                && !xml.getDatatypeURI().equals(XML_URI)
                && !xml.getDatatypeURI().equals("http://www.w3.org/2001/XMLSchema#string")) {
            LOG.debug("The URI of NodeValue1 MUST be <" + XML_URI + ">"
                    + " or <http://www.w3.org/2001/XMLSchema#string>. Got "
                    + xml.getDatatypeURI());
        }
        if (!xpath.isString()) {
            LOG.debug("The second argument should be a string. Got " + xpath);
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
            if (xmlNode == null) {
                LOG.debug("No evaluation of " + xpath);
                throw new ExprEvalException("No evaluation of " + xpath);
            }
            return nodeForNode(xmlNode);
        } catch (Exception ex) {
            if(LOG.isDebugEnabled()) {
                Node compressed = LogUtils.compress(xml.asNode());
                LOG.debug("No evaluation of " + compressed + ", " + xpath, ex);
            }
            throw new ExprEvalException("No evaluation of " + xpath, ex);
        }
    }

    public NodeValue nodeForNode(org.w3c.dom.Node xmlNode) throws TransformerException {
        if(xmlNode == null) {
            return null;
        }
        String nodeValue = xmlNode.getNodeValue();
        if (nodeValue != null) {
            return new NodeValueString(nodeValue);
        } else {
            DOMSource source = new DOMSource(xmlNode);
            StringWriter writer = new StringWriter();
            Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
            transformer.transform(source, new StreamResult(writer));
            Node node = NodeFactory.createLiteral(writer.toString(), DT);
            return new NodeValueNode(node);
        }
    }

    public static class UniversalNamespaceResolver implements NamespaceContext {
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
