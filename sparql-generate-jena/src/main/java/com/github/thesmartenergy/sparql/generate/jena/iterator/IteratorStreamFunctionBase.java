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
package com.github.thesmartenergy.sparql.generate.jena.iterator;

import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerateContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import org.apache.jena.query.QueryBuildException;
import org.apache.jena.sparql.ARQInternalErrorException;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionEnv;
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
    public CompletableFuture<Void> exec(
            Binding binding, ExprList args, FunctionEnv env, Function<Collection<List<NodeValue>>, CompletableFuture<Void>> collectionListNodeValue) {
        if (!(env.getContext() instanceof SPARQLGenerateContext)) {
            throw new ARQInternalErrorException("Context should be of type"
                    + " SPARQLGenerateContext");
        }
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

        return exec(evalArgs, collectionListNodeValue);
    }

    /**
     * Return the Context object for this execution.
     *
     * @return -
     */
    public SPARQLGenerateContext getContext() {
        return (SPARQLGenerateContext) env.getContext();
    }

    /**
     * IteratorFunction call to a list of evaluated argument values.
     *
     * @param args -
     * @param collectionListNodeValue - where to emit new future collections of lists of values
     */
    public abstract CompletableFuture<Void> exec(List<NodeValue> args, Function<Collection<List<NodeValue>>, CompletableFuture<Void>> collectionListNodeValue);

}
