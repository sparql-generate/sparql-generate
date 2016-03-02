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


import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.SortCondition;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprFunction;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.util.FileManager;
import org.w3id.sparql.generate.SPARQLGenerate;
import org.w3id.sparql.generate.query.SPARQLGenerateQuery;
import org.w3id.sparql.generate.selector.Selector;
import org.w3id.sparql.generate.selector.SelectorFactory;
import org.w3id.sparql.generate.selector.SelectorRegistry;
import org.w3id.sparql.generate.syntax.ElementGenerateTriplesBlock;
import org.w3id.sparql.generate.syntax.ElementSelector;
import org.w3id.sparql.generate.syntax.ElementSelectorOrSource;
import org.w3id.sparql.generate.syntax.ElementSource;
import org.w3id.sparql.generate.syntax.ElementSubGenerate;

/**
 * A factory for generation plans. It parses a query, and establishes a plan to make the generation of RDF from a new context as fast as possible.
 * @author maxime.lefrancois
 */
public class GenerationPlanFactory {

    protected GenerationPlanFactory() {
    }

    /**
     * Create a GenerationExecutionPlan
     *
     * @param query GenerationQuery
     * @return GenerationExecutionPlan
     */
    static public GenerationPlan create(SPARQLGenerateQuery query) {
        checkArg(query);
        return make(query);
    }

    /**
     * Create a GenerationExecutionPlan
     *
     * @param queryStr Query string
     * @return GenerationExecutionPlan
     */
    static public GenerationPlan create(String queryStr) {
        checkArg(queryStr);
        return make(makeQuery(queryStr));
    }
    
    static private GenerationPlanBase make(SPARQLGenerateQuery query) {
        GenerationPlanBase plan = new GenerationPlanRoot();
        planSelectorsAndSources(plan, query);
        return plan;
    }
    
    // -----------------
    static Map<String, SelectorFactory> selectorFactories = new HashMap<>();
    

    static private void planSelectorsAndSources(GenerationPlanBase plan, SPARQLGenerateQuery query) {
        if(query.hasSelectorsAndSources()) {
            Iterator<ElementSelectorOrSource> iterator = query.getSelectorsAndSources().iterator();
            planSelectorOrSource(plan, query, iterator);
        } else {
           planSelect(plan, query);
        }
    }
    
    static private void planSelectorOrSource(GenerationPlanBase plan, SPARQLGenerateQuery query, Iterator<ElementSelectorOrSource> iterator) {
        if(iterator.hasNext()) {
            ElementSelectorOrSource selectorOrSource = iterator.next();
            if(selectorOrSource instanceof ElementSelector) {
                ElementSelector selector = (ElementSelector) selectorOrSource;
                planSelector(plan, selector, iterator, query);
            } else if(selectorOrSource instanceof ElementSource) {
                ElementSource source = (ElementSource) selectorOrSource;
                planSource(plan, source, iterator, query);
            }
        } else {
           planSelect(plan, query);
        }

    } 
    
    static private void planSelector(GenerationPlanBase plan, ElementSelector elementSelector, Iterator<ElementSelectorOrSource> iterator, SPARQLGenerateQuery query) {
        Expr expr = elementSelector.getExpr();
        Var var = elementSelector.getVar();
        if(!expr.isFunction()) {
            throw new IllegalArgumentException("Selector should be a function <iri>(...)");
        }
        ExprFunction function = expr.getFunction();
        String iri = function.getFunctionIRI();
        SelectorFactory factory;
        if( !selectorFactories.containsKey(iri) ) {
            SelectorRegistry sr = SelectorRegistry.get();
            factory = sr.get(iri);
            selectorFactories.put(iri, factory);
        } else {
            factory = selectorFactories.get(iri);
        }
        Selector selector = factory.create(iri);
        ExprList exprList = new ExprList(function.getArgs());
        selector.build(exprList); 
        GenerationPlanBase subPlan = new GenerationPlanSelector(selector, exprList, var);
        plan.addSubPlan(subPlan);
        planSelectorOrSource(subPlan, query, iterator);
    }
     
    static private void planSource(GenerationPlanBase plan, ElementSource elementSource, Iterator<ElementSelectorOrSource> iterator, SPARQLGenerateQuery query) {
        Node node = elementSource.getSource();
        String accept = elementSource.getAccept();
        Var var = elementSource.getVar();
        GenerationPlanBase subPlan = new GenerationPlanSource(node, accept, var);
        plan.addSubPlan(subPlan);
        planSelectorOrSource(plan, query, iterator);
    }
        
