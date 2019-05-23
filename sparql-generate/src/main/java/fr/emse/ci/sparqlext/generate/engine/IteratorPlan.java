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
package fr.emse.ci.sparqlext.generate.engine;

import fr.emse.ci.sparqlext.iterator.IteratorFunction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.ARQConstants;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.engine.binding.BindingMap;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionEnv;
import org.apache.jena.sparql.function.FunctionEnvBase;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sparql.util.NodeFactoryExtra;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executes the ITERATOR clause.
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class IteratorPlan implements BindingsClausePlan {

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(IteratorPlan.class);

    /**
     * The SPARQL-Generate iterator.
     */
    private final IteratorFunction iterator;

    /**
     * The list of expressions on which to evaluate the iterator.
     */
    private final ExprList exprList;

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
     * @param values the set of values.
     * @param listBindingFunction where new bindings are emited.
     * @param context the execution context.
     * @param executor the executor.
     * @return the future that will be completed when the iterator completes.
     */
    final public CompletableFuture<Void> exec(
            final List<Var> variables,
            final List<Binding> values,
            final Function<List<Binding>, CompletableFuture<Void>> listBindingFunction,
            final Context context,
            final Executor executor) {
        context.set(ARQConstants.sysCurrentTime, NodeFactoryExtra.nowAsDateTime());
        final FunctionEnv env = new FunctionEnvBase(context);
        final Batches batches = new Batches(values, listBindingFunction);
        try {
            final List<CompletableFuture<Void>> cfs = values.stream()
                    .map(binding -> exec(binding, env, batches)
                            .thenRunAsync(() -> batches.executionComplete(binding)))
                    .collect(Collectors.toList());
            return CompletableFuture.allOf(cfs.toArray(new CompletableFuture[cfs.size()]));
        } catch (Exception ex) {
            LOG.warn("Execution failed for " + toString(), ex);
            return CompletableFuture.completedFuture(null);
        }
    }

    private CompletableFuture<Void> exec(
            final Binding binding,
            final FunctionEnv env,
            final Batches batches) {
        LOG.debug("Start " + this);
        try {
            return iterator.exec(binding, exprList, env, (nodeValues) -> batches.add(binding, nodeValues));
        } catch (ExprEvalException ex) {
            LOG.debug("No evaluation for " + this + ", caused by " + ex.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }

    private class Batches {

        final Function<List<Binding>, CompletableFuture<Void>> listBindingFunction;
        final List<Binding> uncompleteExecutions = new ArrayList<>();
        final List<Batch> uncompleteBatches = new ArrayList<>();
        final Map<Binding, Batch> lastBatch = new HashMap<>();

        Batches(final List<Binding> binding,
                final Function<List<Binding>, CompletableFuture<Void>> listBindingFunction) {
            uncompleteExecutions.addAll(binding);
            this.listBindingFunction = listBindingFunction;
        }

        synchronized CompletableFuture<Void> add(
                final Binding binding,
                final List<List<NodeValue>> nodeValues) {
            final List<Binding> bindings = getListBinding(binding, nodeValues);
            final Batch batch = getNextBatch(lastBatch.get(binding));
            LOG.trace("Iterator adds " + nodeValues.size() + " to batch " + batch);
            lastBatch.put(binding, batch);
            if (batch.add(binding, bindings)) {
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
                final Batch last) {
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

        synchronized CompletableFuture<Void> executionComplete(
                final Binding binding) {
            LOG.trace("Iterator " + binding + " complete");
            uncompleteExecutions.remove(binding);
            lastBatch.remove(binding);
            List<CompletableFuture<Void>> cfs = new ArrayList<>();
            for (Batch batch : uncompleteBatches) {
                if (batch.stopWaiting(binding)) {
                    cfs.add(batchComplete(batch));
                }
            }
            return CompletableFuture.allOf(cfs.toArray(new CompletableFuture[cfs.size()]));
        }

        private CompletableFuture<Void> batchComplete(final Batch batch) {
            LOG.trace("Batch  " + batch + " complete with " + batch.bindings.size() + " bindings");
            uncompleteBatches.remove(batch);
            return listBindingFunction.apply(batch.bindings);
        }

    }

    private class Batch {

        final List<Binding> expectedExecutions = new ArrayList<>();
        final List<Binding> bindings = new ArrayList<>();

        Batch(final List<Binding> uncompleteExecutions) {
            expectedExecutions.addAll(expectedExecutions);
        }

        synchronized boolean add(
                final Binding execution,
                final List<Binding> bindings) {
            expectedExecutions.remove(execution);
            this.bindings.addAll(bindings);
            return expectedExecutions.isEmpty();
        }

        synchronized boolean stopWaiting(
                final Binding execution) {
            expectedExecutions.remove(execution);
            return expectedExecutions.isEmpty();
        }
    }

    @Override
    public String toString() {
        return "ITERATOR " + iterator.getClass().getSimpleName() + " " + exprList + " AS " + vars;
    }

}
