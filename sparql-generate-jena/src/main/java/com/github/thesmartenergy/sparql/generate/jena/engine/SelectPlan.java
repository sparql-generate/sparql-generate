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

import com.github.thesmartenergy.sparql.generate.jena.engine.impl.BindingHashMapOverwrite;
import java.util.List;
import org.apache.jena.query.Dataset;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.util.Context;

/**
 * Executes generated SPARQL SELECT query.
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public interface SelectPlan {

    /**
     * Updates a values block with the execution of a SPARQL SELECT query.
     *
     * @param inputDataset the Dataset to use for the SPARQL SELECT part of the
     * query.
     * @param variables the set of bound variables.
     * @param values the set of bindings.
     * @param context the execution context.
     */
    void exec(
            final Dataset inputDataset,
            final List<Var> variables,
            final List<BindingHashMapOverwrite> values,
            final Context context);
    
}