    static private void planSelect(GenerationPlanBase plan, SPARQLGenerateQuery query) {
        if(query.getQueryPattern()!=null) {
            SPARQLGenerateQuery select = asSelectQuery(query);
            GenerationPlanBase subPlan = new GenerationPlanSelect(select);
            plan.addSubPlan(subPlan);
            planGenerate(subPlan, query);
        } else {
            planGenerate(plan, query);        
        }
    }
    
    static private void planGenerate(GenerationPlanBase plan, SPARQLGenerateQuery query) {
        if(query.hasSource()) {
            planGenerateSource(plan, query);        
        } else if(query.hasGenerateTemplate()) {
            GenerationPlanGenerate subPlan = new GenerationPlanGenerate();
            plan.addSubPlan(subPlan);
            planGenerateTemplate(subPlan, query);
        } else {
            throw new UnsupportedOperationException("should not reach this point");
        }
    }
    
    static private void planGenerateSource(GenerationPlanBase plan, SPARQLGenerateQuery query) {
        if(!query.hasSource()) {
            throw new UnsupportedOperationException("Nothing to do");
        }
        try{ 
            String qString = IOUtils.toString(FileManager.get().open(query.getSource()));
            SPARQLGenerateQuery q = (SPARQLGenerateQuery) QueryFactory.create(qString, SPARQLGenerate.syntaxSPARQLGenerate);
            GenerationPlanRoot subPlan = new GenerationPlanRoot();
            plan.addSubPlan(subPlan);
            planSelectorsAndSources(subPlan,  q);
        } catch (IOException ex) {
            Logger.getLogger(GenerationPlanFactory.class.getName()).log(Level.SEVERE, "Error loading local SPARGL query at path "+query.getSource(), ex);
        }
    }
    
    static private void planGenerateTemplate(GenerationPlanBase plan, SPARQLGenerateQuery query) {
        if(!query.hasGenerateTemplate()) {
            throw new UnsupportedOperationException("Nothing to do");
        }
        for(Element elem : query.getGenerateTemplate().getElements()) {
            if(elem instanceof ElementGenerateTriplesBlock) {
                ElementGenerateTriplesBlock sub = (ElementGenerateTriplesBlock) elem;
                GenerationPlanTriples subPlan = new GenerationPlanTriples(sub.getPattern());
                plan.addSubPlan(subPlan);
            } else if(elem instanceof ElementSubGenerate) {
                ElementSubGenerate sub = (ElementSubGenerate) elem;
                planSelectorsAndSources(plan, sub.getQuery());
            } else {
                throw new UnsupportedOperationException("should not reach this point");
            }
        }
    }
    

    // ---------------- Internal routine

    // Make query
    static private SPARQLGenerateQuery makeQuery(String queryStr) {
        return (SPARQLGenerateQuery) QueryFactory.create(queryStr, SPARQLGenerate.syntaxSPARQLGenerate);
    }

    
    // ---- Parameter checks
    
    static private void checkNotNull(Object obj, String msg) {
        if (obj == null) {
            throw new IllegalArgumentException(msg);
        }
    }

    static private void checkArg(String parameter) {
        checkNotNull(parameter, "Parameter string is null");
    }

    static private void checkArg(SPARQLGenerateQuery query) {
        checkNotNull(query, "Query is null");
    }
    
    static private SPARQLGenerateQuery asSelectQuery(SPARQLGenerateQuery query) {
        SPARQLGenerateQuery output = new SPARQLGenerateQuery(query.getPrologue());
        output.setQuerySelectType();
        output.setQueryResultStar(true);
        output.setQueryPattern(query.getQueryPattern());
        if(query.hasGroupBy()) {
            VarExprList groupVars = query.getGroupBy();
            for( Var var : groupVars.getVars() ) {
                output.addGroupBy(var, groupVars.getExpr(var));
            }
        }
        if(query.hasHaving()) {
            for( Expr expr : query.getHavingExprs() ) {
                output.addHavingCondition(expr);
            }            
        }
        if(query.hasOrderBy()) {
            for (SortCondition sc : query.getOrderBy()) {
                output.addOrderBy(sc);
            }
        }
        if(query.hasLimit()) {
            output.setLimit(query.getLimit());
        }
        if(query.hasOffset()) {
            output.setOffset(query.getOffset());
        }
        return output;
    }

}
