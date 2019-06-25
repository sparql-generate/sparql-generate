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
package fr.emse.ci.sparqlext.iterator;

import fr.emse.ci.sparqlext.SPARQLExt;
import fr.emse.ci.sparqlext.engine.BNodeMap;
import fr.emse.ci.sparqlext.engine.PlanFactory;
import fr.emse.ci.sparqlext.engine.RootPlan;
import fr.emse.ci.sparqlext.query.SPARQLExtQuery;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.ARQException;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.engine.binding.BindingMap;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.apache.jena.sparql.util.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Iterator function
 * <a href="http://w3id.org/sparql-generate/iter/call-select">iter:call-select</a>
 * takes as input a IRI to a SPARQL-Select document, runs it on the current
 * Dataset, and binds the given variables to the output of the select query, in
 * order.
 *
 * <ul>
 * <li>Param 1: (select) is the IRI to a SPARQL-Select document</li>
 * </ul>
 *
 * Multiple variables may be bound: variable <em>n</em> is bound to the value of
 * the
 * <em>n</em><sup>th</sup> project variable of the select query.
 *
 * @author Maxime Lefrançois <maxime.lefrançois@emse.fr>
 */
public class SPARQLExtIteratorFunction extends IteratorStreamFunctionBase {
    
    private static final Logger LOG = LoggerFactory.getLogger(SPARQLExtIteratorFunction.class);
    
    private final SPARQLExtQuery select;
    private final RootPlan plan;

    
    public SPARQLExtIteratorFunction(SPARQLExtQuery select, Context context) {
        if(!select.isSelectType()) {
            throw new ARQException("creating iterator for a query that is not a SELECT");
        }
        this.select = select;
        plan = PlanFactory.create(select);
        select.normalize();
    }
    
    @Override
    public void exec(
            final List<NodeValue> parameters,
            final Consumer<List<List<NodeValue>>> collectionListNodeValue, 
            final ExecutionControl control) {
        Binding newBinding = getNewBinding(parameters);
        CompletableFuture<Void> planExecution = exec(newBinding, (result) -> {
            List<List<NodeValue>> list = getListNodeValues(result);
            collectionListNodeValue.accept(list);
        });
        control.registerFuture(planExecution);
        control.complete();
    }

    @Override
    public void checkBuild(ExprList args) {
    }

    
    private List<List<NodeValue>> getListNodeValues(ResultSet result) {
        List<String> resultVars = result.getResultVars();
        List<List<NodeValue>> listNodeValues = new ArrayList<>();
        while (result.hasNext()) {
            List<NodeValue> nodeValues = new ArrayList<>();
            QuerySolution sol = result.next();
            for (String var : resultVars) {
                RDFNode rdfNode = sol.get(var);
                if (rdfNode != null) {
                    NodeValue n = new NodeValueNode(rdfNode.asNode());
                    nodeValues.add(n);
                } else {
                    nodeValues.add(null);
                }
            }
            listNodeValues.add(nodeValues);
        }
        return listNodeValues;
    }

    private Binding getNewBinding(final List<NodeValue> callParameters) {
        final List<Var> querySignature = select.getSignature();
        final BindingMap newBinding = BindingFactory.create();
        if (querySignature != null) {
            int max = Math.min(callParameters.size(), querySignature.size());
            if (callParameters.size() != querySignature.size()) {
                throw new ExprEvalException("The number of "
                        + "parameters (" + callParameters.size() + ") is not equal to"
                        + " the size of the signature of query " + select.getName()
                        + "(" + querySignature.size() + ").");
            }
            for (int i = 0; i < max; i++) {
                final Var parameter = querySignature.get(i);
                final NodeValue node = callParameters.get(i);
                if (node != null && node.asNode().isConcrete()) {
                    newBinding.add(parameter, node.asNode());
                }
            }
        }
        return newBinding;
    }

    private CompletableFuture<Void> exec(
            final Binding binding,
            final Consumer<ResultSet> resultStream) {
        Dataset inputDataset = getContext().get(SPARQLExt.DATASET);
        List<Var> vars = new ArrayList<>();
        for (Iterator<Var> vs = binding.vars(); vs.hasNext();) {
            vars.add(vs.next());
        }
        final List<Binding> values = new ArrayList<>();
        values.add(binding);
        BNodeMap bNodeMap = new BNodeMap();
        Context newContext = SPARQLExt.createContext(getContext());
        return plan.exec(inputDataset, vars, values, bNodeMap, newContext, null, resultStream, null);
    }
}
