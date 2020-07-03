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
package fr.emse.ci.sparqlext.xml;

import fr.emse.ci.sparqlext.utils.LogUtils;
import fr.emse.ci.sparqlext.SPARQLExt;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.apache.jena.sparql.function.FunctionBase2;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Binding function
 * <a href="http://w3id.org/sparql-generate/fn/CSSPath">fun:CSSPath</a>
 * evaluates a CSS Selector over a HTML document and can return (a) the outer
 * HTML, (b) the inner text, or (c) the value of an attribute.
 *
 * The parameters are defined as follows
 * <ul>
 * <li>Param 1: (html) is a literal that contains a HTML document;</li>
 * <li>Param 2: (cssSelector) is the CSS Selector. See
 * <a href="https://jsoup.org/apidocs/org/jsoup/select/Selector.html">
 * https://jsoup.org/apidocs/org/jsoup/select/Selector.html</a> for the base
 * syntax specification. By default, the output is the outer HTML of the first matched
 * element. However, two additions to the CSS Selector syntax can change
 * this behaviour:</li>
 * <ul>
 * <li>(if the selector ends with <code>/text()</code>) the output is the
 * combined text of the first matched element and all its children. Whitespace
 * is normalized and trimmed.</li>
 * <li>(if the selector ends with <code>@attributeName</code>) the output is the
 * value of the attribute <code>attributeName</code> for the first matched
 * element.</li>
 * </ul>
 * </ul>
 *
 * The output is a string literal.
 *
 * @author Maxime Lefrançois
 */
public class FUN_CSSPath extends FunctionBase2 {

    private static final Logger LOG = LoggerFactory.getLogger(FUN_CSSPath.class);

    public static final String URI = SPARQLExt.FUN + "CSSPath";

    private static final String HTML_URI = "http://www.iana.org/assignments/media-types/text/html";
    
    private static final RDFDatatype DT = TypeMapper.getInstance().getSafeTypeByName(HTML_URI);


    @Override
    public NodeValue exec(NodeValue html, NodeValue query) {
        if (html.getDatatypeURI() != null
                && !html.getDatatypeURI().equals(HTML_URI)
                && !html.getDatatypeURI().equals("http://www.w3.org/2001/XMLSchema#string")) {
            LOG.debug("The URI of NodeValue1 should be <" + HTML_URI + ">"
                    + "or <http://www.w3.org/2001/XMLSchema#string>."
            );
        }
        if (!query.isString()) {
            LOG.debug("The second argument should be a string. Got " + query);
        }

        try {
            String sourceHtml = String.valueOf(html.asNode().getLiteralLexicalForm());
            String selectPath = String.valueOf(query.asNode().getLiteralLexicalForm());

//            for tag element was
//            org.jsoup.nodes.Document htmldoc = Jsoup.parse(sourceHtml);
//            for attribute was 
//            org.jsoup.nodes.Document htmldoc = Jsoup.parseBodyFragment(sourceHtml);
//            for tag was
//            org.jsoup.nodes.Document htmldoc = Jsoup.parse(sourceHtml, "", Parser.xmlParser());
            org.jsoup.nodes.Document htmldoc = Jsoup.parse(sourceHtml);
            return select(htmldoc, selectPath);
        } catch (ExprEvalException ex) {
            throw ex;
        } catch (Exception ex) {
            if(LOG.isDebugEnabled()) {
                Node compressed = LogUtils.compress(html.asNode());
                LOG.debug("No evaluation of " + compressed + ", " + query, ex);
            }
            throw new ExprEvalException("No evaluation of" + query, ex);
        }
    }

    public NodeValue select(Element htmldoc, String selectPath) throws ExprEvalException {
        if (selectPath.endsWith("/text()")) {
            selectPath = selectPath.substring(0, selectPath.length() - 7);
            return selectText(htmldoc, selectPath);
        } else if (selectPath.matches(".*@[^\"'>/=^$*~]+$")) {
            final String attributeName = selectPath.substring(1 + selectPath.lastIndexOf("@"));
            selectPath = selectPath.replaceAll("@[^\"'>/=^$*~@]+$", "");
            return selectAttribute(htmldoc, selectPath, attributeName);
        } else {
            return selectElement(htmldoc, selectPath);
        }
    }

    private NodeValue selectText(Element element, String selectPath) throws ExprEvalException {
        Elements elements = element.select(selectPath);
        Element e = elements.first();
        if (e == null) {
            throw new ExprEvalException("No evaluation of " + element + ", " + selectPath);
        }
        return new NodeValueString(e.text());
    }

    private NodeValue selectAttribute(Element element, String selectPath, String attributeName) {
        Elements elements = element.select(selectPath);
        Element e = elements.first();
        if (e == null) {
            throw new ExprEvalException("No evaluation of " + element + ", " + selectPath);
        }
        if (!e.hasAttr(attributeName)) {
            throw new ExprEvalException("The evaluation of " + element + ", " + selectPath + " is an element that does not have attribute " + attributeName);
        }
        return new NodeValueString(e.attr(attributeName));
    }

    private NodeValue selectElement(Element element, String selectPath) {
        Elements elements = element.select(selectPath);
        Element e = elements.first();
        if (e == null) {
            throw new ExprEvalException("No evaluation of " + element + ", " + selectPath);
        }
        Node n = NodeFactory.createLiteral(e.outerHtml(), DT);
        return new NodeValueNode(n);
    }

}
