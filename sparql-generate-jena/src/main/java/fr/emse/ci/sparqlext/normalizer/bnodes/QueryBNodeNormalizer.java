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
package fr.emse.ci.sparqlext.normalizer.bnodes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.expr.E_BNode;
import org.apache.jena.sparql.syntax.Element;

import com.google.common.collect.Sets;

import fr.emse.ci.sparqlext.SPARQLExtException;
import fr.emse.ci.sparqlext.query.SPARQLExtQuery;
import fr.emse.ci.sparqlext.syntax.ElementGenerateTriplesBlock;
import fr.emse.ci.sparqlext.syntax.ElementSubExtQuery;
import fr.emse.ci.sparqlext.utils.VarUtils;

/**
 *
 * @author Maxime Lefrançois
 */
public class QueryBNodeNormalizer {
    /// 

    // when there is a bnode with same label "bn" in a query and a sub*-query, 
    //  replace all of them by a fresh variable ?bn, 
    //  add (bnode() as ?bn) to the project vars of the outer query
    //  add add ?bn to the query parameters of each sub-query
    public static void normalizeCallParameters(SPARQLExtQuery query) {
        if (query.isSubQuery()) {
            throw new SPARQLExtException("Normalize call parameters only for main queries");
        }
        normalizeBNodes(query, new HashMap<>());
        query.getFromClauses().forEach((fromClause) -> {
            if (fromClause.getGenerate() != null) {
            	normalizeBNodes(fromClause.getGenerate(), new HashMap<>());
            }
        });

    }

    /**
     *
     * @param query
     * @param assignSuper assignments already defined in the super queries
     * (bnodes present in the super queries
     * @return the set of bnodes labels that are present in this query or the
     * sub queries
     */
    public static Set<String> normalizeBNodes(SPARQLExtQuery query, Map<String, Var> assignSuper) {
        if (!query.isGenerateType() || !query.hasGenerateClause()) {
            return null;
        }

        // the labels in assign needed are the ones that need to be replaced by
        // a varible assigned to bnode(), either in this query or in a superquery.
        Set<String> assignNeeded = new HashSet<>();

        // check the bnodes present in the BGPs of this query.
        // if already present in a superquery (assignSuper), then the assignment is needed.
        // else, check an assignment has been created
        Map<String, Var> assign = new HashMap<>();
        for (Element elem : query.getGenerateClause()) {
            if (elem instanceof ElementGenerateTriplesBlock) {
                ElementGenerateTriplesBlock sub
                        = (ElementGenerateTriplesBlock) elem;
                for (Triple t : sub.getPattern().getList()) {
                    processBNode(t.getSubject(), assignNeeded, assignSuper, assign);
                    processBNode(t.getPredicate(), assignNeeded, assignSuper, assign);
                    processBNode(t.getObject(), assignNeeded, assignSuper, assign);

                }
            }
        }

        // this gives a new assignSuper to pass to the subqueries
        Map<String, Var> newAssignSuper = new HashMap<>(assignSuper);
        newAssignSuper.putAll(assign);
        for (Element elem : query.getGenerateClause()) {
            if (elem instanceof ElementSubExtQuery) {
                SPARQLExtQuery sub = ((ElementSubExtQuery) elem).getQuery();
                Set<String> normalized = normalizeBNodes(sub, newAssignSuper);
                if(normalized != null) {
                    assignNeeded.addAll(normalized);
                }
            }
        }

        // check the bnodes present in the BGPs of this query.
        // if already present in a superquery (assignSuper), then the assignment is needed.
        // else, check an assignment has been created
        for (int i = 0; i < query.getGenerateClause().size(); i++) {
            Element elem = query.getGenerateClause().get(i);
            if (elem instanceof ElementGenerateTriplesBlock) {
                BasicPattern bgp = new BasicPattern();
                for (Iterator<Triple> it = ((ElementGenerateTriplesBlock) elem).patternElts(); it.hasNext();) {
                    Triple t = it.next();
                    Node s = replace(t.getSubject(), assignNeeded, assignSuper, assign);
                    Node p = replace(t.getPredicate(), assignNeeded, assignSuper, assign);
                    Node o = replace(t.getObject(), assignNeeded, assignSuper, assign);
                    bgp.add(new Triple(s, p, o));
                }
                Element newElem = new ElementGenerateTriplesBlock(bgp);
                query.getGenerateClause().set(i, newElem);
            }
        }

        // the labels in assignNeeded that are also keys of assign need to be 
        // replaced by a varible assigned to bnode() in this query 
        final VarExprList postSelect = query.getPostSelect();
        for (String label : Sets.intersection(assignNeeded, assign.keySet())) {
            postSelect.add(assign.get(label), new E_BNode());
        }

        // the labels in assignNeeded that are also keys of assignSuper need to be 
        // replaced by a varible assigned to bnode() in a superquery.
        return Sets.intersection(assignNeeded, assignSuper.keySet());
    }

    private static void processBNode(Node node, Set<String> assignNeeded, Map<String, Var> assignSuper, Map<String, Var> assign) {
        if (!node.isBlank()) {
            return;
        }
        String label = node.getBlankNodeLabel();
        if (assignSuper.containsKey(label)) {
            assignNeeded.add(label);
            return;
        }
        if (!assign.containsKey(label)) {
            String uuid = UUID.randomUUID().toString().substring(0, 8);
            Var var = VarUtils.allocVar(uuid);
            assign.put(label, var);
        }
    }

    private static Node replace(Node node, Set<String> assignNeeded, Map<String, Var> assignSuper, Map<String, Var> assign) {
        if (!node.isBlank()) {
            return node;
        }
        String label = node.getBlankNodeLabel();
        if (!assignNeeded.contains(label)) {
            return node;
        }
        if (assignSuper.containsKey(label)) {
            return assignSuper.get(label);
        }
        if (assign.containsKey(label)) {
            return assign.get(label);
        }
        return node;
    }

}
