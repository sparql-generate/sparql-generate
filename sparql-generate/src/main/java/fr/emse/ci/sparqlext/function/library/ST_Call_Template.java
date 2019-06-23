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
import fr.emse.ci.sparqlext.generate.engine.BNodeMap;
import fr.emse.ci.sparqlext.generate.engine.PlanFactory;
import fr.emse.ci.sparqlext.generate.engine.RootPlan;
import fr.emse.ci.sparqlext.lang.ParserSPARQLExt;
import fr.emse.ci.sparqlext.query.SPARQLExtQuery;
import fr.emse.ci.sparqlext.stream.LookUpRequest;
import fr.emse.ci.sparqlext.stream.SPARQLExtStreamManager;
import fr.emse.ci.sparqlext.utils.ST;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.apache.commons.io.IOUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.sparql.ARQInternalErrorException;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.engine.binding.BindingMap;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.apache.jena.sparql.function.Function;
import org.apache.jena.sparql.function.FunctionEnv;
import org.apache.jena.sparql.util.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * https://ns.inria.fr/sparql-template/ sec. 4.3 A named template is called by
 * name with parameter values using the st:call-template function. When several
 * parameters occur, parameter passing is done by position (i.e. not by name).
 *
 * @author maxime.lefrancois
 */
public class ST_Call_Template implements Function {

    private static final Logger LOG = LoggerFactory.getLogger(ST_Call_Template.class);

    public static String URI = ST.callTemplate;

    @Override
    public final void build(String uri, ExprList args) {
        if (args.size() < 1) {
            throw new ExprEvalException("Expecting at least one argument");
        }
    }

    /**
     *
     * @param binding
     * @param args
     * @param uri
     * @param env
     * @return
     */
    @Override
    public NodeValue exec(
            final Binding binding,
            final ExprList args,
            final String uri,
            final FunctionEnv env) {
        if (args == null) {
            throw new ARQInternalErrorException("FunctionBase: Null args list");
        }
        if (args.size() < 1) {
            throw new ExprEvalException("Expecting at least one argument");
        }
        NodeValue queryNode = args.get(0).eval(binding, env);
        if (!(queryNode.isIRI() || queryNode.isLiteral() && SPARQLExt.MEDIA_TYPE_URI.equals(queryNode.getDatatypeURI()))) {
            throw new ExprEvalException("Name of sub query "
                    + "should be a URI or a literal with datatype " + SPARQLExt.MEDIA_TYPE_URI + ". Got: " + queryNode);
        }
        if (queryNode.isLiteral() && args.size() > 1) {
            throw new ExprEvalException("Expecting at most one argument when first argument is a literal.");
        }

        RootPlan plan;
        final Binding newBinding;
        if (queryNode.isIRI()) {
            String queryName = queryNode.asNode().getURI();
            plan = getPlanforName(queryName, env);
            newBinding = getNewBindingForName(binding, queryName, args, env);
        } else {
            String queryString = queryNode.asNode().getLiteralLexicalForm();
            plan = getPlanforString(queryString, env);
            newBinding = getNewBindingForString(binding, queryString, env);
        }
        final String output = exec(plan, newBinding, env);
        return new NodeValueString(output);
    }

    private RootPlan getPlanforName(
            String queryName,
            FunctionEnv env) {
        try {
            final Context context = env.getContext();
            final Map<String, RootPlan> loadedPlans = (Map<String, RootPlan>) context.get(SPARQLExt.LOADED_PLANS);
            final Map<String, SPARQLExtQuery> loadedQueries = (Map<String, SPARQLExtQuery>) context.get(SPARQLExt.LOADED_QUERIES);
            if (loadedPlans.containsKey(queryName)) {
                return loadedPlans.get(queryName);
            }
            final SPARQLExtStreamManager sm = (SPARQLExtStreamManager) context.get(SPARQLExt.STREAM_MANAGER);
            final LookUpRequest request = new LookUpRequest(queryName, SPARQLExt.MEDIA_TYPE);
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
            throw new ExprEvalException(message, ex);
        }
    }

