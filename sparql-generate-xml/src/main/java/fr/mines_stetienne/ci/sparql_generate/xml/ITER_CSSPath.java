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
import fr.mines_stetienne.ci.sparql_generate.iterator.IteratorFunctionBase;
import fr.mines_stetienne.ci.sparql_generate.stream.LookUpRequest;
import fr.mines_stetienne.ci.sparql_generate.stream.SPARQLExtStreamManager;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import java.util.List;
import java.util.Objects;
import org.apache.commons.io.IOUtils;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.riot.SysRIOT;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueBoolean;
import org.apache.jena.sparql.expr.nodevalue.NodeValueInteger;
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
 * extracts parts of a HTML document, using CSS-Selector-like queries.
 *
 * <p>
 * See
 * <a href="https://w3id.org/sparql-generate/playground.html#ex=example/generate/Example-CSSPath">Live
 * example</a></p>
 *
 * <ul>
 * <li>Param 1: (html): the URI of the HTML document (a URI), or the HTML
 * document itself (a String);</li>
 * <li>Param 2: (cssSelector) is the CSS Selector. See
 * <a href="https://jsoup.org/apidocs/org/jsoup/select/Selector.html">
 * https://jsoup.org/apidocs/org/jsoup/select/Selector.html</a> for the base
 * syntax specification.
 * <li>Param 3 .. N : (auxCssSelector ... ) other CSS Selectors, which will be
 * executed over each of the results of the execution of xPath, exactly as if
 * the binding function
 * <a href="http://w3id.org/sparql-generate/fun/CSSPath">fun:CSSPath</a> was
 * applied. By default, the output is the outer HTML of the first matched
 * element. However, two additions to the CSS Selector syntax can change this
 * behaviour:</li>
 * <ul>
 * <li>(if the selector ends with <code>/text()</code>) the output is the
 * combined text of the first matched element and all its children. Whitespaces
 * are normalized and trimmed.</li>
 * <li>(if the selector ends with <code>@attributeName</code>) the output is the
 * value of the attribute <code>attributeName</code> for the first matched
 * element.</li>
 * </ul></li>
 * </ul>
 *
 * The following variables may be bound:
 *
 * <ul>
 * <li>Output 1: (string) outer HTML of the matched element;</li>
 * <li>Output 2 .. N-1: (string) result of the execution of the auxiliary CSS
 * Selector queries on Output 1, encoded as literals;</li>
 * <li>Output N: (integer) the position of the result in the list;</li>
 * <li>Output N+1: (boolean) true if this result has a next result in the
 * list.</li>
 * </ul>
 *
 * Output N and N+1 can be used to generate RDF lists from the input, but the
 * use of keyword LIST( ?var ) as the object of a triple pattern covers most
 * cases more elegantly.
 *
 * @author Noorani Bakerally <noorani.bakerally at emse.fr>
 * @author Maxime Lefrançois
 */
public class ITER_CSSPath extends IteratorFunctionBase {

    private static final Logger LOG = LoggerFactory.getLogger(ITER_CSSPath.class);

    public static final String URI = SPARQLExt.ITER + "CSSPath";

    private static final String HTML_URI = "https://www.iana.org/assignments/media-types/text/html";

    private static final NodeValue TRUE = new NodeValueBoolean(true);

    private static final NodeValue FALSE = new NodeValueBoolean(false);

    private static final FUN_CSSPath function = new FUN_CSSPath();

    private static final RDFDatatype DT = TypeMapper.getInstance().getSafeTypeByName(HTML_URI);

