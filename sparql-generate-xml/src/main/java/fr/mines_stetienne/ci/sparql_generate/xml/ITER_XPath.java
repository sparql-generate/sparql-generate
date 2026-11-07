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
import fr.mines_stetienne.ci.sparql_generate.xml.FUN_XPath;
import fr.mines_stetienne.ci.sparql_generate.iterator.IteratorFunctionBase;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import fr.mines_stetienne.ci.sparql_generate.stream.LookUpRequest;
import fr.mines_stetienne.ci.sparql_generate.stream.SPARQLExtStreamManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.List;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.apache.jena.sparql.expr.nodevalue.NodeValueBoolean;
import org.apache.jena.sparql.expr.nodevalue.NodeValueInteger;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.PrettyXmlSerializer;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.CleanerProperties;

import java.nio.charset.StandardCharsets;

import java.util.Objects;
import javax.xml.transform.TransformerFactory;
import org.apache.commons.io.IOUtils;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.riot.SysRIOT;
import org.apache.jena.sparql.expr.ExprList;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Iterator function
 * <a href="http://w3id.org/sparql-generate/iter/XPath">iter:XPath</a>
 * extracts parts of a XML document, using XPath queries.
 *
 * <p>
 * See
 * <a href="https://w3id.org/sparql-generate/playground.html#ex=example/generate/Example-XML">Live
 * example</a></p>
 *
 * <ul>
 * <li>Param 1: (xml): the URI of the XML document (a URI), or the XML document
 * itself (a String);</li>
 * <li>Param 2: (xPath) the XPath query;</li>
 * <li>Param 3 .. N : (auxXPath ... ) other XPath queries, which will be
 * executed over the results of the execution of xPath, and provide one result
 * each.</li>
 * </ul>
 *
 * The following variables may be bound:
 *
 * <ul>
 * <li>Output 1: (literal) matched XML element, encoded as a boolean, float,
 * double, integer, string, as it best fits;</li>
 * <li>Output 2 .. N-1: (string) result of the execution of the auxiliary XPath
 * queries on Output 1, encoded as a boolean, float, double, integer, string, as
 * it best fits;</li>
 * <li>Output N: (integer) the position of the result in the list;</li>
 * <li>Output N+1: (boolean) true if this result has a next result in the
 * list.</li>
 * </ul>
 *
 * Output N and N+1 can be used to generate RDF lists from the input, but the
 * use of keyword LIST( ?var ) as the object of a triple pattern covers most
 * cases more elegantly.
 *
 * @author Maxime Lefrançois
 */
public class ITER_XPath extends IteratorFunctionBase {

    private static final Logger LOG = LoggerFactory.getLogger(ITER_XPath.class);

    public static final String URI = SPARQLExt.ITER + "XPath";

    private static final String XML_URI = "https://www.iana.org/assignments/media-types/application/xml";

    private static final NodeValue TRUE = new NodeValueBoolean(true);

    private static final NodeValue FALSE = new NodeValueBoolean(false);

    private static final FUN_XPath function = new FUN_XPath();

    private static final RDFDatatype DT = TypeMapper.getInstance().getSafeTypeByName(XML_URI);

    private static final DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();

    private static final TransformerFactory TRANSFORMER_FACTORY = TransformerFactory.newInstance();

    private static final CleanerProperties props = new CleanerProperties();
    
    static {
        builderFactory.setNamespaceAware(true);
        
        props.setTranslateSpecialEntities(true);
		props.setTransResCharsToNCR(true);
        props.setOmitHtmlEnvelope(true);
		props.setOmitComments(true);
   }

