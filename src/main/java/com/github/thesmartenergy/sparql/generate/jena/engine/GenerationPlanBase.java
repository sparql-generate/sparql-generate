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

import java.util.ArrayList;
import java.util.List;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

/**
 * One execution
 *
 * @author maxime.lefrancois
 */
abstract class GenerationPlanBase implements GenerationPlan {

    protected List<GenerationPlan> subPlans = new ArrayList<>();

    @Override
    public final Model exec() {
        Model initialModel = ModelFactory.createDefaultModel();
        exec((Dataset) null, null, initialModel);
        return initialModel;
    }

    @Override
    public Model exec(Model inputModel) {
        if (inputModel == null) {
            inputModel = ModelFactory.createDefaultModel();
        }
        Model initialModel = ModelFactory.createDefaultModel();
        exec(inputModel, null, initialModel);
        return initialModel;
    }

    @Override
    public Model exec(Dataset inputDataset) {
        if (inputDataset == null) {
            inputDataset = DatasetFactory.create();
        }
        Model initialModel = ModelFactory.createDefaultModel();
        exec(inputDataset, null, initialModel);
        return initialModel;
    }

    @Override
    public void exec(GenerationQuerySolution initialBindings, Model initialModel) {
        if (initialBindings == null) {
            initialBindings = new GenerationQuerySolution();
        }
        if (initialModel == null) {
            initialModel = ModelFactory.createDefaultModel();
        }
        exec((Dataset) null, initialBindings, initialModel);
    }

    @Override
    public void exec(Model inputModel, Model initialModel) {
        if (inputModel == null) {
            inputModel = ModelFactory.createDefaultModel();
        }
        if (initialModel == null) {
            initialModel = ModelFactory.createDefaultModel();
        }
        exec(inputModel, null, initialModel);
    }

    @Override
    public void exec(Dataset inputDataset, Model initialModel) {
        if (inputDataset == null) {
            inputDataset = DatasetFactory.create();
        }
        if (initialModel == null) {
            initialModel = ModelFactory.createDefaultModel();
        }
        GenerationQuerySolution initialBindings = new GenerationQuerySolution();
        exec(inputDataset, initialBindings, initialModel);
    }

    @Override
    public void exec(Model inputModel, GenerationQuerySolution initialBindings, Model initialModel) {
        if (inputModel == null) {
            inputModel = ModelFactory.createDefaultModel();
        }
        if (initialBindings == null) {
            initialBindings = new GenerationQuerySolution();
        }
        if (initialModel == null) {
            initialModel = ModelFactory.createDefaultModel();
        }
        exec(DatasetFactory.create(inputModel), initialBindings, initialModel);
    }

    @Override
    public final void exec(Dataset inputDataset, GenerationQuerySolution initialBindings, Model initialModel) {
        if (inputDataset == null) {
            inputDataset = DatasetFactory.create(ModelFactory.createDefaultModel());
        }
        if (initialBindings == null) {
            initialBindings = new GenerationQuerySolution();
        }
        if (initialModel == null) {
            initialModel = ModelFactory.createDefaultModel();
        }
        $exec(inputDataset, initialBindings, initialModel);
    }

    protected abstract void $exec(Dataset inputDataset, GenerationQuerySolution initialBindings, Model initialModel);

    void addSubPlan(GenerationPlan plan) {
        subPlans.add(plan);
    }

}
