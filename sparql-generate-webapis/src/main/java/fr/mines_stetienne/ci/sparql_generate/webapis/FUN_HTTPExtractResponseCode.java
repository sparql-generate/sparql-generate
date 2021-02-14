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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.apache.jena.sparql.function.FunctionBase1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;

/**
 * Binding function
 * <a href="http://w3id.org/sparql-generate/fn/HTTPExtractResponseCode">fun:HTTPExtractResponseCode</a> 
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

		NodeValue outNode = null;
		RDFDatatype dt;
		// TODO what if the response is not a literal.
		String res = String.valueOf(response.asNode().getLiteralLexicalForm());

		// TODO there may be issues with different server/client platforms ? 
		String blankLine = System.getProperty("line.separator") + "{2}";

		// TODO what if the response is not well formed.
		String[] responseParts = res.split(blankLine, 2);

		try {

			List<String> lines = IOUtils.readLines(new StringReader(responseParts[0]));

			String responseStatus = lines.get(0);

			// LOG.info("Response status:\t" + responseStatus);

			// TODO: does this select xsd:integer ?
			dt = TypeMapper.getInstance().getTypeByValue(responseStatus);

			outNode = new NodeValueNode(NodeFactory.createLiteralByValue(responseStatus, dt));

		} catch (IOException e) {

			throw new ExprEvalException(e.getMessage());
		}

		return outNode;
	}

}
