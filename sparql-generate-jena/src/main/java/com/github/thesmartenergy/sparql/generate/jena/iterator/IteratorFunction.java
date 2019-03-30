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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionEnv;

/**
 * The interface of SPARQL-Generate iterator functions. SPARQL-Generate
 * iterators are similar to SPARQL functions, except they return a list of
 * RDF terms or SPARQL variables, instead of only one.
 */
public interface IteratorFunction {

    /**
     * Called during query plan construction immediately after the construction
     * of the extension instance. Can throw ExprBuildException if something is
     * wrong (like wrong number of arguments).
     *
     * @param args The parsed arguments
     */
    void build(ExprList args);

    /**
     * Test a list of values - argument will not be null but may have the wrong
     * number of arguments. FunctionBase provides a more convenient way to
     * implement a function. Can throw ExprEvalsException if something goes
     * wrong.
     *
     * @param binding The current solution
     * @param args A list of unevaluated expressions
     * @param env The execution context
     * @param nodeValuesStream where to emit new values
     * @return when the iterator finishes its job
     */
    CompletableFuture<Void> exec(Binding binding, ExprList args, FunctionEnv env, Function<Collection<List<NodeValue>>, CompletableFuture<Void>> nodeValuesStream);
}
