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


import java.util.ArrayList;
import java.util.List;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;

/**
 * One execution 
 * @author maxime.lefrancois
 */
abstract class GenerationPlanBase implements GenerationPlan {

    protected List<GenerationPlan> subPlans = new ArrayList<>();
    
    static private RDFNode makeMessage(Model model, String message, String messageDatatypeIRI) {
        if(messageDatatypeIRI==null) {
            return model.createLiteral(message);
        }
        return model.createTypedLiteral(message, messageDatatypeIRI);
    }

    @Override
    public final Model exec(String message, String messageDatatypeIRI) {
        Model model = ModelFactory.createDefaultModel();
        exec(model, message, messageDatatypeIRI);
        return model;
    }

    @Override
    public final Model exec(RDFNode message) {
        Model model = ModelFactory.createDefaultModel();
        exec(model, message);
        return model;
    }

    @Override
    public final void exec(Model model, String message, String messageDatatypeIRI) {
        exec(model, makeMessage(model, message, messageDatatypeIRI));
    }

    @Override
    public final void exec(Model model, RDFNode message) {
        QuerySolution binding = new GenerationQuerySolution(message);
        exec(model, binding);
    }

    @Override
    public abstract void exec(Model model, QuerySolution binding);
    
    void addSubPlan(GenerationPlan plan) {
        subPlans.add(plan);
    }

}
