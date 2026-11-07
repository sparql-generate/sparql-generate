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
package fr.mines_stetienne.ci.sparql_generate.function;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.query.SPARQLExtQuery;
import fr.mines_stetienne.ci.sparql_generate.stream.LookUpRequest;
import fr.mines_stetienne.ci.sparql_generate.stream.SPARQLExtStreamManager;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.apache.jena.atlas.logging.Log;

import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.query.QueryBuildException;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.riot.SysRIOT;
import org.apache.jena.sparql.function.Function;
import org.apache.jena.sparql.function.FunctionFactory;
import org.apache.jena.sparql.function.FunctionRegistry;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sparql.util.MappedLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Maxime Lefrançois
 */
public class SPARQLExtFunctionRegistry extends FunctionRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(SPARQLExtFunctionRegistry.class);

    private final Context context;
    private final Map<String, FunctionFactory> registry = new HashMap<>();
    private final Set<String> attemptedLoads = new HashSet<>();

    public SPARQLExtFunctionRegistry(FunctionRegistry parent, Context context) {
        Iterator<String> uris = parent.keys();
        while (uris.hasNext()) {
            String uri = uris.next();
            registry.put(uri, parent.get(uri));
        }
        this.context = context;
    }

    /**
     * Insert a class that is the function implementation
     *
     * @param uri String URI
     * @param funcClass Class for the function (new instance called).
     */
    @Override
    public void put(String uri, Class<?> funcClass) {
        if (!Function.class.isAssignableFrom(funcClass)) {
            Log.warn(this, "Class " + funcClass.getName() + " is not a Function");
            return;
        }
        put(uri, new FunctionFactoryAuto(funcClass));
    }

    /**
     * Insert a function. Re-inserting with the same URI overwrites the old
     * entry.
     *
     * @param uri
     * @param f
     */
    @Override
    public void put(String uri, FunctionFactory f) {
        registry.put(uri, f);
    }

    @Override
    public boolean isRegistered(String uri) {
        return registry.containsKey(uri);
    }

    /**
     * Remove by URI
     *
     * @param uri
     * @return
     */
    @Override
    public FunctionFactory remove(String uri) {
        return registry.remove(uri);
    }

    /**
     * Lookup by URI
     *
     * @return the function, or null
     */
    @Override
    public FunctionFactory get(String uri) {
        if (registry.get(uri) != null) {
            return registry.get(uri);
        }
        if (attemptedLoads.contains(uri)) {
            return null;
        }
        // functions of Jena loaded by class name: afn:, java:
        final Class<?> functionClass = MappedLoader.loadClass(uri, Function.class);
        if (functionClass != null) {
            put(uri, functionClass);
            attemptedLoads.add(uri);
            return registry.get(uri);
        }
        if (isBuiltInNamespace(uri)) {
            LOG.warn(String.format("Unknown function %s", uri));
            attemptedLoads.add(uri);
            return null;
        }
        LOG.trace("attempting load " + uri);
        final LookUpRequest req = new LookUpRequest(uri, "application/vnd.sparql-generate");
        final SPARQLExtStreamManager sm = (SPARQLExtStreamManager) context.get(SysRIOT.sysStreamManager);
        Objects.requireNonNull(sm);
        TypedInputStream tin = sm.openOrNull(req);
        if (tin == null) {
            LOG.warn(String.format("Could not look up function %s", uri));
            attemptedLoads.add(uri);
            return null;
        }
        String functionString;
        try {
            functionString = IOUtils.toString(tin.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            LOG.warn(String.format("Could not read function %s as UTF-8 string", uri));
            attemptedLoads.add(uri);
            return null;
        }
        SPARQLExtQuery functionQuery;
        try {
            functionQuery = (SPARQLExtQuery) QueryFactory.create(functionString, SPARQLExt.SYNTAX);
        } catch (Exception ex) {
            LOG.warn(String.format("Could not parse function %s", uri), ex);
            attemptedLoads.add(uri);
            return null;
        }
        if (!functionQuery.isFunctionType()) {
            LOG.warn(String.format("The query is not a function: %s", uri));
            attemptedLoads.add(uri);
            return null;
        }
        final SPARQLExtFunctionFactory function = new SPARQLExtFunctionFactory(functionQuery);
        put(uri, function);
        attemptedLoads.add(uri);
        return function;
    }

    /**
     * Namespaces of the functions and iterators of SPARQL, XPath, Jena and
     * SPARQL-Generate. An unknown IRI in one of them is not looked up as a
     * FUNCTION or SELECT query: it would be a request on the network.
     */
    private static final String[] BUILT_IN_NAMESPACES = {
            "http://www.w3.org/2001/XMLSchema#",
            "http://www.w3.org/2005/xpath-functions",
            "http://www.w3.org/ns/sparql#",
            "http://jena.apache.org/",
            "java:",
            SPARQLExt.NS };

    /**
     * True if the IRI is in the namespace of the functions and iterators of
     * SPARQL, XPath, Jena or SPARQL-Generate.
     */
    public static boolean isBuiltInNamespace(String uri) {
        for (String ns : BUILT_IN_NAMESPACES) {
            if (uri.startsWith(ns)) {
                return true;
            }
        }
        return false;
    }

    class FunctionFactoryAuto implements FunctionFactory {

        Class<?> extClass;

        FunctionFactoryAuto(Class<?> xClass) {
            extClass = xClass;
        }

        @Override
        public Function create(String uri) {
            try {
                return (Function) extClass.newInstance();
            } catch (Exception e) {
                LOG.debug("Can't instantiate function"
                        + " for " + uri, e);
                throw new QueryBuildException("Can't instantiate function"
                        + " for " + uri, e);
            }
        }
    }

    class SPARQLExtFunctionFactory implements FunctionFactory {

        final SPARQLExtQuery functionQuery;

        public SPARQLExtFunctionFactory(final SPARQLExtQuery functionQuery) {
            this.functionQuery = functionQuery;
        }

        @Override
        public Function create(String uri) {
            return new SPARQLExtFunction(functionQuery);
        }
    }

}
