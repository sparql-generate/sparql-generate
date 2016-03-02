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


import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.core.QuerySolutionBase;
import org.apache.jena.sparql.core.Var;

/**
 *
 * @author maxime.lefrancois
 */
public class GenerationQuerySolution extends QuerySolutionBase {

    protected QuerySolution parent = null;
    protected Map<String, RDFNode> map = new HashMap<>();

    public GenerationQuerySolution() {
    }

    public GenerationQuerySolution(QuerySolution parent) {
        this.parent = parent;
    }
    
    public void put(String varName, RDFNode node) {
        map.put(varName, node);
    }

    public GenerationQuerySolution(Var var, RDFNode message) {
        put(var.getName(), message);
    }

    @Override
    protected RDFNode _get(String varName) {
        RDFNode n = map.get(varName);
        if (n != null) {
            return n;
        }
        if (parent != null) {
            return parent.get(varName);
        }
        return null;
    }

    @Override
    protected boolean _contains(String varName) {
        if (map.containsKey(varName)) {
            return true;
        }
        if (parent != null && parent.contains(varName)) {
            return true;
        }
        return false;
    }

    @Override
    public Iterator<String> varNames() {
        Set<String> varNames = new HashSet<>(map.keySet());
        if(parent != null) {
            Iterator<String> it = parent.varNames();
            while(it.hasNext()) {
                String n = it.next();
                varNames.add(n);
            }
        }
        return varNames.iterator();
    }

}
