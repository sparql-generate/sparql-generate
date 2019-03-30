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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import org.apache.jena.graph.Node;
import org.apache.jena.query.ARQ;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.util.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author maxime.lefrancois
 */
public abstract class SPARQLGenerateContext extends Context implements AutoCloseable {

    /**
     * The logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(SPARQLGenerateContext.class);

    public SPARQLGenerateContext() {
        super(ARQ.getContext());
    }

    public abstract Var allocVar(final String label);

    public abstract Map<String, RootPlan> getLoadedPlans() ;

    public abstract Map<String, Set<BindingHashMapOverwrite>> getQueryExecutionCalls(Context context);

    public abstract Map<String, SPARQLGenerateQuery> getLoadedQueries();

    public abstract SPARQLGenerateStreamManager getStreamManager();

    public abstract  Executor getExecutor();

    public abstract boolean alreadyExecuted(
            final String queryName,
            final BindingHashMapOverwrite currentCall);
    

    public abstract void registerExecution(String queryName, BindingHashMapOverwrite values);

    public abstract void addTaskOnClose(Runnable task);
    
    public abstract SPARQLGenerateContext forkContext(int size);
    
    public abstract Node getNode(Node_List list, int position);

}
