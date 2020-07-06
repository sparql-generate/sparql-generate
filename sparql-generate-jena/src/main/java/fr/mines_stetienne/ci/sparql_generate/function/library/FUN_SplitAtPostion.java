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
package fr.mines_stetienne.ci.sparql_generate.function.library;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.apache.jena.sparql.function.FunctionBase3;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Binding function
 * <a href="http://w3id.org/sparql-generate/fn/SplitAtPosition">fun:SplitAtPosition</a>
 * splits the input string around matches of the given regular expression, and
 * returns the ith element in the array.
 *
 * <ul>
 * <li>Param 1 is the input string;</li>
 * <li>Param 2 is a regular expression;</li>
 * <li>Param 3 is the number of substring to return;</li>
 * <li>Result is a xsd:string.</li>
 * </ul>
 *
 * @author Noorani Bakerally <noorani.bakerally at emse.fr>
 */
public class FUN_SplitAtPostion extends FunctionBase3 {
    //TODO write multiple unit tests for this class.

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(FUN_SplitAtPostion.class);

    /**
     * The SPARQL function URI.
     */
    public static final String URI = SPARQLExt.FUN + "SplitAtPosition";

    public NodeValue exec(NodeValue string, NodeValue regex, NodeValue position) {
        String[] splits = string.getString().split(regex.getString());
        int index = position.getInteger().intValue();
        if (index > splits.length) {
            throw new ExprEvalException("array index out of bounds: " + index + " , got " + splits.length);
        }
        return new NodeValueString(splits[index]);
    }
}
