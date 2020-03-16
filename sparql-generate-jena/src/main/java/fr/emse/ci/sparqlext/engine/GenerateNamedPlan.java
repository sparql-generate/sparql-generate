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

import fr.emse.ci.sparqlext.utils.ContextUtils;
import fr.emse.ci.sparqlext.utils.EvalUtils;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.jena.graph.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.function.FunctionEnv;
import org.apache.jena.sparql.function.FunctionEnvBase;
import org.apache.jena.sparql.util.Context;

/**
 * Executes a named sub-query in the GENERATE clause.
 *
 * @author maxime.lefrancois
 */
public class GenerateNamedPlan implements GeneratePlan {

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(GenerateNamedPlan.class);

    private final Expr name;

    private final ExprList callParameters;

    public GenerateNamedPlan(Expr name, ExprList callParameters) {
        Objects.requireNonNull(name, "name must not be null");
        this.name = name;
        this.callParameters = callParameters;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void exec(
            final List<Var> variables,
            final List<Binding> values,
            final Context context) {
        final QueryExecutor queryExecutor = ContextUtils.getQueryExecutor(context);
        final FunctionEnv env = new FunctionEnvBase(context);
        final Map<String, List<Binding>> splitValues = EvalUtils.splitBindingsForQuery(name, values, env);
        for (String queryName : splitValues.keySet()) {
            final List<Binding> queryValues = splitValues.get(queryName);
            final List<List<Node>> queryCall = EvalUtils.eval(callParameters, queryValues, env);
            queryExecutor.execGenerateFromName(queryName, queryCall, context);
        }
    }

}
