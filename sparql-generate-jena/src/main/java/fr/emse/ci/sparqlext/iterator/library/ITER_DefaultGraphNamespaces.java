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
package fr.emse.ci.sparqlext.iterator.library;

import fr.emse.ci.sparqlext.SPARQLExt;
import fr.emse.ci.sparqlext.iterator.IteratorFunctionBase;
import fr.emse.ci.sparqlext.utils.ContextUtils;
import java.util.ArrayList;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.jena.query.Dataset;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import org.apache.jena.sparql.expr.nodevalue.NodeValueString;

/**
 * Iterator function
 * <a href="http://w3id.org/sparql-generate/iter/defaultGraphNamespaces">iter:defaultGraphNamespaces</a>
 * iterates over the prefix declarations of the default graph on which the query
 * is executed.
 *
 * <ul>
 * <li>It takes no parameters</li>
 * </ul>
 *
 * The following variables may be bound:
 *
 * <ul>
 * <li>Output 1: (string) the prefix;</li>
 * <li>Output 2: (string) the namespace.</li>
 * </ul>
 *
 * @author Maxime Lefrançois
 */
public class ITER_DefaultGraphNamespaces extends IteratorFunctionBase {

    private static final Logger LOG = LoggerFactory.getLogger(ITER_DefaultGraphNamespaces.class);

    public static final String URI = SPARQLExt.ITER + "defaultGraphNamespaces";

    @Override
    public List<List<NodeValue>> exec(List<NodeValue> args) {
        if (!args.isEmpty()) {
            LOG.debug("Expecting zero arguments.");
            throw new ExprEvalException("Expecting zero arguments.");
        }
        
        Dataset dataset = ContextUtils.getDataset(getContext());
        
        List<List<NodeValue>> output = new ArrayList<>();
        for (Map.Entry<String, String> prefix : dataset.getDefaultModel().getNsPrefixMap().entrySet()) {
            List<NodeValue> ns = new ArrayList<>();
            ns.add(new NodeValueString(prefix.getKey()));
            ns.add(new NodeValueString(prefix.getValue()));
            output.add(ns);
        }
        return output;
    }

    @Override
    public void checkBuild(ExprList args) {
        Objects.nonNull(args);
    }

}
