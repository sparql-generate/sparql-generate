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
package fr.mines_stetienne.ci.sparql_generate.serializer;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.graph.Node_Expr;
import fr.mines_stetienne.ci.sparql_generate.graph.Node_ExtendedLiteral;
import fr.mines_stetienne.ci.sparql_generate.graph.Node_ExtendedURI;
import fr.mines_stetienne.ci.sparql_generate.graph.Node_List;
import fr.mines_stetienne.ci.sparql_generate.graph.Node_Template;
import fr.mines_stetienne.ci.sparql_generate.query.SPARQLExtQuery;
import fr.mines_stetienne.ci.sparql_generate.query.SPARQLExtQueryVisitor;
import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import static org.apache.jena.sparql.serializer.FormatterElement.INDENT;
import org.apache.jena.sparql.serializer.QuerySerializerFactory;
import org.apache.jena.sparql.serializer.SerializationContext;
import org.apache.jena.sparql.serializer.SerializerRegistry;
import org.apache.jena.sparql.util.FmtUtils;

/**
 * Utility class to serialize tiples, nodes, basic graph patterns.
 *
 * @author Maxime Lefrançois
 */
public class SPARQLExtFmtUtils {

    public static void formatPattern(IndentedWriter out, BasicPattern pattern, SerializationContext sCxt) {
        boolean first = true;
        for (Triple triple : pattern) {
            if (!first) {
                out.print("\n");
            }
            printTriple(out, triple, sCxt);
            out.print(" .");
            first = false;
        }
    }

    public static void printTriple(IndentedWriter out, Triple triple, SerializationContext sCxt) {
        printNode(out, triple.getSubject(), sCxt);
        out.print(" ");
        printNode(out, triple.getPredicate(), sCxt);
        out.print(" ");
        printNode(out, triple.getObject(), sCxt);
    }

    public static void printNode(IndentedWriter out, Node n, SerializationContext context) {
        if (n instanceof Node_Expr) {
            SPARQLExtFmtExprSPARQL v = new SPARQLExtFmtExprSPARQL(out, context);
            Node_Expr n2 = (Node_Expr) n;
            out.print("?{ ");
            v.format(n2.getExpr());
            out.print(" }");
        } else if (n instanceof Node_ExtendedLiteral) {
            SPARQLExtFmtExprSPARQL v = new SPARQLExtFmtExprSPARQL(out, context);
            Node_ExtendedLiteral n2 = (Node_ExtendedLiteral) n;
            out.print("\"");
            for (Expr component : n2.getComponents()) {
                if (component instanceof NodeValueString) {
                    String cString = ((NodeValueString) component).getString();
                    StringBuilder sb = new StringBuilder();
                    FmtUtils.stringEsc(sb, cString, true);
                    cString = sb.toString();
                    cString = cString.replace("{", "\\{");
                    cString = cString.replace("}", "\\}");
//                    cString = cString.replace("\\\\n", "\\n");
//                    cString = cString.replace("\\\\{", "\\{");
//                    cString = cString.replace("\\\\}", "\\}");
                    out.print(cString);
                } else {
                    out.print("{ ");
                    v.format(component);
                    out.print(" }");
                }
            }
            out.print("\"");
            if (n2.getLang() != null) {
                out.print("@");
                out.print(n2.getLang());
            } else if (n2.getDatatype() != null) {
                out.print("^^");
                printNode(out, n2.getDatatype(), context);
            }
        } else if (n instanceof Node_ExtendedURI) {
            SPARQLExtFmtExprSPARQL v = new SPARQLExtFmtExprSPARQL(out, context);
            Node_ExtendedURI n2 = (Node_ExtendedURI) n;
            out.print("<");
            for (Expr component : n2.getComponents()) {
                if (component instanceof NodeValueString) {
                    out.print(((NodeValueString) component).asString());
                } else {
                    out.print("{ ");
                    v.format(component);
                    out.print(" }");
                }
            }
            out.print(">");
        } else if (n instanceof Node_List) {
            SPARQLExtFmtExprSPARQL v = new SPARQLExtFmtExprSPARQL(out, context);
            Node_List n2 = (Node_List) n;
            out.print("LIST( ");
            v.format(n2.getExpr());
            out.print(" )");
        } else if (n instanceof Node_Template) {
            SPARQLExtQuery q = ((Node_Template) n).getQuery();
            if (!q.isTemplateType()) {
                throw new IllegalArgumentException("Extended template node is expected to be a template query");
            }
            out.incIndent(INDENT);
            QuerySerializerFactory factory = SerializerRegistry.get().getQuerySerializerFactory(SPARQLExt.SYNTAX);
            SPARQLExtQueryVisitor visitor = (SPARQLExtQueryVisitor) factory.create(SPARQLExt.SYNTAX, new SerializationContext(q.getPrologue()), out);

            visitor.startVisit(q);
            visitor.visitTemplateClause(q);
            visitor.visitDatasetDecl(q);
            visitor.visitBindingClauses(q);
            visitor.visitQueryPattern(q);
            visitor.visitGroupBy(q);
            visitor.visitHaving(q);
            visitor.visitOrderBy(q);
            visitor.visitOffset(q);
            visitor.visitLimit(q);
            visitor.visitPostSelect(q);
            visitor.visitValues(q);
            visitor.finishVisit(q);

            out.print(" . ");
            out.decIndent(INDENT);
//        } else if (n instanceof Node_BGP) {
//            Node_BGP n2 = (Node_BGP) n;
//            out.print("{ ");
//            SPARQLExtFmtUtils.formatPattern(out, n2.getPattern(), context);
//            out.print(" }");
        } else {
            String s = FmtUtils.stringForNode(n, context);
            s = s.replace("{", "\\{");
            s = s.replace("}", "\\}");
//            s = s.replace("\\\\", "\\");
            out.print(s);
        }
    }

    public static void fmtSPARQL(IndentedWriter iOut, ExprList exprs, SerializationContext pmap) {
        SPARQLExtFmtExprSPARQL fmt = new SPARQLExtFmtExprSPARQL(iOut, pmap);
        String sep = "";
        for (Expr expr : exprs) {
            iOut.print(sep);
            sep = " , ";
            fmt.format(expr);
        }
    }

}
