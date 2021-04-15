/*
 * Copyright 2020 MINES Saint-Étienne
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
package fr.mines_stetienne.ci.sparql_generate.md;

import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.apache.jena.sparql.function.FunctionBase1;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;

/**
 * Binding function
 * <a href="http://w3id.org/sparql-generate/fn/Markdown">fun:Markdown</a> takes as input
 * a Markdown document and outputs a HTML document.
 *
 * <ul>
 * <li>Param 1 (markdown) is a Markdown document in a RDF Literal
 * with datatype URI
 * {@code <https://www.iana.org/assignments/media-types/text/markdown>} or
 * {@code xsd:string}</li>
 * </ul>
 *
 */
public final class FUN_Markdown extends FunctionBase1 {
    //TODO write multiple unit tests for this class.

    private static final Logger LOG = LoggerFactory.getLogger(FUN_Markdown.class);

    public static final String URI = SPARQLExt.FUN + "markdownToHTML";

    private static final String datatypeUri = "https://www.iana.org/assignments/media-types/text/markdown";

    @Override
    public NodeValue exec(NodeValue markdown) {
        if(markdown == null) {
        	String msg = "Expects one argument";
            LOG.debug(msg);
        	throw new ExprEvalException(msg);
        }
        if (markdown.getDatatypeURI() != null
                && !markdown.getDatatypeURI().equals(datatypeUri)
                && !markdown.getDatatypeURI().equals("http://www.w3.org/2001/XMLSchema#string")) {
            LOG.debug("The URI of NodeValue1 must be <" + datatypeUri + ">"
                    + "or <http://www.w3.org/2001/XMLSchema#string>."
            );
        }
        try {
        	String md = markdown.asNode().getLiteralLexicalForm();
	        Parser parser = Parser.builder().build();
	        Node document = parser.parse(md);
	        HtmlRenderer renderer = HtmlRenderer.builder().build();
	        String html = renderer.render(document);
	        return new NodeValueString(html);
        } catch (Exception ex) {
            throw new ExprEvalException("FunctionBase: no evaluation", ex);
        }
    }
}
