/*
 * Copyright 2016 ITEA 12004 SEAS Project.
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
package com.github.thesmartenergy.sparql.generate.jena.query;

import java.util.Objects;
import org.apache.jena.atlas.lib.Lib;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.core.ComparisonException;
import org.apache.jena.sparql.core.Prologue;
import org.apache.jena.sparql.util.NodeIsomorphismMap;

/**
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class SPARQLGenerateQueryCompare implements SPARQLGenerateQueryVisitor {

    private SPARQLGenerateQuery query2;
    private boolean result = true;
    static public boolean PrintMessages = false;

    public static boolean equals(SPARQLGenerateQuery query1, SPARQLGenerateQuery query2) {
        if (query1 == query2) {
            return true;
        }

        query1.setResultVars();
        query2.setResultVars();
        SPARQLGenerateQueryCompare visitor = new SPARQLGenerateQueryCompare(query1);
        try {
            query2.visit(visitor);
        } catch (ComparisonException ex) {
            return false;
        }
        return visitor.isTheSame();
    }

    public SPARQLGenerateQueryCompare(SPARQLGenerateQuery query2) {
        this.query2 = query2;

    }

    @Override
    public void startVisit(Query query1) {
    }

    @Override
    public void visitResultForm(Query query1) {
        check("Query result form", query1.getQueryType() == query2.getQueryType());
    }

    @Override
    public void visitPrologue(Prologue query1) {
        check("Prefixes/Base", query1.samePrologue(query2));
    }

    @Override
    public void visitSelectResultForm(Query query1) {
        check("Not both SELECT queries", query2.isSelectType());
        check("DISTINCT modifier",
                query1.isDistinct() == query2.isDistinct());
        check("SELECT *", query1.isQueryResultStar() == query2.isQueryResultStar());
        check("Result variables", query1.getProject(), query2.getProject());
    }

    @Override
    public void visitConstructResultForm(Query query1) {
        check("Not both CONSTRUCT queries", query2.isConstructType());
        check("CONSTRUCT templates",
                query1.getConstructTemplate().equalIso(query2.getConstructTemplate(), new NodeIsomorphismMap()));
    }

    @Override
    public void visitDescribeResultForm(Query query1) {
        check("Not both DESCRIBE queries", query2.isDescribeType());
        check("Result variables",
                query1.getResultVars(), query2.getResultVars());
        check("Result URIs",
                query1.getResultURIs(), query2.getResultURIs());

    }

    @Override
    public void visitAskResultForm(Query query1) {
        check("Not both ASK queries", query2.isAskType());
    }

    @Override
    public void visitDatasetDecl(Query query1) {
        boolean b1 = Lib.equalsListAsSet(query1.getGraphURIs(), query2.getGraphURIs());
        check("Default graph URIs", b1);
        boolean b2 = Lib.equalsListAsSet(query1.getNamedGraphURIs(), query2.getNamedGraphURIs());
        check("Named graph URIs", b2);
    }

    @Override
    public void visitGenerateResultForm(SPARQLGenerateQuery query) {
        if (query.hasGenerateURI()) {
            boolean b1 = query.getGenerateURI().equals(query.getGenerateURI());
            check("Generate pattern URIs", b1);
        } else if (query.hasGenerateTemplate()) {
            boolean b2 = query.getGenerateTemplate().equals(query2.getGenerateTemplate());
            check("Generate pattern URIs", b2);
        }
    }
    
    @Override
    public void visitIteratorsAndSources(SPARQLGenerateQuery query) {
        boolean b1 = Lib.equalsListAsSet(query.getIteratorsAndSources(), query2.getIteratorsAndSources());
        check("Iterators and sources", b1);
    }

    @Override
    public void visitQueryPattern(Query query1) {
        if (query1.getQueryPattern() == null
                && query2.getQueryPattern() == null) {
            return;
        }

        if (query1.getQueryPattern() == null) {
            throw new SPARQLGenerateComparisonException("Missing pattern");
        }
        if (query2.getQueryPattern() == null) {
            throw new SPARQLGenerateComparisonException("Missing pattern");
        }

        // The checking for patterns (elements) involves a potential
        // remapping of system-allocated variable names.
        // Assumes blank node variables only appear in patterns.
        check("Pattern", query1.getQueryPattern().equalTo(query2.getQueryPattern(), new NodeIsomorphismMap()));
    }

    @Override
    public void visitGroupBy(Query query1) {
        check("GROUP BY", query1.getGroupBy(), query2.getGroupBy());
    }

    @Override
    public void visitHaving(Query query1) {
        check("HAVING", query1.getHavingExprs(), query2.getHavingExprs());
    }

    @Override
    public void visitLimit(Query query1) {
        check("LIMIT", query1.getLimit() == query2.getLimit());
    }

    @Override
    public void visitOrderBy(Query query1) {
        check("ORDER BY", query1.getOrderBy(), query2.getOrderBy());
    }

    @Override
    public void visitOffset(Query query1) {
        check("OFFSET", query1.getOffset() == query2.getOffset());
    }

    @Override
    public void visitValues(Query query1) {
        // Must be same order for now.
        check("VALUES/variables", query1.getValuesVariables(), query2.getValuesVariables());
        check("VALUES/values", query1.getValuesData(), query2.getValuesData());
    }

    @Override
    public void finishVisit(Query query1) {
    }

    private void check(String msg, Object obj1, Object obj2) {
        check(msg, Objects.equals(obj1, obj2));
    }

    private void check(String msg, boolean b) {
        if (!b) {
            if (PrintMessages && msg != null) {
                System.out.println("Different: " + msg);
            }
            result = false;
            throw new SPARQLGenerateComparisonException(msg);
        }
    }

    public boolean isTheSame() {
        return result;
    }


}
