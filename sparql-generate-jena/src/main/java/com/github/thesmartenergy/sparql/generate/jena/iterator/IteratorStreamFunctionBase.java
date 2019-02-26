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

import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.apache.jena.query.QueryBuildException;
import org.apache.jena.sparql.ARQInternalErrorException;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionEnv;
import org.apache.jena.sparql.util.Context;

/**
 * The base implementation of the {@link IteratorFunction} interface.
 */
public abstract class IteratorStreamFunctionBase implements IteratorFunction {

    /** The list of argument expressions. */
    protected ExprList arguments = null;
    /** The function environment. */
    private FunctionEnv env;

    /**
     * Build a iterator function execution with the given arguments,
     * and operate a check of the build.
     * @param args -
     * @throws QueryBuildException if the iterator function cannot be
     * executed with the
     * given arguments.
     */
    @Override
    public final void build(ExprList args) {
        arguments = args;
        checkBuild(args);
    }

    /**
     * Partially checks if the iterator function can be executed with the given
     * arguments.
     * @param args -
     * @throws QueryBuildException if the iterator function cannot be executed with the
     * given arguments.
     */
    public abstract void checkBuild(ExprList args);

    @Override
    public void exec(
            Binding binding, ExprList args, FunctionEnv env, Consumer<List<List<NodeValue>>> nodeValuesStream) {
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

        exec(evalArgs, nodeValuesStream);
        arguments = null;
    }

    /**
     * Return the Context object for this execution.
     * @return -
     */
    public Context getContext() {
        return env.getContext();
    }

    /**
     * IteratorFunction call to a list of evaluated argument values.
     * @param args -
     * @param nodeValuesStream - where to emit new values
     */
    public abstract void exec(List<NodeValue> args, Consumer<List<List<NodeValue>>> nodeValuesStream);
   
   /**
    * Register new Thread in the context
    */
    public final void registerThread() {
        if(getContext().isUndef(SPARQLGenerate.THREAD)) {
            getContext().set(SPARQLGenerate.THREAD, new HashSet<Thread>());
        }
        ((Set<Thread>) getContext().get(SPARQLGenerate.THREAD)).add(Thread.currentThread());
    } 

   /**
    * Register new Thread in the context
    */
    public final void unregisterThread() {
        if(getContext().isUndef(SPARQLGenerate.THREAD)) {
            getContext().set(SPARQLGenerate.THREAD, new HashSet<Thread>());
        }
        ((Set<Thread>) getContext().get(SPARQLGenerate.THREAD)).remove(Thread.currentThread());
    } 

}
