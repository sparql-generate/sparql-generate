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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.apache.jena.sparql.function.FunctionBase2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;

/**
 * Binding function
 * <a href="http://w3id.org/sparql-generate/fn/HTTPPost">fun:HTTPPost</a> 
 * operates a HTTP Post operation.
 *
 * <ul>
 * <li>Param 1: (a URL) the IRI of the target resource;</li>
 * <li>Param 2: (string): additional HTTP headers for the request.</li>
 * </ul>
 *
 * Returns the full HTTP response as a literal
 *
 * @author Omar Qawasmeh, Maxime Lefrançois
 */
public final class FUN_HTTPPost extends FunctionBase2 {

	private static final Logger LOG = LoggerFactory.getLogger(FUN_HTTPPost.class);
	public static final String URI = SPARQLExt.FUN + "HTTPPost";

	@SuppressWarnings("unused")
	@Override
	public NodeValue exec(NodeValue iri, NodeValue header) {

		if (iri == null) {
			LOG.debug("Must have two arguments, a URI and a header");
			throw new ExprEvalException("Must have two arguments, a URI and a header");
		}
		if (header == null) {
			LOG.debug("Must have two arguments, a URI and a header");
			throw new ExprEvalException("Must have two arguments, a URI and a header");
		}

		if (!iri.isIRI()) {
			LOG.debug("First argument must be a URI ");
			throw new ExprEvalException("First argument must be a URI ");

		}

// TODO: need  to check that IRI is http or https
// TODO: need  to check that header is literal
// TODO: need  to check that header is well formed
// TODO: second argument should be optional

		String[] headerArgs = String.valueOf(header.asNode().getLiteralLexicalForm()).split("\n");

		RDFDatatype dt;
		NodeValue outNode;
		CloseableHttpClient httpclient = HttpClients.createDefault();

		String fileURI = iri.asNode().getURI(); // construct the URI from args

		try {

			HttpPost req = new HttpPost(fileURI);
			setHeadersFromArgs(req, headerArgs);
			CloseableHttpResponse res = httpclient.execute(req);
			String response = FUN_GenerateResponse.generateResponse(res);
			dt = TypeMapper.getInstance().getTypeByValue(response);
			outNode = new NodeValueNode(NodeFactory.createLiteralByValue(response, dt));
			return outNode;

		} catch (Exception ex) {
			LOG.debug(ex.getMessage());
			throw new ExprEvalException(ex.getMessage());
		}
	}

	public void setHeadersFromArgs(HttpPost req, String[] headerArgs) {
		// TODO Auto-generated method stub
		for (String header : headerArgs) {
			String hArgs[] = header.split(":", 2);
			req.addHeader(hArgs[0], hArgs[1]);
			LOG.info("Header:\t" + hArgs[0] + ":" + hArgs[1] + " added successfully");

		}

	}

}
