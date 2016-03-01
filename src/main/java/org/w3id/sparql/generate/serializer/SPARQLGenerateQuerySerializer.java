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

import java.io.OutputStream;
import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryVisitor;
import org.apache.jena.query.Syntax;
import org.apache.jena.sparql.core.Prologue;
import org.apache.jena.sparql.serializer.FmtExprSPARQL;
import org.apache.jena.sparql.serializer.FmtTemplate;
import org.apache.jena.sparql.serializer.FormatterElement;
import org.apache.jena.sparql.serializer.FormatterTemplate;
import org.apache.jena.sparql.serializer.QuerySerializerFactory;
import org.apache.jena.sparql.serializer.SerializationContext;
import org.apache.jena.sparql.serializer.SerializerRegistry;
import org.apache.jena.sparql.util.NodeToLabelMapBNode;
import org.w3id.sparql.generate.query.SPARQLGenerateSyntax;

/**
 *
 * @author maxime.lefrancois
 */
public class SPARQLGenerateQuerySerializer implements org.w3id.sparql.generate.query.SPARQLGenerateQueryVisitor {

    public static void init() {
        QuerySerializerFactory factory = new QuerySerializerFactory() {
            @Override
            public boolean accept(Syntax syntax) {
                // Since ARQ syntax is a super set of SPARQL 1.1 both SPARQL 1.0
                // and SPARQL 1.1 can be serialized by the same serializer
                return Syntax.syntaxARQ.equals(syntax) || Syntax.syntaxSPARQL_10.equals(syntax)
                        || Syntax.syntaxSPARQL_11.equals(syntax) || SPARQLGenerateSyntax.syntaxSPARQLGenerate.equals(syntax);
            }

            @Override
            public QueryVisitor create(Syntax syntax, Prologue prologue, IndentedWriter writer) {
                QueryVisitor serializer = SerializerRegistry.get().getQuerySerializerFactory(Syntax.syntaxSPARQL_11).create(syntax, prologue, writer);
                // For the generate pattern
                SerializationContext cxt = new SerializationContext(prologue, new NodeToLabelMapBNode("g", false));
                return new SPARQLGenerateQuerySerializer(serializer, writer, new SPARQLGenerateFormatterElement(writer, cxt), new FmtExprSPARQL(writer, cxt),
                        new FmtTemplate(writer, cxt));
            }

            @Override
            public QueryVisitor create(Syntax syntax, SerializationContext context, IndentedWriter writer) {
                QueryVisitor serializer = SerializerRegistry.get().getQuerySerializerFactory(Syntax.syntaxSPARQL_11).create(syntax, context, writer);
                return new SPARQLGenerateQuerySerializer(serializer, writer, new SPARQLGenerateFormatterElement(writer, context), new FmtExprSPARQL(writer,
                        context), new FmtTemplate(writer, context));
            }
        };

        SerializerRegistry registry = SerializerRegistry.get();
        registry.addQuerySerializer(SPARQLGenerateSyntax.syntaxSPARQLGenerate, factory);
    }

    public final int BLOCK_INDENT = 2;
    private final QueryVisitor decorated;
    private final IndentedWriter out;
    private final FormatterElement fmtElement;
    private final FmtExprSPARQL fmtExpr;
    private final FormatterTemplate fmtTemplate;

    public SPARQLGenerateQuerySerializer(QueryVisitor serializer, OutputStream _out, FormatterElement formatterElement, FmtExprSPARQL formatterExpr, FormatterTemplate formatterTemplate) {
        this(serializer, new IndentedWriter(_out), formatterElement, formatterExpr, formatterTemplate);
    }

    SPARQLGenerateQuerySerializer(QueryVisitor serializer, IndentedWriter iwriter, FormatterElement formatterElement, FmtExprSPARQL formatterExpr, FormatterTemplate formatterTemplate) {
        decorated = serializer;
        out = iwriter;
        fmtTemplate = formatterTemplate;
        fmtElement = formatterElement;
        fmtExpr = formatterExpr;
    }

    @Override
    public void visitGenerateResultForm(org.w3id.sparql.generate.query.SPARQLGenerateQuery query) {
        out.print("GENERATE ");
        if (query.hasSource()) {
            out.print(" ");
            out.print("<" + query.getSource() + ">");
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
    public void visitSelector(org.w3id.sparql.generate.query.SPARQLGenerateQuery query) {
        if (query.hasSelector()) {
            out.print("SELECTOR " + query.getSelector());
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
        decorated.visitOffset(query);
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
