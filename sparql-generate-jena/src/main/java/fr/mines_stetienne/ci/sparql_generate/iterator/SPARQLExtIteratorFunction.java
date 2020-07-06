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
package fr.mines_stetienne.ci.sparql_generate.iterator;

import fr.mines_stetienne.ci.sparql_generate.engine.QueryExecutor;
import fr.mines_stetienne.ci.sparql_generate.query.SPARQLExtQuery;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;
import fr.mines_stetienne.ci.sparql_generate.utils.EvalUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.apache.jena.graph.Node;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.ARQException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.apache.jena.sparql.util.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Iterator function <a href=
 * "http://w3id.org/sparql-generate/iter/call-select">iter:call-select</a> takes
 * as input a IRI to a SPARQL-Select document, runs it on the current Dataset,
 * and binds the given variables to the output of the select query, in order.
 *
 * <ul>
 * <li>Param 1: (select) is the IRI to a SPARQL-Select document</li>
 * </ul>
 *
 * Multiple variables may be bound: variable <em>n</em> is bound to the value of
 * the <em>n</em><sup>th</sup> project variable of the select query.
 *
 * @author Maxime Lefrançois
 */
public class SPARQLExtIteratorFunction extends IteratorStreamFunctionBase {

	private static final Logger LOG = LoggerFactory.getLogger(SPARQLExtIteratorFunction.class);

	private final SPARQLExtQuery select;

	public SPARQLExtIteratorFunction(SPARQLExtQuery select, Context context) {
		if (!select.isSelectType()) {
			throw new ARQException("creating iterator for a query that is not a SELECT");
		}
		this.select = select;
		select.normalizeXExpr();
		select.normalizeBNode();
	}

	@Override
	public void exec(final List<NodeValue> parameters, final Consumer<List<List<NodeValue>>> collectionListNodeValue) {
		final List<List<Node>> callParameters = new ArrayList<>();
		callParameters.add(EvalUtils.eval(parameters));
		final Context newContext = ContextUtils.fork(getContext()).setSelectOutput((result) -> {
			List<List<NodeValue>> list = getListNodeValues(result);
			collectionListNodeValue.accept(list);
		}).fork();
		final QueryExecutor queryExecutor = ContextUtils.getQueryExecutor(getContext());
		queryExecutor.execSelectFromQuery(select, callParameters, newContext);
	}

	@Override
	public void checkBuild(ExprList args) {
	}

	private List<List<NodeValue>> getListNodeValues(ResultSet result) {
		List<String> resultVars = result.getResultVars();
		List<List<NodeValue>> listNodeValues = new ArrayList<>();
		while (result.hasNext()) {
			List<NodeValue> nodeValues = new ArrayList<>();
			QuerySolution sol = result.next();
			for (String var : resultVars) {
				RDFNode rdfNode = sol.get(var);
				if (rdfNode != null) {
					NodeValue n = new NodeValueNode(rdfNode.asNode());
					nodeValues.add(n);
				} else {
					nodeValues.add(null);
				}
			}
			listNodeValues.add(nodeValues);
		}
		return listNodeValues;
	}

}
