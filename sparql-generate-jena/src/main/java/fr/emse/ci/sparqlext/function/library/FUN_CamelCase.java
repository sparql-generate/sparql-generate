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
package fr.emse.ci.sparqlext.function.library;

import fr.emse.ci.sparqlext.SPARQLExt;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.apache.jena.sparql.function.FunctionBase1;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Binding function
 * <a href="http://w3id.org/sparql-generate/fn/camelCase">fun:camelCase</a>
 * takes as input a String, and return a CamelCase version of that string.
 *
 *
 * <ul>
 * <li>Param 1 (input) a string literal</li>
 * </ul>
 *
 * @author Maxime Lefrançois
 */
public final class FUN_CamelCase extends FunctionBase1 {

    private static final Logger LOG = LoggerFactory.getLogger(FUN_CamelCase.class);

    public static final String URI = SPARQLExt.FUN + "camelCase";

    @Override
    public NodeValue exec(NodeValue node) {
        if (!node.isString()) {
            LOG.debug("The input should be a string. Got " + node);
            throw new ExprEvalException("The input should be a string. Got " + node);
        }
        String string = node.getString();
        StringBuilder converted = new StringBuilder();
        boolean convertNext = true;
        for (char ch : string.toCharArray()) {
            if (Character.isWhitespace(ch)) {
                convertNext = true;
            } else if (!Character.isLetterOrDigit(ch)) {
            } else if (convertNext) {
                converted.append(Character.toUpperCase(ch));
                convertNext = false;
            } else {
                converted.append(ch);
            }
        }
        return new NodeValueString(converted.toString());
    }
}
