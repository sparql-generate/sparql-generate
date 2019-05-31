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
package fr.emse.ci.sparqlext.generate.engine;

import fr.emse.ci.sparqlext.SPARQLExt;
import fr.emse.ci.sparqlext.SPARQLExtException;
import java.util.List;
import org.apache.jena.query.Dataset;
import fr.emse.ci.sparqlext.query.SPARQLExtQuery;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.apache.jena.riot.system.StreamRDF;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.engine.binding.BindingMap;
import org.apache.jena.sparql.util.Context;

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
    private final SPARQLExtQuery query;

    /**
     * pm.
     */
    private final PrefixMapping pm;

    /**
     * Selector and Source plans.
     */
    private final List<BindingsClausePlan> iteratorAndSourcePlans;

    /**
     * The plan for the SPARQL SELECT.
     */
    public final SelectPlan selectPlan;

    /**
     * The plan for the GENERATE or TEMPLATE clause.
     */
    private final ExecutionPlan outputPlan;

    /**
     * true if the query is not a sub-query.
     */
    private final boolean initial;

    public List<Var> getQuerySignature() {
        return query.getSignature();
    }

    /**
     * Get the plans for the ITERATOR and SOURCE clauses.
     *
     * @return the plans.
     */
    public List<BindingsClausePlan> getIteratorAndSourcePlans() {
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
    public ExecutionPlan getGeneratePlan() {
        return outputPlan;
    }

    @Override
    public PrefixMapping getPrefixMapping() {
        return pm;
    }

    /**
     * Constructor
     *
     * @param query
     * @param iteratorAndSourcePlans
     * @param selectPlan
     * @param outputPlan
     * @param initial
     */
    public RootPlanImpl(
            final SPARQLExtQuery query,
            final List<BindingsClausePlan> iteratorAndSourcePlans,
            final SelectPlan selectPlan,
            final ExecutionPlan outputPlan,
            final boolean initial) {
        Objects.requireNonNull(iteratorAndSourcePlans, "iterator and source"
                + " plans may be empty, but not null.");
        this.query = query;
        this.pm = query.getPrefixMapping();
        this.iteratorAndSourcePlans = iteratorAndSourcePlans;
        this.selectPlan = selectPlan;
        this.outputPlan = outputPlan;
        this.initial = initial;
    }

    @Override
    public final CompletableFuture<Void> exec(
            final Dataset inputDataset,
            final List<Var> variables,
            final List<Binding> values,
            final BNodeMap bNodeMap,
            final Context context,
            final StreamRDF outputGenerate,
            final Consumer<ResultSet> outputSelect,
            final Consumer<String> outputTemplate) {
        Objects.requireNonNull(inputDataset, "inputDataset must not be null.");
        Objects.requireNonNull(variables, "variables must not be null.");
        Objects.requireNonNull(values, "values must not be null.");
        Objects.requireNonNull(bNodeMap, "bNodeMap must not be null.");
        Objects.requireNonNull(context, "context must not be null.");
        final Context newContext = SPARQLExt.createContext(context);
        newContext.set(SPARQLExt.DATASET, inputDataset);
        final Executor executor = (Executor) newContext.get(SPARQLExt.EXECUTOR);
        start(outputGenerate, values, newContext, executor);
        final List<CompletableFuture<Binding>> firstValues
                = values.stream().map((b) -> CompletableFuture.completedFuture(b)).collect(Collectors.toList());
        final CompletableFuture<Void> f = execPlans(inputDataset, variables, firstValues, bNodeMap, 0, newContext, outputGenerate, outputSelect, outputTemplate)
                .whenCompleteAsync((n, t) -> {
                    if (initial) {
                        SPARQLExt.close(newContext);
                    }
                });
        return finish(f, outputGenerate, executor);
    }

    private void start(
            final StreamRDF outputStream,
            final List<Binding> values,
            final Context context,
            final Executor executor) {
        if (!initial) {
            return;
        }
        executor.execute(() -> {
            LOG.info("Starting transformation");
            if (outputStream != null) {
                outputStream.start();
            }
            if (query.getName() != null) {
                if (!query.getName().isURI()) {
                    throw new UnsupportedOperationException("not implemented yet");
                }
                String name = query.getName().getURI();
                final Map<String, RootPlan> loadedPlans = (Map<String, RootPlan>) context.get(SPARQLExt.LOADED_PLANS);
                loadedPlans.put(name, this);
                final Map<String, SPARQLExtQuery> loadedQueries = (Map<String, SPARQLExtQuery>) context.get(SPARQLExt.LOADED_QUERIES);
                loadedQueries.put(name, query);
                SPARQLExt.registerExecution(context, name, values);
            }
            if (outputStream != null) {
                for (String prefix : query.getPrefixMapping().getNsPrefixMap().keySet()) {
                    outputStream.prefix(prefix, query.getPrefixMapping().getNsPrefixURI(prefix));
                }
                if(query.getBaseURI()!=null) {
                    outputStream.base(query.getBaseURI());
                }
            }
        });
    }

    private CompletableFuture<Void> finish(
            final CompletableFuture<Void> future,
            final StreamRDF outputStream,
            final Executor executor) {
        if (initial) {
            return future.thenRunAsync(() -> {
                if (outputStream != null) {
                    outputStream.finish();
                }
                LOG.info("End of transformation");
            }, executor);
        } else {
            return future;
        }
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
            final List<Var> variables,
            final List<CompletableFuture<Binding>> futureValues,
            final BNodeMap bNodeMap,
            final int i,
            final Context context,
            final StreamRDF outputGenerate,
            final Consumer<ResultSet> outputSelect,
            final Consumer<String> outputTemplate) {
        if (Thread.interrupted()) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(new InterruptedException());
            return future;
        }
        final Executor executor = (Executor) context.get(SPARQLExt.EXECUTOR);
        if (i < iteratorAndSourcePlans.size()) {
            final BindingsClausePlan plan = iteratorAndSourcePlans.get(i);
            if (plan instanceof BindOrSourcePlan) {
                final BindOrSourcePlan bindOrSourcePlan = (BindOrSourcePlan) plan;
                final List<Var> newVariables = new ArrayList<>(variables);
                newVariables.add(bindOrSourcePlan.getVar());
                final List<CompletableFuture<Binding>> newFutureValues
                        = bindOrSourcePlan.exec(futureValues, context, executor);
                return execPlans(inputDataset, newVariables, newFutureValues, bNodeMap, i + 1, context, outputGenerate, outputSelect, outputTemplate)
                        .thenRunAsync(() -> {
                            LOG.debug("Finished source or bind plan " + bindOrSourcePlan);
                        }, executor);
            } else {
                IteratorPlan iteratorPlan = (IteratorPlan) plan;
                return iteratorPlan.exec(variables, futureValues, (newValues) -> {
                    final List<Var> newVariables = new ArrayList<>(variables);
                    newVariables.addAll(iteratorPlan.getVars());
                    final List<CompletableFuture<Binding>> newFutureValues
                            = newValues.stream().map((b) -> CompletableFuture.completedFuture(b)).collect(Collectors.toList());
                    return execPlans(inputDataset, newVariables, newFutureValues, bNodeMap, i + 1, context, outputGenerate, outputSelect, outputTemplate)
                            .thenRunAsync(() -> {
                                LOG.debug("Iterator plan produced batch " + iteratorPlan);
                            }, executor);
                }, context, executor).thenRunAsync(() -> {
                    LOG.debug("Finished iterator plan " + iteratorPlan);
                }, executor);
            }
        } else {
            final List<Var> newVariables = selectPlan.getVars();
            final CompletableFuture<ResultSet> futureResultSet
                    = selectPlan.exec(inputDataset, variables, futureValues, context, executor);
            if (outputSelect != null) {
                return futureResultSet.thenAcceptAsync(outputSelect::accept, executor);
            } else if (outputTemplate != null) {
                return futureResultSet.thenAcceptAsync((results) -> execTemplatePlan(results, outputTemplate, context), executor);
            } else if (outputGenerate != null) {
                final CompletableFuture<List<Binding>> f;
                f = futureResultSet.thenApplyAsync((results) -> {
                    final List<Binding> newBindings = new ArrayList<>();
                    while (results.hasNext()) {
                        final QuerySolution sol = results.next();

                        final BindingMap p = BindingFactory.create();
                        for (Iterator<String> it = sol.varNames(); it.hasNext();) {
                            final String varName = it.next();
                            p.add(SPARQLExt.allocVar(varName, context), sol.get(varName).asNode());
                        }
                        newBindings.add(p);
                    }
                    return newBindings;
                }, executor);
                return f.thenComposeAsync((n) -> execGeneratePlan(inputDataset, newVariables, f, outputGenerate, bNodeMap, context), executor);
            } else {
                throw new SPARQLExtException("One of the outputs should not be null!");
            }
        }
    }

    private CompletableFuture<Void> execGeneratePlan(
            final Dataset inputDataset,
            final List<Var> variables,
            final CompletableFuture<List<Binding>> f,
            final StreamRDF outputGenerate,
            final BNodeMap bNodeMap,
            final Context context) {
        if (Thread.interrupted()) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(new InterruptedException());
            return future;
        }
        final Executor executor = (Executor) context.get(SPARQLExt.EXECUTOR);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        return f.thenAcceptAsync((values) -> {
            final Context forkedContext = SPARQLExt.forkContext(context, values.size());
            futures.add(outputPlan.exec(inputDataset, variables, values, bNodeMap, forkedContext, outputGenerate, null, null));
        }, executor)
                .thenComposeAsync((n) -> {
                    return allOf(futures).thenRunAsync(() -> LOG.trace("Finished all generatePlan " + futures.size(), executor), executor);
                }, executor)
                .exceptionally((ex) -> {
                    LOG.warn("Exception", ex);
                    return null;
                });
    }

    private void execTemplatePlan(ResultSet results, Consumer<String> outputTemplate, Context context) {
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        String control = SPARQLExt.getIndentControl(context);
        while (results.hasNext()) {
            if (!first && query.hasTemplateClauseSeparator()) {
                sb.append(query.getTemplateClauseSeparator());
            }
            QuerySolution sol = results.next();
            if (!sol.contains("out")) {
                LOG.debug("Variable ?out not bounded");
                continue;
            }
            if (!sol.get("out").isLiteral()) {
                LOG.debug("Variable ?out is not a literal. Got " + sol.get("out"));
                continue;
            }
            String out = sol.getLiteral("out").getLexicalForm();
            StringBuffer buf = new StringBuffer(out);
            int cursor = 0;
            while (cursor < buf.length()) {
                if (cursor + 5 <= buf.length() && buf.substring(cursor, cursor + 5).equals(control)) {
                    buf.delete(cursor, cursor + 5);
                    SPARQLExt.updateIndent(context, buf.substring(cursor, cursor + 2));
                    buf.delete(cursor, cursor + 2);
                } else if (buf.substring(cursor, cursor + 1).equals("\n")) {
                    cursor++;
                    int indent = SPARQLExt.getIndent(context);
                    for (int i = 0; i < indent; i++) {
                        buf.insert(cursor, " ");
                        cursor++;
                    }
                } else {
                    cursor++;
                }
            }
            sb.append(buf);
            first = false;
        }
        outputTemplate.accept(sb.toString());
    }

    private <T> CompletableFuture<Void> allOf(Collection<CompletableFuture<T>> futures) {
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
    }

}
