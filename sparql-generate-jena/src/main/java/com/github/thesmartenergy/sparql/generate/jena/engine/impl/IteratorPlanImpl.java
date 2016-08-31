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
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.log4j.Logger;
import com.github.thesmartenergy.sparql.generate.jena.iterator.IteratorFunction;
import com.github.thesmartenergy.sparql.generate.jena.engine.IteratorOrSourcePlan;

/**
 * Executes a {@code ITERATOR <iterator>(<expreList>) AS <var>} clause.
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class IteratorPlanImpl extends PlanBase implements IteratorOrSourcePlan {

    /**
     * The logger.
     */
    private static final Logger LOG
            = Logger.getLogger(IteratorPlanImpl.class);

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
    private final Var var;

    /**
     * The constructor.
     *
     * @param s - The SPARQL-Generate iterator function.
     * @param e - The list of expressions on which to evaluate the
     * iterator function.
     * @param v - The variable that will be bound to each result of the iterator
     *  function evaluation.
     */
    public IteratorPlanImpl(
            final IteratorFunction s,
            final ExprList e,
            final Var v) {
        this.iterator = s;
        this.exprList = e;
        this.var = v;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    final public void exec(
            final List<Var> variables,
            final List<BindingHashMapOverwrite> values) {

        boolean added = variables.add(var);
        if (!added) {
            LOG.debug("Bindings of variable " + var + " will be overriden");
        }

        ensureNotEmpty(variables, values);
        int j = values.size();
        for (int i = 0; i < j; i++) {
            BindingHashMapOverwrite value = values.remove(0);
            List<NodeValue> messages = null;
            try {
                messages = iterator.exec(value, exprList, null);
            } catch (ExprEvalException ex) {
                LOG.warn("Iterator function execution failed: "
                        + iterator.toString()
                        + ". Continue anyways.", ex);
            }
            if (messages == null || messages.isEmpty()) {
                values.add(new BindingHashMapOverwrite(value, var, null));
            } else {
                for (NodeValue message : messages) {
                    values.add(
                            new BindingHashMapOverwrite(
                                    value, var, message.asNode()));
                }
            }
        }
    }

}
