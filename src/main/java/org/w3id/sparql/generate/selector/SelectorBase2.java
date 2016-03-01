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
package org.w3id.sparql.generate.selector;

import java.util.List ;
import org.apache.jena.query.QueryBuildException;
import org.apache.jena.sparql.ARQInternalErrorException;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;


/** Support for a selector of one argument. */

public abstract class SelectorBase2 extends SelectorBase
{
    
    public SelectorBase2(String iri) {
        super(iri);
    }
    
    @Override
    public void checkBuild(ExprList args)
    { 
        if ( args.size() != 2 )
            throw new QueryBuildException("Selector '"+this.getClass().getName()+"' takes two arguments") ;
    }

    
    @Override
    public final List<NodeValue> exec(List<NodeValue> args)
    {
        if ( args == null )
            // The contract on the selector interface is that this should not happen.
            throw new ARQInternalErrorException(this.getClass().getName()+": Null args list") ;
        
        if ( args.size() != 2 )
            throw new ExprEvalException(this.getClass().getName()+": Wrong number of arguments: Wanted 2, got "+args.size()) ;
        
        NodeValue v1 = args.get(0) ;
        NodeValue v2 = args.get(1) ;
        
        return exec(v1, v2) ;
    }
    
    public abstract List<NodeValue> exec(NodeValue v1, NodeValue v2) ;
}
