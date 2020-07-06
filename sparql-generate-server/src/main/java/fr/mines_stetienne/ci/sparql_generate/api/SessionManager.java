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
package fr.mines_stetienne.ci.sparql_generate.api;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.websocket.Session;

import org.apache.jena.ext.com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.log4j.Level;

import com.google.gson.Gson;

/**
 *
 * @author Maxime Lefrançois
 */
public class SessionManager {

    private static final int TOO_MANY = 1000;

    private static final Gson GSON = new Gson();

    private boolean isOpen = true;

    private final Session session;

    private final List<Response> responses = new ArrayList<>();

    private final ScheduledExecutorService service;
    
    private final ExecutorService executor;

    private Level level = Level.TRACE;

    public SessionManager(final Session session) {
        this.session = session;
        service = Executors.newScheduledThreadPool(1);
        service.scheduleAtFixedRate(this::flush, 0, 200, TimeUnit.MILLISECONDS);
        ThreadFactory tf = new ThreadFactoryBuilder().setNameFormat("session-" + session.getId()).build();
        executor = Executors.newSingleThreadExecutor(tf);
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public final String getSessionId() {
        return session.getId();
    }
    
    public ExecutorService getExecutor() {
		return executor;
	}

    public void appendLog(String log) {
        append(new Response.Log(log));
    }

    public void appendGenerate(String result) {
        append(new Response.Result(result, Response.TYPE.GENERATE));
    }

    public void appendSelect(String result) {
        append(new Response.Result(result, Response.TYPE.SELECT));
    }

    public void appendTemplate(String result) {
        append(new Response.Result(result, Response.TYPE.TEMPLATE));
    }
              
    synchronized private void append(Response response) {
        if (!isOpen) {
            return;
        }
        responses.add(response);
        sendIfTooMany();
    }

    private void sendIfTooMany() {
        if (responses.size() > TOO_MANY) {
            flush();
        }
    }

    synchronized void clear() {
        responses.add(new Response.Clear());
        flush();
    }

    synchronized void flush() {
        if (!isOpen) {
            return;
        }
        if (!responses.isEmpty()) {
            try {
                session.getBasicRemote().sendText(GSON.toJson(responses));
            } catch (Exception ex) {
                System.out.println("Error while sending for session: " + session.getId() + ": " + ex.getMessage() + " " + ex + "\n: " + GSON.toJson(responses));
            } finally {
                responses.clear();
            }
        }
    }

    synchronized void close() {
        if (isOpen) {
            isOpen = false;
            service.shutdownNow();
        }
    }

    public static class Response {

        static private enum TYPE {
            GENERATE(0), SELECT(1), TEMPLATE(2);

            private final int value;

            private TYPE(int value) {
                this.value = value;
            }

        }

        public static class Log extends Response {

            public String log;

            public Log(String log) {
                this.log = log;
            }
        }

        public static class Clear extends Response {

            public boolean clear = true;
        }

        public static class Result extends Response {

            public int type; // 0=GENERATE, 1=SELECT, 2=TEMPLATE
            public String result;

            public Result(String result, TYPE type) {
                this.type = type.value;
                this.result = result;
            }

        }
    }

}
