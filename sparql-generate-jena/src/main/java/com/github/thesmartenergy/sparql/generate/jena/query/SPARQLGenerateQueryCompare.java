/*
 * Copyright 2016 Ecole des Mines de Saint-Etienne.
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
import org.apache.jena.sparql.core.QueryCompare;

/**
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class SPARQLGenerateQueryCompare implements SPARQLGenerateQueryVisitor {

    private SPARQLGenerateQuery query2;
    private boolean result = true;
    static public boolean PrintMessages = false;
    private QueryCompare qc;

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
        this.qc = new QueryCompare(query2);
        this.query2 = query2;
    }

    @Override
    public void startVisit(Query query1) {
    }

    @Override
    public void visitResultForm(Query query1) {
        qc.visitResultForm(query1);
    }

    @Override
    public void visitPrologue(Prologue query1) {
        qc.visitPrologue(query1);
    }

    @Override
    public void visitSelectResultForm(Query query1) {
        qc.visitSelectResultForm(query1);
    }

    @Override
    public void visitConstructResultForm(Query query1) {
        qc.visitConstructResultForm(query1);
    }

    @Override
    public void visitDescribeResultForm(Query query1) {
        qc.visitDescribeResultForm(query1);
    }

    @Override
    public void visitAskResultForm(Query query1) {
        qc.visitAskResultForm(query1);
    }

    @Override
    public void visitDatasetDecl(Query query1) {
        qc.visitDatasetDecl(query1);
    }

    @Override
    public void visitGenerateResultForm(SPARQLGenerateQuery query) {
        if (query.hasGenerateURI()) {
            boolean b1 = query.getGenerateURI().equals(query.getGenerateURI());
            try {
                check("Generate pattern URIs", b1);
            } catch(Exception e) {
                System.out.println("Exception occured " + result);
            } 
        } else if (query.hasGenerateTemplate()) {
            boolean b2 = query.getGenerateTemplate().equals(query2.getGenerateTemplate());
            try {
                check("Generate pattern URIs", b2);
            } catch(Exception e) {
                System.out.println("Exception occured " + result);
            } 
        }
    }

    @Override
    public void visitIteratorsAndSources(SPARQLGenerateQuery query) {
        boolean b1 = Lib.equalsListAsSet(query.getIteratorsAndSources(), query2.getIteratorsAndSources());
        try {
            check("Iterators and sources", b1);
        } catch(Exception e) {
            System.out.println("Exception occured " + result);
        } 
    }

    @Override
    public void visitQueryPattern(Query query1) {
        qc.visitQueryPattern(query1);
    }

    @Override
    public void visitGroupBy(Query query1) {
        qc.visitGroupBy(query1);
    }

    @Override
    public void visitHaving(Query query1) {
        qc.visitHaving(query1);
    }

    @Override
    public void visitLimit(Query query1) {
        qc.visitLimit(query1);
    }

    @Override
    public void visitOrderBy(Query query1) {
        qc.visitOrderBy(query1);
    }

    @Override
    public void visitOffset(Query query1) {
        qc.visitOffset(query1);
    }

    @Override
    public void visitValues(Query query1) {
        qc.visitValues(query1);
    }

    @Override
    public void finishVisit(Query query1) {
    }

    private void check(String msg, Object obj1, Object obj2) throws Exception {
        check(msg, Objects.equals(obj1, obj2));
    }

    private void check(String msg, boolean b) throws Exception {
        if (!b) {
            if (PrintMessages && msg != null) {
                System.out.println("Different: " + msg);
            }
            result = false;
            throw new Exception(msg);
        }
    }

    public boolean isTheSame() {
        return result;
    }

}
