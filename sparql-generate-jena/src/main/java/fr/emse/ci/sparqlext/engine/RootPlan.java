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
package fr.emse.ci.sparqlext.engine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
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
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sparql.util.ResultSetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.emse.ci.sparqlext.SPARQLExtException;
import fr.emse.ci.sparqlext.query.SPARQLExtQuery;
import fr.emse.ci.sparqlext.utils.ContextUtils;
import fr.emse.ci.sparqlext.utils.EvalUtils;

/**
 * Entry point to a SPARQL-Generate query execution.
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
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
		return ResultSetUtils.union(results.toArray(new ResultSet[results.size()]));
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
		if (Thread.interrupted()) {
			LOG.warn("Interrupted " + System.identityHashCode(this));
			return;
		}
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

		Binding binding = values.size() > 0 ? values.get(0) : null;
		Context newContext = datasetDeclarationPlan.prepareDataset(binding, context);
		execIteratorAndSourcePlans(variables, values, newContext, 0);

		if (ContextUtils.isRootContext(context)) {
			StreamRDF outputGenerate = ContextUtils.getGenerateOutput(context);
			if (outputGenerate != null) {
				outputGenerate.finish();
			}
			ContextUtils.close(context);
			LOG.info("End of execution");
		} else {
			LOG.trace("End of sub-execution");
		}
	}

	private void execIteratorAndSourcePlans(final List<Var> variables, final List<Binding> values,
			final Context context, final int i) {
		if (i < iteratorAndSourcePlans.size()) {
			final BindingsClausePlan plan = iteratorAndSourcePlans.get(i);
			if (plan instanceof BindOrSourcePlan) {
				final BindOrSourcePlan bindOrSourcePlan = (BindOrSourcePlan) plan;
				variables.add(bindOrSourcePlan.getVar());
				final List<Binding> newValues = bindOrSourcePlan.exec(values, context);
				execIteratorAndSourcePlans(variables, newValues, context, i + 1);
				LOG.debug("Finished plan " + bindOrSourcePlan);
			} else {
				IteratorPlan iteratorPlan = (IteratorPlan) plan;
				iteratorPlan.exec(variables, values, context, (newValues) -> {
					final List<Var> newVariables = new ArrayList<>(variables);
					newVariables.addAll(iteratorPlan.getVars());
					execIteratorAndSourcePlans(newVariables, newValues, context, i + 1);
					LOG.debug("Finished batch for " + iteratorPlan);
				});
				LOG.debug("Finished plan " + iteratorPlan);
			}
		} else {
			execSelectPlan(variables, values, context);
		}
	}

	private void execSelectPlan(final List<Var> variables, final List<Binding> values, final Context context) {
		if (selectPlan == null) {
			if (query.isSelectType()) {
				final List<String> listVar = variables.stream().map(Var::getVarName).collect(Collectors.toList());
				final Model model = ContextUtils.getDataset(context).getDefaultModel();
				final ResultSet resultSet = new ResultSetStream(listVar, model, values.iterator());
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
					final List<Var> newVariables = new ArrayList<>();
					newVariables.addAll(variables); // can we delete this?
					newVariables.addAll(selectPlan.getVars());
					final List<Binding> newValues = new ArrayList<>();
					while (resultSet.hasNext()) {
						final Binding p = EvalUtils.createBinding(resultSet.next());
						newValues.add(p);
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
