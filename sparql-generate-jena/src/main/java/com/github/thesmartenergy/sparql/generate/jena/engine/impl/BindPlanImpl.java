/*
 * Copyright 2017 Ecole des Mines de Saint-Etienne.
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

import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerateContext;
import java.util.Objects;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.NodeValue;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.github.thesmartenergy.sparql.generate.jena.engine.BindOrSourcePlan;
import org.apache.jena.sparql.ARQConstants;
import org.apache.jena.sparql.function.FunctionEnv;
import org.apache.jena.sparql.function.FunctionEnvBase;
import org.apache.jena.sparql.util.NodeFactoryExtra;

/**
 * Executes a {@code BIND( <expr> AS <var>)} clause.
 *
 * @author maxime.lefrancois
 */
public class BindPlanImpl implements BindOrSourcePlan {

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(BindPlanImpl.class);

    /**
     * The expression.
     */
    private final Expr expr;

    /**
     * The bound variable.
     */
    private final Var var;

    /**
     * The generation plan of a <code>{@code (BIND <expr> AS <var>)}</code>
     * clause.
     *
     * @param expr The expression. Must not be null.
     * @param var The variable to bind the evaluation of the expression. Must
     * not be null.
     */
    public BindPlanImpl(
            final Expr expr,
            final Var var) {
        Objects.requireNonNull(expr, "Expression must not be null");
        Objects.requireNonNull(var, "Var must not be null");
        this.expr = expr;
        this.var = var;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    final public BindingHashMapOverwrite exec(
            final BindingHashMapOverwrite binding,
            final SPARQLGenerateContext context) {
        LOG.debug("Exec BIND(" + expr + " AS " + var + ")");
        context.set(ARQConstants.sysCurrentTime, NodeFactoryExtra.nowAsDateTime());
        final FunctionEnv env = new FunctionEnvBase(context);
        final NodeValue n = expr.eval(binding, env);
        LOG.trace("New binding " + var + " = " + n);
        return new BindingHashMapOverwrite(binding, var, n.asNode());
    }

}
