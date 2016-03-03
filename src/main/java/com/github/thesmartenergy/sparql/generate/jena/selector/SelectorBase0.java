/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.thesmartenergy.sparql.generate.jena.selector;

import java.util.List ;
import org.apache.jena.query.QueryBuildException;
import org.apache.jena.sparql.ARQInternalErrorException;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;


/** Support for a function of zero arguments. */

public abstract class SelectorBase0 extends SelectorBase
{
    
    public SelectorBase0(String iri) {
        super(iri);
    }
    
    @Override
    public void checkBuild(ExprList args)
    { 
        if ( args.size() != 0 )
            throw new QueryBuildException("Selector '"+this.getClass().getName()+"' takes no arguments") ;
    }
    
    @Override
    public final List<NodeValue> exec(List<NodeValue> args)
    {
        if ( args == null )
            // The contract on the function interface is that this should not happen.
            throw new ARQInternalErrorException("Selector '"+this.getClass().getName()+" Null args list") ;
        
        if ( args.size() != 0 )
            throw new ExprEvalException("Selector '"+this.getClass().getName()+" Wanted 0, got "+args.size()) ;
        
        return exec() ;
    }
    
    public abstract List<NodeValue> exec() ;
}
