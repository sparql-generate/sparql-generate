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
import java.util.ArrayList;
import com.github.thesmartenergy.sparql.generate.jena.iterator.IteratorFunctionBase3;

import java.util.Collections;
import java.util.List;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Iterator function
 * <a href="http://w3id.org/sparql-generate/iter/regex">iter:regex</a>
 * iterates over the input subsequences captured by the ith groups of every
 * regex matches.
 *
 * <ul>
 * <li>Param 1 is the input string;</li>
 * <li>Param 2 is a regular expression;</li>
 * <li>Param 3 is the number of the group to capture;</li>
 * </ul>
 *
 * @author Noorani Bakerally <noorani.bakerally at emse.fr>
 */
public class ITER_regex extends IteratorFunctionBase3 {

    private static final Logger LOG = LoggerFactory.getLogger(ITER_regex.class);
    public static final String URI = SPARQLGenerate.ITER + "regex";

    public ITER_regex() {
    }

    @Override
    public List<List<NodeValue>> exec(NodeValue stringValue, NodeValue regex, NodeValue locationV) {

        String string = stringValue.getString();
        String regexString = regex.getString();

        int location = locationV.getInteger().intValue();
        Pattern pattern = Pattern.compile(regexString, Pattern.MULTILINE);

        Matcher matcher = pattern.matcher(string);

        List<NodeValue> nodeValues = new ArrayList<>();
        while (matcher.find()) {
            NodeValue nodeValue = new NodeValueString(matcher.group(location));
            nodeValues.add(nodeValue);
        }
        return new ArrayList<>(Collections.singletonList(nodeValues));
    }

}
