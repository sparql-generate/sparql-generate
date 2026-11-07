/*
 * Copyright 2016 The Apache Software Foundation.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.ResultSetStream;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.engine.binding.BindingProject;
import org.apache.jena.sparql.util.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExtException;
import fr.mines_stetienne.ci.sparql_generate.query.SPARQLExtQuery;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;
import fr.mines_stetienne.ci.sparql_generate.utils.EvalUtils;

/**
 * Entry point to a SPARQL-Generate query execution.
 *
 * @author Maxime Lefrançois
 */
public class RootPlan {

	/**
	 * The logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(RootPlan.class);

	/**
	 * query.
	 */
	private final SPARQLExtQuery query;

	/**
	 * Selector and Source plans.
	 */
	private final DatasetDeclarationPlan datasetDeclarationPlan;

	/**
	 * Selector and Source plans.
	 */
	private final List<BindingsClausePlan> iteratorAndSourcePlans;

	/**
	 * The plan for the SPARQL SELECT.
	 */
	public final SelectPlan selectPlan;

	/**
	 * The plan for the GENERATE clause.
	 */
	private final GeneratePlan generatePlan;

	/**
	 * The plan for the TEMPLATE clause.
	 */
	private final TemplatePlan templatePlan;

	public SPARQLExtQuery getQuery() {
		return query;
	}

	/**
	 * Constructor
	 *
	 * @param query
	 * @param iteratorAndSourcePlans
	 * @param selectPlan
	 */
	public RootPlan(final SPARQLExtQuery query, final DatasetDeclarationPlan datasetDeclarationPlan,
			final List<BindingsClausePlan> iteratorAndSourcePlans, final SelectPlan selectPlan) {
		Objects.requireNonNull(iteratorAndSourcePlans, "iterator and source" + " plans may be empty, but not null.");
		this.query = query;
		this.datasetDeclarationPlan = datasetDeclarationPlan;
		this.iteratorAndSourcePlans = iteratorAndSourcePlans;
		this.selectPlan = selectPlan;
		this.generatePlan = null;
		this.templatePlan = null;
	}

	/**
	 * Constructor
	 *
	 * @param query
	 * @param iteratorAndSourcePlans
	 * @param selectPlan
	 * @param generatePlan
	 */
	public RootPlan(final SPARQLExtQuery query, final DatasetDeclarationPlan datasetDeclarationPlan,
			final List<BindingsClausePlan> iteratorAndSourcePlans, final SelectPlan selectPlan,
			final GeneratePlan generatePlan) {
		Objects.requireNonNull(iteratorAndSourcePlans, "iterator and source" + " plans may be empty, but not null.");
		this.query = query;
		this.datasetDeclarationPlan = datasetDeclarationPlan;
		this.iteratorAndSourcePlans = iteratorAndSourcePlans;
		this.selectPlan = selectPlan;
		this.generatePlan = generatePlan;
		this.templatePlan = null;
	}

	/**
	 * Constructor
	 *
	 * @param query
	 * @param iteratorAndSourcePlans
	 * @param selectPlan
	 * @param templatePlan
	 */
	public RootPlan(final SPARQLExtQuery query, final DatasetDeclarationPlan datasetDeclarationPlan,
			final List<BindingsClausePlan> iteratorAndSourcePlans, final SelectPlan selectPlan,
			final TemplatePlan templatePlan) {
		Objects.requireNonNull(iteratorAndSourcePlans, "iterator and source" + " plans may be empty, but not null.");
		this.query = query;
		this.datasetDeclarationPlan = datasetDeclarationPlan;
		this.iteratorAndSourcePlans = iteratorAndSourcePlans;
		this.selectPlan = selectPlan;
		this.generatePlan = null;
		this.templatePlan = templatePlan;
	}

	/**
	 * Executes a GENERATE query and returns the generated RDF Graph.
	 *
	 * @param context
	 *            the execution context, created using {@link ContextUtils}
	 * @return the RDF Graph.
	 */
	public Model execGenerate(final Context context) {
		return execGenerate(null, context);
	}

	/**
	 * Executes a GENERATE query and returns the generated RDF Graph.
	 *
	 * @param values
	 *            the values for the query signature, or null.
	 * @param context
	 *            the execution context, created using {@link ContextUtils}
	 * @return the RDF Graph.
	 */
	public Model execGenerate(final List<Binding> values, final Context context) {
		final Model model = ModelFactory.createDefaultModel();
		final StreamRDF output = new StreamRDFModel(model);
		checkContextHasNoOutput(context);
		boolean isRoot = ContextUtils.isRootContext(context);
		final Context newContext = ContextUtils.fork(context, isRoot).setGenerateOutput(output).fork();
		execGenerateStream(values, newContext);
		return model;
	}

