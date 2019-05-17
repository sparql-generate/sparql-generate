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
package fr.emse.ci.sparqlext.api;

import fr.emse.ci.sparqlext.utils.Request;
import fr.emse.ci.sparqlext.SPARQLExt;
import fr.emse.ci.sparqlext.generate.engine.PlanFactory;
import fr.emse.ci.sparqlext.stream.LocatorStringMap;
import fr.emse.ci.sparqlext.stream.SPARQLExtStreamManager;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
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
import fr.emse.ci.sparqlext.generate.engine.RootPlan;
import fr.emse.ci.sparqlext.query.SPARQLExtQuery;
import fr.emse.ci.sparqlext.utils.Response;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryException;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.serializer.SerializationContext;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sparql.util.FmtUtils;
import org.apache.log4j.Level;

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
    private static final Level[] LEVELS = new Level[]{Level.FATAL, Level.ERROR, Level.WARN, Level.INFO, Level.DEBUG, Level.TRACE};

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

        if (message.getBytes().length > 2 * Math.pow(2, 20)) {
            LOG.warn("Request size exceeded 2 MB.");
            sessionManager.appendLog("ERROR: In this web interface request size cannot exceed 2 MB. Please use the executable jar instead.");
            sessionManager.flush();
            return;
        }
        Request request = GSON.fromJson(message, Request.class);
        if (request.cancel) {
            return;
        }
        sessionManager.appendLog("INFO: starting transformation");
        sessionManager.flush();
        sessionManager.setLevel(LEVELS[request.loglevel]);
        
        final Dataset dataset = DatasetFactory.create();
        final LocatorStringMap loc = new LocatorStringMap();
        final String defaultquery = request.defaultquery;
        final SPARQLExtStreamManager sm;
        final boolean stream = request.stream;
        try {
            sm = readRequest(request, dataset, loc);
        } catch (JsonSyntaxException | IOException ex) {
            logError(sessionManager, "ERROR: while reading parameters:", ex);
            return;
        }
        final Context context = SPARQLExt.createContext(sm, sessionManager.getExecutor());
        final SPARQLExtQuery q;
        try {
            q = supply(sessionManager, ()->(SPARQLExtQuery) QueryFactory.create(defaultquery, SPARQLExt.SYNTAX));
        } catch (InterruptedException | ExecutionException ex) {
            logError(sessionManager, "ERROR: while parsing query:", ex);
            return;
        }
        context.set(SPARQLExt.PREFIX_MANAGER, q.getPrefixMapping());

        final RootPlan plan;
        try {
            plan = supply(sessionManager, ()->PlanFactory.create(q));
        } catch (InterruptedException | ExecutionException ex) {
            logError(sessionManager, "ERROR: while creating execution plan:", ex);
            return;
        }
        if (q.isGenerateType() && !stream) {
            executeGenerate(plan, dataset, context, stream, sessionManager);
        } else if (q.isSelectType() && !stream) {
            executeSelect(plan, q, dataset, context, stream, sessionManager);
        } else if (q.isTemplateType() && !stream) {
            executeTemplate(plan, dataset, context, stream, sessionManager);
        } else if (q.isGenerateType() && stream) {
            executeGenerateStream(plan, dataset, context, stream, sessionManager);
        } else if (q.isSelectType() && stream) {
            executeSelectStream(plan, q, dataset, context, stream, sessionManager);
        } else if (q.isTemplateType() && stream) {
            executeTemplateStream(plan, dataset, context, stream, sessionManager);
        } else {
            logError(sessionManager, "Error: unknown query type:", null);
        }
    }

    @OnError
    public void handleError(Throwable error
    ) {
        if (error instanceof TimeoutException) {
            LOG.error("TimeoutException");
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
                return EnumSet.noneOf(Option.class
                );
            }
        });
    }

    private SPARQLExtStreamManager readRequest(Request request, Dataset dataset, LocatorStringMap loc) throws IOException {
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

    private void executeGenerate(RootPlan plan, Dataset dataset, Context context, boolean stream, SessionManager sessionManager) {
        final ExecutorService executor = sessionManager.getExecutor();
        Executors.newSingleThreadExecutor().submit(() -> {
            final Model model = ModelFactory.createDefaultModel();
            try {
                executor.submit(() -> {
                    plan.execGenerate(dataset, model, context);
                }).get(10, TimeUnit.SECONDS);
            } catch (final TimeoutException ex) {
                LOG.error("In this web interface query execution cannot exceed 10 s. Consider using the executable jar instead.");
            } catch (final InterruptedException ex) {
                LOG.error("Interrupted while executing the request");
            } catch (final ExecutionException ex) {
                LOG.error("An exception occurred", ex);
            } finally {
                final StringWriter sw = new StringWriter();
                model.write(sw, "TTL", "http://example.org/");
                sessionManager.appendResult(sw.toString(), Response.TYPE.GENERATE);
                sessionManager.flush();
                sessionManager.close();
            }
        });
    }

    private void executeGenerateStream(RootPlan plan, Dataset dataset, Context context, boolean stream, SessionManager sessionManager) {
        Executors.newSingleThreadExecutor().submit(() -> {
            final Model model = ModelFactory.createDefaultModel();
            try {
                plan.execGenerate(dataset, new StreamRDF() {
                    final SerializationContext sc = new SerializationContext();

                    @Override
                    public void start() {
                    }

                    @Override
                    public void triple(Triple triple) {
                        sessionManager.appendResult(FmtUtils.stringForTriple(triple, sc) + " .", Response.TYPE.GENERATE);
                    }

                    @Override
                    public void quad(Quad quad) {
                    }

                    @Override
                    public void base(String base) {
                        sc.setBaseIRI(base);
                        sessionManager.appendResult("@base <" + base + "> .", Response.TYPE.GENERATE);
                    }

                    @Override
                    public void prefix(String prefix, String iri) {
                        sc.getPrefixMapping().setNsPrefix(prefix, iri);
                        sessionManager.appendResult("@prefix " + prefix + ": <" + iri + "> .", Response.TYPE.GENERATE);
                    }

                    @Override
                    public void finish() {
                    }
                }, context).get(10, TimeUnit.SECONDS);
            } catch (final TimeoutException ex) {
                LOG.error("In this web interface query execution cannot exceed 10 s. Consider using the executable jar instead.");
            } catch (final InterruptedException ex) {
                LOG.error("Interrupted while executing the request");
            } catch (final ExecutionException ex) {
                LOG.error("An exception occurred", ex);
            } finally {
                final StringWriter sw = new StringWriter();
                model.write(sw, "TTL", "http://example.org/");
                sessionManager.appendResult(sw.toString(), Response.TYPE.GENERATE);
                sessionManager.flush();
                sessionManager.close();
            }
        });
    }

    private void executeSelect(RootPlan plan, Query q, Dataset dataset, Context context, boolean stream, SessionManager sessionManager) {
        final ExecutorService executor = sessionManager.getExecutor();
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                executor.submit(() -> {
                    ResultSet resultSet = plan.execSelect(dataset, context);
                    String output = ResultSetFormatter.asText(resultSet, q);
                    sessionManager.appendResult(output, Response.TYPE.SELECT);
                }).get(10, TimeUnit.SECONDS);
            } catch (final TimeoutException ex) {
                LOG.error("In this web interface query execution cannot exceed 10 s. Consider using the executable jar instead.");
            } catch (final InterruptedException ex) {
                LOG.error("Interrupted while executing the request");
            } catch (final ExecutionException ex) {
                LOG.error("An exception occurred", ex);
            } finally {
                sessionManager.flush();
                sessionManager.close();
            }
        });
    }

    private void executeSelectStream(RootPlan plan, Query q, Dataset dataset, Context context, boolean stream, SessionManager sessionManager) {
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                plan.execSelect(dataset, (resultSet) -> {
                    String output = ResultSetFormatter.asText(resultSet, q);
                    sessionManager.appendResult(output, Response.TYPE.SELECT);
                }, context).get(10, TimeUnit.SECONDS);
            } catch (final TimeoutException ex) {
                LOG.error("In this web interface query execution cannot exceed 10 s. Consider using the executable jar instead.");
            } catch (final InterruptedException ex) {
                LOG.error("Interrupted while executing the request");
            } catch (final ExecutionException ex) {
                LOG.error("An exception occurred", ex);
            } finally {
                sessionManager.flush();
                sessionManager.close();
            }
        });
    }

    private void executeTemplate(RootPlan plan, Dataset dataset, Context context, boolean stream, SessionManager sessionManager) {
        final ExecutorService executor = sessionManager.getExecutor();
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                executor.submit(() -> {
                    String output = plan.execTemplate(dataset, context);
                    sessionManager.appendResult(output, Response.TYPE.TEMPLATE);
                }).get(10, TimeUnit.SECONDS);
            } catch (final TimeoutException ex) {
                LOG.error("In this web interface query execution cannot exceed 10 s. Consider using the executable jar instead.");
            } catch (final InterruptedException ex) {
                LOG.error("Interrupted while executing the request");
            } catch (final ExecutionException ex) {
                LOG.error("An exception occurred", ex);
            } finally {
                sessionManager.flush();
                sessionManager.close();
            }
        });
    }

    private void executeTemplateStream(RootPlan plan, Dataset dataset, Context context, boolean stream, SessionManager sessionManager) {
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                plan.execTemplate(dataset, (s) -> {
                    System.out.println("NEW SOL");
                    sessionManager.appendResult(s, Response.TYPE.TEMPLATE);
                }, context).get(10, TimeUnit.SECONDS);
            } catch (final TimeoutException ex) {
                LOG.error("In this web interface query execution cannot exceed 10 s. Consider using the executable jar instead.");
            } catch (final InterruptedException ex) {
                LOG.error("Interrupted while executing the request");
            } catch (final ExecutionException ex) {
                LOG.error("An exception occurred", ex);
            } finally {
                sessionManager.flush();
                sessionManager.close();
            }   
        }
        );
    }

    private <T> T supply(SessionManager sessionManager, Supplier<T> task) throws InterruptedException, ExecutionException {
        return CompletableFuture.supplyAsync(task, sessionManager.getExecutor()).get();
    }

}
