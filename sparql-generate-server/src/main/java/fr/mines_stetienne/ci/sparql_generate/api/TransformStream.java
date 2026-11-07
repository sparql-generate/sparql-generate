/*
 * Copyright 2017 École des Mines de Saint-Étienne.
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
 
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.apache.commons.io.output.WriterOutputStream;
import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.serializer.SerializationContext;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sparql.util.FmtUtils;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import fr.mines_stetienne.ci.sparql_generate.JerseyApp;
import fr.mines_stetienne.ci.sparql_generate.api.entities.Request;
import fr.mines_stetienne.ci.sparql_generate.ExecutionLimitException;
import fr.mines_stetienne.ci.sparql_generate.engine.PlanFactory;
import fr.mines_stetienne.ci.sparql_generate.engine.RootPlan;
import fr.mines_stetienne.ci.sparql_generate.query.SPARQLExtQuery;
import fr.mines_stetienne.ci.sparql_generate.stream.SPARQLExtStreamManager;
import fr.mines_stetienne.ci.sparql_generate.utils.ContextUtils;

/**
 *
 * @author Maxime Lefrançois
 */
@ServerEndpoint("/transformStream")
public class TransformStream {

	private static final Logger LOG = LoggerFactory.getLogger(TransformStream.class);
	private static final Gson GSON = new Gson();
	private static final StringWriterAppender APPENDER = (StringWriterAppender) org.apache.log4j.Logger.getRootLogger()
			.getAppender("WEBSOCKET");
	private static final Level[] LEVELS = new Level[] { Level.FATAL, Level.ERROR, Level.WARN, Level.INFO, Level.DEBUG,
			Level.TRACE };

	@OnOpen
	public void open(Session session) {
		RequestSetup.init();
		LOG.info("Open session " + session.getId());
		session.setMaxTextMessageBufferSize((int) (2 * Math.pow(2, 20)));
	}

	@OnClose
	public void close(Session session) {
		LOG.info("Closing session " + session.getId());
	}