	/**
	 * Executes a GENERATE query.
	 *
	 * @param context
	 *            the execution context, created using {@link ContextUtils}
	 */
	public void execGenerateStream(final Context context) {
		execGenerateStream(null, context);
	}

	/**
	 * Executes a GENERATE query and emits generated triples to the stream.
	 *
	 * @param values
	 *            the values for the query signature, or null.
	 * @param context
	 *            the execution context, created using {@link ContextUtils}
	 * @param output
	 *            the RDF Stream object.
	 */
	public void execGenerateStream(final List<Binding> values, final Context context) {
		Objects.requireNonNull(context);
		Objects.requireNonNull(ContextUtils.getGenerateOutput(context));
		if (!query.isGenerateType()) {
			throw new SPARQLExtException("Query is not a GENERATE query.");
		}
		final List<Var> variables = getVariables(values);
		final List<Binding> newValues = getValues(variables, values);
		exec(variables, newValues, context);
	}

	
	/**
	 * Executes a GENERATE sub-query on the bindings of its parent, whose
	 * variables are known: they are not computed again from every binding.
	 */
	void execSubGenerateStream(final List<Var> variables, final List<Binding> values, final Context context) {
		if (query.getSignature() != null || values == null) {
			execGenerateStream(values, context);
			return;
		}
		Objects.requireNonNull(context);
		Objects.requireNonNull(ContextUtils.getGenerateOutput(context));
		exec(new ArrayList<>(new LinkedHashSet<>(variables)), values, context);
	}

	/**
	 * Executes a SELECT query and returns the results.
	 *
	 * @param context
	 *            the execution context, created using {@link ContextUtils}
	 * @return the {@link ResultSet}.
	 */
	public ResultSet execSelect(final Context context) {
		return execSelect(null, context);
	}

	/**
	 * Executes a SELECT query and returns the results.
	 *
	 * @param values
	 *            the values for the query signature, or null.
	 * @param context
	 *            the execution context, created using {@link ContextUtils}
	 * @return the {@link ResultSet}.
	 */
	public ResultSet execSelect(final List<Binding> values, final Context context) {
		List<ResultSet> results = new ArrayList<>();
		checkContextHasNoOutput(context);
		boolean isRoot = ContextUtils.isRootContext(context);
		final Context newContext = ContextUtils.fork(context, isRoot).setSelectOutput(results::add).fork();
		execSelectStream(values, newContext);
		return union(results);
	}

	public void execSelectStream(final Context context) {
		execSelectStream(null, context);
	}

	/**
	 * Executes a SELECT query and emits generated results to the stream.
	 *
	 * @param values
	 *            the values for the query signature, or null.
	 * @param context
	 *            the execution context, created using {@link ContextUtils}
	 * @param output
	 *            where to emit {@link ResultSet} objects.
	 */
	public void execSelectStream(final List<Binding> values, final Context context) {
		Objects.requireNonNull(context);
		Objects.requireNonNull(ContextUtils.getSelectOutput(context));
		if (!query.isSelectType()) {
			throw new SPARQLExtException("Query is not a SELECT query.");
		}
		final List<Var> variables = getVariables(values);
		final List<Binding> newValues = getValues(variables, values);
		exec(variables, newValues, context);
	}

	/**
	 * Executes a TEMPLATE query.
	 *
	 * @param context
	 *            the execution context, created using {@link ContextUtils}
	 * @return the output string.
	 */
	public String execTemplate(final Context context) {
		return execTemplate(null, context);
	}

