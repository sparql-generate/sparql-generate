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

/**
 * Binding function
 * <a href="http://w3id.org/sparql-generate/fn/HTMLAttribute">fun:HTMLAttribute</a>
 * extracts an attribute in an HTML element.
 *
 * <ul>
 * <li>Param 1 (html) is a literal that contains a HTML element;</li>
 * <li>Param 2 (attributeName) is the name of the attribute;</li>
 * <li>Result is the value of the attribute.</li>
 * </ul>
 *
 * @author Noorani Bakerally <noorani.bakerally at emse.fr>
 */
public class FUN_HTMLAttribute extends FunctionBase2 {
    //TODO write multiple unit tests for this class.

    private static final Logger LOG = LoggerFactory.getLogger(FUN_HTMLAttribute.class);

    public static final String URI = SPARQLGenerate.FUN + "HTMLAttribute";

    private static final String datatypeUri = "http://www.iana.org/assignments/media-types/text/html";

    public NodeValue exec(NodeValue html, NodeValue attributeName) {
        if (html.getDatatypeURI() != null
                && !html.getDatatypeURI().equals(datatypeUri)
                && !html.getDatatypeURI().equals("http://www.w3.org/2001/XMLSchema#string")) {
            LOG.warn("The URI of NodeValue1 MUST be <" + datatypeUri + ">"
                    + "or <http://www.w3.org/2001/XMLSchema#string>."
            );
        }

        try {
            String sourceHtml = String.valueOf(html.asNode().getLiteralLexicalForm());
            org.jsoup.nodes.Document htmldoc = Jsoup.parseBodyFragment(sourceHtml);
            String attributeNameValue = String.valueOf(attributeName.asNode().getLiteralValue());
            return new NodeValueString(htmldoc.body().child(0).attributes().get(attributeNameValue));
        } catch (Exception ex) {
            LOG.debug("No evaluation of " + html + ", " + attributeName, ex);
            throw new ExprEvalException("No evaluation of " + html + ", " + attributeName, ex);
        }
    }

}
