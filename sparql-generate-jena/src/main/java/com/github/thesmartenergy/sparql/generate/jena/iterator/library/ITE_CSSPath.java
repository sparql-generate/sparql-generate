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
import java.util.ArrayList;
import com.github.thesmartenergy.sparql.generate.jena.iterator.IteratorFunctionBase2;
import java.io.StringWriter;
import java.util.List;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;

import org.apache.log4j.Logger;

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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * A SPARQL Iterator function that extracts a list of sub-XML elements of a
 * XML root element, according to a XPath expression. The Iterator function URI is
 * {@code <http://w3id.org/sparql-generate/ite/XPath>}.
 * It takes two parameters as input:
 * <ul>
 * <li>a RDF Literal with datatype URI
 * {@code <urn:iana:mime:application/xml>}</li>
 * <li>a RDF Literal with datatype {@code xsd:string}</li>
 * </ul>
 * and returns a list of RDF Literal with datatype URI
 * {@code <urn:iana:mime:application/xml>}.
 *
 * @author maxime.lefrancois
 */
public class ITE_CSSPath extends IteratorFunctionBase2 {

    /**
     * The logger.
     */
    private static final Logger LOG = Logger.getLogger(ITE_CSSPath.class);

    /**
     * The SPARQL function URI.
     */
    public static final String URI = SPARQLGenerate.ITE + "CSSPath";

    /**
     * The datatype URI of the first parameter and the return literals.
     */
    private static final String datatypeUri = "urn:iana:mime:text/html";

    @Override
    public List<NodeValue> exec(NodeValue html, NodeValue v2) {
        if (html.getDatatypeURI() == null
                && datatypeUri == null
                || html.getDatatypeURI() != null
                && html.getDatatypeURI().equals(datatypeUri)
                && !html.getDatatypeURI().equals("http://www.w3.org/2001/XMLSchema#string")) {
            LOG.warn("The URI of NodeValue1 MUST be"
                    + " <" + datatypeUri + "> or"
                    + " <http://www.w3.org/2001/XMLSchema#string>. Got <"
                    + html.getDatatypeURI() + ">. Returning null.");
        }
       
        try {
            
            String sourceHtml = String.valueOf(html.asNode().getLiteralValue());
            Document htmldoc = Jsoup.parse(sourceHtml);
            
            String selectPath = String.valueOf(v2.asNode().getLiteralValue());
            Elements elements = htmldoc.select(selectPath);
            
            LOG.debug("===> Number of iterations for "+v2+" "+elements.size());
            
            List<NodeValue> nodeValues = new ArrayList<>(elements.size());
            
            NodeValue nodeValue;
            for (Element element:elements){
                String htmlValue = element.toString();
                Node node = NodeFactory.createLiteral(htmlValue);
                nodeValue = new NodeValueNode(node);
                
                nodeValues.add(nodeValue);
            }
            
            return nodeValues;
        } catch (Exception e) {
            throw new ExprEvalException("FunctionBase: no evaluation", e);
        }
    }
}