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
package fr.mines_stetienne.ci.sparql_generate.function.library;

import java.util.regex.Pattern;

import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionBase1;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;

/**
 * Binding function
 * <a href="http://w3id.org/sparql-generate/fn/unsafeIRI">fun:unsafeIRI</a>
 * takes a string that starts with a scheme ({@code http:}, {@code urn:}...)
 * and returns the IRI of that string, as it is: unlike {@code IRI(str)}, the
 * string is not checked against the syntax of IRIs, nor resolved.
 *
 * <p>
 * It is meant for the IRIs that the term type {@code rml:UnsafeIRI} of RML
 * produces, which may hold spaces ({@code <http://example.com/Emily Smith>}).
 * Such an IRI is not valid in RDF: the N-Triples and N-Quads writers write a
 * space or another character that an IRI cannot hold as a {@code UCHAR}
 * escape sequence, and a strict RDF parser may reject the output. A string
 * without scheme (a relative IRI) is an evaluation error; an IRI is returned
 * unchanged.
 * </p>
 *
 * @author Maxime Lefrançois
 */
public final class FUN_unsafeIRI extends FunctionBase1 {

	public static final String URI = SPARQLExt.FUN + "unsafeIRI";

	private static final Pattern SCHEME = Pattern.compile("^[A-Za-z][A-Za-z0-9+.-]*:");

	@Override
	public NodeValue exec(NodeValue value) {
		if (value == null) {
			throw new ExprEvalException("Expecting a string, got null");
		}
		if (value.isIRI()) {
			return value;
		}
		if (!value.isLiteral()) {
			throw new ExprEvalException("Expecting a string, got " + value);
		}
		String lexical = value.asNode().getLiteralLexicalForm();
		if (!SCHEME.matcher(lexical).find()) {
			throw new ExprEvalException("Expecting a string that starts with a scheme, got " + value);
		}
		return NodeValue.makeNode(NodeFactory.createURI(lexical));
	}
}
