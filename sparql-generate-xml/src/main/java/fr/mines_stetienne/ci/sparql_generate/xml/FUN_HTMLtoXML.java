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
package fr.mines_stetienne.ci.sparql_generate.xml;

import fr.mines_stetienne.ci.sparql_generate.utils.LogUtils;
import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.apache.jena.sparql.function.FunctionBase1;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * Binding function
 * <a href="http://w3id.org/sparql-generate/fn/HTMLtoXML">fun:HTMLtoXML</a>
 * evaluates a HTML document and returns a XML document, closing unclosed tags
 * if necessary.
 *
 * The parameters are defined as follows
 * <ul>
 * <li>Param 1: (html) is a literal that contains a HTML document;</li>
 * </ul>
 *
 * The output is a string literal.
 *
 * @author Maxime Lefrançois
 */
public class FUN_HTMLtoXML extends FunctionBase1 {

    private static final Logger LOG = LoggerFactory.getLogger(FUN_HTMLtoXML.class);

    public static final String URI = SPARQLExt.FUN + "HTMLtoXML";

    private static final String HTML_URI = "https://www.iana.org/assignments/media-types/text/html";
    
    private static final RDFDatatype DT = TypeMapper.getInstance().getSafeTypeByName(HTML_URI);


    @Override
    public NodeValue exec(NodeValue html) {
        if (html.getDatatypeURI() != null
                && !html.getDatatypeURI().equals(HTML_URI)
                && !html.getDatatypeURI().equals("http://www.w3.org/2001/XMLSchema#string")) {
            LOG.debug("The URI of NodeValue1 should be <" + HTML_URI + ">"
                    + "or <http://www.w3.org/2001/XMLSchema#string>."
            );
        }

        try {
            String sourceHtml = String.valueOf(html.asNode().getLiteralLexicalForm());

            org.jsoup.nodes.Document htmldoc = Jsoup.parse(sourceHtml);
            htmldoc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);    
            return new NodeValueString(htmldoc.html());
            
        } catch (ExprEvalException ex) {
            throw ex;
        } catch (Exception ex) {
            Node compressed = LogUtils.compress(html.asNode());
            LOG.debug("Exception while converting to XML " + compressed, ex);
            throw new ExprEvalException("Exception while converting to XML " + compressed, ex);
        }
    }

}
