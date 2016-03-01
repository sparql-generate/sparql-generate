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
package org.w3id.sparql.generate.engine;


import java.util.List;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingUtils;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.w3id.sparql.generate.selector.Selector;

/**
 *
 * @author maxime.lefrancois
 */
public class GenerationPlanSelector extends GenerationPlanBase {
    
    private final Selector selector;
    private final ExprList exprList;
    
    GenerationPlanSelector(Selector selector, ExprList exprList) {
        this.selector = selector;
        this.exprList = exprList;
    }
    
    @Override
    public void exec(Model model, QuerySolution binding) {
        System.out.println("Selector - " + binding);
        Binding b = BindingUtils.asBinding(binding);
        try{
            List<NodeValue> messages = selector.exec(b, exprList, null);
            for(NodeValue message : messages) {
                GenerationQuerySolution subBinding = new GenerationQuerySolution(binding);
                subBinding.put("msg", model.asRDFNode(message.asNode()));
                for(GenerationPlan plan : subPlans) {
                    plan.exec(model, subBinding);
                }
            }
        } catch (ExprEvalException e) {
            for(GenerationPlan plan : subPlans) {
                plan.exec(model, binding);
            }
        }
    }
    
}
