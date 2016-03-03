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

import java.util.ArrayList ;
import java.util.List ;
import org.apache.jena.query.QueryBuildException;
import org.apache.jena.sparql.ARQInternalErrorException;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionEnv;
import org.apache.jena.sparql.util.Context;


/** Interface to value-testing extensions to the expression evaluator. */

public abstract class SelectorBase implements Selector
{
    String iri;
    
    protected SelectorBase(String iri) {
        this.iri = iri;
    }
    
    protected ExprList arguments = null ;
    private FunctionEnv env ;
    
    @Override
    public final void build(ExprList args)
    {
        arguments = args ;
        checkBuild(args) ;
    }

    @Override
    public List<NodeValue> exec(Binding binding, ExprList args, FunctionEnv env)
    {
        // This is merely to allow functions to be 
        this.env = env ;
        
        if ( args == null )
            // The contract on the function interface is that this should not happen.
            throw new ARQInternalErrorException("SelectorBase: Null args list") ;
        
        List<NodeValue> evalArgs = new ArrayList<>() ;
        for ( Expr e : args )
        {
            NodeValue x = e.eval( binding, env );
            evalArgs.add( x );
        }
        
        List<NodeValue> nv =  exec(evalArgs) ;
        arguments = null ;
        return nv ;
    }
    
    /** Return the Context object for this execution */
    public Context getContext() { return env.getContext() ; }
    
    /** Selector call to a list of evaluated argument values */ 
    public abstract List<NodeValue> exec(List<NodeValue> args) ;

    public void checkBuild(ExprList args) {
        if ( args.size() != 2 )
            throw new QueryBuildException("Selector '"+this.getClass().getName()+"' takes two arguments") ;
    }
    
    public String getIRI() {
        return iri;
    }
    
//    /** Get argument, indexing from 1 **/
//    public NodeValue getArg(int i)
//    {
//        i = i-1 ;
//        if ( i < 0 || i >= arguments.size()  )
//            return null ;
//        return (NodeValue)arguments.get(i) ;
//    }
}
