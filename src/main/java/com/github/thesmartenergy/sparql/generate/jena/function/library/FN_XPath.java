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

import java.io.ByteArrayInputStream;
import com.github.thesmartenergy.sparql.generate.jena.selector.library.SEL_JSONPath;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.apache.jena.sparql.function.FunctionBase2;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

/**
 *
 * @author maxime.lefrancois
 */
public class FN_XPath extends FunctionBase2 {

    private static final String uri = "urn:iana:mime:application/json";

    @Override
    public NodeValue exec(NodeValue v1, NodeValue v2) {
        if (v1.getDatatypeURI() == null ? uri != null : !v1.getDatatypeURI().equals(uri)) {
            Logger.getLogger(SEL_JSONPath.class).warn("The URI of NodeValue1 MUST be <" + uri + ">. Returning null.");
        }

        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        try {
            builder = builderFactory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(v1.asNode().getLiteralLexicalForm().getBytes()));
            
            XPath xPath =  XPathFactory.newInstance().newXPath();
            Node node = (Node) xPath.compile(v2.getString()).evaluate(document, XPathConstants.NODE);

            return new NodeValueString((String) node.toString());
        } catch (Exception e) {
            throw new ExprEvalException("FunctionBase: no evaluation", e);
        }
    }
}
