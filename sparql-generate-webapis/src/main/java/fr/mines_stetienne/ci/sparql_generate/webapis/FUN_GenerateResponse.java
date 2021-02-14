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
import java.io.InputStreamReader;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates a Literal from a CloseableHttpResponse.
 *
 * @author Omar Qawasmeh
 * 
 * @organization Ecole des Mines de Saint Etienne
 */
public class FUN_GenerateResponse {
	private static final Logger LOG = LoggerFactory.getLogger(FUN_GenerateResponse.class);

	public static String generateResponse(CloseableHttpResponse res) throws UnsupportedOperationException, IOException {

		BufferedReader reader = new BufferedReader(new InputStreamReader(res.getEntity().getContent()));

		String body;

		StringBuffer response = new StringBuffer();
		response.append(res.getStatusLine() + "\n");

		Header[] headerList = extractHeaders(res);
		for (int i = 0; i < headerList.length; i++) {
			response.append(headerList[i] + "\n");
		}
		response.append("\n");
		while ((body = reader.readLine()) != null) {
			response.append(body);
		}
		reader.close();
		// LOG.info("Generated response:\t" + response.toString());
		return response.toString();
	}

	public static Header[] extractHeaders(CloseableHttpResponse res) {

		return res.getAllHeaders();
	}

}
