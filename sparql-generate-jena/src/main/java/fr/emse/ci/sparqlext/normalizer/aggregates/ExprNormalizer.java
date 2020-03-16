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
package fr.emse.ci.sparqlext.normalizer.aggregates;

import java.util.UUID;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.E_StrEncodeForURI;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprAggregator;
import org.apache.jena.sparql.expr.ExprFunction1;
import org.apache.jena.sparql.expr.ExprFunction2;
import org.apache.jena.sparql.expr.ExprFunction3;
import org.apache.jena.sparql.expr.ExprFunctionN;
import org.apache.jena.sparql.expr.ExprFunctionOp;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;

import fr.emse.ci.sparqlext.expr.E_URIParam;
import fr.emse.ci.sparqlext.graph.Node_Extended;
import fr.emse.ci.sparqlext.query.SPARQLExtQuery;
import fr.emse.ci.sparqlext.utils.VarUtils;

/**
 * Class used to normalize expressions and nodes, and output an expression that
 * is equivalent
 *
 * @author maxime.lefrancois
 */
public class ExprNormalizer {

    private final SPARQLExtQuery query;
    
    public ExprNormalizer(SPARQLExtQuery query) {
		this.query = query;
	}
    
    
    /**
     * normalizes an expression, substituting every instance of NodeValueNode
     * whose node is a Node_Extended with the associated expression of that
     * Node_Extended.
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
        if (func instanceof E_URIParam) {
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
        ExprList args = new ExprList();
        for (Expr expr : funcOp.getArgs()) {
            Expr arg = normalize(expr);
            args.add(arg);
        }
        return funcOp.copy(args, funcOp.getGraphPattern());
    }

    private Expr normalize(NodeValueNode nv) {
        return normalize(nv.asNode());
    }

    private Expr normalize(ExprAggregator eAgg) {
    	Var var = VarUtils.allocVar(UUID.randomUUID().toString().substring(0, 8));
    	query.addPostSelect(var, eAgg);
    	return new ExprVar(var);
    }

    /**
     * get a normalized expression for the node given as input, returns either
     * the expression of a expression node (Node_Extended), or a NodeValueNode
     * whose node is the input node.
     *
     * @param n node (potentially expression node) to get an expression from
     * @return
     */
    public Expr normalize(Node n) {
        if (n instanceof Node_Extended) {
        	throw new UnsupportedOperationException("should not reach this point");
        }
        return new NodeValueNode(n);
    }

}
