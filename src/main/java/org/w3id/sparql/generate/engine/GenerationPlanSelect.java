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

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
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
    public void exec(Model model, QuerySolution binding) {
//        System.out.println("Select - " + binding);
        model.setNsPrefixes(select.getPrologue().getPrefixMapping());
        QueryExecution exec = QueryExecutionFactory.create(select, ModelFactory.createDefaultModel(), binding);
        ResultSet results = exec.execSelect();
        while(results.hasNext()) {
            QuerySolution b = results.next();
            for(GenerationPlan plan : subPlans) {
                plan.exec(model, b);
            }
        }
    }
        
    
}
