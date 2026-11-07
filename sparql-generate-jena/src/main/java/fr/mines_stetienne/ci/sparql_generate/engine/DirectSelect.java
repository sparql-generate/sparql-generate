/*
 * Copyright 2026 MINES Saint-Étienne
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
package fr.mines_stetienne.ci.sparql_generate.engine;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.jena.graph.Node;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.ARQConstants;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.engine.ResultSetStream;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.engine.binding.BindingProject;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprAggregator;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprFunction;
import org.apache.jena.sparql.expr.ExprFunctionOp;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionEnv;
import org.apache.jena.sparql.function.FunctionEnvBase;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementBind;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sparql.util.NodeFactoryExtra;

import fr.mines_stetienne.ci.sparql_generate.query.SPARQLExtQuery;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;

/**
 * Evaluates, without Jena's query engine, the SELECT of a GENERATE or
 * TEMPLATE query whose WHERE clause has only BIND and FILTER, and that has no
 * solution modifier: the solutions are the input bindings, extended by each
 * BIND in order (an error leaves the variable unbound), kept if every FILTER
 * is true, extended by the projected expressions, and projected. This is the
 * value of {@code SELECT ... WHERE { VALUES ... BIND ... FILTER ... }}, which
 * Jena evaluates with a VALUES block of all the input bindings.
 */
final class DirectSelect {

	private final SPARQLExtQuery select;
	private final List<Var> bindVars = new ArrayList<>();
	private final List<Expr> bindExprs = new ArrayList<>();
	private final List<Expr> filters = new ArrayList<>();
	private final List<Var> projectVars = new ArrayList<>();
	private final List<Expr> projectExprs = new ArrayList<>();

	/**
	 * False to evaluate every SELECT with Jena's query engine (tests).
	 */
	static volatile boolean enabled = true;

	/**
	 * The number of batches evaluated directly (tests).
	 */
	static final AtomicLong EXECUTIONS = new AtomicLong();

	private DirectSelect(SPARQLExtQuery select) {
		this.select = select;
	}

	/**
	 * @return the direct evaluation of the SELECT, or null if it does not apply.
	 */
	static DirectSelect of(SPARQLExtQuery select, boolean isSelectType) {
		if (!enabled || isSelectType || select.hasGroupBy() || select.hasAggregators() || select.hasHaving()
				|| select.hasOrderBy() || select.isDistinct() || select.isReduced() || select.hasLimit()
				|| select.hasOffset() || select.hasValues()) {
			return null;
		}
		final DirectSelect direct = new DirectSelect(select);
		final Element pattern = select.getQueryPattern();
		if (pattern != null) {
			if (!(pattern instanceof ElementGroup)) {
				return null;
			}
			for (Element el : ((ElementGroup) pattern).getElements()) {
				if (el instanceof ElementBind) {
					final ElementBind bind = (ElementBind) el;
					if (!isSimple(bind.getExpr()) || direct.bindVars.contains(bind.getVar())) {
						return null;
					}
					direct.bindVars.add(bind.getVar());
					direct.bindExprs.add(bind.getExpr());
				} else if (el instanceof ElementFilter) {
					final Expr expr = ((ElementFilter) el).getExpr();
					if (!isSimple(expr)) {
						return null;
					}
					direct.filters.add(expr);
				} else {
					return null;
				}
			}
		}
		final VarExprList project = select.getProject();
		for (Var var : project.getVars()) {
			final Expr expr = project.getExpr(var);
			if (expr != null) {
				if (!isSimple(expr) || direct.bindVars.contains(var) || direct.projectVars.contains(var)) {
					return null;
				}
				direct.projectVars.add(var);
				direct.projectExprs.add(expr);
			}
		}
		return direct;
	}

	/**
	 * No EXISTS, which needs the query engine, and no aggregate.
	 */
	private static boolean isSimple(Expr expr) {
		if (expr instanceof ExprFunctionOp || expr instanceof ExprAggregator) {
			return false;
		}
		if (expr instanceof ExprFunction) {
			for (Expr arg : ((ExprFunction) expr).getArgs()) {
				if (!isSimple(arg)) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * @return the solutions, computed as they are read, or null if a variable
	 *         of a BIND or of a projected expression is already bound, which
	 *         is left to Jena.
	 */
	ResultSet exec(final List<Var> variables, final List<Binding> values, final Context context) {
		for (Var var : bindVars) {
			if (variables.contains(var)) {
				return null;
			}
		}
		for (Var var : projectVars) {
			if (variables.contains(var)) {
				return null;
			}
		}
		final Context ctx = context.copy();
		// the base of IRI(), and NOW()
		ctx.set(ARQConstants.sysCurrentQuery, select);
		if (!ctx.isDefined(ARQConstants.sysCurrentTime)) {
			ctx.set(ARQConstants.sysCurrentTime, NodeFactoryExtra.nowAsDateTime());
		}
		final Dataset dataset = ContextUtils.getDataset(context);
		final DatasetGraph dsg = dataset == null ? null : dataset.asDatasetGraph();
		final FunctionEnv env = new FunctionEnvBase(ctx, dsg == null ? null : dsg.getDefaultGraph(), dsg);
		final List<Var> resultVars;
		final Set<Var> projection;
		if (select.isQueryResultStar()) {
			final Set<Var> all = new LinkedHashSet<>(variables);
			all.addAll(bindVars);
			all.addAll(projectVars);
			resultVars = new ArrayList<>(all);
			projection = null;
		} else {
			final Set<Var> all = new LinkedHashSet<>(select.getProjectVars());
			all.addAll(variables);
			resultVars = new ArrayList<>(all);
			// a projection that keeps every variable is not applied: reading a
			// projected binding costs a look-up in the projection
			projection = all.containsAll(bindVars) && all.containsAll(projectVars) ? null : all;
		}
		final Iterator<Binding> input = values.iterator();
		final Iterator<Binding> solutions = new Iterator<Binding>() {
			private Binding next = null;

			@Override
			public boolean hasNext() {
				while (next == null && input.hasNext()) {
					next = solution(input.next(), env, projection);
				}
				return next != null;
			}

			@Override
			public Binding next() {
				if (!hasNext()) {
					throw new NoSuchElementException();
				}
				final Binding b = next;
				next = null;
				return b;
			}
		};
		EXECUTIONS.incrementAndGet();
		return ResultSetStream.create(resultVars, solutions);
	}

	/**
	 * @return the solution for an input binding, or null if a filter rejects
	 *         it.
	 */
	private Binding solution(final Binding input, final FunctionEnv env, final Set<Var> projection) {
		Binding b = input;
		for (int i = 0; i < bindVars.size(); i++) {
			b = extend(b, bindVars.get(i), bindExprs.get(i), env);
		}
		for (Expr filter : filters) {
			try {
				if (!filter.isSatisfied(b, env)) {
					return null;
				}
			} catch (ExprEvalException ex) {
				return null;
			}
		}
		for (int i = 0; i < projectVars.size(); i++) {
			b = extend(b, projectVars.get(i), projectExprs.get(i), env);
		}
		return projection == null ? b : new BindingProject(projection, b);
	}

	private static Binding extend(final Binding b, final Var var, final Expr expr, final FunctionEnv env) {
		try {
			final NodeValue value = expr.eval(b, env);
			final Node node = value == null ? null : value.asNode();
			if (node == null || !node.isConcrete()) {
				return b;
			}
			return BindingFactory.binding(b, var, node);
		} catch (ExprEvalException ex) {
			// as BIND: the variable is unbound
			return b;
		}
	}
}
