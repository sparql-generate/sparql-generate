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
package com.github.thesmartenergy.sparql.generate.jena.serializer;

import com.github.thesmartenergy.sparql.generate.jena.graph.Node_Expr;
import com.github.thesmartenergy.sparql.generate.jena.graph.Node_ExtendedLiteral;
import com.github.thesmartenergy.sparql.generate.jena.graph.Node_ExtendedURI;
import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.apache.jena.sparql.serializer.SerializationContext;
import org.apache.jena.sparql.util.FmtUtils;

/**
 *
 * @author maxime.lefrancois
 */
public class SPARQLGenerateFmtUtils {

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
            SPARQLGenerateFmtExprSPARQL v = new SPARQLGenerateFmtExprSPARQL(out, context);
            Node_Expr n2 = (Node_Expr) n;
            out.print("?{ ");
            v.format(n2.getExpr());
            out.print(" }");
        } else if (n instanceof Node_ExtendedLiteral) {
            SPARQLGenerateFmtExprSPARQL v = new SPARQLGenerateFmtExprSPARQL(out, context);
            Node_ExtendedLiteral n2 = (Node_ExtendedLiteral) n;
            out.print("\"");
            for (Expr component : n2.getComponents()) {
                if (component instanceof NodeValueString) {
                    String cString = ((NodeValueString) component).getString();
                    StringBuilder sb = new StringBuilder();
                    FmtUtils.stringEsc(sb, cString, true) ;
                    cString = sb.toString();
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
            SPARQLGenerateFmtExprSPARQL v = new SPARQLGenerateFmtExprSPARQL(out, context);
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
        } else {
            out.print(FmtUtils.stringForNode(n, context));
        }
    }

}
