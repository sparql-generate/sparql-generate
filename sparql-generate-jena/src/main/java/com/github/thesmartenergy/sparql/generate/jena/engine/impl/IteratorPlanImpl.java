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

import com.github.thesmartenergy.sparql.generate.jena.engine.IteratorPlan;
import com.github.thesmartenergy.sparql.generate.jena.iterator.IteratorFunction;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionEnv;
import org.apache.jena.sparql.function.FunctionEnvBase;
import org.apache.jena.sparql.util.Context;

/**
 * Executes a {@code ITERATOR <iterator>(<expreList>) AS <var>} clause.
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class IteratorPlanImpl extends PlanBase implements IteratorPlan {

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(IteratorPlanImpl.class);

    /**
     * The SPARQL-Generate iterator.
     */
    private final IteratorFunction iterator;

    /**
     * The list of expressions on which to evaluate the iterator.
     */
    private final ExprList exprList;

    /**
     * The variable that will be bound to each result of the iterator
     * evaluation.
     */
    private final List<Var> vars;

    /**
     * The constructor.
     *
     * @param s    - The SPARQL-Generate iterator function.
     * @param e    - The list of expressions on which to evaluate the iterator
     *             function.
     * @param vars - The list of variables that will be bound to each result of the iterator
     *             function evaluation.
     */
    public IteratorPlanImpl(
            final IteratorFunction s,
            final ExprList e,
            final List<Var> vars) {
        this.iterator = s;
        this.exprList = e;
        this.vars = vars;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    final public void exec(
            final List<Var> variables,
            final List<BindingHashMapOverwrite> values,
            final Consumer<List<BindingHashMapOverwrite>> bindingStream,
            final Context context) {

        boolean added = variables.addAll(vars);
        if (!added) {
            LOG.debug("Bindings of variables " + vars + " will be overriden");
        }

        ensureNotEmpty(variables, values);
        final StringBuilder sb;
        if (LOG.isTraceEnabled()) {
            sb = new StringBuilder("Execution of " + iterator + " " + exprList + " AS  " + vars + ":\n");
        } else {
            sb = null;
        }
        values.forEach((binding) -> {
            try {
                final FunctionEnv env = new FunctionEnvBase(context);
                iterator.exec(binding, exprList, env, (nodeValues) -> {
                    List<BindingHashMapOverwrite> newValues = new ArrayList<>();
                    if (nodeValues == null || nodeValues.isEmpty()) {
                        for (Var v : vars) {
                            newValues.add(new BindingHashMapOverwrite(binding, v, null));
                        }
                    } else {
                        int nbIterables = nodeValues.get(0).size();
                        for (int j = 0; j < nbIterables; j++) {
                            BindingHashMapOverwrite bindingHashMapOverwrite = new BindingHashMapOverwrite(binding, null, null);
                            for (int i = 0; i < vars.size(); i++) {
                                Var v = vars.get(i);
                                try {
                                    if(nodeValues.get(i) != null
                                            && nodeValues.get(i).get(j) != null) {
                                        Node n = nodeValues.get(i).get(j).asNode();
                                        bindingHashMapOverwrite.add(v, n);
                                    }
                                } catch (IndexOutOfBoundsException ex) {
                                    LOG.warn("The number of variables does not match the number of names provided to the iterator arguments");
                                    break;
                                }
                            }
                            newValues.add(bindingHashMapOverwrite);
                        }
                        bindingStream.accept(newValues);
                    }
                });
                if (LOG.isTraceEnabled()) {
                    sb.setLength(sb.length() - 2);
                    LOG.trace(sb.toString());
                }
            } catch (ExprEvalException ex) {
                LOG.debug("Iterator execution failed due to " + ex.getClass().getSimpleName() + " - " + ex.getMessage());
                //newValues.add(new BindingHashMapOverwrite(binding, vars, null));
            }
        });
    }
}
