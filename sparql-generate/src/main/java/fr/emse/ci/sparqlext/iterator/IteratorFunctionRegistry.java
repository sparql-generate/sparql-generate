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
package fr.emse.ci.sparqlext.iterator;

import fr.emse.ci.sparqlext.SPARQLExt;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.jena.atlas.logging.Log;
import org.apache.jena.query.ARQ;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sparql.util.MappedLoader;
import java.util.Iterator;

/**
 * Registry of iterator functions.
 * 
 * @author maxime.lefrancois
 */
public class IteratorFunctionRegistry //extends HashMap<String, Iterator>
{

    // Extract a Registry class and do casting and initialization here.
    Map<String, IteratorFunctionFactory> registry = new HashMap<>();
    Set<String> attemptedLoads = new HashSet<>();

    public synchronized static IteratorFunctionRegistry standardRegistry() {
        IteratorFunctionRegistry reg = new IteratorFunctionRegistry();
        return reg;
    }

    public synchronized static IteratorFunctionRegistry get() {
        // Intialize if there is no registry already set 
        IteratorFunctionRegistry reg = get(ARQ.getContext());
        if (reg == null) {
            reg = standardRegistry();
            set(ARQ.getContext(), reg);
        }

        return reg;
    }

    public static IteratorFunctionRegistry get(Context context) {
        if (context == null) {
            return null;
        }
        return (IteratorFunctionRegistry) context.get(SPARQLExt.Constants.registryIterators);
    }

    public static void set(Context context, IteratorFunctionRegistry reg) {
        context.set(SPARQLExt.Constants.registryIterators, reg);
    }

    public IteratorFunctionRegistry() {
    }

    /**
     * Insert a function. Re-inserting with the same URI overwrites the old
     * entry.
     *
     * @param uri
     * @param f
     */
    public void put(String uri, IteratorFunctionFactory f) {
        registry.put(uri, f);
    }

    /**
     * Insert a class that is the iterator function implementation
     *
     * @param uri String URI
     * @param funcClass Class for the function (new instance called).
     */
    public void put(String uri, Class<?> funcClass) {
        if (!IteratorFunction.class.isAssignableFrom(funcClass)) {
            Log.warn(this, "Class " + funcClass.getName() + " is not a Iterator");
            return;
        }

        registry.put(uri, new IteratorFunctionFactoryAuto(funcClass));
    }

    /**
     * Lookup by URI
     */
    public IteratorFunctionFactory get(String uri) {
        IteratorFunctionFactory function = registry.get(uri);
        if (function != null) {
            return function;
        }

        if (attemptedLoads.contains(uri)) {
            return null;
        }

        Class<?> functionClass = MappedLoader.loadClass(uri, IteratorFunction.class);
        if (functionClass == null) {
            return null;
        }
        // Registry it
        put(uri, functionClass);
        attemptedLoads.add(uri);
        // Call again to get it.
        return registry.get(uri);
    }

    public boolean isRegistered(String uri) {
        return registry.containsKey(uri);
    }

    /**
     * Remove by URI
     */
    public IteratorFunctionFactory remove(String uri) {
        return registry.remove(uri);
    }

    /**
     * Iterate over URIs
     */
    public Iterator<String> keys() {
        return registry.keySet().iterator();
    }

}
