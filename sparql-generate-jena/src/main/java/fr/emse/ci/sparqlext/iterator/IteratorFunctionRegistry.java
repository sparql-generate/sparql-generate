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
import fr.emse.ci.sparqlext.query.SPARQLExtQuery;
import fr.emse.ci.sparqlext.stream.LookUpRequest;
import fr.emse.ci.sparqlext.stream.SPARQLExtStreamManager;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.jena.query.ARQ;
import org.apache.jena.sparql.util.Context;
import java.util.Iterator;
import java.util.Objects;
import org.apache.commons.io.IOUtils;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.riot.SysRIOT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registry of iterator functions.
 *
 * @author maxime.lefrancois
 */
public class IteratorFunctionRegistry //extends HashMap<String, Iterator>
{

    private static final Logger LOG = LoggerFactory.getLogger(IteratorFunctionRegistry.class);

    // Extract a Registry class and do casting and initialization here.
    private final Context context;
    private final Map<String, IteratorFunctionFactory> registry = new HashMap<>();
    private final Set<String> failedAttempts = new HashSet<>();

    public synchronized static IteratorFunctionRegistry standardRegistry() {
        IteratorFunctionRegistry reg = new IteratorFunctionRegistry(ARQ.getContext());
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
        return (IteratorFunctionRegistry) context.get(SPARQLExt.REGISTRY_ITERATORS);
    }

    public static void set(Context context, IteratorFunctionRegistry reg) {
        context.set(SPARQLExt.REGISTRY_ITERATORS, reg);
    }

    public IteratorFunctionRegistry() {
        this(ARQ.getContext());
    }

    public IteratorFunctionRegistry(Context context) {
        this.context = context;
    }

    public IteratorFunctionRegistry(IteratorFunctionRegistry parent, Context context) {
        this(context);
        Iterator<String> uris = parent.keys();
        while (uris.hasNext()) {
            String uri = uris.next();
            registry.put(uri, parent.get(uri));
        }
    }

    /**
     * Insert a class that is the iterator function implementation
     *
     * @param uri String URI
     * @param funcClass Class for the function (new instance called).
     */
    public void put(String uri, Class<?> funcClass) {
        if (!IteratorFunction.class.isAssignableFrom(funcClass)) {
            LOG.warn("Class " + funcClass.getName() + " is not a Iterator");
            return;
        }
        registry.put(uri, new IteratorFunctionFactoryAuto(funcClass));
    }

    /**
     * Insert a iterator. Re-inserting with the same URI overwrites the old
     * entry.
     *
     * @param uri
     * @param f
     */
    public void put(String uri, IteratorFunctionFactory f) {
        registry.put(uri, f);
    }

    public boolean isRegistered(String uri) {
        return registry.containsKey(uri);
    }

    /**
     * Iterate over URIs
     */
    public Iterator<String> keys() {
        return registry.keySet().iterator();
    }

    /**
     * Remove by URI
     *
     * @param uri
     * @return
     */
    public IteratorFunctionFactory remove(String uri) {
        return registry.remove(uri);
    }

    /**
     * Lookup by URI
     *
     * @return the iterator, or null
     */
    public IteratorFunctionFactory get(String uri) {
        if (registry.get(uri) != null) {
            return registry.get(uri);
        }
        if (failedAttempts.contains(uri)) {
            return null;
        }
        final LookUpRequest req = new LookUpRequest(uri, "application/vnd.sparql-generate");
        final SPARQLExtStreamManager sm = (SPARQLExtStreamManager) context.get(SysRIOT.sysStreamManager);
        Objects.requireNonNull(sm);
        TypedInputStream tin = sm.open(req);
        if (tin == null) {
            LOG.warn(String.format("Could not look up iterator %s", uri));
            failedAttempts.add(uri);
            return null;
        }
        String selectString;
        try {
            selectString = IOUtils.toString(tin.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            LOG.warn(String.format("Could not read function %s as UTF-8 string", uri));
            failedAttempts.add(uri);
            return null;
        }
        SPARQLExtQuery selectQuery;
        try {
            selectQuery = (SPARQLExtQuery) QueryFactory.create(selectString, SPARQLExt.SYNTAX);
        } catch (Exception ex) {
            LOG.warn(String.format("Could not parse iterator %s", uri), ex);
            failedAttempts.add(uri);
            return null;
        }
        if (!selectQuery.isSelectType()) {
            LOG.warn(String.format("The query is not a SELECT: %s", uri));
            failedAttempts.add(uri);
            return null;
        }
        final SPARQLExtIteratorFunctionFactory iterator = new SPARQLExtIteratorFunctionFactory(selectQuery);
        put(uri, iterator);
        return iterator;
    }

    class SPARQLExtIteratorFunctionFactory implements IteratorFunctionFactory {

        final SPARQLExtQuery selectQuery;

        public SPARQLExtIteratorFunctionFactory(final SPARQLExtQuery selectQuery) {
            this.selectQuery = selectQuery;
        }

        @Override
        public IteratorFunction create(String uri) {
            return new SPARQLExtIteratorFunction(selectQuery, context);
        }
    }
}
