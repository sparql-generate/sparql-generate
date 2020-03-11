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
import fr.emse.ci.sparqlext.engine.QueryExecutor;
import fr.emse.ci.sparqlext.iterator.IteratorStreamFunctionBase;
import fr.emse.ci.sparqlext.utils.ContextUtils;
import fr.emse.ci.sparqlext.utils.EvalUtils;

import java.util.*;
import java.util.function.Consumer;
import org.apache.jena.graph.Node;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.apache.jena.sparql.util.Context;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Iterator function
 * <a href="http://w3id.org/sparql-generate/iter/call-select">iter:call-select</a>
 * takes as input a IRI to a SPARQL-Select document, runs it on the current
 * Dataset, and binds the given variables to the output of the select query, in
 * order.
 *
 * <ul>
 * <li>Param 1: (select) is the IRI to a SPARQL-Select document</li>
 * <li>Param 2 to n: are the call parameters corresponding to the SPARQL-Select
 * query signature</li>
 * </ul>
 *
 * Multiple variables may be bound: variable <em>n</em> is bound to the value of
 * the
 * <em>n</em><sup>th</sup> project variable of the select query.
 *
 * @author Maxime Lefrançois <maxime.lefrançois@emse.fr>
 */
public class ITER_Call_Select extends IteratorStreamFunctionBase {

    private static final Logger LOG = LoggerFactory.getLogger(ITER_Call_Select.class);

    public static final String URI = SPARQLExt.ITER + "call-select";

    @Override
    public void exec(
            final List<NodeValue> args,
            final Consumer<List<List<NodeValue>>> collectionListNodeValue) {
        if (args.isEmpty()) {
            LOG.debug("There should be at least one IRI parameter.");
            throw new ExprEvalException("There should be at least one IRI parameter.");
        }
        if (!args.get(0).isIRI()) {
            LOG.debug("The first parameter must be a IRI.");
            throw new ExprEvalException("The first parameter must be a IRI.");
        }
        String queryName = args.get(0).asNode().getURI();

        final List<List<Node>> callParameters = new ArrayList<>();
        callParameters.add(EvalUtils.eval(args.subList(1, args.size())));

        final Context context = ContextUtils.fork(getContext()).setSelectOutput((result) -> {
            List<List<NodeValue>> list = getListNodeValues(result);
            collectionListNodeValue.accept(list);
        }).fork();
        final QueryExecutor queryExecutor = ContextUtils.getQueryExecutor(context);
        queryExecutor.execSelectFromName(queryName, callParameters, context);
    }

    @Override
    public void checkBuild(ExprList args) {
    }

    private List<List<NodeValue>> getListNodeValues(ResultSet result) {
        List<String> resultVars = result.getResultVars();
        List<List<NodeValue>> listNodeValues = new ArrayList<>();
        while (result.hasNext()) {
            List<NodeValue> nodeValues = new ArrayList<>();
            QuerySolution sol = result.next();
            for (String var : resultVars) {
                RDFNode rdfNode = sol.get(var);
                if (rdfNode != null) {
                    NodeValue n = new NodeValueNode(rdfNode.asNode());
                    nodeValues.add(n);
                } else {
                    nodeValues.add(null);
                }
            }
            listNodeValues.add(nodeValues);
        }
        return listNodeValues;
    }

}
