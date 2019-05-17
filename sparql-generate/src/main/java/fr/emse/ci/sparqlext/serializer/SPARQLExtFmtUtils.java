/*
 * Copyright 2017 Ecole des Mines de Saint-Etienne.
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

import fr.emse.ci.sparqlext.graph.Node_Expr;
import fr.emse.ci.sparqlext.graph.Node_ExtendedLiteral;
import fr.emse.ci.sparqlext.graph.Node_ExtendedURI;
import fr.emse.ci.sparqlext.graph.Node_List;
import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.apache.jena.sparql.serializer.SerializationContext;
import org.apache.jena.sparql.util.FmtUtils;

/**
 * Utility class to serialize tiples, nodes, basic graph patterns.
 *
 * @author maxime.lefrancois
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
                    cString = cString.replace("\\\\n", "\\n");
                    cString = cString.replace("\\\\{", "\\{");
                    cString = cString.replace("\\\\}", "\\}");
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
//        } else if (n instanceof Node_BGP) {
//            Node_BGP n2 = (Node_BGP) n;
//            out.print("{ ");
//            SPARQLExtFmtUtils.formatPattern(out, n2.getPattern(), context);
//            out.print(" }");
        } else {
            String s = FmtUtils.stringForNode(n, context);
            s = s.replace("{", "\\{");
            s = s.replace("}", "\\}");
            s = s.replace("\\\\", "\\");
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
