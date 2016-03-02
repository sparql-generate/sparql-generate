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
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.log4j.Logger;

/**
 *
 * @author maxime.lefrancois
 */
public class GenerationPlanGenerate extends GenerationPlanBase {

    @Override
    public void $exec(Dataset inputDataset, GenerationQuerySolution initialBindings, Model initialModel) {
        Logger.getLogger(GenerationPlanGenerate.class.getName()).info("Start Generate");
        Map<Node, Node> bNodeMap = new HashMap<>();
        for (GenerationPlan subPlan : subPlans) {
            if (subPlan instanceof GenerationPlanTriples) {
                GenerationPlanTriples subPlanTriples = (GenerationPlanTriples) subPlan;
                subPlanTriples.exec(inputDataset, initialBindings, initialModel, bNodeMap);
            } else {
                subPlan.exec(inputDataset, initialBindings, initialModel);
            }
        }
    }

}
