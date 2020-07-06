/*
 * Copyright 2020 MINES Saint-Étienne
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
package fr.mines_stetienne.ci.sparql_generate.iterator;

import java.util.List;
import java.util.function.Consumer;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionEnv;

/**
 * The interface of SPARQL-Generate iterator functions. SPARQL-Generate
 * iterators are similar to SPARQL functions, except they return a list of list
 * of RDF terms or SPARQL variables.
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
     */
    void exec(Binding binding, ExprList args, FunctionEnv env, Consumer<List<List<NodeValue>>> nodeValuesStream);
}
