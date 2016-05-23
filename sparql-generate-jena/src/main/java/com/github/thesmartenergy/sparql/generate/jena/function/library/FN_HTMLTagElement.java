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
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.apache.jena.sparql.function.FunctionBase2;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

/**
 * A SPARQL function that extracts the text from an HTML element 
 * The function URI is {@code <http://w3id.org/sparql-generate/fn/HTMLTag>}.
 * It takes two parameters as input:
 * <ul>
 * <li>{@param html} a RDF Literal with datatype URI
 * {@code <urn:iana:mime:text/html>} representing the source HTML document</li>
 * <li>{@param elementName} a RDF Literal with datatype {@code xsd:string} representing 
 * name of the HTML element from which text is to be extracted
 * </li>
 * </ul>
 * and return a RDF Literal with datatype URI {@code xsd:string} for the text of the element {@param elementName}.
 *
 * @author Noorani Bakerally
 */
public class FN_HTMLTagElement extends FunctionBase2{
    //TODO write multiple unit tests for this class.

    /**
     * The logger.
     */
    private static final Logger LOG = Logger.getLogger(FN_HTMLTagElement.class);

    /**
     * The SPARQL function URI.
     */
    public static final String URI = SPARQLGenerate.FN + "HTMLTagElement";

    /**
     * The datatype URI of the first parameter and the return literals.
     */
    private static final String datatypeUri = "urn:iana:mime:text/html";

   /**
     * {@inheritDoc }
     */
    @Override
    public NodeValue exec(NodeValue html, NodeValue v2) {
        if (html.getDatatypeURI() == null
                && datatypeUri == null
                || html.getDatatypeURI() != null
                && !html.getDatatypeURI().equals(datatypeUri)
                && !html.getDatatypeURI().equals("http://www.w3.org/2001/XMLSchema#string")) {
            LOG.warn("The URI of NodeValue1 MUST be <" + datatypeUri + ">"
                    + "or <http://www.w3.org/2001/XMLSchema#string>."
                    + " Returning null.");
        }
      
        try {
            String sourceHtml = String.valueOf(html.asNode().getLiteralValue());
            org.jsoup.nodes.Document htmldoc = Jsoup.parse(sourceHtml);
            
            String selectPath = String.valueOf(v2.asNode().getLiteralValue());
            Elements elements = htmldoc.select(selectPath);
            
            return new NodeValueString(elements.outerHtml());
        } catch (Exception e) {
            LOG.debug("Error:HTML Tag "+e.getMessage());
            throw new ExprEvalException("FunctionBase: no evaluation", e);
        }
    }
}
