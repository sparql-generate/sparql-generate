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
package com.github.thesmartenergy.sparql.generate.jena.engine.impl;

import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerate;
import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerateContext;
import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerateException;
import com.github.thesmartenergy.sparql.generate.jena.engine.GeneratePlan;
import com.github.thesmartenergy.sparql.generate.jena.engine.PlanFactory;
import com.github.thesmartenergy.sparql.generate.jena.engine.RootPlan;
import com.github.thesmartenergy.sparql.generate.jena.query.SPARQLGenerateQuery;
import com.github.thesmartenergy.sparql.generate.jena.stream.LookUpRequest;
import com.github.thesmartenergy.sparql.generate.jena.stream.SPARQLGenerateStreamManager;
import com.github.thesmartenergy.sparql.generate.jena.syntax.Param;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;
import org.apache.commons.io.IOUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.core.Substitute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.CompletableFuture;
import org.apache.jena.riot.system.StreamRDF;

/**
 *
 * @author maxime.lefrancois
 */
public class GenerateNamedQueryPlanImpl implements GeneratePlan {

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(GenerateNamedQueryPlanImpl.class);

    private final Node name;

    private final List<Node> callParameters;

    public GenerateNamedQueryPlanImpl(Node name, List<Node> callParameters) {
        Objects.requireNonNull(name, "name must not be null");
        this.name = name;
        this.callParameters = callParameters;
    }
    

    @Override
    public CompletableFuture<Void> exec(
            final Dataset inputDataset,
            final BindingHashMapOverwrite binding,
            final StreamRDF outputStream,
            final BNodeMap bNodeMap,
            final SPARQLGenerateContext context,
            final int position) {
        return exec(inputDataset, binding, outputStream, bNodeMap, context);
    }
    
    @Override
    public CompletableFuture<Void> exec(
            final Dataset inputDataset,
            final BindingHashMapOverwrite binding,
            final StreamRDF outputStream,
            final BNodeMap bNodeMap,
            final SPARQLGenerateContext context) {
        // resolves the actual name of the query and get the expanded query
        final Node n = Substitute.substitute(name, binding);
        if (!n.isURI()) {
            throw new SPARQLGenerateException("Name of sub query "
                    + "resolved to something else than a URI: " + n);
        }
        final String queryName = n.getURI();
        final RootPlan plan;
        try {
            plan = getPlan(queryName, context);
        } catch (NullPointerException | IOException | QueryParseException ex) {
            String message = "Error while loading the query file " + queryName;
            throw new SPARQLGenerateException(message, ex);
        }
        if (plan == null) {
            throw new NullPointerException("Could not construct plan for " + queryName);
        }
        final SPARQLGenerateQuery expandedQuery = context.getLoadedQueries().get(queryName);
        final List<Param> querySignature = expandedQuery.getQuerySignature();

        // prepare the initial binding
        final QuerySolutionMap newInitialBinding = new QuerySolutionMap();
        if (callParameters != null && querySignature != null) {
            int max = Math.min(callParameters.size(), querySignature.size());
            if (callParameters.size() != querySignature.size()) {
                throw new SPARQLGenerateException("The number of "
                        + "parameters is not equal to the size of the"
                        + " signature of query " + queryName + ". call"
                        + " parameters: " + callParameters + ". and"
                        + " query signature" + querySignature);
            }
            for (int i = 0; i < max; i++) {
                final String parameter = querySignature.get(i).getVar().getVarName();
                Node value = callParameters.get(i);
                value = Substitute.substitute(value, binding);
                if (value.isConcrete()) {
                    final RDFNode paramJena = inputDataset.getDefaultModel().asRDFNode(value);
                    newInitialBinding.add(parameter, paramJena);
                }
            }
        }
        final BindingHashMapOverwrite newBinding
                = new BindingHashMapOverwrite(newInitialBinding, context);
        if (context.alreadyExecuted(queryName, newBinding)) {
            LOG.debug("Already executed " + queryName + " with same bindings.");
            LOG.trace("Already executed " + queryName + " with bindings " + newInitialBinding);
            return null;
        }
        context.registerExecution(queryName, newBinding);
        LOG.trace("Executing " + queryName + ": " + newInitialBinding);

        return plan.exec(inputDataset, newBinding, outputStream, new BNodeMap(), context);
    }

    private RootPlan getPlan(
            String queryName,
            final SPARQLGenerateContext context)
            throws NullPointerException, IOException, QueryParseException {
        if (context.getLoadedPlans().containsKey(queryName)) {
            return context.getLoadedPlans().get(queryName);
        }
        final LookUpRequest request = new LookUpRequest(queryName, SPARQLGenerate.MEDIA_TYPE);
        final SPARQLGenerateStreamManager sm = context.getStreamManager();
        final InputStream in = sm.open(request);
        String qString = IOUtils.toString(in, Charset.forName("UTF-8"));
        final SPARQLGenerateQuery q
                = (SPARQLGenerateQuery) QueryFactory.create(qString,
                        SPARQLGenerate.SYNTAX);
        context.getLoadedQueries().put(queryName, q);
        final RootPlan plan = PlanFactory.createPlanForSubQuery(q);
        context.getLoadedPlans().put(queryName, plan);
        return plan;
    }
}
