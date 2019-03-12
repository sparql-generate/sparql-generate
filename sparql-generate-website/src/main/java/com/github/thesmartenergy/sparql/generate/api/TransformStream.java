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
import com.github.thesmartenergy.sparql.generate.jena.engine.PlanFactory;
import com.github.thesmartenergy.sparql.generate.jena.engine.RootPlan;
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
import java.io.IOException;
import java.io.StringWriter;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import org.apache.commons.io.IOUtils;
import org.apache.jena.query.ARQ;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.util.Context;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 *
 * @author maxime.lefrancois
 */
@ServerEndpoint("/transformStream")
public class TransformStream {

    private static final Logger LOG = LoggerFactory.getLogger(TransformStream.class);
    private static final Gson gson = new Gson();
    private static String base = "http://example.org/";
    private static StringWriterAppender appender = (StringWriterAppender) org.apache.log4j.Logger.getRootLogger().getAppender("WEBSOCKET");

    private final Map<Session, Context> contexts = new HashMap<>();
    
    @OnOpen
    public void open(Session session) {
        final Context context = new Context(ARQ.getContext());
        LOG.info("Starting context for session " + session);
        context.set(SessionManager.SYMBOL, new SessionManager(session));
        contexts.put(session, context);
        appender.addContext(context);
        session.setMaxTextMessageBufferSize((int) (2 * Math.pow(2, 20)));
    }

    @OnClose
    public void close(Session session) {
        final Context context = contexts.remove(session);
        LOG.info("Stopping context for session " + session);
        appender.removeContext(context);
        final SessionManager sessionManager = context.get(SessionManager.SYMBOL);
        sessionManager.stop();
    }

    @OnMessage
    public void handleMessage(String message, Session session) throws IOException, InterruptedException {
        if(!contexts.containsKey(session)) {
            throw new IOException("can't find context");
        }
        final Context context = contexts.get(session);
        final Set<Thread> threads = new HashSet<>();
        final SessionManager sessionManager = context.get(SessionManager.SYMBOL);
        final Thread thread = Thread.currentThread();
        threads.add(thread);
        context.set(SPARQLGenerate.THREAD, threads);
        sessionManager.appendResponse(new Response("", "", true));
        LOG.info("Handling message for session " + session);
        LOG.debug("Having thread " + thread.getName());

        if (message.getBytes().length > 2 * Math.pow(2, 20)) {
            LOG.error("In this web interface request size cannot exceed 2 MB. Please use the executable jar instead.");
            return;
        }

        final Dataset dataset = DatasetFactory.create();
        final LocatorStringMap loc = new LocatorStringMap();
        final String defaultquery;
        try {
            Request request = gson.fromJson(message, Request.class);
            LOG.info("Executing request " + gson.toJson(request));

            defaultquery = request.defaultquery;
            request.namedqueries.forEach((nq) -> {
                loc.put(nq.uri, nq.string, nq.mediatype);
            });
            Model g = ModelFactory.createDefaultModel();
            RDFDataMgr.read(g, IOUtils.toInputStream(request.defaultgraph, "UTF-8"), base, Lang.TTL);
            dataset.setDefaultModel(g);

            request.namedgraphs.forEach((ng) -> {
                Model model = ModelFactory.createDefaultModel();
                try {
                    RDFDataMgr.read(model, IOUtils.toInputStream(ng.string, "UTF-8"), base, Lang.TTL);
                } catch (IOException ex) {
                    LOG.warn("error while parsing graph " + ng.uri, ex);
                }
                dataset.addNamedModel(ng.uri, model);
            });

            request.documentset.forEach((doc) -> {
                loc.put(doc.uri, doc.string, doc.mediatype);
            });

            context.set(SPARQLGenerate.STREAM_MANAGER, SPARQLGenerateStreamManager.makeStreamManager(loc));
        } catch (JsonSyntaxException | IOException ex) {
            LOG.error("Error while reading parameters:", ex);
            return;
        }

        final ExecutorService service = Executors.newSingleThreadExecutor();

        try {
            final Future f = service.submit(() -> {
                threads.add(Thread.currentThread());

                SPARQLGenerateQuery q = (SPARQLGenerateQuery) QueryFactory.create(defaultquery, SPARQLGenerate.SYNTAX);
                RootPlan plan = PlanFactory.create(q);

                Model model = plan.exec(dataset, context);
                StringWriter sw = new StringWriter();
                model.write(sw, "TTL", "http://example.org/");
                sessionManager.appendResponse(new Response("", sw.toString(), false));
            });
            f.get(10, TimeUnit.SECONDS);
        } catch (final TimeoutException ex) {
            LOG.error("In this web interface requests cannot exceed 10 s. Please use the executable jar instead.", ex);
        } catch (final Exception ex) {
            LOG.error("An exception occurred", ex);
        } finally {
            service.awaitTermination(100, TimeUnit.MILLISECONDS);
        }

    }

    @OnError
    public void handleError(Throwable error) {
        LOG.error("Error", error);
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

}
