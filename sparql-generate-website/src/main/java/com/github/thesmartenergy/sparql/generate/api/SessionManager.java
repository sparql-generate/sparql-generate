/*
 * Copyright 2018 École des Mines de Saint-Étienne.
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

import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerateContext;
import com.github.thesmartenergy.sparql.generate.jena.cli.Response;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.websocket.Session;

/**
 *
 * @author maxime.lefrancois
 */
public class SessionManager {

    private static final int TOO_MANY = 1000;

    private static final Gson GSON = new Gson();

    private final Session session;

    private final List<Response> responses = new ArrayList<>();

    private final ScheduledExecutorService service;

    private SPARQLGenerateContext context;

    public SessionManager(final Session session) {
        this.session = session;
        service = Executors.newScheduledThreadPool(1);
        service.scheduleAtFixedRate(this::flush, 0, 200, TimeUnit.MILLISECONDS);
    }

    public SPARQLGenerateContext getContext() {
        return context;
    }

    public void setContext(SPARQLGenerateContext context) {
        this.context = context;
    }
    
    public final String getSessionId() {
        return session.getId();
    }

    void appendLog(String log) {
        responses.add(new Response(log, "", false));
        sendIfTooMany();
    }

    void appendResult(String result) {
        responses.add(new Response("", result, false));
        sendIfTooMany();
    }

    private void sendIfTooMany() {
        if (responses.size() > TOO_MANY) {
            flush();
        }
    }
    
    void clear() {
        responses.add(new Response("", "", true));
        flush();
    }

    void flush() {
        if (!responses.isEmpty()) {
            try {
                session.getBasicRemote().sendText(GSON.toJson(responses));
                responses.clear();
            } catch (IOException ex) {
                System.out.println("SessionManager Error while sending for session: " + session.getId() + ": " + ex.getMessage());
            }
        }
    }

    void stop() {
        System.out.println("SessionManager stopping " + session.getId());
        flush();
        service.shutdown();
    }

}
