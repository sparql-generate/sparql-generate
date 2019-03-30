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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.vocabulary.RDF;

/**
 *
 * @author maxime.lefrancois
 */
public class SPARQLGenerateSubContext extends SPARQLGenerateContext {

    private final SPARQLGenerateContext decoratedContext;

    /**
     * used for the generation of lists
     */
    private final int size;

    private final Map<Node_List, Node[]> listNodes = new HashMap<>();

    /**
     *
     * @param decoratedContext
     * @param size the number of bindings for which this context will be used
     */
    public SPARQLGenerateSubContext(SPARQLGenerateContext decoratedContext, int size) {
        this.decoratedContext = decoratedContext;
        this.size = size;
    }

    /**
     * get the node at position i in the LIST( expr ). Or rdf:nil if the
     * position is equal to the number of bindings
     *
     * @param list
     * @param position
     * @return
     */
    @Override
    public Node getNode(Node_List list, int position) {
        return getInfo(list)[position];
    }

    private synchronized Node[] getInfo(Node_List list) {
        if (!listNodes.containsKey(list)) {
            Node[] nodes = new Node[size + 1];
            for (int i = 0; i < size; i++) {
                nodes[i] = NodeFactory.createBlankNode();
            }
            nodes[size] = RDF.nil.asNode();
            listNodes.put(list, nodes);
        }
        return listNodes.get(list);
    }

    @Override
    public SPARQLGenerateContext forkContext(int size) {
        return new SPARQLGenerateSubContext(decoratedContext, size);
    }

    @Override
    public Var allocVar(String label) {
        return decoratedContext.allocVar(label);
    }

    @Override
    public Map<String, RootPlan> getLoadedPlans() {
        return decoratedContext.getLoadedPlans();
    }

    @Override
    public Map<String, Set<BindingHashMapOverwrite>> getQueryExecutionCalls(Context context) {
        return decoratedContext.getQueryExecutionCalls(context);
    }

    @Override
    public Map<String, SPARQLGenerateQuery> getLoadedQueries() {
        return decoratedContext.getLoadedQueries();
    }

    @Override
    public SPARQLGenerateStreamManager getStreamManager() {
        return decoratedContext.getStreamManager();
    }

    @Override
    public Executor getExecutor() {
        return decoratedContext.getExecutor();
    }

    @Override
    public boolean alreadyExecuted(String queryName, BindingHashMapOverwrite currentCall) {
        return decoratedContext.alreadyExecuted(queryName, currentCall);
    }

    @Override
    public void registerExecution(String queryName, BindingHashMapOverwrite values) {
        decoratedContext.registerExecution(queryName, values);
    }

    @Override
    public void addTaskOnClose(Runnable task) {
        decoratedContext.addTaskOnClose(task);
    }

    @Override
    public void close() throws Exception {
        decoratedContext.close();
    }


}
