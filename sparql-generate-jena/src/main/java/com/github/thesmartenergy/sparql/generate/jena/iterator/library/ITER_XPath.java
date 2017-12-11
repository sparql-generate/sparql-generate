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
import com.github.thesmartenergy.sparql.generate.jena.iterator.IteratorFunctionBase2;
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
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Iterator function
 * <a href="http://w3id.org/sparql-generate/iter/XPath">iter:XPath</a>
 * extracts a list of sub-XML elements of a XML root element, according to a
 * XPath query.
 *
 * <ul>
 * <li>Param 1: (input) is the input XML document;</li>
 * <li>Param 2: (xpath) is a XPath query;</li>
 * </ul>
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class ITER_XPath extends IteratorFunctionBase2 {

    private static final Logger LOG = LoggerFactory.getLogger(ITER_XPath.class);

    public static final String URI = SPARQLGenerate.ITER + "XPath";

    private static final String datatypeUri = "http://www.iana.org/assignments/media-types/application/xml";

    @Override
    public List<NodeValue> exec(NodeValue xml, NodeValue v2) {
        if (xml.getDatatypeURI() != null
                && !xml.getDatatypeURI().equals(datatypeUri)
                && !xml.getDatatypeURI().equals("http://www.w3.org/2001/XMLSchema#string")) {
            LOG.warn("The URI of NodeValue1 MUST be"
                    + " <" + datatypeUri + "> "
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
            Document document = builder
                    .parse(new ByteArrayInputStream(xmlstring.getBytes("UTF-8")));

            XPath xPath = XPathFactory.newInstance().newXPath();

            NodeList nodeList = (NodeList) xPath
                    .compile(v2.getString())
                    .evaluate(document, XPathConstants.NODESET);

            //will contain the final results
            List<NodeValue> nodeValues = new ArrayList<>(nodeList.getLength());

            for (int i = 0; i < nodeList.getLength(); i++) {

                org.w3c.dom.Node xmlNode = nodeList.item(i);

                RDFDatatype dt = TypeMapper.getInstance()
                        .getSafeTypeByName(datatypeUri);
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

                    TransformerFactory tFactory = TransformerFactory.newInstance();
                    Transformer transformer = tFactory.newTransformer();
                    DOMSource source = new DOMSource(xmlNode);
                    StringWriter writer = new StringWriter();
                    transformer.transform(source, new StreamResult(writer));
                    Node node = NodeFactory.createLiteral(writer.getBuffer().toString(), dt);
                    nodeValue = new NodeValueNode(node);
                }
                nodeValues.add(nodeValue);
            }
            LOG.trace("Evaluation of " + v2 + ": " + nodeValues.size() + " values");
            return nodeValues;
        } catch (Exception ex) {
            LOG.debug("No evaluation for " + xml + ", " + v2, ex);
            throw new ExprEvalException("No evaluation for " + v2, ex);
        }
    }
}
