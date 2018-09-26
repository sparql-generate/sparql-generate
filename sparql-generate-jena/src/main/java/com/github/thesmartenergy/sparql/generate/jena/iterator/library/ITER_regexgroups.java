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
package com.github.thesmartenergy.sparql.generate.jena.iterator.library;

import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerate;
import com.github.thesmartenergy.sparql.generate.jena.iterator.IteratorFunctionBase;
import org.apache.jena.query.QueryBuildException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Iterator function
 * <a href="http://w3id.org/sparql-generate/iter/regex">iter:regexgroups</a>
 * iterates over each captured ith group matched by the regex.
 *
 * <ul>
 * <li>Param 1 is the input string;</li>
 * <li>Param 2 is a regular expression;</li>
 * <li>The remaining parameters correspond to the captured group numbers;</li>
 * </ul>
 *
 * @author El-Mehdi Khalfi <el-mehdi.khalfi at emse.fr>
 * @since 2018-09-26
 */
public class ITER_regexgroups extends IteratorFunctionBase {

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ITER_regexgroups.class);

    /**
     * The SPARQL function URI.
     */
    public static final String URI = SPARQLGenerate.ITER + "regexgroups";


    @Override
    public List<List<NodeValue>> exec(List<NodeValue> args) {
        String string = args.get(0).asString();
        String regexString = args.get(1).asString();
        List<NodeValue> groups = args.subList(2, args.size());

        List<List<NodeValue>> nodeValues = new ArrayList<>();

        Pattern pattern = Pattern.compile(regexString);
        Matcher matcher = pattern.matcher(string);

        List<Integer> grps = groups.stream().map(g -> g.getInteger().intValue()).collect(Collectors.toList());

        for (int i = 0; i < groups.size(); i++) {
            nodeValues.add(new ArrayList<>());
        }

        while (matcher.find()) {
            int i = 0;
            for (int grp : grps) {
                NodeValue n = new NodeValueString(matcher.group(grp));
                nodeValues.get(i++).add(n);
            }
        }

        return nodeValues;
    }

    @Override
    public void checkBuild(ExprList args) {
        if (args.size() < 3 ) {
            throw new QueryBuildException("Function '"
                    + this.getClass().getName() + "' takes at least a tree arguments: an input string, a regular expression, and at least one group number.");
        }
    }
}
