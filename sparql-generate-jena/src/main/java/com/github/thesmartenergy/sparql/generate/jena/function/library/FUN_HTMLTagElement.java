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
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.apache.jena.sparql.function.FunctionBase2;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

/**
 * Binding function
 * <a href="http://w3id.org/sparql-generate/fn/HTMLTagElement">fun:HTMLTagElement</a>
 * extracts the text of HTML elements including attributes and the inner html
 * that match the given Selector CSS-like query. The syntax for the CSS-like
 * query is https://jsoup.org/apidocs/org/jsoup/select/Selector.html .
 *
 * <ul>
 * <li>Param 1 is a HTML document;</li>
 * <li>Param 2 is the CSS-like query. See
 * https://jsoup.org/apidocs/org/jsoup/select/Selector.html for the syntax
 * specification;/li>
 * <li>Result is a xsd:string.</li>
 * </ul>
 *
 * @author Noorani Bakerally <noorani.bakerally at emse.fr>
 */
public class FUN_HTMLTagElement extends FunctionBase2 {
    //TODO write multiple unit tests for this class.

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(FUN_HTMLTagElement.class);

    /**
     * The SPARQL function URI.
     */
    public static final String URI = SPARQLGenerate.FUN + "HTMLTagElement";

    /**
     * The datatype URI of the first parameter and the return literals.
     */
    private static final String datatypeUri = "http://www.iana.org/assignments/media-types/text/html";

    /**
     * {@inheritDoc }
     */
    @Override
    public NodeValue exec(NodeValue html, NodeValue v2) {
        if (html.getDatatypeURI() != null
                && !html.getDatatypeURI().equals(datatypeUri)
                && !html.getDatatypeURI().equals("http://www.w3.org/2001/XMLSchema#string")) {
            LOG.debug("The URI of NodeValue1 MUST be <" + datatypeUri + ">"
                    + "or <http://www.w3.org/2001/XMLSchema#string>."
            );
        }

        try {
            String sourceHtml = String.valueOf(html.asNode().getLiteralLexicalForm());
            org.jsoup.nodes.Document htmldoc = Jsoup.parse(sourceHtml);

            String selectPath = String.valueOf(v2.asNode().getLiteralLexicalForm());
            Elements elements = htmldoc.select(selectPath);

            return new NodeValueString(elements.outerHtml());
        } catch (Exception ex) {
            LOG.debug("No evaluation of " + html + ", " + v2, ex);
            throw new ExprEvalException("No evaluation of " + html + ", " + v2, ex);
        }
    }
}
