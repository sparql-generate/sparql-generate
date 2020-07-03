/*
 * Copyright 2020 MINES Saint-Étienne
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
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.util.Context;


/**
 * Executes a BIND or SOURCE clause.
 * @author Maxime Lefrançois
 */
public abstract class BindOrSourcePlan implements BindingsClausePlan {
    
    protected final Var var;
    
    protected BindOrSourcePlan(Var var) {
        Objects.requireNonNull(var, "Var must not be null");
        this.var = var;
    }
    
    /**
     * Updates the values block.
     * @param values the values.
     * @param context the execution context.
     * 
     * @return the new binding
     */
    final public List<Binding> exec(
            final List<Binding> values,
            final Context context) {
        return values
                .stream()
                .map((binding) -> exec(binding, context))
                .collect(Collectors.toList());
    }

    abstract protected Binding exec(
            final Binding binding,
            final Context context);

    public final Var getVar() {
        return var;
    }

}