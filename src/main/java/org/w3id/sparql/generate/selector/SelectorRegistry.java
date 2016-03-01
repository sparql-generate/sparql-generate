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
import java.util.HashMap ;
import java.util.HashSet ;
import java.util.Iterator ;
import java.util.Map ;
import java.util.Set ;

import org.apache.jena.atlas.logging.Log ;
import org.apache.jena.query.ARQ;
import org.apache.jena.sparql.ARQConstants;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sparql.util.MappedLoader;
import org.w3id.sparql.generate.SPARQLGenerateConstants;


public class SelectorRegistry //extends HashMap<String, Selector>
{
    // Extract a Registry class and do casting and initialization here.
    Map<String, SelectorFactory> registry = new HashMap<>() ;
    Set<String> attemptedLoads = new HashSet<>() ;
    
    public synchronized static SelectorRegistry standardRegistry()
    {
        SelectorRegistry reg = new SelectorRegistry() ;
        return reg ;   
    }
    
    public synchronized static SelectorRegistry get()
    {
        // Intialize if there is no registry already set 
        SelectorRegistry reg = get(ARQ.getContext()) ;
        if ( reg == null )
        {
            reg = standardRegistry() ;
            set(ARQ.getContext(), reg) ;
        }

        return reg ;
    }

    public static SelectorRegistry get(Context context)
    {
        if ( context == null )
            return null ;
        return (SelectorRegistry) context.get(SPARQLGenerateConstants.registrySelectors) ;
    }
    
    public static void set(Context context, SelectorRegistry reg)
    {
        context.set(SPARQLGenerateConstants.registrySelectors, reg) ;
    }

    public SelectorRegistry()
    {}
    /** Insert a function. Re-inserting with the same URI overwrites the old entry. 
     * 
     * @param uri
     * @param f
     */
    public void put(String uri, SelectorFactory f) { registry.put(uri,f) ; }

    /** Insert a class that is the function implementation 
     * 
     * @param uri           String URI
     * @param funcClass     Class for the function (new instance called).
     */
    public void put(String uri, Class<?> funcClass)
    { 
        if ( ! Selector.class.isAssignableFrom(funcClass) )
        {
            Log.warn(this, "Class "+funcClass.getName()+" is not a Selector" );
            return ; 
        }
        
        registry.put(uri, new SelectorFactoryAuto(funcClass)) ;
    }
    
    /** Lookup by URI */
    public SelectorFactory get(String uri)
    {
        SelectorFactory function = registry.get(uri) ;
        if ( function != null )
            return function ;

        if ( attemptedLoads.contains(uri) )
            return null ;

        Class<?> functionClass = MappedLoader.loadClass(uri, Selector.class) ;
        if ( functionClass == null )
            return null ;
        // Registry it
        put(uri, functionClass) ;
        attemptedLoads.add(uri) ;
        // Call again to get it.
        return registry.get(uri) ;
    }
    
    public boolean isRegistered(String uri) { return registry.containsKey(uri) ; }
    
    /** Remove by URI */
    public SelectorFactory remove(String uri) { return registry.remove(uri) ; } 
    
    /** Iterate over URIs */
    public Iterator<String> keys() { return registry.keySet().iterator() ; }

}
