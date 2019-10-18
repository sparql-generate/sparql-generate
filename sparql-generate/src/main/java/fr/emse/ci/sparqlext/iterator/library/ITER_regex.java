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
package fr.emse.ci.sparqlext.iterator.library;

import fr.emse.ci.sparqlext.SPARQLExt;
import fr.emse.ci.sparqlext.iterator.IteratorFunctionBase;
import java.util.ArrayList;
import java.math.BigInteger;

import java.util.List;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Iterator function
 * <a href="http://w3id.org/sparql-generate/iter/regex">iter:regex</a>
 * iterates over the input subsequences captured by the ith groups of every
 * regex matches.
 *
 * <p>
 * See
 * <a href="https://w3id.org/sparql-generate/playground.html#ex=example/generate/Example-Regex">Live
 * example</a></p>
 *
 *
 * <ul>
 * <li>Param 1 (string) is the input string;</li>
 * <li>Param 2 (string) is a regular expression;</li>
 * <li>Param 3..N (integers) are the numbers of the groups to capture.</li>
 * </ul>
 *
 * @author Noorani Bakerally &lt;noorani.bakerally at emse.fr>
 * @author Maxime Lefrançois &lt;maxime.lefrancois at emse.fr>
 */
public class ITER_regex extends IteratorFunctionBase {

    private static final Logger LOG = LoggerFactory.getLogger(ITER_regex.class);
    public static final String URI = SPARQLExt.ITER + "regex";

    public ITER_regex() {
    }

    @Override
    public List<List<NodeValue>> exec(List<NodeValue> args) {
        if (args.size() < 2) {
            LOG.debug("Requires at least two arguments that are strings. Got: " + args.size());
            throw new ExprEvalException("Requires at least two arguments that are strings. Got: " + args.size());
        }

        if (args.get(0) == null || !args.get(0).isLiteral()) {
            LOG.debug("First argument must be a literal, got: " + args.get(0));
            throw new ExprEvalException("First argument must be a literal, got: " + args.get(0));
        }
        String string = args.get(0).asNode().getLiteralLexicalForm();

        if (args.get(1) == null || !args.get(1).isString()) {
            LOG.debug("Second argument must be a string, got: " + args.get(1));
            throw new ExprEvalException("Second argument must be a string, got: " + args.get(1));
        }
        String regexString = args.get(1).asString();

        Pattern pattern;
        try {
            pattern = Pattern.compile(regexString, Pattern.MULTILINE);
        } catch (Exception ex) {
            LOG.debug("Exception while compiling regex string " + regexString, ex);
            throw new ExprEvalException("Exception while compiling regex string " + regexString, ex);
        }

        Integer[] gSelection = null;

        if (args.size()
                > 2) {
            final List<NodeValue> gNumbers = args.subList(2, args.size());
            if (gNumbers.stream().anyMatch(g -> g == null || !g.isInteger())) {
                LOG.debug("Group numbers must strings, got: " + gNumbers);
                throw new ExprEvalException("Columns names must be strings, got: " + gNumbers);
            }
            gSelection = gNumbers.stream().map(NodeValue::getInteger).map(BigInteger::intValue).toArray(Integer[]::new);
        }

        Matcher matcher = pattern.matcher(string);

        List<List<NodeValue>> listListNodeValues = new ArrayList<>();

        List<NodeValue> nodeValues;
        int size = gSelection != null ? gSelection.length : matcher.groupCount() + 1;

        while (matcher.find()) {
            nodeValues = new ArrayList<>(size);
            if (gSelection == null) {
                for (int i = 0; i < matcher.groupCount() + 1; i++) {
                    if(matcher.group(i)!=null) {
                        nodeValues.add(new NodeValueString(matcher.group(i)));
                    } else {
                        nodeValues.add(null);
                    }
                }
            } else {
                for (Integer g : gSelection) {
                    if(matcher.group(g)!=null) {
                        nodeValues.add(new NodeValueString(matcher.group(g)));
                    } else {
                        nodeValues.add(null);
                    }
                }
            }
            listListNodeValues.add(nodeValues);
        }
        return listListNodeValues;
    }

    @Override
    public void checkBuild(ExprList args) {
    }

}
