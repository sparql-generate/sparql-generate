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
package fr.mines_stetienne.ci.sparql_generate.iterator.library;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import java.util.ArrayList;
import fr.mines_stetienne.ci.sparql_generate.iterator.IteratorFunctionBase2;

import java.util.Collections;
import java.util.List;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import java.util.Arrays;

import org.apache.jena.sparql.expr.ExprEvalException;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Iterator function
 * <a href="http://w3id.org/sparql-generate/iter/Split">iter:Split</a>
 * iterates over the array of strings resulting from spliting the input string
 * around matches of the given regular expression.
 *
 * <ul>
 * <li>Param 1: (input) is the input string;</li>
 * <li>Param 2: (regex) is a regular expression;</li>
 * </ul>
 *
 * @author Noorani Bakerally <noorani.bakerally at emse.fr>
 */
public class ITER_Split extends IteratorFunctionBase2 {

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ITER_Split.class);
    public static final String URI = SPARQLExt.ITER + "Split";

    /**
     *
     * @param stringValue a RDF Literal with datatype {@code xsd:string}
     * representing the source string
     * @param delimeterValue a RDF Literal with datatype {@code xsd:string}
     * representing the delimeter which can be regular expression
     * @return a list of RDF Literal with datatype {@code xsd:string}
     */
    @Override
    public List<List<NodeValue>> exec(NodeValue stringValue, NodeValue delimeterValue) {
        try {
            String string = stringValue.asNode().getLiteralLexicalForm();
            String delimeter = delimeterValue.asNode().getLiteralLexicalForm();
            List<String> splits = new ArrayList<>(Arrays.asList(string.split(delimeter)));

            List<List<NodeValue>> listNodeValues = new ArrayList<>(splits.size());

            for (String split : splits) {
                NodeValue nodeValue = new NodeValueString(split);
                listNodeValues.add(Collections.singletonList(nodeValue));
            }
            return listNodeValues;
        } catch (Exception ex) {
            LOG.debug("No evaluation for " + stringValue + ", " + delimeterValue, ex);
            throw new ExprEvalException("No evaluation for " + stringValue + ", " + delimeterValue, ex);
        }

    }
}
