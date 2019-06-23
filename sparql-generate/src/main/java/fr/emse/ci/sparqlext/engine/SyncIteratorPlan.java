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
package fr.emse.ci.sparqlext.engine;

import fr.emse.ci.sparqlext.iterator.IteratorFunction;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import org.apache.jena.sparql.ARQConstants;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;
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
public class SyncIteratorPlan extends IteratorPlan {

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(SyncIteratorPlan.class);

    /**
     * The constructor.
     *
     * @param s - The SPARQL-Generate iterator function.
     * @param e - The list of expressions on which to evaluate the iterator
     * function.
     * @param vars - The list of variables that will be bound to each result of
     * the iterator function evaluation.
     */
    public SyncIteratorPlan(
            final IteratorFunction s,
            final ExprList e,
            final List<Var> vars) {
        super(s, e, vars);
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
    @Override
    final public CompletableFuture<Void> exec(
            final List<Var> variables,
            final List<CompletableFuture<Binding>> futureValues,
            final Function<List<Binding>, CompletableFuture<Void>> listBindingFunction,
            final Context context,
            final Executor executor) {
        context.set(ARQConstants.sysCurrentTime, NodeFactoryExtra.nowAsDateTime());
        final FunctionEnv env = new FunctionEnvBase(context);
        final IteratorPlan.Batches batches = new IteratorPlan.Batches(futureValues, listBindingFunction);
        try {
            CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
            for (CompletableFuture<Binding> futureBinding : futureValues) {
                future = future.thenComposeAsync((n) -> futureBinding.thenComposeAsync((binding) -> {
                    try {
                        return iterator.exec(binding, exprList, env, (nodeValues) -> batches.add(futureBinding, binding, nodeValues));
                                //.thenRunAsync(()->batches.executionComplete(futureBinding),executor);
                    } catch (ExprEvalException ex) {
                        LOG.debug("No evaluation for " + this + ", caused by " + ex.getMessage());
                        return CompletableFuture.completedFuture(null);
                    }
                }, executor),
                        executor);
            }
            return future.thenCompose((n) -> {
                return batches.allExecutionComplete();
            });
        } catch (Exception ex) {
            LOG.warn("Execution failed for " + toString(), ex);
            return CompletableFuture.completedFuture(null);
        }
    }

}
