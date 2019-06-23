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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ResultSet;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.util.Context;

/**
 * Executes the GENERATE Clause
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public interface ExecutionPlan {

    /**
     * Executes a plan.
     * 
     * @param inputDataset the Dataset to use for the SPARQL SELECT part of the
     * query.
     * @param variables the list of variables.
     * @param values the list of bindings.
     * @param bNodeMap when instantiating a basic graph pattern for multiple
     * solutions, some of the blank nodes will be mapped to the same blank node
     * across solutions, while other blank nodes will be mapped to a different
     * blank node for each solution. This map holds the information of the first
     * category.
     * @param context the execution context
     * @param outputGenerate the RDFStream where triples are emitted, or null.
     * @param outputSelect the consumer where query results are emitted, or null.
     * @param outputTemplate where text is emitted, or null.
     * @return the completable future
     */
    CompletableFuture<Void> exec(
            final Dataset inputDataset,
            final List<Var> variables,
            final List<Binding> values,
            final BNodeMap bNodeMap,
            final Context context,
            final StreamRDF outputGenerate,
            final Consumer<ResultSet> outputSelect,
            final Consumer<String> outputTemplate);
    
}
