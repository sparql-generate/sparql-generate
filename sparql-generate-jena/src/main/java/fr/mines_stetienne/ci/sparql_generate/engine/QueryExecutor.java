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
package fr.mines_stetienne.ci.sparql_generate.engine;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.graph.Node;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingHashMap;
import org.apache.jena.sparql.util.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheStats;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.SPARQLExtException;
import fr.mines_stetienne.ci.sparql_generate.lang.ParserSPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.query.SPARQLExtQuery;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;
import fr.mines_stetienne.ci.sparql_generate.utils.LogUtils;

/**
 *
 * @author Maxime Lefrançois
 */
public class QueryExecutor {

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(QueryExecutor.class);

    private final Cache<String, SPARQLExtQuery> loadedQueries = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.MINUTES).maximumSize(200_000).build();
    private final Cache<SPARQLExtQuery, RootPlan> loadedPlans = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.MINUTES).maximumSize(200_000).build();
    private final Cache<ExecutionKey, String> templateExecutions = CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.SECONDS).maximumSize(200_000).recordStats().build();
    private final Cache<ExecutionKey, ResultSetRewindable> selectExecutions = CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.SECONDS).maximumSize(200_000).recordStats().build();
    static int nbselect = 0, nbtemplate = 0, nbgenerate = 0;

    /**
     *
     * @param queryName
     * @param context
     * @return the query, or null.
     */
    private SPARQLExtQuery getQueryFromName(
            final String queryName,
            final Context context) {
        try {
            return loadedQueries.get(queryName, () -> {
                LOG.debug("Loading " + queryName);
                try (TypedInputStream tin = ContextUtils.openStream(context, queryName, SPARQLExt.MEDIA_TYPE)) {
                    if (tin == null) {
                        throw new SPARQLExtException("No query found at " + queryName);
                    }
                    String qString;
                    try {
                        qString = IOUtils.toString(tin.getInputStream(), Charset.forName("UTF-8"));
                    } catch (IOException ex) {
                        throw new SPARQLExtException("Error while loading the query file " + queryName, ex);
                    }
                    final SPARQLExtQuery q;
                    try {
                    	String base = ContextUtils.getBase(context);
                        q = (SPARQLExtQuery) QueryFactory.create(qString, 
                        		base,
                                SPARQLExt.SYNTAX);
                        if(!q.explicitlySetBaseURI()) {
                        	q.setBaseURI(base);
                        }
                    } catch (QueryParseException ex) {
                        throw new SPARQLExtException("Error while parsing the query file " + queryName, ex);
                    }
                    return q;
                }
            });
        } catch (ExecutionException ex) {
            throw (SPARQLExtException) ex.getCause();
        }
    }

    /**
     *
     * @param queryString
     * @param base
     * @return
     */
    private SPARQLExtQuery getQueryFromString(
            final String queryString,
            final String base) {
        try {
            return loadedQueries.get(queryString, () -> {
                try {
                	SPARQLExtQuery query = new SPARQLExtQuery();
                    return (SPARQLExtQuery) ParserSPARQLExt.parseSubQuery(query, queryString);
                } catch (QueryParseException ex) {
                    throw new SPARQLExtException("Error while parsing the query " + LogUtils.compress(queryString), ex);
                }
            });
        } catch (ExecutionException ex) {
            throw (SPARQLExtException) ex.getCause();
        }
    }

    /**
     *
     * @param queryName
     * @param base
     * @param context
     * @return the plan, or null.
     */
    public RootPlan getPlanFromName(
            final String queryName,
            final Context context) {
        return getPlan(getQueryFromName(queryName, context));
    }

    /**
     *
     * @param queryString
     * @param base
     * @return the plan, or null.
     */
    public RootPlan getPlanFromString(
            final String queryString,
            final String base) {
        return getPlan(getQueryFromString(queryString, base));
    }

    /**
     *
     * @param query
     * @param context
     * @return the plan, or null.
     */
    public RootPlan getPlan(
            final SPARQLExtQuery query) {
        try {
            return loadedPlans.get(query, () -> {
                return PlanFactory.create(query);
            });
        } catch (ExecutionException ex) {
            throw (SPARQLExtException) ex.getCause();
        }
    }

    /**
     *
     * @param queryName
     * @param callParameters never null
     * @param context
     */
    public void execGenerateFromQuery(
            final SPARQLExtQuery query,
            final Binding binding,
            final Context context) {
    	Objects.nonNull(query);
        Objects.nonNull(binding);
        Objects.nonNull(context);
        final RootPlan plan = getPlan(query);
        final List<Binding> newValues = new ArrayList<>();
        newValues.add(binding);
        execGeneratePlan(plan, newValues, context);
    }

    /**
     *
     * @param queryName
     * @param callParameters never null
     * @param context
     */
    public void execGenerateFromName(
            final String queryName,
            final List<List<Node>> callParameters,
            final Context context) {
        Objects.nonNull(queryName);
        Objects.nonNull(callParameters);
        Objects.nonNull(context);
        final RootPlan plan = getPlanFromName(queryName, context);
        final SPARQLExtQuery query = plan.getQuery();
        final List<Var> signature = getSignature(query);
        final List<Binding> newValues = getNewValues(queryName, query, signature, callParameters);
        execGeneratePlan(plan, newValues, context);
    }
    
    public void execGeneratePlan(
            final RootPlan plan,
            final List<Binding> values,
            final Context context) {
        Objects.nonNull(ContextUtils.getGenerateOutput(context));
        if (++nbgenerate % 2000 == 00) {
            LOG.info(String.format("Called generates %s times.", nbgenerate));
        }
        plan.execGenerateStream(values, context);
    }
    /**
     *
     * @param queryName the name of the query (needs to be fetched)
     * @param callParameters
     * @param context
     */
    public void execSelectFromName(
            final String queryName,
            final List<List<Node>> callParameters,
            final Context context) {
        Objects.nonNull(queryName);
        Objects.nonNull(callParameters);
        Objects.nonNull(context);
        final RootPlan plan = getPlanFromName(queryName, context);
        final SPARQLExtQuery query = plan.getQuery();
        final List<Var> signature = getSignature(query);
        final List<Binding> newValues = getNewValues(queryName, query, signature, callParameters);
        execSelectPlan(plan, newValues, context);
    }

    /**
     *
     * @param queryString the query as a string
     * @param callParameters
     * @param context
     * @param outputSelect
     */
    public void execSelectFromString(
            final String queryString,
            final List<List<Node>> callParameters,
            final Context context) {
        Objects.nonNull(queryString);
        final SPARQLExtQuery query = getQueryFromString(queryString, null);
        execSelectFromQuery(query, callParameters, context);
    }

    public void execSelectFromQuery(
            final SPARQLExtQuery query,
            final List<List<Node>> callParameters,
            final Context context) {
        Objects.nonNull(query);
        Objects.nonNull(callParameters);
        Objects.nonNull(context);
        final String queryName = LogUtils.compress(query.toString());
        final RootPlan plan = getPlan(query);
        final List<Var> signature = getSignature(query);
        final List<Binding> newValues = getNewValues(queryName, query, signature, callParameters);
        execSelectPlan(plan, newValues, context);
    }

    public void execSelectPlan(
            final RootPlan plan,
            final List<Binding> newValues,
            final Context context) {
        Objects.nonNull(ContextUtils.getSelectOutput(context));
        final ExecutionKey key = new ExecutionKey(plan, newValues);
        if (++nbselect % 2000 == 00) {
            CacheStats stats = selectExecutions.stats();

            LOG.info("call select " + nbselect + " count " + stats.loadCount() + " - hit count " + stats.hitCount() + " - rate " + stats.hitRate());
        }
        ResultSetRewindable resultSet = selectExecutions.getIfPresent(key);
        if (resultSet != null) {
            resultSet.reset();
        } else {
            ResultSet memResultSet = plan.execSelect(newValues, context);
            resultSet = ResultSetFactory.copyResults(memResultSet);
            selectExecutions.put(key, resultSet);
        }
        ContextUtils.getSelectOutput(context).accept(resultSet);
    }

    /**
     *
     * @param queryName the query uri
     * @param callParameters
     * @param context
     * @param outputTemplate
     */
    public void execTemplateFromName(
            final String queryName,
            final List<List<Node>> callParameters,
            final Context context) {
        Objects.nonNull(queryName);
        Objects.nonNull(callParameters);
        Objects.nonNull(context);
        final RootPlan plan = getPlanFromName(queryName, context);
        final SPARQLExtQuery query = plan.getQuery();
        final List<Var> signature = getSignature(query);
        final List<Binding> newValues = getNewValues(queryName, query, signature, callParameters);
        execTemplatePlan(plan, newValues, context);
    }

    /**
     *
     * @param queryString the query as a string
     * @param binding
     * @param context
     * @param outputTemplate
     */
    public void execTemplateFromString(
            final String queryString,
            final Binding binding,
            final Context context) {
        Objects.nonNull(queryString);
        Objects.nonNull(binding);
        Objects.nonNull(context);
        final SPARQLExtQuery query = getQueryFromString(queryString, null);
        final RootPlan plan = getPlan(query);
        final List<Binding> newValues = new ArrayList<>();
        newValues.add(binding);
        execTemplatePlan(plan, newValues, context);
    }

    public void execTemplateFromQuery(
            final SPARQLExtQuery query,
            final List<List<Node>> callParameters,
            final Context context) {
        Objects.nonNull(query);
        Objects.nonNull(callParameters);
        Objects.nonNull(context);
        final String queryName = LogUtils.compress(query.toString());
        final RootPlan plan = getPlan(query);
        final List<Var> signature = getSignature(query);
        final List<Binding> newValues = getNewValues(queryName, query, signature, callParameters);
        execTemplatePlan(plan, newValues, context);
    }


    
    public void execTemplatePlan(
            final RootPlan plan,
            final List<Binding> newValues,
            final Context context) {
        Objects.nonNull(ContextUtils.getTemplateOutput(context));
        if (++nbtemplate % 2000 == 00) {
            LOG.info(String.format("Called templates %s times.", nbtemplate));
        }
        plan.execTemplateStream(newValues, context);
    }

    private List<Var> getSignature(SPARQLExtQuery query) {
        if (query.hasSignature()) {
            return query.getSignature();
        } else {
            return Collections.emptyList();
        }
    }

    private List<Binding> getNewValues(String queryName, SPARQLExtQuery query, List<Var> signature, List<List<Node>> callParameters) {
        final int size = signature.size();
        final List<Binding> bindings = new ArrayList<>();
        for (List<Node> callParams : callParameters) {
            if (callParams.size() != size) {
                throw new SPARQLExtException("Query " + queryName + " called with " + callParams.size() + " parameters but accepts only " + size);
            }
            final BindingHashMap b = new BindingHashMap();
            for (int i = 0; i < size; i++) {
            	if(callParams.get(i) != null) {
            		b.add(signature.get(i), callParams.get(i));
            	}
            }
            bindings.add(b);
        }
        return bindings;
    }

    private class ExecutionKey {

        RootPlan plan;
        List<Binding> binding;

        public ExecutionKey(RootPlan plan, List<Binding> binding) {
            this.plan = plan;
            this.binding = binding;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof ExecutionKey)) {
                return false;
            }
            ExecutionKey other = (ExecutionKey) obj;
            if (plan != other.plan) {
                return false;
            }
            boolean eq = binding.equals(other.binding);
            return eq;
        }

        @Override
        public int hashCode() {
            return 3 * Objects.hashCode(this.plan) + 17 * Objects.hashCode(binding);
        }

    }

}
