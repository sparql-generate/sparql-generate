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
import com.google.gson.Gson;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.websocket.Session;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.ErrorCode;
import org.apache.log4j.spi.LoggingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author maxime.lefrancois
 */
public class StringWriterAppender extends AppenderSkeleton {
    // The iterator across the "clients" Collection must
    // support a "remove()" method.
    
    private static final Logger LOG = LoggerFactory.getLogger(StringWriterAppender.class);
    private static final Gson gson = new Gson();

    private final Map<String,Session> sessions = new HashMap<>();
    
    public void putSession(Thread thread, Session session) {
        this.sessions.put(thread.getName(), session);
    }

    public void removeSession(Thread thread) {
        this.sessions.remove(thread.getName());
    }

    // map of thread - websocket sessions
    // every time a logging event is passed, a message is sent ot the session associated to the currentThread
    @Override
    protected void append(LoggingEvent event) {
        if (this.layout == null) {
            errorHandler.error("No layout for appender " + name,
                    null, ErrorCode.MISSING_LAYOUT);
            return;
        }
        String message = this.layout.format(event);
        Session session = sessions.get(event.getThreadName());
        if (session != null) {
            try {
                session.getBasicRemote().sendText(gson.toJson(new Response(message, "", false)));
            } catch (IOException ex) {
                System.err.println("IOException " + ex.getMessage());
            }
        } else {
            System.err.println("No session found for thread " + event.getThreadName());
        }
    }

    @Override
    public void close() {
        System.out.println("closing");
    }

    @Override
    public boolean requiresLayout() {
        return true;
    }
    
}
