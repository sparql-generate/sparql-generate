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

import java.util.List;

import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.util.Context;

/**
 * Plan for the GENERATE &lt;...&gt;(...), or GENERATE { ... }
 * clause
 * 
 * @author maxime.lefrancois
 */
public interface GeneratePlan {
    
    /**
     * Executes a GENERATE plan. Method is blocking.
     *
     * @param variables the list of variables.
     * @param values the list of bindings.
     * @param context the execution context
     */
    void exec(
            final List<Var> variables,
            final List<Binding> values,
            final Context context);

}
