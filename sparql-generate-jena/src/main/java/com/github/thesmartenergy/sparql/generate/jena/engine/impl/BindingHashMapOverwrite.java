/*
 * Copyright 2016 Ecole des Mines de Saint-Etienne.
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

import org.apache.jena.graph.Node;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import org.apache.jena.sparql.util.Context;

/**
 * Class to store overridable bindings efficiently.
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class BindingHashMapOverwrite extends PlanBase implements Binding {

    private static final Logger LOG = LoggerFactory.getLogger(BindingHashMapOverwrite.class);

    /**
     * The parent binding.
     */
    private final Binding parent;
    /**
     * The variables and the corresponding nodes of this binding
     */
    private final Map<Var, Node> map = new HashMap<>();

    /**
     * Constructs a new binding by binding a new (or not new) variable.
     *
     * @param parent -
     * @param var    -
     * @param node   -
     */
    public BindingHashMapOverwrite(
            final Binding parent,
            final Var var,
            final Node node) {
        this.parent = parent;
        if (var != null)
            map.put(var, node);
//        LOG.trace("New binding #" + System.identityHashCode(this) + " overrides " + parent + " with " + var + " = " + node);
    }

    /**
     * Adds a variable and its bound node to this binding.
     */
    public final void add(final Var var, final Node node) {
        map.put(var, node);
    }

    /**
     * Constructs a new binding from a query solution.
     *
     * @param binding -
     * @param context -
     */
    public BindingHashMapOverwrite(
            final QuerySolution binding,
            final Context context) {
        if (binding == null) {
            parent = null;
        } else {
            final BindingHashMap p = new BindingHashMap();
            for (Iterator<String> it = binding.varNames(); it.hasNext(); ) {
                final String varName = it.next();
                if (binding.get(varName) != null) {
                    p.add(allocVar(context, varName), binding.get(varName).asNode());
                }
            }
            parent = p;
        }
//        LOG.trace("New binding #" + System.identityHashCode(this) + " copies " + binding);
    }

    /**
     * Lists the variables of this binding.
     *
     * @return The list of variables.
     */
    public final List<Var> varsList() {
        List<Var> vars = new ArrayList<>();
        vars.addAll(map.keySet());
        if (parent != null) {
            for (Iterator<Var> it = parent.vars(); it.hasNext(); ) {
                Var v = it.next();
                if (!map.containsKey(v)) {
                    vars.add(v);
                }
            }
        }
        return vars;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public final Iterator<Var> vars() {
        return varsList().iterator();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public final boolean contains(final Var var) {
        if (var == null) {
            return false;
        }
        if (this.map.containsKey(var)) {
            return true;
        }
        if (parent != null) {
            return parent.contains(var);
        }
        return false;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public final Node get(final Var var) {
        if (var != null && map.containsKey(var)) {
            return map.get(var);
        }
        if (parent != null) {
            return parent.get(var);
        }
        return null;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public final int size() {
        return varsList().size();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public final boolean isEmpty() {
        return varsList().isEmpty();
    }

    //todo write equal, hash, and tostring methods.

}
