/*
 * Copyright 2016 The Apache Software Foundation.
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
package com.github.thesmartenergy.sparql.generate.jena.engine.impl;

import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerate;
import com.github.thesmartenergy.sparql.generate.jena.engine.RootPlan;
import com.github.thesmartenergy.sparql.generate.jena.query.SPARQLGenerateQuery;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingHashMap;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sparql.util.Symbol;

/**
 * One execution.
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
abstract class PlanBase {
    
    
        
    private final static Symbol QUERY_EXECUTION_CALLS = Symbol.create(SPARQLGenerate.NS + "symbol_query_exec_calls");
    
    private final static Symbol LOADED_QUERIES = Symbol.create(SPARQLGenerate.NS + "symbol_loaded_queries");
    

    /**
     * Utility function. ensures there is at least a row of null values in a
     * values block.
     *
     * @param <T> a sub class of Binding
     * @param variables the values variables
     * @param values the values bindings
     */
    protected final <T extends Binding>  void ensureNotEmpty(
            final List<Var> variables,
            final List<T> values) {
        Objects.requireNonNull(variables);
        Objects.requireNonNull(values);
        if (values.isEmpty()) {
            final BindingHashMap map = new BindingHashMap();
            values.add((T) new BindingHashMapOverwrite(map, null, null));
        }
    }
    
    private final static Symbol VAR = Symbol.create(SPARQLGenerate.NS + "symbol_var");
    
    protected Var allocVar(final Context context, final String label) {
        if(context.isUndef(VAR)) {
            context.set(VAR, new HashMap<String, Var>());
        }
        final Map<String,Var> vars = context.get(VAR);
        if(vars.containsKey(label)) {
            return vars.get(label);
        }
        final Var var = Var.alloc(label);
        vars.put(label, var);
        return var;
    }

    // stocker ce qui est associé à une execution de requête:
    // les variables ? BindingHashMapOverwrite:77 changer Var.alloc  par ExecutiunoContextImpl.alloc
    // les variables ? SelectPlanImpl :99 eviter Var.alloc par ExecutiunoContextImpl.alloc
    protected void initContext(Context context, Node queryName, SPARQLGenerateQuery query, RootPlanImpl plan, BindingHashMapOverwrite values) {
        if(queryName == null) {
            return;
        }
        if(!queryName.isURI()) {
            throw new UnsupportedOperationException("not implemented yet");
        }
        getLoadedQueries(context).put(queryName.getURI(), query);
        getLoadedPlans(context).put(queryName.getURI(), plan);
        registerExecution(context, queryName.getURI(), values);
    }

    private final static Symbol PLANS = Symbol.create(SPARQLGenerate.NS + "symbol_plans");
    
    protected Map<String, RootPlan> getLoadedPlans(final Context context) {
        if(context.isUndef(PLANS)) {
            context.set(PLANS, new HashMap<String, RootPlan>());
        }
        return (Map<String, RootPlan>) context.get(PLANS);
    }

    protected boolean alreadyExecuted(
            final Context context, 
            final String queryName, 
            final BindingHashMapOverwrite currentCall) {
        final Map<String, Set<BindingHashMapOverwrite>> queryExecutionCalls = getQueryExecutionCalls(context);

        if(!queryExecutionCalls.containsKey(queryName)) {
            return false;
        }
        for(BindingHashMapOverwrite previousCall : queryExecutionCalls.get(queryName) ) {
            boolean found = true;
            final Iterator<Var> variables = currentCall.vars();
            while(variables.hasNext()) {
                final Var var = variables.next();
                if(!currentCall.get(var).equals(previousCall.get(var))) {
                    found = false;
                }
            }
            if(found) {
                return true;
            }
        }
        return false;
    }

    protected final void registerExecution(Context context, String queryName, BindingHashMapOverwrite values) {
        final Map<String, Set<BindingHashMapOverwrite>> queryExecutionCalls = getQueryExecutionCalls(context);
        if(!queryExecutionCalls.containsKey(queryName)) {
            queryExecutionCalls.put(queryName, new HashSet<>());
        }
        queryExecutionCalls.get(queryName).add(values);
    }
    
    protected Map<String, Set<BindingHashMapOverwrite>> getQueryExecutionCalls(Context context) {
        if(context.isUndef(QUERY_EXECUTION_CALLS)) {
            context.set(QUERY_EXECUTION_CALLS, new HashMap<String, Set<BindingHashMapOverwrite>>());
        }
        return (Map<String, Set<BindingHashMapOverwrite>>) context.get(QUERY_EXECUTION_CALLS);
    }
    
    protected Map<String, SPARQLGenerateQuery> getLoadedQueries(Context context) {
        if(context.isUndef(LOADED_QUERIES)) {
            context.set(LOADED_QUERIES, new HashMap<String, SPARQLGenerateQuery>());
        }
        return (Map<String, SPARQLGenerateQuery>) context.get(LOADED_QUERIES);
    }
    
}
