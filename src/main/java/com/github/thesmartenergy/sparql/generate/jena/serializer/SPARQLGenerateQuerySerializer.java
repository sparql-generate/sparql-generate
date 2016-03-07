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
package com.github.thesmartenergy.sparql.generate.jena.serializer;

import java.io.OutputStream;
import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryVisitor;
import org.apache.jena.sparql.core.Prologue;
import org.apache.jena.sparql.serializer.FmtExprSPARQL;
import org.apache.jena.sparql.serializer.FormatterTemplate;
import com.github.thesmartenergy.sparql.generate.jena.query.SPARQLGenerateQuery;
import com.github.thesmartenergy.sparql.generate.jena.syntax.ElementIterator;
import com.github.thesmartenergy.sparql.generate.jena.syntax.ElementIteratorOrSource;
import com.github.thesmartenergy.sparql.generate.jena.syntax.ElementSource;

/**
 * Extends the ARQ Query Serializer with SPARQL Generate specificities.
 * 
 * @author maxime.lefrancois
 */
public class SPARQLGenerateQuerySerializer implements com.github.thesmartenergy.sparql.generate.jena.query.SPARQLGenerateQueryVisitor {

    public final int BLOCK_INDENT = 2;
    private final QueryVisitor decorated;
    private final IndentedWriter out;
    private final SPARQLGenerateFormatterElement fmtElement;
    private final FmtExprSPARQL fmtExpr;
    private final FormatterTemplate fmtTemplate;

    public SPARQLGenerateQuerySerializer(QueryVisitor serializer, OutputStream _out, SPARQLGenerateFormatterElement formatterElement, FmtExprSPARQL formatterExpr, FormatterTemplate formatterTemplate) {
        this(serializer, new IndentedWriter(_out), formatterElement, formatterExpr, formatterTemplate);
    }

    public SPARQLGenerateQuerySerializer(QueryVisitor serializer, IndentedWriter iwriter, SPARQLGenerateFormatterElement formatterElement, FmtExprSPARQL formatterExpr, FormatterTemplate formatterTemplate) {
        decorated = serializer;
        out = iwriter;
        fmtTemplate = formatterTemplate;
        fmtElement = formatterElement;
        fmtExpr = formatterExpr;
    }

    @Override
    public void visitGenerateResultForm(com.github.thesmartenergy.sparql.generate.jena.query.SPARQLGenerateQuery query) {
        out.print("GENERATE ");
        if (query.hasGenerateURI()) {
            out.print(" ");
            out.print("<" + query.getGenerateURI() + ">");
            out.newline();
        } else if (query.hasGenerateTemplate()) {
            out.newline();
            out.incIndent(BLOCK_INDENT);
            fmtElement.visit(query.getGenerateTemplate());
            out.decIndent(BLOCK_INDENT);
            out.newline();
        }
    }
    
    @Override
    public void visitIteratorsAndSources(SPARQLGenerateQuery query) {
        if(query.getIteratorsAndSources() == null) {
            return;
        }
        for (ElementIteratorOrSource iteratorOrSource : query.getIteratorsAndSources()) {
            if(iteratorOrSource instanceof ElementIterator) {
                fmtElement.visit((ElementIterator) iteratorOrSource);
            } else if(iteratorOrSource instanceof ElementSource) {
                fmtElement.visit((ElementSource) iteratorOrSource);
            }
            out.newline();
        }
    }

    @Override
    public void startVisit(Query query) {
        decorated.startVisit(query);
    }

    @Override
    public void visitPrologue(Prologue prologue) {
        decorated.visitPrologue(prologue);
    }

    @Override
    public void visitResultForm(Query query) {
        decorated.visitResultForm(query);
    }

    @Override
    public void visitSelectResultForm(Query query) {
        decorated.visitSelectResultForm(query);
    }

    @Override
    public void visitConstructResultForm(Query query) {
        decorated.visitConstructResultForm(query);
    }

    @Override
    public void visitDescribeResultForm(Query query) {
        decorated.visitDescribeResultForm(query);
    }

    @Override
    public void visitAskResultForm(Query query) {
        decorated.visitAskResultForm(query);
    }

    @Override
    public void visitDatasetDecl(Query query) {
        decorated.visitDatasetDecl(query);
    }

    @Override
    public void visitQueryPattern(Query query) {
        decorated.visitQueryPattern(query);
    }

    @Override
    public void visitGroupBy(Query query) {
        decorated.visitGroupBy(query);
    }

    @Override
    public void visitHaving(Query query) {
        decorated.visitHaving(query);
    }

    @Override
    public void visitOrderBy(Query query) {
        decorated.visitOrderBy(query);
    }

    @Override
    public void visitLimit(Query query) {
        decorated.visitLimit(query);
    }

    @Override
    public void visitOffset(Query query) {
        //FIXME appeared in double
//        decorated.visitOffset(query);
    }

    @Override
    public void visitValues(Query query) {
        decorated.visitOffset(query);
    }

    @Override
    public void finishVisit(Query query) {
        decorated.finishVisit(query);
    }

}
