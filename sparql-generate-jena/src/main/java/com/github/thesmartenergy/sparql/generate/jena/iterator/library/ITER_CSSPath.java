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

import java.util.Collections;
import java.util.List;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Iterator function
 * <a href="http://w3id.org/sparql-generate/iter/CSSPath">iter:CSSPath</a>
 * extracts a list of HTML elements from a HTML document, according to a
 * Selector CSS-like query.
 *
 * <ul>
 * <li>Param 1: (html) is a literal that contains a HTML element;</li>
 * <li>Param 2: (cssSelector) is the CSS-like query. See
 * https://jsoup.org/apidocs/org/jsoup/select/Selector.html for the syntax
 * specification;</li>
 * </ul>
 *
 * @author Noorani Bakerally <noorani.bakerally at emse.fr>
 */
public class ITER_CSSPath extends IteratorFunctionBase2 {

    private static final Logger LOG = LoggerFactory.getLogger(ITER_CSSPath.class);

    public static final String URI = SPARQLGenerate.ITER + "CSSPath";

    private static final String datatypeUri = "http://www.iana.org/assignments/media-types/text/html";

    @Override
    public List<List<NodeValue>> exec(NodeValue html, NodeValue cssSelector) {
        if (html.getDatatypeURI() != null
                && !html.getDatatypeURI().equals(datatypeUri)
                && !html.getDatatypeURI().equals("http://www.w3.org/2001/XMLSchema#string")) {
            LOG.debug("The URI of NodeValue1 MUST be"
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

            return new ArrayList<>(Collections.singletonList(nodeValues));
        } catch (Exception ex) {
            LOG.debug("No evaluation of " + html + ", " + cssSelector, ex);
            throw new ExprEvalException("No evaluation of " + html + ", " + cssSelector, ex);
        }
    }
}
