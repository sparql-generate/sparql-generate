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
package fr.emse.ci.sparqlext.xml;

import fr.emse.ci.sparqlext.utils.LogUtils;
import fr.emse.ci.sparqlext.SPARQLExt;
import fr.emse.ci.sparqlext.xml.FUN_XPath;
import fr.emse.ci.sparqlext.iterator.IteratorFunctionBase;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import fr.emse.ci.sparqlext.stream.LookUpRequest;
import fr.emse.ci.sparqlext.stream.SPARQLExtStreamManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
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
import org.w3c.dom.NodeList;
import org.apache.jena.sparql.expr.nodevalue.NodeValueBoolean;
import org.apache.jena.sparql.expr.nodevalue.NodeValueInteger;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.PrettyXmlSerializer;
import org.htmlcleaner.TagNode;

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
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class ITER_XPath extends IteratorFunctionBase {

    private static final Logger LOG = LoggerFactory.getLogger(ITER_XPath.class);

    public static final String URI = SPARQLExt.ITER + "XPath";

    private static final String XML_URI = "http://www.iana.org/assignments/media-types/application/xml";

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
		props.setOmitComments(true);
   }

    @Override
    public List<List<NodeValue>> exec(List<NodeValue> args) {
        if (args.size() < 2) {
            LOG.debug("Expecting at least two arguments.");
            throw new ExprEvalException("Expecting at least two arguments.");
        }
        final NodeValue xml = args.get(0);
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
                if (!subquery.isString()) {
                    LOG.debug("Argument " + i + " must be a String.");
                    throw new ExprEvalException("Argument " + i + " must be a String.");
                }
                subqueries[i - 2] = subquery.getString();
            }
        }

        try {
        	
        	//to clean the xmlString
        	TagNode tagNode = new HtmlCleaner(props).clean(xmlString);
            String xmlStringCleaned= new PrettyXmlSerializer(props).getAsString(tagNode);
        	//********            
            
            InputStream is = new ByteArrayInputStream(xmlStringCleaned.getBytes("UTF-8"));
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document document = builder.parse(is);
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
