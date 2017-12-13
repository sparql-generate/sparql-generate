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

import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerate;
import com.github.thesmartenergy.sparql.generate.jena.engine.PlanFactory;
import com.github.thesmartenergy.sparql.generate.jena.engine.RootPlan;
import com.github.thesmartenergy.sparql.generate.jena.query.SPARQLGenerateQuery;
import com.github.thesmartenergy.sparql.generate.jena.stream.LocatorStringMap;
import com.github.thesmartenergy.sparql.generate.jena.stream.SPARQLGenerateStreamManager;
import com.google.gson.Gson;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import java.io.IOException;
import java.io.StringWriter;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import org.apache.commons.io.IOUtils;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.serializer.SerializationContext;
import org.apache.jena.sparql.util.FmtUtils;
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
    private final StringWriterAppender appender = (StringWriterAppender) org.apache.log4j.Logger.getRootLogger().getAppender("WEBSOCKET");

    private Thread thread;
    
    @OnOpen
    public void open(Session session) {
        LOG.info("Establishing connection");
    }

    @OnClose
    public void close(Session session) {
        LOG.info("Closing connection");
    }

    @OnMessage
    public void handleMessage(String message, Session session) throws IOException, InterruptedException {
        //todo check size of message.
        System.out.println(Thread.currentThread().getName());
        appender.putSession(Thread.currentThread(), session);
        try {
            session.getBasicRemote().sendText(gson.toJson(new Response("", "", true)));
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(TransformStream.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }

        if (message.getBytes().length > 2 * Math.pow(2, 20)) {
            LOG.error("In this web interface request size cannot exceed 2 MB. Please use the executable jar instead.");
            appender.removeSession(Thread.currentThread());
            return;
        }

        Dataset dataset = DatasetFactory.create();
        LocatorStringMap loc = new LocatorStringMap();
        String defaultquery;
        boolean stream;
        try {
            Request request = gson.fromJson(message, Request.class);

            defaultquery = request.defaultquery;
            stream = request.stream;

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

            SPARQLGenerateStreamManager sm = SPARQLGenerateStreamManager.makeStreamManager(loc);
            SPARQLGenerate.setStreamManager(sm);
        } catch (Exception ex) {
            System.out.println("error while reading parameters: " + ex.getMessage());
            return;
        }

        final ExecutorService service = Executors.newSingleThreadExecutor();

        try {
            final Future f = service.submit(() -> {
                thread = Thread.currentThread();
                appender.putSession(thread, session);
                SPARQLGenerateQuery q = (SPARQLGenerateQuery) QueryFactory.create(defaultquery, SPARQLGenerate.SYNTAX);
                RootPlan plan = PlanFactory.create(q);

                if (stream) {
                    StreamRDF outputStream = new WebSocketRDF(session, q.getPrefixMapping());
                    outputStream.start();
                    plan.exec(dataset, outputStream);
                } else {
                    Model model = plan.exec(dataset);
                    StringWriter sw = new StringWriter();
                    model.write(sw, "TTL", "http://example.org/");
                    LOG.trace("end of transformation");
                    try {
                        session.getBasicRemote().sendText(gson.toJson(new Response("", sw.toString(), false)));
                    } catch (Exception ex) {
                        System.out.println("error while sending result: " + ex.getMessage());
                    }
                }
            });
            f.get(10, TimeUnit.SECONDS);
        } catch (final TimeoutException ex) {
            LOG.error("In this web interface requests cannot exceed 10 s. Please use the executable jar instead.");
        } catch (final Exception ex) {
            LOG.error("An exception occurred:" + ex.getMessage());
            throw new RuntimeException(ex);
        } finally {
            appender.removeSession(thread);
            service.shutdown();
        }

        appender.removeSession(Thread.currentThread());
    }

    private static class WebSocketRDF implements StreamRDF {

        private final Session session;
        private final PrefixMapping pm;
        private final SerializationContext context;

        public WebSocketRDF(Session session, PrefixMapping pm) {
            this.session = session;
            this.pm = pm;
            context = new SerializationContext(pm);
        }

        @Override
        public void start() {
            StringBuilder sb = new StringBuilder();
            pm.getNsPrefixMap().forEach((prefix, uri) -> {
                sb.append("@Prefix ").append(prefix).append(": <").append(uri).append("> .\n");
            });
            try {
                session.getBasicRemote().sendText(gson.toJson(new Response("", sb.toString() + "\n", false)));
            } catch (IOException ex) {
                LOG.error("IOException ", ex);
            }
        }

        @Override
        public void base(String string) {
            try {
                session.getBasicRemote().sendText(gson.toJson(new Response("", "@base <" + string + ">\n", false)));
            } catch (IOException ex) {
                LOG.error("IOException ", ex);
            }
        }

        @Override
        public void prefix(String prefix, String uri) {
            if (!uri.equals(pm.getNsPrefixURI(prefix))) {
                pm.setNsPrefix(prefix, uri);
                StringBuilder sb = new StringBuilder();
                sb.append("@Prefix ").append(prefix).append(": <").append(uri).append("> .\n");
                try {
                    session.getBasicRemote().sendText(gson.toJson(new Response("", sb.toString(), false)));
                } catch (IOException ex) {
                    LOG.error("IOException ", ex);
                }
            }
        }

        @Override
        public void triple(Triple triple) {
            try {
                Response response = new Response("", FmtUtils.stringForTriple(triple, context) + " .\n", false);
                session.getBasicRemote().sendText(gson.toJson(response));
            } catch (IOException ex) {
                LOG.error("IOException ", ex);
            }
        }

        @Override
        public void quad(Quad quad) {
        }

        @Override
        public void finish() {
            LOG.trace("end of transformation");
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

}
