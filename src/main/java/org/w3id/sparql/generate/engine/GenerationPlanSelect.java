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

import static org.apache.jena.enhanced.BuiltinPersonalities.model;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.shared.PrefixMapping;
import org.apache.log4j.Logger;
import org.w3id.sparql.generate.query.SPARQLGenerateQuery;



/**
 *
 * @author maxime.lefrancois
 */
public class GenerationPlanSelect extends GenerationPlanBase {
    
    private SPARQLGenerateQuery select;

    GenerationPlanSelect(SPARQLGenerateQuery select) {
        if(!select.isSelectType()) {
            throw new IllegalArgumentException("Should be select query.");
        }
        this.select = select;
    }

    @Override
    public void $exec(Dataset inputDataset, GenerationQuerySolution initialBindings, Model initialModel) {
        Logger.getLogger(GenerationPlanSelect.class.getName()).info("Generation Select");
        
        PrefixMapping pm = select.getPrologue().getPrefixMapping();
        
        for(String prefix : pm.getNsPrefixMap().keySet()) {
            initialModel.setNsPrefix(prefix, pm.getNsPrefixURI(prefix));            
        }
        
        QueryExecution exec = QueryExecutionFactory.create(select, inputDataset, initialBindings);
        ResultSet results = exec.execSelect();
        while(results.hasNext()) {
            // should one instantiate a new Generation QuerySolution here, or in the sub-loop ?
            GenerationQuerySolution b = new GenerationQuerySolution(results.next());
            for(GenerationPlan plan : subPlans) {
                plan.exec(inputDataset, b, initialModel);
            }
        }
    }
        
    
}
