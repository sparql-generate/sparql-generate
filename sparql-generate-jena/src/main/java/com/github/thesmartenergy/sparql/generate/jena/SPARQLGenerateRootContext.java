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
package com.github.thesmartenergy.sparql.generate.jena;

import com.github.thesmartenergy.sparql.generate.jena.engine.RootPlan;
import com.github.thesmartenergy.sparql.generate.jena.engine.impl.BindingHashMapOverwrite;
import com.github.thesmartenergy.sparql.generate.jena.graph.Node_List;
import com.github.thesmartenergy.sparql.generate.jena.query.SPARQLGenerateQuery;
import com.github.thesmartenergy.sparql.generate.jena.stream.SPARQLGenerateStreamManager;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.util.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author maxime.lefrancois
 */
public class SPARQLGenerateRootContext extends SPARQLGenerateContext {

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(SPARQLGenerateContext.class);

    private final SPARQLGenerateStreamManager streamManager;
    private final Map<String, Var> vars = new HashMap<>();
    private final Map<String, SPARQLGenerateQuery> loadedQueries = new HashMap<>();
    private final Map<String, RootPlan> loadedPlans = new HashMap<>();
    private final Map<String, Set<BindingHashMapOverwrite>> queryExecutionCalls = new HashMap<>();
    private final Executor executor;
    private final Set<Runnable> closeTasks = new HashSet<>();

    public SPARQLGenerateRootContext() {
        this(SPARQLGenerateStreamManager.makeStreamManager(), Executors.newWorkStealingPool());
    }

    public SPARQLGenerateRootContext(SPARQLGenerateStreamManager sm) {
        this(sm, Executors.newWorkStealingPool());
    }

    public SPARQLGenerateRootContext(SPARQLGenerateStreamManager sm, Executor executor) {
        super();
        this.streamManager = sm;
        this.executor = executor;
    }

    @Override
    public final Var allocVar(final String label) {
        if (vars.containsKey(label)) {
            return vars.get(label);
        }
        final Var var = Var.alloc(label);
        vars.put(label, var);
        return var;
    }

    // stocker ce qui est associé à une execution de requête:
    // les variables ? BindingHashMapOverwrite:77 changer Var.alloc  par ExecutiunoContextImpl.alloc
    // les variables ? SelectPlanImpl :99 eviter Var.alloc par ExecutiunoContextImpl.alloc
    @Override
    public final Map<String, RootPlan> getLoadedPlans() {
        return loadedPlans;
    }

    @Override
    public final Map<String, Set<BindingHashMapOverwrite>> getQueryExecutionCalls(Context context) {
        return queryExecutionCalls;
    }

    @Override
    public final Map<String, SPARQLGenerateQuery> getLoadedQueries() {
        return loadedQueries;
    }

    @Override
    public final SPARQLGenerateStreamManager getStreamManager() {
        return streamManager;
    }

    @Override
    public final Executor getExecutor() {
        return executor;
    }

    @Override
    public final synchronized boolean alreadyExecuted(
            final String queryName,
            final BindingHashMapOverwrite currentCall) {
        if (!queryExecutionCalls.containsKey(queryName)) {
            return false;
        }
        for (BindingHashMapOverwrite previousCall : queryExecutionCalls.get(queryName)) {
            boolean found = true;
            final Iterator<Var> variables = currentCall.vars();
            while (variables.hasNext()) {
                final Var var = variables.next();
                if (!currentCall.get(var).equals(previousCall.get(var))) {
                    found = false;
                }
            }
            if (found) {
                return true;
            }
        }
        return false;
    }

    @Override
    public final synchronized void registerExecution(String queryName, BindingHashMapOverwrite values) {
        if (!queryExecutionCalls.containsKey(queryName)) {
            queryExecutionCalls.put(queryName, new HashSet<>());
        }
        queryExecutionCalls.get(queryName).add(values);
    }

    @Override
    public final void addTaskOnClose(Runnable task) {
        this.closeTasks.add(task);
    }

    @Override
    public final void close() {
        LOG.trace("Closing context");
        closeTasks.forEach(Runnable::run);
    }

    @Override
    public SPARQLGenerateContext forkContext(int size) {
        return new SPARQLGenerateSubContext(this, size);
    }

    @Override
    public Node getNode(Node_List list, int position) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
