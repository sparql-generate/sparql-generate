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

import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;
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

    private final Map<String, SessionManager> sessionManagers = new HashMap<>();

    public SessionManager putSessionManager(String id, SessionManager sessionManager) {
        return sessionManagers.put(id, sessionManager);
    }

    public void removeSessionManager(String id) {
        sessionManagers.remove(id);
    }

    // every time a logging event is passed, a message is sent ot the session associated to the currentThread
    @Override
    protected void append(LoggingEvent event) {
        if (this.layout == null) {
            errorHandler.error("No layout for appender " + name,
                    null, ErrorCode.MISSING_LAYOUT);
            return;
        }
        if(!event.getThreadName().startsWith("session-")) {
            return;
        }
        String n = event.getThreadName().substring(8);
        n = n.substring(0, n.indexOf('-'));
        SessionManager sessionManager = sessionManagers.get(n);
        if(sessionManager==null) {
            System.out.println("Error: no session manager for session id " + n);
            return;
        }
        
        if(event.getLevel().isGreaterOrEqual(sessionManager.getLevel())) {
            final StringBuilder sb = new StringBuilder(this.layout.format(event));
            if (event.getThrowableStrRep() != null) {
                sb.append(String.join("\n", event.getThrowableStrRep()));
                sb.append("\n");
            }
            sessionManager.appendLog(sb.toString());
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
