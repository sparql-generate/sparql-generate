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
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
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
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.WriterAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

/**
 *
 * @author maxime.lefrancois
 */
@ServerEndpoint("/transformStream")
public class TransformStream {

    private static final Logger LOG = LogManager.getLogger(TransformStream.class);
    private Appender appender;
    private String appenderName;
    private static long MAX_DURATION = TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES);
    private static int MAX_TRIPLES = 5000;
    private static String base = "http://example.org/";
    private StringWriter swLog;

    @PostConstruct
    private void postConstruct() {
        appenderName = UUID.randomUUID().toString();
    }

    @OnOpen
    public void open(Session session) {
        swLog = setLogger(null);
        LOG.info("Establishing connection");
    }

    @OnClose
    public void close(Session session) {
        LOG.info("Closing connection");
        removeAppender();
    }

    @OnMessage
    public void handleMessage(String message, Session session) throws IOException, InterruptedException {
        Dataset dataset = DatasetFactory.create();
        LocatorStringMap loc = new LocatorStringMap();
        String defaultquery;
        try {
            Request request = new Gson().fromJson(message, Request.class);

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

            SPARQLGenerateStreamManager sm = SPARQLGenerateStreamManager.makeStreamManager(loc);
            SPARQLGenerate.setStreamManager(sm);
        } catch (Exception ex) {
            System.out.println("error while reading parameters: " + ex.getMessage());
            return;
        }

        SPARQLGenerateQuery q = (SPARQLGenerateQuery) QueryFactory.create(defaultquery, SPARQLGenerate.SYNTAX);
        RootPlan plan = PlanFactory.create(q);

        StreamRDF outputStream = new WebSocketRDF(session, swLog, q.getPrefixMapping());
        outputStream.start();
        plan.exec(dataset, outputStream);
        outputStream.finish();
    }

    private static class WebSocketRDF implements StreamRDF {

        private final Session session;
        private final StringWriter swLog;
        private final PrefixMapping pm;
        private final SerializationContext context;

        public WebSocketRDF(Session session, StringWriter swLog, PrefixMapping pm) {
            this.session = session;
            this.swLog = swLog;
            this.pm = pm;
            context = new SerializationContext(pm);
        }

        @Override
        public void start() {
            try {
                session.getBasicRemote().sendText("clear");
            } catch (IOException ex) {
                LOG.error("IOException ", ex);
            }
        }

        @Override
        public void base(String string) {
            try {
                session.getBasicRemote().sendText(new Gson().toJson(new Response("", "@base <" + string + ">\n")));
            } catch (IOException ex) {
                LOG.error("IOException ", ex);
            }
        }

        @Override
        public void prefix(String prefix, String uri) {
            pm.setNsPrefix(prefix, uri);
            StringBuilder sb = new StringBuilder();
            sb.append("@Prefix ").append(prefix).append(": <").append(uri).append("> .\n");
            try {
                session.getBasicRemote().sendText(new Gson().toJson(new Response("", sb.toString())));
            } catch (IOException ex) {
                LOG.error("IOException ", ex);
            }
        }

        @Override
        public void triple(Triple triple) {
            try {
                Response response = new Response(swLog.toString(), FmtUtils.stringForTriple(triple, context) + " .\n");
//                    swLog.getBuffer().setLength(0);
                session.getBasicRemote().sendText(new Gson().toJson(response));
            } catch (IOException ex) {
                LOG.error("IOException ", ex); 
            }
        }

        @Override
        public void quad(Quad quad) {
        }

        @Override
        public void finish() {
            LOG.info("end of transformation");
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

    StringWriter setLogger(String logLevel) {
//        final LoggerContext context = LoggerContext.getContext(true);
//        final org.apache.logging.log4j.core.config.Configuration config = context.getConfiguration();
//        final PatternLayout layout = PatternLayout.newBuilder().withPattern("%d{HH:mm:ss,SSS} %-5p %t:%C:%L%n%m%n").build();

        StringWriter sw = new StringWriter();
//        appender = WriterAppender.createAppender(layout, null, sw, appenderName, false, true);
//        appender.start();
//        config.addAppender(appender);
//        addAppender(logLevel);

        return sw;
    }

    void addAppender(String logLevel) {
//        Level level;
//        if (logLevel == null) {
//            level = Level.TRACE;
//        } else {
//            level = Level.getLevel(logLevel);
//            if (level == null) {
//                level = Level.TRACE;
//            }
//        }
//        final LoggerContext context = LoggerContext.getContext(true);
//        final org.apache.logging.log4j.core.config.Configuration config = context.getConfiguration();
//        for (final LoggerConfig loggerConfig : config.getLoggers().values()) {
//            loggerConfig.addAppender(appender, level, null);
//        }
    }

    void removeAppender() {
//        final LoggerContext context = LoggerContext.getContext(true);
//        final org.apache.logging.log4j.core.config.Configuration config = context.getConfiguration();
//        for (final LoggerConfig loggerConfig : config.getLoggers().values()) {
//            loggerConfig.removeAppender(appenderName);
//        }
    }
}
