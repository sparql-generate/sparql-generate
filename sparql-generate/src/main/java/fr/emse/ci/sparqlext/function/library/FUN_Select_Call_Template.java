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
import fr.emse.ci.sparqlext.query.SPARQLExtQuery;
import fr.emse.ci.sparqlext.stream.LookUpRequest;
import fr.emse.ci.sparqlext.stream.SPARQLExtStreamManager;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import org.apache.commons.io.IOUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.ResultSet;
import org.apache.jena.riot.SysRIOT;
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
 * Extension of st:call-template where the first parameter is a select * query
 * (a string) the second parameter is the name of a template, the rest are the
 * call parameters
 *
 * @author maxime.lefrancois
 */
public class FUN_Select_Call_Template implements Function {

    private static final Logger LOG = LoggerFactory.getLogger(FUN_Select_Call_Template.class);

    public static String URI = SPARQLExt.FUN + "select-call-template";

    @Override
    public final void build(String uri, ExprList args) {
        if (args.size() < 2) {
            throw new ExprEvalException("Expecting at least two arguments");
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
        if (args.size() < 2) {
            throw new ExprEvalException("Expecting at least two arguments");
        }
        final QueryAndPlan select = getSelect(binding, args, env);
        final QueryAndPlan template = getTemplate(binding, args, env);

        final List<Expr> callParameters = args.getList().subList(2, args.size());
        if (callParameters.size() != template.query.getSignature().size()) {
            throw new ExprEvalException("The number of "
                    + "parameters (" + callParameters.size() + ") is not equal to"
                    + " the size of the signature of query " + template.name
                    + "(" + template.query.getSignature().size() + ").");
        }
        final StringBuilder output = new StringBuilder();
        CompletableFuture<Void> future = execSelect(select, binding, env, (result) -> {
            final String output2 = execTemplate(template, result, callParameters, env);
            output.append(output2);
        });
        try {
            future.get();
            return new NodeValueString(output.toString());
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

    private QueryAndPlan getTemplate(
            final Binding binding,
            final ExprList args,
            final FunctionEnv env) {
        final NodeValue templateNameNode = args.get(1).eval(binding, env);
        if (!templateNameNode.isIRI()) {
            throw new ExprEvalException("Second parameter should be a URI. "
                    + "Got: " + templateNameNode);
        }
        String templateName = templateNameNode.asNode().getURI();
        try {
            final Context context = env.getContext();
            final Map<String, RootPlan> loadedPlans = (Map<String, RootPlan>) context.get(SPARQLExt.LOADED_PLANS);
            final Map<String, SPARQLExtQuery> loadedQueries = (Map<String, SPARQLExtQuery>) context.get(SPARQLExt.LOADED_QUERIES);
            if (loadedQueries.containsKey(templateName)) {
                final SPARQLExtQuery q = loadedQueries.get(templateName);
                if (!q.isTemplateType() || !q.hasSignature()) {
                    String message = "Expecting TEMPLATE query with signature " + templateName;
                    throw new ExprEvalException(message);
                }
                final RootPlan plan = loadedPlans.get(templateName);
                return new QueryAndPlan(q, plan, templateName);
            }
            final SPARQLExtStreamManager sm = (SPARQLExtStreamManager) context.get(SysRIOT.sysStreamManager);
            final LookUpRequest request = new LookUpRequest(templateName, SPARQLExt.MEDIA_TYPE);
            final InputStream in = sm.open(request);
            String qString = IOUtils.toString(in, Charset.forName("UTF-8"));
            final SPARQLExtQuery q
                    = (SPARQLExtQuery) QueryFactory.create(qString,
                            SPARQLExt.SYNTAX);
            loadedQueries.put(templateName, q);
            if (!q.isTemplateType() || !q.hasSignature()) {
                String message = "Expecting TEMPLATE query with signature " + templateName;
                throw new ExprEvalException(message);
            }
            final RootPlan plan = PlanFactory.createPlanForSubQuery(q);
            loadedPlans.put(templateName, plan);
            return new QueryAndPlan(q, plan, templateName);
        } catch (NullPointerException | IOException | QueryParseException ex) {
            String message = "Error while loading the query file " + templateName;
            throw new ExprEvalException(message, ex);
        }
    }

    private QueryAndPlan getSelect(
            final Binding binding,
            final ExprList args,
            final FunctionEnv env) {
        final NodeValue selectQueryNode = args.get(0).eval(binding, env);
        if (!(selectQueryNode.isLiteral() && SPARQLExt.MEDIA_TYPE_URI.equals(selectQueryNode.getDatatypeURI()))) {
            throw new ExprEvalException("First parameter should be a literal "
                    + "with datatype " + SPARQLExt.MEDIA_TYPE_URI + ". Got: "
                    + selectQueryNode);
        }
        final String queryString = selectQueryNode.asNode().getLiteralLexicalForm();
        try {
            final Context context = env.getContext();
            final Map<String, RootPlan> loadedPlans = (Map<String, RootPlan>) context.get(SPARQLExt.LOADED_PLANS);
            final Map<String, SPARQLExtQuery> loadedQueries = (Map<String, SPARQLExtQuery>) context.get(SPARQLExt.LOADED_QUERIES);
            if (loadedPlans.containsKey(queryString)) {
                final SPARQLExtQuery q = loadedQueries.get(queryString);
                if (!q.isSelectType()) {
                    String message = "Expecting SELECT query" + queryString;
                    throw new ExprEvalException(message);
                }
                final RootPlan plan = loadedPlans.get(queryString);
                return new QueryAndPlan(q, plan, null);
            }
            final SPARQLExtQuery q = (SPARQLExtQuery) QueryFactory.create(queryString, SPARQLExt.SYNTAX);
            loadedQueries.put(queryString, q);
            final RootPlan plan = PlanFactory.createPlanForSubQuery(q);
            loadedPlans.put(queryString, plan);
            return new QueryAndPlan(q, plan, null);
        } catch (NullPointerException | QueryParseException ex) {
            String message = "Error while loading the query file " + queryString;
            throw new ExprEvalException(message, ex);
        }
    }

    private CompletableFuture<Void> execSelect(
            final QueryAndPlan select,
            final Binding binding,
            final FunctionEnv env,
            final Consumer<ResultSet> resultStream) {
        final Dataset inputDataset = env.getContext().get(SPARQLExt.DATASET);
        List<Var> vars = new ArrayList<>();
        for (Iterator<Var> vs = binding.vars(); vs.hasNext();) {
            vars.add(vs.next());
        }
        final List<Binding> values = new ArrayList<>();
        values.add(binding);
        BNodeMap bNodeMap = new BNodeMap();
        Context newContext = SPARQLExt.createContext(env.getContext());
        return select.plan.exec(inputDataset, vars, values, bNodeMap, newContext, null, resultStream, null);
    }

    private String execTemplate(
            final QueryAndPlan template,
            final ResultSet result,
            final List<Expr> callParameters,
            final FunctionEnv env) {
        final Dataset inputDataset = env.getContext().get(SPARQLExt.DATASET);
        final List<Var> querySignature = template.query.getSignature();
        final List<Binding> newValues = new ArrayList<>();
        while (result.hasNext()) {
            Binding binding = result.nextBinding();
            final BindingMap newBinding = BindingFactory.create();
            for (int i = 0; i < querySignature.size(); i++) {
                final Var var = querySignature.get(i);
                final Expr callParameter = callParameters.get(i);
                final NodeValue node = callParameter.eval(binding, env);
                if (node.asNode().isConcrete()) {
                    newBinding.add(var, node.asNode());
                }
            }
            newValues.add(newBinding);
        }
        BNodeMap bNodeMap = new BNodeMap();
        Context newContext = SPARQLExt.createContext(env.getContext());
        StringBuilder sb = new StringBuilder();
        final CompletableFuture<Void> future = template.plan.exec(inputDataset, querySignature, newValues, bNodeMap, newContext, null, null, sb::append);
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

    private class QueryAndPlan {

        private final SPARQLExtQuery query;
        private final RootPlan plan;
        private final String name;

        public QueryAndPlan(SPARQLExtQuery query, RootPlan plan, String name) {
            this.query = query;
            this.plan = plan;
            this.name = name;
        }

    }
}
