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
package org.w3id.sparql.generate.serializer;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.sparql.serializer.FormatterElement;
import org.apache.jena.sparql.serializer.QuerySerializerFactory;
import org.apache.jena.sparql.serializer.SerializationContext;
import org.apache.jena.sparql.serializer.SerializerRegistry;
import org.w3id.sparql.generate.query.SPARQLGenerateQuery;
import org.w3id.sparql.generate.query.SPARQLGenerateQueryVisitor;
import org.w3id.sparql.generate.query.SPARQLGenerateSyntax;
import org.w3id.sparql.generate.syntax.ElementGenerateTriplesBlock;
import org.w3id.sparql.generate.syntax.ElementSubGenerate;
import org.w3id.sparql.generate.syntax.SPARQLGenerateElementVisitor;

/**
 *
 * @author maxime.lefrancois
 */
public class SPARQLGenerateFormatterElement extends FormatterElement implements SPARQLGenerateElementVisitor {


    public SPARQLGenerateFormatterElement(IndentedWriter out, SerializationContext context) {
        super(out, context) ;
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
        
        if(!q.isGenerateType()) {
            throw new IllegalArgumentException("SubGenerate Query must be a generate query");
        }
        
        QuerySerializerFactory factory = SerializerRegistry.get().getQuerySerializerFactory(SPARQLGenerateSyntax.syntaxSPARQLGenerate);
        SPARQLGenerateQueryVisitor visitor = (SPARQLGenerateQueryVisitor) factory.create(SPARQLGenerateSyntax.syntaxSPARQLGenerate, q.getPrologue(), out);
        
        visitor.startVisit(q) ;
        visitor.visitGenerateResultForm(q) ;
        visitor.visitQueryPattern(q) ;
        visitor.visitGroupBy(q) ;
        visitor.visitHaving(q) ;
        visitor.visitOrderBy(q) ;
        visitor.visitOffset(q) ;
        visitor.visitLimit(q) ;
        visitor.visitValues(q) ;
        visitor.visitSelector(q) ;
        visitor.finishVisit(q) ;
        
        out.print(" .");
        out.decIndent(INDENT);
    }

}
