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
package fr.emse.ci.sparqlext.engine;

import fr.emse.ci.sparqlext.SPARQLExt;
import fr.emse.ci.sparqlext.SPARQLExtException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sparql.util.ResultSetUtils;
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
    public final Model execGenerate() {
        final Dataset inputDataset = DatasetFactory.create();
        final QuerySolution initialBinding = new QuerySolutionMap();
        final Model initialModel = ModelFactory.createDefaultModel();
        final Context context = SPARQLExt.createContext(getPrefixMapping());
        execGenerate(inputDataset, initialBinding, initialModel, context);
        return initialModel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Model execGenerate(
            final Context context) {
        final Dataset inputDataset = DatasetFactory.create();
        final QuerySolution initialBinding = new QuerySolutionMap();
        final Model initialModel = ModelFactory.createDefaultModel();
        execGenerate(inputDataset, initialBinding, initialModel, context);
        return initialModel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Model execGenerate(
            final Model inputModel) {
        final Dataset inputDataset = DatasetFactory.create(inputModel);
        final QuerySolution initialBinding = new QuerySolutionMap();
        final Model initialModel = ModelFactory.createDefaultModel();
        final Context context = SPARQLExt.createContext(getPrefixMapping());
        execGenerate(inputDataset, initialBinding, initialModel, context);
        return initialModel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Model execGenerate(
            final Model inputModel,
            final Context context) {
        final Dataset inputDataset = DatasetFactory.create(inputModel);
        final QuerySolution initialBinding = new QuerySolutionMap();
        final Model initialModel = ModelFactory.createDefaultModel();
        execGenerate(inputDataset, initialBinding, initialModel, context);
        return initialModel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Model execGenerate(
            final Model inputModel,
            final QuerySolution initialBinding,
            final Context context) {
        final Dataset inputDataset = DatasetFactory.create(inputModel);
        final Model initialModel = ModelFactory.createDefaultModel();
        execGenerate(inputDataset, initialBinding, initialModel, context);
        return initialModel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Model execGenerate(
            final Dataset inputDataset) {
        final QuerySolution initialBinding = new QuerySolutionMap();
        final Model initialModel = ModelFactory.createDefaultModel();
        final Context context = SPARQLExt.createContext(getPrefixMapping());
        execGenerate(inputDataset, initialBinding, initialModel, context);
        return initialModel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Model execGenerate(
            final Dataset inputDataset,
            final Context context) {
        final QuerySolution initialBinding = new QuerySolutionMap();
        final Model initialModel = ModelFactory.createDefaultModel();
        execGenerate(inputDataset, initialBinding, initialModel, context);
        return initialModel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Model execGenerate(
            final Dataset inputDataset,
            final QuerySolution initialBinding,
            final Context context) {
        final Model initialModel = ModelFactory.createDefaultModel();
        execGenerate(inputDataset, initialBinding, initialModel, context);
        return initialModel;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public final void execGenerate(
            final QuerySolution initialBinding,
            final Model initialModel) {
        final Dataset inputDataset = DatasetFactory.create();
        final Context context = SPARQLExt.createContext(getPrefixMapping());
        execGenerate(inputDataset, initialBinding, initialModel, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void execGenerate(
            final QuerySolution initialBinding,
            final Model initialModel,
            final Context context) {
        final Dataset inputDataset = DatasetFactory.create();
        execGenerate(inputDataset, initialBinding, initialModel, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void execGenerate(
            final Model inputModel,
            final Model initialModel) {
        final Dataset inputDataset = DatasetFactory.create(inputModel);
        final QuerySolution initialBinding = new QuerySolutionMap();
        final Context context = SPARQLExt.createContext(getPrefixMapping());
        execGenerate(inputDataset, initialBinding, initialModel, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void execGenerate(
            final Model inputModel,
            final Model initialModel,
            final Context context) {
        final Dataset inputDataset = DatasetFactory.create(inputModel);
        final QuerySolution initialBinding = new QuerySolutionMap();
        execGenerate(inputDataset, initialBinding, initialModel, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void execGenerate(
            final Dataset inputDataset,
            final Model initialModel) {
        final QuerySolution initialBinding = new QuerySolutionMap();
        final Context context = SPARQLExt.createContext(getPrefixMapping());
        execGenerate(inputDataset, initialBinding, initialModel, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void execGenerate(
            final Dataset inputDataset,
            final Model initialModel,
            final Context context) {
        final QuerySolution initialBinding = new QuerySolutionMap();
        execGenerate(inputDataset, initialBinding, initialModel, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void execGenerate(
            final Model inputModel,
            final QuerySolution initialBinding,
            final Model initialModel) {
        final Dataset inputDataset = DatasetFactory.create(inputModel);
        final Context context = SPARQLExt.createContext(getPrefixMapping());
        execGenerate(inputDataset, initialBinding, initialModel, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void execGenerate(
            final Model inputModel,
            final QuerySolution initialBinding,
            final Model initialModel,
            final Context context) {
        final Dataset inputDataset = DatasetFactory.create(inputModel);
        execGenerate(inputDataset, initialBinding, initialModel, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void execGenerate(
            final Dataset inputDataset,
            final QuerySolution initialBinding,
            final Model initialModel) {
        final Context context = SPARQLExt.createContext(getPrefixMapping());
        execGenerate(inputDataset, initialBinding, initialModel, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void execGenerate(
            final Dataset inputDataset,
            final QuerySolution initialBinding,
            final Model initialModel,
            final Context context) {
        final StreamRDF outputStream = new StreamRDFModel(initialModel);
        final CompletableFuture<Void> future = execGenerate(inputDataset, initialBinding, outputStream, context);
        try {
            future.get();
        } catch (InterruptedException ex) {
            LOG.error("Interrupted while executing the SPARQL-Generate query");
            throw new SPARQLExtException(ex);
        } catch (ExecutionException ex) {
            LOG.error("Error while executing the SPARQL-Generate query", ex);
            throw new SPARQLExtException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final CompletableFuture<Void> execGenerate(
            final Model inputModel,
            final StreamRDF outputStream) {
        final Dataset inputDataset = DatasetFactory.create(inputModel);
        final QuerySolution initialBinding = new QuerySolutionMap();
        final Context context = SPARQLExt.createContext(getPrefixMapping());
        return execGenerate(inputDataset, initialBinding, outputStream, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final CompletableFuture<Void> execGenerate(
            final Model inputModel,
            final StreamRDF outputStream,
            final Context context) {
        final Dataset inputDataset = DatasetFactory.create(inputModel);
        final QuerySolution initialBinding = new QuerySolutionMap();
        return execGenerate(inputDataset, initialBinding, outputStream, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final CompletableFuture<Void> execGenerate(
            final StreamRDF outputStream) {
        final Dataset inputDataset = DatasetFactory.create();
        final QuerySolution initialBinding = new QuerySolutionMap();
        final Context context = SPARQLExt.createContext(getPrefixMapping());
        return execGenerate(inputDataset, initialBinding, outputStream, context);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final CompletableFuture<Void> execGenerate(
            final StreamRDF outputStream,
            final Context context) {
        final Dataset inputDataset = DatasetFactory.create();
        final QuerySolution initialBinding = new QuerySolutionMap();
        return execGenerate(inputDataset, initialBinding, outputStream, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final CompletableFuture<Void> execGenerate(
            final Dataset inputDataset,
            final StreamRDF outputStream) {
        final QuerySolution initialBinding = new QuerySolutionMap();
        final Context context = SPARQLExt.createContext(getPrefixMapping());
        return execGenerate(inputDataset, initialBinding, outputStream, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final CompletableFuture<Void> execGenerate(
            final Dataset inputDataset,
            final StreamRDF outputStream,
            final Context context) {
        final QuerySolution initialBinding = new QuerySolutionMap();
        return execGenerate(inputDataset, initialBinding, outputStream, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final CompletableFuture<Void> execGenerate(
            final QuerySolution initialBinding,
            final StreamRDF outputStream) {
        final Dataset inputDataset = DatasetFactory.create();
        final Context context = SPARQLExt.createContext(getPrefixMapping());
        return execGenerate(inputDataset, initialBinding, outputStream, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final CompletableFuture<Void> execGenerate(
            final QuerySolution initialBinding,
            final StreamRDF outputStream,
            final Context context) {
        final Dataset inputDataset = DatasetFactory.create();
        return execGenerate(inputDataset, initialBinding, outputStream, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final CompletableFuture<Void> execGenerate(
            final Model inputModel,
            final QuerySolution initialBinding,
            final StreamRDF outputStream) {
        final Dataset inputDataset = DatasetFactory.create(inputModel);
        final Context context = SPARQLExt.createContext(getPrefixMapping());
        return execGenerate(inputDataset, initialBinding, outputStream, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final CompletableFuture<Void> execGenerate(
            final Model inputModel,
            final QuerySolution initialBinding,
            final StreamRDF outputStream,
            final Context context) {
        final Dataset inputDataset = DatasetFactory.create(inputModel);
        return execGenerate(inputDataset, initialBinding, outputStream, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final CompletableFuture<Void> execGenerate(
            final Dataset inputDataset,
            final QuerySolution initialBinding,
            final StreamRDF outputStream) {
        final Context context = SPARQLExt.createContext(getPrefixMapping());
        return execGenerate(inputDataset, initialBinding, outputStream, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final CompletableFuture<Void> execGenerate(
            final Dataset inputDataset,
            final QuerySolution initialBinding,
            final StreamRDF outputStream,
            final Context context) {
        final BNodeMap bNodeMap = new BNodeMap();
        final List<Var> variables = SPARQLExt.getVariables(initialBinding, context);
        final Binding binding = SPARQLExt.getBinding(initialBinding, context);
        final List<Binding> values = Collections.singletonList(binding);
        return exec(inputDataset, variables, values, bNodeMap, context, outputStream, null, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final ResultSet execSelect() {
        final Dataset inputDataset = DatasetFactory.create();
        final QuerySolution initialBinding = new QuerySolutionMap();
        final Context context = SPARQLExt.createContext(getPrefixMapping());
        return execSelect(inputDataset, initialBinding, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final ResultSet execSelect(
            final Context context) {
        final Dataset inputDataset = DatasetFactory.create();
        final QuerySolution initialBinding = new QuerySolutionMap();
        return execSelect(inputDataset, initialBinding, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final ResultSet execSelect(
            final Model inputModel) {
        final Dataset inputDataset = DatasetFactory.create(inputModel);
        final QuerySolution initialBinding = new QuerySolutionMap();
        final Context context = SPARQLExt.createContext(getPrefixMapping());
        return execSelect(inputDataset, initialBinding, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final ResultSet execSelect(
            final Model inputModel,
            final Context context) {
        final Dataset inputDataset = DatasetFactory.create(inputModel);
        final QuerySolution initialBinding = new QuerySolutionMap();
        return execSelect(inputDataset, initialBinding, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final ResultSet execSelect(
            final Model inputModel,
            final QuerySolution initialBinding,
            final Context context) {
        final Dataset inputDataset = DatasetFactory.create();
        return execSelect(inputDataset, initialBinding, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final ResultSet execSelect(
            final Dataset inputDataset) {
        final QuerySolution initialBinding = new QuerySolutionMap();
        final Context context = SPARQLExt.createContext(getPrefixMapping());
        return execSelect(inputDataset, initialBinding, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final ResultSet execSelect(
            final Dataset inputDataset,
            final Context context) {
        final QuerySolution initialBinding = new QuerySolutionMap();
        return execSelect(inputDataset, initialBinding, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final ResultSet execSelect(
            final Dataset inputDataset,
            final QuerySolution initialBinding,
            final Context context) {
        List<ResultSet> results = new ArrayList<>();
        final CompletableFuture<Void> future = execSelect(inputDataset, initialBinding, results::add, context);
        try {
            future.get();
            return ResultSetUtils.union(results.toArray(new ResultSet[results.size()]));
        } catch (InterruptedException ex) {
            LOG.error("Interrupted while executing the SPARQL-Select query");
            throw new SPARQLExtException(ex);
        } catch (ExecutionException ex) {
            LOG.error("Error while executing the SPARQL-Select query", ex);
            throw new SPARQLExtException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final CompletableFuture<Void> execSelect(
            final Consumer<ResultSet> outputStream) {
        final Dataset inputDataset = DatasetFactory.create();
        final QuerySolution initialBinding = new QuerySolutionMap();
        final Context context = SPARQLExt.createContext(getPrefixMapping());
        return execSelect(inputDataset, initialBinding, outputStream, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final CompletableFuture<Void> execSelect(
            final Model inputModel,
            final Consumer<ResultSet> outputStream) {
        final Dataset inputDataset = DatasetFactory.create(inputModel);
        final QuerySolution initialBinding = new QuerySolutionMap();
        final Context context = SPARQLExt.createContext(getPrefixMapping());
        return execSelect(inputDataset, initialBinding, outputStream, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final CompletableFuture<Void> execSelect(
            final Dataset inputDataset,
            final Consumer<ResultSet> outputStream) {
        final QuerySolution initialBinding = new QuerySolutionMap();
        final Context context = SPARQLExt.createContext(getPrefixMapping());
        return execSelect(inputDataset, initialBinding, outputStream, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final CompletableFuture<Void> execSelect(
            final Model inputModel,
            final Consumer<ResultSet> outputStream,
            final Context context) {
        final Dataset inputDataset = DatasetFactory.create(inputModel);
        final QuerySolution initialBinding = new QuerySolutionMap();
        return execSelect(inputDataset, initialBinding, outputStream, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final CompletableFuture<Void> execSelect(
            final Dataset inputDataset,
            final Consumer<ResultSet> outputStream,
            final Context context) {
        final QuerySolution initialBinding = new QuerySolutionMap();
        return execSelect(inputDataset, initialBinding, outputStream, context);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final CompletableFuture<Void> execSelect(
            final Model inputModel,
            final QuerySolution initialBinding,
            final Consumer<ResultSet> outputStream) {
        final Dataset inputDataset = DatasetFactory.create(inputModel);
        final Context context = SPARQLExt.createContext(getPrefixMapping());
        return execSelect(inputDataset, initialBinding, outputStream, context);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final CompletableFuture<Void> execSelect(
            final Dataset inputDataset,
            final QuerySolution initialBinding,
            final Consumer<ResultSet> outputStream) {
        final Context context = SPARQLExt.createContext(getPrefixMapping());
        return execSelect(inputDataset, initialBinding, outputStream, context);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final CompletableFuture<Void> execSelect(
            final Model inputModel,
            final QuerySolution initialBinding,
            final Consumer<ResultSet> outputStream,
            final Context context) {
        final Dataset inputDataset = DatasetFactory.create(inputModel);
        return execSelect(inputDataset, initialBinding, outputStream, context);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final CompletableFuture<Void> execSelect(
            final Consumer<ResultSet> outputStream,
            final Context context) {
        final Dataset inputDataset = DatasetFactory.create();
        final QuerySolution initialBinding = new QuerySolutionMap();
        return execSelect(inputDataset, initialBinding, outputStream, context);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final CompletableFuture<Void> execSelect(
            final QuerySolution initialBinding,
            final Consumer<ResultSet> outputStream) {
        final Dataset inputDataset = DatasetFactory.create();
        final Context context = SPARQLExt.createContext(getPrefixMapping());
        return execSelect(inputDataset, initialBinding, outputStream, context);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final CompletableFuture<Void> execSelect(
            final QuerySolution initialBinding,
            final Consumer<ResultSet> outputStream,
            final Context context) {
        final Dataset inputDataset = DatasetFactory.create();
        return execSelect(inputDataset, initialBinding, outputStream, context);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final CompletableFuture<Void> execSelect(
            final Dataset inputDataset,
            final QuerySolution initialBinding,
            final Consumer<ResultSet> outputStream,
            final Context context) {
        final BNodeMap bNodeMap = new BNodeMap();
        final List<Var> variables = SPARQLExt.getVariables(initialBinding, context);
        final Binding binding = SPARQLExt.getBinding(initialBinding, context);
        final List<Binding> values = Collections.singletonList(binding);
        return exec(inputDataset, variables, values, bNodeMap, context, null, outputStream, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String execTemplate() {
        final Dataset inputDataset = DatasetFactory.create();
        final QuerySolution initialBinding = new QuerySolutionMap();
        final Context context = SPARQLExt.createContext(getPrefixMapping());
        return execTemplate(inputDataset, initialBinding, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String execTemplate(
            final Context context) {
        final Dataset inputDataset = DatasetFactory.create();
        final QuerySolution initialBinding = new QuerySolutionMap();
        return execTemplate(inputDataset, initialBinding, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String execTemplate(
            final Model inputModel) {
        final Dataset inputDataset = DatasetFactory.create(inputModel);
        final QuerySolution initialBinding = new QuerySolutionMap();
        final Context context = SPARQLExt.createContext(getPrefixMapping());
        return execTemplate(inputDataset, initialBinding, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String execTemplate(
            final Model inputModel,
            final Context context) {
        final Dataset inputDataset = DatasetFactory.create(inputModel);
        final QuerySolution initialBinding = new QuerySolutionMap();
        return execTemplate(inputDataset, initialBinding, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String execTemplate(
            final Dataset inputDataset) {
        final QuerySolution initialBinding = new QuerySolutionMap();
        final Context context = SPARQLExt.createContext(getPrefixMapping());
        return execTemplate(inputDataset, initialBinding, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String execTemplate(
            final Dataset inputDataset,
            final Context context) {
        final QuerySolution initialBinding = new QuerySolutionMap();
        return execTemplate(inputDataset, initialBinding, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String execTemplate(
            final Model inputModel,
            final QuerySolution initialBinding,
            final Context context) {
        final Dataset inputDataset = DatasetFactory.create(inputModel);
        return execTemplate(inputDataset, initialBinding, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String execTemplate(
            final Dataset inputDataset,
            final QuerySolution initialBinding,
            final Context context) {
        final StringWriter sw = new StringWriter();
        final CompletableFuture<Void> future = execTemplate(inputDataset, initialBinding, sw::append, context);
        try {
            future.get();
            return sw.toString();
        } catch (InterruptedException ex) {
            LOG.error("Interrupted while executing the SPARQL-Template query");
            throw new SPARQLExtException(ex);
        } catch (ExecutionException ex) {
            LOG.error("Error while executing the SPARQL-Template query", ex);
            throw new SPARQLExtException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final CompletableFuture<Void> execTemplate(
            final Consumer<String> output) {
        final Dataset inputDataset = DatasetFactory.create();
        final QuerySolution initialBinding = new QuerySolutionMap();
        final Context context = SPARQLExt.createContext(getPrefixMapping());
        return execTemplate(inputDataset, initialBinding, output, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final CompletableFuture<Void> execTemplate(
            final Consumer<String> output,
            final Context context) {
        final Dataset inputDataset = DatasetFactory.create();
        final QuerySolution initialBinding = new QuerySolutionMap();
        return execTemplate(inputDataset, initialBinding, output, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final CompletableFuture<Void> execTemplate(
            final Model inputModel,
            final Consumer<String> output) {
        final Dataset inputDataset = DatasetFactory.create(inputModel);
        final QuerySolution initialBinding = new QuerySolutionMap();
        final Context context = SPARQLExt.createContext(getPrefixMapping());
        return execTemplate(inputDataset, initialBinding, output, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final CompletableFuture<Void> execTemplate(
            final Dataset inputDataset,
            final Consumer<String> output) {
        final QuerySolution initialBinding = new QuerySolutionMap();
        final Context context = SPARQLExt.createContext(getPrefixMapping());
        return execTemplate(inputDataset, initialBinding, output, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final CompletableFuture<Void> execTemplate(
            final Model inputModel,
            final Consumer<String> output,
            final Context context) {
        final Dataset inputDataset = DatasetFactory.create(inputModel);
        final QuerySolution initialBinding = new QuerySolutionMap();
        return execTemplate(inputDataset, initialBinding, output, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final CompletableFuture<Void> execTemplate(
            final Dataset inputDataset,
            final Consumer<String> output,
            final Context context) {
        final QuerySolution initialBinding = new QuerySolutionMap();
        return execTemplate(inputDataset, initialBinding, output, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final CompletableFuture<Void> execTemplate(
            final QuerySolution initialBinding,
            final Consumer<String> output) {
        final Dataset inputDataset = DatasetFactory.create();
        final Context context = SPARQLExt.createContext(getPrefixMapping());
        return execTemplate(inputDataset, initialBinding, output, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final CompletableFuture<Void> execTemplate(
            final QuerySolution initialBinding,
            final Consumer<String> output,
            final Context context) {
        final Dataset inputDataset = DatasetFactory.create();
        return execTemplate(inputDataset, initialBinding, output, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final CompletableFuture<Void> execTemplate(
            final Model inputModel,
            final QuerySolution initialBinding,
            final Consumer<String> output) {
        final Dataset inputDataset = DatasetFactory.create(inputModel);
        final Context context = SPARQLExt.createContext(getPrefixMapping());
        return execTemplate(inputDataset, initialBinding, output, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final CompletableFuture<Void> execTemplate(
            final Dataset inputDataset,
            final QuerySolution initialBinding,
            final Consumer<String> output) {
        final Context context = SPARQLExt.createContext(getPrefixMapping());
        return execTemplate(inputDataset, initialBinding, output, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final CompletableFuture<Void> execTemplate(
            final Model inputModel,
            final QuerySolution initialBinding,
            final Consumer<String> output,
            final Context context) {
        final Dataset inputDataset = DatasetFactory.create(inputModel);
        return execTemplate(inputDataset, initialBinding, output, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final CompletableFuture<Void> execTemplate(
            final Dataset inputDataset,
            final QuerySolution initialBinding,
            final Consumer<String> output,
            final Context context) {
        final BNodeMap bNodeMap = new BNodeMap();
        final List<Var> variables = SPARQLExt.getVariables(initialBinding, context);
        final Binding binding = SPARQLExt.getBinding(initialBinding, context);
        final List<Binding> values = Collections.singletonList(binding);
        return exec(inputDataset, variables, values, bNodeMap, context, null, null, output);
    }

}
