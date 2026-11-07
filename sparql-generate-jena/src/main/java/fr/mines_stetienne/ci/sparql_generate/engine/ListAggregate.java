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
package fr.mines_stetienne.ci.sparql_generate.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.jena.datatypes.BaseDatatype;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.aggregate.Accumulator;
import org.apache.jena.sparql.expr.aggregate.AccumulatorFactory;
import org.apache.jena.sparql.expr.aggregate.AggregateRegistry;
import org.apache.jena.sparql.function.FunctionEnv;
import org.apache.jena.sparql.util.FmtUtils;
import org.apache.jena.sys.JenaSystem;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;

/**
 * The aggregate that LIST( expr ) of a GENERATE template becomes when the
 * query has a GROUP BY clause: the values of expr in a group, in the order of
 * the solutions. Its value is a literal whose value is the list of nodes;
 * {@link GenerateFormPlan} writes it as an RDF list.
 */
public final class ListAggregate {

	public static final String URI = SPARQLExt.NS + "aggregate/list";

	public static final RDFDatatype DATATYPE = new BaseDatatype(SPARQLExt.NS + "list") {
		@Override
		public String unparse(Object value) {
			if (!(value instanceof List)) {
				return String.valueOf(value);
			}
			return ((List<?>) value).stream().map((n) -> FmtUtils.stringForNode((Node) n))
					.collect(Collectors.joining(" ", "(", ")"));
		}

		@Override
		public Class<?> getJavaClass() {
			return List.class;
		}
	};

	private static final AccumulatorFactory FACTORY = (agg, distinct) -> new Accumulator() {

		private final List<Node> nodes = new ArrayList<>();

		@Override
		public void accumulate(Binding binding, FunctionEnv functionEnv) {
			try {
				NodeValue nv = agg.getExprList().get(0).eval(binding, functionEnv);
				if (nv != null && (!distinct || !nodes.contains(nv.asNode()))) {
					nodes.add(nv.asNode());
				}
			} catch (ExprEvalException ex) {
				// an unbound value is not in the list
			}
		}

		@Override
		public NodeValue getValue() {
			return NodeValue.makeNode(NodeFactory.createLiteralByValue(new ArrayList<>(nodes), DATATYPE));
		}
	};

	private ListAggregate() {
	}

	/**
	 * Registers the aggregate, if needed. The initialization of Jena resets the
	 * registry of aggregates, so this is also called when a list aggregate is
	 * created.
	 */
	public static void register() {
		JenaSystem.init();
		if (!AggregateRegistry.isRegistered(URI)) {
			AggregateRegistry.register(URI, FACTORY);
		}
	}

	public static boolean isList(Node node) {
		return node != null && node.isLiteral() && DATATYPE.getURI().equals(node.getLiteralDatatypeURI())
				&& node.getLiteralValue() instanceof List;
	}

	@SuppressWarnings("unchecked")
	public static List<Node> nodes(Node node) {
		return (List<Node>) node.getLiteralValue();
	}
}