    @Override
    public List<List<NodeValue>> exec(List<NodeValue> args) {
        if (args.size() < 2) {
            LOG.debug("Expecting at least two arguments.");
            throw new ExprEvalException("Expecting at least two arguments.");
        }
        final NodeValue html = args.get(0);
        if(html == null) {
        	String msg = "No HTML provided";
            LOG.debug(msg);
        	throw new ExprEvalException(msg);
        }
        if (!html.isIRI() && !html.isString() && !html.asNode().isLiteral()) {
            LOG.debug("First argument must be a URI or a String.");
            throw new ExprEvalException("First argument must be a URI or a String.");
        }
        if (!html.isIRI() && html.getDatatypeURI() != null
                && !html.getDatatypeURI().equals(HTML_URI)
                && !html.getDatatypeURI().equals("http://www.w3.org/2001/XMLSchema#string")) {
            LOG.debug("The datatype of the first argument should be"
                    + " <" + HTML_URI + "> or"
                    + " <http://www.w3.org/2001/XMLSchema#string>. Got "
                    + html.getDatatypeURI());
        }

        final NodeValue cssSelectorNode = args.get(1);
        if (!cssSelectorNode.isString()) {
            LOG.debug("Second argument must be a String.");
            throw new ExprEvalException("Second argument must be a String.");
        }

        String[] subqueries = new String[args.size() - 2];
        if (args.size() > 2) {
            for (int i = 2; i < args.size(); i++) {
                final NodeValue subquery = args.get(i);
                if(subquery == null) {
                	subqueries[i - 2] = null;
    			}
                if (!subquery.isString()) {
                    LOG.debug("Argument " + i + " must be a String.");
                    throw new ExprEvalException("Argument " + i + " must be a String.");
                }
                subqueries[i - 2] = subquery.getString();
            }
        }

        try {

            String htmlString = getString(html);
            Document htmldoc = Jsoup.parse(htmlString);
            Elements elements = htmldoc.select(cssSelectorNode.getString());
            int size = elements.size();
            final List<List<NodeValue>> listNodeValues = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                Element element = elements.get(i);
                List<NodeValue> nodeValues = new ArrayList<>(args.size() + 1);
                Node n = NodeFactory.createLiteral(element.outerHtml(), DT);
                nodeValues.add(new NodeValueNode(n));
                for (String subquery : subqueries) {
                    try {
                        nodeValues.add(function.select(element, subquery));
                    } catch (Exception ex) {
                        if(LOG.isDebugEnabled()) {
                            Node compressed = LogUtils.compress(n);
                            LOG.debug("No evaluation for " + compressed + ", " + subquery, ex);
                        }
                        nodeValues.add(null);
                    }
                }
                nodeValues.add(new NodeValueInteger(i));
                nodeValues.add((i == size - 1) ? TRUE : FALSE);
                listNodeValues.add(nodeValues);
            }
            return listNodeValues;
        } catch (Exception ex) {
            if(LOG.isDebugEnabled()) {
                Node compressed = LogUtils.compress(html.asNode());
                LOG.debug("No evaluation of " + compressed + ", " + cssSelectorNode, ex);
            }
            throw new ExprEvalException("No evaluation of " + cssSelectorNode, ex);
        }
    }

    private String getString(NodeValue html) throws ExprEvalException {
        if (html.isString()) {
            return html.getString();
        } else if (html.isLiteral() && html.asNode().getLiteralDatatypeURI().equals(HTML_URI)) {
            return html.asNode().getLiteralLexicalForm();
        } else if (!html.isIRI()) {
            String message = String.format("First argument must be a URI or a String");
            LOG.warn(message);
            throw new ExprEvalException(message);
        }
        String htmlPath = html.asNode().getURI();
        LookUpRequest req = new LookUpRequest(htmlPath, "text/html");
        final SPARQLExtStreamManager sm = (SPARQLExtStreamManager) getContext().get(SysRIOT.sysStreamManager);
        Objects.requireNonNull(sm);
        TypedInputStream tin = sm.open(req);
        if (tin == null) {
            String message = String.format("Could not look up html document %s", htmlPath);
            LOG.warn(message);
            throw new ExprEvalException(message);
        }
        try {
            return IOUtils.toString(tin.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new ExprEvalException("IOException while looking up html document " + htmlPath, ex);
        }
    }

    @Override
    public void checkBuild(ExprList args) {
        Objects.nonNull(args);
    }

}
