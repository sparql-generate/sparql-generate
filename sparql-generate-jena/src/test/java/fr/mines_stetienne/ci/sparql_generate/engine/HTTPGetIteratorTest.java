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
package fr.mines_stetienne.ci.sparql_generate.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.junit.Test;

import com.sun.net.httpserver.HttpServer;

import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;

/**
 * iter:HTTPGet, on a local HTTP server.
 */
public class HTTPGetIteratorTest {

	static {
		SPARQLExt.init();
	}

	@Test(timeout = 30000)
	public void callsTypedByTheirMediaType() throws Exception {
		final AtomicInteger calls = new AtomicInteger();
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/t", (exchange) -> {
			byte[] body = ("{\"call\": " + calls.incrementAndGet() + "}").getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
			exchange.sendResponseHeaders(200, body.length);
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(body);
			}
		});
		server.start();
		try {
			String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/t";
			long start = System.nanoTime();
			Model model = PlanFactory
					.create("PREFIX iter: <http://w3id.org/sparql-generate/iter/> "
							+ "GENERATE { <http://ex/s> <http://ex/p> ?m } ITERATOR iter:HTTPGet(<" + url + ">, 1, 2) AS ?m")
					.execGenerate(ContextUtils.build().build());
			long millis = (System.nanoTime() - start) / 1_000_000;
			List<RDFNode> objects = model.listObjects().toList();
			assertEquals(EngineTestUtils.write(model), 2, objects.size());
			for (RDFNode o : objects) {
				assertEquals("https://www.iana.org/assignments/media-types/application/json",
						o.asLiteral().getDatatypeURI());
			}
			// one wait of one second between the two calls, none after the last
			assertTrue("took " + millis + " ms", millis < 1900);
		} finally {
			server.stop(0);
		}
	}
}