    @Override
    public List<List<NodeValue>> exec(List<NodeValue> args) {
        if (args.size() < 2) {
            LOG.debug("Expecting at least two arguments.");
            throw new ExprEvalException("Expecting at least two arguments.");
        }
        final NodeValue xml = args.get(0);
        if(xml == null) {
        	String msg = "No XML provided";
            LOG.debug(msg);
        	throw new ExprEvalException(msg);
        }
        if (!xml.isIRI() && !xml.isString() && !xml.asNode().isLiteral()) {
            LOG.debug("First argument must be a URI or a String.");
            throw new ExprEvalException("First argument must be a URI or a String.");
        }
        if (!xml.isIRI() && xml.getDatatypeURI() != null
                && !xml.getDatatypeURI().equals(XML_URI)
                && !xml.getDatatypeURI().equals("http://www.w3.org/2001/XMLSchema#string")) {
            LOG.debug("The datatype of the first argument should be"
                    + " <" + XML_URI + "> or"
                    + " <http://www.w3.org/2001/XMLSchema#string>. Got "
                    + xml.getDatatypeURI());
        }

        // @TODO: FIND A BETTER WAY TO MANAGE NAMESPACES
        String xmlString = getString(xml).replaceAll("xmlns=\"[^\"]*\"", "");;

        final NodeValue xPathNode = args.get(1);
        if (!xPathNode.isString()) {
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
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            builder.setErrorHandler(SILENT);
            Document document = parse(builder, xmlString);
            XPath xPath = XPathFactory.newInstance().newXPath();
            xPath.setNamespaceContext(new FUN_XPath.UniversalNamespaceResolver(document));
            NodeList nodeList = (NodeList) xPath
                    .compile(xPathNode.getString())
                    .evaluate(document, XPathConstants.NODESET);
            int size = nodeList.getLength();
            final List<List<NodeValue>> listNodeValues = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                org.w3c.dom.Node value = nodeList.item(i);
                List<NodeValue> nodeValues = new ArrayList<>(args.size() + 1);
                NodeValue nodeValue = function.nodeForNode(value);
                nodeValues.add(nodeValue);
                for (String subquery : subqueries) {
                    final Object simple = simpleSelect(value, subquery);
                    if (simple != NOT_SIMPLE) {
                        nodeValues.add(function.nodeForNode((org.w3c.dom.Node) simple));
                        continue;
                    }
                    try {
                        InputStream subis = new ByteArrayInputStream(nodeValue.asString().getBytes("UTF-8"));
                        Document subDocument = builder.parse(subis);
                        org.w3c.dom.Node subvalue = (org.w3c.dom.Node) xPath
                                .compile(subquery)
                                .evaluate(subDocument, XPathConstants.NODE);
//                        LOG.trace("subvalue " + subvalue);
                        nodeValues.add(function.nodeForNode(subvalue));
                    } catch (Exception ex) {
                        LOG.debug("No evaluation for " + value + ", " + subquery, ex);
                        nodeValues.add(null);
                    }
                }
                nodeValues.add(new NodeValueInteger(i));
                nodeValues.add((i == size - 1) ? FALSE : TRUE);
                listNodeValues.add(nodeValues);
            }
            return listNodeValues;
        } catch (Exception ex) {
            if (LOG.isDebugEnabled()) {
                String compressed = LogUtils.compress(xmlString);
                LOG.debug("No evaluation for " + compressed + ", " + xPathNode, ex);
            }
            throw new ExprEvalException("No evaluation for " + xPathNode, ex);
        }
    }

    /**
     * False to evaluate every sub-query with the XPath engine (tests).
     */
    static volatile boolean simplePaths = true;

    private static final Object NOT_SIMPLE = new Object();

    /**
     * A path of element names from the root, ending with an attribute or
     * text(), as "/person/@id" or "/person/name/text()".
     */
    private static final Pattern SIMPLE_PATH = Pattern
            .compile("(/[A-Za-z_][A-Za-z0-9_.-]*)+(/@[A-Za-z_][A-Za-z0-9_.-]*|/text\\(\\))");

    /**
     * Evaluates a sub-query on a matched element without the XPath engine,
     * which builds new tables for each evaluation, when the sub-query is a
     * path of element names ending with an attribute or text(). The value is
     * that of the XPath engine on a document made of the element: the first
     * node of the path in document order, or null.
     *
     * @return the node, null, or NOT_SIMPLE if the path is not simple.
     */
    static Object simpleSelect(final org.w3c.dom.Node root, final String path) {
        if (!simplePaths || path == null || !(root instanceof Element) || !SIMPLE_PATH.matcher(path).matches()) {
            return NOT_SIMPLE;
        }
        final String[] steps = path.substring(1).split("/");
        final String last = steps[steps.length - 1];
        if (!steps[0].equals(root.getNodeName())) {
            return null;
        }
        return find((Element) root, steps, 1, last);
    }

    private static org.w3c.dom.Node find(final Element element, final String[] steps, final int k, final String last) {
        if (k == steps.length - 1) {
            if (last.startsWith("@")) {
                return element.getAttributeNode(last.substring(1));
            }
            for (org.w3c.dom.Node c = element.getFirstChild(); c != null; c = c.getNextSibling()) {
                if (c.getNodeType() == org.w3c.dom.Node.TEXT_NODE
                        || c.getNodeType() == org.w3c.dom.Node.CDATA_SECTION_NODE) {
                    return c;
                }
            }
            return null;
        }
        for (org.w3c.dom.Node c = element.getFirstChild(); c != null; c = c.getNextSibling()) {
            if (c.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE && steps[k].equals(c.getNodeName())) {
                final org.w3c.dom.Node found = find((Element) c, steps, k + 1, last);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * Parses the document as XML. Only if it is not well-formed, such as HTML
     * with unclosed elements, clean it first with HtmlCleaner: cleaning
     * well-formed XML would lower the case of names and restructure elements
     * that have an HTML name (body, head, title, p, ...).
     */
    private Document parse(DocumentBuilder builder, String xmlString) throws IOException, SAXException {
        try {
            return builder.parse(new InputSource(new StringReader(xmlString)));
        } catch (SAXParseException ex) {
            LOG.debug("Not well-formed XML, cleaning it as HTML: " + ex.getMessage());
        }
        TagNode tagNode = new HtmlCleaner(props).clean(xmlString);
        String xmlStringCleaned = new PrettyXmlSerializer(props).getAsString(tagNode);
        return builder.parse(new InputSource(new StringReader(xmlStringCleaned)));
    }

    private static final ErrorHandler SILENT = new ErrorHandler() {
        @Override
        public void warning(SAXParseException exception) {
        }

        @Override
        public void error(SAXParseException exception) {
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            throw exception;
        }
    };

    private String getString(NodeValue xml) throws ExprEvalException {
        if (xml.isString()) {
            return xml.getString();
        } else if (xml.isLiteral() && xml.asNode().getLiteralDatatypeURI().equals(XML_URI)) {
            return xml.asNode().getLiteralLexicalForm();
        } else if (!xml.isIRI()) {
            String message = String.format("First argument must be a URI or a String");
            LOG.warn(message);
            throw new ExprEvalException(message);
        }
        String xmlPath = xml.asNode().getURI();
        String acceptHeader = "application/xml";
        LookUpRequest req = new LookUpRequest(xmlPath, acceptHeader);
        final SPARQLExtStreamManager sm = (SPARQLExtStreamManager) getContext().get(SysRIOT.sysStreamManager);
        Objects.requireNonNull(sm);
        TypedInputStream tin = sm.open(req);
        if (tin == null) {
            String message = String.format("Could not look up xml document %s", xmlPath);
            LOG.warn(message);
            throw new ExprEvalException(message);
        }

        try {
            String output = IOUtils.toString(tin.getInputStream(), StandardCharsets.UTF_8);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Loaded <" + xmlPath + "> ACCEPT "
                        + acceptHeader + ". Enable TRACE level for more.");
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Loaded <" + xmlPath + "> ACCEPT "
                            + acceptHeader + ". returned\n" + LogUtils.compress(output));
                }
            }
            return output;
        } catch (IOException ex) {
            throw new ExprEvalException("IOException while looking up xml document " + xmlPath, ex);
        }
    }

    @Override
    public void checkBuild(ExprList args) {
        Objects.nonNull(args);
    }
}
