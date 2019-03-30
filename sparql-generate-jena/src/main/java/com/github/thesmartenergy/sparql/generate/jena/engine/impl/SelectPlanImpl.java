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
import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerateException;
import com.github.thesmartenergy.sparql.generate.jena.engine.SelectPlan;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingHashMap;
import org.apache.jena.sparql.syntax.ElementData;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.util.Symbol;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Executes the SPARQL SELECT query.
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class SelectPlanImpl implements SelectPlan {

    private static final Logger LOG = LoggerFactory.getLogger(SelectPlanImpl.class);

    /**
     * The query.
     */
    private final Query select;

    /**
     * Constructor.
     *
     * @param query the SPARQL SELECT query.
     */
    public SelectPlanImpl(final Query query) {
        if (!query.isSelectType()) {
            LOG.error("Should be select query. " + query);
            throw new IllegalArgumentException("Should be select query.");
        }
        this.select = query;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    final public List<BindingHashMapOverwrite> exec(
            final Dataset inputDataset,
            final Collection<BindingHashMapOverwrite> values,
            final SPARQLGenerateContext context) {
        try {
            final Query q = createQuery(select, values);
            final QueryExecution exec = QueryExecutionFactory.create(q, inputDataset);
            for (Symbol symbol : context.keys()) {
                exec.getContext().set(symbol, context.get(symbol));
            }
            final ResultSet results = exec.execSelect();
            final List<BindingHashMapOverwrite> newBindings = new ArrayList<>();
            while (results.hasNext()) {
                newBindings.add(new BindingHashMapOverwrite(results.next(), context));
            }
            return newBindings;
        } catch (Exception ex) {
            LOG.error("Error while executing SELECT Query", ex);
            throw new SPARQLGenerateException("Error while executing"
                    + " SELECT Query", ex);
        }
    }

    private Query createQuery(Query select, Collection<BindingHashMapOverwrite> values) {
        LOG.debug("Create select query for " + values.size() + " initial values");
        LOG.trace("Initial values are " + values);
        Query q = select.cloneQuery();
        ElementGroup old = (ElementGroup) q.getQueryPattern();
        ElementGroup temp = new ElementGroup();
        if (old.size() > 1 && old.get(0) instanceof ElementData) {
            ElementData qData = (ElementData) old.get(0);
            int oldSize = qData.getRows().size();
            qData = mergeValues(qData, values);
            temp.addElement(qData);
            LOG.debug("New query has " + qData.getRows().size() + " initial values. It had " + oldSize + " values before");
            LOG.trace("New values are " + qData);
            for (int i = 1; i < old.size(); i++) {
                temp.addElement(old.get(i));
            }
        } else {
            ElementData data = new ElementData();
            values.forEach((binding) -> {
                binding.varsList().forEach(data::add);
                data.add(binding);
            });
            LOG.debug("New query has " + data.getRows().size() + " initial values");
            if (data.getRows().size() != values.size()) {
                LOG.warn("Different size for the values block here.\n Was "
                        + values.size() + ": \n" + values + "\n now is "
                        + data.getRows().size() + ": \n" + data.getRows());
                StringBuilder sb = new StringBuilder("Different size for the values block here.\n Was "
                        + values.size() + ": \n" + values + "\n\n");
                int i = 0;
                for (Binding b: values) {
                    sb.append("\nbinding " + i++ + " is " + b);
                }
                LOG.warn(sb.toString());
            }

            temp.addElement(data);
            old.getElements().forEach(temp::addElement);
        }
        q.setQueryPattern(temp);
        return q;
    }

    private ElementData mergeValues(
            final ElementData qData,
            final Collection<BindingHashMapOverwrite> values) {

        if (values.isEmpty()) {
            return qData;
        }

        // check that no variable is duplicate in variables and qvariables
        // brute force, ok if not many variables.
        List<Var> vars = qData.getVars();
        for (BindingHashMapOverwrite binding : values) {
            List<Var> newVars = binding.varsList();
            checkNotContainsSome(vars, newVars);
            checkNotContainsSome(newVars, vars);
        }

        if (values.isEmpty()) {
            return qData;
        }

        ElementData data = new ElementData();
        qData.getVars().forEach((v) -> data.add(v));
        for (BindingHashMapOverwrite binding : values) {
            binding.varsList().forEach((v) -> data.add(v));
            for (Binding qbinding : qData.getRows()) {
                BindingHashMap newb = new BindingHashMap(qbinding);
                binding.varsList().forEach((v) -> newb.add(v, binding.get(v)));
                data.add(newb);
            }
        }
        return data;
    }

    private void checkNotContainsSome(List<Var> vars1, List<Var> vars2) {
        for (Var v : vars1) {
            if (vars2.contains(v)) {
                throw new SPARQLGenerateException("Variable " + v + " has"
                        + " already been bound !");
            }
        }
    }

}
