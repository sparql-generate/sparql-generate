package fr.mines_stetienne.ci.sparql_generate.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.irix.IRIs;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFLib;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.core.Var;
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
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionEnv;
import org.apache.jena.sparql.function.FunctionEnvBase;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.sparql.util.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.mines_stetienne.ci.sparql_generate.query.SPARQLExtQuery;
import fr.mines_stetienne.ci.sparql_generate.syntax.FromClause;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;

public class DatasetDeclarationPlan {

	/**
	 * The logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(DatasetDeclarationPlan.class);

	/**
	 * The baseURI.
	 */
	private final String baseURI;

	/**
	 * The from clauses.
	 */
	private final List<FromClause> fromClauses;

	/**
	 * For each FROM GENERATE clause, the names of the variables its query
	 * mentions.
	 */
	private final Map<FromClause, Set<String>> generateVariables = new HashMap<>();

	/**
	 * True if the dataset depends on the binding of the variables.
	 */
	private final boolean dependsOnBinding;

	private static final Pattern VARIABLE = Pattern.compile("[?$]([\\p{L}\\p{N}_][\\p{L}\\p{N}_\\u00B7\\u0300-\\u036F\\u203F-\\u2040]*)");

	public DatasetDeclarationPlan(final SPARQLExtQuery query) {
		Objects.requireNonNull(query, "query must not be null");
		if (query.getBaseURI() != null) {
			baseURI = IRIs.resolve(query.getBaseURI());
		} else {
			baseURI = IRIs.getBaseStr();
		}

		this.fromClauses = query.getFromClauses();
		boolean depends = false;
		if (fromClauses != null) {
			for (FromClause fromClause : fromClauses) {
				if (fromClause.getName() != null && !fromClause.getName().getVarsMentioned().isEmpty()) {
					depends = true;
				}
				if (fromClause.getGenerate() != null) {
					final Set<String> names = new HashSet<>();
					final Matcher matcher = VARIABLE.matcher(fromClause.getGenerate().toString());
					while (matcher.find()) {
						names.add(matcher.group(1));
					}
					generateVariables.put(fromClause, names);
					depends = depends || !names.isEmpty();
				}
			}
		}
		this.dependsOnBinding = depends;
	}

	/**
	 * Groups the bindings by the dataset they determine, in the order of their
	 * first occurrence. A dataset that does not depend on the bindings gives
	 * one group.
	 *
	 * @param values the bindings
	 * @param context the execution context
	 * @return the groups of bindings.
	 */
	protected final List<List<Binding>> groupByDataset(final List<Binding> values, final Context context) {
		final List<List<Binding>> groups = new ArrayList<>();
		if (!dependsOnBinding || values.size() <= 1) {
			groups.add(values);
			return groups;
		}
		final Map<List<Object>, List<Binding>> byKey = new LinkedHashMap<>();
		for (Binding binding : values) {
			byKey.computeIfAbsent(datasetKey(binding, context), (k) -> new ArrayList<>()).add(binding);
		}
		groups.addAll(byKey.values());
		return groups;
	}

	/**
	 * What the dataset of a binding depends on: the value of the expressions
	 * of the FROM clauses, and the variables that the queries of the FROM
	 * GENERATE clauses mention.
	 */
	private List<Object> datasetKey(final Binding binding, final Context context) {
		final List<Object> key = new ArrayList<>();
		final FunctionEnv env = new FunctionEnvBase(context);
		for (FromClause fromClause : fromClauses) {
			final Expr name = fromClause.getName();
			if (name != null && !name.getVarsMentioned().isEmpty()) {
				try {
					key.add(name.eval(binding, env).asNode());
				} catch (ExprEvalException ex) {
					// an error, reported when the dataset is prepared
					key.add(null);
				}
			}
			if (fromClause.getGenerate() != null) {
				final Map<Var, Node> projection = new HashMap<>();
				final Set<String> names = generateVariables.get(fromClause);
				binding.forEach((v, n) -> {
					if (names.contains(v.getVarName())) {
						projection.put(v, n);
					}
				});
				key.add(projection);
			}
		}
		return key;
	}

	protected final Context prepareDataset(Binding binding, Context context) {
		if (fromClauses == null || fromClauses.isEmpty()) {
			return context;
		}
		final DatasetGraph dsg = DatasetGraphFactory.createGeneral();
		fromClauses.forEach((fromClause) -> {
			if (fromClause.getGenerate() == null) {
				if (!fromClause.isNamed()) {
					addDefaultGraph(binding, context, dsg, fromClause.getName());
				} else {
					addNamedGraph(binding, context, dsg, fromClause.getName());
				}
			} else {
				SPARQLExtQuery generate = fromClause.getGenerate();
				if (!fromClause.isNamed()) {
					addDefaultGraph(binding, context, dsg, generate);
				} else {
					addNamedGraph(binding, context, dsg, generate, fromClause.getName());
				}
			}
		});
		Dataset newDataset = DatasetFactory.wrap(dsg);
		return ContextUtils.fork(context).setDataset(newDataset).fork();
	}

