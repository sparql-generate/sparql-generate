/*
 * Copyright 2018 École des Mines de Saint-Étienne.
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
package com.github.thesmartenergy.sparql.generate.jena.engine.impl;

import com.github.thesmartenergy.sparql.generate.jena.engine.ExecutionContext;
import java.util.HashMap;
import java.util.Map;
import org.apache.jena.sparql.core.Var;

/**
 *
 * @author maxime.lefrancois
 */
public class ExecutionContextImpl implements ExecutionContext {
    
    final Map<String,Var> vars = new HashMap<>();

    public ExecutionContextImpl() {
        
    }
    // stocker ce qui est associé à une execution de requête:
    // les variables ? BindingHashMapOverwrite:77 changer Var.alloc  par ExecutiunoContextImpl.alloc
    // les variables ? SelectPlanImpl :99 eviter Var.alloc par ExecutiunoContextImpl.alloc

    @Override
    public Var allocVar(final String label) {
        if(vars.containsKey(label)) {
            return vars.get(label);
        }
        final Var var = Var.alloc(label);
        vars.put(label, var);
        return var;
    }
     
}
