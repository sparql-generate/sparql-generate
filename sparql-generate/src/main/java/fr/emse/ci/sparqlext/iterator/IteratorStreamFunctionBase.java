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
package fr.emse.ci.sparqlext.iterator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import org.apache.jena.query.QueryBuildException;
import org.apache.jena.sparql.ARQInternalErrorException;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionEnv;
import org.apache.jena.sparql.util.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The base implementation of the {@link IteratorFunction} interface.
 */
public abstract class IteratorStreamFunctionBase implements IteratorFunction {

    private static final Logger LOG = LoggerFactory.getLogger(IteratorStreamFunctionBase.class);

    /**
     * The list of argument expressions.
     */
    protected ExprList arguments = null;
    /**
     * The function environment.
     */
    private FunctionEnv env;

    private final CompletableFuture<Void> returnedFuture = new CompletableFuture<>();

    private final List<CompletableFuture<Void>> pendingFutures = Collections.synchronizedList(new ArrayList<>());

    private final CompletableFuture<Void> future = new CompletableFuture<>();

    private void waitFor(CompletableFuture<Void> future) {
        pendingFutures.add(future);
        LOG.info("Will wait for " + future + " now waiting for " + pendingFutures.size());
    }

    protected final void registerFuture(CompletableFuture<Void> newFuture) {
        waitFor(newFuture);
        newFuture.thenRun(() -> stopWaiting(newFuture));
    }

    private void stopWaiting(CompletableFuture<Void> completedFuture) {
        LOG.info("stop waiting " + completedFuture);
        if (completedFuture.isCancelled()) {
            returnedFuture.completeExceptionally(new InterruptedException());
        } else if (completedFuture.isCompletedExceptionally()) {
            completedFuture.exceptionally((t) -> {
                returnedFuture.completeExceptionally(t);
                return null;
            });
        } else if (completedFuture.isDone()) {
            pendingFutures.remove(completedFuture);
        }
        LOG.info("still waiting " + pendingFutures.size());
        if (pendingFutures.isEmpty()) {
            returnedFuture.complete(null);
        }
    }
    
    protected final void complete() {
        future.complete(null);
        stopWaiting(future);
    }

    /**
     * Build a iterator function execution with the given arguments, and operate
     * a check of the build.
     *
     * @param args -
     * @throws QueryBuildException if the iterator function cannot be executed
     * with the given arguments.
     */
    @Override
    public final void build(ExprList args) {
        arguments = args;
        checkBuild(args);
    }

    /**
     * Partially checks if the iterator function can be executed with the given
     * arguments.
     *
     * @param args -
     * @throws QueryBuildException if the iterator function cannot be executed
     * with the given arguments.
     */
    public abstract void checkBuild(ExprList args);

    @Override
    public final CompletableFuture<Void> exec(
            final Binding binding,
            final ExprList args,
            final FunctionEnv env,
            final Function<List<List<NodeValue>>, CompletableFuture<Void>> collectionListNodeValue) {
        registerFuture(future);
        this.env = env;
        if (args == null) {
            throw new ARQInternalErrorException("IteratorFunctionBase:"
                    + " Null args list");
        }

        List<NodeValue> evalArgs = new ArrayList<>();
        for (Expr e : args) {
            NodeValue x = e.eval(binding, env);
            evalArgs.add(x);
        }
        try {
            exec(evalArgs, (listNodeValues) -> {
                registerFuture(collectionListNodeValue.apply(listNodeValues));
            });
        } catch (ExprEvalException ex) {
            returnedFuture.completeExceptionally(ex);
        }
        return returnedFuture;
    }
    
    

    /**
     * Return the Context object for this execution.
     *
     * @return -
     */
    public Context getContext() {
        return (Context) env.getContext();
    }

    /**
     * IteratorFunction call to a list of evaluated argument values.
     *
     * @param args -
     * @param collectionListNodeValue - where to emit new future collections of
     * lists of values
     */
    public abstract void exec(List<NodeValue> args, Consumer<List<List<NodeValue>>> collectionListNodeValue);

}
