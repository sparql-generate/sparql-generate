/*
 * Copyright 2017 Ecole des Mines de Saint-Etienne.
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
package fr.emse.ci.sparqlext.graph;

import java.util.List;
import org.apache.jena.sparql.expr.Expr;

/**
 * Abstract class for expression nodes where the expression depends on a list of 
 * expressions.
 * 
 * @author maxime.lefrancois
 */
public abstract class Node_ExprList extends Node_Extended {
    
    /**
     * Some list of expressions
     */
    protected final List<Expr> components;

    /**
     * Constructor
     * 
     * @param components 
     */
    public Node_ExprList(List<Expr> components) {
        this.components = components;
    }   
    
    /**
     * Get the list of expressions
     * 
     * @return 
     */
    public List<Expr> getComponents() {
        return components;
    }

}
