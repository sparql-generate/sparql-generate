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


import java.util.HashMap;
import java.util.Map;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingUtils;
import org.apache.jena.sparql.modify.TemplateLib;

/**
 * A simple GenerationPlan where only triples will be generated
 *
 * @author maxime.lefrancois
 */
public class GenerationPlanTriples extends GenerationPlanBase {

    private BasicPattern bgp;

    GenerationPlanTriples(BasicPattern bgp) {
        this.bgp = bgp;
    }

    @Override
    public void exec(Model model, QuerySolution binding) {
        exec(model, binding, new HashMap<Node, Node>());
    }

    public void exec(Model model, QuerySolution binding, Map<Node, Node> bNodeMap) {
        System.out.println("Triples - " + binding);
        for (Triple t : bgp.getList()) {
            Binding b = BindingUtils.asBinding(binding);
            Triple t2 = TemplateLib.subst(t, b, bNodeMap);
            if( t2.isConcrete()) {
                Statement s = model.asStatement(t2);            
                model.add(s);
            }
        }
    }
}
