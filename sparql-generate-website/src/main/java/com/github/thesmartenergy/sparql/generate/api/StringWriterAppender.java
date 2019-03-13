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
import com.github.thesmartenergy.sparql.generate.jena.cli.Response;
import com.google.gson.Gson;
import java.util.HashSet;
import java.util.Set;
import org.apache.jena.sparql.util.Context;
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

    private final Set<Context> contexts = new HashSet<>();

//    public StringWriterAppender() {
//        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
//        Runnable task = () -> {
//            int tot = 0;
//            Set<Thread> uniqueThreads = new HashSet<>();
//            for (Context c : contexts) {
//                final Set<Thread> contextThreads = (Set<Thread>) c.get(SPARQLGenerate.THREAD);
//                tot += contextThreads.size();
//                uniqueThreads.addAll(contextThreads);
//                System.out.println("One of the contexts has threads " + contextThreads);
//            }
//            System.out.println("StringWriterAppender: " + contexts.size() + " contexts. Total: " + tot + "; Unique: "+uniqueThreads.size());
//        };
//        scheduler.scheduleAtFixedRate(task, 0, 5, TimeUnit.MINUTES);
//    }

    public void addContext(Context context) {
        contexts.add(context);
    }

    public void removeContext(Context context) {
        contexts.remove(context);
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
        if(event.getThrowableStrRep() != null) {
            sb.append(String.join("\n", event.getThrowableStrRep()));
            sb.append("\n");
        }
        final Response response = new Response(sb.toString(), "", false);
        for (Context context : contexts) {
            if (context.get(SPARQLGenerate.THREAD) == null) {
                continue;
            }
            for (Thread thread : (Set<Thread>) context.get(SPARQLGenerate.THREAD)) {
                if (thread.getName().equals(event.getThreadName())) {
                    ((SessionManager) context.get(SessionManager.SYMBOL)).appendResponse(response);
                    return;
                }
            }
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