	/**
	 * Executes a TEMPLATE query.
	 *
	 * @param values
	 *            the values for the query signature, or null.
	 * @param context
	 *            the execution context, created using {@link ContextUtils}
	 * @return the output string.
	 */
	public String execTemplate(final List<Binding> values, final Context context) {
		checkContextHasNoOutput(context);
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
				IndentedWriter output = new IndentedWriter(baos);) {
			boolean isRoot = ContextUtils.isRootContext(context);
			final Context newContext = ContextUtils.fork(context, isRoot).setTemplateOutput(output).fork();
			execTemplateStream(values, newContext);
			output.flush();
			return new String(baos.toByteArray());
		} catch (IOException ex) {
			throw new SPARQLExtException(ex);
		}
	}
	
	public void execTemplateStream(final Context context) {
		execTemplateStream(null, context);
	}

	/**
	 * Executes a TEMPLATE query.
	 *
	 * @param values
	 *            the values for the query signature, or null.
	 * @param context
	 *            the execution context, created using {@link ContextUtils}
	 * @return the output string.
	 */
	public void execTemplateStream(final List<Binding> values, final Context context) {
		Objects.requireNonNull(context);
		Objects.requireNonNull(ContextUtils.getTemplateOutput(context));
		if (!query.isTemplateType()) {
			throw new SPARQLExtException("Query is not a TEMPLATE query.");
		}
		final List<Var> variables = getVariables(values);
		final List<Binding> newValues = getValues(variables, values);
		exec(variables, newValues, context);
	}

	private List<Var> getVariables(List<Binding> values) {
		Set<Var> variables = new HashSet<>();
		if (values != null) {
			for (Binding binding : values) {
				for (Iterator<Var> it = binding.vars(); it.hasNext();) {
					variables.add(it.next());
				}
			}
		}
		List<Var> signature = query.getSignature();
		if (signature == null) {
			return new ArrayList<>(variables);
		}
		if (!signature.containsAll(variables)) {
			Set<Var> superfluousVars = variables.stream().filter((v) -> !signature.contains(v))
					.collect(Collectors.toSet());
			throw new SPARQLExtException(String.format(
					"The given input bindings use variables that are not in the signature: %s are not in the signature %s",
					superfluousVars, signature));

		}
		return new ArrayList<>(signature);
	}

	private List<Binding> getValues(List<Var> variables, List<Binding> values) {
		Objects.requireNonNull(variables);
		if (values == null) {
			values = new ArrayList<>();
			values.add(BindingFactory.binding());
		}
		return values;
	}

	private void exec(final List<Var> variables, final List<Binding> values, final Context context) {
		ContextUtils.checkLimits(context);
		if (ContextUtils.isRootContext(context)) {
			LOG.info("Starting execution");
			StreamRDF outputGenerate = ContextUtils.getGenerateOutput(context);
			if (outputGenerate != null) {
				outputGenerate.start();
				for (String prefix : query.getPrefixMapping().getNsPrefixMap().keySet()) {
					outputGenerate.prefix(prefix, query.getPrefixMapping().getNsPrefixURI(prefix));
				}
				if (query.getBaseURI() != null) {
					outputGenerate.base(query.getBaseURI());
				}
			}
		} else {
			LOG.trace("Starting sub-execution");
		}

		try {
			// a query called on several rows is evaluated once per dataset
			final List<List<Binding>> groups = datasetDeclarationPlan.groupByDataset(values, context);
			for (List<Binding> group : groups) {
				final Binding binding = group.size() > 0 ? group.get(0) : null;
				final Context newContext = datasetDeclarationPlan.prepareDataset(binding, context);
				final List<Var> groupVariables = groups.size() == 1 ? variables : new ArrayList<>(variables);
				execIteratorAndSourcePlans(groupVariables, group, newContext, 0);
			}
			if (ContextUtils.isRootContext(context)) {
				StreamRDF outputGenerate = ContextUtils.getGenerateOutput(context);
				if (outputGenerate != null) {
					outputGenerate.finish();
				}
				LOG.info("End of execution");
			} else {
				LOG.trace("End of sub-execution");
			}
		} finally {
			if (ContextUtils.isRootContext(context)) {
				// streams are closed, even when the execution fails
				ContextUtils.close(context);
			}
		}
	}

	private void execIteratorAndSourcePlans(final List<Var> variables, final List<Binding> values,
			final Context context, final int i) {
		if (i < iteratorAndSourcePlans.size()) {
			final BindingsClausePlan plan = iteratorAndSourcePlans.get(i);
			final List<BindPlan> binds = consecutiveBinds(variables, i);
			if (binds != null) {
				for (BindPlan bind : binds) {
					variables.add(bind.getVar());
				}
				final List<Binding> newValues = BindPlan.execAll(binds, values, context);
				execIteratorAndSourcePlans(variables, newValues, context, i + binds.size());
			} else if (plan instanceof BindOrSourcePlan) {
				final BindOrSourcePlan bindOrSourcePlan = (BindOrSourcePlan) plan;
				if (!variables.contains(bindOrSourcePlan.getVar())) {
					variables.add(bindOrSourcePlan.getVar());
				}
				final List<Binding> newValues = bindOrSourcePlan.exec(values, context);
				execIteratorAndSourcePlans(variables, newValues, context, i + 1);
				if (LOG.isDebugEnabled()) {
					LOG.debug("Finished plan " + bindOrSourcePlan);
				}
			} else {
				IteratorPlan iteratorPlan = (IteratorPlan) plan;
				iteratorPlan.exec(variables, values, context, (newValues) -> {
					final List<Var> newVariables = new ArrayList<>(variables);
					for (Var var : iteratorPlan.getVars()) {
						if (!newVariables.contains(var)) {
							newVariables.add(var);
						}
					}
					execIteratorAndSourcePlans(newVariables, newValues, context, i + 1);
					LOG.debug("Finished batch for " + iteratorPlan);
				});
				LOG.debug("Finished plan " + iteratorPlan);
			}
		} else {
			execSelectPlan(variables, values, context);
		}
	}

	/**
	 * @return the BIND clauses that start at index i, if there are at least
	 *         two and none of their variables is bound by the input or by
	 *         another of them, so that they can be executed in one pass; or
	 *         null.
	 */
	private List<BindPlan> consecutiveBinds(final List<Var> variables, final int i) {
		final List<BindPlan> binds = new ArrayList<>();
		final Set<Var> vars = new HashSet<>();
		for (int j = i; j < iteratorAndSourcePlans.size() && iteratorAndSourcePlans.get(j) instanceof BindPlan; j++) {
			final BindPlan bind = (BindPlan) iteratorAndSourcePlans.get(j);
			if (variables.contains(bind.getVar()) || !vars.add(bind.getVar())) {
				break;
			}
			binds.add(bind);
		}
		return binds.size() >= 2 ? binds : null;
	}

	private void execSelectPlan(final List<Var> variables, final List<Binding> values, final Context context) {
		if (selectPlan == null) {
			if (query.isSelectType()) {
				// no WHERE clause: project the solutions of the binding clauses
				final List<Var> projected = query.isQueryResultStar() ? variables : query.getProjectVars();
				final List<String> listVar = projected.stream().map(Var::getVarName).collect(Collectors.toList());
				final List<Binding> projectedValues = query.isQueryResultStar() ? values
						: values.stream().map((b) -> (Binding) new BindingProject(projected, b))
								.collect(Collectors.toList());
				final Model model = ContextUtils.getDataset(context).getDefaultModel();
				final ResultSet resultSet = ResultSetStream.create(listVar, model, projectedValues.iterator());
				ContextUtils.getSelectOutput(context).accept(resultSet);
			} else if(query.isGenerateType()) {
				generatePlan.exec(variables, values, context);
			} else if(query.isTemplateType()) {
				templatePlan.exec(variables, values, context);
			}
		} else {
			selectPlan.exec(variables, values, context, resultSet -> {
				if (query.isSelectType()) {
					ContextUtils.getSelectOutput(context).accept(resultSet);
				} else {
					// every variable that a solution may bind: those of the input and
					// those of the results
					final Set<Var> allVariables = new LinkedHashSet<>(variables);
					for (String name : resultSet.getResultVars()) {
						allVariables.add(Var.alloc(name));
					}
					final List<Var> newVariables = new ArrayList<>(allVariables);
					final List<Binding> newValues = new ArrayList<>();
					while (resultSet.hasNext()) {
						newValues.add(resultSet.nextBinding());
					}
					if(query.isGenerateType()) {
						generatePlan.exec(newVariables, newValues, context);
					} else if(query.isTemplateType()) {
						templatePlan.exec(newVariables, newValues, context);
					}
				}
			});
		}
	}

	/**
	 * Concatenates result sets, as ResultSetUtils.union did before Jena 5.
	 */
	private static ResultSet union(List<ResultSet> results) {
		final List<String> vars = new ArrayList<>();
		final List<Binding> bindings = new ArrayList<>();
		for (ResultSet rs : results) {
			for (String var : rs.getResultVars()) {
				if (!vars.contains(var)) {
					vars.add(var);
				}
			}
			while (rs.hasNext()) {
				bindings.add(rs.nextBinding());
			}
		}
		return ResultSetStream.create(Var.varList(vars), bindings.iterator());
	}

	private void checkContextHasNoOutput(Context context) {
		if (!ContextUtils.isRootContext(context)) {
			return;
		}
		if (ContextUtils.getGenerateOutput(context) != null) {
			throw new SPARQLExtException("Context already has output for GENERATE");
		}
		if (ContextUtils.getSelectOutput(context) != null) {
			throw new SPARQLExtException("Context already has output for SELECT");
		}
		if (ContextUtils.getTemplateOutput(context) != null) {
			throw new SPARQLExtException("Context already has output for TEMPLATE");
		}
	}

}
