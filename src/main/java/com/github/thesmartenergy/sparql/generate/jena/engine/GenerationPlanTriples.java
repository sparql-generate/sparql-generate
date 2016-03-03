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

import java.util.HashMap;
import java.util.Map;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
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

    private final BasicPattern bgp;

    GenerationPlanTriples(BasicPattern bgp) {
        this.bgp = bgp;
    }

    @Override
    public void $exec(Dataset inputDataset, GenerationQuerySolution initialBindings, Model initialModel) {
        exec(inputDataset, initialBindings, initialModel, new HashMap<Node, Node>());
    }

    public void exec(Dataset inputDataset, GenerationQuerySolution initialBindings, Model initialModel, Map<Node, Node> bNodeMap) {
        org.apache.log4j.Logger.getLogger(GenerationPlanSource.class.getName()).info("Generation Triples");
        for (Triple t : bgp.getList()) {
            Binding b = BindingUtils.asBinding(initialBindings);
            Triple t2 = TemplateLib.subst(t, b, bNodeMap);
            if (t2.isConcrete()) {
                Statement s = initialModel.asStatement(t2);
                initialModel.add(s);
            }
        }
    }
}
