/*
 * Copyright 2016 ITEA 12004 SEAS Project.
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

package com.github.thesmartenergy.sparql.generate.jena.selector;


import java.util.List;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionEnv;

/** Interface to value-testing extensions to the expression evaluator. */

public interface Selector
{
    /** Called during query plan construction immediately after the
     * construction of the extension instance.
     * Can throw ExprBuildException if something is wrong (like wrong number of arguments). 
     * @param args The parsed arguments
     */ 
    public void build(ExprList args) ;

    /** Test a list of values - argument will not be null but
     *  may have the wrong number of arguments.
     *  FunctionBase provides a more convenient way to implement a function. 
     *  Can throw ExprEvalsException if something goes wrong.
     *   
     * @param binding   The current solution
     * @param args      A list of unevaluated expressions
     * @param env   The execution context
     * @return List<NodeValue> - a list of values
     */ 
    
    public List<NodeValue> exec(Binding binding, ExprList args, FunctionEnv env) ;
}
