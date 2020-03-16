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
package fr.emse.ci.sparqlext.engine;

import fr.emse.ci.sparqlext.utils.LogUtils;
import fr.emse.ci.sparqlext.SPARQLExtException;
import fr.emse.ci.sparqlext.query.SPARQLExtQuery;
import fr.emse.ci.sparqlext.utils.ContextUtils;
import fr.emse.ci.sparqlext.utils.VarUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.jena.atlas.logging.Log;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.QueryEngineFactory;
import org.apache.jena.sparql.engine.QueryEngineRegistry;
import org.apache.jena.sparql.engine.QueryExecutionBase;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.engine.binding.BindingHashMap;
import org.apache.jena.sparql.engine.binding.BindingMap;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.syntax.ElementData;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.util.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executes the generated SPARQL SELECT query.
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class SelectPlan {

	private static final Logger LOG = LoggerFactory.getLogger(SelectPlan.class);

	/**
	 * The query.
	 */
	private final SPARQLExtQuery select;

	private final boolean isSelectType;

	private final List<Var> signature;

	/**
	 * Constructor.
	 *
	 * @param query
	 *            the SPARQL SELECT part of the query.
	 * @param isSelectType
	 *            if the query itself is a SELECT query
	 * @param signature
	 *            the signature of the query
	 */
	public SelectPlan(final SPARQLExtQuery query, final boolean isSelectType, final List<Var> signature) {
		if (!query.isSelectType()) {
			throw new SPARQLExtException("Should be select query. " + query);
		}
		this.select = query;
		this.isSelectType = isSelectType;
		this.signature = signature;
	}

	public List<Var> getVars() {
		return select.getProjectVars();
	}

	/**
	 * Updates a values block with the execution of a SPARQL SELECT query.
	 *
	 * @param variables
	 *            the variables
	 * @param values
	 *            the list of bindings.
	 * @param context
	 *            the execution context.
	 * @return the new list of bindings
	 */
	final public void exec(final List<Var> variables, final List<Binding> values, final Context context, Consumer<ResultSet> output) {
		if (Thread.interrupted()) {
			throw new SPARQLExtException(new InterruptedException());
		}
		final Query q = createQuery(select, variables, values, context);
		final Dataset inputDataset = ContextUtils.getDataset(context);
		if (LOG.isTraceEnabled()) {
			StringBuilder sb = new StringBuilder("Executing select query:\n");
			sb.append(q.toString());
			if (variables.size() > 0 && values.size() > 0) {
				sb.append(" \nwith initial values:\n");
				sb.append(LogUtils.log(variables, values));
			} else {
				sb.append(" \nwithout initial values.");
			}
			LOG.trace(sb.toString());
		} else if (LOG.isDebugEnabled()) {
			LOG.debug("Executing select query with " + values.size() + " bindings.");
		}
		try {
			augmentQuery(q, variables, values);
			final QueryEngineFactory factory = QueryEngineRegistry.get().find(q, inputDataset.asDatasetGraph(), context);
			try (QueryExecution exec = new QueryExecutionBase(q, inputDataset, context, factory)) {
				ResultSet resultSet = exec.execSelect();
				if (LOG.isTraceEnabled()) {
					ResultSetRewindable rewindable = ResultSetFactory.copyResults(resultSet);
					final List<Var> resultVariables = getVariables(rewindable.getResultVars());
					final List<Binding> resultBindings = new ArrayList<>();
					while (rewindable.hasNext()) {
						resultBindings.add(rewindable.nextBinding());
					}
					LOG.trace(String.format("Query output is\n%s", LogUtils.log(resultVariables, resultBindings)));
					rewindable.reset();
					resultSet = rewindable;
				} else if (LOG.isDebugEnabled()) {
					ResultSetRewindable rewindable = ResultSetFactory.copyResults(resultSet);
					int size = 0;
					while (rewindable.hasNext()) {
						rewindable.next();
						size++;
					}
					LOG.debug(String.format("Query has %s output for variables %s", size, rewindable.getResultVars()));
					rewindable.reset();
					resultSet = rewindable;
				} else {
					// got exception with call of unionOf in RootPlan. Would be better not to need to make rewindable
					ResultSetRewindable rewindable = ResultSetFactory.copyResults(resultSet);
					resultSet = rewindable;
				}				
				output.accept(resultSet);
			}
		} catch (Exception ex) {
			LOG.error("Error while executing SELECT Query " + q, ex);
			throw new SPARQLExtException("Error while executing SELECT Query " + q, ex);
		}
	}

	private Query createQuery(final SPARQLExtQuery select, final List<Var> variables,
			final List<Binding> values, final Context context) {
		// SPARQLExtQuery q = select.cloneQuery();
		Binding binding = !values.isEmpty() ? values.get(0) : null;
		SelectQueryPartialCopyVisitor cloner = new SelectQueryPartialCopyVisitor(binding, context);
		select.visit(cloner);
		Query q = cloner.getOutput();
		if (!isSelectType && !q.hasGroupBy() && !q.hasAggregators()) {
			variables.forEach(v -> {
				if (!q.getProjectVars().contains(v)) {
					q.getProject().add(v);
				}
			});
		}
		return q;
	}

	private void augmentQuery(final Query q, final List<Var> variables, final List<Binding> values) {
		if (variables.isEmpty()) {
			return;
		}
		ElementGroup old = (ElementGroup) q.getQueryPattern();
		ElementGroup newQueryPattern = new ElementGroup();
		q.setQueryPattern(newQueryPattern);
		if (old.size() >= 1 && old.get(0) instanceof ElementData) {
			ElementData qData = (ElementData) old.get(0);
			int oldSize = qData.getRows().size();
			qData = mergeValues(qData, variables, values);
			newQueryPattern.addElement(qData);
			for (int i = 1; i < old.size(); i++) {
				newQueryPattern.addElement(old.get(i));
			}
			LOG.debug("New query has " + qData.getRows().size() + " initial values. It had " + oldSize
					+ " values before");
		} else {
			ElementData data = new ElementData();
			variables.forEach(data::add);
			values.forEach(data::add);
			newQueryPattern.addElement(data);
			old.getElements().forEach(newQueryPattern::addElement);
			// unexplainable, but did happen
			check(data, values);
		}
	}

	private ElementData mergeValues(final ElementData qData, final List<Var> variables, final List<Binding> values) {
		if (values.isEmpty()) {
			return qData;
		}
		List<Var> vars = qData.getVars();
		if (!Collections.disjoint(vars, variables)) {
			throw new SPARQLExtException("Variables " + vars.retainAll(variables) + "were already bound.");
		}
		ElementData data = new ElementData();
		qData.getVars().forEach(data::add);
		variables.forEach(data::add);
		qData.getRows().forEach((qbinding) -> {
			values.forEach((binding) -> {
				BindingHashMap newb = new BindingHashMap(qbinding);
				variables.forEach((v) -> newb.add(v, binding.get(v)));
				data.add(newb);
			});
		});
		return data;
	}

	private void check(ElementData data, Collection<Binding> values) {
		if (data.getRows().size() != values.size()) {
			LOG.warn("Different size for the values block here.\n Was " + values.size() + ": \n" + values + "\n now is "
					+ data.getRows().size() + ": \n" + data.getRows());
			StringBuilder sb = new StringBuilder(
					"Different size for the values block here.\n Was " + values.size() + ": \n" + values + "\n\n");
			int i = 0;
			for (Binding b : values) {
				sb.append("\nbinding ").append(i++).append(" is ").append(b);
			}
			LOG.warn(sb.toString());
		}
	}


	private List<Var> getVariables(List<String> varNames) {
		return varNames.stream().map(VarUtils::allocVar).collect(Collectors.toList());
	}
}
