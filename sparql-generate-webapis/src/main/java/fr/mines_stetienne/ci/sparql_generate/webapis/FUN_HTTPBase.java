/*
 * Copyright 2021 MINES Saint-Étienne
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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.atlas.lib.Lib;
import org.apache.jena.query.QueryBuildException;
import org.apache.jena.sparql.ARQInternalErrorException;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.apache.jena.sparql.function.FunctionBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class FUN_HTTPBase extends FunctionBase {

	private static final Logger LOG = LoggerFactory.getLogger(FUN_HTTPBase.class);

	@Override
	public void checkBuild(String uri, ExprList args) {
		if (args.size() != 1 && args.size() != 2 && args.size() != 3)
			throw new QueryBuildException("Function '" + Lib.className(this) + "' Wrong number of arguments: Wanted 1, 2, or 3.");
	}

	@Override
	public final NodeValue exec(List<NodeValue> args) {
		if (args == null) {
			throw new ARQInternalErrorException(Lib.className(this) + ": Null args list");
		}
		if (args.size() != 1 && args.size() != 2 && args.size() != 3) {
			throw new ExprEvalException(
					Lib.className(this) + ": Wrong number of arguments: Wanted 1, 2, or 3. got " + args.size());
		}
		String fileURI = processIRI(args.get(0));
		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
			HttpRequestBase req = createRequest(fileURI);
			if (args.size() >= 2) {
				processHeaders(req, args.get(1));
			}
			if (args.size() >= 3) {
				processBody(req, args.get(2));
			}
			try (CloseableHttpResponse res = httpclient.execute(req)) {
				String response = generateResponse(res);
				return new NodeValueString(response);
			}
		} catch (IOException ex) {
			LOG.debug("Exception while executing the HTTP request", ex);
			throw new ExprEvalException(ex.getMessage());
		}
	}

	protected abstract HttpRequestBase createRequest(String fileURI);

	private String processIRI(NodeValue iri) {
		if (iri == null) {
			LOG.debug("Must have at least one arguments, a URI and a header");
			throw new ExprEvalException("Must have two arguments, a URI and a header");
		}
		if (!iri.isIRI()) {
			LOG.debug("First argument must be a URI ");
			throw new ExprEvalException("First argument must be a URI ");
		}
		return iri.asNode().getURI();
	}

	private void processHeaders(HttpRequestBase req, NodeValue headerNode) {
		if (headerNode == null) {
			return;
		}
		if (!headerNode.isLiteral()) {
			LOG.debug("Second argument must be a Literal");
			throw new ExprEvalException("Second argument must be a Literal");
		}
		String[] headerArgs = String.valueOf(headerNode.asNode().getLiteralLexicalForm()).split("\n");
		for (String header : headerArgs) {
			String hArgs[] = header.split(":", 2);
			if (hArgs.length < 2) {
				req.addHeader(hArgs[0], "");
			} else {
				req.addHeader(hArgs[0], hArgs[1]);
			}
		}
	}

	private void processBody(HttpRequestBase req, NodeValue bodyNode) throws UnsupportedEncodingException {
		if (bodyNode == null) {
			return;
		}
		if (!bodyNode.isLiteral()) {
			LOG.debug("Third argument must be a Literal");
			throw new ExprEvalException("Third argument must be a Literal");
		}
		if (!(req instanceof HttpEntityEnclosingRequestBase)) {
			LOG.debug("Request cannot accept a body");
			throw new ExprEvalException("Request cannot accept a body");
		}
		HttpEntityEnclosingRequestBase request = (HttpEntityEnclosingRequestBase) req;
		String body = bodyNode.asNode().getLiteralLexicalForm();
		request.setEntity(new StringEntity(body));

	}

	private String generateResponse(CloseableHttpResponse res) throws IOException {
		StringBuffer response = new StringBuffer();
		response.append(res.getStatusLine() + "\n");
		Header[] headerList = res.getAllHeaders();
		for (int i = 0; i < headerList.length; i++) {
			response.append(headerList[i] + "\n");
		}
		if (res.getEntity() != null) {
			response.append("\n");
			String body = IOUtils.toString(res.getEntity().getContent(), StandardCharsets.UTF_8);
			response.append(body);
		}
		return response.toString();
	}
}
