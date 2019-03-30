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

import com.github.thesmartenergy.sparql.generate.jena.engine.RootPlanBase;
import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerateContext;
import com.github.thesmartenergy.sparql.generate.jena.engine.GeneratePlan;
import com.github.thesmartenergy.sparql.generate.jena.engine.IteratorPlan;
import java.util.List;
import org.apache.jena.query.Dataset;
import com.github.thesmartenergy.sparql.generate.jena.engine.SelectPlan;
import com.github.thesmartenergy.sparql.generate.jena.query.SPARQLGenerateQuery;
import com.github.thesmartenergy.sparql.generate.jena.syntax.Param;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.apache.jena.riot.system.StreamRDF;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.github.thesmartenergy.sparql.generate.jena.engine.BindOrSourcePlan;
import com.github.thesmartenergy.sparql.generate.jena.engine.IteratorOrSourceOrBindPlan;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Entry point to a SPARQL-Generate query execution.
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public final class RootPlanImpl extends RootPlanBase {

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(RootPlanImpl.class);

    /**
     * query.
     */
    private final SPARQLGenerateQuery query;

    /**
     * Selector and Source plans.
     */
    private final List<IteratorOrSourceOrBindPlan> iteratorAndSourcePlans;

    /**
     * The plan for the SPARQL SELECT.
     */
    private final SelectPlan selectPlan;

    /**
     * The plan for the GENERATE clause.
     */
    private final GeneratePlan generatePlan;

    /**
     * true if the query is not a sub-query.
     */
    private final boolean initial;

    public List<Param> getQuerySignature() {
        return query.getQuerySignature();
    }

    /**
     * Get the plans for the ITERATOR and SOURCE clauses.
     *
     * @return the plans.
     */
    public List<IteratorOrSourceOrBindPlan> getIteratorAndSourcePlans() {
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
     * Constructor
     *
     * @param query
     * @param iteratorAndSourcePlans
     * @param selectPlan
     * @param generatePlan
     * @param initial
     */
    public RootPlanImpl(
            final SPARQLGenerateQuery query,
            final List<IteratorOrSourceOrBindPlan> iteratorAndSourcePlans,
            final SelectPlan selectPlan,
            final GeneratePlan generatePlan,
            final boolean initial) {
        Objects.requireNonNull(iteratorAndSourcePlans, "iterator and source"
                + " plans may be empty, but not null.");
        this.query = query;
        this.iteratorAndSourcePlans = iteratorAndSourcePlans;
        this.selectPlan = selectPlan;
        this.generatePlan = generatePlan;
        this.initial = initial;
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
    public final CompletableFuture<Void> exec(
            final Dataset inputDataset,
            final BindingHashMapOverwrite binding,
            final StreamRDF outputStream,
            final BNodeMap bNodeMap,
            final SPARQLGenerateContext context) {
        Objects.requireNonNull(inputDataset, "inputDataset must not be null.");
        Objects.requireNonNull(binding, "binding must not be null.");
        Objects.requireNonNull(outputStream, "outputStream must not be null.");
        Objects.requireNonNull(bNodeMap, "bNodeMap must not be null.");
        Objects.requireNonNull(context, "context must not be null.");
        if (initial) {
            start(outputStream, binding, context);
        }
        final Collection<CompletableFuture<BindingHashMapOverwrite>> firstValues = Collections.singleton(CompletableFuture.completedFuture(binding));
        CompletableFuture<Void> f = CompletableFuture.completedFuture(null)
                .thenComposeAsync(
                        (n) -> execPlans(inputDataset, outputStream, firstValues, bNodeMap, 0, context),
                        context.getExecutor());
        if (initial) {
            return finish(f, outputStream, context);
        } else {
            return f;
        }
    }

    private void start(
            final StreamRDF outputStream,
            final BindingHashMapOverwrite binding,
            final SPARQLGenerateContext context) {
        context.getExecutor().execute(() -> {
            LOG.info("Starting transformation");
            outputStream.start();
            if (query.getQueryName() != null) {
                if (!query.getQueryName().isURI()) {
                    throw new UnsupportedOperationException("not implemented yet");
                }
                String name = query.getQueryName().getURI();
                context.getLoadedQueries().put(name, query);
                context.getLoadedPlans().put(name, this);
                context.registerExecution(name, binding);
            }
            for (String prefix : query.getPrefixMapping().getNsPrefixMap().keySet()) {
                outputStream.prefix(prefix, query.getPrefixMapping().getNsPrefixURI(prefix));
            }
        });
    }

    private CompletableFuture<Void> finish(
            final CompletableFuture<Void> future,
            final StreamRDF outputStream,
            final SPARQLGenerateContext context) {
        return future.thenRunAsync(() -> {
            outputStream.finish();
            try {
                context.close();
            } catch (Exception ex) {
                LOG.warn("Exception while closing context:", ex);
            }
            LOG.info("End of transformation");
        }, context.getExecutor());
    }

    /**
     *
     * @param currentFuture the future that will trigger this execution
     * @param inputDataset
     * @param outputStream
     * @param futureValues the batch
     * @param bNodeMap
     * @param i the index of the next iterator, source, or bind plan to execute
     * @param context
     * @return
     */
    private CompletableFuture<Void> execPlans(
            final Dataset inputDataset,
            final StreamRDF outputStream,
            final Collection<CompletableFuture<BindingHashMapOverwrite>> futureValues,
            final BNodeMap bNodeMap,
            final int i,
            final SPARQLGenerateContext context) {
        if (i < iteratorAndSourcePlans.size()) {
            final IteratorOrSourceOrBindPlan plan = iteratorAndSourcePlans.get(i);
            if (plan instanceof BindOrSourcePlan) {
                return execBindOrSourcePlan((BindOrSourcePlan) plan, inputDataset, outputStream, futureValues, bNodeMap, i, context);
            } else {
                return execIteratorPlan((IteratorPlan) plan, inputDataset, outputStream, futureValues, bNodeMap, i, context);
            }
        } else {
            final CompletableFuture<List<BindingHashMapOverwrite>> f;
            if (selectPlan != null) {
                f = execSelectPlan(inputDataset, futureValues, context);
            } else {
                final List<BindingHashMapOverwrite> values = new ArrayList<>(futureValues.size());
                final List<CompletableFuture<Void>> fs = futureValues
                        .stream()
                        .map(futureBinding -> futureBinding.thenAcceptAsync(values::add, context.getExecutor()))
                        .collect(Collectors.toList());
                f = allOf(fs).thenApplyAsync(n -> values, context.getExecutor());
            }
            if (generatePlan != null) {
                return f.thenComposeAsync((n) -> execGeneratePlan(f, inputDataset, outputStream, bNodeMap, context), context.getExecutor());
            } else {
                return f.thenRunAsync(() -> System.out.println("noooo"), context.getExecutor());
            }
        }
    }

    private CompletableFuture<Void> execBindOrSourcePlan(
            final BindOrSourcePlan bindOrSourcePlan,
            final Dataset inputDataset,
            final StreamRDF outputStream,
            final Collection<CompletableFuture<BindingHashMapOverwrite>> futureValues,
            final BNodeMap bNodeMap,
            final int i,
            final SPARQLGenerateContext context) {
        final Set<CompletableFuture<BindingHashMapOverwrite>> newFutureValues
                = futureValues
                .stream()
                .map((f) -> f.thenApplyAsync(
                        (binding) -> bindOrSourcePlan.exec(binding, context), context.getExecutor()))
                .collect(Collectors.toSet());
        return execPlans(inputDataset, outputStream, newFutureValues, bNodeMap, i + 1, context).thenRunAsync(() -> {
            LOG.debug("finished source plan");
        }, context.getExecutor());
    }

    private CompletableFuture<Void> execIteratorPlan(
            final IteratorPlan iteratorPlan,
            final Dataset inputDataset,
            final StreamRDF outputStream,
            final Collection<CompletableFuture<BindingHashMapOverwrite>> futureValues,
            final BNodeMap bNodeMap,
            final int i,
            final SPARQLGenerateContext context) {
        return allOf(futureValues.stream().map(
                (futureBinding) -> futureBinding.thenComposeAsync(
                        (binding) -> iteratorPlan.exec(binding, context,
                                (newValues) -> {
                                    return execPlans(
                                            inputDataset, outputStream,
                                            newValues.stream()
                                            .map((b) -> {
                                                LOG.debug("add now " + b);
                                                return CompletableFuture.completedFuture(b);
                                            })
                                            .collect(Collectors.toSet()),
                                            bNodeMap, i + 1, context).thenRunAsync(() -> {
                                        LOG.debug("some iterator batch finished " + iteratorPlan);
                                    }, context.getExecutor());
                                }), context.getExecutor()))
                .collect(Collectors.toSet()))
                .thenRunAsync(() -> {
                    LOG.debug("finished iterator plan " + iteratorPlan);
                }, context.getExecutor());
    }

    private CompletableFuture<List<BindingHashMapOverwrite>> execSelectPlan(
            final Dataset inputDataset,
            final Collection<CompletableFuture<BindingHashMapOverwrite>> futureValues,
            final SPARQLGenerateContext context) {
        Collection<BindingHashMapOverwrite> values = new HashSet<>(futureValues.size());
        Collection<CompletableFuture<Void>> fs = new HashSet<>(futureValues.size());
        futureValues.forEach((futureBinding) -> {
            fs.add(futureBinding.thenAccept(b->values.add(b)));
        });
        return allOf(fs).thenApplyAsync((n) -> {
            LOG.debug("Executing plan for select " + futureValues.size() + " = " + values.size());
            LOG.trace("bindings are " + values);
            List<BindingHashMapOverwrite> newBindings = selectPlan.exec(inputDataset, values, context);
            return newBindings;
        }, context.getExecutor()).thenApplyAsync((r) -> {
            LOG.debug("some select finished with number of bindings is " + r.size());
            LOG.trace("bindings are " + r);
            return r;
        }, context.getExecutor()).exceptionally((ex) -> {
            LOG.warn("Error while executing the SELECT query ", ex);
            return null;
        });
    }

    private CompletableFuture<Void> execGeneratePlan(
            final CompletableFuture<List<BindingHashMapOverwrite>> f,
            final Dataset inputDataset,
            final StreamRDF outputStream,
            final BNodeMap bNodeMap,
            final SPARQLGenerateContext context) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        return f.thenAcceptAsync((values) -> {
            SPARQLGenerateContext forkedContext = context.forkContext(values.size());
            for (int i = 0; i < values.size(); i++) {
                futures.add(generatePlan.exec(inputDataset, values.get(i), outputStream, bNodeMap, forkedContext, i));
            }
        }, context.getExecutor())
                .thenComposeAsync((n) -> {
                    return allOf(futures).thenRunAsync(() -> LOG.trace("finished all generatePlan " + futures.size(), context.getExecutor()));
                }, context.getExecutor())
                .exceptionally((ex) -> {
                    LOG.warn("Exception", ex);
                    return null;
                });
    }

    private <T> CompletableFuture<Void> allOf(Collection<CompletableFuture<T>> futures) {
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
    }

}
