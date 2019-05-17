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

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.util.Context;

/**
 * Class to execute queries. Instances of this class are created by a
 * {@link PlanFactory}.
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public interface RootPlan extends ExecutionPlan {

    PrefixMapping getPrefixMapping();

    /**
     * Executes a SPARQL-Generate query. And returns the generated RDF triples.
     *
     * @return the Model that contains the generated RDF triples.
     */
    Model execGenerate();

    /**
     * Executes a SPARQL-Generate query. And returns the generated RDF triples.
     *
     * @param context the execution context
     * @return the Model that contains the generated RDF triples.
     */
    Model execGenerate(Context context);

    /**
     * Executes a SPARQL-Generate query. Emit generated triples to the stream.
     *
     * @param outputStream the RDF Stream object.
     * @return the completable future
     */
    CompletableFuture<Void> execGenerate(StreamRDF outputStream);

    /**
     * Executes a SPARQL-Generate query. Emit generated triples to the stream.
     *
     * @param context the execution context
     * @param outputStream the RDF Stream object.
     * @return the completable future
     */
    CompletableFuture<Void> execGenerate(StreamRDF outputStream, Context context);

    /**
     * Executes a SPARQL-Generate query. Uses the given {@code inputModel} for
     * the SPARQL SELECT part of the query. And returns the generated RDF
     * triples.
     *
     * @param inputModel the Model to use for the SPARQL SELECT part of the
     * query.
     * @return the Model that contains the generated RDF triples.
     */
    Model execGenerate(Model inputModel);

    /**
     * Executes a SPARQL-Generate query. Uses the given {@code inputModel} for
     * the SPARQL SELECT part of the query. And returns the generated RDF
     * triples.
     *
     * @param context the execution context
     * @param inputModel the Model to use for the SPARQL SELECT part of the
     * query.
     * @return the Model that contains the generated RDF triples.
     */
    Model execGenerate(Model inputModel, Context context);

    /**
     * Executes a SPARQL-Generate query. Uses the given {@code inputModel} for
     * the SPARQL SELECT part of the query. Emit generated triples to the
     * stream.
     *
     * @param inputModel the Model to use for the SPARQL SELECT part of the
     * query.
     * @param outputStream the RDF Stream object.
     * @return the completable future
     */
    CompletableFuture<Void> execGenerate(Model inputModel, StreamRDF outputStream);

    /**
     * Executes a SPARQL-Generate query. Uses the given {@code inputModel} for
     * the SPARQL SELECT part of the query. Emit generated triples to the
     * stream.
     *
     * @param inputModel the Model to use for the SPARQL SELECT part of the
     * query.
     * @param outputStream the RDF Stream object.
     * @param context the execution context
     * @return the completable future
     */
    CompletableFuture<Void> execGenerate(Model inputModel, StreamRDF outputStream, Context context);

    /**
     * Executes a SPARQL-Generate query. Uses the given {@code inputDataset} for
     * the SPARQL SELECT part of the query. And returns the generated RDF
     * triples.
     *
     * @param inputDataset the Dataset to use for the SPARQL SELECT part of the
     * query.
     * @return the Model that contains the generated RDF triples.
     */
    Model execGenerate(Dataset inputDataset);

    /**
     * Executes a SPARQL-Generate query. Uses the given {@code inputDataset} for
     * the SPARQL SELECT part of the query. And returns the generated RDF
     * triples.
     *
     * @param inputDataset the Dataset to use for the SPARQL SELECT part of the
     * query.
     * @param context the execution context
     * @return the Model that contains the generated RDF triples.
     */
    Model execGenerate(Dataset inputDataset, Context context);

    /**
     * Executes a SPARQL-Generate query. Uses the given {@code inputDataset} for
     * the SPARQL SELECT part of the query. Emit generated triples to the
     * stream.
     *
     * @param inputDataset the Dataset to use for the SPARQL SELECT part of the
     * query.
     * @param outputStream the RDF Stream object.
     * @return the completable future
     */
    CompletableFuture<Void> execGenerate(Dataset inputDataset, StreamRDF outputStream);

    /**
     * Executes a SPARQL-Generate query. Uses the given {@code inputDataset} for
     * the SPARQL SELECT part of the query. Emit generated triples to the
     * stream.
     *
     * @param inputDataset the Dataset to use for the SPARQL SELECT part of the
     * query.
     * @param context the execution context
     * @param outputStream the RDF Stream object.
     * @return the completable future
     */
    CompletableFuture<Void> execGenerate(Dataset inputDataset, StreamRDF outputStream, Context context);

    /**
     * Executes a SPARQL-Generate query. Uses the given {@code initialBindings}
     * as if they were specified in a SPARQL VALUES clause. Augments the given
     * {@code initialModel} with the generated RDF triples.
     *
     * @param initialBindings one map of variable-RDF nodes bindings.
     * @param initialModel the Model to augment with the generated RDF triples.
     */
    void execGenerate(QuerySolution initialBindings, Model initialModel);

    /**
     * Executes a SPARQL-Generate query. Uses the given {@code initialBindings}
     * as if they were specified in a SPARQL VALUES clause. Augments the given
     * {@code initialModel} with the generated RDF triples.
     *
     * @param initialBindings one map of variable-RDF nodes bindings.
     * @param initialModel the Model to augment with the generated RDF triples.
     * @param context the execution context
     */
    void execGenerate(QuerySolution initialBindings, Model initialModel, Context context);

    /**
     * Executes a SPARQL-Generate query. Uses the given {@code initialBindings}
     * as if they were specified in a SPARQL VALUES clause. Emit generated
     * triples to the stream.
     *
     * @param initialBindings one map of variable-RDF nodes bindings.
     * @param outputStream the RDF Stream object.
     * @return the completable future
     */
    CompletableFuture<Void> execGenerate(QuerySolution initialBindings, StreamRDF outputStream);

    /**
     * Executes a SPARQL-Generate query. Uses the given {@code initialBindings}
     * as if they were specified in a SPARQL VALUES clause. Emit generated
     * triples to the stream.
     *
     * @param initialBindings one map of variable-RDF nodes bindings.
     * @param outputStream the RDF Stream object.
     * @param context the execution context
     * @return the completable future
     */
    CompletableFuture<Void> execGenerate(QuerySolution initialBindings, StreamRDF outputStream, Context context);

    /**
     * Executes a SPARQL-Generate query. Uses the given {@code inputModel} for
     * the SPARQL SELECT part of the query. Augments the given
     * {@code initialModel} with the generated RDF triples. The behaviour if the
     * two models are equal is not specified.
     *
     * @param inputModel the Model to use for the SPARQL SELECT part of the
     * query.
     * @param initialModel the Model to augment with the generated RDF triples.
     */
    void execGenerate(Model inputModel, Model initialModel);

    /**
     * Executes a SPARQL-Generate query. Uses the given {@code inputModel} for
     * the SPARQL SELECT part of the query. Augments the given
     * {@code initialModel} with the generated RDF triples. The behaviour if the
     * two models are equal is not specified.
     *
     * @param inputModel the Model to use for the SPARQL SELECT part of the
     * query.
     * @param initialModel the Model to augment with the generated RDF triples.
     * @param context the execution context
     */
    void execGenerate(Model inputModel, Model initialModel, Context context);

    /**
     * Executes a SPARQL-Generate query. Uses the given {@code inputModel} for
     * the SPARQL SELECT part of the query. Uses the given
     * {@code initialBindings} as if they were specified in a SPARQL VALUES
     * clause. Augments the given {@code initialModel} with the generated RDF
     * triples. The behaviour if the two models are equal is not specified.
     *
     * @param inputModel the Model to use for the SPARQL SELECT part of the
     * query.
     * @param initialBindings one map of variable-RDF nodes bindings.
     * @param initialModel the Model to augment with the generated RDF triples.
     */
    void execGenerate(Model inputModel, QuerySolution initialBindings,
            Model initialModel);

    /**
     * Executes a SPARQL-Generate query. Uses the given {@code inputModel} for
     * the SPARQL SELECT part of the query. Uses the given
     * {@code initialBindings} as if they were specified in a SPARQL VALUES
     * clause. Augments the given {@code initialModel} with the generated RDF
     * triples. The behaviour if the two models are equal is not specified.
     *
     * @param inputModel the Model to use for the SPARQL SELECT part of the
     * query.
     * @param initialBindings one map of variable-RDF nodes bindings.
     * @param initialModel the Model to augment with the generated RDF triples.2
     * @param context the execution context
     */
    void execGenerate(Model inputModel, QuerySolution initialBindings,
            Model initialModel, Context context);

    /**
     * Executes a SPARQL-Generate query. Uses the given {@code inputModel} for
     * the SPARQL SELECT part of the query. Uses the given
     * {@code initialBindings} as if they were specified in a SPARQL VALUES
     * clause. Emit generated triples to the stream.
     *
     * @param inputModel the Model to use for the SPARQL SELECT part of the
     * query.
     * @param initialBindings one map of variable-RDF nodes bindings.
     * @param outputStream the RDF Stream object.
     * @return the completable future
     */
    CompletableFuture<Void> execGenerate(Model inputModel, QuerySolution initialBindings,
            StreamRDF outputStream);

    /**
     * Executes a SPARQL-Generate query. Uses the given {@code inputModel} for
     * the SPARQL SELECT part of the query. Uses the given
     * {@code initialBindings} as if they were specified in a SPARQL VALUES
     * clause. Emit generated triples to the stream.
     *
     * @param inputModel the Model to use for the SPARQL SELECT part of the
     * query.
     * @param initialBindings one map of variable-RDF nodes bindings.
     * @param outputStream the RDF Stream object.
     * @param context the execution context
     * @return the completable future
     */
    CompletableFuture<Void> execGenerate(Model inputModel, QuerySolution initialBindings,
            StreamRDF outputStream, Context context);

    /**
     * Executes a SPARQL-Generate query. Uses the given {@code inputDataset} for
     * the SPARQL SELECT part of the query. Augments the given
     * {@code initialModel} with the generated RDF triples. The behaviour if
     * {@code initialModel} is one of the RDF graphs in {@code inputDataset} is
     * not specified.
     *
     * @param inputDataset the Dataset to use for the SPARQL SELECT part of the
     * query.
     * @param initialModel the Model to augment with the generated RDF triples.
     */
    void execGenerate(Dataset inputDataset, Model initialModel);

    /**
     * Executes a SPARQL-Generate query. Uses the given {@code inputDataset} for
     * the SPARQL SELECT part of the query. Augments the given
     * {@code initialModel} with the generated RDF triples. The behaviour if
     * {@code initialModel} is one of the RDF graphs in {@code inputDataset} is
     * not specified.
     *
     * @param inputDataset the Dataset to use for the SPARQL SELECT part of the
     * query.
     * @param initialModel the Model to augment with the generated RDF triples.
     * @param context the execution context
     */
    void execGenerate(Dataset inputDataset, Model initialModel, Context context);

    /**
     * Executes a SPARQL-Generate query. Uses the given {@code inputDataset} for
     * the SPARQL SELECT part of the query. Uses the given
     * {@code initialBindings} as if they were specified in a SPARQL VALUES
     * clause. Augments the given {@code initialModel} with the generated RDF
     * triples. The behaviour if {@code initialModel} is one of the RDF graphs
     * in {@code inputDataset} is not specified.
     *
     * @param inputDataset the Dataset to use for the SPARQL SELECT part of the
     * query.
     * @param initialBindings one map of variable-RDF nodes bindings.
     * @param initialModel the Model to augment with the generated RDF triples.
     */
    void execGenerate(Dataset inputDataset, QuerySolution initialBindings,
            Model initialModel);

    /**
     * Executes a SPARQL-Generate query. Uses the given {@code inputDataset} for
     * the SPARQL SELECT part of the query. Uses the given
     * {@code initialBindings} as if they were specified in a SPARQL VALUES
     * clause. Augments the given {@code initialModel} with the generated RDF
     * triples. The behaviour if {@code initialModel} is one of the RDF graphs
     * in {@code inputDataset} is not specified.
     *
     * @param inputDataset the Dataset to use for the SPARQL SELECT part of the
     * query.
     * @param initialBindings one map of variable-RDF nodes bindings.
     * @param initialModel the Model to augment with the generated RDF triples.
     * @param context the execution context
     */
    void execGenerate(Dataset inputDataset, QuerySolution initialBindings,
            Model initialModel, Context context);

    /**
     * Executes a SPARQL-Generate query. Uses the given {@code inputDataset} for
     * the SPARQL SELECT part of the query. Uses the given
     * {@code initialBindings} as if they were specified in a SPARQL VALUES
     * clause. Emit generated triples to the stream.
     *
     * @param inputDataset the Dataset to use for the SPARQL SELECT part of the
     * query.
     * @param initialBindings one map of variable-RDF nodes bindings.
     * @param outputStream the RDF Stream object.
     * @return the completable future
     */
    CompletableFuture<Void> execGenerate(Dataset inputDataset, QuerySolution initialBindings,
            StreamRDF outputStream);

    /**
     * Executes a SPARQL-Generate query. Uses the given {@code inputDataset} for
     * the SPARQL SELECT part of the query. Uses the given
     * {@code initialBindings} as if they were specified in a SPARQL VALUES
     * clause. Emit generated triples to the stream.
     *
     * @param inputDataset the Dataset to use for the SPARQL SELECT part of the
     * query.
     * @param initialBindings one map of variable-RDF nodes bindings.
     * @param outputStream the RDF Stream object.
     * @param context the execution context
     * @return the completable future
     */
    CompletableFuture<Void> execGenerate(Dataset inputDataset, QuerySolution initialBindings,
            StreamRDF outputStream, Context context);

    /**
     * Executes a SPARQL-Select query.
     *
     * @return the resultset
     */
    ResultSet execSelect();

    /**
     * Executes a SPARQL-Select query.
     *
     * @param context the execution context
     * @return the resultset
     */
    ResultSet execSelect(Context context);

    /**
     * Executes a SPARQL-Select query. Uses the given {@code inputModel}.
     * triples.
     *
     * @param inputModel the input Model
     * @return the resultset
     */
    ResultSet execSelect(Model inputModel);

    /**
     * Executes a SPARQL-Select query. Uses the given {@code inputModel}.
     * triples.
     *
     * @param context the execution context
     * @param inputModel the input Model
     * @return the resultset
     */
    ResultSet execSelect(Model inputModel, Context context);

    /**
     * Executes a SPARQL-Select query. Uses the given {@code inputModel} and the
     * given {@code initialBindings} as if they were specified in a SPARQL
     * VALUES clause.
     *
     * @param inputModel the input Model
     * @param initialBindings one map of variable-RDF nodes bindings.
     * @param context the execution context
     * @return the resultset
     */
    ResultSet execSelect(Model inputModel, QuerySolution initialBindings,
            Context context);

    /**
     * Executes a SPARQL-Select query. Uses the given {@code inputDataset}.
     *
     * @param inputDataset the input Dataset.
     * @return the resultset
     */
    ResultSet execSelect(Dataset inputDataset);

    /**
     * Executes a SPARQL-Select query. Uses the given {@code inputDataset}.
     *
     * @param inputDataset the input Dataset.
     * @param context the execution context
     * @return the resultset
     */
    ResultSet execSelect(Dataset inputDataset, Context context);

    /**
     * Executes a SPARQL-Select query. Uses the given {@code inputDataset} and
     * the given {@code initialBindings} as if they were specified in a SPARQL
     * VALUES clause.
     *
     * @param inputDataset the input Dataset.
     * @param initialBindings one map of variable-RDF nodes bindings.
     * @param context the execution context
     * @return the resultset
     */
    ResultSet execSelect(Dataset inputDataset, QuerySolution initialBindings,
            Context context);

    /**
     * Executes a SPARQL-Select query.
     *
     * @param outputStream where ResultSets are emitted.
     * @return the completable future
     */
    CompletableFuture<Void> execSelect(Consumer<ResultSet> outputStream);

    /**
     * Executes a SPARQL-Select query. Uses the given {@code inputDataset} and
     * the given {@code initialBindings} as if they were specified in a SPARQL
     * VALUES clause.
     *
     * @param outputStream where ResultSets are emitted.
     * @param context the execution context
     * @return the completable future
     */
    CompletableFuture<Void> execSelect(Consumer<ResultSet> outputStream,
            Context context);

    /**
     * Executes a SPARQL-Select query. Uses the given {@code inputDataset} and
     * the given {@code initialBindings} as if they were specified in a SPARQL
     * VALUES clause.
     *
     * @param inputModel the input Model.
     * @param outputStream where ResultSets are emitted.
     * @return the completable future
     */
    CompletableFuture<Void> execSelect(Model inputModel, Consumer<ResultSet> outputStream);

    /**
     * Executes a SPARQL-Select query. Uses the given {@code inputDataset} and
     * the given {@code initialBindings} as if they were specified in a SPARQL
     * VALUES clause.
     *
     * @param inputDataset the input Dataset.
     * @param outputStream where ResultSets are emitted.
     * @return the completable future
     */
    CompletableFuture<Void> execSelect(Dataset inputDataset, Consumer<ResultSet> outputStream);

    /**
     * Executes a SPARQL-Select query. Uses the given {@code inputDataset} and
     * the given {@code initialBindings} as if they were specified in a SPARQL
     * VALUES clause.
     *
     * @param inputModel the input Model.
     * @param outputStream where ResultSets are emitted.
     * @param context the execution context
     * @return the completable future
     */
    CompletableFuture<Void> execSelect(Model inputModel, Consumer<ResultSet> outputStream,
            Context context);

    /**
     * Executes a SPARQL-Select query. Uses the given {@code inputDataset} and
     * the given {@code initialBindings} as if they were specified in a SPARQL
     * VALUES clause.
     *
     * @param inputDataset the input Dataset.
     * @param outputStream where ResultSets are emitted.
     * @param context the execution context
     * @return the completable future
     */
    CompletableFuture<Void> execSelect(Dataset inputDataset, Consumer<ResultSet> outputStream,
            Context context);

    /**
     * Executes a SPARQL-Select query.
     *
     * @param initialBindings one map of variable-RDF nodes bindings.
     * @param outputStream where ResultSets are emitted.
     * @return the completable future
     */
    CompletableFuture<Void> execSelect(QuerySolution initialBindings, Consumer<ResultSet> outputStream);

    /**
     * Executes a SPARQL-Select query. Uses the given {@code inputDataset} and
     * the given {@code initialBindings} as if they were specified in a SPARQL
     * VALUES clause.
     *
     * @param outputStream where ResultSets are emitted.
     * @param initialBindings one map of variable-RDF nodes bindings.
     * @param context the execution context
     * @return the completable future
     */
    CompletableFuture<Void> execSelect(QuerySolution initialBindings, Consumer<ResultSet> outputStream,
            Context context);

    /**
     * Executes a SPARQL-Select query. Uses the given {@code inputDataset} and
     * the given {@code initialBindings} as if they were specified in a SPARQL
     * VALUES clause.
     *
     * @param inputModel the input Model.
     * @param initialBindings one map of variable-RDF nodes bindings.
     * @param outputStream where ResultSets are emitted.
     * @return the completable future
     */
    CompletableFuture<Void> execSelect(Model inputModel, QuerySolution initialBindings, Consumer<ResultSet> outputStream);

    /**
     * Executes a SPARQL-Select query. Uses the given {@code inputDataset} and
     * the given {@code initialBindings} as if they were specified in a SPARQL
     * VALUES clause.
     *
     * @param inputDataset the input Dataset.
     * @param initialBindings one map of variable-RDF nodes bindings.
     * @param outputStream where ResultSets are emitted.
     * @return the completable future
     */
    CompletableFuture<Void> execSelect(Dataset inputDataset, QuerySolution initialBindings, Consumer<ResultSet> outputStream);

    /**
     * Executes a SPARQL-Select query. Uses the given {@code inputDataset} and
     * the given {@code initialBindings} as if they were specified in a SPARQL
     * VALUES clause.
     *
     * @param inputModel the input Model.
     * @param initialBindings one map of variable-RDF nodes bindings.
     * @param outputStream where ResultSets are emitted.
     * @param context the execution context
     * @return the completable future
     */
    CompletableFuture<Void> execSelect(Model inputModel, QuerySolution initialBindings, Consumer<ResultSet> outputStream,
            Context context);

    /**
     * Executes a SPARQL-Select query. Uses the given {@code inputDataset} and
     * the given {@code initialBindings} as if they were specified in a SPARQL
     * VALUES clause.
     *
     * @param inputDataset the input Dataset.
     * @param initialBindings one map of variable-RDF nodes bindings.
     * @param outputStream where ResultSets are emitted.
     * @param context the execution context
     * @return the completable future
     */
    CompletableFuture<Void> execSelect(Dataset inputDataset, QuerySolution initialBindings, Consumer<ResultSet> outputStream,
            Context context);

    /**
     * Executes a SPARQL-Template query.
     *
     * @return the resultset
     */
    String execTemplate();

    /**
     * Executes a SPARQL-Template query.
     *
     * @param context the execution context
     * @return the resultset
     */
    String execTemplate(Context context);

    /**
     * Executes a SPARQL-Template query. Uses the given {@code inputModel}.
     * triples.
     *
     * @param inputModel the input Model
     * @return the resultset
     */
    String execTemplate(Model inputModel);

    /**
     * Executes a SPARQL-Template query. Uses the given {@code inputModel}.
     * triples.
     *
     * @param context the execution context
     * @param inputModel the input Model
     * @return the resultset
     */
    String execTemplate(Model inputModel, Context context);

    /**
     * Executes a SPARQL-Template query. Uses the given {@code inputModel} and
     * the given {@code initialBindings} as if they were specified in a SPARQL
     * VALUES clause.
     *
     * @param inputModel the input Model
     * @param initialBindings one map of variable-RDF nodes bindings.
     * @param context the execution context
     * @return the resultset
     */
    String execTemplate(Model inputModel, QuerySolution initialBindings,
            Context context);

    /**
     * Executes a SPARQL-Template query. Uses the given {@code inputDataset}.
     *
     * @param inputDataset the input Dataset.
     * @return the resultset
     */
    String execTemplate(Dataset inputDataset);

    /**
     * Executes a SPARQL-Template query. Uses the given {@code inputDataset}.
     *
     * @param inputDataset the input Dataset.
     * @param context the execution context
     * @return the resultset
     */
    String execTemplate(Dataset inputDataset, Context context);

    /**
     * Executes a SPARQL-Template query. Uses the given {@code inputDataset} and
     * the given {@code initialBindings} as if they were specified in a SPARQL
     * VALUES clause.
     *
     * @param inputDataset the input Dataset.
     * @param initialBindings one map of variable-RDF nodes bindings.
     * @param context the execution context
     * @return the resultset
     */
    String execTemplate(Dataset inputDataset, QuerySolution initialBindings,
            Context context);

    /**
     * Executes a SPARQL-Template query.
     *
     * @param output where the result is printed.
     * @return the completable future
     */
    CompletableFuture<Void> execTemplate(Consumer<String> output);

    /**
     * Executes a SPARQL-Template query. Uses the given {@code inputDataset} and
     * the given {@code initialBindings} as if they were specified in a SPARQL
     * VALUES clause.
     *
     * @param output where the result is printed.
     * @param context the execution context
     * @return the completable future
     */
    CompletableFuture<Void> execTemplate(Consumer<String> output,
            Context context);

    /**
     * Executes a SPARQL-Template query. Uses the given {@code inputDataset} and
     * the given {@code initialBindings} as if they were specified in a SPARQL
     * VALUES clause.
     *
     * @param inputModel the input Model.
     * @param output where the result is printed.
     * @return the completable future
     */
    CompletableFuture<Void> execTemplate(Model inputModel, Consumer<String> output);

    /**
     * Executes a SPARQL-Template query. Uses the given {@code inputDataset} and
     * the given {@code initialBindings} as if they were specified in a SPARQL
     * VALUES clause.
     *
     * @param inputDataset the input Dataset.
     * @param output where the result is printed.
     * @return the completable future
     */
    CompletableFuture<Void> execTemplate(Dataset inputDataset, Consumer<String> output);

    /**
     * Executes a SPARQL-Template query. Uses the given {@code inputDataset} and
     * the given {@code initialBindings} as if they were specified in a SPARQL
     * VALUES clause.
     *
     * @param inputModel the input Model.
     * @param output where the result is printed.
     * @param context the execution context
     * @return the completable future
     */
    CompletableFuture<Void> execTemplate(Model inputModel, Consumer<String> output,
            Context context);

    /**
     * Executes a SPARQL-Template query. Uses the given {@code inputDataset} and
     * the given {@code initialBindings} as if they were specified in a SPARQL
     * VALUES clause.
     *
     * @param inputDataset the input Dataset.
     * @param output where the result is printed.
     * @param context the execution context
     * @return the completable future
     */
    CompletableFuture<Void> execTemplate(Dataset inputDataset, Consumer<String> output,
            Context context);

    /**
     * Executes a SPARQL-Template query.
     *
     * @param initialBindings one map of variable-RDF nodes bindings.
     * @param output where the result is printed.
     * @return the completable future
     */
    CompletableFuture<Void> execTemplate(QuerySolution initialBindings, Consumer<String> output);

    /**
     * Executes a SPARQL-Template query. Uses the given {@code inputDataset} and
     * the given {@code initialBindings} as if they were specified in a SPARQL
     * VALUES clause.
     *
     * @param output where the result is printed.
     * @param initialBindings one map of variable-RDF nodes bindings.
     * @param context the execution context
     * @return the completable future
     */
    CompletableFuture<Void> execTemplate(QuerySolution initialBindings, Consumer<String> output,
            Context context);

    /**
     * Executes a SPARQL-Template query. Uses the given {@code inputDataset} and
     * the given {@code initialBindings} as if they were specified in a SPARQL
     * VALUES clause.
     *
     * @param inputModel the input Model.
     * @param initialBindings one map of variable-RDF nodes bindings.
     * @param output where the result is printed.
     * @return the completable future
     */
    CompletableFuture<Void> execTemplate(Model inputModel, QuerySolution initialBindings, Consumer<String> output);

    /**
     * Executes a SPARQL-Template query. Uses the given {@code inputDataset} and
     * the given {@code initialBindings} as if they were specified in a SPARQL
     * VALUES clause.
     *
     * @param inputDataset the input Dataset.
     * @param initialBindings one map of variable-RDF nodes bindings.
     * @param output where the result is printed.
     * @return the completable future
     */
    CompletableFuture<Void> execTemplate(Dataset inputDataset, QuerySolution initialBindings, Consumer<String> output);

    /**
     * Executes a SPARQL-Template query. Uses the given {@code inputDataset} and
     * the given {@code initialBindings} as if they were specified in a SPARQL
     * VALUES clause.
     *
     * @param inputModel the input Model.
     * @param initialBindings one map of variable-RDF nodes bindings.
     * @param output where the result is printed.
     * @param context the execution context
     * @return the completable future
     */
    CompletableFuture<Void> execTemplate(Model inputModel, QuerySolution initialBindings, Consumer<String> output,
            Context context);

    /**
     * Executes a SPARQL-Template query. Uses the given {@code inputDataset} and
     * the given {@code initialBindings} as if they were specified in a SPARQL
     * VALUES clause.
     *
     * @param inputDataset the input Dataset.
     * @param initialBindings one map of variable-RDF nodes bindings.
     * @param output where the result is printed.
     * @param context the execution context
     * @return the completable future
     */
    CompletableFuture<Void> execTemplate(Dataset inputDataset, QuerySolution initialBindings, Consumer<String> output,
            Context context);

}
