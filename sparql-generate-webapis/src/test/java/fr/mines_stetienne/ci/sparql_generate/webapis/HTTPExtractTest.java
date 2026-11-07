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
package fr.mines_stetienne.ci.sparql_generate.webapis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.NodeValue;
import org.junit.Test;

/**
 * fun:HTTPExtractBody, fun:HTTPExtractHeader and fun:HTTPExtractResponseCode
 * read the response of a server of any version of HTTP.
 */
public class HTTPExtractTest {

	private static final String[] VERSIONS = { "HTTP/1.0", "HTTP/1.1", "HTTP/2" };

	private static NodeValue response(String version) {
		return NodeValue.makeString(version + " 404 Not Found\nContent-Type: text/plain\nServer: test\n\nno such page");
	}

	@Test
	public void body() {
		for (String version : VERSIONS) {
			assertEquals(version, "no such page", new FUN_HTTPExtractBody().exec(response(version)).getString());
		}
	}

	@Test
	public void headers() {
		for (String version : VERSIONS) {
			assertEquals(version, "Content-Type: text/plain\nServer: test",
					new FUN_HTTPExtractHeader().exec(response(version)).getString());
		}
	}

	@Test
	public void responseCode() {
		for (String version : VERSIONS) {
			assertEquals(version, 404, new FUN_HTTPExtractResponseCode().exec(response(version)).getInteger().intValue());
		}
	}

	@Test
	public void notAResponse() {
		try {
			new FUN_HTTPExtractBody().exec(NodeValue.makeString("hello\n\nworld"));
			fail("not an HTTP response");
		} catch (ExprEvalException ex) {
			// expected
		}
	}
}
