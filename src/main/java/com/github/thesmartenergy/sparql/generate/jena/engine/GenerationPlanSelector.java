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
package com.github.thesmartenergy.sparql.generate.jena.engine;


import java.util.List;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingUtils;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.log4j.Logger;
import com.github.thesmartenergy.sparql.generate.jena.selector.Selector;

/**
 *
 * @author maxime.lefrancois
 */
public class GenerationPlanSelector extends GenerationPlanBase {
    
    private final Selector selector;
    private final ExprList exprList;
    private final Var var;
    
    GenerationPlanSelector(Selector selector, ExprList exprList, Var var) {
        this.selector = selector;
        this.exprList = exprList;
        this.var = var;
    }
    
    @Override
    public void $exec(Dataset inputDataset, GenerationQuerySolution initialBindings, Model initialModel) {
        Logger.getLogger(GenerationPlanSelector.class.getName()).info("Generation Selector " + selector.toString());
        Binding b = BindingUtils.asBinding(initialBindings);
        try{
            List<NodeValue> messages = selector.exec(b, exprList, null);
            for(NodeValue message : messages) {
                GenerationQuerySolution subBinding = new GenerationQuerySolution(initialBindings);
                subBinding.put(var.getVarName(), initialModel.asRDFNode(message.asNode()));
                for(GenerationPlan plan : subPlans) {
                    plan.exec(inputDataset, subBinding, initialModel);
                }
            }
        } catch (ExprEvalException e) {
            for(GenerationPlan plan : subPlans) {
                plan.exec(inputDataset, initialBindings, initialModel);
            }
        }
    }
    
}
