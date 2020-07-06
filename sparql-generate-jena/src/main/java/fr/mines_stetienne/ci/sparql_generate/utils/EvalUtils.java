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
package fr.mines_stetienne.ci.sparql_generate.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.jena.graph.Node;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.engine.binding.BindingMap;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionEnv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Maxime Lefrançois
 */
public class EvalUtils {

    private static final Logger LOG = LoggerFactory.getLogger(EvalUtils.class);

    /**
     * The query name may be an expression, which evaluates differently
     * depending on the input bindings. This method groups the bindings for
     * which the query name evaluation is the same.
     *
     * @param expr the expression for the query name
     * @param bindings
     * @param env
     * @return
     */
    public static Map<String, List<Binding>> splitBindingsForQuery(
            final Expr expr,
            final List<Binding> bindings,
            final FunctionEnv env) {
        final Map<String, List<Binding>> calls = new HashMap<>();
        for (Binding binding : bindings) {
            final Node n = eval(expr, binding, env);
            if (n == null) {
                continue;
            }
            if (!n.isURI()) {
                LOG.warn("Name of sub query resolved to something else than a"
                        + " URI: " + n);
                continue;
            }
            String queryName = n.getURI();
            List<Binding> call = calls.get(queryName);
            if (call == null) {
                call = new ArrayList<>();
                calls.put(queryName, call);
            }
            call.add(binding);
        }
        return calls;
    }

    public static List<List<Node>> eval(
            final ExprList exprs,
            final List<Binding> bindings,
            final FunctionEnv env) {
        final List<List<Node>> nodesList = new ArrayList<>();
        for (Binding binding : bindings) {
            List<Node> nodes = eval(exprs, binding, env);
            if (nodes != null) {
                nodesList.add(nodes);
            }
        }
        return nodesList;
    }

    public static List<Node> eval(
            final ExprList exprs,
            final Binding binding,
            final FunctionEnv env) {
        Objects.requireNonNull(binding);
        Objects.requireNonNull(env);
        if (exprs == null) {
            return null;
        }
        final List<Node> nodes = new ArrayList<>();
        exprs.forEach((expr) -> {
            nodes.add(eval(expr, binding, env));
        });
        return nodes;
    }

    public static Node eval(
            final Expr expr,
            final Binding binding,
            final FunctionEnv env) {
        Objects.requireNonNull(expr);
        try {
            NodeValue node = expr.eval(binding, env);
            if (node.asNode().isConcrete()) {
                return node.asNode();
            }
        } catch (ExprEvalException ex) {
            LOG.trace("Exception while evaluating the expression " + expr + ":", ex);
        }
        return null;
    }

    public static List<Node> eval(List<NodeValue> nodeValues) {
        Objects.requireNonNull(nodeValues);
        List<Node> nodes = new ArrayList<>();
        nodeValues.forEach((node) -> {
            if(node.asNode().isConcrete()) {
                nodes.add(node.asNode());
            } else {
                nodes.add(null);
            }
        });
        return nodes;
    }

	public static Binding createBinding(QuerySolution sol) {
		final BindingMap binding = BindingFactory.create();
		for (Iterator<String> it = sol.varNames(); it.hasNext();) {
			final String varName = it.next();
			binding.add(VarUtils.allocVar(varName), sol.get(varName).asNode());
		}
		return binding;
	}
}
