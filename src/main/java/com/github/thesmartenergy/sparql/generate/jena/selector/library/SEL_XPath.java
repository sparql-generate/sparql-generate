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
package com.github.thesmartenergy.sparql.generate.jena.selector.library;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import com.github.thesmartenergy.sparql.generate.jena.selector.SelectorBase2;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 *
 * @author maxime.lefrancois
 */
public class SEL_XPath extends SelectorBase2 {

    private static final String iri = "urn:iana:mime:application/xml";

    public SEL_XPath() {
        super(iri);
    }

    @Override
    public List<NodeValue> exec(NodeValue v1, NodeValue v2) {
        if (v1.getDatatypeURI() == null ? iri != null : !v1.getDatatypeURI().equals(iri)) {
            Logger.getLogger(SEL_XPath.class).warn("The URI of NodeValue1 MUST be <" + iri + ">. Returning null.");
        }

        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        try {
            builder = builderFactory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(v1.asNode().getLiteralLexicalForm().getBytes()));
            
            XPath xPath =  XPathFactory.newInstance().newXPath();
            
            NodeList nodeList = (NodeList) xPath.compile(v2.getString()).evaluate(document, XPathConstants.NODESET);
            List<NodeValue> nodeValues = new ArrayList<>(nodeList.getLength());

            for (int i=0;i<nodeList.getLength();i++) {
                org.w3c.dom.Node xmlNode = nodeList.item(i);
                System.out.println("SEL --> "+xmlNode);
                RDFDatatype dt = org.apache.jena.datatypes.TypeMapper.getInstance().getSafeTypeByName(iri);
                
                Node node = org.apache.jena.graph.NodeFactory.createLiteral(xmlNode.toString(), dt);
                NodeValue nodeValue = new NodeValueNode(node);
                nodeValues.add(nodeValue);
            }
            return nodeValues;
        } catch (Exception e) {
            throw new ExprEvalException("FunctionBase: no evaluation", e);
        }
    }
}
