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

import com.github.thesmartenergy.sparql.generate.jena.SPARQLGenerateContext;
import com.google.gson.Gson;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.ErrorCode;
import org.apache.log4j.spi.LoggingEvent;

/**
 *
 * @author maxime.lefrancois
 */
public class StringWriterAppender extends AppenderSkeleton {
    // The iterator across the "clients" Collection must
    // support a "remove()" method.

    private static final Gson gson = new Gson();

    private final Set<SessionManager> sessionManagers = new HashSet<>();

    public void addSessionManager(SessionManager sessionManager) {
        sessionManagers.add(sessionManager);
    }

    public void removeSessionManager(SessionManager sessionManager) {
        sessionManagers.remove(sessionManager);
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
        final StringBuilder sb = new StringBuilder(this.layout.format(event));
        if (event.getThrowableStrRep() != null) {
            sb.append(String.join("\n", event.getThrowableStrRep()));
            sb.append("\n");
        }
        int i = 0;
        for (SessionManager sessionManager : sessionManagers) {
            final SPARQLGenerateContext context = sessionManager.getContext();
            if(context == null) {
                continue;
            }
            for (Thread thread : context.getRegisteredThreads()) {
                if (thread.getName().equals(event.getThreadName())) {
                    i++;
                    sessionManager.appendLog(sb.toString());
                }
            }
        }
        if (i == 0) {
            System.out.print("WARN: " + new Date() + " [" + event.getThreadName() + "] no session got this log message \n\t" + sb.toString());
            sessionManagers.stream().forEach((sessionManager) -> { 
                final SPARQLGenerateContext context = sessionManager.getContext();
                if(context == null) {
                    System.out.println("   session " + sessionManager.getSessionId() + " has no registered thread.");
                } else {
                    System.out.println("   session " + sessionManager.getSessionId() + " has threads " + sessionManager.getContext().getRegisteredThreads());
                }
            });            
        }
        if (i > 1) {
            System.out.println("ERROR: " + new Date() + " [" + Thread.currentThread() + "] more than one session got this log message " + sb.toString());
        }
    }

    @Override
    public void close() {
    }

    @Override
    public boolean requiresLayout() {
        return true;
    }

}
