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

import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerateContext;
import com.github.thesmartenergy.sparql.generate.jena.engine.IteratorPlan;
import com.github.thesmartenergy.sparql.generate.jena.iterator.IteratorFunction;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.ExprList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.apache.jena.sparql.ARQConstants;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionEnv;
import org.apache.jena.sparql.function.FunctionEnvBase;
import org.apache.jena.sparql.util.NodeFactoryExtra;

/**
 * Executes a {@code ITERATOR <iterator>(<expreList>) AS <var>} clause.
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class IteratorPlanImpl implements IteratorPlan {

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
     * @param s - The SPARQL-Generate iterator function.
     * @param e - The list of expressions on which to evaluate the iterator
     * function.
     * @param vars - The list of variables that will be bound to each result of
     * the iterator function evaluation.
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
    final public CompletableFuture<Void> exec(
            final BindingHashMapOverwrite binding,
            final SPARQLGenerateContext context,
            final Function<Collection<BindingHashMapOverwrite>, CompletableFuture<Void>> functionCollectionBindings) {

        context.set(ARQConstants.sysCurrentTime, NodeFactoryExtra.nowAsDateTime());
        final FunctionEnv env = new FunctionEnvBase(context);

        LOG.trace("Starting execution of ITERATOR " + iterator.getClass().getSimpleName() + " " + exprList + " AS " + vars);
        try {
            return iterator.exec(binding, exprList, env, (collectionNodeValues) -> {
                LOG.debug("ITERATOR " + iterator.getClass().getSimpleName() + " " + exprList + " AS  " + vars + " generated " + collectionNodeValues.size() + " new bindings");
                LOG.trace("New bindings are " + collectionNodeValues);
                final Collection<BindingHashMapOverwrite> collectionBindings = getCollectionBinding(binding, collectionNodeValues);
                return functionCollectionBindings.apply(collectionBindings);
            });
        } catch (Exception ex) {
            LOG.warn("Execution failed for ITERATOR " + iterator.getClass().getSimpleName() + " " + exprList + " AS " + vars, ex);
            return CompletableFuture.completedFuture(null);
        }
    }

    private Collection<BindingHashMapOverwrite> getCollectionBinding(
            final BindingHashMapOverwrite binding,
            final Collection<List<NodeValue>> collectionListNodeValues) {
        final Collection<BindingHashMapOverwrite> collectionBindings = new HashSet<>();
        collectionListNodeValues.forEach((listNodeValues) -> {
            if (vars.size() > listNodeValues.size()) {
                LOG.warn("Too many variables, some will not be bound: " + listNodeValues);
                return;
            }
            BindingHashMapOverwrite bindingHashMapOverwrite = new BindingHashMapOverwrite(binding, null, null);
            for (int i = 0; i < vars.size(); i++) {
                if (listNodeValues.get(i) != null) {
                    Node n = listNodeValues.get(i).asNode();
                    bindingHashMapOverwrite.add(vars.get(i), n);
                }
            }
            collectionBindings.add(bindingHashMapOverwrite);
        });
        return collectionBindings;
    }

    @Override
    public String toString() {
        return "ITERATOR " + iterator.getClass().getSimpleName() + " " + exprList + " AS " + vars;
    }
    
    
}
