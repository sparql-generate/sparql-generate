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

import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerateContext;
import com.github.thesmartenergy.sparql.generate.jena.engine.impl.BindingHashMapOverwrite;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Executes a ITERATOR clause.
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public interface IteratorPlan extends IteratorOrSourceOrBindPlan {

    /**
     * Updates the values block.
     *
     * @param binding the existing bindings.
     * @param valuesStream where new bindings are emited.
     * @param context the execution context.
     * @return the future that will be completed when the iterator completes.
     */
    CompletableFuture<Void> exec(
            final BindingHashMapOverwrite binding,
            final SPARQLGenerateContext context,
            final Function<Collection<BindingHashMapOverwrite>, CompletableFuture<Void>> valuesStream);

}
