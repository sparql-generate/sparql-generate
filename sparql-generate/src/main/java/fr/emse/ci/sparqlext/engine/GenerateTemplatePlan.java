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
package fr.emse.ci.sparqlext.engine;

import fr.emse.ci.sparqlext.SPARQLExt;
import fr.emse.ci.sparqlext.SPARQLExtException;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.graph.Node;
import org.apache.jena.query.Dataset;
import org.apache.jena.sparql.core.Var;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.apache.jena.query.ResultSet;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.engine.binding.BindingMap;
import org.apache.jena.sparql.util.Context;

/**
 * Executes the GENERATE {...} template.
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class GenerateTemplatePlan implements ExecutionPlan {

    /**
     * the logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(GenerateTemplatePlan.class);

    /**
     * the list of generate blocks.
     */
    private final List<ExecutionPlan> templateElementPlans;

    /**
     * Constructs a new Generate plan from the given list of generate block
     * plans.
     *
     * @param templateElementPlans a list of plans
     */
    public GenerateTemplatePlan(
            final List<ExecutionPlan> templateElementPlans) {
        this.templateElementPlans = templateElementPlans;
    }

    /**
     * gets the list of plans for generate blocks.
     *
     * @return a list of plans
     */
    public List<ExecutionPlan> getTemplateElementsPlans() {
        return templateElementPlans;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<Void> exec(
            final Dataset inputDataset,
            final List<Var> variables,
            final List<Binding> values,
            final BNodeMap bNodeMap,
            final Context context,
            final StreamRDF outputGenerate,
            final Consumer<ResultSet> outputSelect,
            final Consumer<String> outputTemplate) {
        final List<CompletableFuture<Void>> fs = new ArrayList<>();
//        final BNodeMap bNodeMap2 = new BNodeMap(bNodeMap, variables, values);
        templateElementPlans.forEach((el) -> {
            if (el instanceof GenerateTriplesPlan) {
                final GenerateTriplesPlan subPlan = (GenerateTriplesPlan) el;
                fs.add(subPlan.exec(inputDataset, variables, values, bNodeMap, context, outputGenerate, outputSelect, outputTemplate));
//                fs.add(subPlan.exec(inputDataset, variables, values, bNodeMap2, context, outputGenerate, outputSelect, outputTemplate));
            } else if (el instanceof RootPlanImpl) {
                final RootPlanImpl subPlan = (RootPlanImpl) el;
                final List<Var> newVariables = getNewVariables(subPlan, variables);
                final List<Binding> newValues = makeQuerySolution(newVariables, values, bNodeMap);
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Entering sub SPARQL-Generate with \n\t" + SPARQLExt.log(variables, newValues));
                }
                fs.add(subPlan.exec(inputDataset, newVariables, newValues, bNodeMap, context, outputGenerate, outputSelect, outputTemplate));
            } else {
                throw new SPARQLExtException("should not reach this point" + el);
            }
        });
        return CompletableFuture.allOf(fs.toArray(new CompletableFuture[fs.size()]));
    }

    private List<Var> getNewVariables(
            final RootPlanImpl rootPlan,
            final List<Var> variables) {
        final List<Var> newVariables = new ArrayList<>();
        final List<Var> signature = rootPlan.getQuerySignature();
        if (signature == null) {
            newVariables.addAll(variables);
        } else {
            signature.forEach(newVariables::add);
        }
        return newVariables;
    }

    private List<Binding> makeQuerySolution(
            final List<Var> variables,
            final List<Binding> values,
            final BNodeMap bNodeMap) {
        return values.stream().map((binding) -> {
            final BindingMap b = BindingFactory.create();
            variables.forEach((v) -> {
                Node n = binding.get(v);
                if (!(n == null)) {
                    if (bNodeMap.contains(n)) {
                        b.add(v, bNodeMap.get(n));
                    } else {
                        b.add(v, n);
                    }
                }
            });
            return b;
        }).collect(Collectors.toList());
    }

}
