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
package com.github.thesmartenergy.sparql.generate.jena.engine.impl;

import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerateContext;
import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerateException;
import com.github.thesmartenergy.sparql.generate.jena.engine.GeneratePlan;
import com.github.thesmartenergy.sparql.generate.jena.syntax.Param;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.graph.Node;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.sparql.core.Var;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import java.util.concurrent.CompletableFuture;
import org.apache.jena.riot.system.StreamRDF;

/**
 * The GENERATE {...} template.
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class GenerateTemplatePlanImpl implements GeneratePlan {

    /**
     * the logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(GenerateTemplatePlanImpl.class);

    /**
     * the list of generate blocks.
     */
    private final List<GeneratePlan> templateElementPlans;

    /**
     * Constructs a new Generate plan from the given list of generate block
     * plans.
     *
     * @param templateElementPlans a list of plans
     */
    public GenerateTemplatePlanImpl(
            final List<GeneratePlan> templateElementPlans) {
        this.templateElementPlans = templateElementPlans;
    }

    /**
     * gets the list of plans for generate blocks.
     *
     * @return a list of plans
     */
    public List<GeneratePlan> getTemplateElementsPlans() {
        return templateElementPlans;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<Void> exec(
            final Dataset inputDataset,
            final BindingHashMapOverwrite binding,
            final StreamRDF outputStream,
            final BNodeMap bNodeMap,
            final SPARQLGenerateContext context) {
        return exec(inputDataset, binding, outputStream, bNodeMap, context, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<Void> exec(
            final Dataset inputDataset,
            final BindingHashMapOverwrite binding,
            final StreamRDF outputStream,
            final BNodeMap bNodeMap,
            final SPARQLGenerateContext context,
            final int position) {
        final List<CompletableFuture<Void>> fs = new ArrayList<>();
        final BNodeMap bNodeMap2 = new BNodeMap(bNodeMap, binding);
        templateElementPlans.forEach((el) -> {
            if (el instanceof GenerateTriplesPlanImpl) {
                final GenerateTriplesPlanImpl subPlan = (GenerateTriplesPlanImpl) el;
                fs.add(subPlan.exec(inputDataset, binding, outputStream, bNodeMap2, context, position));
            } else if (el instanceof RootPlanImpl) {
                final RootPlanImpl subPlan = (RootPlanImpl) el;
                final BindingHashMapOverwrite newBinding = makeQuerySolution(subPlan, inputDataset, binding, bNodeMap, context);
                LOG.trace("Binding was \n\t" + binding);
                LOG.trace("Entering sub SPARQL-Generate with \n\t" + newBinding);
                fs.add(subPlan.exec(inputDataset, newBinding, outputStream, bNodeMap2, context));
            } else {
                throw new SPARQLGenerateException("should not reach this point" + el);
            }
        });
        return CompletableFuture.allOf(fs.toArray(new CompletableFuture[fs.size()]));
    }

    private BindingHashMapOverwrite makeQuerySolution(
            final RootPlanImpl rootPlan,
            final Dataset inputDataset,
            final BindingHashMapOverwrite binding,
            final BNodeMap bNodeMap,
            final SPARQLGenerateContext context) {
        final QuerySolutionMap b = new QuerySolutionMap();
        final List<Var> variables = new ArrayList<>();
        final List<Param> signature = rootPlan.getQuerySignature();
        if (signature == null) {
            variables.addAll(binding.varsList());
        } else {
            signature.forEach(s -> variables.add(s.getVar()));
        }
        variables.forEach((v) -> {
            Node n = binding.get(v);
            if (!(n == null)) {
                if (bNodeMap.contains(n)) {
                    b.add(v.getVarName(), inputDataset
                            .getDefaultModel()
                            .asRDFNode(bNodeMap.get(n)));
                } else {
                    b.add(v.getVarName(), inputDataset
                            .getDefaultModel()
                            .asRDFNode(n));
                }
            }
        });
        return new BindingHashMapOverwrite(b, context);
    }
}
