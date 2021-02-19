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
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.apache.jena.sparql.function.FunctionBase1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;

/**
 * Binding function
 * <a href="http://w3id.org/sparql-generate/fn/HTTPExtractBody">fun:HTTPExtractBody</a> 
 * extracts the body from a full HTTP response.
 *
 * <ul>
 * <li>Param 1: (a string) the full HTTP response;</li>
 * </ul>
 *
 * Returns the body section of the full HTTP reponse
 * 
 * @author Omar Qawasmeh, Maxime Lefrançois
 * 
 * @organization Ecole des Mines de Saint Etienne
 */
public class FUN_HTTPExtractBody extends FunctionBase1 {
	private static final Logger LOG = LoggerFactory.getLogger(FUN_HTTPExtractBody.class);
	public static final String URI = SPARQLExt.FUN + "HTTPExtractBody";

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
		int firstTwoNewLinesCharacter = res.indexOf("\n\n");
		if (firstTwoNewLinesCharacter == -1) {
			throw new ExprEvalException("Argument does not contain a body");
		}
		return new NodeValueString(res.substring(firstTwoNewLinesCharacter+2));
	}

}
