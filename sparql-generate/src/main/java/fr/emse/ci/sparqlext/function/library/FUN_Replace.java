/*
 * Copyright 2019 École des Mines de Saint-Étienne.
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
import org.apache.jena.sparql.function.FunctionBase3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Binding function
 * <a href="http://w3id.org/sparql-generate/fn/replace">fun:replace</a>
 * searches for pattern of a regular expression in a string and replace every 
 * pattern with a replacement pattern. The java String::replace method is used.
 * 
 * <ul>
 * <li>Param 1: (string): the input</li>
 * <li>Param 2: (pattern) the regular expression pattern;</li>
 * <li>Param 3: (replacement) the replacement string;</li>
 * </ul>
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class FUN_Replace extends FunctionBase3 {

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(FUN_Replace.class);

    /**
     * The SPARQL function URI.
     */
    public static final String URI = SPARQLExt.FUN + "replace";

    @Override
    public NodeValue exec(NodeValue nodeString, NodeValue nodePattern, NodeValue nodeReplacement) {
        String string = getString(nodeString);
        String pattern = getString(nodePattern);
        String replacement = getString(nodeReplacement);
        try {
            String result = string.replaceAll(pattern, replacement);
            return new NodeValueString(result);
        } catch (Exception ex) {
            String message = "Eception while replacing";
            LOG.debug(message, ex);
            throw new ExprEvalException(message, ex);
        }
    }

    private String getString(NodeValue nodeString) {
        if (!nodeString.isLiteral()) {
            String message = "The first argument should be a string. Got " + nodeString;
            LOG.debug(message);
            throw new ExprEvalException(message);
        }
        return nodeString.asNode().getLiteralLexicalForm();
    }

}
