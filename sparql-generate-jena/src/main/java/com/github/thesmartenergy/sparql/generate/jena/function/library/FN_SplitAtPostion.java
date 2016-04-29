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
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.apache.jena.graph.Node;
import java.math.BigDecimal;
import java.util.Map;
import static org.apache.jena.query.ResultSetFactory.result;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.apache.jena.sparql.function.FunctionBase2;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.apache.jena.sparql.expr.nodevalue.NodeValueBoolean;
import org.apache.jena.sparql.expr.nodevalue.NodeValueDecimal;
import org.apache.jena.sparql.expr.nodevalue.NodeValueDouble;
import org.apache.jena.sparql.expr.nodevalue.NodeValueFloat;
import org.apache.jena.sparql.expr.nodevalue.NodeValueInteger;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.apache.jena.sparql.function.FunctionBase3;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.CsvMapReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;
/**
 * A SPARQL Function that extracts part of a string from another string, based on a
 * regular expression and position. The Function URI is
 * {@code <http://w3id.org/sparql-generate/fn/SplitAtPosition>}.
 * It takes three parameters as input:
 * <ul>
 * <li>{@param string} a RDF Literal with datatype {@code xsd:string} for the source string </li>
 * <li>{@param regex} a RDF Literal with datatype {@code xsd:string} for the regular expression </li>
 * <li>{@param position} a RDF Literal with datatype {@code xsd:int} for index of the array of string obtained after splitting </li>
 * </ul>
 * @author Noorani Bakerally
 */
public class FN_SplitAtPostion extends FunctionBase3 {
    //TODO write multiple unit tests for this class.

    /**
     * The logger.
     */
    private static final Logger LOG = Logger.getLogger(FN_SplitAtPostion.class);
    
    
    /**
     * The SPARQL function URI.
     */
   public static final String URI = SPARQLGenerate.FN + "SplitAtPosition";

    /**
     * {@inheritDoc }
     */
    public NodeValue exec(NodeValue string, NodeValue regex, NodeValue position) {
        String [] splits = string.getString().split(regex.getString());
        NodeValue nodeValue = new NodeValueString(splits[position.getInteger().intValue()]);
        //NodeValue nodeValue = new NodeValueString(position.getString);
        return nodeValue;
    }
}
