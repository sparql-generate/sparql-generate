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

import org.apache.jena.graph.NodeVisitor;

/**
 * Extension of NodeVisitor to account for the three new types of nodes.
 * 
 * @author maxime.lefrancois
 */
public interface SPARQLExtNodeVisitor extends NodeVisitor {

    Object visit(Node_Expr node);

    Object visit(Node_ExtendedLiteral node);

    Object visit(Node_ExtendedURI node);
    
    Object visit(Node_Template node);
    
    Object visit(Node_List node);
    
}
