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
import fr.emse.ci.sparqlext.engine.BNodeMap;
import fr.emse.ci.sparqlext.engine.PlanFactory;
import fr.emse.ci.sparqlext.engine.RootPlan;
import fr.emse.ci.sparqlext.iterator.ExecutionControl;
import fr.emse.ci.sparqlext.iterator.IteratorStreamFunctionBase;
import fr.emse.ci.sparqlext.query.SPARQLExtQuery;
import fr.emse.ci.sparqlext.stream.LookUpRequest;
import fr.emse.ci.sparqlext.stream.SPARQLExtStreamManager;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.apache.commons.io.IOUtils;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.SysRIOT;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.engine.binding.BindingMap;
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
            final Consumer<List<List<NodeValue>>> collectionListNodeValue, 
            final ExecutionControl control) {
        if (args.isEmpty()) {
            LOG.debug("There should be at least one IRI parameter.");
            throw new ExprEvalException("There should be at least one IRI parameter.");
        }
        if (!args.get(0).isIRI()) {
            LOG.debug("The first parameter must be a IRI.");
            throw new ExprEvalException("The first parameter must be a IRI.");
        }
        String queryName = args.get(0).asNode().getURI();
        final List<NodeValue> parameters = args.subList(1, args.size());

        final Context context = getContext();
        RootPlan plan = getQuery(queryName, context);

        Binding newBinding = getNewBindingForName(queryName, parameters);
        CompletableFuture<Void> planExecution = exec(plan, newBinding, (result) -> {
            List<List<NodeValue>> list = getListNodeValues(result);
            collectionListNodeValue.accept(list);
        });
        control.registerFuture(planExecution);
        control.complete();
    }

    @Override
    public void checkBuild(ExprList args) {
    }

    private RootPlan getQuery(String queryName, Context context) {
        try {
            final Map<String, RootPlan> loadedPlans = (Map<String, RootPlan>) context.get(SPARQLExt.LOADED_PLANS);
            final Map<String, SPARQLExtQuery> loadedQueries = (Map<String, SPARQLExtQuery>) context.get(SPARQLExt.LOADED_QUERIES);
            if (loadedPlans.containsKey(queryName)) {
                return loadedPlans.get(queryName);
            }
            final SPARQLExtStreamManager sm = (SPARQLExtStreamManager) context.get(SysRIOT.sysStreamManager);
            final LookUpRequest request = new LookUpRequest(queryName, SPARQLExt.MEDIA_TYPE);
            final InputStream in = sm.open(request);
            String qString = IOUtils.toString(in, Charset.forName("UTF-8"));
            final SPARQLExtQuery q
                    = (SPARQLExtQuery) QueryFactory.create(qString,
                            SPARQLExt.SYNTAX);
            if (!q.isSelectType()) {
                throw new ExprEvalException("Expecting a SELECT query. Got " + q.getQueryType());
            }
            loadedQueries.put(queryName, q);
            final RootPlan plan = PlanFactory.createPlanForSubQuery(q);
            loadedPlans.put(queryName, plan);
            return plan;
        } catch (NullPointerException | IOException | QueryParseException ex) {
            String message = "Error while loading the query file " + queryName;
            throw new ExprEvalException(message, ex);
        }
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

    private Binding getNewBindingForName(
            final String queryName,
            final List<NodeValue> callParameters) {
        final Map<String, SPARQLExtQuery> loadedQueries = (Map<String, SPARQLExtQuery>) getContext().get(SPARQLExt.LOADED_QUERIES);
        final SPARQLExtQuery expandedQuery = loadedQueries.get(queryName);
        final List<Var> querySignature = expandedQuery.getSignature();
        final BindingMap newBinding = BindingFactory.create();
        if (querySignature != null) {
            int max = Math.min(callParameters.size(), querySignature.size());
            if (callParameters.size() != querySignature.size()) {
                throw new ExprEvalException("The number of "
                        + "parameters (" + callParameters.size() + ") is not equal to"
                        + " the size of the signature of query " + queryName
                        + "(" + querySignature.size() + ").");
            }
            for (int i = 0; i < max; i++) {
                final Var parameter = querySignature.get(i);
                final NodeValue node = callParameters.get(i);
                if (node.asNode().isConcrete()) {
                    newBinding.add(parameter, node.asNode());
                }
            }
        }
        return newBinding;
    }

    private CompletableFuture<Void> exec(
            final RootPlan plan,
            final Binding binding,
            final Consumer<ResultSet> resultStream) {
        Dataset inputDataset = getContext().get(SPARQLExt.DATASET);
        List<Var> vars = new ArrayList<>();
        for (Iterator<Var> vs = binding.vars(); vs.hasNext();) {
            vars.add(vs.next());
        }
        final List<Binding> values = new ArrayList<>();
        values.add(binding);
        BNodeMap bNodeMap = new BNodeMap();
        Context newContext = SPARQLExt.createContext(getContext());
        return plan.exec(inputDataset, vars, values, bNodeMap, newContext, null, resultStream, null);
    }
}
