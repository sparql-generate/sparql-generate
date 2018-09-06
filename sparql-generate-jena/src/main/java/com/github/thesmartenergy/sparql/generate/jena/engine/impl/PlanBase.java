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

import java.util.List;
import java.util.Objects;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingHashMap;

/**
 * One execution.
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
abstract class PlanBase {

    /**
     * Utility function. ensures there is at least a row of null values in a
     * values block.
     *
     * @param <T> a sub class of Binding
     * @param variables the values variables
     * @param values the values bindings
     */
    protected final <T extends Binding>  void ensureNotEmpty(
            final List<Var> variables,
            final List<T> values) {
        Objects.requireNonNull(variables);
        Objects.requireNonNull(values);
        if (values.isEmpty()) {
            final BindingHashMap map = new BindingHashMap();
            values.add((T) new BindingHashMapOverwrite(map, null, null));
        }
    }
}
