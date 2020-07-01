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

import fr.emse.ci.sparqlext.utils.LogUtils;
import fr.emse.ci.sparqlext.SPARQLExtException;
import fr.emse.ci.sparqlext.iterator.IteratorFunction;
import fr.emse.ci.sparqlext.iterator.IteratorFunctionFactory;
import fr.emse.ci.sparqlext.iterator.IteratorFunctionRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
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
 *
 * @author maxime.lefrancois
 */
public class IteratorPlan implements BindingsClausePlan {

    private static final Logger LOG = LoggerFactory.getLogger(IteratorPlan.class);

    /**
     * The SPARQL-Generate iterator IRI.
     */
    private final String iri;

    /**
     * The SPARQL-Generate iterator.
     */
    private IteratorFunction iterator;

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
     * @param iri - The SPARQL-Generate iterator iri.
     * @param e - The list of expressions on which to evaluate the iterator
     * function.
     * @param vars - The list of variables that will be bound to each result of
     * the iterator function evaluation.
     */
    public IteratorPlan(
            final String iri,
            final ExprList e,
            final List<Var> vars) {
        this.iri = iri;
        this.exprList = e;
        this.vars = vars;
    }

    public IteratorFunction getIterator(Context context) {
        if (iterator != null) {
            return iterator;
        }
        final IteratorFunctionRegistry sr = IteratorFunctionRegistry.get(context);
        IteratorFunctionFactory factory = sr.get(iri);
        if (factory == null) {
            throw new SPARQLExtException("Unknown Iterator Function: " + iri);
        }
        iterator = factory.create(iri);
        iterator.build(exprList);
        return iterator;
    }

    public List<Var> getVars() {
        return vars;
    }

    /**
     * Updates the values block. Method is blocking
     *
     * @param variables the current variables.
     * @param values the existing bindings.
     * @param listBindingStream where new bindings are emited.
     * @param context the execution context.
     */
    public void exec(
            List<Var> variables,
            List<Binding> values,
            Context context,
            Consumer<List<Binding>> listBindingStream) {
        context.set(ARQConstants.sysCurrentTime, NodeFactoryExtra.nowAsDateTime());
        final IteratorFunction iterator = getIterator(context);
        final FunctionEnv env = new FunctionEnvBase(context);
        final IteratorPlan.Batches batches = new IteratorPlan.Batches(values, listBindingStream);
        for (Binding binding : values) {
            try {
            	iterator.exec(binding, exprList, env, (nodeValues) -> batches.add(binding, nodeValues));
            } catch (ExprEvalException ex) {
                LOG.debug("No evaluation for " + this + ", caused by " + ex.getMessage());
            } catch (Exception ex) {
                LOG.warn("Unanticipated exception for " + toString(), ex);
            }
        }
    }
    protected class Batches {

        final Consumer<List<Binding>> listBindingStream;
        final List<Binding> uncompleteExecutions = Collections.synchronizedList(new ArrayList<>());
        final List<Batch> uncompleteBatches = Collections.synchronizedList(new ArrayList<>());
        final Map<Binding, Batch> lastBatch = Collections.synchronizedMap(new HashMap<>());

        Batches(final List<Binding> executions,
                final Consumer<List<Binding>> listBindingStream) {
            uncompleteExecutions.addAll(executions);
            this.listBindingStream = listBindingStream;
        }

        void add(
                final Binding binding,
                final List<List<NodeValue>> nodeValues) {
            final List<Binding> bindings = getListBinding(binding, nodeValues);
            final Batch batch = getNextBatch(binding);
            lastBatch.put(binding, batch);
            if (batch.addAndCheckIfEmpty(binding, bindings)) {
                batchComplete(batch);
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

        synchronized Batch getNextBatch(final Binding execution) {
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

        void allExecutionComplete() {
            if (!uncompleteBatches.isEmpty()) {
                LOG.info("Forcing completion of remaining batches");
            }
            for (Batch batch : uncompleteBatches) {
                batch.expectedExecutions.clear();
                LOG.trace("A batch is complete " + batch);
                listBindingStream.accept(batch.bindings);
            }
            uncompleteExecutions.clear();
            uncompleteBatches.clear();
            lastBatch.clear();
        }

        private void batchComplete(final Batch batch) {
            uncompleteBatches.remove(batch);
            if (LOG.isTraceEnabled()) {
                LOG.trace("A batch is complete " + batch);
            }
            listBindingStream.accept(batch.bindings);
        }

        @Override
        public String toString() {
            return "Batches " + System.identityHashCode(this);
        }

    }

    private class Batch {

        final List<Binding> expectedExecutions = new ArrayList<>();
        final List<Binding> bindings = new ArrayList<>();

        Batch(final List<Binding> uncompleteExecutions) {
            expectedExecutions.addAll(uncompleteExecutions);
        }

        synchronized boolean addAndCheckIfEmpty(
                final Binding iterator,
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
            sb.append(LogUtils.log(bindings));
            return sb.toString();
        }

    }

    @Override
    public String toString() {
        return "ITERATOR " + iri + " " + exprList + " AS " + vars;
    }
}
