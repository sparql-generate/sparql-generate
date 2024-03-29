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
package fr.mines_stetienne.ci.sparql_generate.json;

import fr.mines_stetienne.ci.sparql_generate.function.FunctionLoader;
import fr.mines_stetienne.ci.sparql_generate.iterator.IteratorFunctionRegistry;
import fr.mines_stetienne.ci.sparql_generate.iterator.IteratorFunctionLoader;
import org.apache.jena.sparql.function.FunctionRegistry;

/**
 *
 * @author Maxime Lefrançois
 */
public class FunctionsLoader implements FunctionLoader, IteratorFunctionLoader {
    
    @Override
    public void load(FunctionRegistry fnreg) {
        fnreg.put(FUN_JSONPath.URI, FUN_JSONPath.class);
        fnreg.put(FUN_CBOR.URI, FUN_CBOR.class);
    }
    
    @Override
    public void load(IteratorFunctionRegistry itereg) {
        itereg.put(ITER_JSONPath.URI, ITER_JSONPath.class);
        itereg.put(ITER_JSONListKeys.URI, ITER_JSONListKeys.class);
        itereg.put(ITER_CBOR.URI, ITER_CBOR.class);
        itereg.put(ITER_JSONSurfer.URI, ITER_JSONSurfer.class);
    }
    
}
