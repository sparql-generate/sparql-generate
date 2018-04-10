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
package com.github.thesmartenergy.sparql.generate.jena.query;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryVisitor;
import org.apache.jena.sparql.core.Prologue;

/**
 *
 * @author maxime.lefrancois
 */
public class SPARQLGenerateQueryVisitorDelegate implements SPARQLGenerateQueryVisitor {

    final QueryVisitor delegate;

    public SPARQLGenerateQueryVisitorDelegate(QueryVisitor delegate) {
        this.delegate = delegate;
    }

    @Override
    public void visitGenerateResultForm(SPARQLGenerateQuery query) {
        if (delegate instanceof SPARQLGenerateQueryVisitor) {
            ((SPARQLGenerateQueryVisitor) delegate).visitGenerateResultForm(query);
        }
    }

    @Override
    public void visitIteratorsAndSources(SPARQLGenerateQuery query) {
        if (delegate instanceof SPARQLGenerateQueryVisitor) {
            ((SPARQLGenerateQueryVisitor) delegate).visitIteratorsAndSources(query);
        }
    }

    @Override
    public void startVisit(Query query) {
        delegate.startVisit(query);
    }

    @Override
    public void visitPrologue(Prologue prologue) {
        delegate.visitPrologue(prologue);
    }

    @Override
    public void visitResultForm(Query query) {
        delegate.visitResultForm(query);
    }

    @Override
    public void visitSelectResultForm(Query query) {
        delegate.visitSelectResultForm(query);
    }

    @Override
    public void visitConstructResultForm(Query query) {
        delegate.visitConstructResultForm(query);
    }

    @Override
    public void visitDescribeResultForm(Query query) {
        delegate.visitDescribeResultForm(query);
    }

    @Override
    public void visitAskResultForm(Query query) {
        delegate.visitAskResultForm(query);
    }

    @Override
    public void visitDatasetDecl(Query query) {
        delegate.visitDatasetDecl(query);
    }

    @Override
    public void visitQueryPattern(Query query) {
        delegate.visitQueryPattern(query);
    }

    @Override
    public void visitGroupBy(Query query) {
        delegate.visitGroupBy(query);
    }

    @Override
    public void visitHaving(Query query) {
        delegate.visitHaving(query);
    }

    @Override
    public void visitOrderBy(Query query) {
        delegate.visitOrderBy(query);
    }

    @Override
    public void visitLimit(Query query) {
        delegate.visitLimit(query);
    }

    @Override
    public void visitOffset(Query query) {
        delegate.visitOffset(query);
    }

    @Override
    public void visitValues(Query query) {
        delegate.visitValues(query);
    }

    @Override
    public void finishVisit(Query query) {
        delegate.finishVisit(query);
    }

}
