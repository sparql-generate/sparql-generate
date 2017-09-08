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
package com.github.thesmartenergy.sparql.generate.jena.normalizer;

import com.github.thesmartenergy.sparql.generate.jena.expr.E_URIParam;
import com.github.thesmartenergy.sparql.generate.jena.graph.Node_X;
import com.github.thesmartenergy.sparql.generate.jena.graph.Node_XExpr;
import com.github.thesmartenergy.sparql.generate.jena.graph.Node_XLiteral;
import com.github.thesmartenergy.sparql.generate.jena.graph.Node_XURI;
import java.util.List;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.E_IRI;
import org.apache.jena.sparql.expr.E_StrConcat;
import org.apache.jena.sparql.expr.E_StrDatatype;
import org.apache.jena.sparql.expr.E_StrEncodeForURI;
import org.apache.jena.sparql.expr.E_StrLang;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprAggregator;
import org.apache.jena.sparql.expr.ExprFunction1;
import org.apache.jena.sparql.expr.ExprFunction2;
import org.apache.jena.sparql.expr.ExprFunction3;
import org.apache.jena.sparql.expr.ExprFunctionN;
import org.apache.jena.sparql.expr.ExprFunctionOp;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;

/**
 * Class used to normalize expressions and nodes, and output an expression
 * that is equivalent
 * 
 * @author maxime.lefrancois
 */
public class ExprNormalizer {

    /**
     * normalizes an expression, substituting every instance of NodeValueNode
     * whose node is a Node_X with the associated expression of that Node_X.
     *
     * @param expr expression to normalize
     * @return 
     */
    public Expr normalize(Expr expr) {
        if (expr instanceof ExprFunction1) {
            return normalize((ExprFunction1) expr);
        } else if (expr instanceof ExprFunction2) {
            return normalize((ExprFunction2) expr);
        } else if (expr instanceof ExprFunction3) {
            return normalize((ExprFunction3) expr);
        } else if (expr instanceof ExprFunctionN) {
            return normalize((ExprFunctionN) expr);
        } else if (expr instanceof ExprFunctionOp) {
            return normalize((ExprFunctionOp) expr);
        } else if (expr instanceof NodeValueNode) {
            return normalize((NodeValueNode) expr);
        } else if (expr instanceof ExprAggregator) {
            return normalize((ExprAggregator) expr);
        }
        return expr;
    }

    private Expr normalize(ExprFunction1 func) {
        Expr arg = normalize(func.getArg());
        if(func instanceof E_URIParam) {
            return new E_StrEncodeForURI(arg);
        } else {
            return func.copy(arg);
        }
    }

    private Expr normalize(ExprFunction2 func) {
        Expr arg1 = normalize(func.getArg1());
        Expr arg2 = normalize(func.getArg2());
        return func.copy(arg1, arg2);
    }

    private Expr normalize(ExprFunction3 func) {
        Expr arg1 = normalize(func.getArg1());
        Expr arg2 = normalize(func.getArg2());
        Expr arg3 = normalize(func.getArg3());
        return func.copy(arg1, arg2, arg3);
    }

    private Expr normalize(ExprFunctionN func) {
        ExprList args = new ExprList();
        for (Expr expr : func.getArgs()) {
            Expr arg = normalize(expr);
            args.add(arg);
        }
        return func.copy(args);
    }

    private Expr normalize(ExprFunctionOp funcOp) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private Expr normalize(NodeValueNode nv) {
        return normalize(nv.asNode());
    }

    private Expr normalize(ExprAggregator eAgg) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * get a normalized expression for the node given as input, returns either
     * the expression of a expression node (Node_X), or a NodeValueNode whose
     * node is the input node.
     *
     * @param n node (potentially expression node) to get an expression from
     * @return 
     */
    public Expr normalize(Node n) {
        if (n instanceof Node_X) {
            return normalize((Node_X) n);
        }
        return new NodeValueNode(n);
    }

    private Expr normalize(Node_X n) {
        if (n instanceof Node_XExpr) {
            return normalize((Node_XExpr) n);
        } else if (n instanceof Node_XURI) {
            return normalize((Node_XURI) n);
        } else if (n instanceof Node_XLiteral) {
            return normalize((Node_XLiteral) n);
        }
        throw new NullPointerException();
    }

    private Expr normalize(Node_XExpr n) {
        Expr expr = normalize(n.getExpr());
        return expr;
    }

    private Expr normalize(Node_XURI n) {
        ExprList args = new ExprList();
        List<Expr> components = n.getComponents();
        for (Expr e : components) {
            args.add(normalize(e));
        }
        Expr str = new E_StrConcat(args);
        Expr expr = new E_IRI(str);
        return expr;
    }

    private Expr normalize(Node_XLiteral n) {
        ExprList args = new ExprList();
        List<Expr> components = n.getComponents();
        for (Expr e : components) {
            args.add(normalize(e));
        }
        Expr str = new E_StrConcat(args);
        if (n.getLang() != null) {
            Expr expr = new E_StrLang(str, new NodeValueString(n.getLang()));
            return expr;
        } else if (n.getDatatype() != null) {
            Expr dt = normalize(n.getDatatype());
            Expr expr = new E_StrDatatype(str, dt);
            return expr;
        } else {
            return str;
        }
    }
}
