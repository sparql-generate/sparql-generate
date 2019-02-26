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

import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerateException;
import com.github.thesmartenergy.sparql.generate.jena.engine.GeneratePlan;
import com.github.thesmartenergy.sparql.generate.jena.engine.GenerateTemplateElementPlan;
import com.github.thesmartenergy.sparql.generate.jena.syntax.Param;
import java.util.List;
import org.apache.jena.graph.Node;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.util.Context;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * The GENERATE {...} template.
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class GenerateTemplatePlanImpl extends PlanBase implements GeneratePlan {

    /**
     * the logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(GenerateTemplatePlanImpl.class);

    /**
     * the list of generate blocks.
     */
    private final List<GenerateTemplateElementPlan> templateElementPlans;

    /**
     * Constructs a new Generate plan from the given list of generate block
     * plans.
     *
     * @param templateElementPlans a list of plans
     */
    public GenerateTemplatePlanImpl(
            final List<GenerateTemplateElementPlan> templateElementPlans) {
        this.templateElementPlans = templateElementPlans;
    }

    /**
     * gets the list of plans for generate blocks.
     *
     * @return a list of plans
     */
    public List<GenerateTemplateElementPlan> getTemplateElementsPlans() {
        return templateElementPlans;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void exec(
            final Dataset inputDataset,
            final StreamRDF outputStream,
            final List<Var> variables,
            final List<BindingHashMapOverwrite> values,
            final BNodeMap bNodeMap,
            final Context context) {
        values.forEach((binding) -> {
            BNodeMap bNodeMap2 = new BNodeMap(bNodeMap, binding);
            templateElementPlans.forEach((el) -> {
                if (el instanceof GenerateTriplesPlanImpl) {
                    GenerateTriplesPlanImpl subPlanTriples
                            = (GenerateTriplesPlanImpl) el;
                    subPlanTriples.exec(
                            inputDataset, outputStream,
                            binding, bNodeMap2);
                } else if (el instanceof RootPlanImpl) {
                    RootPlanImpl rootPlan = (RootPlanImpl) el;
                    QuerySolutionMap b = new QuerySolutionMap();
                    List<Param> signature = rootPlan.getQuerySignature();
                    if(signature == null) {
                        binding.varsList().forEach((v) -> {
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
                    } else {
                        signature.forEach((param) -> {
                            Var p = param.getVar();
                            Node n = binding.get(p);
                            if (!(n == null)) {
                                if (bNodeMap.contains(n)) {
                                    b.add(p.getVarName(), inputDataset
                                            .getDefaultModel()
                                            .asRDFNode(bNodeMap.get(n)));
                                } else {
                                    b.add(p.getVarName(), inputDataset
                                            .getDefaultModel()
                                            .asRDFNode(n));
                                }
                            }
                        });                        
                    }
                    LOG.trace("Entering sub SPARQL-Generate with \n\t" + b);
                    rootPlan.exec(inputDataset, b, outputStream, bNodeMap2, context);
                } else {
                    throw new SPARQLGenerateException("should not reach this"
                            + " point");
                }
            });
        });
    }

}
