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

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
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

public final class FUN_HTTPPut extends FunctionBase2 {


	private static final Logger LOG = LoggerFactory.getLogger(FUN_HTTPPut.class);
	public static final String URI = SPARQLExt.FUN + "HTTPPut";

	@SuppressWarnings("unused")
	@Override
	public NodeValue exec(NodeValue iri, NodeValue header) {

		LOG.info("URI is \n" + iri);
		LOG.info("Header issss \n" + header.asNode().getLiteralLexicalForm());

		String[] headerArgs = String.valueOf(header.asNode().getLiteralLexicalForm()).split("\n");

		RDFDatatype dt;
		NodeValue outNode;
		CloseableHttpClient httpclient = HttpClients.createDefault();

		if (iri == null) {
			LOG.debug("Must have two arguments, a URI and a header");
			throw new ExprEvalException("Must have two arguments, a URI and a header");
		}
		if (header == null) {
			LOG.debug("Must have two arguments, a URI and a header");
			throw new ExprEvalException("Must have two arguments, a URI and a header");
		}

		if (!iri.isIRI()) {
			LOG.debug("Must be a URI ");
			throw new ExprEvalException("Must be a URI ");

		}

		String fileURI = iri.asNode().getURI(); // construct the URI from args

		try {

		
			HttpPut req = new HttpPut(fileURI);

			CloseableHttpResponse res = httpclient.execute(req);

			setHeadersFromArgs(req, headerArgs); // REQUEST?

			int code = res.getStatusLine().getStatusCode(); // response code, 201 (ok)

			Header[] headers = extractHeaders(res); // Header from response

			HttpEntity entity = res.getEntity();

			String body = IOUtils.toString(entity.getContent(), StandardCharsets.UTF_8); // will only work if content is

			if (code != 201) {
				LOG.debug("No response from server ");
				throw new ExprEvalException("No response, return with code: " + code);
			}

			else {

				String dtFromHeader = getContentTypeFromHeader(entity); // will get the content type from headers

				String datatypeUri = "http://www.iana.org/assignments/media-types/" + dtFromHeader; // construct
																									// datatype URI from
																									// iana.

				if (checkIanaDt(datatypeUri)) {

					dt = TypeMapper.getInstance().getSafeTypeByName(datatypeUri);
					outNode = new NodeValueNode(NodeFactory.createLiteral(body, dt));

				}

				else {

					dt = TypeMapper.getInstance().getTypeByValue(body); // as String
					outNode = new NodeValueNode(NodeFactory.createLiteral(body, dt));

				}

				return outNode;// return the literals

			}

		} catch (Exception ex) {
			LOG.debug(ex.getMessage());
			throw new ExprEvalException(ex.getMessage());
		}
	}

	private void setHeadersFromArgs(HttpPut req, String[] headerArgs) {
		// TODO Auto-generated method stub
		for (String header : headerArgs) {
			String hArgs[] = header.split(":");
			req.addHeader(hArgs[0], hArgs[1]);
			LOG.info("Header:\t" + hArgs[0] + ":" + hArgs[1] + " added successfully");

		}

	}

	public String getContentTypeFromHeader(HttpEntity entity) {

		return entity.getContentType().toString().replaceAll("Content-Type: ", "").trim();
	}

	public Header[] extractHeaders(CloseableHttpResponse res) {

		return res.getAllHeaders();
	}

	public static boolean checkIanaDt(String URLName) {
		try {
			HttpURLConnection.setFollowRedirects(false);
			HttpURLConnection con = (HttpURLConnection) new URL(URLName).openConnection();
			con.setRequestMethod("HEAD");
			return (con.getResponseCode() == HttpURLConnection.HTTP_OK);
		} catch (Exception e) {
			LOG.debug(e.getMessage());
			return false;
		}
	}


	
	
	
}
