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
package fr.mines_stetienne.ci.sparql_generate.webapis;

import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueInteger;
import org.apache.jena.sparql.function.FunctionBase1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;

/**
 * Binding function <a href=
 * "http://w3id.org/sparql-generate/fn/HTTPExtractResponseCode">fun:HTTPExtractResponseCode</a>
 * extracts the response code from a full HTTP response.
 *
 * <ul>
 * <li>Param 1: (a string) the full HTTP response;</li>
 * </ul>
 *
 * Returns the response code of the full HTTP reponse
 * 
 * @author Omar Qawasmeh, Maxime Lefrançois
 * 
 * @organization Ecole des Mines de Saint Etienne
 */
public class FUN_HTTPExtractResponseCode extends FunctionBase1 {
	private static final Logger LOG = LoggerFactory.getLogger(FUN_HTTPExtractResponseCode.class);
	public static final String URI = SPARQLExt.FUN + "HTTPExtractResponseCode";

	@Override
	public NodeValue exec(NodeValue response) {
		if (!response.isLiteral()) {
			LOG.debug("Argument must be a Literal");
			throw new ExprEvalException("Argument must be a Literal");
		}
		String res = response.asNode().getLiteralLexicalForm();
		if (!res.startsWith("HTTP/1.1 ")) {
			LOG.debug("Argument must start with string HTTP/1.1 ");
			throw new ExprEvalException("Argument must start with string HTTP/1.1 ");
		}
		String codeString = res.substring(9, 12);
		try {
			int code = Integer.parseInt(codeString);
			return new NodeValueInteger(code);
		} catch (NumberFormatException ex) {
			LOG.debug(String.format("Argument must start with HTTP/1.1 , then a three digit response code. Got %s",
					codeString));
			throw new ExprEvalException(String.format(
					"Argument must start with HTTP/1.1 , then a three digit response code. Got %s", codeString));
		}
	}

}
