/*
 * Copyright 2019 École des Mines de Saint-Étienne.
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
package fr.emse.ci.sparqlext.function;

import fr.emse.ci.sparqlext.SPARQLExt;
import fr.emse.ci.sparqlext.query.SPARQLExtQuery;
import fr.emse.ci.sparqlext.stream.LookUpRequest;
import fr.emse.ci.sparqlext.stream.SPARQLExtStreamManager;
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
 * @author maxime.lefrancois
 */
public class SPARQLExtFunctionRegistry extends FunctionRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(SPARQLExtFunctionRegistry.class);

    private final Context context;
    Map<String, FunctionFactory> registry = new HashMap<>();
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
        throw new UnsupportedOperationException("Should not reach this point");
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
        final LookUpRequest req = new LookUpRequest(uri, "application/vnd.sparql-generate");
        final SPARQLExtStreamManager sm = (SPARQLExtStreamManager) context.get(SysRIOT.sysStreamManager);
        Objects.requireNonNull(sm);
        TypedInputStream tin = sm.open(req);
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
