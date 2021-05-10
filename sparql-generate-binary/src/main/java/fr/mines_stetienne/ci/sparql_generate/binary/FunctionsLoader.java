/*
 * Copyright 2021 Ecole des Mines de Saint-Ã‰tienne.
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
package fr.mines_stetienne.ci.sparql_generate.binary;

import fr.mines_stetienne.ci.sparql_generate.iterator.IteratorFunctionRegistry;
import fr.mines_stetienne.ci.sparql_generate.iterator.IteratorFunctionLoader;

import org.apache.jena.sparql.function.FunctionRegistry;

import fr.mines_stetienne.ci.sparql_generate.function.FunctionLoader;

/**
 *
 * @author Maxime Lefrançois
 */
public class FunctionsLoader implements IteratorFunctionLoader, FunctionLoader {
    
    @Override
    public void load(IteratorFunctionRegistry itereg) {
    }
    
    @Override
    public void load(FunctionRegistry funreg) {
    	funreg.put(FUN_BitwiseNot.URI, FUN_BitwiseNot.class);
    	funreg.put(FUN_BitwiseAnd.URI, FUN_BitwiseAnd.class);
    	funreg.put(FUN_BitwiseOr.URI, FUN_BitwiseOr.class);
    	funreg.put(FUN_BitwiseXOR.URI, FUN_BitwiseXOR.class);
    	funreg.put(FUN_BitwiseLeftShift.URI, FUN_BitwiseLeftShift.class);
    	funreg.put(FUN_BitwiseRightShift.URI, FUN_BitwiseRightShift.class);
    	funreg.put(FUN_Base10.URI, FUN_Base10.class);
    }
    
}
