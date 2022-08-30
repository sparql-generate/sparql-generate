package fr.mines_stetienne.ci.sparql_generate.engine;

import java.util.List;
import java.util.Objects;

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

	// private Binding signatureBinding;
	// private FunctionEnv env;

	public DatasetDeclarationPlan(final SPARQLExtQuery query) {
		Objects.requireNonNull(query, "query must not be null");
		if (query.getBaseURI() != null) {
			baseURI = IRIs.resolve(query.getBaseURI());
		} else {
			baseURI = IRIs.getBaseStr();
		}

		this.fromClauses = query.getFromClauses();
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
			return IRIs.resolve(sourceURI, absBaseURI);
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
		Graph g = dsg.getGraph(n);
		if (g == null) {
			g = GraphFactory.createJenaDefaultGraph();
			dsg.addGraph(n, g);
		}
		// default: check the dataset
		if (dataset.containsNamedModel(absURI)) {
			Graph dg = dataset.getNamedModel(absURI).getGraph();
			GraphUtil.addInto(g, dg);
			return;
		}
		// fallback: load as RDF graph
		StreamRDF dest = StreamRDFLib.graph(g);
		ContextUtils.loadGraph(context, sourceURI, absURI, dest);
	}

	private void addDefaultGraph(Binding binding, Context context, DatasetGraph dsg, SPARQLExtQuery generate) {
		loadGraph(binding, context, generate, dsg.getDefaultGraph());
	}

	private void addNamedGraph(Binding binding, Context context, DatasetGraph dsg, SPARQLExtQuery generate, Expr name) {
		String sourceURI = evalSourceURI(binding, context, name);
		final String absURI = baseURI(sourceURI, baseURI);
		Node n = NodeFactory.createURI(absURI);
		Graph g = dsg.getGraph(n);
		if (g == null) {
			g = GraphFactory.createJenaDefaultGraph();
			dsg.addGraph(n, g);
		}
		loadGraph(binding, context, generate, g);
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
