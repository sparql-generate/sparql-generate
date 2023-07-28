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
import java.util.EnumSet;
import java.util.Set;
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

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.WriterOutputStream;
import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.serializer.SerializationContext;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sparql.util.FmtUtils;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;

import fr.mines_stetienne.ci.sparql_generate.JerseyApp;
import fr.mines_stetienne.ci.sparql_generate.SPARQLExt;
import fr.mines_stetienne.ci.sparql_generate.api.entities.Request;
import fr.mines_stetienne.ci.sparql_generate.engine.PlanFactory;
import fr.mines_stetienne.ci.sparql_generate.engine.RootPlan;
import fr.mines_stetienne.ci.sparql_generate.query.SPARQLExtQuery;
import fr.mines_stetienne.ci.sparql_generate.stream.LocatorStringMap;
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
	private static final String TURTLE_MEDIATYPE = "text/turtle";
	private static final String BASE = "http://example.org/";

	@OnOpen
	public void open(Session session) {
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
			dataset = getDataset(sessionManager, request);
		} catch (Exception ex) {
			logError(sessionManager, "ERROR: while reading parameters:", ex);
			return;
		}

		final SPARQLExtStreamManager sm;
		try {
			sm = getStreamManager(request);
		} catch (JsonSyntaxException | IOException ex) {
			logError(sessionManager, "ERROR: while reading parameters:", ex);
			return;
		}

		final SPARQLExtQuery q;
		try {
			q = supply(sessionManager,
					() -> (SPARQLExtQuery) QueryFactory.create(request.defaultquery, SPARQLExt.SYNTAX));
		} catch (InterruptedException | ExecutionException ex) {
			logError(sessionManager, "ERROR: while parsing query:", ex);
			return;
		}

		final ContextUtils.Builder contextBuilder = ContextUtils.build().setStreamManager(sm)
				.setDebugTemplate(request.debugTemplate).setPrefixMapping(q.getPrefixMapping())
				.setInputDataset(dataset);

		final RootPlan plan;
		try {
			plan = supply(sessionManager, () -> PlanFactory.create(q));
		} catch (InterruptedException | ExecutionException ex) {
			logError(sessionManager, "ERROR: while creating execution plan:", ex);
			return;
		}
		

		try {
			sessionManager.getExecutor().submit(() -> {
				if (!request.stream) {
					Context context = contextBuilder.build();
					if (q.isGenerateType()) {
						Model model = plan.execGenerate(context);
						final StringWriter sw = new StringWriter();
						model.write(sw, "TTL", BASE);
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
			}).get(JerseyApp.MAX_TIME, TimeUnit.SECONDS);
		} catch (TimeoutException ex) {
			logError(sessionManager,
					"In this web interface query execution cannot exceed 10 s. Consider using the executable jar instead.",
					ex);
		} catch (final InterruptedException ex) {
			logError(sessionManager, "Interrupted while executing the request", ex);
		} catch (final ExecutionException ex) {
			logError(sessionManager, "An exception occurred", ex);
		} finally {
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

	static {
		com.jayway.jsonpath.Configuration.setDefaults(new com.jayway.jsonpath.Configuration.Defaults() {

			private final JsonProvider jsonProvider = new JacksonJsonProvider();
			private final MappingProvider mappingProvider = new JacksonMappingProvider();

			@Override
			public JsonProvider jsonProvider() {
				return jsonProvider;
			}

			@Override
			public MappingProvider mappingProvider() {
				return mappingProvider;
			}

			@Override
			public Set<Option> options() {
				return EnumSet.noneOf(Option.class);
			}
		});
	}

	private Dataset getDataset(SessionManager sessionManager, Request request) throws IOException {
		Dataset dataset = DatasetFactory.create();
		Model g = ModelFactory.createDefaultModel();
		RDFDataMgr.read(g, IOUtils.toInputStream(request.defaultgraph, "UTF-8"), BASE, Lang.TTL);
		dataset.setDefaultModel(g);
		request.namedgraphs.forEach((ng) -> {
			Model model = ModelFactory.createDefaultModel();
			try {
				RDFDataMgr.read(model, IOUtils.toInputStream(ng.string, "UTF-8"), BASE, Lang.TTL);
			} catch (Exception ex) {
				logError(sessionManager, "WARN: unable to parse graph " + ng.uri, ex);
			}
			dataset.addNamedModel(ng.uri, model);
		});
		return dataset;
	}

	private SPARQLExtStreamManager getStreamManager(Request request) throws IOException {
		final LocatorStringMap loc = new LocatorStringMap();
		request.namedqueries.forEach((nq) -> {
			loc.put(nq.uri, nq.string, nq.mediatype);
		});
		request.namedgraphs.forEach((ng) -> {
			loc.put(ng.uri, ng.string, TURTLE_MEDIATYPE);
		});
		request.documentset.forEach((doc) -> {
			loc.put(doc.uri, doc.string, doc.mediatype);
		});
		return SPARQLExtStreamManager.makeStreamManager(loc);
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
			sc.setBaseIRI(base);
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
