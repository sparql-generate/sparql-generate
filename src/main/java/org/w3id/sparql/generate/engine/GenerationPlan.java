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

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;

/**
 *
 * @author maxime.lefrancois
 */
public interface GenerationPlan {

    public Model exec();

    public Model exec(Model inputModel);

    public Model exec(Dataset inputDataset);

    public void exec(GenerationQuerySolution initialBindings, Model initialModel);

    public void exec(Model inputModel, Model initialModel);

    public void exec(Model inputModel, GenerationQuerySolution initialBindings, Model initialModel);

    public void exec(Dataset inputDataset, Model initialModel);

    public void exec(Dataset inputDataset, GenerationQuerySolution initialBindings, Model initialModel);

}
