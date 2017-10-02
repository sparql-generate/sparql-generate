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

import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerateException;
import com.github.thesmartenergy.sparql.generate.jena.engine.SelectPlan;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingHashMap;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementData;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Executes the SPARQL SELECT query.
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class SelectPlanImpl extends PlanBase implements SelectPlan {

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
    final public void exec(
            final Dataset inputDataset,
            final List<Var> variables,
            final List<BindingHashMapOverwrite> values) {

        Query q = select.cloneQuery();
        
        // add data block in the where clause.
        ElementData data = new ElementData();
        
        for (Var v : variables) {
            data.add(v);
        }
        for (BindingHashMapOverwrite b : values) {
            data.add(b);
        }
        ElementGroup old = (ElementGroup) q.getQueryPattern();
        
        ElementGroup temp = new ElementGroup();
        temp.addElement(data);
        for(Element olde : old.getElements()) {
            temp.addElement(olde);
        }
        q.setQueryPattern(temp);        
        try {
            QueryExecution exec = QueryExecutionFactory.create(q, inputDataset);
            ResultSet results = exec.execSelect();            
            exec = QueryExecutionFactory.create(q, inputDataset);
            results = exec.execSelect();
            List<Var> newVariables = new ArrayList<>();
            List<Binding> newValues = new ArrayList<>();
            for (String var : results.getResultVars()) {
                newVariables.add(Var.alloc(var));
            }
            while (results.hasNext()) {
                newValues.add(new BindingHashMapOverwrite(results.next()));
            }
            
            variables.clear();
            values.clear();
            for(Var newVar : newVariables) {
                variables.add(newVar);
            }
            for(Binding newBinding : newValues) {
                values.add((BindingHashMapOverwrite) newBinding);
            }
        } catch (Exception ex) {
            LOG.error("Error while executing SELECT Query", ex);
            throw new SPARQLGenerateException("Error while executing"
                    + " SELECT Query", ex);
        }
    }

    /**
     * merge values in qvalues.
     *
     * @param qvariables
     * @param qvalues
     * @param variables
     * @param values
     */
    private void mergeValues(
            final List<Var> qvariables,
            final List<Binding> qvalues,
            final List<Var> variables,
            final List<BindingHashMapOverwrite> values) {

        // check that no variable is duplicate in variables and qvariables
        // brute force, ok if not many variables.
        checkNotContainsSome(qvariables, variables);
        checkNotContainsSome(variables, qvariables);

        // merge variables
        qvariables.addAll(variables);

        // deal with empty cases
        if (values.isEmpty() && qvalues.isEmpty()) {
            return;
        }
        // ensure not empty
        ensureNotEmpty(qvariables, qvalues);
        ensureNotEmpty(variables, values);

        // perform the merge
        int j = qvalues.size();
        for (int i = 0; i < j; i++) {
            Binding qb = qvalues.remove(0);
            for (Binding b : values) {
                BindingHashMap newqb = new BindingHashMap();
                newqb.addAll(qb);
                newqb.addAll(b);
                qvalues.add(newqb);
            }
        }
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
