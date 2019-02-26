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

import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerate;
import com.github.thesmartenergy.sparql.generate.jena.cli.Response;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.websocket.Session;
import org.apache.jena.sparql.util.Symbol;

/**
 *
 * @author maxime.lefrancois
 */
public class SessionManager {

    static final Symbol SYMBOL = Symbol.create(SPARQLGenerate.NS + "symbol_session");

    private static final Gson gson = new Gson();
    
    private final Session session;

    private final List<Response> responses = new ArrayList<>();

    private final ScheduledExecutorService service;

    public SessionManager(final Session session) {
        this.session = session;
        service = Executors.newScheduledThreadPool(1);
        service.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                send();
            }
        }, 0, 200, TimeUnit.MILLISECONDS);  // execute every x seconds
    }

    void appendResponse(Response response) {
        responses.add(response);
        if(responses.size()>1000) {
            send();
        }
    }

    private void send() {
        if (!responses.isEmpty()) {
            try {
                session.getBasicRemote().sendText(gson.toJson(responses));
                responses.clear();
            } catch (IOException ex) {
                System.out.println("SessionManager Error while sending for session: " + session + ": " + ex.getMessage());
            }
        }
    }

    void stop() {
        send();
        service.shutdown();
    }
}
