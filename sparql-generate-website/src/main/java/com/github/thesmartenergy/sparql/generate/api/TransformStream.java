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

import com.google.gson.Gson;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author maxime.lefrancois
 */
@ServerEndpoint("/transformStream")
public class TransformStream {

    private static final Logger LOG = LogManager.getLogger(TransformStream.class);
    private static long MAX_DURATION = TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES);
    private static int MAX_TRIPLES = 5000;

    private TransformUtils utils;

    @PostConstruct
    private void postConstruct() {
        utils = new TransformUtils(UUID.randomUUID().toString(), LOG);
        System.out.println("postconstruct " + utils);
    }

    @OnOpen
    public void open(Session session) {
        System.out.println("Established connection");
        LOG.info("Established connection");
    }

    @OnClose
    public void close(Session session) {
        LOG.info("Closing connection");
    }

    	
    @OnMessage
    public void handleMessage(String message, Session session) throws IOException, InterruptedException {

//		
//        // Print the client message for testing purposes
//        System.out.println("Received: " + message);
//
//        // Send the first message to the client
//        session.getBasicRemote().sendText("This is the first server message");
//
//        // Send 3 messages to the client every 5 seconds
//        int sentMessages = 0;
//        while(sentMessages < 3){
//                Thread.sleep(5000);
//                session.getBasicRemote().
//                        sendText("This is an intermediate server message. Count: " 
//                                + sentMessages);
//                sentMessages++;
//        }
//
//        // Send a final message to the client
//        session.getBasicRemote().sendText("This is the last server message");
        
        LOG.info("Received: " + message);

        Map<String, Object> document = (new Gson()).fromJson(message, Map.class);
        String query, queryurl, defaultGraph, logLevel;
        Map<String, Object> documentset;
        if (document != null ) {
            query = (String) document.getOrDefault("query", "");
            queryurl = (String) document.getOrDefault("queryurl", "");
            defaultGraph = (String) document.getOrDefault("defaultGraph", "");
            logLevel = (String) document.getOrDefault("logLevel", "warn"); 
            documentset = (Map<String, Object>) document.get("documentset");  
        } 

        int sentMessages = 0; 
        while (sentMessages < 3) {
            Thread.sleep(5000);
            session.getBasicRemote().sendText("This is an intermediate server message. Count: " + sentMessages);
            sentMessages++;
        }
          
        // Send a final message to the client
        session.getBasicRemote().sendText("This is the last server message");
        
        Thread.sleep(5000);
        session.close();

//            for (int i = 0; i < documents.size(); i++) {
//                StringMap document = (StringMap) documents.get(i);
//                String uri = (String) document.get("uri");
//                String doc = (String) document.get("document");
//                locator.put(uri, doc);
//                log.trace("with document: " + uri + " = " + doc);
//            }
//        }
//        SPARQLGenerate.resetStreamManager(locator);
//        
//        new Gson()/
//        @DefaultValue("") @FormParam("query") String query,
//            @DefaultValue("") @FormParam("queryurl") String queryurl,
//            @DefaultValue("") @FormParam("defaultGraph") String defaultGraph,
//            @DefaultValue("") @FormParam("documentset") String documentset,
//            @DefaultValue("warn") @FormParam("logLevel") String logLevel
//        
//        
//        session.getQueryString()
//        
//        if (query.equals("") && queryurl.equals("")) {
//            return Response.status(Response.Status.BAD_REQUEST).entity("At least one of query or queryurl query parameters must be set.").build();
//        }
//        if (!queryurl.equals("") && !query.equals("")) {
//            return Response.status(Response.Status.BAD_REQUEST).entity("At most one of query or queryurl query parameters must be set.").build();
//        }
//               
//
//        final StringWriter swLog = utils.setLogger(logLevel);
//
//        try {
//            utils.setStreamManager(documentset);
//        } catch (Exception ex) {
//            LOG.error("Error while processing the documentset", ex);
//            return createResponse(Response.Status.BAD_REQUEST, null, swLog);
//        }
//
//        final StringWriter swOut = new StringWriter();
//        try {
//            if (!queryurl.equals("")) {
//                query = IOUtils.toString(StreamManager.get().open(queryurl), "UTF-8");
//            }
//        } catch (IOException | NullPointerException ex) {
//            LOG.error("Error while loading the query", ex);
//            return createResponse(Response.Status.INTERNAL_SERVER_ERROR, null, swLog);
//        }
//
//        final Model inputModel = utils.setInputModel(defaultGraph);
//
//        final RootPlan plan;
//        try {
//            plan = PlanFactory.create(query);
//        } catch (Exception e) {
//            LOG.error("Exception while processing the request", e);
//            return createResponse(Response.Status.BAD_REQUEST, swOut, swLog);
//        }
//
//        return execFull(plan, inputModel, swOut, swLog);
//        
//        
//
//        final long startDate = (new Date()).getTime();
//
//        try {
//            plan.exec(inputModel, new MyStreamRDF());
//
//            return createResponse(Response.Status.OK, swOut, swLog);
//
//        } catch (Exception ex) {
//            LOG.error("Exception while creating",)
//        }
//        removeAppender();
//        return Response.ok(status(status)
//                .entity(GSON.toJson(response))
//                .type("application/json")
//                .build();
    }

//
//    private static final class MyStreamRDF implements StreamRDF {
//        
//        PrefixMapping pm = PrefixMapping.Standard;        
//        final Map<String, String> response= new HashMap<>();
//
//        @Override
//        public void start() {
//            LOG.trace("Starting streaming.");
//        }
//
//        @Override
//        public void triple(Triple triple) {
//            if (swOut != null) {
//                response.put("output", triple.toString(pm));
//            } 
//            response.put("log", swLog.toString());
//        }
//
//        @Override
//        public void quad(Quad quad) {
//            throw new UnsupportedOperationException("Not supported yet.");
//        }
//
//        @Override
//        public void base(String string) {
//
//        }
//
//        @Override
//        public void prefix(String string, String string1) {
//            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//        }
//
//        @Override
//        public void finish() {
//            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//        }
//    }
}
