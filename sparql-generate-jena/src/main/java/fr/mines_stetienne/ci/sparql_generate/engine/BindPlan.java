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
package fr.mines_stetienne.ci.sparql_generate.engine;

import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;
import fr.mines_stetienne.ci.sparql_generate.utils.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.NodeValue;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.apache.jena.sparql.ARQConstants;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingBuilder;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.function.FunctionEnv;
import org.apache.jena.sparql.function.FunctionEnvBase;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sparql.util.NodeFactoryExtra;

/**
 * Executes a {@code BIND( <expr> AS <var>)} clause.
 *
 * @author Maxime Lefrançois
 */
public class BindPlan extends BindOrSourcePlan {

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(BindPlan.class);

    /**
     * The expression.
     */
    private final Expr expr;


    /**
     * The generation plan of a <code>{@code (BIND <expr> AS <var>)}</code>
     * clause.
     *
     * @param expr The expression. Must not be null.
     * @param var The variable to bind the evaluation of the expression. Must
     * not be null.
     */
    public BindPlan(
            final Expr expr,
            final Var var) {
        super(var);
        Objects.requireNonNull(expr, "Expression must not be null");
        this.expr = expr;
    }

    @Override
    protected void prepare(Context context) {
        // NOW() is the same for the bindings of a batch
        context.set(ARQConstants.sysCurrentTime, NodeFactoryExtra.nowAsDateTime());
    }

    @Override
    protected final Binding exec(Binding binding, Context context) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Start " + this);
        }
        final FunctionEnv env = new FunctionEnvBase(context);
        try {
            final NodeValue n = expr.eval(binding, env);
            if (LOG.isTraceEnabled()) {
                LOG.trace("New binding " + var + " = " + LogUtils.compress(n.asNode()));
            }
            return BindingFactory.binding(parentBinding(binding), var, n.asNode());
        } catch(ExprEvalException ex) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("No evaluation for " + this + " " + ex.getMessage());
            }
            return parentBinding(binding);
        }
    }

    
    /**
     * Executes consecutive BIND clauses in one pass over the bindings, when
     * none of their variables is bound by the input and they are distinct:
     * each binding is extended once, by a flat binding, instead of once per
     * clause. The value is that of executing them one after the other.
     *
     * @param plans the BIND clauses, in order
     * @param values the input bindings
     * @param context the execution context
     * @return the bindings, extended by the clauses
     */
    static List<Binding> execAll(final List<BindPlan> plans, final List<Binding> values, final Context context) {
        // NOW() is the same for the bindings of a batch
        context.set(ARQConstants.sysCurrentTime, NodeFactoryExtra.nowAsDateTime());
        final FunctionEnv env = new FunctionEnvBase(context);
        final int n = plans.size();
        final Node[] nodes = new Node[n];
        final List<Binding> result = new ArrayList<>(values.size());
        int row = 0;
        for (Binding binding : values) {
            if ((row++ & 255) == 0) {
                ContextUtils.checkLimits(context);
            }
            Binding current = binding;
            for (int k = 0; k < n; k++) {
                final BindPlan plan = plans.get(k);
                nodes[k] = null;
                try {
                    final Node node = plan.expr.eval(current, env).asNode();
                    nodes[k] = node;
                    current = BindingFactory.binding(current, plan.var, node);
                } catch (ExprEvalException ex) {
                    // the variable is unbound
                }
            }
            final BindingBuilder builder = Binding.builder(binding);
            for (int k = 0; k < n; k++) {
                if (nodes[k] != null) {
                    builder.add(plans.get(k).var, nodes[k]);
                }
            }
            result.add(builder.build());
        }
        return result;
    }

    @Override
    public String toString() {
        return "BIND( " + expr + " AS " + var + ")";
    }

}
