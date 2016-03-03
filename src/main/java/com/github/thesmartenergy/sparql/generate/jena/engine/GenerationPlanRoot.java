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

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.log4j.Logger;

/**
 *
 * @author maxime.lefrancois
 */
public class GenerationPlanRoot extends GenerationPlanBase {

    @Override
    public void $exec(Dataset inputDataset, GenerationQuerySolution initialBindings, Model initialModel) {
        Logger.getLogger(GenerationPlanRoot.class.getName()).info("Generation Root");
        for(GenerationPlan subPlan : subPlans) {
            subPlan.exec(inputDataset, initialBindings, initialModel);
        }
    }
    
}
