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
package com.github.thesmartenergy.sparql.generate.jena.engine;

import com.github.thesmartenergy.sparql.generate.jena.engine.impl.BNodeMap;
import com.github.thesmartenergy.sparql.generate.jena.engine.impl.BindingHashMapOverwrite;
import java.util.List;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.core.Var;

/**
 * The GENERATE Clause
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public interface GeneratePlan {

    /**
     * Executes the GENERATE Clause. Uses the given {@code inputDataset}
     * for the subsequent SPARQL GENERATE queries. Augments the given
     * {@code initialModel} with the generated RDF triples.
     *
     * @param inputDataset the Dataset to use for the SPARQL SELECT part of the
     * query.
     * @param initialModel the Model to augment with the generated RDF triples.
     * @param initialBindings an initial binding.
     * @param bNodeMap when instantiating a basic graph pattern for multiple
     * solutions, some of the blank nodes will be mapped to the same blank node
     * across solutions, while other blank nodes will be mapped to a different
     * blank node for each solution. This map holds the information of the first
     * category.
     * @throws IllegalArgumentException if the {@code initialModel} is null.
     */
    void exec(
        final Dataset inputDataset,
        final QuerySolution initialBindings,
        final Model initialModel,
        final BNodeMap bNodeMap);

    /**
     * Executes the GENERATE Clause. Uses the given {@code inputDataset}
     * for the subsequent SPARQL GENERATE queries. Augments the given
     * {@code initialModel} with the generated RDF triples. Use the given
     * {@code variables} and {@code values} to generate triples. The behaviour
     * if {@code initialModel} is one of the RDF graphs in {@code inputDataset}
     * is not specified.
     *
     * @param inputDataset the Dataset to use for the SPARQL SELECT part of the
     * query.
     * @param initialModel the Model to augment with the generated RDF triples.
     * @param variables the set of bound variables.
     * @param values the set of bindings.
     * @param bNodeMap when instantiating a basic graph pattern for multiple
     * solutions, some of the blank nodes will be mapped to the same blank node
     * across solutions, while other blank nodes will be mapped to a different
     * blank node for each solution. This map holds the information of the first
     * category.
     * @throws IllegalArgumentException if the {@code initialModel} is null.
     */
    void exec(
        final Dataset inputDataset,
        final Model initialModel,
        final List<Var> variables,
        final List<BindingHashMapOverwrite> values,
        final BNodeMap bNodeMap);

}
