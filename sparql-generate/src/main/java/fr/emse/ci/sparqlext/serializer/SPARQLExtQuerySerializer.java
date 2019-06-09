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
package fr.emse.ci.sparqlext.serializer;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.sparql.core.Prologue;
import fr.emse.ci.sparqlext.query.SPARQLExtQuery;
import java.util.List;
import org.apache.jena.graph.Node;
import org.apache.jena.query.SortCondition;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.serializer.PrologueSerializer;
import org.apache.jena.sparql.serializer.SerializationContext;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.Template;
import org.apache.jena.sparql.util.FmtUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import fr.emse.ci.sparqlext.query.SPARQLExtQueryVisitor;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;

/**
 * Extends the ARQ Query Serializer with SPARQL-Ext specificities.
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class SPARQLExtQuerySerializer implements SPARQLExtQueryVisitor {

    public final int BLOCK_INDENT = 2;
    private final IndentedWriter out;
    private final SerializationContext context;
    private final SPARQLExtFormatterElement fmtElement;
    private final SPARQLExtFormatterTemplate fmtTemplate;
    private final SPARQLExtFmtExprSPARQL fmtExpr;

    private static Logger LOG = LoggerFactory.getLogger(SPARQLExtQuerySerializer.class);

    public SPARQLExtQuerySerializer(IndentedWriter out, SerializationContext context) {
        this.out = out;
        this.context = context;
        this.fmtTemplate = new SPARQLExtFormatterTemplate(out, context);
        this.fmtExpr = new SPARQLExtFmtExprSPARQL(out, context);
        this.fmtElement = new SPARQLExtFormatterElement(out, context);
    }

    @Override
    public void visitGenerateClause(SPARQLExtQuery query) {
        out.print("GENERATE ");
        printName(query);
        if(!query.hasName() || query.hasGenerateClause()) {
            out.print(" {");
            if(query.hasGenerateClause()) {
                out.newline();
                out.incIndent(BLOCK_INDENT);
                query.getGenerateClause().forEach(e -> {
                    e.visit(fmtElement);
                    out.newline();
                });
                out.decIndent(BLOCK_INDENT);
            }
            out.print("} ");
        }
    }

    @Override
    public void visitTemplateClause(SPARQLExtQuery query) {
        out.print("TEMPLATE");
        printName(query);
        if(!query.hasName() || query.hasTemplateClause()) {
            out.print("{");
            if(query.hasTemplateClauseBefore()) {
                out.print(" BEFORE = ");
                NodeValue n = new NodeValueString(query.getTemplateClauseBefore());
                fmtExpr.format(n);
                out.print(" ; ");
            }
            if(query.hasTemplateClause()) {
                out.newline();
                out.incIndent(BLOCK_INDENT);
                query.getTemplateClause().forEach(e -> {
                    e.visit(fmtElement);
                    out.print(" ");
                });
                if(query.hasTemplateClauseSeparator()) {
                    NodeValue n = new NodeValueString(query.getTemplateClauseSeparator());
                    out.print(" ; SEPARATOR = ");
                    fmtExpr.format(n);
                }
                out.decIndent(BLOCK_INDENT);
            }
            if(query.hasTemplateClauseAfter()) {
                out.print(" ; AFTER = ");
                NodeValue n = new NodeValueString(query.getTemplateClauseAfter());
                fmtExpr.format(n);
            }
            out.print("}");
        }   
    }

    @Override
    public void visitPerformClause(SPARQLExtQuery query) {
        out.print("PERFORM ");
        printName(query);
        if(!query.hasName() || query.hasPerformClause()) {
            out.print(" {");
            if(query.hasPerformClause()) {
                out.newline();
                out.incIndent(BLOCK_INDENT);
                query.getPerformClause().forEach(e -> {
                    e.visit(fmtElement);
                    out.newline();
                });
                out.decIndent(BLOCK_INDENT);
            }
            out.print("}");
        }
    }

    @Override
    public void visitFunctionExpression(SPARQLExtQuery query) {
        out.print("FUNCTION ");
        printName(query);
        if(!query.hasName() || query.hasPerformClause()) {
            out.print(" {");
            if(query.hasFunctionExpression()) {
                out.newline();
                out.incIndent(BLOCK_INDENT);
                fmtExpr.format(query.getFunctionExpression());
                out.newline();
                out.decIndent(BLOCK_INDENT);
            }
            out.print("}");
        }
    }

    @Override
    public void visitBindingClauses(SPARQLExtQuery query) {
        if (query.getBindingClauses() == null) {
            return;
        }
        for (Element iteratorOrSource : query.getBindingClauses()) {
            iteratorOrSource.visit(fmtElement);
            out.newline();
        }
    }

    @Override
    public void startVisit(Query query) {
    }

    @Override
    public void visitResultForm(Query query) {
    }

    @Override
    public void visitPrologue(Prologue prologue) {
        int row1 = out.getRow();
        PrologueSerializer.output(out, prologue);
        int row2 = out.getRow();
        if (row1 != row2) {
            out.newline();
        }
    }

    @Override
    public void visitSelectResultForm(Query query) {
        out.print("SELECT ");
        if (query.isDistinct()) {
            out.print("DISTINCT ");
        }
        if (query.isReduced()) {
            out.print("REDUCED ");
        }
        out.print(" "); //Padding

        if (query.isQueryResultStar()) {
            out.print("*");
        } else {
            appendNamedExprList(query, out, query.getProject());
        }
        out.newline();
    }

    @Override
    public void visitConstructResultForm(Query query) {
        out.print("CONSTRUCT ");
//        if ( query.isQueryResultStar() )
//        {
//            out.print("*") ;
//            out.newline() ;
//        }
//        else
        {
            out.incIndent(BLOCK_INDENT);
            out.newline();
            Template t = query.getConstructTemplate();
            fmtTemplate.format(t);
            out.decIndent(BLOCK_INDENT);
        }
    }

    @Override
    public void visitDescribeResultForm(Query query) {
        out.print("DESCRIBE ");

        if (query.isQueryResultStar()) {
            out.print("*");
        } else {
            appendVarList(query, out, query.getResultVars());
            if (query.getResultVars().size() > 0
                    && query.getResultURIs().size() > 0) {
                out.print(" ");
            }
            appendURIList(query, out, query.getResultURIs());
        }
        out.newline();
    }

    @Override
    public void visitAskResultForm(Query query) {
        out.print("ASK");
        out.newline();
    }

    @Override
    public void visitDatasetDecl(Query query) {
        if (query.getGraphURIs() != null && query.getGraphURIs().size() != 0) {
            for (String uri : query.getGraphURIs()) {
                out.print("FROM ");
                out.print(FmtUtils.stringForURI(uri, query));
                out.newline();
            }
        }
        if (query.getNamedGraphURIs() != null && query.getNamedGraphURIs().size() != 0) {
            for (String uri : query.getNamedGraphURIs()) {
                // One per line
                out.print("FROM NAMED ");
                out.print(FmtUtils.stringForURI(uri, query));
                out.newline();
            }
        }
    }

    @Override
    public void visitQueryPattern(Query query) {
        if (query.getQueryPattern() != null) {
            out.print("WHERE ");
            Element el = query.getQueryPattern();
            fmtElement.visitAsGroup(el);
            out.print(" ");
        }
    }

    @Override
    public void visitGroupBy(Query query) {
        if (query.hasGroupBy()) {
            // Can have an empty GROUP BY list if the groupin gis implicit
            // by use of an aggregate in the SELECT clause.
            if (!query.getGroupBy().isEmpty()) {
                out.print("GROUP BY ");
                appendNamedExprList(query, out, query.getGroupBy());
                out.println();
            }
        }
    }

    @Override
    public void visitHaving(Query query) {
        if (query.hasHaving()) {
            out.print("HAVING");
            for (Expr expr : query.getHavingExprs()) {
                out.print(" ");
                fmtExpr.format(expr);
            }
            out.println();
        }
    }

    @Override
    public void visitOrderBy(Query query) {
        if (query.hasOrderBy()) {
            out.print("ORDER BY ");
            boolean first = true;
            for (SortCondition sc : query.getOrderBy()) {
                if (!first) {
                    out.print(" ");
                }
                sc.format(fmtExpr, out);
                first = false;
            }
            out.println();
        }
    }

    @Override
    public void visitLimit(Query query) {
        if (query.hasLimit()) {
            out.print("LIMIT   " + query.getLimit());
            out.newline();
        }
    }

    @Override
    public void visitOffset(Query query) {
        if (query.hasOffset()) {
            out.print("OFFSET  " + query.getOffset());
            out.newline();
        }
    }

    @Override
    public void visitValues(Query query) {
        if (query.hasValues()) {
            outputDataBlock(out, query.getValuesVariables(), query.getValuesData(), context);
            out.newline();
        }
    }

    public static void outputDataBlock(IndentedWriter out, List<Var> variables, List<Binding> values, SerializationContext cxt) {
        out.print("VALUES ");
        if (variables.size() == 1) {
            // Short form.
            out.print("?");
            out.print(variables.get(0).getVarName());
            out.print(" {");
            out.incIndent();
            for (Binding valueRow : values) {
                outputValuesOneRow(out, variables, valueRow, cxt);
            }
            out.decIndent();
            out.print(" }");
            return;
        }
        // Long form.
        out.print("(");
        for (Var v : variables) {
            out.print(" ");
            out.print(v.toString());
        }
        out.print(" )");
        out.print(" {");
        out.incIndent();
        for (Binding valueRow : values) {
            out.println();
            out.print("(");
            outputValuesOneRow(out, variables, valueRow, cxt);
            out.print(" )");
        }
        out.decIndent();
        out.ensureStartOfLine();
        out.print("}");
    }

    private static void outputValuesOneRow(IndentedWriter out, List<Var> variables, Binding row, SerializationContext cxt) {
        // A value may be null for UNDEF
        for (Var var : variables) {
            out.print(" ");
            Node value = row.get(var);
            if (value == null) {
                out.print("UNDEF");
            } else {
                // Context for bnodes.
                // Bnodes don't occur in legal syntax but a rewritten query may
                // have them.  The output will not be legal SPARQL.
                // ARQ (SPARQL with extensions) does parse blankd nodes in VALUES. 
                SPARQLExtFmtUtils.printNode(out, value, cxt);
            }
        }
    }

    @Override
    public void finishVisit(Query query) {
        out.flush();
    }

    // ----
    void appendVarList(Query query, IndentedWriter sb, List<String> vars) {
        boolean first = true;
        for (String varName : vars) {
            Var var = Var.alloc(varName);
            if (!first) {
                sb.print(" ");
            }
            sb.print(var.toString());
            first = false;
        }

    }

    void appendNamedExprList(Query query, IndentedWriter sb, VarExprList namedExprs) {
        boolean first = true;
        for (Var var : namedExprs.getVars()) {
            Expr expr = namedExprs.getExpr(var);
            if (!first) {
                sb.print(" ");
            }

            if (expr != null) {
                // The following are safe to write without () 
                // Compare/merge with fmtExpr.format
                boolean needParens = true;

                if (expr.isFunction()) {
                    needParens = false;
                } //                else if ( expr instanceof E_Aggregator )
                //                    // Aggregators are variables (the function maps to an internal variable 
                //                    // that is accesses by the E_Aggregator
                //                    needParens = false ;
                else if (expr.isVariable()) {
                    needParens = false;
                }

                if (!Var.isAllocVar(var)) // AS ==> need parens
                {
                    needParens = true;
                }

                if (needParens) {
                    out.print("(");
                }
                fmtExpr.format(expr);
                if (!Var.isAllocVar(var)) {
                    sb.print(" AS ");
                    sb.print(var.toString());
                }
                if (needParens) {
                    out.print(")");
                }
            } else {
                sb.print(var.toString());
            }
            first = false;
        }
    }

    static void appendURIList(Query query, IndentedWriter sb, List<Node> vars) {
        SerializationContext cxt = new SerializationContext(query);
        boolean first = true;
        for (Node node : vars) {
            if (!first) {
                sb.print(" ");
            }
            SPARQLExtFmtUtils.printNode(sb, node, cxt);
            first = false;
        }
    }

    @Override
    public void visitPragma(SPARQLExtQuery query) {
    }

    private void printName(SPARQLExtQuery query) {
        if (query.hasName()) {
            out.print(" ");
            SPARQLExtFmtUtils.printNode(out, query.getName(), context);
        }
        if(!query.hasSignature() && !query.hasCallParameters()) {
            out.print(" ");
        }
        if (query.hasSignature()) {
            printSignature(query);
        }
        if (query.hasCallParameters()) {
            printCallParameters(query);
        }
    }

    void printSignature(SPARQLExtQuery query) {
        out.print("(");
        boolean first = true;
        List<Var> signature = query.getSignature();
        for (int i = 0; i < signature.size(); i++) {
            Var var = signature.get(i);
            if (i!=0) {
                out.print(", ");
            }
            out.print("?");
            out.print(var.getName());
            first = false;
        }
        out.print(")");
    }

    void printCallParameters(SPARQLExtQuery query) {
        out.print("(");
        List<Expr> parameters = query.getCallParameters().getList();
        for (int i = 0; i < parameters.size(); i++) {
            if (i!=0) {
                out.print(", ");
            }
            Expr param = parameters.get(i);
            fmtExpr.format(param);
        }
        out.print(")");
    }

    @Override
    public void visitPostSelect(SPARQLExtQuery query) {
        if (query.hasPostSelect()) {
            out.print("EXPRESSIONS ");
            appendNamedExprList(query, out, query.getPostSelect());
            out.newline();
        }
    }

}
