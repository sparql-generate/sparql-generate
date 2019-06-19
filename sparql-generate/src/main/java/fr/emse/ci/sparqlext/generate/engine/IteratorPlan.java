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
package fr.emse.ci.sparqlext.generate.engine;

import fr.emse.ci.sparqlext.SPARQLExt;
import fr.emse.ci.sparqlext.iterator.IteratorFunction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.logging.Level;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.engine.binding.BindingMap;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.util.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author maxime.lefrancois
 */
public abstract class IteratorPlan implements BindingsClausePlan {

    private static final Logger LOG = LoggerFactory.getLogger(IteratorPlan.class);

    /**
     * The SPARQL-Generate iterator.
     */
    protected final IteratorFunction iterator;

    /**
     * The list of expressions on which to evaluate the iterator.
     */
    protected final ExprList exprList;

    /**
     * The variable that will be bound to each result of the iterator
     * evaluation.
     */
    private final List<Var> vars;

    /**
     * The constructor.
     *
     * @param s - The SPARQL-Generate iterator function.
     * @param e - The list of expressions on which to evaluate the iterator
     * function.
     * @param vars - The list of variables that will be bound to each result of
     * the iterator function evaluation.
     */
    public IteratorPlan(
            final IteratorFunction s,
            final ExprList e,
            final List<Var> vars) {
        this.iterator = s;
        this.exprList = e;
        this.vars = vars;
    }

    public List<Var> getVars() {
        return vars;
    }

    /**
     * Updates the values block.
     *
     * @param variables the current variables.
     * @param futureValues the future set of values.
     * @param listBindingFunction where new bindings are emited.
     * @param context the execution context.
     * @param executor the executor.
     * @return the future that will be completed when the iterator completes.
     */
    abstract public CompletableFuture<Void> exec(
            List<Var> variables,
            List<CompletableFuture<Binding>> futureValues,
            Function<List<Binding>, CompletableFuture<Void>> listBindingFunction,
            Context context,
            Executor executor);

    protected class Batches {

        final Function<List<Binding>, CompletableFuture<Void>> listBindingFunction;
        final List<CompletableFuture<Binding>> uncompleteExecutions = Collections.synchronizedList(new ArrayList<>());
        final List<Batch> uncompleteBatches = Collections.synchronizedList(new ArrayList<>());
        final Map<CompletableFuture<Binding>, Batch> lastBatch = Collections.synchronizedMap(new HashMap<>());

        Batches(final List<CompletableFuture<Binding>> executions,
                final Function<List<Binding>, CompletableFuture<Void>> listBindingFunction) {
            uncompleteExecutions.addAll(executions);
            this.listBindingFunction = listBindingFunction;
        }

        synchronized CompletableFuture<Void> add(
                final CompletableFuture<Binding> execution,
                final Binding binding,
                final List<List<NodeValue>> nodeValues) {
            final List<Binding> bindings = getListBinding(binding, nodeValues);
            final Batch batch = getNextBatch(execution);
            lastBatch.put(execution, batch);
            if (batch.addAndCheckIfEmpty(execution, bindings)) {
                return batchComplete(batch);
            } else {
                return CompletableFuture.completedFuture(null);
            }
        }

        List<Binding> getListBinding(
                final Binding binding,
                final List<List<NodeValue>> nodeValues) {
            final List<Binding> listBindings = new ArrayList<>();
            nodeValues.forEach((listNodeValues) -> {
                if (vars.size() > listNodeValues.size()) {
                    LOG.warn("Too many variables, some will not be bound: " + listNodeValues);
                    return;
                }
                final BindingMap b = BindingFactory.create(binding);
                for (int i = 0; i < vars.size(); i++) {
                    if (listNodeValues.get(i) != null) {
                        Node n = listNodeValues.get(i).asNode();
                        b.add(vars.get(i), n);
                    }
                }
                listBindings.add(b);
            });
            return listBindings;
        }

        synchronized Batch getNextBatch(
                final CompletableFuture<Binding> execution) {
            Batch last = lastBatch.get(execution);
            if (last == null) {
                if (uncompleteBatches.isEmpty()) {
                    final Batch batch = new Batch(uncompleteExecutions);
                    uncompleteBatches.add(batch);
                    return batch;
                } else {
                    return uncompleteBatches.get(0);
                }
            } else {
                int index = uncompleteBatches.indexOf(last);
                if (index == uncompleteBatches.size() - 1) {
                    final Batch batch = new Batch(uncompleteExecutions);
                    uncompleteBatches.add(batch);
                    return batch;
                } else {
                    return uncompleteBatches.get(index + 1);
                }
            }
        }

        CompletableFuture<Void> allExecutionComplete() {
            List<CompletableFuture<Void>> cfs = new ArrayList<>();
            if (!uncompleteBatches.isEmpty()) {
                LOG.info("Forcing completion of remaining batches");
            }
            for (Batch batch : uncompleteBatches) {
                batch.expectedExecutions.clear();
                LOG.trace("A batch is complete " + batch);
                cfs.add(listBindingFunction.apply(batch.bindings));
            }
            uncompleteExecutions.clear();
            uncompleteBatches.clear();
            lastBatch.clear();
            return CompletableFuture.allOf(cfs.toArray(new CompletableFuture[cfs.size()]));
        }

//        CompletableFuture<Void> executionComplete(
//                final CompletableFuture<Binding> execution) {
//            uncompleteExecutions.remove(execution);
//            lastBatch.remove(execution);
//            List<CompletableFuture<Void>> cfs = new ArrayList<>();
//            for (Batch batch : uncompleteBatches) {
//                if (batch.expectedExecutions.remove(execution) && batch.expectedExecutions.isEmpty()) {
//                    cfs.add(listBindingFunction.apply(batch.bindings));
//                    uncompleteBatches.remove(batch);
//                    LOG.info("Forcing completion of batch " + batch);
//                }
//            }
//            return CompletableFuture.allOf(cfs.toArray(new CompletableFuture[cfs.size()]));
//        }

        private CompletableFuture<Void> batchComplete(final Batch batch) {
            uncompleteBatches.remove(batch);
            if (LOG.isTraceEnabled()) {
                LOG.trace("A batch is complete " + batch);
            }
            return listBindingFunction.apply(batch.bindings);
        }

        @Override
        public String toString() {
            return "Batches " + System.identityHashCode(this);
        }

    }

    private class Batch {

        final List<CompletableFuture<Binding>> expectedExecutions = new ArrayList<>();
        final List<Binding> bindings = new ArrayList<>();

        Batch(final List<CompletableFuture<Binding>> uncompleteExecutions) {
            expectedExecutions.addAll(uncompleteExecutions);
        }

        synchronized boolean addAndCheckIfEmpty(
                final CompletableFuture<Binding> iterator,
                final List<Binding> bindings) {
            expectedExecutions.remove(iterator);
            this.bindings.addAll(bindings);
            return expectedExecutions.isEmpty();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("Batch ");
            sb.append(System.identityHashCode(this));
            if (expectedExecutions.isEmpty()) {
                sb.append(" complete with ");
            } else {
                sb.append(" still waiting for ");
                sb.append(expectedExecutions.size());
                sb.append(" and has ");
            }
            sb.append(SPARQLExt.log(bindings));
            return sb.toString();
        }

    }

    @Override
    public String toString() {
        return "ITERATOR " + iterator.getClass().getSimpleName() + " " + exprList + " AS " + vars;
    }
}
