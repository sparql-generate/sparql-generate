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
package com.github.thesmartenergy.sparql.generate.jena.function.library;

import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerate;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.apache.jena.sparql.function.FunctionBase3;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * A SPARQL Function that extracts part of a string from another string, based
 * on a regular expression and position. The Function URI is
 * {@code <http://w3id.org/sparql-generate/fn/SplitAtPosition>}.
 *
 * @author Noorani Bakerally <noorani.bakerally at emse.fr>
 */
public class FN_SplitAtPostion extends FunctionBase3 {
    //TODO write multiple unit tests for this class.

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(FN_SplitAtPostion.class);

    /**
     * The SPARQL function URI.
     */
    public static final String URI = SPARQLGenerate.FN + "SplitAtPosition";

    /**
     *
     * @param string a RDF Literal with datatype {@code xsd:string} for the
     * source string
     * @param regex a RDF Literal with datatype {@code xsd:string} for the
     * regular expression
     * @param position a RDF Literal with datatype {@code xsd:int} for index of
     * the array of string obtained after splitting
     * @return 
     */
    public NodeValue exec(NodeValue string, NodeValue regex, NodeValue position) {
        String[] splits = string.getString().split(regex.getString());
        int index = position.getInteger().intValue();
        if(index > splits.length) {
            throw new ExprEvalException("array index out of bounds: " + index + " , got " + splits.length);
        }
        return  new NodeValueString(splits[index]);
    }
}
