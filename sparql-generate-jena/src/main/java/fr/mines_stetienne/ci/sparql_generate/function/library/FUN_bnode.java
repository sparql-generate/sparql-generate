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
package fr.mines_stetienne.ci.sparql_generate.function.library;

import java.util.List;

import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionBase1;
import org.apache.jena.sparql.function.FunctionEnv;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;

/**
 * Binding function
 * <a href="http://w3id.org/sparql-generate/fn/bnode">fun:bnode</a> takes a
 * string and returns a blank node: the same blank node for the same string
 * during one execution of a query, sub-queries included.
 *
 * <p>
 * The SPARQL function {@code BNODE(str)} returns the same blank node for the
 * same string only within one solution. {@code fun:bnode(str)} is useful to
 * identify a resource by a key across solutions, like the blank node templates
 * of RML.
 * </p>
 *
 * @author Maxime Lefrançois
 */
public final class FUN_bnode extends FunctionBase1 {

	public static final String URI = SPARQLExt.FUN + "bnode";

	@Override
	protected NodeValue exec(List<NodeValue> args, FunctionEnv env) {
		NodeValue label = args.get(0);
		if (label == null || !label.isLiteral()) {
			throw new ExprEvalException("Expecting a literal, got " + label);
		}
		return NodeValue.makeNode(ContextUtils.getBlankNode(env.getContext(), label.asNode().getLiteralLexicalForm()));
	}

	@Override
	public NodeValue exec(NodeValue label) {
		throw new ExprEvalException("fun:bnode needs an execution context");
	}
}
