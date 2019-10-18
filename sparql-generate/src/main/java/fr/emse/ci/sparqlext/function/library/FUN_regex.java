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
package fr.emse.ci.sparqlext.function.library;

import fr.emse.ci.sparqlext.SPARQLExt;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.apache.jena.sparql.function.FunctionBase3;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.jena.sparql.expr.ExprEvalException;

/**
 * Binding function
 * <a href="http://w3id.org/sparql-generate/fn/regex">fun:regex</a>
 * returns the input subsequence captured by the ith group during a regex match
 * operation.
 *
 * <ul>
 * <li>Param 1 is the input string;</li>
 * <li>Param 2 is a regular expression;</li>
 * <li>Param 3 is the number of the group to capture;</li>
 * <li>Result is a xsd:string.</li>
 * </ul>
 *
 * @author Noorani Bakerally <noorani.bakerally at emse.fr>
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public final class FUN_regex extends FunctionBase3 {
    //TODO write multiple unit tests for this class.

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(FUN_regex.class);

    /**
     * The SPARQL function URI.
     */
    public static final String URI = SPARQLExt.FUN + "regex";

    @Override
    public NodeValue exec(NodeValue stringValue, NodeValue regex, NodeValue locationV) {
        if (!stringValue.isLiteral()) {
            LOG.debug("First argument must be a literal, got: " + stringValue);
            throw new ExprEvalException("First argument must be a literal, got: " + stringValue);
        }
        String string = stringValue.asNode().getLiteralLexicalForm();

        if (!regex.isString()) {
            LOG.debug("Second argument must be a string, got: " + regex);
            throw new ExprEvalException("Second argument must be a string, got: " + regex);
        }
        String regexString = regex.asString();
        Pattern pattern;
        try {
            pattern = Pattern.compile(regexString, Pattern.MULTILINE);
        } catch(Exception ex) {
            LOG.debug("Exception while compiling regex string " + regexString, ex);
            throw new ExprEvalException("Exception while compiling regex string " + regexString, ex);
        }

        if (!locationV.isInteger()) {
            LOG.debug("Third argument must be an integer, got: " + locationV);
            throw new ExprEvalException("Third argument must be an integer, got: " + locationV);
        }

        int location = locationV.getInteger().intValue();

        Matcher matcher = pattern.matcher(string);

        if (matcher.find()) {
            return new NodeValueString(matcher.group(location));
        } else {
            throw new ExprEvalException();
        }
    }
}
