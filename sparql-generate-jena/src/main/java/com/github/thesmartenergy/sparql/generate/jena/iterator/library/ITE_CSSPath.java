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
import java.util.ArrayList;
import com.github.thesmartenergy.sparql.generate.jena.iterator.IteratorFunctionBase2;
import java.util.List;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * A SPARQL Iterator function that extracts a list of HTML elements from a HTML
 * element, according to a CSS Selector expression. It can be used to select
 * HTML elements based on their name, id, classes, types, attributes, values of
 * attributes etc. The Iterator function URI is
 * {@code <http://w3id.org/sparql-generate/iter/CSSPath>}. 
 *
 * @author Noorani Bakerally <noorani.bakerally at emse.fr>
 */
public class ITE_CSSPath extends IteratorFunctionBase2 {

    /**
     * The logger.
     */
    private static final Logger LOG = LogManager.getLogger(ITE_CSSPath.class);

    /**
     * The SPARQL function URI.
     */
    public static final String URI = SPARQLGenerate.ITER + "CSSPath";

    /**
     * The datatype URI of the first parameter and the return literals.
     */
    private static final String datatypeUri = "http://www.iana.org/assignments/media-types/text/html";

    /**
     * @param html a RDF Literal with datatype URI
     * {@code <http://www.iana.org/assignments/media-types/text/html>}  or {@code xsd:string} representing the source HTML document
     * @param cssSelector a RDF Literal with datatype {@code xsd:string}
     * representing the CSS selector expression
     * @return a list of RDF Literal with datatype URI
     * {@code <http://www.iana.org/assignments/media-types/text/html>} .
     */
    @Override
    public List<NodeValue> exec(NodeValue html, NodeValue cssSelector) {
        if (html.getDatatypeURI() != null
                && !html.getDatatypeURI().equals(datatypeUri)
                && !html.getDatatypeURI().equals("http://www.w3.org/2001/XMLSchema#string")) {
            LOG.warn("The URI of NodeValue1 MUST be"
                    + " <" + datatypeUri + "> or"
                    + " <http://www.w3.org/2001/XMLSchema#string>. Got <"
                    + html.getDatatypeURI() + ">. Returning null.");
        }
        RDFDatatype dt = TypeMapper.getInstance()
                .getSafeTypeByName(datatypeUri);
        try {

            String sourceHtml = String.valueOf(html.asNode().getLiteralLexicalForm());
            Document htmldoc = Jsoup.parse(sourceHtml);

            String selectPath = String.valueOf(cssSelector.asNode().getLiteralValue());
            Elements elements = htmldoc.select(selectPath);

            LOG.debug("===> Number of iterations for " + cssSelector + " " + elements.size());

            List<NodeValue> nodeValues = new ArrayList<>(elements.size());

            NodeValue nodeValue;
            for (Element element : elements) {
                String htmlValue = element.toString();
                Node node = NodeFactory.createLiteral(htmlValue, dt);
                nodeValue = new NodeValueNode(node);

                nodeValues.add(nodeValue);
            }

            return nodeValues;
        } catch (Exception e) {
            throw new ExprEvalException("FunctionBase: no evaluation", e);
        }
    }
}
