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

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Dataset;
import org.apache.jena.riot.Lang;
import org.apache.jena.sparql.ARQInternalErrorException;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.apache.jena.sparql.function.Function;
import org.apache.jena.sparql.function.FunctionBase3;
import org.apache.jena.sparql.function.FunctionEnv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;

public final class FUN_HTTPGet extends FunctionBase3 {

	private static final Logger LOG = LoggerFactory.getLogger(FUN_HTTPGet.class);

	@Override
	public NodeValue exec(NodeValue v1, NodeValue v2, NodeValue v3) {
		// do something
	
		CloseableHttpClient httpclient = HttpClients.createDefault();
		
		
		String iri; // extract argument 1
		
		try {
			HttpGet req = new HttpGet(iri);
			
			// extract headers in argument 2
//			for() {
//				get.addHeader(name, value);
//			}
			
			// execute query
			CloseableHttpResponse res = httpclient.execute(req);
			int code = res.getStatusLine().getStatusCode();
			Header[] headers = res.getAllHeaders();
			HttpEntity entity = res.getEntity();
			String body = IOUtils.toString(entity.getContent(), StandardCharsets.UTF_8); // will only work if content is string encoded as utf-8 !
			
			// reconstruct the literal, with proper datatype
			
			return null; // return the literal
			
		} catch (Exception ex ) {
			// do seomtheintg
			return null;
		}
	}

}
