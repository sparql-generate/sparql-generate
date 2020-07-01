/*
 * Copyright 2020 Ã‰cole des Mines de Saint-Ã‰tienne.
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
package fr.emse.ci.sparqlext.sql;

import fr.emse.ci.sparqlext.iterator.IteratorFunctionRegistry;
import fr.emse.ci.sparqlext.iterator.IteratorFunctionLoader;

/**
 * @author Omar Qawasmeh, Maxime Lefrançois
 * 
 * @organization Ecole des Mines de Saint Etienne
 */
public class FunctionsLoader implements IteratorFunctionLoader {
    
    @Override
    public void load(IteratorFunctionRegistry itereg) {
        itereg.put(ITER_SQL.URI, ITER_SQL.class);
    }
    
}
