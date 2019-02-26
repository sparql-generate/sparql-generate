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
import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerateException;
import com.github.thesmartenergy.sparql.generate.jena.engine.GeneratePlan;
import com.github.thesmartenergy.sparql.generate.jena.engine.PlanFactory;
import com.github.thesmartenergy.sparql.generate.jena.engine.RootPlan;
import com.github.thesmartenergy.sparql.generate.jena.query.SPARQLGenerateQuery;
import com.github.thesmartenergy.sparql.generate.jena.stream.LookUpRequest;
import com.github.thesmartenergy.sparql.generate.jena.syntax.Param;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.commons.io.IOUtils;
import org.apache.jena.ext.com.google.common.collect.Lists;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.Substitute;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.util.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author maxime.lefrancois
 */
public class GenerateNamedQueryPlanImpl extends PlanBase implements GeneratePlan {

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
    public void exec(
            final Dataset inputDataset,
            final StreamRDF outputStream,
            final List<Var> variables,
            final List<BindingHashMapOverwrite> values,
            final BNodeMap bNodeMap,
            final Context context) {


        for(BindingHashMapOverwrite binding : values) {

            // resolves the actual name of the query and get the expanded query
            final Node n = Substitute.substitute(name, binding);
            if(!n.isURI()) {
                throw new UnsupportedOperationException("name of sub query resolved to something else than a URI: " + n);
            }
            final String queryName = n.getURI();
            final RootPlan plan = getPlan(queryName, context);
            if(plan == null) {
                return;
            }
            final SPARQLGenerateQuery expandedQuery = getLoadedQueries(context).get(queryName);
            final List<Param> querySignature = expandedQuery.getQuerySignature();

            // prepare the initial binding
            QuerySolutionMap newInitialBinding = new QuerySolutionMap();
            if(callParameters!=null && querySignature!=null) {
                int max = Math.min(callParameters.size(), querySignature.size());
                if(callParameters.size() != querySignature.size()) {
                    LOG.debug("The number of parameters is not equal to the size of the signature of query " + queryName+ ". call parameters: " + callParameters + ". and query signature" + querySignature);
                }
                for(int i=0; i<max; i++) {
                    final String parameter = querySignature.get(i).getVar().getVarName();
                    Node value = callParameters.get(i);
                    value = Substitute.substitute(value, binding);
                    if(value.isConcrete()) {
                        final RDFNode paramJena = inputDataset.getDefaultModel().asRDFNode(value);
                        newInitialBinding.add(parameter, paramJena);
                    }
                }
            }
            if(alreadyExecuted(context, queryName, newInitialBinding)) {
                return;
            }
            LOG.debug("Executing " + queryName + ": " + newInitialBinding);
            registerExecution(context, queryName, newInitialBinding);
            
            final BNodeMap bNodeMap2 = new BNodeMap();
            final BindingHashMapOverwrite newBinding = new BindingHashMapOverwrite(newInitialBinding, context);
            final List<Var> newVariables = newBinding.varsList();
            final List<BindingHashMapOverwrite> newValues = Lists.newArrayList(newBinding);
            plan.exec(inputDataset, outputStream, newVariables, newValues, bNodeMap2, context);
        }
    }

    private RootPlan getPlan(String queryName, Context context) {
        if(getLoadedPlans(context).containsKey(queryName)) {
            return getLoadedPlans(context).get(queryName);
        }

        try {
            final LookUpRequest request = new LookUpRequest(queryName, SPARQLGenerate.MEDIA_TYPE);
            InputStream in = SPARQLGenerate.getStreamManager().open(request);
            String qString = IOUtils.toString(in, Charset.forName("UTF-8"));
            SPARQLGenerateQuery q
                    = (SPARQLGenerateQuery) QueryFactory.create(qString,
                            SPARQLGenerate.SYNTAX);
            getLoadedQueries(context).put(queryName, q);

            RootPlan plan = PlanFactory.create(q);
            getLoadedPlans(context).put(queryName, plan);

            return plan;
        } catch (NullPointerException ex) {
            LOG.error("NullPointerException while loading the query"
                    + " file " + queryName + ": " + ex.getMessage());
            throw new SPARQLGenerateException("NullPointerException exception while loading the query"
                    + " file " + queryName + ": " + ex.getMessage());
        } catch (IOException ex) {
            LOG.error("IOException while loading the query"
                    + " file " + queryName + ": " + ex.getMessage());
            throw new SPARQLGenerateException("IOException  while loading the query"
                    + " file " + queryName + ": " + ex.getMessage());
        } catch (QueryParseException ex) {
            LOG.error("QueryParseException while parsing the query"
                    + queryName + ": " + ex.getMessage());
            throw new SPARQLGenerateException("QueryParseException while parsing the query "
                    + queryName + ": " + ex.getMessage());
        }
    }
}
