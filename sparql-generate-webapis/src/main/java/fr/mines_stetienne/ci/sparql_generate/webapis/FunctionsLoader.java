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
package fr.mines_stetienne.ci.sparql_generate.webapis;


import org.apache.jena.sparql.function.FunctionRegistry;

import fr.mines_stetienne.ci.sparql_generate.function.FunctionLoader;
import fr.mines_stetienne.ci.sparql_generate.iterator.IteratorFunctionLoader;
import fr.mines_stetienne.ci.sparql_generate.iterator.IteratorFunctionRegistry;


/**
 *
 * @author Maxime Lefrançois
 */
public class FunctionsLoader implements FunctionLoader{
	@Override
    public void load(FunctionRegistry fnreg) {
        fnreg.put(FUN_HTTPGet.URI, FUN_HTTPGet.class);
        fnreg.put(FUN_HTTPPost.URI, FUN_HTTPPost.class);
        fnreg.put(FUN_HTTPPut.URI, FUN_HTTPPut.class);
        fnreg.put(FUN_HTTPDelete.URI, FUN_HTTPDelete.class);
        fnreg.put(FUN_HTTPExtractHeader.URI, FUN_HTTPExtractHeader.class);
        fnreg.put(FUN_HTTPExtractResponseCode.URI, FUN_HTTPExtractResponseCode.class);
        fnreg.put(FUN_HTTPExtractBody.URI, FUN_HTTPExtractBody.class);
        fnreg.put(FUN_Turtle.URI, FUN_Turtle.class);
        fnreg.put(FUN_RDFXML.URI, FUN_RDFXML.class);
    }

    
}
