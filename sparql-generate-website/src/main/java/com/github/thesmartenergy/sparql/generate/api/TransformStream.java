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
package com.github.thesmartenergy.sparql.generate.api;

import com.github.thesmartenergy.sparql.generate.jena.cli.Response;
import com.github.thesmartenergy.sparql.generate.jena.cli.Request;
import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerate;
import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerateContext;
import com.github.thesmartenergy.sparql.generate.jena.engine.PlanFactory;
import com.github.thesmartenergy.sparql.generate.jena.engine.RootPlan;
import com.github.thesmartenergy.sparql.generate.jena.iterator.library.ITER_HTTPGet;
import com.github.thesmartenergy.sparql.generate.jena.iterator.library.ITER_WebSocket;
import com.github.thesmartenergy.sparql.generate.jena.query.SPARQLGenerateQuery;
import com.github.thesmartenergy.sparql.generate.jena.stream.LocatorStringMap;
import com.github.thesmartenergy.sparql.generate.jena.stream.SPARQLGenerateStreamManager;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import org.apache.commons.io.IOUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 *
 * @author maxime.lefrancois
 */
@ServerEndpoint("/transformStream")
public class TransformStream {

    private static final Logger LOG = LoggerFactory.getLogger(TransformStream.class);
    private static final Gson GSON = new Gson();
    private static final String BASE = "http://example.org/";
    private static final StringWriterAppender APPENDER = (StringWriterAppender) org.apache.log4j.Logger.getRootLogger().getAppender("WEBSOCKET");
    private static final Map<String, SessionManager> MANAGERS =  new HashMap<>();

    @OnOpen
    public void open(Session session) {
        LOG.info("Open session " + session.getId());
        final SessionManager sessionManager = new SessionManager(session);
        MANAGERS.put(session.getId(), sessionManager);
        APPENDER.addSessionManager(sessionManager);
        session.setMaxTextMessageBufferSize((int) (2 * Math.pow(2, 20)));
    }

    @OnClose
    public void close(Session session) {
        LOG.info("Closing session " + session.getId());
        final SessionManager sessionManager = MANAGERS.get(session.getId());
        MANAGERS.remove(session.getId());
        APPENDER.removeSessionManager(sessionManager);
    }

    @OnMessage
    public void handleMessage(String message, Session session) throws IOException, InterruptedException {
        final SessionManager sessionManager = MANAGERS.get(session.getId());
        sessionManager.clear();

        if (message.getBytes().length > 2 * Math.pow(2, 20)) {
            LOG.warn("Request size exceeded 2 MB.");
            sessionManager.appendLog("ERROR: In this web interface request size cannot exceed 2 MB. Please use the executable jar instead.");
            sessionManager.flush();
            return;
        }

        // create context
        final SPARQLGenerateContext context;
        final Dataset dataset = DatasetFactory.create();
        final LocatorStringMap loc = new LocatorStringMap();
        final String defaultquery;
        try {
            Request request = GSON.fromJson(message, Request.class);
            defaultquery = request.defaultquery;
            final SPARQLGenerateStreamManager sm = readRequest(request, dataset, loc);
            context = new SPARQLGenerateContext(sm);
            sessionManager.setContext(context);
        } catch (JsonSyntaxException | IOException ex) {
            final StringWriter sw = new StringWriter();
            sw.append("ERROR: while reading parameters:");
            LOG.error(sw.toString(), ex);
            sw.append("\n");
            PrintWriter pw = new PrintWriter(sw, true);
            ex.printStackTrace(pw);
            sw.append(pw.toString());
            sessionManager.appendLog(sw.toString());
            sessionManager.flush();
            return;
        }

        final ExecutorService service = Executors.newSingleThreadExecutor();
        final Model model = ModelFactory.createDefaultModel();
        try {
            final Future f = service.submit(() -> {
                context.registerThread();
                SPARQLGenerateQuery q = (SPARQLGenerateQuery) QueryFactory.create(defaultquery, SPARQLGenerate.SYNTAX);
                RootPlan plan = PlanFactory.create(q);
                plan.exec(dataset, model, context);
                LOG.info("END EXECUTION");
            });
            LOG.info("START  GET 10");
            f.get(10, TimeUnit.SECONDS);
            LOG.info("END OF GET 10");
        } catch (final TimeoutException ex) {
            LOG.info("TIMEOUT EXCEPTION");
            LOG.error("In this web interface requests cannot exceed 10 s. Please use the executable jar instead.", ex);
        } catch (final Exception ex) {
            LOG.error("An exception occurred", ex);
        }
        final StringWriter sw = new StringWriter();
        model.write(sw, "TTL", "http://example.org/");
        sessionManager.appendResult(sw.toString());
        sessionManager.flush();
        LOG.info("FLUSHED RESULT \n" + sw.toString());
    }

    @OnError
    public void handleError(Throwable error) {
        if (error instanceof TimeoutException) {
            LOG.error("TiomeoutException");
        } else {
            LOG.error("Error", error);
        }
    }

    static {
        com.jayway.jsonpath.Configuration.setDefaults(new com.jayway.jsonpath.Configuration.Defaults() {

            private final JsonProvider jsonProvider = new JacksonJsonProvider();
            private final MappingProvider mappingProvider
                    = new JacksonMappingProvider();

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

    private SPARQLGenerateStreamManager readRequest(Request request, Dataset dataset, LocatorStringMap loc) throws IOException {
        request.namedqueries.forEach((nq) -> {
            loc.put(nq.uri, nq.string, nq.mediatype);
        });
        Model g = ModelFactory.createDefaultModel();
        RDFDataMgr.read(g, IOUtils.toInputStream(request.defaultgraph, "UTF-8"), BASE, Lang.TTL);
        dataset.setDefaultModel(g);

        request.namedgraphs.forEach((ng) -> {
            Model model = ModelFactory.createDefaultModel();
            try {
                RDFDataMgr.read(model, IOUtils.toInputStream(ng.string, "UTF-8"), BASE, Lang.TTL);
            } catch (IOException ex) {
                LOG.warn("error while parsing graph " + ng.uri, ex);
            }
            dataset.addNamedModel(ng.uri, model);
        });

        request.documentset.forEach((doc) -> {
            loc.put(doc.uri, doc.string, doc.mediatype);
        });
        return SPARQLGenerateStreamManager.makeStreamManager(loc);
    }

}
