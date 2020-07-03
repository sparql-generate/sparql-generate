/*
 * Copyright 2020 MINES Saint-Étienne
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
package fr.emse.ci.sparqlext.utils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.engine.binding.BindingMap;

/**
 *
 * @author Maxime Lefrançois
 */
public class VarUtils {

    private static final Cache<String, Var> CACHE = CacheBuilder.newBuilder().recordStats().maximumSize(500_000).build();

    public static Var allocVar(String label) {
        Var var = CACHE.getIfPresent(label);
        if (var == null) {
            var = Var.alloc(label);
            CACHE.put(label, var);
        }
        return var;
    }

    public static List<Var> getVariables(
            final QuerySolution sol) {
        final List<Var> variables = new ArrayList<>();
        for (Iterator<String> it = sol.varNames(); it.hasNext();) {
            variables.add(allocVar(it.next()));
        }
        return variables;
    }

    public static Binding getBinding(
            final QuerySolution sol) {
        final BindingMap binding = BindingFactory.create();
        for (Iterator<String> it = sol.varNames(); it.hasNext();) {
            String varName = it.next();
            binding.add(allocVar(varName), sol.get(varName).asNode());
        }
        return binding;
    }

}