    private RootPlan getPlanforString(
            String queryString,
            FunctionEnv env) {
        try {
            final Context context = env.getContext();
            final Map<String, RootPlan> loadedPlans = (Map<String, RootPlan>) context.get(SPARQLExt.LOADED_PLANS);
            final Map<String, SPARQLExtQuery> loadedQueries = (Map<String, SPARQLExtQuery>) context.get(SPARQLExt.LOADED_QUERIES);
            if (loadedPlans.containsKey(queryString)) {
                return loadedPlans.get(queryString);
            }
            final SPARQLExtQuery q = ParserSPARQLExt.parseSubQuery(queryString);
            loadedQueries.put(queryString, q);
            final RootPlan plan = PlanFactory.createPlanForSubQuery(q);
            loadedPlans.put(queryString, plan);
            return plan;
        } catch (NullPointerException | QueryParseException ex) {
            String message = "Error while loading the query file " + queryString;
            throw new ExprEvalException(message, ex);
        }
    }

    private Binding getNewBindingForName(
            final Binding binding,
            final String queryName,
            final ExprList args,
            final FunctionEnv env) {
        final Map<String, SPARQLExtQuery> loadedQueries = (Map<String, SPARQLExtQuery>) env.getContext().get(SPARQLExt.LOADED_QUERIES);
        final SPARQLExtQuery expandedQuery = loadedQueries.get(queryName);
        final List<Var> querySignature = expandedQuery.getSignature();
        final BindingMap newBinding = BindingFactory.create();
        final List<Expr> callParameters = args.getList().subList(1, args.size());
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
                final NodeValue node = callParameters.get(i).eval(binding, env);
                if (node.asNode().isConcrete()) {
                    newBinding.add(parameter, node.asNode());
                }
            }
        }
        return newBinding;
    }

    private Binding getNewBindingForString(
            final Binding binding,
            final String queryString,
            final FunctionEnv env) {
        final Map<String, SPARQLExtQuery> loadedQueries = (Map<String, SPARQLExtQuery>) env.getContext().get(SPARQLExt.LOADED_QUERIES);
        final SPARQLExtQuery expandedQuery = loadedQueries.get(queryString);
        if (!expandedQuery.hasSignature()) {
            return BindingFactory.binding(binding);
        }
        final BindingMap newBinding = BindingFactory.create();
        for (Var v : expandedQuery.getSignature()) {
            newBinding.add(v, binding.get(v));
        }
        return newBinding;
    }

    private String exec(
            final RootPlan plan,
            final Binding binding,
            final FunctionEnv env) {
        Dataset inputDataset = env.getContext().get(SPARQLExt.DATASET);
        List<Var> vars = new ArrayList<>();
        for (Iterator<Var> vs = binding.vars(); vs.hasNext();) {
            vars.add(vs.next());
        }
        final List<Binding> values = new ArrayList<>();
        values.add(binding);
        BNodeMap bNodeMap = new BNodeMap();
        Context newContext = SPARQLExt.createContext(env.getContext());
        StringBuilder sb = new StringBuilder();
        final CompletableFuture<Void> future = plan.exec(inputDataset, vars, values, bNodeMap, newContext, null, null, sb::append);
        try {
            future.get();
            return sb.toString();
        } catch (InterruptedException ex) {
            LOG.warn("Interrupted while executing the SPARQL-Template query");
            throw new ExprEvalException(ex);
        } catch (ExecutionException ex) {
            if (ex.getCause() instanceof ExprEvalException) {
                LOG.warn("ExprEvalException while executing the SPARQL-Template query", ex.getCause());
                throw (ExprEvalException) ex.getCause();
            }
            LOG.warn("Error while executing the SPARQL-Template query", ex);
            throw new ExprEvalException(ex);
        }

    }

}
