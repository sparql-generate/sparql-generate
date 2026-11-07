/*
 * Copyright 2026 École des Mines de Saint-Étienne.
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
package fr.mines_stetienne.ci.sparql_generate.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.mines_stetienne.ci.sparql_generate.JerseyApp;

/**
 * Starts the API of the playground ({@link JerseyApp} under {@code api/}) and
 * its WebSocket endpoint ({@link TransformStream}) in an embedded Jetty, as
 * the WAR is deployed under {@code /sparql-generate}, and calls them. It fails
 * if Jersey cannot scan the classes, as Jersey 2.28 could not read Java 17
 * class files.
 */
public class ServerSmokeTest {

	private static Server server;

	private static String base;

	private static final HttpClient CLIENT = HttpClient.newHttpClient();

	@BeforeClass
	public static void start() throws Exception {
		server = new Server(0);
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/sparql-generate");
		ServletHolder jersey = new ServletHolder(new ServletContainer(new JerseyApp()));
		context.addServlet(jersey, "/api/*");
		WebSocketServerContainerInitializer.configure(context,
				(servletContext, container) -> container.addEndpoint(TransformStream.class));
		server.setHandler(context);
		server.start();
		int port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
		base = "http://localhost:" + port + "/sparql-generate/";
	}

	@AfterClass
	public static void stop() throws Exception {
		if (server != null) {
			server.stop();
		}
	}

	private static HttpResponse<String> get(String path) throws Exception {
		return CLIENT.send(HttpRequest.newBuilder(URI.create(base + path)).build(),
				HttpResponse.BodyHandlers.ofString());
	}

	@Test
	public void listsAndLoadsExamples() throws Exception {
		HttpResponse<String> list = get("api/example/generate");
		assertEquals(200, list.statusCode());
		assertTrue(list.body(), list.body().contains("\"00-Source\""));
		HttpResponse<String> example = get("api/example/template/04-Group");
		assertEquals(200, example.statusCode());
		assertTrue(example.body().contains("\"defaultquery\""));
	}

	@Test
	public void transformsWithTheWebApi() throws Exception {
		String query = URLEncoder.encode("GENERATE { <http://ex/s> <http://ex/p> 42 } WHERE {}", StandardCharsets.UTF_8);
		HttpResponse<String> response = get("api/transform?query=" + query);
		assertEquals(200, response.statusCode());
		assertTrue(response.body(), response.body().contains("42"));
	}

	@Test
	public void runsAnExampleOverTheWebSocket() throws Exception {
		String request = get("api/example/template/04-Group").body();
		CompletableFuture<String> result = new CompletableFuture<>();
		StringBuilder received = new StringBuilder();
		WebSocket socket = CLIENT.newWebSocketBuilder()
				.buildAsync(URI.create(base.replace("http:", "ws:") + "transformStream"), new WebSocket.Listener() {
					@Override
					public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
						received.append(data);
						if (received.toString().contains("Air Quality Index of")) {
							result.complete(received.toString());
						}
						webSocket.request(1);
						return null;
					}
				}).get(10, TimeUnit.SECONDS);
		socket.sendText(request, true);
		String messages = result.get(30, TimeUnit.SECONDS);
		assertTrue(messages, messages.contains("\"type\":2"));
		socket.sendClose(WebSocket.NORMAL_CLOSURE, "done");
	}
}
