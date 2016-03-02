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

package org.w3id.sparql.generate.selector;

import java.util.List ;
import org.apache.jena.query.QueryBuildException;
import org.apache.jena.sparql.ARQInternalErrorException;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;


/** Support for a function of one argument. */

public abstract class SelectorBase1 extends SelectorBase
{
    
    public SelectorBase1(String iri) {
        super(iri);
    }
    
    @Override
    public void checkBuild(ExprList args)
    { 
        if ( args.size() != 1 )
            throw new QueryBuildException("Selector '"+this.getClass().getName()+"' takes one argument") ;
    }

    @Override
    public final List<NodeValue> exec(List<NodeValue> args)
    {
        if ( args == null )
            // The contract on the function interface is that this should not happen.
            throw new ARQInternalErrorException("SelectorBase1: Null args list") ;
        
        if ( args.size() != 1 )
            throw new ExprEvalException("SelectorBase1: Wrong number of arguments: Wanted 1, got "+args.size()) ;
        
        NodeValue v1 = args.get(0) ;
        
        return exec(v1) ;
    }
    
    public abstract List<NodeValue> exec(NodeValue v) ;
}
