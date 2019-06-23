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
package fr.emse.ci.sparqlext.engine;

import fr.emse.ci.sparqlext.SPARQLExt;
import fr.emse.ci.sparqlext.SPARQLExtException;
import fr.emse.ci.sparqlext.query.SPARQLExtQuery;
import fr.emse.ci.sparqlext.stream.LookUpRequest;
import fr.emse.ci.sparqlext.stream.SPARQLExtStreamManager;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.query.Dataset;
import org.apache.jena.sparql.core.Substitute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.ResultSet;
import org.apache.jena.riot.SysRIOT;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.engine.binding.BindingMap;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionEnv;
import org.apache.jena.sparql.function.FunctionEnvBase;
import org.apache.jena.sparql.util.Context;

/**
 * Executes a named sub-query in the GENERATE clause.
 * 
 * @author maxime.lefrancois
 */
public class NamedSubQueryPlan implements ExecutionPlan {

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(NamedSubQueryPlan.class);

    private final Node name;

    private final ExprList callParameters;

    public NamedSubQueryPlan(Node name, ExprList callParameters) {
        Objects.requireNonNull(name, "name must not be null");
        this.name = name;
        this.callParameters = callParameters;
    }

    @Override
    public CompletableFuture<Void> exec(
            final Dataset inputDataset,
            final List<Var> variables,
            final List<Binding> values,
            final BNodeMap bNodeMap,
            final Context context,
            final StreamRDF outputGenerate,
            final Consumer<ResultSet> outputSelect,
            final Consumer<String> outputTemplate) {
        final Map<String, List<Binding>> queriesValues = getQueryNames(values);
        final Set<CompletableFuture<Void>> cfs = new HashSet<>();
        for (String queryName : queriesValues.keySet()) {
            final List<Binding> queryValues = queriesValues.get(queryName);
            final RootPlan plan = getPlan(queryName, context);
            final List<Var> newVariables = getNewVariables(queryName, variables, context);

            final List<Binding> newValues
                    = getQueryValues(newVariables, queryValues, context);

            if (SPARQLExt.alreadyExecuted(context, queryName, newValues)) {
                LOG.debug("Already executed " + queryName + " with same values.");
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Already executed " + queryName + " with values " + SPARQLExt.log(newVariables, newValues));
                }
                return null;
            }
            SPARQLExt.registerExecution(context, queryName, newValues);
            if (LOG.isTraceEnabled()) {
                LOG.trace("Executing " + queryName + ": " + SPARQLExt.log(newVariables, newValues));
            }

            cfs.add(plan.exec(inputDataset, new ArrayList<>(newVariables), newValues, new BNodeMap(), context, outputGenerate, outputSelect, outputTemplate));
        }
        return CompletableFuture.allOf(cfs.toArray(new CompletableFuture[cfs.size()]));
    }

    private Map<String, List<Binding>> getQueryNames(List<Binding> values) {
        final Map<String, List<Binding>> queryValues = new HashMap<>();
        for (Binding binding : values) {
            final Node n = Substitute.substitute(name, binding);
            if (!n.isURI()) {
                LOG.warn("Name of sub query resolved to something else than a"
                        + " URI: " + n);
                continue;
            }
            String queryName = n.getURI();
            List<Binding> newValues = queryValues.get(queryName);
            if (newValues == null) {
                newValues = new ArrayList<>();
                queryValues.put(queryName, newValues);
            }
            newValues.add(binding);
        }
        return queryValues;
    }

    private RootPlan getPlan(
            String queryName,
            final Context context) {
        try {
            final Map<String, RootPlan> loadedPlans = (Map<String, RootPlan>) context.get(SPARQLExt.LOADED_PLANS);
            final Map<String, SPARQLExtQuery> loadedQueries = (Map<String, SPARQLExtQuery>) context.get(SPARQLExt.LOADED_QUERIES);
            if (loadedPlans.containsKey(queryName)) {
                return loadedPlans.get(queryName);
            }
            final LookUpRequest request = new LookUpRequest(queryName, SPARQLExt.MEDIA_TYPE);
            final SPARQLExtStreamManager sm = (SPARQLExtStreamManager) context.get(SysRIOT.sysStreamManager);
            final InputStream in = sm.open(request);
            String qString = IOUtils.toString(in, Charset.forName("UTF-8"));
            final SPARQLExtQuery q
                    = (SPARQLExtQuery) QueryFactory.create(qString,
                            SPARQLExt.SYNTAX);
            loadedQueries.put(queryName, q);
            final RootPlan plan = PlanFactory.createPlanForSubQuery(q);
            loadedPlans.put(queryName, plan);
            return plan;
        } catch (NullPointerException | IOException | QueryParseException ex) {
            String message = "Error while loading the query file " + queryName;
            throw new SPARQLExtException(message, ex);
        }
    }

    private List<Var> getNewVariables(
            final String queryName,
            final List<Var> variables,
            final Context context) {
        final Map<String, SPARQLExtQuery> loadedQueries = (Map<String, SPARQLExtQuery>) context.get(SPARQLExt.LOADED_QUERIES);
        final SPARQLExtQuery expandedQuery = loadedQueries.get(queryName);
        final List<Var> querySignature = expandedQuery.getSignature();
        if (callParameters != null && querySignature == null) {
            throw new SPARQLExtException("Query " + queryName + " has no "
                    + "signature and cannot accept parameters. Got: "
                    + callParameters);
        }
        if (callParameters == null && querySignature != null) {
            throw new SPARQLExtException("Query " + queryName + " has a "
                    + "signature " + querySignature + "so parameters should be "
                    + "provided");
        }
        if (callParameters == null && querySignature == null) {
            return new ArrayList<>(variables);
        }
        if (callParameters.size() != querySignature.size()) {
            throw new SPARQLExtException("The number of "
                    + "parameters is not equal to the size of the"
                    + " signature of query " + queryName + ". call"
                    + " parameters: " + callParameters + ". and"
                    + " query signature" + querySignature);
        }
        return querySignature;
    }

    private List<Binding> getQueryValues(
            final List<Var> variables,
            final List<Binding> values,
            final Context context) {
        final FunctionEnv env = new FunctionEnvBase(context);
        final List<Binding> newValues = new ArrayList<>();
        for (Binding binding : values) {
            BindingMap newBinding = BindingFactory.create();
            if (callParameters == null) {
                for (int i = 0; i < variables.size(); i++) {
                    Var v = variables.get(i);
                    Node node = binding.get(v);
                    if (node.isConcrete()) {
                        newBinding.add(v, node);
                    }
                }
            } else {
                for (int i = 0; i < variables.size(); i++) {
                    Expr expr = callParameters.get(i);
                    expr = Substitute.substitute(expr, binding);
                    NodeValue node = expr.eval(binding, env);
                    if (node.asNode().isConcrete()) {
                        Var v = variables.get(i);
                        newBinding.add(v, node.asNode());
                    }
                }
            }
            newValues.add(newBinding);
        }
        return newValues;
    }

}
