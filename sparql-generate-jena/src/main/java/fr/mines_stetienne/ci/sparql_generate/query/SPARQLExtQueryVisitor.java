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
package fr.mines_stetienne.ci.sparql_generate.query;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExtException;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryVisitor;

/**
 * Extends the ARQ query visitor for elements specific to SPARQL-Ext queries.
 *
 * @author Maxime Lefrançois
 */
public interface SPARQLExtQueryVisitor extends QueryVisitor {

    /**
     * Visits the {@code GENERATE} clause.
     *
     * @param query the SPARQL-Ext query.
     */
    public void visitGenerateClause(SPARQLExtQuery query);

    /**
     * Visits the {@code TEMPLATE} clause.
     *
     * @param query the SPARQL-Ext query.
     */
    public void visitTemplateClause(SPARQLExtQuery query);

    /**
     * Visits the {@code FUNCTION} clause.
     *
     * @param query the SPARQL-Ext query.
     */
    public void visitFunctionExpression(SPARQLExtQuery query);

    /**
     * Visits the {@code PERFORM} clause.
     *
     * @param query the SPARQL-Ext query.
     */
    public void visitPerformClause(SPARQLExtQuery query);

    /**
     * Visits all the binding clauses like {@code ITERATOR} and {@code SOURCE} .
     *
     * @param query the SPARQL-Ext query.
     */
    public void visitBindingClauses(SPARQLExtQuery query);

    /**
     * Visits the select clause after the where.
     *
     * @param query the SPARQL-Ext query.
     */
    public void visitPostSelect(SPARQLExtQuery query);


    /**
     * Visits the pragma clause.
     *
     * @param query the SPARQL-Ext query.
     */
    public void visitPragma(SPARQLExtQuery query);
    
    
    public default SPARQLExtQuery asSPARQLExtQuery(Query q)  {
        if (!(q instanceof SPARQLExtQuery)) {
            throw new SPARQLExtException("Expecting an instance of type SPARQLExtQuery:" + q);
        }
        return (SPARQLExtQuery) q;
    }

}
