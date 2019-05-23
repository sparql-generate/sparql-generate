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
package fr.emse.ci.sparqlext.generate.engine;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.util.Context;


/**
 * Executes a BIND or SOURCE clause.
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public abstract class BindOrSourcePlan implements BindingsClausePlan {
    
    protected final Var var;
    
    protected BindOrSourcePlan(Var var) {
        Objects.requireNonNull(var, "Var must not be null");
        this.var = var;
    }
    
    /**
     * Updates the values block.
     * @param futureValues the future values.
     * @param context the execution context.
     * @param executor the executor
     * 
     * @return the new binding
     */
    public abstract List<Binding> exec(
            final List<Binding> futureValues,
            final Context context,
            final Executor executor);

    public final Var getVar() {
        return var;
}

}