	private static String baseURI(String sourceURI, String absBaseURI) {
		if (absBaseURI == null) {
			return IRIs.resolve(sourceURI);
		} else {
			// the base first
			return IRIs.resolve(absBaseURI, sourceURI);
		}
	}

	private void addDefaultGraph(Binding binding, Context context, DatasetGraph dsg, Expr sourceExpr) {
		String sourceURI = evalSourceURI(binding, context, sourceExpr);
		final String absURI = baseURI(sourceURI, baseURI);
		// default: check the dataset
		Dataset dataset = ContextUtils.getDataset(context);
		if (dataset.containsNamedModel(absURI)) {
//			Node n = NodeFactory.createURI(absURI);
			Graph g = dataset.getNamedModel(absURI).getGraph();
			GraphUtil.addInto(dsg.getDefaultGraph(), g);
			return;
		}
		// fallback: load as RDF graph
		StreamRDF dest = StreamRDFLib.graph(dsg.getDefaultGraph());
		ContextUtils.loadGraph(context, sourceURI, absURI, dest);
	}

	private void addNamedGraph(Binding binding, Context context, DatasetGraph dsg, Expr sourceExpr) {
		String sourceURI = evalSourceURI(binding, context, sourceExpr);
		final String absURI = baseURI(sourceURI, baseURI);
		Dataset dataset = ContextUtils.getDataset(context);
		Node n = NodeFactory.createURI(absURI);
		Graph g = GraphFactory.createJenaDefaultGraph();
		if (dataset.containsNamedModel(absURI)) {
			// default: check the dataset
			GraphUtil.addInto(g, dataset.getNamedModel(absURI).getGraph());
		} else {
			// fallback: load as RDF graph
			StreamRDF dest = StreamRDFLib.graph(g);
			ContextUtils.loadGraph(context, sourceURI, absURI, dest);
		}
		putNamedGraph(dsg, n, g);
	}

	/**
	 * Adds the graph as a named graph, or merges it into the named graph.
	 * DatasetGraph.getGraph may give a graph that is not in the dataset, so
	 * the graph is filled first and then added.
	 */
	private static void putNamedGraph(DatasetGraph dsg, Node n, Graph g) {
		if (dsg.containsGraph(n)) {
			GraphUtil.addInto(dsg.getGraph(n), g);
		} else {
			dsg.addGraph(n, g);
		}
	}

	private void addDefaultGraph(Binding binding, Context context, DatasetGraph dsg, SPARQLExtQuery generate) {
		loadGraph(binding, context, generate, dsg.getDefaultGraph());
	}

	private void addNamedGraph(Binding binding, Context context, DatasetGraph dsg, SPARQLExtQuery generate, Expr name) {
		String sourceURI = evalSourceURI(binding, context, name);
		final String absURI = baseURI(sourceURI, baseURI);
		Node n = NodeFactory.createURI(absURI);
		Graph g = GraphFactory.createJenaDefaultGraph();
		loadGraph(binding, context, generate, g);
		putNamedGraph(dsg, n, g);
	}

	private String evalSourceURI(Binding binding, Context context, Expr sourceExpr) {
		if (binding == null) {
			throw new NullPointerException("No binding to evaluate the source expression " + sourceExpr);
		}
		try {
			FunctionEnv env = new FunctionEnvBase(context);
			NodeValue nodeValue = sourceExpr.eval(binding, env);
			if (!nodeValue.isIRI()) {
				throw new IllegalArgumentException("FROM source expression did not eval to a URI " + sourceExpr);
			}
			return nodeValue.asNode().getURI();
		} catch (ExprEvalException ex) {
			throw new IllegalArgumentException("Exception when evaluating the source expression " + sourceExpr, ex);
		}
	}

	private void loadGraph(Binding binding, Context context, SPARQLExtQuery generate, Graph graph) {
		QueryExecutor queryExecutor = ContextUtils.getQueryExecutor(context);
		StreamRDF dest = StreamRDFLib.graph(graph);
		Context newContext = ContextUtils.fork(context).setGenerateOutput(dest).fork();
		queryExecutor.execGenerateFromQuery(generate, binding, newContext);
	}
}
