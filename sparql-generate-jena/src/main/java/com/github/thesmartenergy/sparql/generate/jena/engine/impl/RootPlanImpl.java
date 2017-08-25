/*
 * Copyright 2016 The Apache Software Foundation.
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

import com.github.thesmartenergy.sparql.generate.jena.engine.GeneratePlan;
import com.github.thesmartenergy.sparql.generate.jena.engine.GenerateTemplateElementPlan;
import com.github.thesmartenergy.sparql.generate.jena.engine.RootPlan;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.ext.com.google.common.collect.Lists;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.core.Var;
import org.apache.log4j.Logger;
import com.github.thesmartenergy.sparql.generate.jena.engine.IteratorOrSourcePlan;
import com.github.thesmartenergy.sparql.generate.jena.engine.SelectPlan;
import java.util.Objects;
import org.apache.jena.query.QuerySolutionMap;

/**
 * Entry point to a SPARQL Generate query execution.
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public final class RootPlanImpl extends PlanBase implements RootPlan, 
        GeneratePlan, GenerateTemplateElementPlan {

    /**
     * The logger.
     */
    private static final Logger LOG = Logger.getLogger(RootPlanImpl.class);

    /**
     * Selector and Source plans.
     */
    private final List<IteratorOrSourcePlan> iteratorAndSourcePlans;

    /**
     * The plan for the SPARQL SELECT.
     */
    private final SelectPlan selectPlan;

    /**
     * The plan for the GENERATE clause.
     */
    private final GeneratePlan generatePlan;

    /**
     * The prefix mapping.
     */
    private final PrefixMapping prefixMapping;

    /**
     * true if the query generate template is specified by a URI.
     */
    private final boolean distant;

    /**
     * Get the plans for the ITERATOR and SOURCE clauses.
     *
     * @return the plans.
     */
    public List<IteratorOrSourcePlan> getIteratorAndSourcePlans() {
        return iteratorAndSourcePlans;
    }

    /**
     * Gets the plan for the SPARQL SELECT.
     *
     * @return -
     */
    public SelectPlan getSelectPlan() {
        return selectPlan;
    }

    /**
     * Gets the plan for the GENERATE clause.
     *
     * @return -
     */
    public GeneratePlan getGeneratePlan() {
        return generatePlan;
    }

    /**
     * Gets it the query generate template is specified by a URI.
     *
     * @return -
     */
    public boolean isDistant() {
        return distant;
    }

    /**
     * Constructor
     *
     * @param iteratorAndSourcePlans
     * @param selectPlan
     * @param generatePlan
     * @param prefixMapping
     * @param distant
     */
    public RootPlanImpl(
            final List<IteratorOrSourcePlan> iteratorAndSourcePlans,
            final SelectPlan selectPlan,
            final GeneratePlan generatePlan,
            final PrefixMapping prefixMapping,
            final boolean distant) {
        Objects.requireNonNull(iteratorAndSourcePlans, "iterator and source"
                + " plans may be empty, but not null.");
        this.iteratorAndSourcePlans = iteratorAndSourcePlans;
        this.selectPlan = selectPlan;
        this.generatePlan = generatePlan;
        this.prefixMapping = prefixMapping;
        this.distant = distant;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Model exec() {
        Dataset inputDataset = DatasetFactory.create();
        QuerySolution initialBindings = new QuerySolutionMap();
        Model initialModel = ModelFactory.createDefaultModel();
        exec(inputDataset, initialBindings, initialModel);
        return initialModel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Model exec(final Model inputModel) {
        Objects.requireNonNull(inputModel, "inputModel must not be null.");
        Dataset inputDataset = DatasetFactory.create(inputModel);
        QuerySolution initialBindings = new QuerySolutionMap();
        Model initialModel = ModelFactory.createDefaultModel();
        exec(inputDataset, initialBindings, initialModel);
        return initialModel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Model exec(final Dataset inputDataset) {
        Objects.requireNonNull(inputDataset, "inputDataset must not be null.");
        QuerySolution initialBindings = new QuerySolutionMap();
        Model initialModel = ModelFactory.createDefaultModel();
        exec(inputDataset, initialBindings, initialModel);
        return initialModel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void exec(
            final QuerySolution initialBindings,
            final Model initialModel) {
        Objects.requireNonNull(initialModel, "initialModel must not be null.");
        Dataset inputDataset = DatasetFactory.create();
        exec(inputDataset, initialBindings, initialModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void exec(
            final Model inputModel,
            final Model initialModel) {
        Objects.requireNonNull(inputModel, "inputModel must not be null.");
        Objects.requireNonNull(initialModel, "initialModel must not be null.");
        Dataset inputDataset = DatasetFactory.create(inputModel);
        QuerySolution initialBindings = new QuerySolutionMap();
        exec(inputDataset, initialBindings, initialModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void exec(
            final Dataset inputDataset,
            final Model initialModel) {
        Objects.requireNonNull(inputDataset, "inputDataset must not be null.");
        Objects.requireNonNull(initialModel, "initialModel must not be null.");
        QuerySolution initialBindings = new QuerySolutionMap();
        exec(inputDataset, initialBindings, initialModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void exec(
            final Model inputModel,
            final QuerySolution initialBindings,
            final Model initialModel) {
        Objects.requireNonNull(inputModel, "inputModel must not be null.");
        Objects.requireNonNull(initialModel, "initialModel must not be null.");
        Dataset inputDataset = DatasetFactory.create(inputModel);
        exec(inputDataset, initialBindings, initialModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void exec(
            final Dataset inputDataset,
            final QuerySolution initialBindings,
            final Model initialModel) {
        BNodeMap bNodeMap = new BNodeMap();
        exec(inputDataset, initialBindings, initialModel, bNodeMap);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void exec(
            final Dataset inputDataset,
            final QuerySolution initialBindings,
            final Model initialModel,
            final BNodeMap bNodeMap) {
        Objects.requireNonNull(inputDataset, "inputDataset must not be null.");
        Objects.requireNonNull(initialModel, "initialModel must not be null.");
        Objects.requireNonNull(bNodeMap, "bNodeMap must not be null.");
        final List<BindingHashMapOverwrite> values;
        final List<Var> variables;
        if (initialBindings == null) {
            values = new ArrayList<>();
            variables = new ArrayList<>();
        } else {
            final BindingHashMapOverwrite binding
                    = new BindingHashMapOverwrite(initialBindings);
            values = Lists.newArrayList(binding);
            variables = binding.varsList();
        }
        exec(inputDataset, initialModel, variables, values, bNodeMap);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void exec(
            final Dataset inputDataset,
            final Model initialModel,
            final List<Var> variables,
            final List<BindingHashMapOverwrite> values,
            final BNodeMap bNodeMap) {

        for (String prefix : prefixMapping.getNsPrefixMap().keySet()) {
            initialModel.setNsPrefix(
                    prefix, prefixMapping.getNsPrefixURI(prefix));
        }

        // augments the variables and values.
        for (IteratorOrSourcePlan plan : iteratorAndSourcePlans) {
            plan.exec(variables, values);
        }
        if (selectPlan != null) {
            selectPlan.exec(inputDataset, variables, values);
        }
        if (generatePlan != null) {
            if (distant) {
                BNodeMap bNodeMap2 = new BNodeMap();
                generatePlan.exec(inputDataset, initialModel, variables, values,
                        bNodeMap2);
            } else {
                generatePlan.exec(inputDataset, initialModel, variables, values,
                        bNodeMap);
            }
        }
    }
}