	@OnMessage
	public void handleMessage(String message, Session session) {
		final SessionManager sessionManager = new SessionManager(session);
		final SessionManager oldSessionManager = APPENDER.putSessionManager(session.getId(), sessionManager);
		if (oldSessionManager != null) {
			oldSessionManager.close();
		}
		sessionManager.clear();

		if (message.getBytes().length > 5 * Math.pow(2, 20)) {
			LOG.warn("Request size exceeded 5 MB.");
			sessionManager.appendLog(
					"ERROR: In this web interface request size cannot exceed 5 MB. Please use another mean to execute SPARQL-Generate queries.");
			sessionManager.flush();
			return;
		}
		Request request = GSON.fromJson(message, Request.class);
		if (request.cancel) {
			return;
		}
		sessionManager.appendLog("INFO: starting transformation\n");
		sessionManager.flush();

		try {
			setLevel(sessionManager, request);
		} catch (Exception ex) {
			logError(sessionManager, "ERROR: while reading parameters:", ex);
			return;
		}

		final Dataset dataset;
		try {
			dataset = RequestSetup.dataset(request, (msg, ex) -> logError(sessionManager, msg, ex));
		} catch (Exception ex) {
			logError(sessionManager, "ERROR: while reading parameters:", ex);
			return;
		}

		final SPARQLExtStreamManager sm;
		try {
			sm = RequestSetup.streamManager(request);
		} catch (RuntimeException ex) {
			logError(sessionManager, "ERROR: while reading parameters:", ex);
			return;
		}

		final SPARQLExtQuery q;
		try {
			q = supply(sessionManager,
					() -> RequestSetup.query(request));
		} catch (InterruptedException | ExecutionException ex) {
			logError(sessionManager, "ERROR: while parsing query:", ex);
			return;
		}

		final ContextUtils.Builder contextBuilder = ContextUtils.build().setStreamManager(sm)
				.setDebugTemplate(request.debugTemplate).setPrefixMapping(q.getPrefixMapping())
				.setInputDataset(dataset).setTimeLimit(JerseyApp.MAX_TIME).setMaxTriples(JerseyApp.MAX_TRIPLES);

		final RootPlan plan;
		try {
			plan = supply(sessionManager, () -> PlanFactory.create(q));
		} catch (InterruptedException | ExecutionException ex) {
			logError(sessionManager, "ERROR: while creating execution plan:", ex);
			return;
		}
		

		java.util.concurrent.Future<?> execution = null;
		try {
			execution = sessionManager.getExecutor().submit(() -> {
				if (!request.stream) {
					Context context = contextBuilder.build();
					if (q.isGenerateType()) {
						Model model = plan.execGenerate(context);
						final StringWriter sw = new StringWriter();
						model.write(sw, "TTL", RequestSetup.BASE);
						sessionManager.appendGenerate(sw.toString());
					} else if (q.isSelectType()) {
						ResultSet resultSet = plan.execSelect(context);
						String output = ResultSetFormatter.asText(resultSet, q);
						sessionManager.appendSelect(output);
					} else if (q.isTemplateType()) {
						String output = plan.execTemplate(context);
						sessionManager.appendTemplate(output);
					} else {
						logError(sessionManager, "Error: unknown query type:", null);
					}
				} else {
					if (q.isGenerateType()) {
						Context context = contextBuilder.setGenerateOutput(new SessionManagerStreamRDF(sessionManager))
								.build();
						plan.execGenerateStream(context);
					} else if (q.isSelectType()) {
						Context context = contextBuilder.setSelectOutput((resultSet) -> {
							String output = ResultSetFormatter.asText(resultSet, q);
							sessionManager.appendSelect(output);
						}).build();
						plan.execSelectStream(context);
					} else if (q.isTemplateType() && request.stream) {
						StringWriter writer = new StringWriter() {
							public void flush() {
								String s = getBuffer().toString();
								getBuffer().delete(0, getBuffer().length());
								sessionManager.appendTemplate(s);
							};
						};
						IndentedWriter out = new IndentedWriter(new WriterOutputStream(new BufferedWriter(writer), StandardCharsets.UTF_8));
			        	Context context = contextBuilder.setTemplateOutput(out).build();
			        	plan.execTemplateStream(context);
					} else {
						logError(sessionManager, "Error: unknown query type:", null);
					}
				}
			});
			// the execution checks its own time limit; one more second here
			execution.get(JerseyApp.MAX_TIME + 1, TimeUnit.SECONDS);
		} catch (TimeoutException ex) {
			// not an error: the output so far was sent
			logError(sessionManager, String.format(
					"Time limit of %s s reached, output truncated. Use the command-line tool for longer executions.",
					JerseyApp.MAX_TIME), null);
		} catch (final InterruptedException ex) {
			logError(sessionManager, "Interrupted while executing the request", ex);
		} catch (final ExecutionException ex) {
			final ExecutionLimitException limit = ExecutionLimitException.findIn(ex);
			if (limit != null) {
				// not an error: the output so far was sent
				logError(sessionManager, limit.getMessage() + " Use the command-line tool for longer executions.", null);
			} else {
				logError(sessionManager, "An exception occurred", ex);
			}
		} finally {
			if (execution != null) {
				// interrupts the execution if it still runs
				execution.cancel(true);
			}
			sessionManager.flush();
			sessionManager.close();
		}

	}

	@OnError
	public void handleError(Throwable error) {
		if (error instanceof TimeoutException) {
			LOG.error("TimeoutException");
		} else {
			LOG.error("Error", error);
		}
	}

	private void logError(SessionManager sessionManager, String error, Exception ex) {
		final StringWriter sw = new StringWriter();
		sw.append(error);
		sw.append("\n");
		if (ex != null) {
			PrintWriter pw = new PrintWriter(sw, true);
			ex.printStackTrace(pw);
			sw.append(pw.toString());
		}
		sessionManager.appendLog(sw.toString());
		sessionManager.flush();
	}

    private <T> T supply(SessionManager sessionManager, Supplier<T> task) throws InterruptedException, ExecutionException {
        return CompletableFuture.supplyAsync(task, sessionManager.getExecutor()).get();
    }

	private void setLevel(SessionManager sessionManager, Request request) {
		sessionManager.setLevel(LEVELS[request.loglevel]);
	}

	private static class SessionManagerStreamRDF implements StreamRDF {

		private final SessionManager sessionManager;

		private final SerializationContext sc = new SerializationContext();

		public SessionManagerStreamRDF(SessionManager sessionManager) {
			this.sessionManager = sessionManager;
		}

		@Override
		public void start() {
		}

		@Override
		public void triple(Triple triple) {
			sessionManager.appendGenerate(FmtUtils.stringForTriple(triple, sc) + " .");
		}

		@Override
		public void quad(Quad quad) {
		}

		@Override
		public void base(String base) {
			sc.getPrologue().setBaseURI(base);
			sessionManager.appendGenerate("@base <" + base + "> .");
		}

		@Override
		public void prefix(String prefix, String iri) {
			sc.getPrefixMapping().setNsPrefix(prefix, iri);
			sessionManager.appendGenerate("@prefix " + prefix + ": <" + iri + "> .");
		}

		@Override
		public void finish() {
		}
	}
}
