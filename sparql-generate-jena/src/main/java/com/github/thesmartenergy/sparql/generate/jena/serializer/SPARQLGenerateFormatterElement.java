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

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.sparql.serializer.FmtExprSPARQL;
import org.apache.jena.sparql.serializer.FormatterElement;
import org.apache.jena.sparql.serializer.QuerySerializerFactory;
import org.apache.jena.sparql.serializer.SerializationContext;
import org.apache.jena.sparql.serializer.SerializerRegistry;
import org.apache.jena.sparql.util.FmtUtils;
import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerate;
import com.github.thesmartenergy.sparql.generate.jena.query.SPARQLGenerateQuery;
import com.github.thesmartenergy.sparql.generate.jena.query.SPARQLGenerateQueryVisitor;
import com.github.thesmartenergy.sparql.generate.jena.syntax.ElementGenerateTriplesBlock;
import com.github.thesmartenergy.sparql.generate.jena.syntax.ElementIterator;
import com.github.thesmartenergy.sparql.generate.jena.syntax.ElementSource;
import com.github.thesmartenergy.sparql.generate.jena.syntax.ElementSubGenerate;
import com.github.thesmartenergy.sparql.generate.jena.syntax.SPARQLGenerateElementVisitor;

/**
 * Extends the ARQ Element Formatter with SPARQL Generate elements.
 * 
 * @author maxime.lefrancois
 */
public class SPARQLGenerateFormatterElement extends FormatterElement implements SPARQLGenerateElementVisitor {
    
    /**
     * 
     * @param out
     * @param context 
     */
    public SPARQLGenerateFormatterElement(IndentedWriter out, SerializationContext context) {
        super(out, context);
    }

    @Override
    public void visit(ElementGenerateTriplesBlock el) {
        if (el.isEmpty()) {
            out.println("# Empty BGGP");
            return;
        }
        formatTriples(el.getPattern());
    }

    @Override
    public void visit(ElementSubGenerate el) {
        out.incIndent(INDENT);
        SPARQLGenerateQuery q = el.getQuery();

        if (!q.isGenerateType()) {
            throw new IllegalArgumentException("SubGenerate Query must be a generate query");
        }

        QuerySerializerFactory factory = SerializerRegistry.get().getQuerySerializerFactory(SPARQLGenerate.SYNTAX);
        SPARQLGenerateQueryVisitor visitor = (SPARQLGenerateQueryVisitor) factory.create(SPARQLGenerate.SYNTAX, q.getPrologue(), out);

        visitor.startVisit(q);
        visitor.visitGenerateResultForm(q);
        visitor.visitIteratorsAndSources(q);
        visitor.visitQueryPattern(q);
        visitor.visitGroupBy(q);
        visitor.visitHaving(q);
        visitor.visitOrderBy(q);
        visitor.visitOffset(q);
        visitor.visitLimit(q);
        visitor.visitValues(q);
        visitor.finishVisit(q);

        out.print(" .");
        out.decIndent(INDENT);
    }

    @Override
    public void visit(ElementIterator el) {
        FmtExprSPARQL v = new FmtExprSPARQL(out, context) ;
        out.print("ITERATOR ");
        v.format(el.getExpr()) ;        
        out.print(" AS " + el.getVar());
    }

    @Override
    public void visit(ElementSource el) {
        out.print("SOURCE ");
        out.print(FmtUtils.stringForNode(el.getSource(), context.getPrologue()));
        if(el.getAccept()!=null) {
            out.print(" ACCEPT \"" + el.getAccept() + "\"");
        }
        out.print(" AS " + el.getVar());
    }

}
