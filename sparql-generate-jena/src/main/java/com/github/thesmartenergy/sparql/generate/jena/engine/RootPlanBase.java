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
package com.github.thesmartenergy.sparql.generate.jena.engine;

import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerateContext;
import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerateException;
import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerateRootContext;
import com.github.thesmartenergy.sparql.generate.jena.engine.impl.BNodeMap;
import com.github.thesmartenergy.sparql.generate.jena.engine.impl.BindingHashMapOverwrite;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.system.StreamRDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author maxime.lefrancois
 */
public abstract class RootPlanBase implements RootPlan {

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(RootPlanBase.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public final Model exec() {
        final Dataset inputDataset = DatasetFactory.create();
        final QuerySolution initialBindings = new QuerySolutionMap();
        final Model initialModel = ModelFactory.createDefaultModel();
        final SPARQLGenerateContext context = new SPARQLGenerateRootContext();
        exec(inputDataset, initialBindings, initialModel, context);
        return initialModel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Model exec(final SPARQLGenerateContext context) {
        final Dataset inputDataset = DatasetFactory.create();
        final QuerySolution initialBindings = new QuerySolutionMap();
        final Model initialModel = ModelFactory.createDefaultModel();
        exec(inputDataset, initialBindings, initialModel, context);
        return initialModel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Model exec(final Model inputModel) {
        final Dataset inputDataset = DatasetFactory.create(inputModel);
        final QuerySolution initialBindings = new QuerySolutionMap();
        final Model initialModel = ModelFactory.createDefaultModel();
        final SPARQLGenerateContext context = new SPARQLGenerateRootContext();
        exec(inputDataset, initialBindings, initialModel, context);
        return initialModel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Model exec(final Model inputModel, final SPARQLGenerateContext context) {
        final Dataset inputDataset = DatasetFactory.create(inputModel);
        final QuerySolution initialBindings = new QuerySolutionMap();
        final Model initialModel = ModelFactory.createDefaultModel();
        exec(inputDataset, initialBindings, initialModel, context);
        return initialModel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Model exec(final Dataset inputDataset) {
        final QuerySolution initialBindings = new QuerySolutionMap();
        final Model initialModel = ModelFactory.createDefaultModel();
        final SPARQLGenerateContext context = new SPARQLGenerateRootContext();
        exec(inputDataset, initialBindings, initialModel, context);
        return initialModel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Model exec(final Dataset inputDataset, final SPARQLGenerateContext context) {
        final QuerySolution initialBindings = new QuerySolutionMap();
        final Model initialModel = ModelFactory.createDefaultModel();
        exec(inputDataset, initialBindings, initialModel, context);
        return initialModel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void exec(
            final QuerySolution initialBindings,
            final Model initialModel) {
        final Dataset inputDataset = DatasetFactory.create();
        final SPARQLGenerateContext context = new SPARQLGenerateRootContext();
        exec(inputDataset, initialBindings, initialModel, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void exec(
            final QuerySolution initialBindings,
            final Model initialModel,
            final SPARQLGenerateContext context) {
        final Dataset inputDataset = DatasetFactory.create();
        exec(inputDataset, initialBindings, initialModel, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void exec(
            final Model inputModel,
            final Model initialModel) {
        final Dataset inputDataset = DatasetFactory.create(inputModel);
        final QuerySolution initialBindings = new QuerySolutionMap();
        final SPARQLGenerateContext context = new SPARQLGenerateRootContext();
        exec(inputDataset, initialBindings, initialModel, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void exec(
            final Model inputModel,
            final Model initialModel,
            final SPARQLGenerateContext context) {
        final Dataset inputDataset = DatasetFactory.create(inputModel);
        final QuerySolution initialBindings = new QuerySolutionMap();
        exec(inputDataset, initialBindings, initialModel, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void exec(
            final Dataset inputDataset,
            final Model initialModel) {
        final QuerySolution initialBindings = new QuerySolutionMap();
        final SPARQLGenerateContext context = new SPARQLGenerateRootContext();
        exec(inputDataset, initialBindings, initialModel, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void exec(
            final Dataset inputDataset,
            final Model initialModel,
            final SPARQLGenerateContext context) {
        final QuerySolution initialBindings = new QuerySolutionMap();
        exec(inputDataset, initialBindings, initialModel, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void exec(
            final Model inputModel,
            final QuerySolution initialBindings,
            final Model initialModel) {
        final Dataset inputDataset = DatasetFactory.create(inputModel);
        final SPARQLGenerateContext context = new SPARQLGenerateRootContext();
        exec(inputDataset, initialBindings, initialModel, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void exec(
            final Model inputModel,
            final QuerySolution initialBindings,
            final Model initialModel,
            final SPARQLGenerateContext context) {
        final Dataset inputDataset = DatasetFactory.create(inputModel);
        exec(inputDataset, initialBindings, initialModel, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void exec(
            final Dataset inputDataset,
            final QuerySolution initialBindings,
            final Model initialModel) {
        final SPARQLGenerateContext context = new SPARQLGenerateRootContext();
        exec(inputDataset, initialBindings, initialModel, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void exec(
            final Dataset inputDataset,
            final QuerySolution initialBindings,
            final Model initialModel,
            final SPARQLGenerateContext context) {
        final StreamRDF outputStream = new StreamRDFModel(initialModel);
        final CompletableFuture<Void> future = exec(inputDataset, initialBindings, outputStream, context);
        try {
            future.get();
        } catch (InterruptedException | ExecutionException ex) {
            LOG.error("Error while executing the SPARQL-Generate query", ex);
            throw new SPARQLGenerateException(ex);
        }
    }

    @Override
    public final CompletableFuture<Void> exec(final Model inputModel, final StreamRDF outputStream) {
        final Dataset inputDataset = DatasetFactory.create(inputModel);
        final QuerySolution initialBindings = new QuerySolutionMap();
        final BNodeMap bNodeMap = new BNodeMap();
        final SPARQLGenerateContext context = new SPARQLGenerateRootContext();
        final BindingHashMapOverwrite binding = new BindingHashMapOverwrite(initialBindings, context);
        return exec(inputDataset, binding, outputStream, bNodeMap, context);
    }

    @Override
    public final CompletableFuture<Void> exec(final Model inputModel, final StreamRDF outputStream, final SPARQLGenerateContext context) {
        final Dataset inputDataset = DatasetFactory.create(inputModel);
        final QuerySolution initialBindings = new QuerySolutionMap();
        final BNodeMap bNodeMap = new BNodeMap();
        final BindingHashMapOverwrite binding = new BindingHashMapOverwrite(initialBindings, context);
        return exec(inputDataset, binding, outputStream, bNodeMap, context);
    }

    @Override
    public final CompletableFuture<Void> exec(final StreamRDF outputStream) {
        final Dataset inputDataset = DatasetFactory.create();
        final QuerySolution initialBindings = new QuerySolutionMap();
        final BNodeMap bNodeMap = new BNodeMap();
        final SPARQLGenerateContext context = new SPARQLGenerateRootContext();
        final BindingHashMapOverwrite binding = new BindingHashMapOverwrite(initialBindings, context);
        return exec(inputDataset, binding, outputStream, bNodeMap, context);
    }

    @Override
    public final CompletableFuture<Void> exec(final StreamRDF outputStream, final SPARQLGenerateContext context) {
        final Dataset inputDataset = DatasetFactory.create();
        final QuerySolution initialBindings = new QuerySolutionMap();
        final BNodeMap bNodeMap = new BNodeMap();
        final BindingHashMapOverwrite binding = new BindingHashMapOverwrite(initialBindings, context);
        return exec(inputDataset, binding, outputStream, bNodeMap, context);
    }

    @Override
    public final CompletableFuture<Void> exec(final Dataset inputDataset, final StreamRDF outputStream) {
        final QuerySolution initialBindings = new QuerySolutionMap();
        final BNodeMap bNodeMap = new BNodeMap();
        final SPARQLGenerateContext context = new SPARQLGenerateRootContext();
        final BindingHashMapOverwrite binding = new BindingHashMapOverwrite(initialBindings, context);
        return exec(inputDataset, binding, outputStream, bNodeMap, context);
    }

    @Override
    public final CompletableFuture<Void> exec(final Dataset inputDataset, final StreamRDF outputStream, final SPARQLGenerateContext context) {
        final QuerySolution initialBindings = new QuerySolutionMap();
        final BNodeMap bNodeMap = new BNodeMap();
        final BindingHashMapOverwrite binding = new BindingHashMapOverwrite(initialBindings, context);
        return exec(inputDataset, binding, outputStream, bNodeMap, context);
    }

    @Override
    public final CompletableFuture<Void> exec(final QuerySolution initialBindings, final StreamRDF outputStream) {
        final Dataset inputDataset = DatasetFactory.create();
        final BNodeMap bNodeMap = new BNodeMap();
        final SPARQLGenerateContext context = new SPARQLGenerateRootContext();
        final BindingHashMapOverwrite binding = new BindingHashMapOverwrite(initialBindings, context);
        return exec(inputDataset, binding, outputStream, bNodeMap, context);
    }

    @Override
    public final CompletableFuture<Void> exec(final QuerySolution initialBindings, final StreamRDF outputStream, final SPARQLGenerateContext context) {
        final Dataset inputDataset = DatasetFactory.create();
        final BNodeMap bNodeMap = new BNodeMap();
        final BindingHashMapOverwrite binding = new BindingHashMapOverwrite(initialBindings, context);
        return exec(inputDataset, binding, outputStream, bNodeMap, context);
    }

    @Override
    public final CompletableFuture<Void> exec(final Model inputModel, final QuerySolution initialBindings, final StreamRDF outputStream) {
        final Dataset inputDataset = DatasetFactory.create(inputModel);
        final BNodeMap bNodeMap = new BNodeMap();
        final SPARQLGenerateContext context = new SPARQLGenerateRootContext();
        final BindingHashMapOverwrite binding = new BindingHashMapOverwrite(initialBindings, context);
        return exec(inputDataset, binding, outputStream, bNodeMap, context);
    }

    @Override
    public final CompletableFuture<Void> exec(final Model inputModel, final QuerySolution initialBindings, final StreamRDF outputStream, final SPARQLGenerateContext context) {
        final BNodeMap bNodeMap = new BNodeMap();
        final Dataset inputDataset = DatasetFactory.create(inputModel);
        final BindingHashMapOverwrite binding = new BindingHashMapOverwrite(initialBindings, context);
        return exec(inputDataset, binding, outputStream, bNodeMap, context);
    }

    @Override
    public final CompletableFuture<Void> exec(final Dataset inputDataset, final QuerySolution initialBindings, final StreamRDF outputStream) {
        final BNodeMap bNodeMap = new BNodeMap();
        final SPARQLGenerateContext context = new SPARQLGenerateRootContext();
        final BindingHashMapOverwrite binding = new BindingHashMapOverwrite(initialBindings, context);
        return exec(inputDataset, binding, outputStream, bNodeMap, context);
    }

    @Override
    public final CompletableFuture<Void> exec(final Dataset inputDataset, final QuerySolution initialBindings, final StreamRDF outputStream, final SPARQLGenerateContext context) {
        final BNodeMap bNodeMap = new BNodeMap();
        final BindingHashMapOverwrite binding = new BindingHashMapOverwrite(initialBindings, context);
        return exec(inputDataset, binding, outputStream, bNodeMap, context);
    }

}
